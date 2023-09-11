'''Busclient implementation for OpenDSB'''

from abc import ABC, abstractmethod
from concurrent.futures import Future
from typing import Callable, Any

from opendsb.messaging.datamessage import TypedData
from opendsb.client.subscription import Subscription


class Result:
    def __init__(self, success: bool, value: Any, reason: str='') -> None:
        self.success = success
        self.value = value
        self.reason = reason


class BusClient(ABC):
    '''BusClient abstract class'''

    @abstractmethod
    def subscribe(self, topic: str, handler: Callable) -> Subscription:
        '''Subscribe to a topic'''

    @abstractmethod
    def unsubscribe(self, subscription: Subscription) -> None:
        '''Cancel a subscription'''

    @abstractmethod
    def publish_data(self, topic: str, data: str) -> None:
        '''Publish data to a topic'''

    @abstractmethod
    def publish_reply(self, topic: str, reply: str) -> None:
        '''Publish data to a topic'''

    @abstractmethod
    def call(self, topic: str, parameters: list[TypedData]) -> Future:
        '''Call a method'''

    @abstractmethod
    def call_and_wait(self, topic: str, parameters: list[TypedData], timeout: float) -> Result:
        '''Call a method and wait for the response'''

