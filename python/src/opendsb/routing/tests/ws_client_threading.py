import json
from threading import Thread
import time
import websocket

#websocket.enableTrace(True)

class WebSocketClientThread(Thread):
    def __init__(self, server_uri):
        #Thread.__init__(self)
        super().__init__()
        self.server_uri = server_uri
        self.websocket = None
        self.is_connected = False

    def run(self):
        self.websocket = websocket.WebSocketApp(self.server_uri, 
                                                on_open=self.on_open,
                                                on_message=self.on_message
        )
        self.websocket.run_forever()

    def connect(self):
        self.start()

    def disconnect(self):
        self.websocket.close()

    def publish(self, message):
        while not self.is_connected:
            print('Waiting for connection...')
            time.sleep(1)  # Aguarda 0.1 segundos para conexão ser estabelecida

        self.websocket.send(message)

    def on_open(self, wsapp):
        self.is_connected = True

    def on_message(self, wsapp, message):
        print(f"Server response: {message}")
        

def main_thread():
    client = WebSocketClientThread('ws://localhost:8000')  # Substitua pelo URI do servidor WebSocket
    #client = WebSocketClientThread('ws://localhost:8080/open-dsb/bus')  # Substitua pelo URI do servidor WebSocket
    
    print('Starting client...')
    client.connect()
    #time.sleep(5)

    print('Publishing...')
    #client.publish('Hello, server!')
    data = {'dataType': 'abc', 'data': 'Ola, servidor!'}
    message = {'type': 'PUBLISH', 'data': data}
    message_json = json.dumps(message)
    client.publish(message_json)

    time.sleep(10)
    print('Stopping connection and thread...')
    client.disconnect()
    client.join()


if __name__ == '__main__':
    main_thread()
