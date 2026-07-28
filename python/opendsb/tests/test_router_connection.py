# SPDX-FileCopyrightText: 2023-present João Antonio Ferreira <joao.parana@gmail.com>
#
# SPDX-License-Identifier: MIT
"""Estabelecimento de conexão pelo DefaultRouter e propagação de erros."""

import logging
import queue

import pytest

from opendsb.routing.defaultrouter import DefaultRouter

from tests.conftest import FakeMessage

MONITOR_CALL = "service/SapiensDataIngestionMonitorService/publishControlStartEvent"


class TestErrorQueueIsOptional:
    """``error_queue`` é opcional e não pode mascarar a falha real.

    Regressão: ``connect_to_remote_router()`` fazia ``error_queue.put(ex)`` sem
    verificar ``None``. Clientes antigos — o ``gamma_py_connector`` 1.0.15, por
    exemplo — instanciam ``DefaultRouter()`` sem fila, então qualquer falha de
    conexão virava ``AttributeError`` e escondia a causa verdadeira.
    """

    def test_connection_failure_without_queue_raises_ioerror(self, closed_port):
        router = DefaultRouter()  # sem error_queue, como o gamma_py_connector 1.0.15

        with pytest.raises(IOError, match="Unable to establish connection"):
            router.connect_to_remote_router(f"ws://127.0.0.1:{closed_port}/api-v1/somaServer")

    def test_connection_failure_preserves_the_original_cause(self, closed_port):
        router = DefaultRouter()

        with pytest.raises(IOError) as exc_info:
            router.connect_to_remote_router(f"ws://127.0.0.1:{closed_port}/api-v1/somaServer")

        assert exc_info.value.__cause__ is not None, "a exceção original deve ser encadeada"

    def test_connection_failure_with_queue_publishes_the_exception(self, closed_port):
        error_queue = queue.Queue()
        router = DefaultRouter(error_queue)

        with pytest.raises(IOError):
            router.connect_to_remote_router(f"ws://127.0.0.1:{closed_port}/api-v1/somaServer")

        assert not error_queue.empty()

    def test_send_failure_is_published_to_the_queue(self):
        error_queue = queue.Queue()
        router = DefaultRouter(error_queue)

        class ExplodingPeer:
            peer_id = "p1"

            def send_message(self, message):
                msg = "falha no envio"
                raise RuntimeError(msg)

        router._route_message_to_peer_task(FakeMessage(MONITOR_CALL), ExplodingPeer())

        assert isinstance(error_queue.get_nowait(), RuntimeError)


class TestSilentPeerIntegration:
    """Integração contra um peer que aceita o WebSocket e não fala o barramento.

    É o cenário B-1 de ``analise-falha-producao-2026-07-27.md``: nenhuma camada
    acusa erro, o socket fica de pé e as chamadas simplesmente nunca respondem.
    O teste fixa o comportamento que permite reconhecê-lo.
    """

    def test_wire_connects_but_bus_does_not(self, silent_ws_server):
        router = DefaultRouter()

        router.connect_to_remote(silent_ws_server.url)

        peers = list(router.remote_peers.values())
        assert len(peers) == 1
        peer = peers[0]
        assert peer.wire_connected is True, "o WebSocket abre normalmente"
        assert peer.bus_connected is False, "mas o handshake de barramento nunca completa"

    def test_connection_request_reaches_the_server(self, silent_ws_server):
        router = DefaultRouter()

        router.connect_to_remote(silent_ws_server.url)

        assert silent_ws_server.received_bytes.wait(timeout=5), "o CONNECTION_REQUEST deve chegar ao servidor"

    def test_routing_table_stays_at_the_initial_value(self, silent_ws_server):
        """Sem CONNECTION_REPLY, o peer só conhece o tópico 'control'."""
        router = DefaultRouter()

        router.connect_to_remote(silent_ws_server.url)

        peer = next(iter(router.remote_peers.values()))
        assert sorted(peer.remote_routing_table_counter) == ["control"]

    def test_service_calls_are_discarded_with_a_warning(self, silent_ws_server, caplog):
        caplog.set_level(logging.DEBUG, logger="opendsb")
        router = DefaultRouter()
        router.connect_to_remote(silent_ws_server.url)
        peer = next(iter(router.remote_peers.values()))

        peer.send_message(FakeMessage(MONITOR_CALL))

        warnings = [
            r.getMessage()
            for r in caplog.records
            if r.levelno == logging.WARNING and "No listeners registered" in r.getMessage()
        ]
        assert len(warnings) == 1
        assert MONITOR_CALL in warnings[0]
