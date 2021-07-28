package org.opendsb.json.info;

import java.lang.reflect.Type;

import org.opendsb.json.TypedData;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DefaultData extends TypedData {

	private Class<?> dataType;

	public DefaultData(Object data) {
		super(data, DefaultData.class.getName());
		if (data != null) {
			this.dataType = data.getClass();
		}
	}

	public Class<?> getDataType() {
		return dataType;
	}
	
	public static class DefultDataAdapter implements JsonDeserializer<DefaultData>, JsonSerializer<DefaultData> {

		@Override
		public DefaultData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			
			DefaultData defaultData = new DefaultData(null);
			
			JsonObject obj = json.getAsJsonObject();
			
			try {
			
				if (obj.has("dataType") && obj.has("data")) {
				
					Object data = null;
					
					Class<?> type = Class.forName(obj.get("dataType").getAsString());
					
					if (type.getName().equals(Class.class.getName())) {
						String dataClassName = obj.get("data").getAsString();
						data = Class.forName(dataClassName);
					} else {
						data = context.deserialize(obj.get("data"), type);
					}
					
					defaultData = new DefaultData(data);
				}
			
			} catch(Exception e) {
				throw new JsonParseException(e);
			}
			
			return defaultData;
		}
		
		@Override
		public JsonElement serialize(DefaultData data, Type arg1, JsonSerializationContext context) {
			JsonObject jObj = new JsonObject();
			
			jObj.addProperty("concreteType", DefaultData.class.getName());
			
			jObj.addProperty("dataType", data.getDataType() != null ? data.getDataType().getName() : null);
			
			if (data.getDataType() != null && data.getDataType().getName().equals(Class.class.getName())) {
				jObj.add("data", context.serialize(((Class<?>)data.getData()).getName()));
			} else {
				jObj.add("data", context.serialize(data.getData()));
			}
			
			return jObj;
		}
		
	}
}
