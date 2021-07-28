package org.opendsb.json.info;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opendsb.json.TypedData;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

public class TypedMap extends TypedData {

	private Class<?> keyType;
	
	private Class<?> valueType;

	public TypedMap(Object map, Class<?> keyType, Class<?> valueType) {
		super(map, TypedMap.class.getName());
		this.keyType = keyType;
		this.valueType = valueType;
	}

	public Class<?> getKeyType() {
		return keyType;
	}

	public Class<?> getValueType() {
		return valueType;
	}

	public static class TypedMapAdapter implements JsonDeserializer<TypedMap> {

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public TypedMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			
			TypedMap typedMap = new TypedMap(null, null, null);
			
			JsonObject obj = json.getAsJsonObject();

			try {
				if (obj.has("keyType") && obj.has("valueType")) {
					
					Class<?> keyType = Class.forName(obj.get("keyType").getAsString());
					Class<?> valueType = Class.forName(obj.get("valueType").getAsString());
					
					JsonObject mapObj = obj.get("data").getAsJsonObject();
					
					Map resultMap = new HashMap<>();
					
					for (Entry<String, JsonElement> entries : mapObj.entrySet()) {
						resultMap.put(deserializeKey(entries.getKey(), keyType, context), context.deserialize(entries.getValue(), valueType));
					}
					
					typedMap = new TypedMap(resultMap, keyType, valueType);
				}
			} catch (Exception e) {
				throw new JsonParseException(e);
			}
			
			return typedMap;
		}
		
		private Object deserializeKey(String key, Class<?> keyClass, JsonDeserializationContext context) {
			
			Object keyValue = null;
			
			if (Long.class.equals(keyClass)) {
				keyValue = Long.parseLong(key);
			} else if (Integer.class.equals(keyClass)) {
				keyValue = Integer.parseInt(key);
			} else if (Float.class.equals(keyClass)) {
				keyValue = Float.parseFloat(key);
			} else if (Double.class.equals(keyClass)) {
				keyValue = Double.parseDouble(key);
			} else if (String.class.equals(keyClass)) {
				keyValue = key;
			} else {
				JsonElement el = new JsonPrimitive(key);
				keyValue = context.deserialize(el, keyClass);
			}
			
			return keyValue;
		}
		
	}
	
}
