package org.opendsb.messaging;

public interface Message {
	public String getOrigin();

	public MessageType getType();

	public String getMessageId();

	public String getDestination();

	public String getLatestHop();

	public void setLatestHop(String latestHop);

	public String toJSON();
}
