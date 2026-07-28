# SPDX-FileCopyrightText: 2023-present João Antonio Ferreira <joao.parana@gmail.com>
#
# SPDX-License-Identifier: MIT
"""Filtro de roteamento remoto e visibilidade das mensagens descartadas.

Um peer só transmite uma mensagem se o lado remoto tiver anunciado assinantes para
o destino (ou para algum de seus prefixos). Quando não anunciou, a mensagem é
descartada ANTES do fio: quem chamou fica esperando uma resposta que jamais virá,
até estourar o timeout, sem nenhum erro de rede em lugar algum.

Essa era a falha silenciosa investigada em
``sapiens-old/docs/analise-falha-producao-2026-07-27.md``.
"""

import logging

import pytest

from tests.conftest import FakeMessage, make_connection_reply, make_route_count_update

MONITOR_TOPIC = "service/SapiensDataIngestionMonitorService"
MONITOR_CALL = f"{MONITOR_TOPIC}/publishControlStartEvent"


@pytest.fixture
def spy_peer(peer):
    """Peer que registra o que foi efetivamente enviado pelo fio."""
    peer.peer_id = "soma-prd-1"
    peer.sent_over_the_wire = []
    peer.wire_send_message = lambda message: peer.sent_over_the_wire.append(message.destination)
    return peer


def warnings_about_discard(caplog) -> list[str]:
    return [
        record.getMessage()
        for record in caplog.records
        if record.levelno == logging.WARNING and "No listeners registered" in record.getMessage()
    ]


class TestInterestFilter:
    """A regra de prefixos que decide se a mensagem vai para o fio."""

    @pytest.mark.parametrize(
        ("announced", "expected"),
        [
            ({"control": 1}, False),
            ({"control": 1, "service": 3}, True),
            ({"control": 1, MONITOR_TOPIC: 1}, True),
            ({"control": 1, MONITOR_CALL: 1}, True),
            ({"control": 1, "service/OutroService": 2}, False),
            ({"control": 1, MONITOR_TOPIC: 0}, False),
        ],
    )
    def test_prefix_matching(self, spy_peer, announced, expected):
        spy_peer.remote_routing_table_counter = announced

        assert spy_peer.is_remote_peer_interested(MONITOR_CALL) is expected

    def test_message_is_not_sent_when_peer_is_not_interested(self, spy_peer):
        spy_peer.remote_routing_table_counter = {"control": 1}

        spy_peer.send_message(FakeMessage(MONITOR_CALL))

        assert spy_peer.sent_over_the_wire == []

    def test_message_is_sent_when_peer_announced_the_topic(self, spy_peer):
        spy_peer.remote_routing_table_counter = {"control": 1, MONITOR_TOPIC: 1}

        spy_peer.send_message(FakeMessage(MONITOR_CALL))

        assert spy_peer.sent_over_the_wire == [MONITOR_CALL]


class TestDiscardIsVisible:
    """O descarte precisa aparecer no log de operação (nível INFO).

    Antes era ``DEBUG``: em produção, com o log em INFO, a mensagem sumia sem
    deixar rastro e a falha ficava praticamente indiagnosticável.
    """

    def test_first_discard_is_logged_as_warning(self, spy_peer, caplog):
        caplog.set_level(logging.DEBUG, logger="opendsb")
        spy_peer.remote_routing_table_counter = {"control": 1}

        spy_peer.send_message(FakeMessage(MONITOR_CALL))

        warnings = warnings_about_discard(caplog)
        assert len(warnings) == 1
        assert MONITOR_CALL in warnings[0]
        assert "DISCARDED" in warnings[0], "o log deve dizer que a mensagem não foi enviada"
        assert "soma-prd-1" in warnings[0], "o log deve identificar o peer"
        assert "['control']" in warnings[0], "o log deve mostrar a routing table anunciada"

    def test_repeated_discards_are_deduplicated(self, spy_peer, caplog):
        """Sem dedupe, um tópico tentado em laço inundaria o log."""
        caplog.set_level(logging.DEBUG, logger="opendsb")
        spy_peer.remote_routing_table_counter = {"control": 1}

        for _ in range(10):
            spy_peer.send_message(FakeMessage(MONITOR_CALL))

        assert len(warnings_about_discard(caplog)) == 1

    def test_distinct_destinations_are_each_reported(self, spy_peer, caplog):
        caplog.set_level(logging.DEBUG, logger="opendsb")
        spy_peer.remote_routing_table_counter = {"control": 1}

        spy_peer.send_message(FakeMessage(MONITOR_CALL))
        spy_peer.send_message(FakeMessage("service/OutroService/metodo"))

        assert len(warnings_about_discard(caplog)) == 2


class TestBusHandshakeLogging:
    """O handshake de BARRAMENTO precisa ser distinguível do de FIO.

    Sem este log, o nível INFO só evidenciava o WebSocket aberto, o que levava a
    confundir "socket de pé" com "barramento pronto" — estados bem diferentes.
    """

    def test_connection_reply_is_logged_with_peer_and_routing_table(self, spy_peer, caplog):
        caplog.set_level(logging.INFO, logger="opendsb")
        spy_peer.pending_bus_connection_id = "ConnectionRequest_abc"

        spy_peer.do_connection_reply(
            make_connection_reply("ConnectionRequest_abc", "soma-server-9", {"control": 1, MONITOR_TOPIC: 2})
        )

        messages = [r.getMessage() for r in caplog.records if "Bus connected" in r.getMessage()]
        assert len(messages) == 1
        assert "soma-server-9" in messages[0]
        assert MONITOR_TOPIC in messages[0]

    def test_connection_reply_marks_the_bus_as_connected(self, spy_peer):
        spy_peer.pending_bus_connection_id = "tx-1"

        spy_peer.do_connection_reply(make_connection_reply("tx-1", "soma-1", {"control": 1}))

        assert spy_peer.bus_connected is True
        assert spy_peer.peer_id == "soma-1"

    def test_reply_from_unknown_transaction_is_ignored(self, spy_peer):
        spy_peer.pending_bus_connection_id = "tx-esperada"

        spy_peer.do_connection_reply(make_connection_reply("tx-outra", "impostor", {"control": 1}))

        assert spy_peer.bus_connected is False
        assert spy_peer.peer_id != "impostor"


class TestDedupeIsResetWhenRoutingTableChanges:
    """Um destino inalcançável pode passar a existir; o alerta não pode ficar mudo."""

    def test_route_count_update_clears_the_dedupe(self, spy_peer, caplog):
        caplog.set_level(logging.DEBUG, logger="opendsb")
        spy_peer.remote_routing_table_counter = {"control": 1}
        spy_peer.send_message(FakeMessage(MONITOR_CALL))
        assert MONITOR_CALL in spy_peer.unrouteable_destinations

        spy_peer.process(make_route_count_update({"control": 1, "service": 5}))

        assert spy_peer.unrouteable_destinations == set()

    def test_message_flows_after_the_topic_is_announced(self, spy_peer):
        spy_peer.remote_routing_table_counter = {"control": 1}
        spy_peer.send_message(FakeMessage(MONITOR_CALL))
        assert spy_peer.sent_over_the_wire == []

        spy_peer.process(make_route_count_update({"control": 1, "service": 5}))
        spy_peer.send_message(FakeMessage(MONITOR_CALL))

        assert spy_peer.sent_over_the_wire == [MONITOR_CALL]

    def test_connection_reply_also_clears_the_dedupe(self, spy_peer):
        spy_peer.remote_routing_table_counter = {"control": 1}
        spy_peer.send_message(FakeMessage(MONITOR_CALL))
        spy_peer.pending_bus_connection_id = "tx-1"

        spy_peer.do_connection_reply(make_connection_reply("tx-1", "soma-1", {"control": 1, MONITOR_TOPIC: 1}))

        assert spy_peer.unrouteable_destinations == set()
