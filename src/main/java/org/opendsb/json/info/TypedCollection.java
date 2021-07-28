package org.opendsb.json.info;

import java.lang.reflect.Type;
import java.util.Collection;

import org.opendsb.json.TypedData;
import org.opendsb.util.ReflectionUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class TypedCollection extends TypedData {

	private Class<?> collectionRawType;

	private Class<?> collectionGenericType;

	public TypedCollection(Object data, Class<?> collectionGenericType) {
		super(data, TypedCollection.class.getName());
		if (data != null) {
			this.collectionRawType = data.getClass();
			this.collectionGenericType = collectionGenericType;
		}
	}

	public Class<?> getCollectionRawType() {
		return collectionRawType;
	}

	public Class<?> getCollectionGenericType() {
		return collectionGenericType;
	}

	public static class TypedCollectionAdapter implements JsonDeserializer<TypedCollection> {

		@Override
		public TypedCollection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			
			TypedCollection typedCollection = new TypedCollection(null, null);
			
			JsonObject obj = json.getAsJsonObject();
			
			try {
				if (obj.has("collectionRawType") && obj.has("collectionGenericType")) {
					
					Class<?> collectionRawType = Class.forName(obj.get("collectionRawType").getAsString());
					Class<?> collectionGenericType = Class.forName(obj.get("collectionGenericType").getAsString());
					
					Collection<Object> collection = ReflectionUtils.createCollection(collectionRawType);
					
					JsonArray rawCollection = obj.get("data").getAsJsonArray();
					
					for (JsonElement el : rawCollection) {
						Object deserialized = context.deserialize(el, collectionGenericType);
						collection.add(deserialized);
					}
					
					typedCollection = new TypedCollection(collection, collectionGenericType);
				}
			} catch(Exception e) {
				throw new JsonParseException(e);
			}
			
			return typedCollection;
		}
		
	}
}
