import logging
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor
import queue

import websocket  # type: ignore

from opendsb.messaging.message import Message
from opendsb.routing.remote.remotepeer import RemotePeer, Router
from opendsb import WebSocketConnectionLost

logger = logging.getLogger("opendsb")


class WebSocketPeer(RemotePeer):
    # DEPRECADO: mantido apenas para compatibilidade com código que referencie o atributo.
    # Não é mais usado para executar conexões — ver _connection_thread abaixo.
    #
    # Este pool era de CLASSE e, portanto, compartilhado por todas as conexões do
    # processo. Como on_close() o desligava quando kill_peer estava setado, encerrar
    # UMA conexão inviabilizava TODAS as conexões seguintes, que passavam a falhar com
    # "cannot schedule new futures after shutdown".
    #
    # Além disso, as threads de um ThreadPoolExecutor não são daemon: como
    # run_forever() só retorna quando a conexão cai, um peer pendurado impedia o
    # processo inteiro de encerrar (o atexit do executor ficava preso no join).
    websocket_pool = ThreadPoolExecutor(max_workers=5)

    def __init__(self, router: Router, address: str, error_queue: queue.Queue=None):
        super().__init__(router, address)
        self._error_queue = error_queue
        self.session: websocket.WebSocketApp = None
        self.lock = threading.Lock()
        self.kill_peer = False
        # Thread dedicada a esta conexão, isolada das demais e daemon, para que uma
        # conexão pendurada nunca impeça o encerramento do processo.
        self._connection_thread: threading.Thread | None = None

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
        self._connection_thread = threading.Thread(
            target=self._wire_connect_task,
            name=f"opendsb-ws-{self.address}",
            daemon=True,
        )
        self._connection_thread.start()

    def on_open(self, wsapp):
        logger.debug("WebsocketPeer on_open(): Connection opened")
        self.wire_connected = True
        self.current_reconnect_delay = self.initial_reconnect_delay
        self.connection_id = "Connection-" + str(uuid.uuid4())
        self.connection_opened()

    def on_message(self, wsapp, json_message: str):
        if json_message:
            str_tmp = json_message[:500]
            logger.debug(f"WebsocketPeer on_message(): '{str_tmp}...'")
        message = self.build_message(json_message)
        self.message_received(message)

    def on_close(self, wsapp, close_status_code, close_msg):
        logger.info(
            f'Connection to peer closed. Session id "{self.connection_id}". Reason code "{close_status_code}" reason phrase "{close_msg}"'
        )
        with self.lock:
            if self.kill_peer:
                # Nada a desligar: a thread desta conexão termina sozinha quando
                # run_forever() retorna, e é daemon. Antes, desligava-se aqui o pool
                # de CLASSE, o que inviabilizava qualquer conexão futura do processo.
                logger.debug(f'Peer "{self.peer_id}" encerrado a pedido (kill_peer).')
            self.connection_closed(close_status_code, close_msg)

    def on_error(self, wsapp, error):
        logger.warning(f'Error detected on bus remote connection to peer "{self.peer_id}" ', exc_info=error)
        if isinstance(error, (websocket.WebSocketConnectionClosedException, TimeoutError, ConnectionRefusedError)):
            logger.warning(f'Error detected on bus remote connection to peer "{self.peer_id}" | "{type(error)}"')
            with self.lock:
                self.reconnect()
        else:
            logger.error(f"Error detected {wsapp}/'{type(error)}'. Lançando Exception ", exc_info=True)
            ex = WebSocketConnectionLost(code=1001,
              message="O peer fechou a conexão.",
              original_exception_class_name=str(type(error))
            )
            logger.info(f"Lançando Exception {str(ex)}")
            if self._error_queue:
                self._error_queue.put(ex)
            raise ex

    def _wire_close_connection(self, **kwargs) -> None:
        try:
            # A flag precisa estar setada ANTES do close(): on_close() roda na thread da
            # conexão e pode ser chamado antes do retorno de close(), o que tornava o
            # encerramento não determinístico.
            if "kill_peer" in kwargs:
                self.kill_peer = kwargs["kill_peer"]
            self.session.close()
            logger.debug("Closing websocket connection")
        except Exception as e:
            logger.error("Error while closing websocket connection", exc_info=True)
            if self._error_queue:
                self._error_queue.put(e)

    def wire_send_message(self, message: Message) -> None:
        try:
            message_str = message.to_json()
        except Exception as e:
            print(
                "WebsocketPeer wire_send_message() - "
                f"Fail to convert from Message object to JSON String '{message}' - {e}",
                flush=True,
            )
            logger.info(
                f"WebsocketPeer wire_send_message() - JSON:s Fail to convert from Message to JSON String '{message}'"
            )
            if self._error_queue:
                self._error_queue.put(e)

            raise Exception("Fail to convert from Message type to JSON String")

        logger.debug(f"WebsocketPeer wire_send_message() - Message will be send: '{message_str}'")
        self.session.send(message_str)
        logger.debug("WebsocketPeer wire_send_message() - Message sent")
