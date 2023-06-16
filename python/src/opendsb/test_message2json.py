# rodar no diretorio opendsb

import json

from messaging.datamessage import DataMessage
from messaging.callmessage import CallMessage
from messaging.replymessage import ReplyMessage
from messaging.controlmessage import ControlMessage, ControlMessageType, ControlTokens
from messaging.message import Message

datamessage = DataMessage(destination='destination', origin='origin', data={'a': 1, 'b': 2})
callmessage = CallMessage(destination='destination', origin='origin', parameters=[1, 'a'], reply_to='call_reply_to')
replymessage = ReplyMessage(destination='destination', origin='origin', reply='reply', cause='cause')
controlmessage = ControlMessage(origin='origin', destination='destination', control_message_type=ControlMessageType.CONNECTION_REQUEST, control_info={ControlTokens.TRANSACTION_ID: 'transaction_id', ControlTokens.CLIENT_ID: 'client_id', ControlTokens.ROUTING_TABLE_COUNT: {'topic1': 10, 'topic2': 5}})

datamessage_jsonenc = datamessage.to_json()
callmessage_jsonenc = callmessage.to_json()
replymessage_jsonenc = replymessage.to_json()
controlmessage_jsonenc = controlmessage.to_json()

# Testing json2message

#{"origin": "origin", "destination": "destination", "data": {"a": 1, "b": 2}, "type": "PUBLISH", "latest_hop": "origin", "id": "6268bacf-822c-4834-ac06-9baff48d55b2"}
#{"origin": "origin", "destination": "destination", "parameters": [1, "a"], "reply_to": "call_reply_to", "type": "CALL", "latest_hop": "origin", "id": "d251df6a-7135-4d74-b5be-375c6328ad16"}
#{"origin": "origin", "destination": "destination", "data": "reply", "cause": "cause", "successful": true, "type": "REPLY", "latest_hop": "origin", "id": "de8d5576-9d4f-411c-8eac-dea3e60f4aca"}
#{"origin": "origin", "destination": "destination", "control_message_type": "CONNECTION_REQUEST", "control_info": {"transactionId": "transaction_id", "clientId": "client_id", "routingTableCount": {"topic1": 10, "topic2": 5}}, "type": "CONTROL", "latest_hop": "origin", "id": "374501ae-4565-4738-a983-d00f11e5586f"}

datamessage_jsondec = json.loads(datamessage.to_json())
callmessage_jsondec = json.loads(callmessage.to_json())
replymessage_jsondec = json.loads(replymessage.to_json())
controlmessage_jsondec = json.loads(controlmessage.to_json())

def decode_message(message_json) -> Message:
    message_dict = json.loads(message_json)
    
    if message_dict['type'] == 'PUBLISH':
        return DataMessage(
            destination=message_dict['destination'], 
            origin=message_dict['origin'], 
            data=message_dict['data']
        )
    
    if message_dict['type'] == 'CALL':
        return CallMessage(
            destination=message_dict['destination'], 
            origin=message_dict['origin'], 
            parameters=message_dict['parameters'], 
            reply_to=message_dict['reply_to']
        )
    
    if message_dict['type'] == 'REPLY':
        return ReplyMessage(
            destination=message_dict['destination'], 
            origin=message_dict['origin'], 
            reply=message_dict['data'], 
            cause=message_dict['cause'], 
            successful=message_dict['successful']
        )
    
    if message_dict['type'] == 'CONTROL':
        return ControlMessage(
            origin=message_dict['origin'], 
            destination=message_dict['destination'], 
            control_message_type=message_dict['control_message_type'], 
            control_info=message_dict['control_info']
        )


pause