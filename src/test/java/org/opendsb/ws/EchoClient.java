package org.opendsb.ws;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

@ClientEndpoint
public class EchoClient {

	@OnOpen
	public void open(Session session) {
		System.out.println("Connecting to server with session id '" + session.getId() + "'");
	}

	@OnMessage
	public void message(String message) {
		System.out.println("Receiving message: " + message);
	}

	@OnClose
	public void close(Session session, CloseReason reason) {

	}

}
