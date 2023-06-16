import asyncio
import websockets

async def server_handler(websocket, path):
    print("Conexão estabelecida com o cliente")
    while True:
        data = await websocket.recv()
        print(f"Mensagem recebida do cliente: {data}")

        if data == "tag_request":
            # Simula a obtenção da tag do servidor
            tag = "ABC123"
            await websocket.send(tag)
        else:
            # Simula o processamento da mensagem pelo servidor
            await asyncio.sleep(1)  # Simula um processamento de 1 segundo
            response = f"Mensagem recebida e processada '{data}'"
            print(f"Enviando resposta: '{response}'")
            await websocket.send(response)

#start_server = websockets.serve(server_handler, "localhost", 8000)

async def main():
    async with websockets.serve(server_handler, "localhost", 8000) as server:
        print("Servidor iniciado em ws://localhost:8000")
        await server.serve_forever()


if __name__ == "__main__":
    asyncio.run(main())
