import uuid

from opendsb.messaging.message import Message, MessageType


class BaseMessage(Message):
    def __init__(
        self, origin: str, destination: str, type: MessageType, id: str | None = None, latest_hop: str | None = None
    ):
        self.origin = origin
        self.destination = destination
        self.type = type
        self.id = id or str(uuid.uuid4())
        self.latest_hop = latest_hop or origin

    def __repr__(self) -> str:
        return f"{self.__class__.__qualname__}(origin={self.origin}, destination={self.destination}, type={self.type}, id={self.id})"
