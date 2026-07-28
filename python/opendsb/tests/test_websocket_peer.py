# SPDX-FileCopyrightText: 2023-present João Antonio Ferreira <joao.parana@gmail.com>
#
# SPDX-License-Identifier: MIT
"""Ciclo de vida da conexão WebSocket de um peer.

Cobre três defeitos corrigidos na versão 2.2.1, todos no caminho de ENCERRAMENTO
da conexão — o menos exercitado e, por isso, o que mais tempo passou quebrado.
"""

import pytest

from opendsb import WebSocketConnectionLost
from opendsb.routing.remote.ws.websocketpeer import WebSocketPeer


class TestConnectionThreadIsolation:
    """Cada conexão roda numa thread própria e daemon.

    Duas regressões corrigidas de uma vez:

    * ``websocket_pool`` era um ``ThreadPoolExecutor`` de CLASSE e ``on_close()`` o
      desligava sempre que ``kill_peer`` estivesse setado — encerrar UMA conexão
      derrubava o executor de TODO o processo, e qualquer conexão posterior falhava
      com "cannot schedule new futures after shutdown". Na prática, um cliente que
      se reconectasse nunca mais conseguia abrir uma conexão.
    * As threads de um ``ThreadPoolExecutor`` não são daemon. Como ``run_forever()``
      só retorna quando a conexão cai, uma conexão viva ou pendurada impedia o
      processo inteiro de encerrar.
    """

    def test_connection_runs_on_a_daemon_thread(self, silent_ws_server, router):
        peer = WebSocketPeer(router, silent_ws_server.url)

        peer.connect()

        assert peer._connection_thread is not None
        assert peer._connection_thread.daemon, "uma conexão pendurada não pode segurar o processo"

    def test_each_peer_gets_its_own_thread(self, silent_ws_server, router):
        first = WebSocketPeer(router, silent_ws_server.url)
        second = WebSocketPeer(router, silent_ws_server.url)

        first.connect()
        second.connect()

        assert first._connection_thread is not second._connection_thread

    def test_killing_one_peer_does_not_affect_another(self, silent_ws_server, router):
        killed = WebSocketPeer(router, silent_ws_server.url)
        survivor = WebSocketPeer(router, silent_ws_server.url)
        killed.connect()
        survivor.connect()

        killed.disconnect(kill_peer=True)

        assert survivor._connection_thread.is_alive(), "a conexão dos demais peers deve sobreviver"

    def test_new_peer_still_connects_after_another_was_killed(self, silent_ws_server, router):
        """O sintoma que inviabilizava qualquer política de reconexão."""
        killed = WebSocketPeer(router, silent_ws_server.url)
        killed.connect()
        killed.disconnect(kill_peer=True)

        fresh = WebSocketPeer(router, silent_ws_server.url)
        connection_id = fresh.connect()

        assert connection_id, "um peer novo deve conseguir conectar após o kill_peer de outro"
        assert fresh.wire_connected is True

    def test_class_level_pool_is_never_shut_down(self, silent_ws_server, router):
        """O atributo de classe é vestigial, mas não pode voltar a ser desligado."""
        peer = WebSocketPeer(router, silent_ws_server.url)
        peer.connect()

        peer.disconnect(kill_peer=True)

        assert not WebSocketPeer.websocket_pool._shutdown


class TestKillPeerOrdering:
    """``kill_peer`` precisa valer ANTES de ``session.close()``.

    Regressão: a flag era atribuída depois do ``close()``, mas ``on_close()`` roda
    na thread da conexão e pode ser chamado antes do retorno do ``close()``. O
    encerramento ficava não determinístico.
    """

    def test_flag_is_set_before_close_is_called(self, peer):
        observed = []

        class SessionSpy:
            def close(self):
                observed.append(peer.kill_peer)

        peer.session = SessionSpy()
        peer.disconnect(kill_peer=True)

        assert observed == [True], "kill_peer já deveria valer True durante o close()"

    def test_close_without_kill_peer_keeps_flag_false(self, peer):
        class SessionSpy:
            def close(self):
                pass

        peer.session = SessionSpy()
        peer.disconnect()

        assert peer.kill_peer is False

    def test_close_failure_is_reported_to_error_queue(self, router):
        import queue

        error_queue = queue.Queue()
        peer = WebSocketPeer(router, "ws://host:8085/x", error_queue)

        class BrokenSession:
            def close(self):
                msg = "socket já fechado"
                raise OSError(msg)

        peer.session = BrokenSession()
        peer.disconnect(kill_peer=True)

        assert not error_queue.empty()
        assert isinstance(error_queue.get_nowait(), OSError)


class TestWebSocketConnectionLost:
    """A exceção do ramo de erro precisa ser construível.

    Regressão: ``on_error()`` construía a exceção com ``original_exception_class=``,
    mas o parâmetro se chama ``original_exception_class_name``. Todo erro que caísse
    nesse ramo virava um ``TypeError``, escondendo a falha original.
    """

    def test_accepts_the_keyword_used_by_on_error(self):
        exception = WebSocketConnectionLost(
            code=1001,
            message="O peer fechou a conexão.",
            original_exception_class_name=str(TypeError),
        )

        assert exception.code == 1001
        assert "1001" in str(exception)
        assert "TypeError" in str(exception)

    def test_is_a_connection_error(self):
        exception = WebSocketConnectionLost(code=1006, message="x", original_exception_class_name="y")

        assert isinstance(exception, ConnectionError)

    def test_rejects_the_old_wrong_keyword(self):
        """Guarda contra a reintrodução do kwarg errado."""
        with pytest.raises(TypeError):
            WebSocketConnectionLost(code=1001, message="x", original_exception_class="y")
