
from abc import ABC
import json
from .basemessage import BaseMessage
from .message import MessageType


class TypedData(ABC):
    def __init__(self, data, concreteType: str) -> None:
        self.data = data
        self.concreteType = concreteType

class DefaultData(TypedData):
    def __init__(self, data: dict) -> None:
        super().__init__(data['payLoad'], 'org.opendsb.json.info.DefaultData')
        self.dataType = data['dataType']

    def to_dict(self):
        return {
            'data': self.data,
            'dataType': self.dataType,
            'concreteType': self.concreteType
        }

class TypedCollection(TypedData):
    def __init__(self, data: dict) -> None:
        super().__init__(data['payLoad'], 'org.opendsb.json.info.TypedCollection')
        self.collectionRawType = data['collectionRawType'] # tipo da colecao em si
        self.collectionGenericType = data['dataType'] # tipo do conteudo da colecao


class DataMessage(BaseMessage):
    def __init__(self, 
                 destination: str, 
                 origin: str, 
                 data: str | dict | list | int | float | bool | None,
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
        return cls(message_dict['destination'], message_dict['origin'], message_dict['data'], message_dict['id'], message_dict['latest_hop'])

    