# -*- coding: utf-8 -*-

#from logging.handlers import RotatingFileHandler
import logging
import random
import time

from opendsb.messaging.callmessage import CallMessage
from opendsb.client.defaultbusclient import DefaultBusClient
from opendsb.routing.defaultrouter import DefaultRouter
from opendsb.messaging.datatypes import DefaultData


def initialize_logger():
    logger = logging.getLogger('opendsb')
    logger.setLevel(logging.DEBUG)
    #logger_handler = RotatingFileHandler('opendsb.log', maxBytes=1_000_000, backupCount=0)
    logger_handler = logging.FileHandler('opendsb.log', mode='w')
    #logger_handler.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(filename)s - %(funcName)s | %(message)s")) #"%(asctime)s - %(name)s - %(levelname)s - %(filename)s:%(funcName)s(%(lineno)d) - %(message)s"
    logger_handler.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(threadName)s - %(filename)s - %(funcName)s | %(message)s"))
    logger.handlers.clear()
    logger.addHandler(logger_handler)
    return logger


def local_test_case():
    '''DEPRECATED'''

    logger.info('Starting OpenDSB...')

    logger.info('Creating router...')
    router = DefaultRouter()

    logger.info('Creating client...')
    client = DefaultBusClient(router)

    def custom_reply_handler(message):
        if not isinstance(message, CallMessage):
            return None
        time.sleep(12)
        client.publish_reply(message.reply_to, 'Results Received!')

    logger.info('Subscribing client to topic...')
    #subscription = client.subscribe('A/B', lambda m: custom_reply_handler(m)) # lambda message: print(message.data)
    subscription = client.subscribe('A/B', custom_reply_handler) # lambda message: print(message.data)

    logger.info('Publishing data to subscribed topics...')
    client.publish_data('A/B', 'Hello World!')
    #client.publish_data('C', 'Hello New World!')

    logger.info('Making a Call wih parameters...')
    response = client.call('A/B', [DefaultData('Client call param 1', 'java.lang.String'), DefaultData('Client call param 2', 'java.lang.String')])
    #response = client.call('C', [DefaultData('Hello World!', 'java.lang.String')])

    logger.info('Waiting response for Client Call...')

    try:
        call_result = response.result(15) #.data
        logger.info(f'Client Call response: "{call_result}"')
    except TimeoutError as e:
        logger.warning(f'TimeoutError: No Client Call response received. {e}')
    except Exception as e:
        logger.warning(f'Exception: {e}')

    sleep = 20
    logger.info(f'Sleeping for {sleep} seconds to wait complete execution...')
    for i in range(sleep):
        logger.info(f'{sleep - i} seconds remaining...')
        time.sleep(1)

    logger.info(f'Routing table: {router.routing_table}')

    logger.info('OpenDSB finished successfully!')


def remote_test_case():
    '''DEPRECATED'''
    logger = logging.getLogger(__name__)

    logger.info('Starting OpenDSB...')

    logger.info('Creating router...')
    router = DefaultRouter()

    logger.info('Creating client...')
    client = DefaultBusClient(router)

    address = 'ws://localhost:8080/open-dsb/bus'
    logger.info(f'Connecting to remote peer at "{address}"...')
    router.connect_to_remote_router(address)


    logger.info('DEBUGRAFA: Executando sem sleep')

#    response = client.call('c/getSample', []) # a lista nao pode ser vazia
#
#    logger.info('Waiting response for Client Call...')
#
#    try:
#        call_result = response.result(15) #.data
#        data_dict = call_result.data['data']
#        #DICT_SCHEMAS = {'br.cepel.amp.vo.SampleVO': ['name', 'description', 'qualityIndex']}
#        #data_dict.pop('description')
#        schema = ['name', 'description', 'qualityIndex']
#        DictValidator.validate(data_dict, schema, call_result.data['dataType'])
#        logger.info(f'Client Call response: "{data_dict}"')
#    except TimeoutError as e:
#        logger.warning(f'TimeoutError: No Client Call response received. {e}')
#    except Exception as e:
#        logger.warning(f'Exception: {e}')
#
#    time.sleep(2)

    payload = {'foo': 'Hello World!'}
    data = {'payLoad': payload, 'dataType': 'java.util.HashMap'} # java.util.List, java.util.Set
    default_data = DefaultData(data)
    client.publish_data('a/b', default_data.to_dict())


    while True:
        time.sleep(2)


def remote_test_case_continuous():
    '''DEPRECATED'''
    logger = logging.getLogger(__name__)

    logger.info('Starting OpenDSB...')

    logger.info('Creating router...')
    router = DefaultRouter()

    logger.info('Creating client...')
    client = DefaultBusClient(router)

    address = 'ws://localhost:8080/open-dsb/bus'
    logger.info(f'Connecting to remote peer at "{address}"...')
    router.connect_to_remote_router(address)

    while True:
        payload = {'foo': str(random.randint(0, 100))}
        data = {'payLoad': payload, 'dataType': 'java.util.HashMap'} # java.util.List, java.util.Set
        default_data = DefaultData(data)
        client.publish_data('a/b', default_data.to_dict())
        time.sleep(10)


def remote_test_case_callandwait():
    '''DEPRECATED'''

    logger = logging.getLogger(__name__)

    logger.info('Starting OpenDSB...')

    logger.info('Creating router...')
    router = DefaultRouter()

    logger.info('Creating client...')
    client = DefaultBusClient(router)

    address = 'ws://localhost:8080/open-dsb/bus'
    logger.info(f'Connecting to remote peer at "{address}"...')
    router.connect_to_remote_router(address)

    time.sleep(2)

    #schema = ['name', 'description', 'qualityIndex']
    data_dict = client.call_and_wait('c/getSample', [], 15)
    print(data_dict)

    #TODO: criar um metodo shutdown


def remote_test_case_connect():
    '''Exemplo de criacao de cliente OpenDSB no Sapiens'''

    logger = logging.getLogger(__name__)

    logger.info('Creating router...')
    router = DefaultRouter()

    address = 'ws://localhost:8080/open-dsb/bus'
    client = router.connect_to_remote(address)

    time.sleep(2)

    #schema = ['name', 'description', 'qualityIndex']
    data_dict = client.call_and_wait('c/getSample', [], 15)
    print(data_dict)

    #TODO: criar um metodo shutdown


def main():
    #local_test_case()
    #remote_test_case()
    #remote_test_case_continuous()
    #remote_test_case_callandwait()
    remote_test_case_connect()


if __name__ == '__main__':
    logger = initialize_logger()
    main()
