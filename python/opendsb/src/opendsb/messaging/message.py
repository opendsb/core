# -*- coding: utf-8 -*-

from abc import ABC
from enum import Enum
import json

from opendsb.messaging.datatypes import TypedData
from opendsb.messaging.jsondecoder import deep_decoder


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
    def get_value(obj):
        if isinstance(obj, Enum):
            return obj.value
        elif isinstance(obj, list) and all(isinstance(item, TypedData) for item in obj):
            return [item.to_dict() for item in obj]
        else:
            return obj

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
        mapped_message = {
            Message.ATTR_MAP[attr]: Message.get_value(obj) for attr, obj in vars(self).items()
        }
        return json.dumps(mapped_message)
