# -*- coding: utf-8 -*-

from concurrent.futures import ThreadPoolExecutor
import logging
from typing import Callable

from opendsb.client.busclient import BusClient
from opendsb.client.defaultbusclient import DefaultBusClient
from opendsb.client.subscription import Subscription
from opendsb.messaging.message import Message
from opendsb.messaging.callmessage import CallMessage
from opendsb.messaging.controlmessage import ControlMessage, ControlMessageType
from opendsb.routing.remote.remotepeer import RemotePeer
from opendsb.routing.remote.ws.websocketpeer import WebSocketPeer
from opendsb.routing.routenode import RouteNode
from opendsb.routing.router import Router

logger = logging.getLogger('opendsb')


class DefaultRouter(Router):
    """ Default implementation of a router

    Comments in Portuguese:
        Os enderecos de roteamento podem ser representados a partir de uma arvore
        Para aproveitar a velocidade de acesso de uma hashtable (dict) esta arvore esta representada em mapa
        Cada par (chave:valor) do mapa representa um no da arvore
        Cada no da arvore possui um conjunto de inscricoes (subscribers) 
        Cada inscricao possui um handler (listener)
        routing_table: dict[Address: str, "Listeners": RouteNode]

        # TODO: no futuro escrever um executor.shutdown()
    """

    def __init__(self):
        super().__init__()
        self.separator: str = '/' # Topic/Destination separator
        self.routing_table: dict[str, RouteNode] = dict() # dict[Address/Topic/Destination, "Listeners": RouteNode]
        self.executor: ThreadPoolExecutor = ThreadPoolExecutor(max_workers=5)
        self.remote_peers: dict[str, RemotePeer] = dict() # dict[Connection_id, RemotePeer]
        #self.lock = threading.Lock()


    def subscribe(self, topic: str, subscription_id: str, handler: Callable) -> Subscription:
        #with self.lock:
        if topic in self.routing_table.keys():
            subnode = self.routing_table[topic]
        else:
            #subnode = RouteNode(topic, self) # Commented because RouteNode no longer needs the router id
            subnode = RouteNode(topic)
            self.routing_table[topic] = subnode
        subscription = subnode.subscribe(subscription_id, handler)
        logger.debug(f'Routing table: "{self.routing_table}"')
        return subscription

    def unsubscribe(self, topic: str, subscription_id: str) -> None:
        routenode = self.routing_table[topic]
        routenode.unsubscribe(subscription_id)
        node_empty = not self._route_has_subscriptions(topic)
        if node_empty:
            self.routing_table.pop(topic)
        logger.debug(f'Routing table: "{self.routing_table}"')

    def route_message(self, message: Message, remote: bool = False) -> None:
        """Route message to destination using an asynchronous task"""      
        logger.debug(f'Routing "{message}"'[:500])
        self.executor.submit(self._routing_task, message, remote)

    def _routing_task(self, message: Message, remote: bool) -> None:
        pieces = message.destination.split(self.separator)
        #logger.debug(f'Possible destinations for message: "{pieces =}" "{message}"')
        concat = ''
        for piece in pieces:
            destination = concat + piece
            #logger.debug(f'Routing task for message from "{message.origin}" to "{message.destination}" with type "{message.type}"')
            route_exists_and_has_subscriptions = self._route_exists(destination) and self._route_has_subscriptions(destination)
            if route_exists_and_has_subscriptions:
                self._execute_routing(destination, message)
            concat = destination + self.separator
        
        if remote:
            self.route_message_to_remote_peers(message)
            

    def route_message_to_remote_peers(self, message: Message) -> None:
        """Route message to remote peers using an asynchronous task"""
        logger.debug(f'Routing "{message}" to remote peers'[:500])
        previous_hop = message.latest_hop
        message.latest_hop = self.id
        # Send the message to all peers except the one that send the message. (Flood strategy)
        for peer in self.remote_peers.values():
            if peer.peer_id == previous_hop:
                continue
            peer.send_message(message)

    def _execute_routing(self, destination: str, message: Message) -> None:
        logger.debug(f'Routing "{message}" to "{destination}"'[:500])
        if isinstance(message, CallMessage):
            logger.debug(f'Handling special case "CallMessage". Routing "ControlMessage" to ACK.')
            ack = None
            try:
                ack = ControlMessage(self.id, message.reply_to, ControlMessageType.CALL_ACK, {'transactionId': message.id})
                self.route_message(ack)
            except Exception as e:
                logger.warning(f'Unable to route ack message "{ack}"', exc_info=True)
        node = self.routing_table[destination]
        node.accept(message)

    def _route_exists(self, topic: str) -> bool:
        """Check if route exists"""
        return topic in self.routing_table.keys()

    def _route_has_subscriptions(self, topic: str) -> bool:
        """Check if route has subscriptions"""
        return len(self.routing_table[topic].subscribers) > 0
    
    def __repr__(self) -> str:
        return f'{self.__class__.__qualname__}(routing_table=Dict[Topic, RouteNode], id={self.id})'


    @property
    def full_subscription_count(self) -> dict[str, int]:
        """Returns the number of subscriptions in each topic"""
        return {topic: len(node.subscribers) for topic, node in self.routing_table.items()}


    def is_wire_connected(self, remote_peer_id: str) -> bool:
        return self.remote_peers[remote_peer_id].wire_connected

    def is_bus_connected(self, remote_peer_id: str) -> bool:
        return self.remote_peers[remote_peer_id].bus_connected

    def get_remote_peer(self, remote_peer_id: str) -> RemotePeer:
        return self.remote_peers[remote_peer_id]

    def connect_to_remote(self, address: str) -> BusClient:
        logger.info('Starting OpenDSB...')

        logger.info('Creating client...')
        client = DefaultBusClient(self)

        logger.info(f'Connecting to remote peer at "{address}"...')
        self.connect_to_remote_router(address)
        return client

    def connect_to_remote_router(self, address: str) -> str:
        try:
            remote_peer = WebSocketPeer(self, address)
            return remote_peer.connect()
        except Exception as e:
            logger.warning(f'Failure establishing connection to address "{address}"', exc_info=True)
            #raise IOError(f'Unable to establish connection with server at "{address}"', e)
            return ''

    def route_message_to_peer(self, message: Message, peer: RemotePeer) -> None:
        self.executor.submit(self._route_message_to_peer_task, message, peer)

    def _route_message_to_peer_task(self, message: Message, peer: RemotePeer) -> None:
        try:
            peer.send_message(message)
        except Exception as e:
            logger.error(f'Failed to send message to remote peer "{peer}"', exc_info=True)
    
    def add_peer(self, peer: RemotePeer) -> None:
        self.remote_peers[peer.connection_id] = peer
        logger.debug(f'Peer "{peer}" added to router "{self}"')

    def remove_peer(self, peer: RemotePeer) -> None:
        if peer.connection_id in self.remote_peers:
            self.remote_peers.pop(peer.connection_id)
            logger.debug(f'Peer "{peer}" removed from router "{self}"')

    def disconnect_remote_peer(self, remote_peer_id: str, **kwargs) -> None:
        self.remote_peers[remote_peer_id].disconnect(**kwargs)

    def disconnect_all_remote_peers(self, **kwargs) -> None:
        # Thread safe coding
        peers_ids = list(self.remote_peers.keys())
        for peer_id in peers_ids:
            self.disconnect_remote_peer(peer_id, **kwargs)
