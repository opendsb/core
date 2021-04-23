package org.opendsb.json;

import java.lang.reflect.Type;

import org.apache.log4j.Logger;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

@SuppressWarnings("unused")
public class TypedData {
	
	private Class<?> dataType;
	
	private Object data;
	
	private TypedData() {
		super();
	}

	public TypedData(Object data) {
		super();
		this.data = data;
		if (data != null) {
			this.dataType = data.getClass();
		}	
	}

	public Class<?> getDataType() {
		return dataType;
	}

	public Object getData() {
		return data;
	}
	
	public static class TypedDataAdapter implements JsonDeserializer<TypedData>, JsonSerializer<TypedData> {

		private static final Logger logger = Logger.getLogger(TypedDataAdapter.class);

		public TypedDataAdapter() {
			super();
		}

		@Override
		public TypedData deserialize(JsonElement jsonElement, Type arg1, JsonDeserializationContext context)
				throws JsonParseException {

			String className = "";
			TypedData typedData = new TypedData(null);
			
			JsonObject obj = jsonElement.getAsJsonObject();

			try {
				
				Object data = null;
				
				if (obj.has("dataType") && obj.has("data")) {
					className = obj.get("dataType").getAsString();
					
					Class<?> type = Class.forName(className);
					data = context.deserialize(obj.get("data"), type);
					
					typedData = new TypedData(data);
				}
			} catch (Exception e) {
				logger.warn("Error decoding a datapackage. An empty value will be assigned", e);
			}

			return typedData;
		}

		@Override
		public JsonElement serialize(TypedData data, Type arg1, JsonSerializationContext context) {
			JsonObject jObj = new JsonObject();
			jObj.add("data", context.serialize(data.getData()));
			jObj.addProperty("dataType", data.getDataType() != null ? data.getDataType().getName() : null);
			return jObj;
		}
	}
}
