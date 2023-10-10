# -*- coding: utf-8 -*-

from abc import ABC, abstractmethod, abstractproperty
from typing import Callable
import uuid

from opendsb.client.subscription import Subscription
from opendsb.messaging.message import Message


class Router(ABC):
    """Abstract class for routers"""
    def __init__(self) -> None:
        self.id: str = self.generate_subid()
    
    @staticmethod
    def generate_subid() -> str:
        return str(uuid.uuid4())
    
    @abstractproperty
    def full_subscription_count(self) -> dict:
        pass
    
    @abstractmethod
    def subscribe(self, topic: str, subscription_id: str, handler: Callable) -> Subscription:
        pass

    @abstractmethod
    def unsubscribe(self, topic: str, subscription_id: str) -> None:
        pass

    @abstractmethod
    def route_message(self, message: Message, remote: bool = False) -> None:
        pass
