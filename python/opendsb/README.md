# Opendsb

[![PyPI - Version](https://img.shields.io/pypi/v/opendsb.svg)](https://pypi.org/project/opendsb)
[![PyPI - Python Version](https://img.shields.io/pypi/pyversions/opendsb.svg)](https://pypi.org/project/opendsb)

---

**Table of Contents**

- [Installation](#installation)
- [Build](#build)
- [Install](#install)
- [Package Publishing](#package-publishing)
- [Tests](#tests)
- [License](#license)

## Installation

```console
python3 -m pip install -e .

# Verifying installation
python3 -m pip list | grep opendsb

# Build package
python3 -m build
# Now, you can install in another module using dist/opendsb-2.1.3.tar.gz
```

## Build using hatch

```console
hatch clean ; hatch build
hatch config show | grep 'data ='
hatch config show | grep 'data =' | awk '{ print $3  $4}'
tree -L 4 $HOME/.local/share/hatch/env/virtual/opendsb
```

## Package Publishing

```console
hatch publish
```

## Tests

A suíte de testes vive em `tests/` e usa **pytest**. Não depende de rede externa nem de um
servidor SOMA: os testes de integração sobem um servidor WebSocket local, em `127.0.0.1`,
numa porta efêmera.

### Como executar

```console
# Via hatch (ambiente declarado no pyproject.toml)
hatch run test

# Ou diretamente, com o pacote instalado em modo editável
python3 -m pip install -e .
python3 -m pytest

# Um arquivo específico, com saída detalhada
python3 -m pytest tests/test_remote_peer_routing.py -v

# Com relatório de cobertura
hatch run cov
```

A suíte completa roda em torno de 16 segundos. A maior parte desse tempo está nos testes de
integração, que aguardam o handshake real de conexões WebSocket.

### O que cada arquivo cobre

| Arquivo                       | Cobertura                                                                                                                                                      |
| ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `test_websocket_peer.py`      | Ciclo de vida da conexão: isolamento entre peers, thread daemon dedicada, ordem de atribuição do `kill_peer` e construção da exceção `WebSocketConnectionLost` |
| `test_remote_peer_routing.py` | Filtro de interesse do peer remoto (regra de prefixos), visibilidade das mensagens descartadas e log do handshake de barramento                                |
| `test_router_connection.py`   | Estabelecimento de conexão pelo `DefaultRouter`, propagação de erros pela `error_queue` e integração contra um peer que não responde                           |

O foco está no **caminho de encerramento e de falha** da conexão — historicamente o menos
exercitado e onde os defeitos mais duradouros se acumularam.

### As fixtures e por que elas existem

As fixtures estão em `tests/conftest.py`. Não são conveniência: cada uma resolve um problema
concreto que, sem ela, torna a suíte instável ou inutilizável.

#### `no_auto_reconnect` — autouse, e sem ela a suíte **trava**

Esta é a fixture mais importante do conjunto. Ela é `autouse=True`, ou seja, aplica-se a
todos os testes automaticamente.

Quando a conexão cai, `RemotePeer.on_error()` chama `reconnect()`, que **dorme e tenta de
novo indefinidamente**, com backoff, sem nunca desistir. Num teste que provoca
`ConnectionRefusedError` — ou que simplesmente fecha o servidor no teardown — essas threads
de reconexão continuam vivas depois que o teste termina. O processo do pytest nunca encerra:
a suíte fica pendurada para sempre, sem mensagem de erro.

A fixture substitui `RemotePeer.reconnect` por uma função inócua durante os testes:

```python
@pytest.fixture(autouse=True)
def no_auto_reconnect(monkeypatch):
    reconnect_calls = []
    monkeypatch.setattr(RemotePeer, "reconnect", lambda self: reconnect_calls.append(self))
    return reconnect_calls
```

Os testes de erro de conexão verificam o **tratamento** do erro, não a política de
retentativa — a fixture os isola dela. Um teste que queira exercitar a reconexão deve
sobrescrever `reconnect` explicitamente, de forma controlada.

> Se um novo teste travar a suíte, o primeiro suspeito é uma conexão que escapou desta
> fixture.

#### `silent_ws_server` — reproduz a falha silenciosa

Sobe um servidor que **aceita o upgrade WebSocket** (responde `HTTP 101 Switching
Protocols`) e depois **fica mudo**: recebe o `CONNECTION_REQUEST` do OpenDSB e nunca
responde.

É o cenário mais difícil de diagnosticar em produção, porque nenhuma camada acusa erro. O
socket fica de pé, o `wire_connected` vira `True`, e as chamadas simplesmente nunca
respondem — o sintoma é apenas um timeout sem causa aparente. Ele acontece quando o
endereço é atendido por um proxy ou ingress intermediário que encaminha o handshake HTTP
mas não fala o protocolo do barramento.

Dois detalhes do teardown importam:

- `close()` fecha **também as conexões já aceitas**, não apenas o socket de escuta. Fechar
  só o socket de escuta deixaria as conexões estabelecidas vivas e, com elas, os
  `run_forever()` do lado cliente.
- O servidor escuta numa **porta efêmera** (`bind` na porta 0), então testes em paralelo ou
  execuções simultâneas não colidem.

#### `closed_port` — falha de conexão determinística

Reserva uma porta, descobre seu número e a fecha imediatamente. Garante um endereço onde
**ninguém** está escutando, para exercitar o caminho de `ConnectionRefusedError` sem
depender de suposições sobre o ambiente (uma porta "provavelmente livre" pode não estar).

#### `router` e `peer` — instâncias limpas

Fornecem um `DefaultRouter` e um `WebSocketPeer` novos a cada teste. Ambos guardam estado
mutável (tabela de rotas, peers conectados, flags de conexão); reaproveitá-los entre testes
faria a ordem de execução importar.

#### Helpers de mensagens de controle

`make_connection_reply()` e `make_route_count_update()` montam as `ControlMessage` que o
servidor envia no handshake e nas atualizações de rota. Concentrar a construção num único
lugar evita que cada teste replique a estrutura de `control_info` — que é onde estão os
tokens `TRANSACTION_ID`, `SERVER_ID` e `ROUTING_TABLE_COUNT`.

`FakeMessage` é uma mensagem mínima: `send_message()` só olha para o destino, então não há
motivo para construir uma `CallMessage` completa.

### Escrevendo novos testes

- Se o teste abre uma conexão real, verifique se o servidor é fechado no teardown e se a
  fixture `no_auto_reconnect` está ativa.
- Para verificar o que foi registrado em log, use a fixture `caplog` do pytest com
  `caplog.set_level(logging.DEBUG, logger="opendsb")`. Vários comportamentos deste pacote
  **só se manifestam no log** — uma mensagem descartada por ausência de assinante remoto,
  por exemplo, não gera erro nem exceção.
- Para verificar o que foi efetivamente transmitido, substitua `wire_send_message` do peer
  por um espião, em vez de inspecionar o socket.

## License

`opendsb` is distributed under the terms of the [MIT](https://spdx.org/licenses/MIT.html) license.
