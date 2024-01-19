from typing import Callable


class Subscription:
    """Subscription class

    It is responsible for storing the subscription information.

    Args:
        topic (str): The topic of the subscription

        handler (MessageHandler): The handler of the subscription

    Attributes:
        topic (str): The topic of the subscription.

        handler (MessageHandler): The handler of the subscription.

        id (str): The id of the subscription. It is generated as uuid4.
    """

    def __init__(self, topic: str, sub_id: str, handler: Callable):
        self.topic = topic
        self.handler = handler
        self.id = sub_id

    def __repr__(self) -> str:
        return f"Subscription(topic={self.topic}, handler={self.handler.__name__}, id={self.id})"
