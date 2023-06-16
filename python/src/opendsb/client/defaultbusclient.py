'''DeafultBusClient'''

from concurrent.futures import Future, ThreadPoolExecutor
import logging
from threading import Timer
from typing import Callable

from .busclient import BusClient
from .subscription import Subscription
from ..messaging.callmessage import CallMessage
from ..messaging.controlmessage import ControlMessage, ControlMessageType
from ..messaging.datamessage import DataMessage
from ..messaging.message import Message
from ..messaging.replymessage import ReplyMessage
from ..routing.router import Router


logger = logging.getLogger('__main__')


def reply_handler(response: Future, topic:str, subscription_id: str, router: Router) -> Callable:
    '''ReplyHandler implementation for OpenDSB'''

    def accept(message: Message):
        logger.debug('Entered ReplyHandler')
        if not isinstance(message, ReplyMessage):
            return None
        response.set_result(message)
        router.unsubscribe(topic, subscription_id)
    return accept


def ack_handler(timeout: Timer, topic:str, ack_subscription_id: str, router: Router) -> Callable:
    '''AckHandler implementation for OpenDSB'''
    def accept(message: Message):
        logger.debug('Entered AckHandler')
        if not isinstance(message, ControlMessage) or message.control_message_type != ControlMessageType.CALL_ACK:
            return None
        timeout.cancel()
        router.unsubscribe(topic, ack_subscription_id)
    return accept


class DefaultBusClient(BusClient):
    '''DefaultBusClient implementation for OpenDSB'''

    def __init__(self, router: Router):
        self.timeout = 10
        self.router = router
        self.executor = ThreadPoolExecutor(max_workers=3)

    def subscribe(self, topic: str, handler: Callable) -> Subscription:
        '''Subscribe to a topic'''
        sub_id = self.router.generate_subid()
        logger.debug(f'Subscribing "{sub_id}" to "{topic}"')
        return self.router.subscribe(topic, sub_id, handler)

    def unsubscribe(self, subscription: Subscription) -> None:
        '''Cancel a subscription'''
        logger.debug(f'Unsubscribing "{subscription}"')
        self.router.unsubscribe(subscription.topic, subscription.id)

    def publish_data(self, topic: str, data: str) -> None:
        '''Publish data to a topic'''
        data_message = DataMessage(destination=topic, origin=self.router.id, data=data)
        logger.debug(f'Publishing data message: "{data_message}"')
        self.router.route_message(data_message, True)

    def publish_reply(self, topic: str, reply: str) -> None:
        '''Publish data to a topic'''
        reply_message = ReplyMessage(destination=topic, origin=self.router.id, reply=reply)
        logger.debug(f'Publishing reply message: "{reply_message}"')
        self.router.route_message(reply_message, True)

    def call(self, topic: str, parameters: list[str]) -> Future:
        '''Call a method'''
        logger.debug(f'Calling "{topic}" with parameters "{parameters}"')
        reply_to = f'reply-{self.router.generate_subid()}/{topic}'

        response = Future()
        reply_subscription_id = f'reply-{self.router.generate_subid()}'
        ack_subscription_id = f'ack-{self.router.generate_subid()}'

        _ = self.router.subscribe(reply_to, reply_subscription_id, reply_handler(response, reply_to, reply_subscription_id, self.router))

        # Impede uma mensagem de call para um topico no qual ninguem esta escutando
        timeout_task = Timer(self.timeout, self._timeout_task, [response, reply_to, reply_subscription_id, ack_subscription_id])
        timeout_task.start()

        _ = self.router.subscribe(reply_to, ack_subscription_id, ack_handler(timeout_task, reply_to, ack_subscription_id, self.router))

        logger.debug(f'Creating CallMessage...')
        call_msg = CallMessage(destination=topic, origin=self.router.id, parameters=parameters, reply_to=reply_to)
        self.router.route_message(call_msg, True)

        return response

    def _timeout_task(self, future: Future, topic: str, reply_subscription_id: str, ack_subscription_id: str) -> None:      
        logger.debug(f'Timeout task for "{topic}" has expired') 
        self.router.unsubscribe(topic, reply_subscription_id)
        self.router.unsubscribe(topic, ack_subscription_id)
        future.set_exception(TimeoutError(f'Call to "{topic}" has timed out for "{reply_subscription_id}"'))
        
    
    def __repr__(self) -> str:
        #return f'{self.__class__.__qualname__}(router={self.router}, timeout={self.timeout})'
        return f'{self.__class__.__qualname__}(router={self.router.id}, timeout={self.timeout})'



