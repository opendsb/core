package org.opendsb.messaging;

import java.util.List;
import java.util.stream.Collectors;

import org.opendsb.json.AnnotationExclusion;
import org.opendsb.json.TypedData;

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
		this.parameters = parameters.stream().map(param -> new TypedData(param)).collect(Collectors.toList());
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
}
