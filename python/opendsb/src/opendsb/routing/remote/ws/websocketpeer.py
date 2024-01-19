import logging
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor

import websocket  # type: ignore

from opendsb.messaging.message import Message
from opendsb.routing.remote.remotepeer import RemotePeer, Router

logger = logging.getLogger("opendsb")


class WebSocketPeer(RemotePeer):
    websocket_pool = ThreadPoolExecutor(max_workers=5)  # Define a quantidada maxima de vizinhos simultaneos

    def __init__(self, router: Router, address: str):
        super().__init__(router, address)
        self.session: websocket.WebSocketApp = None
        self.lock = threading.Lock()
        self.kill_peer = False

    def _wire_connect_task(self) -> None:
        self.session = websocket.WebSocketApp(
            self.address,
            on_open=self.on_open,
            on_message=self.on_message,
            on_error=self.on_error,
            on_close=self.on_close,
        )
        self.session.run_forever()

    def _wire_connect(self) -> None:
        WebSocketPeer.websocket_pool.submit(self._wire_connect_task)

    def on_open(self, wsapp):
        logger.debug("Websocket Connection opened")
        self.wire_connected = True
        self.current_reconnect_delay = self.initial_reconnect_delay
        self.connection_id = "Connection-" + str(uuid.uuid4())
        self.connection_opened()

    def on_message(self, wsapp, json_message: str):
        # logger.debug(f"Websocket Message received: '{json_message}'"[:500])
        # logger.debug(f"Websocket Message received: '{json_message}'")
        message = self.build_message(json_message)
        self.message_received(message)

    def on_close(self, wsapp, close_status_code, close_msg):
        logger.info(
            f'Connection to peer closed. Session id "{self.connection_id}". Reason code "{close_status_code}" reason phrase "{close_msg}"'
        )
        with self.lock:
            if self.kill_peer:
                WebSocketPeer.websocket_pool.shutdown(wait=False)
            self.connection_closed(close_status_code, close_msg)

    def on_error(self, wsapp, error):
        # logger.warning(f'Error detected on bus remote connection to peer "{self.peer_id}" ', exc_info=error)
        if isinstance(error, (websocket.WebSocketConnectionClosedException, TimeoutError, ConnectionRefusedError)):
            logger.warning(f'Error detected on bus remote connection to peer "{self.peer_id}" | "{type(error)}"')
            with self.lock:
                self.reconnect()
        else:
            logger.error("Error detected", exc_info=True)

    def _wire_close_connection(self, **kwargs) -> None:
        try:
            self.session.close()
            if "kill_peer" in kwargs:
                self.kill_peer = kwargs["kill_peer"]
            logger.debug("Closing websocket connection")
        except Exception:
            logger.error("Error while closing websocket connection", exc_info=True)

    def wire_send_message(self, message: Message) -> None:
        message_str = message.to_json()
        self.session.send(message_str)
        logger.debug(f"Websocket Message sent: '{message_str}'")
