"""Busclient implementation for OpenDSB"""

from abc import ABC, abstractmethod
from concurrent.futures import Future
from typing import Any, Callable, Type, Union

from opendsb.client.subscription import Subscription
from opendsb.messaging.datamessage import TypedData

Serializable = Union[str, dict, list, int, float, bool, None]


class Result:
    def __init__(self, success: bool, value: Any, reason: str = "") -> None:
        self.success = success
        self.value = value
        self.reason = reason


class BusClient(ABC):
    """BusClient abstract class"""

    @abstractmethod
    def subscribe(self, topic: str, handler: Callable) -> Subscription:
        """Subscribe to a topic"""

    @abstractmethod
    def unsubscribe(self, subscription: Subscription) -> None:
        """Cancel a subscription"""

    @abstractmethod
    def publish_data(self, topic: str, data: Serializable) -> None:
        """Publish data to a topic"""

    @abstractmethod
    def publish_reply(self, topic: str, reply: Serializable) -> None:
        """Publish data to a topic"""

    @abstractmethod
    def call(self, topic: str, parameters: list[TypedData]) -> Future:
        """Call a method"""

    @abstractmethod
    def call_and_wait(self, topic: str, parameters: list[TypedData], timeout: float) -> Result:
        """Call a method and wait for the response"""

    @abstractmethod
    def shutdown(self) -> None:
        """Shutdown the bus client"""

    # TODO: Avaliar se é possivel criar um Defalt Client mais significativo
    @classmethod
    def default(cls) -> type["BusClient"]:
        """
        Retorna a instância padrão (o Null Object) do cliente de barramento.
        """
        return NullBusClient()

class NullBusClient(BusClient):
    """Implementação do Null Object: não faz nada."""

    def subscribe(self, topic: str, handler: Callable) -> Subscription:
        """Subscribe to a topic"""
        return None

    def unsubscribe(self, subscription: Subscription) -> None:
        """Cancel a subscription"""
        pass

    def publish_data(self, topic: str, data: Serializable) -> None:
        """Publish data to a topic"""
        pass

    def publish_reply(self, topic: str, reply: Serializable) -> None:
        """Publish data to a topic"""
        pass

    def call(self, topic: str, parameters: list[TypedData]) -> Future:
        """Call a method"""
        return None

    def call_and_wait(self, topic: str, parameters: list[TypedData], timeout: float) -> Result:
        """Call a method and wait for the response"""
        return None

    def shutdown(self) -> None:
        """Shutdown the bus client"""
        pass

    def __repr__(self) -> str:
        """
        Retorna uma string inequívoca sobre o objeto.
        """
        # Formato recomendado: NomeDaClasse(chave=valor, ...)
        return "NullBusClient()"

    def __str__(self) -> str:
        """
        Retorna uma string amigável.
        """
        return "NullBusClient()"

def main():
    my_bus_client = BusClient.default()
    print(f"my_bus_client = {my_bus_client}")

if __name__ == '__main__':
    main()
