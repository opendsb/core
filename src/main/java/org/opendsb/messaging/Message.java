package org.opendsb.messaging;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public interface Message {
	public String getOrigin();

	public MessageType getType();

	public String getMessageId();

	public String getDestination();

	public String getLatestHop();

	public void setLatestHop(String latestHop);

	public String toJSON();
	
	public static class MessageTypeAdapter implements JsonDeserializer<Message> {

		@Override
		public Message deserialize(JsonElement jsonMessage, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			
			Message message = null;


			JsonObject obj = jsonMessage.getAsJsonObject();

			if (obj.has("type")) {

				MessageType type = MessageType.valueOf(obj.get("type").getAsString());

				switch (type) {

				case CALL: {
					message = context.deserialize(jsonMessage, CallMessage.class);
					break;
				}
				case CONTROL: {
					message = context.deserialize(jsonMessage, ControlMessage.class);
					break;
				}
				case PUBLISH: {
					message = context.deserialize(jsonMessage, DataMessage.class);
					break;
				}
				case REPLY: {
					message = context.deserialize(jsonMessage, ReplyMessage.class);
					break;
				}
				default: {
				}
				}
			}

			return message;
		}
		
	}
}
