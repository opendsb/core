# -*- coding: utf-8 -*-

from abc import ABC, abstractmethod, abstractclassmethod
from typing import Any


class TypedData(ABC):
    def __init__(self, data: Any, concreteType: str) -> None:
        self.data = data
        self.concreteType = concreteType

    @abstractmethod
    def to_dict(self) -> dict:
        pass

    @abstractclassmethod
    def from_dict(cls, dct: dict):
        pass

class DefaultData(TypedData):
    '''DefaultData
    
    Args
    -----
    data: data content
    dataType: data content JAVA type
    '''

    def __init__(self, data: Any, dataType: str) -> None:
        super().__init__(data, 'org.opendsb.json.info.DefaultData')
        self.dataType = dataType

    def to_dict(self) -> dict:
        return {
            'data': self.data,
            'dataType': self.dataType,
            'concreteType': self.concreteType
        }
    
    @classmethod
    def from_dict(cls, dct: dict):
        return cls(dct['data'], dct['dataType'])
    
    def __str__(self) -> str:
        return f'{self.to_dict()}'
    
    def __dict__(self) -> dict:
        return self.to_dict()
    

class TypedCollection(TypedData):
    def __init__(self, data: Any, collectionGenericType: str, collectionRawType: str) -> None:
        '''TypedCollection

        Args
        -----
        data: collection content
        collectionGenericType: JAVA type of the collection items 
        collectionRawType: JAVA type of the collection itself
        '''
        super().__init__(data, 'org.opendsb.json.info.TypedCollection')
        self.collectionRawType = collectionRawType # tipo da colecao em si
        self.collectionGenericType = collectionGenericType # tipo do conteudo da colecao
    
    def to_dict(self) -> dict:
        return {
            'data': self.data,
            'concreteType': self.concreteType,
            'collectionRawType': self.collectionRawType,
            'collectionGenericType': self.collectionGenericType
        }
    
    @classmethod
    def from_dict(cls, dct: dict):
        return cls(dct['data'], dct['collectionGenericType'], dct['collectionRawType'])
    
    def __str__(self) -> str:
        return f'{self.to_dict()}'
    
    def __dict__(self) -> dict:
        return self.to_dict()


def dict2typeddata(dct: dict) -> TypedData:
    '''dict2typeddata

    Args
    -----
    dct: dict with TypedData attributes

    Returns
    -----
    TypedData object
    '''
    concrete_type = dct.get['concreteType']
    if not concrete_type:
        raise ValueError('Json object is not valid a TypedData. Attribute "concreteType" is required.')
    
    if not isinstance(concrete_type, str):
        raise TypeError('Json object is not valid a TypedData. Attribute "concreteType" must be a string.')

    classname = concrete_type.split('.')[-1]
    typed_data_class = getattr(sys.modules[__name__], classname)
    return typed_data_class.from_dict(dct)
