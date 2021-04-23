package org.opendsb.json;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class SerializerUtils {
	public static class ClassAdapter implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

		@Override
		public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			String className = json.getAsString();
			Class<?> value = null;
			try {
				value = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new JsonParseException("Unable to parse '" + className + "' into a Class", e);
			}
			return value;
		}

		@Override
		public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
			return context.serialize(src.getName());
		}
		
	}
}
