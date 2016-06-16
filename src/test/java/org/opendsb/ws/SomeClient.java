package org.opendsb.ws;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;

@ClientEndpoint
public class SomeClient {

	@OnOpen
	public void onOpen(Session session) {
		session.getAsyncRemote().sendText("Hello");
	}

	@OnError
	public void onError(Session session, Throwable t) {
		System.out.println("Error");
		t.printStackTrace();
	}
}
