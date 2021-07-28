package org.opendsb.messaging;

import java.util.List;
import java.util.stream.Collectors;

import javax.websocket.DecodeException;

import org.opendsb.json.AnnotationExclusion;
import org.opendsb.json.TypedData;
import org.opendsb.json.info.DefaultData;
import org.opendsb.routing.remote.ws.GsonCoder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class CallMessage extends BaseMessage {
	
	private static Gson coder = new GsonBuilder().setExclusionStrategies(new AnnotationExclusion())
			.registerTypeAdapter(TypedData.class, new TypedData.TypedDataAdapter()).create();

	private List<TypedData> parameters;

	private String replyTo;

	public CallMessage(String destination, String origin, List<Object> parameters, String replyTo) {
		super(destination, origin);
		this.replyTo = replyTo;
		type = MessageType.CALL;
		this.parameters = parameters
				.stream()
				.map(param -> {
					if (param instanceof TypedData) {
						return (TypedData)param;
					} else {
						return new DefaultData(param);
					}
				}).collect(Collectors.toList());
	}

	public List<Object> getParameters() {
		return parameters.stream().map(param -> param.getData()).collect(Collectors.toList());
	}

	public String getReplyTo() {
		return replyTo;
	}
	
	public static <T extends CallMessage> T fromJSON(String json, Class<T> clazz) {
		return coder.fromJson(json, clazz);
	}
	
	@Override
	public String toJSON() {
		return coder.toJson(this);
	}
	
	public static void main(String[] args) {
		String msg = "{\"parameters\":[{},{}],\"replyTo\":\"reply-78dfb997-31f3-440e-9d1f-f88c4a87fb28/service/UserService/getUserByUserId\",\"messageId\":\"7a062be5-3d89-47f1-8a6b-16a0551013eb\",\"type\":\"CALL\",\"origin\":\"Router_3cf4d915-16d1-413d-bce6-3b707d0d843c\",\"destination\":\"service/UserService/getUserByUserId\",\"latestHop\":\"Router_3cf4d915-16d1-413d-bce6-3b707d0d843c\"}";
		
		GsonCoder coder = new GsonCoder();
		
		try {
			Message callMessage = coder.decode(msg);
			System.out.println("Hi!");
		} catch (DecodeException e) {
			e.printStackTrace();
		}
		
	}
}
