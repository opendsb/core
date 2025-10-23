import queue
import time

from opendsb.client.busclient import BusClient, Result, Serializable
from opendsb.messaging.datamessage import DataMessage
from opendsb.messaging.datatypes import DefaultData, TypedData
from opendsb.messaging.replymessage import ReplyMessage
from opendsb.routing.defaultrouter import DefaultRouter

from opendsb.utils.logging_conf import configure_logger

logger = configure_logger("opendsb", "INFO", "CONSOLE")  # data_source_grouping usa opendsb

def connect_to_remote(
    server: str,
    port: int,
    error_queue: queue.Queue = None,
    application="api-v1",
    endpoint="somaServer",
) -> BusClient:
    address: str = f"ws://{server}:{port}/{application}/{endpoint}"
    logger.info(
        f"Method connect_to_remote() called: Connecting to remote server {address}",
    )
    router = DefaultRouter(error_queue)
    return router.connect_to_remote(address)


def disconnect_from_remote(bus_client: BusClient):
    logger.info('Disconnecting from remote...')
    bus_client.shutdown()


def main():
    error_queue = queue.Queue()
    logger.info(f"error_queue = {id(error_queue)}")
    SOMA_SERVER = "localhost"
    bus_client: BusClient = connect_to_remote(SOMA_SERVER, 8085, error_queue)
    logger.info(f"bus_client = {id(bus_client)}")


if __name__ == '__main__':
    main()
