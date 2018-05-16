package org.opendsb.routing;

import java.util.Map;
import java.util.function.Consumer;

import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.remote.RemoteRouter;

public interface Router {
	public String getId();
	
	public Map<String, Integer> getFullSubscriptionCount();

	public void setRemoteRouter(RemoteRouter remoteRouter);

	public void routeMessage(Message message, boolean remote);

	public Subscription subscribe(String topic, Consumer<Message> handler, HandlerPriority priority);
}
