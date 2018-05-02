package org.opendsb.routing.remote.ws;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;

import org.apache.log4j.Logger;
import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.DataMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.MessageType;
import org.opendsb.messaging.ReplyMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class MessageDecoder implements Decoder.Text<Message> {

	private static final Logger logger = Logger.getLogger(MessageDecoder.class);

	@Override
	public void init(EndpointConfig config) {
	}

	@Override
	public void destroy() {
	}

	@Override
	public Message decode(String jsonMessage) throws DecodeException {

		logger.trace("Decoding the message '" + jsonMessage + "'.");

		Message message = null;

		try {
			message = doDecoding(jsonMessage);
		} catch (JsonSyntaxException e) {
			throw new DecodeException(jsonMessage, e.getMessage(), e);
		}

		return message;
	}

	@Override
	public boolean willDecode(String jsonMessage) {

		logger.trace("Trying to assertain validity for the message '" + jsonMessage + "'.");

		boolean willDecode = true;

		try {
			doDecoding(jsonMessage);
		} catch (JsonSyntaxException e) {
			logger.error("unable to decode message", e);
			willDecode = false;
		}

		return willDecode;
	}

	public static Message doDecoding(String jsonMessage) throws JsonSyntaxException {

		Message message = null;

		Gson gson = new Gson();

		JsonParser parser = new JsonParser();

		JsonObject obj = parser.parse(jsonMessage).getAsJsonObject();

		if (obj.has("type")) {

			MessageType type = gson.fromJson(obj.get("type"), MessageType.class);

			switch (type) {

			case CALL: {
				message = gson.fromJson(jsonMessage, CallMessage.class);
				break;
			}
			case CONTROL: {
				message = gson.fromJson(jsonMessage, ControlMessage.class);
				break;
			}
			case PUBLISH: {
				message = DataMessage.fromJSON(jsonMessage, DataMessage.class);
				break;
			}
			case REPLY: {
				message = DataMessage.fromJSON(jsonMessage, ReplyMessage.class);
				break;
			}
			default: {
			}
			}
		}

		return message;
	}

}
