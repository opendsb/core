

Messagehandler(Protocol):
    def accept(self, message: Message):
        ...


def process(message):
    print(message)


def execute():
    client
    client.subscribe('topic', lambda x: process(x))
    client.subscribe('topic', NamedTuple('MessageHandler', [('accept', Callable[[Message], None])]))


