import asyncio
import json
import websockets

async def connect_to_server(uri: str, message: object):
    async with websockets.connect(uri) as websocket:
        # Enviar uma mensagem para o servidor
        await websocket.send(message)
        print("Mensagem enviada para o servidor:", message)

        # Receber a resposta do servidor
        response = await websocket.recv()
        print("Resposta recebida do servidor:", response)


if __name__ == '__main__':
    uri = 'ws://localhost:8080/open-dsb/bus'
    #data = json.dumps({'dataType': 'abc', 'data': 'Ola, servidor!'})
    data = {'dataType': 'abc', 'data': 'Ola, servidor!'}
    message = {'type': 'PUBLISH', 'data': data}
    message_json = json.dumps(message)
    asyncio.run(connect_to_server(uri, message_json))
