package org.opendsb.messaging;

import java.lang.reflect.Type;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class DataMessage extends BaseMessage {

	private DataPackage data = new DataPackage();

	public DataMessage(String destination, String origin, Object data) {
		super(destination, origin);
		this.data.data = data;
		this.type = MessageType.PUBLISH;
		if (data != null) {
			this.data.dataType = data.getClass().getName();
		}
	}

	public Object getData() {
		return data.data;
	}

	public String getDataType() {
		return data.dataType;
	}

	public static <T extends DataMessage> T fromJSON(String json, Class<T> clazz) {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(DataPackage.class, new DataPackage.DataPackageAdapter());
		Gson gson = builder.create();
		return gson.fromJson(json, clazz);
	}

	private static class DataPackage {

		private Object data;
		private String dataType;

		private static class DataPackageAdapter implements JsonDeserializer<DataPackage> {

			private static final Logger logger = Logger.getLogger(DataPackageAdapter.class);

			public DataPackageAdapter() {
				super();
			}

			@Override
			public DataPackage deserialize(JsonElement json, Type arg1, JsonDeserializationContext context)
					throws JsonParseException {

				String className = "";
				DataPackage pkg = new DataPackage();

				try {
					if (!json.getAsJsonObject().has("dataType") || !json.getAsJsonObject().has("data")) {
						return pkg;
					}
					className = json.getAsJsonObject().get("dataType").getAsString();
					Class<?> type = Class.forName(className);
					Object data = context.deserialize(json.getAsJsonObject().get("data"), type);

					pkg.dataType = className;
					pkg.data = data;

				} catch (Exception e) {
					logger.warn("Error decoding a datapackage. An empty value will be assigned", e);
				}

				return pkg;
			}
		}
	}
}
