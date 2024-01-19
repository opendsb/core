import json

from opendsb.messaging.basemessage import BaseMessage
from opendsb.messaging.datamessage import TypedData
from opendsb.messaging.message import MessageType


class CallMessage(BaseMessage):
    def __init__(
        self,
        destination: str,
        origin: str,
        parameters: list[TypedData],
        reply_to: str,
        id: str | None = None,
        latest_hop: str | None = None,
    ):
        super().__init__(origin=origin, destination=destination, type=MessageType.CALL, id=id, latest_hop=latest_hop)
        assert isinstance(parameters, list), "Parameters must be a list"
        assert len(parameters) > 0, "Parameters must not be empty"
        assert all(isinstance(param, TypedData) for param in parameters), "Parameters must be TypedData"
        self.parameters = parameters
        self.reply_to = reply_to

    def __repr__(self) -> str:
        attributes = vars(self)
        formatted_attributes = [f"{var}={value}" for var, value in attributes.items()]
        return f"{type(self).__name__}({', '.join(formatted_attributes)})"

    @classmethod
    def from_json(cls, json_message: str):
        # message_dict = json.loads(json_message, object_hook=cls.flatten_hook)
        message_dict = json.loads(json_message)
        attr_map_inv = {v: k for k, v in cls.ATTR_MAP.items()}
        message_dict = {attr_map_inv[attr]: value for attr, value in message_dict.items()}
        return cls(
            message_dict["destination"],
            message_dict["origin"],
            message_dict["parameters"],
            message_dict["reply_to"],
            message_dict["id"],
            message_dict["latest_hop"],
        )
