import asyncio
import threading
import time
from typing import Any, Protocol
import websockets
import logging

logger = logging.getLogger('websockets')
logger.setLevel(logging.DEBUG)
logger.addHandler(logging.FileHandler('websockets.log', mode='w'))


class Websocket(Protocol):
    def send(self, message:str):
        ...

    def recv(self):
        ...


class WebsocketClient:
    def __init__(self, server_uri:str):
        self.server_uri: str = server_uri
        self.response: Any = None
        self.connected: bool = False
        self.websocket = None
        self.check_connection_interval: int = 1  # Intervalo de tempo entre as verificações de conexão
        self.break_connection = threading.Event()
        self.connection_thread = threading.Thread(target=self.run_connect, args=())

    def start(self):
        self.connection_thread.start()

    # Funçao que cancela o comando `asyncio.run()`
    def stop(self):
        print('Disconnecting from server...', end=' ')
        self.disconnect()
        self.connection_thread.join()
        time.sleep(self.check_connection_interval + 2)
        print('Done')

        print('Stopping asyncio loop...', end=' ')
        #asyncio.get_running_loop().stop()
        print('Done')

    def run_connect(self):
        asyncio.run(self.connect())

#    async def connect(self):
#        async with websockets.connect(self.server_uri) as websocket:
#            self.connected = True
#            self.websocket = websocket
#
#            while self.connected:  # Executar enquanto a conexão estiver ativa
#                await asyncio.sleep(self.check_connection_interval)

#    def disconnect(self):
#        self.connected = False  # Desativar a flag de conexão

    async def connect(self):
        self.break_connection.clear()
        stop = asyncio.get_event_loop().run_in_executor(None, self.break_connection.wait)
        
        async with websockets.connect(self.server_uri) as self.websocket:
            self.connected = True
            await stop  # run until

    def disconnect(self):
        if self.connected:
            self.connected = False
            self.break_connection.set()

    def request(self, message:str) -> None:
        async def request_async(message:str) -> None:
            if self.websocket is None:
                print("Client is not connected. Call `start()` method to connect.")
                return None
            await self.websocket.send(message)
            async for message in self.websocket:
                self.response = await message
            #self.response = await self.websocket.recv()
                print(f"Server request response: {self.response}")
        asyncio.run(request_async(message))

    def publish(self, message:str) -> None:
        async def publish_async(message:str) -> None:
            if self.websocket is None:
                print("Client is not connected. Call `start()` method to connect.")
                return None
            await self.websocket.send(message)
            ack = await self.websocket.recv()
            print(f"Server ack: {ack}")        
        asyncio.run(publish_async(message))


# Função que controla a execução do script
def main():
    print('Starting client...')
    client = WebsocketClient("ws://localhost:8000")
    client.start()
    time.sleep(3)

    print('Requesting...')
    client.request("Requesting tag")
    time.sleep(3)
    
    print('Publishing...')
    message = f"Publishing dados da {client.response}"
    client.publish(message)
    
    time.sleep(5)
    print('Stopping...')
    client.stop()

# Chamar a função main para iniciar o script
if __name__ == "__main__":
    main()
