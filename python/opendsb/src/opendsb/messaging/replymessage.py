
import json
from .datamessage import DataMessage
from .message import MessageType


class ReplyMessage(DataMessage):
    def __init__(self, 
                 destination: str, 
                 origin: str, 
                 reply: str | dict | list | int | float | bool | None, 
                 cause: str = '',
                 id: str | None = None,
                 latest_hop: str | None = None,
                 type: MessageType = MessageType.REPLY
        ):
        super().__init__(destination=destination, origin=origin, data=reply, id=id, latest_hop=latest_hop)
        self.successful = reply is not None
        self.type = type
        self.cause = cause

    def __repr__(self) -> str:
        return f'{self.__class__.__qualname__}(origin={self.origin}, destination={self.destination}, data={self.data}, cause={self.cause}, successful={self.successful}, type={self.type})'

    @classmethod
    def from_json(cls, json_message: str):
        message_dict = json.loads(json_message)
        ATTR_MAP_INV = {v: k for k, v in cls.ATTR_MAP.items()}
        message_dict = {ATTR_MAP_INV[attr]: value for attr, value in message_dict.items()}
        return cls(message_dict['destination'], message_dict['origin'], message_dict['data'], message_dict['cause'], message_dict['id'], message_dict['latest_hop'])
    