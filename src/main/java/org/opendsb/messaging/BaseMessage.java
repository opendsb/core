package org.opendsb.messaging;

import java.util.UUID;

import org.opendsb.json.AnnotationExclusion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class BaseMessage implements Message {

	private String messageId = UUID.randomUUID().toString();

	protected MessageType type;

	private String origin;

	private String destination;

	private String latestHop;

	public BaseMessage(String destination, String origin) {
		super();
		this.origin = origin;
		this.latestHop = origin;
		this.destination = destination;
	}

	public BaseMessage(String origin) {
		super();
		this.origin = origin;
		this.latestHop = origin;
	}

	@Override
	public String getMessageId() {
		return messageId;
	}

	@Override
	public String getDestination() {
		return destination;
	}

	@Override
	public MessageType getType() {
		return type;
	}

	@Override
	public String getOrigin() {
		return origin;
	}

	@Override
	public String getLatestHop() {
		return latestHop;
	}

	@Override
	public void setLatestHop(String latestHop) {
		this.latestHop = latestHop;
	}

	@Override
	public String toJSON() {
		Gson gson = new GsonBuilder().setExclusionStrategies(new AnnotationExclusion()).create();
		return gson.toJson(this);
	}

	@Override
	public String toString() {
		return "BaseMessage [messageId=" + messageId + ", type=" + type + ", destination=" + destination + "]";
	}
}
