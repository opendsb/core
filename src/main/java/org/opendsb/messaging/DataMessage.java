package org.opendsb.messaging;

import org.opendsb.json.AnnotationExclusion;
import org.opendsb.json.TypedData;
import org.opendsb.json.info.DefaultData;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DataMessage extends BaseMessage {
	
	private static Gson coder = new GsonBuilder().setExclusionStrategies(new AnnotationExclusion())
			.registerTypeAdapter(TypedData.class, new TypedData.TypedDataAdapter()).create();

	private TypedData data;

	public DataMessage(String destination, String origin, Object data) {
		super(destination, origin);
		this.type = MessageType.PUBLISH;
		
		if (data instanceof TypedData) {
			this.data = (TypedData)data;
		} else {
			this.data = new DefaultData(data);
		}
	}

	public Object getData() {
		return data.getData();
	}

	public static <T extends DataMessage> T fromJSON(String json, Class<T> clazz) {
		return coder.fromJson(json, clazz);
	}
	
	@Override
	public String toJSON() {
		return coder.toJson(this);
	}
	
}
