

# OpenDSB Client

## Sequência simplificada de eventos

1 - Definir um objeto Router (DefaultRouter).

2 - Definir um objeto Cliente (DefaultBusClient) passando para ele o router criado.

3 - Criar um objeto MessageHandler (SampleHandler) passando para ele o client criado.

4 - Subscrever o cliente em determinado tópico ('A/B') e fornecer um objeto MessageHandler na qual o cliente esteja registrado.

5 - Fazer uma Call com o cliente para determinado tópico (tópicos subscritos) com determinados argumentos de entrada (dependendentes da call). Esta Call retorna um objeto assíncrono `Future`, cujo resultado pode ser buscado chamando o método `result` passando um `timeout` como argumento. O objeto retornado trará os dados da call no atributo `data`.

6 - Aguardar um tempo superior ao `timeout` do `result` para encerrar o programa permitindo que todas as threads finalizem suas tarefas.

7 - Fim do programa.

---

## Sequência detalhada de eventos

### 1 - Definir um objeto Router (DefaultRouter)

```python
router = DefaultRouter()
```




---

## Entidades fundamentais da arquitetura

### Subscription

```python
"""
    Args:
    	topic: str
    	handler: MessageHandler

    Atrributes:
        id: str uuid
        topic: str
        handler: MessageHandler
"""
```

### Client
É uma interface que implementa 

---

## Messages

### message.py
Define a classe abstrata básica `Message` e o classe básica de tipos de mensagem `MessageType`.

```python
class MessageType(Enum):
    CONTROL = 1
    PUBLISH = 2
    CALL = 3
    REPLY = 4


@dataclass
class Message(ABC):
    id: str
    origin: str
    destination: str | None
    message_type: MessageType | None

```

### basemessage.py
Estende a classe `Message`. Define o construtor base para as demais mensagens, gera o `id` (uuid) e demais atributos do objeto. Inclui o atributo `latest_hop`, que não está presente na classe `Message`.


### callmessage.py
Estende a classe `BaseMessage`, é marcado como `MessageType.CALL` e cria dois novos atributos: `parameters` e `reply_to`.

### controlmessage.py
Estende a classe `BaseMessage`, é marcado como `MessageType.CONTROL` e cria dois novos atributos: `control_message_type` e `control_info`. O `control_message_type` é uma classe Enum que define o tipo de mensagem de controle.

```python

class ControlMessageType(Enum):
    CONNECTION_REQUEST = 1
    CONNECTION_REPLY = 2
    CALL_ACK = 3
    UPDATE_ROUTE_COUNT = 4

```

### datamessage.py
Estende a classe `BaseMessage`, é marcado como `MessageType.PUBLISH` e cria um novo atributo: `data`. 


### replymessage.py
Estende a classe `DataMessage`, é marcado como `MessageType.REPLY` e cria dois novos atributo: `successful` e `cause`. O primeiro significa que o objeto da classe recebeu um valor diferente de `None` como `reply`, que é passado para o atributo `data`. O segundo é recebido como argumento do construtor.


----

# Funcionamento de uma CALL

Como funciona uma `DefaultBusClient.call`:
- Os argumentos de uma `call` consistem em um tópico (`topic: str`) e uma lista de parametros (`parameters: list[str]`);
- O retorno de uma `call` é um `Future`;
- O método é acionado através de uma chamada com retorno para uma variável: `response = client.call(topic, parameters)`
- Onde o objeto `client` (classe `DefaultBusClient`) deve ser previamente instanciado e possuir uma subscrição (handler associado a um tópico)
- A execução da `call` se dá da seguinte maneira:
    - 1 Cria um novo tópico de reply com a seguinte sintaxe `reply-{uuid}/{topic}`;
    - 2 Instancia um `ReplyHandler` que recebe como argumento a resposta futura da call (`response`);
    - 3 Cria uma subscrição para o `ReplyHandler` no topico de reply criado no passo 1;
    - 4 Passa o objeto `Subscription` criado e o objeto do próprio cliente (`client`) através de composição para a instância do `ReplyHandler`;
    - 5 Lança (`submit`) na ThreadPool uma `timeout_task` para setar um `TimeoutError` como resposta futura da call (`response`) caso ultrapasse um tempo de processamento previamente esperado;
    - 6 Instancia um `AckHandler` que recebe como argumento essa `timeout_task`;
    - 7 Cria uma subscrição para o `AckHandler` no topico de reply criado no passo 1;
    - 8 Passa o objeto `Subscription` criado e o objeto do próprio cliente (`client`) através de composição para a instância do `AckHandler`;
    - 9 Cria uma `CallMessage` passando o tópico (`topic`) como destino, os parametros, o tópico de reply e a id do roteador do cliente que chamou a call (representa a origem da mensagem);
    - 10 Roteia (`route_message`) a CallMessage.

Detalhes:
- 3 Cria uma subscrição para o `ReplyHandler` no topico de reply criado no passo 1;
    - 3.1 Chama o `subscribe` do `DefaultRouter`
        - 3.1.1 Verifica se o tópico já existe na `routing_table` e pega o nó (`RouteNode`) associado. Se não existe, cria;
        - 3.1.2 Chama o método `subcribe` do `RouteNode` e passa o handler (`ReplyHandler`) para criar uma subscrição;
    - OBS: a subscricao do `AckHandler` funciona da mesma maneira

- 10 Roteia (`route_message`) a CallMessage
    - 10.1 Chama o `route_message` do `DefaultRouter` passando a `CallMessage` como argumento;
        10.1.1 Lança (`submit`) na ThreadPool uma `routing_task` para processa a `CallMessage` e enviar aos destinos possíveis (`/`) caso haja subscrições resgistradas para aquele tópico;
        10.1.2 O processamento da menssagem acontece no `execute_routing`




# Dúvidas

Tenho um objeto `handler` e quero chamar desse objeto o método `accept` com argumentos sendo o `self` os argumentos da função (`message`).

```python 
if hasattr(Classe, mgs.cmd):
    func = getattr(Classe, mgs.cmd)
    answer = func(msg.data)
    return answer 

##### 

from plugin_pool import *

class MyHandler:

    def accept(self, method: str):
        for 
        if hasattr(Plugin, method):
            func = getattr(Plugin, method)
            answer = func(msg.data)
            return answer 
```


# TODO:
- Validar metodologia passando handlers do tipo function
- Criar métodos nos clientes e roteador para criar e destruir subscrições passando o id
- No roteador:
 - mudar o metodo de inscricao para incluir o id da inscricao
 - mudar o metodo de desistencia da inscricao para incluir o id e o topico ao inves do objeto suubscription
 - criar um metodo para gerar ids de inscricao
- No routenode:
 - mudar o metodo de inscricao para incluir o id da inscricao
 - mudar o metodo de desistencia da inscricao para incluir o id da inscricao ao inves do objeto suubscription
- No busclient:
 - mudar a implementacao dos metodos de incricao e de chamada levando em conta as mudancas no roteador e routenode

# Rodar servidor de testes OpenDSB
## Caminho da imagem
```shell
cd /home/rafa/Documents/trabalho/cepel/Sapiens/server-opendsb
```
## Importar para as imagens (só precisa rodar 1x)
```shell
docker load -i poc-bus-service-image.tar.gz
```
## Rodar a imagem
```shell
docker run -it --rm -p 8080:8080 draf/poc-bus-service:1.0.0-SNAPSHOT
```




## Teste:
- Comentar a linha 82 do roteador (`self.route_message(ack)`) e fazer com que o programa continue funcionando ok




## Conexão websocket

Dois roteadores conversando 
Existe uma maneira do seu roteador local saber o que o outro está esperando
Eles trocam tabelas de roteamento
Os roteadores, além terem as tabelas e metodos, podem mapear os vizinhos tbm
Esse mapeamento é um dict de topicos e quantos listeners estão subscritos nele

A ideia é ter um objeto do meu lado que represente o lado remoto com infos mais menos atualizadas para tomar decisoes corretas, como enviar a mensagem pela rede ou nao (a depender se ha listeners la do outro lado)

Preciso de uma funcionalidade de conexão desconexão com vizinhos

### testes
- DefaultRouter.RemotePeerconnection ()
- RoutingRemote.RemotePeerconnection
- Tentar implementar uma conexao com o remoto e se possivel um disconnetc tbm
- threadsafe nos dicionarios
