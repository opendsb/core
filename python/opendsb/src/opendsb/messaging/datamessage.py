# -*- coding: utf-8 -*-

import json

from opendsb.messaging.basemessage import BaseMessage
from opendsb.messaging.datatypes import TypedData, dict2typeddata
from opendsb.messaging.message import MessageType


class DataMessage(BaseMessage):
    def __init__(self, 
                 destination: str, 
                 origin: str, 
                 data: TypedData,
                 id: str | None = None,
                 latest_hop: str | None = None,
                 type: MessageType = MessageType.PUBLISH
        ):
        super().__init__(origin=origin, destination=destination, type=type, id=id, latest_hop=latest_hop)
        self.data = data

    def __repr__(self) -> str:
        attributes = vars(self)
        formatted_attributes = [f'{var}={value}' for var, value in attributes.items()]
        return f"{type(self).__name__}({', '.join(formatted_attributes)})"
   
    @classmethod    
    def from_json(cls, json_message: str):
        #message_dict = json.loads(json_message, object_hook=cls.flatten_hook)
        message_dict = json.loads(json_message)
        ATTR_MAP_INV = {v: k for k, v in cls.ATTR_MAP.items()}
        message_dict = {ATTR_MAP_INV[attr]: value for attr, value in message_dict.items()}
        typed_data = dict2typeddata(message_dict['data'])
        return cls(message_dict['destination'], message_dict['origin'], typed_data, message_dict['id'], message_dict['latest_hop'])

    