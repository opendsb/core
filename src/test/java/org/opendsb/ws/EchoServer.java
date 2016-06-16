package org.opendsb.ws;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/echo")
public class EchoServer {

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("Client connected : '" + session.getId() + "'");
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("Client connected : '" + session.getId() + "'");
	}

	@OnMessage
	public String onMessage(Session session, String message) {
		System.out.println("Message received from client '" + session.getId() + "'. \nMessage payload: " + message);
		return message;
	}

	// http://docs.oracle.com/javaee/7/api/javax/websocket/Endpoint.html#onError-javax.websocket.Session-java.lang.Throwable-
	//
	// Developers may implement this method when the web socket session creates
	// some kind of error that is not modeled in the web socket protocol. This
	// may for example be a notification that an incoming message is too big to
	// handle, or that the incoming message could not be encoded. There are a
	// number of categories of exception that this method is (currently) defined
	// to handle: a) connection problems, for example, a socket failure that
	// occurs before the web socket connection can be formally closed. These are
	// modeled as SessionExceptions; b) runtime errors thrown by developer
	// created message handlers calls; c) conversion errors encoding incoming
	// messages before any message handler has been called. These are modeled as
	// DecodeExceptions
	@OnError
	public void onError(Session session, Throwable thr) {
		System.out.println("Error: session.id = '" + session.getId());
		System.out.println(thr);
	}
}
