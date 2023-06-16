from abc import ABC
from enum import Enum
import json

from .jsondecoder import deep_decoder


class MessageType(Enum):
    CONTROL = 'CONTROL'
    PUBLISH = 'PUBLISH'
    CALL = 'CALL'
    REPLY = 'REPLY'


class Message(ABC):
    """Base class for all messages.
    
    Attributes:
        origin: str
        destination: str
        type: MessageType
        id: str    
    """

    # Message keys mapping: Python attribute name -> Java json key
    ATTR_MAP = {
        'cause': 'cause',
        'control_message_type': 'controlMessageType',
        'control_info': 'controlInfo',
        'data': 'data',
        'destination': 'destination',
        'id': 'messageId',
        'latest_hop': 'latestHop',
        'origin': 'origin',
        'parameters': 'parameters',
        'reply_to': 'replyTo',
        'successful': 'successful',
        'type': 'type',
    }

    deep_decoder = deep_decoder

    @staticmethod
    def get_value(value):
        if isinstance(value, Enum):
            return value.value
        else:
            return value


    def __init__(self, 
                origin: str,
                destination: str,
                type: MessageType
        ) -> None:
        self.origin = origin
        self.destination = destination
        self.type = type
        self.id = ''
        self.latest_hop = ''

    def to_json(self) -> str:
        mapped_message = {Message.ATTR_MAP[attr] : Message.get_value(value) for attr, value in vars(self).items()}
        return json.dumps(mapped_message)
    
    



# Java json messages
#{"controlMessageType":"CONNECTION_REQUEST","controlInfo":{"clientId":"Router_0cb10c6c-fb71-4445-87b6-8b568a21877a","routingTableCount":"{}","transactionId":"ConnectionRequest_1b766c16-db28-4bb4-afee-69619d230f98"},"messageId":"a8aab502-cbbe-4f4d-a26f-1636c76598e0","type":"CONTROL","origin":"Router_0cb10c6c-fb71-4445-87b6-8b568a21877a","destination":"control","latestHop":"Router_0cb10c6c-fb71-4445-87b6-8b568a21877a"}
#{"controlMessageType":"UPDATE_ROUTE_COUNT","controlInfo":{"routingTableCount":"{\"a/b/c\":1,\"reply-a384f9f1-9a3f-4d86-8ff4-afe8c7ffb846/c/getSample\":0}","transactionId":"1727f16b-7575-4f96-a650-be26db940a53"},"messageId":"ea95ac50-edbb-43ea-b393-311ed5b66c7d","type":"CONTROL","origin":"reply-a384f9f1-9a3f-4d86-8ff4-afe8c7ffb846/c/getSample@Router_c0768134-ea90-4193-a854-3ee4c06dc536","destination":"control","latestHop":"Router_c0768134-ea90-4193-a854-3ee4c06dc536"}