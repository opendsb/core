#from logging.handlers import RotatingFileHandler
import logging
import json
import random
import time

from .messaging.callmessage import CallMessage
from .client.defaultbusclient import DefaultBusClient
from .routing.defaultrouter import DefaultRouter
from .utils.dictvalidator import DictValidator
from .messaging.datamessage import DefaultData

from .pi.PIConnection import PIConnection

def initialize_logger():
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.DEBUG)
    #logger_handler = RotatingFileHandler('opendsb.log', maxBytes=1_000_000, backupCount=0)
    logger_handler = logging.FileHandler('opendsb.log', mode='w')
    #logger_handler.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(filename)s - %(funcName)s | %(message)s")) #"%(asctime)s - %(name)s - %(levelname)s - %(filename)s:%(funcName)s(%(lineno)d) - %(message)s"
    logger_handler.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s - %(threadName)s - %(filename)s - %(funcName)s | %(message)s"))
    logger.handlers.clear()
    logger.addHandler(logger_handler)
    return logger


def local_test_case():
    
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
    subscription = client.subscribe('A/B', lambda m: custom_reply_handler(m)) # lambda message: print(message.data)

    logger.info('Publishing data to subscribed topics...')
    client.publish_data('A/B', 'Hello World!')
    #client.publish_data('C', 'Hello New World!')
    
    logger.info('Making a Call wih parameters...')
    response = client.call('A/B', ['Client call param 1', 'Client call param 2'])
    #response = client.call('C', ['Hello World!'])

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

#    response = client.call('c/getSample', [])
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



def main():
    #local_test_case()
    #remote_test_case()
    remote_test_case_continuous()


if __name__ == '__main__':
    logger = initialize_logger()
    main()




# TODO: Implementar uma forma de aguardar o servidor quando cair e se conectar automaticamente
# TODO: Implementar uma forma de me reconectar automaticamente 
# TODO: pesquisar sobre "backoff" do tipo exponencial (tipicamente na base 2: 10, 20, 40.... t_max)

# TODO: aguardar a Future do connect retornar para continuar
# Este Future pode completar normalmente e vc segue
# Se der algum erro de conexão tento novamente (backoff)
# Se a conexão for interrompida tenho que reconectar tbm (backoff)

# Tasklist
# 1 - usar o future da conexao para remover o timesleep (DONE)
# 2 - publicar a cada 10s usando uma threadpool, substituindo hello world por um rand
# 3 - tenho que ser capaz de conseguir me reconectar automaticamente quando o servidor docker cair
