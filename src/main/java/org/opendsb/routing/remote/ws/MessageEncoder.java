package org.opendsb.routing.remote.ws;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.opendsb.messaging.Message;

public class MessageEncoder implements Encoder.Text<Message> {

	@Override
	public void init(EndpointConfig config) {
	}

	@Override
	public void destroy() {
	}

	@Override
	public String encode(Message message) throws EncodeException {
		return message.toJSON();
	}
}
