import json
from enum import Enum

from opendsb.messaging.basemessage import BaseMessage
from opendsb.messaging.message import MessageType


class ControlMessageType(Enum):
    CONNECTION_REQUEST = "CONNECTION_REQUEST"
    CONNECTION_REPLY = "CONNECTION_REPLY"
    CALL_ACK = "CALL_ACK"
    UPDATE_ROUTE_COUNT = "UPDATE_ROUTE_COUNT"


class ControlTokens(Enum):
    CLIENT_ID = "clientId"
    SERVER_ID = "serverId"
    TRANSACTION_ID = "transactionId"
    ROUTING_TABLE_COUNT = "routingTableCount"
    TOPIC = "topic"


class ControlMessage(BaseMessage):
    def __init__(
        self,
        origin: str,
        destination: str,
        control_message_type: ControlMessageType,
        control_info: dict[ControlTokens, str] | None = None,
        id: str | None = None,
        latest_hop: str | None = None,
        type: MessageType = MessageType.CONTROL,
    ):
        super().__init__(origin=origin, destination=destination, type=type, id=id, latest_hop=latest_hop)
        self.control_message_type = control_message_type
        self.control_info = control_info or {}

    def __repr__(self) -> str:
        attributes = vars(self)
        formatted_attributes = [f"{var}={value}" for var, value in attributes.items()]
        return f"{type(self).__name__}({', '.join(formatted_attributes)})"

    @classmethod
    def from_json(cls, json_message: str):
        # message_dict = json.loads(json_message, object_hook=cls.flatten_hook)
        message_dict = cls.deep_decoder(json_message)
        attr_map_inv = {v: k for k, v in cls.ATTR_MAP.items()}
        message_dict = {attr_map_inv[attr]: value for attr, value in message_dict.items()}
        control_info = {ControlTokens(token): value for token, value in message_dict["control_info"].items()}
        return cls(
            message_dict["origin"],
            message_dict["destination"],
            ControlMessageType(message_dict["control_message_type"]),
            control_info,
            message_dict["id"],
            message_dict["latest_hop"],
        )

    def to_json(self) -> str:
        message_dict = vars(self).copy()
        message_dict["control_message_type"] = self.control_message_type.value
        message_dict["control_info"] = {token.value: value for token, value in self.control_info.items()}
        mapped_message = {
            ControlMessage.ATTR_MAP[attr]: ControlMessage.get_value(value) for attr, value in message_dict.items()
        }
        return json.dumps(mapped_message)

    # Java json messages


# {"controlMessageType":"CONNECTION_REQUEST","controlInfo":{"clientId":"Router_0cb10c6c-fb71-4445-87b6-8b568a21877a","routingTableCount":"{}","transactionId":"ConnectionRequest_1b766c16-db28-4bb4-afee-69619d230f98"},"messageId":"a8aab502-cbbe-4f4d-a26f-1636c76598e0","type":"CONTROL","origin":"Router_0cb10c6c-fb71-4445-87b6-8b568a21877a","destination":"control","latestHop":"Router_0cb10c6c-fb71-4445-87b6-8b568a21877a"}
# {"controlMessageType":"UPDATE_ROUTE_COUNT","controlInfo":{"routingTableCount":"{\"a/b/c\":1,\"reply-a384f9f1-9a3f-4d86-8ff4-afe8c7ffb846/c/getSample\":0}","transactionId":"1727f16b-7575-4f96-a650-be26db940a53"},"messageId":"ea95ac50-edbb-43ea-b393-311ed5b66c7d","type":"CONTROL","origin":"reply-a384f9f1-9a3f-4d86-8ff4-afe8c7ffb846/c/getSample@Router_c0768134-ea90-4193-a854-3ee4c06dc536","destination":"control","latestHop":"Router_c0768134-ea90-4193-a854-3ee4c06dc536"}
