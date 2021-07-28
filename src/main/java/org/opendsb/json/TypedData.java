package org.opendsb.json;

import java.lang.reflect.Type;

import org.apache.log4j.Logger;
import org.opendsb.json.info.DefaultData;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public abstract class TypedData {
	
	private Object data;
	
	protected String concreteType = "";


	public TypedData(Object data, String concreteType) {
		super();
		this.data = data;
		this.concreteType = concreteType;
	}
	
	public Object getData() {
		return data;
	}

	public String getConcreteType() {
		return concreteType;
	}

	public static class TypedDataAdapter implements JsonDeserializer<TypedData> {

		private static final Logger logger = Logger.getLogger(TypedDataAdapter.class);

		public TypedDataAdapter() {
			super();
		}

		@Override
		public TypedData deserialize(JsonElement jsonElement, Type arg1, JsonDeserializationContext context)
				throws JsonParseException {
			
			Object data = null;
			
			try {
				
				logger.trace("Decoding TypedData '" + jsonElement.toString() + "'");
				
				JsonObject obj = jsonElement.getAsJsonObject();
			
				if (obj.has("concreteType")) {
					Class<?> concreteClass = Class.forName(obj.get("concreteType").getAsString());
					data = context.deserialize(jsonElement, concreteClass);
				} else {
					data = new DefaultData(null);
				}
				
			} catch (Exception e) {
				throw new JsonParseException(e);
			}
			
			return (TypedData)data;
		}
	}
}
