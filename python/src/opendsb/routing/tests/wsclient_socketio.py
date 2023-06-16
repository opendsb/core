import socketio

sio = socketio.Client()

@sio.event
def connect():
    print('Conectado ao servidor')

@sio.event
def disconnect():
    print('Desconectado do servidor')

@sio.event
def mensagem(data):
    print('Mensagem recebida:', data)

#sio.connect('ws://0.0.0.0:8080/open-dsb/bus')  # Atualize com a URL do seu servidor Java
sio.connect('ws://localhost:8080/open-dsb/bus')  # Atualize com a URL do seu servidor Java

# Exemplo de envio de uma mensagem ao servidor
sio.emit('mensagem', 'Ola, servidor!')

sio.wait()

## Nao consigo conectar com o servidor Java