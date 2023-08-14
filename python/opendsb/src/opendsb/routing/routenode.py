"""RouteNode class"""

import logging
from typing import Callable
from ..client.subscription import Subscription
from ..messaging.message import Message

logger = logging.getLogger('opendsb')


class RouteNode:
    """Node of the routing tree. 

    It is responsible for subscribing, canceling, and storing the subscriptions.
    Also processes messages and notifies the subscribers.
    """
	
    def __init__(self, topic: str): #, router_id: str):  # Commented because RouteNode no longer needs the router id
        self.topic = topic
        self.subscribers: dict[str, Subscription] = dict() # dict[Subscription.id, Subscription]
        #self.router_id = router_id  # Commented because RouteNode no longer needs the router id
        logger.debug(f'RouteNode created for topic: "{self.topic}"')

    def accept(self, message: Message) -> bool:
        for subscription in self.subscribers.values():
            logger.debug(f'Passing message "{message}" in node "{self.topic}" to handler'[:500])# "{subscription.handler}"')
            try:
                #subscription.handler.accept(message)
                subscription.handler(message)
            except Exception as e:
                logger.debug(f'Exception in RouteNode "{self.topic}": Unable to process message "{message}".'[:500], exc_info=True)
        return True

    def subscribe(self, subscription_id: str, handler: Callable) -> Subscription:
        """Add a subscription to the node
        All subscriptions are stored in a dict called subscribers and can be accessed by the subscription id. 
        All subscriptions have the same topic as the node.
        """
        subscription = Subscription(self.topic, subscription_id, handler)
        self.subscribers[subscription_id] = subscription
        logger.debug(f'Node "{self.topic}" has new subscription: "{subscription}"')
        logger.debug(f'Topic "{self.topic}" subscribers: "{[subscription for subscription in self.subscribers.values()]}"')
        return subscription

    def unsubscribe(self, subscription_id: str) -> None:
        """Remove a subscription from the node"""
        if subscription_id in self.subscribers.keys():
            self.subscribers.pop(subscription_id)
            logger.debug(f'Node "{self.topic}" removed subscription: "{subscription_id}"')
        else:
            logger.warning(f'Node "{self.topic}" has no subscription with id: "{subscription_id}"')
        logger.debug(f'Topic "{self.topic}" subscribers: "{[subscription for subscription in self.subscribers.values()]}"')

    def __repr__(self) -> str:
        return f'{self.__class__.__qualname__}(topic={self.topic}, subscribers={self.subscribers.keys()})'
