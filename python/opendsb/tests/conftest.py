# SPDX-FileCopyrightText: 2023-present João Antonio Ferreira <joao.parana@gmail.com>
#
# SPDX-License-Identifier: MIT
"""Fixtures compartilhadas pelos testes do OpenDSB."""

import base64
import hashlib
import socket
import threading

import pytest

from opendsb.messaging.controlmessage import ControlMessage, ControlMessageType, ControlTokens
from opendsb.routing.defaultrouter import DefaultRouter
from opendsb.routing.remote.remotepeer import RemotePeer
from opendsb.routing.remote.ws.websocketpeer import WebSocketPeer

WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"


@pytest.fixture(autouse=True)
def no_auto_reconnect(monkeypatch):
    """Desliga a reconexão automática durante os testes.

    ``RemotePeer.on_error()`` chama ``reconnect()`` para erros de conexão, e
    ``reconnect()`` dorme e tenta de novo indefinidamente, em threads que NÃO são
    daemon. Num teste que provoca ``ConnectionRefusedError`` — ou que fecha o
    servidor no teardown — isso impede o processo de encerrar: a suíte trava.

    Os testes de erro de conexão verificam o TRATAMENTO do erro, não a política de
    retentativa; esta fixture os isola dela. Um teste que queira exercitar a
    reconexão deve sobrescrever ``reconnect`` explicitamente.
    """
    reconnect_calls = []
    monkeypatch.setattr(RemotePeer, "reconnect", lambda self: reconnect_calls.append(self))
    return reconnect_calls


class FakeMessage:
    """Mensagem mínima: send_message() só olha para o destino."""

    def __init__(self, destination: str):
        self.destination = destination


class SilentWebSocketServer:
    """Servidor que aceita o upgrade WebSocket e depois fica mudo.

    Reproduz um peer que completa o handshake de FIO (HTTP 101) mas não fala o
    protocolo do barramento — não responde ao CONNECTION_REQUEST. É o cenário
    suspeito no ambiente de produção descrito em
    ``sapiens-old/docs/analise-falha-producao-2026-07-27.md`` (cenário B-1) e o
    caso que mais confunde o diagnóstico, porque nenhuma camada acusa erro.
    """

    def __init__(self):
        self._sock = socket.socket()
        self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self._sock.bind(("127.0.0.1", 0))
        self._sock.listen(5)
        self.port = self._sock.getsockname()[1]
        self.received_bytes = threading.Event()
        self._connections = []
        self._thread = threading.Thread(target=self._accept_loop, daemon=True)
        self._thread.start()

    @property
    def url(self) -> str:
        return f"ws://127.0.0.1:{self.port}/api-v1/somaServer"

    def _accept_loop(self) -> None:
        while True:
            try:
                conn, _ = self._sock.accept()
            except OSError:
                return
            self._connections.append(conn)
            threading.Thread(target=self._handle, args=(conn,), daemon=True).start()

    def _handle(self, conn) -> None:
        try:
            request = conn.recv(65536).decode("utf-8", "replace")
            key = ""
            for line in request.split("\r\n"):
                if line.lower().startswith("sec-websocket-key:"):
                    key = line.split(":", 1)[1].strip()
            accept = base64.b64encode(hashlib.sha1((key + WS_GUID).encode()).digest()).decode()  # noqa: S324
            conn.sendall(
                (
                    "HTTP/1.1 101 Switching Protocols\r\n"
                    "Upgrade: websocket\r\n"
                    "Connection: Upgrade\r\n"
                    f"Sec-WebSocket-Accept: {accept}\r\n\r\n"
                ).encode()
            )
            # Handshake de fio completo; a partir daqui, silêncio absoluto.
            while conn.recv(65536):
                self.received_bytes.set()
        except OSError:
            pass
        finally:
            conn.close()

    def close(self) -> None:
        # Fechar só o socket de escuta deixaria as conexões aceitas vivas, e com elas
        # os run_forever() do lado cliente.
        for conn in self._connections:
            try:
                conn.close()
            except OSError:
                pass
        self._sock.close()


@pytest.fixture
def silent_ws_server():
    server = SilentWebSocketServer()
    yield server
    server.close()


@pytest.fixture
def closed_port() -> int:
    """Porta garantidamente sem ninguém escutando."""
    sock = socket.socket()
    sock.bind(("127.0.0.1", 0))
    port = sock.getsockname()[1]
    sock.close()
    return port


@pytest.fixture
def router() -> DefaultRouter:
    return DefaultRouter()


@pytest.fixture
def peer(router) -> WebSocketPeer:
    return WebSocketPeer(router, "ws://soma-prd:8085/api-v1/somaServer")


def make_connection_reply(transaction_id: str, server_id: str, routing_table: dict) -> ControlMessage:
    """CONNECTION_REPLY como o servidor SOMA o envia ao final do handshake."""
    return ControlMessage(
        origin=server_id,
        destination="control",
        control_message_type=ControlMessageType.CONNECTION_REPLY,
        control_info={
            ControlTokens.TRANSACTION_ID: transaction_id,
            ControlTokens.SERVER_ID: server_id,
            ControlTokens.ROUTING_TABLE_COUNT: routing_table,
        },
    )


def make_route_count_update(routing_table: dict) -> ControlMessage:
    return ControlMessage(
        origin="peer",
        destination="control",
        control_message_type=ControlMessageType.UPDATE_ROUTE_COUNT,
        control_info={ControlTokens.ROUTING_TABLE_COUNT: routing_table},
    )
