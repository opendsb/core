'''Busclient implementation for OpenDSB'''

from abc import ABC, abstractmethod
from concurrent.futures import Future
from typing import Callable

from .subscription import Subscription



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
    def call(self, topic: str, parameters: list[str]) -> Future:
        '''Call a method'''

