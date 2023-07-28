
from abc import ABC, abstractmethod
from concurrent.futures import Future
import json
import logging
import time
from typing import Protocol
import uuid

from ...messaging.controlmessage import ControlMessage
from ...messaging.datamessage import DataMessage
from ...messaging.replymessage import ReplyMessage
from ...messaging.callmessage import CallMessage
from ...messaging.message import Message
from ...messaging.controlmessage import ControlMessage, ControlMessageType, ControlTokens

logger = logging.getLogger('opendsb')


class Router(Protocol):
    
    @property
    def id(self) -> str:
        ...

    @property
    def separator(self) -> str:
        ...

    def full_subscription_count(self) -> int:
        ...

    def message_to_peer(self, message, remote_peer) -> None:
        ...

    def route_message(self, message: Message, remote: bool = False) -> None:
        ...

    def add_peer(self, remote_peer) -> None:
        ...

    def remove_peer(self, remote_peer) -> None:
        ...


class RemotePeer(ABC):
    def __init__(self, router: Router, address: str):
        self.router = router
        self.address = address
        self.remote_routing_table_counter = {'control': 1}
        self.bus_connected: bool = False
        self.wire_connected: bool = False
        self.pending_bus_connection_id: str = ''
        self.peer_id: str = ''
        self.connection_id: str = ''
        self.connected_futures: list[Future] = []
        self.disconnected_futures: list[Future] = []
        self.connection_id_future: Future = Future()
        self.initial_reconnect_delay = 2 # seconds
        self.current_reconnect_delay = 2 # seconds
        self.maximum_reconnect_delay = 60 # seconds

    @abstractmethod
    def wire_connect(self) -> None:
        pass

    @abstractmethod
    def wire_send_message(self, message: Message) -> None:
        pass

    def connect(self) -> str:
        self.wire_connect()
        return self.connection_id_future.result(timeout=2)
    
    def reconnect(self) -> None:
        logger.info(f'Reconnecting in "{self.current_reconnect_delay}" seconds')
        self.connection_id_future = Future()
        time.sleep(self.current_reconnect_delay)
        if self.current_reconnect_delay < self.maximum_reconnect_delay:
            self.current_reconnect_delay *= 2
        self.connect()


    def send_message(self, message: Message) -> None:
        destination = message.destination
        interested = self.is_remote_peer_interested(destination)
        if not interested:
            logger.debug(f"No listeners registered for '{destination}' in remote peer. Skipping!")
            return
        logger.debug(f"Sending remote message to '{destination}'")
        self.wire_send_message(message)

    def is_remote_peer_interested(self, destination: str) -> bool:
        interested = False
        pieces = destination.split(self.router.separator)
        partial_destination = ''
        for piece in pieces:
            partial_destination = partial_destination + piece
            if partial_destination in self.remote_routing_table_counter and self.remote_routing_table_counter[partial_destination] > 0:
                interested = True
                break
            partial_destination = partial_destination + self.router.separator
        return interested

    def message_received(self, message: Message) -> None:
        if isinstance(message, CallMessage):
            reply_to = message.reply_to
            self.remote_routing_table_counter[reply_to] = 1
            
        if isinstance(message, ControlMessage):
            self.process(message)
            return
        
        self.router.route_message(message, True)

    def process(self, message: ControlMessage) -> None:
        if message.destination == 'control':
            if message.control_message_type == ControlMessageType.UPDATE_ROUTE_COUNT:
                route_table_count = json.loads(message.control_info[ControlTokens.ROUTING_TABLE_COUNT])
                self.remote_routing_table_counter = route_table_count
                return
            if message.control_message_type == ControlMessageType.CONNECTION_REPLY:
                self.do_connection_reply(message)
                return
        self.router.route_message(message, False)


    def do_connection_reply(self, message: ControlMessage) -> None:
        try:
            if self.pending_bus_connection_id == message.control_info[ControlTokens.TRANSACTION_ID]:
                self.peer_id = message.control_info[ControlTokens.SERVER_ID]
                #self.remote_routing_table_counter = json.loads(message.control_info[ControlTokens.ROUTING_TABLE_COUNT])
                self.remote_routing_table_counter = message.control_info[ControlTokens.ROUTING_TABLE_COUNT]
                self.bus_connected = True
                self.notify_connection_success()
            else:
                logger.warning('Received a connection reply from unknown source. Ignoring.')
        except Exception as e:
            logger.error('Failure processing a connection request reply.', e)
            self.notify_connection_failure(e)

    def notify_connection_success(self) -> None:
        for future in self.connected_futures:
            future.set_result(True)
        self.connected_futures.clear()

    def notify_connection_failure(self, exception: Exception) -> None:
        for future in self.connected_futures:
            future.set_exception(exception)
        self.connected_futures.clear()

    def notify_disconnection(self) -> None:
        for future in self.disconnected_futures:
            future.set_result(True)
        self.disconnected_futures.clear()

    def connection_opened(self) -> None:
        self.wire_connected = True
        self.pending_bus_connection_id = f'ConnectionRequest_{uuid.uuid4()}'
        
        connection_request = ControlMessage(
            origin=self.router.id, 
            destination='control', 
            control_message_type=ControlMessageType.CONNECTION_REQUEST, 
            control_info={
                ControlTokens.TRANSACTION_ID: self.pending_bus_connection_id,
                ControlTokens.CLIENT_ID: self.router.id,
                ControlTokens.ROUTING_TABLE_COUNT: json.dumps(self.router.full_subscription_count)
            }
        )
        #self.router.route_message_to_peer(connection_request, self)
        self.send_message(connection_request)
        self.router.add_peer(self)
        self.connection_id_future.set_result(self.connection_id)

    def connection_closed(self, code: int, reason: str) -> None:
        self.wire_connected = False
        self.bus_connected = False
        self.router.remove_peer(self)
        self.notify_disconnection()


    @staticmethod
    def build_message(json_message: str) -> Message:
        message_dict = json.loads(json_message)
        match message_dict['type']:
            case 'CONTROL':
                message = ControlMessage.from_json(json_message)
            case 'PUBLISH':
                message = DataMessage.from_json(json_message)
            case 'CALL':
                message = CallMessage.from_json(json_message)
            case 'REPLY':
                message = ReplyMessage.from_json(json_message)
        return message
