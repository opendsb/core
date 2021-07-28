package org.opendsb.routing.remote.ws;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.apache.log4j.Logger;
import org.opendsb.json.AnnotationExclusion;
import org.opendsb.json.SerializerUtils;
import org.opendsb.json.TypedData;
import org.opendsb.json.info.DefaultData;
import org.opendsb.json.info.TypedCollection;
import org.opendsb.json.info.TypedMap;
import org.opendsb.messaging.Message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonCoder implements Decoder.Text<Message>, Encoder.Text<Message> {
	
	private static final Logger logger = Logger.getLogger(GsonCoder.class);
	
	private Gson gson;
	
	
	public GsonCoder() {
		super();
		buildGson(new HashMap<>());
	}
	
	public void updateTypeAdapters(Map<Class<?>, Object> typeAdapterIdx) {
		buildGson(typeAdapterIdx);
	}
	
	private void buildGson(Map<Class<?>, Object> typeAdapterIdx) {
		
		GsonBuilder builder = new GsonBuilder();
		
		builder = builder.setExclusionStrategies(new AnnotationExclusion())
				.registerTypeAdapter(TypedMap.class, new TypedMap.TypedMapAdapter())
				.registerTypeAdapter(Class.class, new SerializerUtils.ClassAdapter())
				.registerTypeAdapter(Message.class, new Message.MessageTypeAdapter())
				.registerTypeAdapter(TypedData.class, new TypedData.TypedDataAdapter())
				.registerTypeAdapter(DefaultData.class, new DefaultData.DefultDataAdapter())
				.registerTypeAdapter(TypedCollection.class, new TypedCollection.TypedCollectionAdapter());
		
		for(Entry<Class<?>, Object> adapterEntry : typeAdapterIdx.entrySet()) {
			Class<?> adapterTargetClass = adapterEntry.getKey();
			Object typeAdapter = adapterEntry.getValue();
			
			builder = builder.registerTypeAdapter(adapterTargetClass, typeAdapter);
		}
		
		gson = builder.create();
	}
	
	@Override
	public void init(EndpointConfig config) {
	}

	@Override
	public void destroy() {
	}

	@Override
	public String encode(Message message) throws EncodeException {
		return gson.toJson(message);
	}

	@Override
	public Message decode(String jsonMessage) throws DecodeException {
		
		logger.trace("Decoding the message '" + jsonMessage + "'.");

		Message message = null;

		try {
			message = gson.fromJson(jsonMessage, Message.class);
		} catch (Exception e) {
			throw new DecodeException(jsonMessage, e.getMessage(), e);
		}

		return message;
	}

	@Override
	public boolean willDecode(String jsonMessage) {
		
		logger.trace("Trying to assertain validity for the message '" + jsonMessage + "'.");

		boolean willDecode = true;

		try {
			gson.fromJson(jsonMessage, Message.class);
		} catch (Exception e) {
			logger.error("unable to decode message", e);
			willDecode = false;
		}
	
		return willDecode;
	}

}
