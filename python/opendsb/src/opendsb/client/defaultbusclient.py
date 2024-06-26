"""DeafultBusClient"""

import logging
from concurrent.futures import Future, ThreadPoolExecutor
from threading import Timer
from typing import Callable

from opendsb.client.busclient import BusClient, Result, Serializable
from opendsb.client.subscription import Subscription
from opendsb.messaging.callmessage import CallMessage
from opendsb.messaging.controlmessage import ControlMessage, ControlMessageType
from opendsb.messaging.datamessage import DataMessage, TypedData
from opendsb.messaging.message import Message
from opendsb.messaging.replymessage import ReplyMessage
from opendsb.routing.router import Router

# from ..utils.dictvalidator import DictValidator

logger = logging.getLogger("opendsb")


# TODO: Criar Type Alias ReplyHandler para Callable[[Message], None]
def reply_handler(response: Future, topic: str, subscription_id: str, router: Router) -> Callable:
    """ReplyHandler implementation for OpenDSB"""

    def accept(message: Message) -> None:
        logger.debug("Entered ReplyHandler")
        if isinstance(message, ReplyMessage):
            response.set_result(message)
            router.unsubscribe(topic, subscription_id)

    return accept


# TODO: Criar Type Alias AckHandler para Callable[[Message], None]
def ack_handler(timeout: Timer, topic: str, ack_subscription_id: str, router: Router) -> Callable:
    """AckHandler implementation for OpenDSB"""

    def accept(message: Message) -> None:
        logger.debug("Entered AckHandler")
        if not isinstance(message, ControlMessage) or message.control_message_type != ControlMessageType.CALL_ACK:
            return
        timeout.cancel()
        router.unsubscribe(topic, ack_subscription_id)

    return accept


class DefaultBusClient(BusClient):
    """DefaultBusClient implementation for OpenDSB"""

    def __init__(self, router: Router):
        self.timeout = 10
        self.router = router
        self.executor = ThreadPoolExecutor(max_workers=3)

    def subscribe(self, topic: str, handler: Callable) -> Subscription:
        """Subscribe to a topic"""
        sub_id = self.router.generate_subid()
        logger.debug(f'Subscribing "{sub_id}" to "{topic}"')
        return self.router.subscribe(topic, sub_id, handler)

    def unsubscribe(self, subscription: Subscription) -> None:
        """Cancel a subscription"""
        logger.debug(f'Unsubscribing "{subscription}"')
        self.router.unsubscribe(subscription.topic, subscription.id)

    def publish_data(self, topic: str, data: Serializable) -> None:
        """Publish data to a topic"""
        data_message = DataMessage(destination=topic, origin=self.router.id, data=data)
        logger.debug(f'Publishing data message: "{data_message}"')
        self.router.route_message(data_message, True)

    def publish_reply(self, topic: str, reply: Serializable) -> None:
        """Publish data to a topic"""
        reply_message = ReplyMessage(destination=topic, origin=self.router.id, reply=reply)
        logger.debug(f'Publishing reply message: "{reply_message}"')
        self.router.route_message(reply_message, True)

    def call_and_wait(self, topic: str, parameters: list[TypedData], timeout: float) -> Result:
        self.timeout = timeout
        response = self.call(topic, parameters)

        logger.info("Waiting response for Client Call...")

        try:
            call_result = response.result(timeout)
            result = Result(True, call_result)
            # data_dict = call_result.data['data']
            # DictValidator.validate(data_dict, schema, call_result.data['concreteType'])
            logger.debug(f'Client Call response: "{call_result}"'[:500])
        except TimeoutError as e:
            msg = f"TimeoutError: No Client Call response received. {e}"
            logger.warning(msg)
            result = Result(False, {}, msg)
        except Exception as e:
            msg = f"Exception: {e}"
            logger.warning(msg, exc_info=True)
            result = Result(False, {}, msg)

        return result

    def call(self, topic: str, parameters: list[TypedData]) -> Future:
        """Call a method"""
        logger.debug(f'Calling "{topic}" with parameters "{parameters}"')
        reply_to = f"reply-{self.router.generate_subid()}/{topic}"

        response = Future()
        reply_subscription_id = f"reply-{self.router.generate_subid()}"
        ack_subscription_id = f"ack-{self.router.generate_subid()}"

        self.router.subscribe(
            reply_to, reply_subscription_id, reply_handler(response, reply_to, reply_subscription_id, self.router)
        )

        # Impede uma mensagem de call para um topico no qual ninguem esta escutando
        timeout_task = Timer(
            self.timeout, self._timeout_task, [response, reply_to, reply_subscription_id, ack_subscription_id]
        )
        timeout_task.start()

        self.router.subscribe(
            reply_to, ack_subscription_id, ack_handler(timeout_task, reply_to, ack_subscription_id, self.router)
        )

        logger.debug("Creating CallMessage...")
        call_msg = CallMessage(destination=topic, origin=self.router.id, parameters=parameters, reply_to=reply_to)
        self.router.route_message(call_msg, True)

        return response

    def _timeout_task(self, future: Future, topic: str, reply_subscription_id: str, ack_subscription_id: str) -> None:
        logger.debug(f'Timeout task for "{topic}" has expired')
        self.router.unsubscribe(topic, reply_subscription_id)
        self.router.unsubscribe(topic, ack_subscription_id)
        future.set_exception(TimeoutError(f'Call to "{topic}" has timed out for "{reply_subscription_id}"'))

    def shutdown(self) -> None:
        """Shutdown the client"""
        logger.debug("Shutting down client...")
        self.router.disconnect_all_remote_peers(kill_peer=True)
        self.executor.shutdown()

    def __repr__(self) -> str:
        # return f'{self.__class__.__qualname__}(router={self.router}, timeout={self.timeout})'
        return f"{self.__class__.__qualname__}(router={self.router.id}, timeout={self.timeout})"
