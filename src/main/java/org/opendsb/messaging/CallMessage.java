package org.opendsb.messaging;

import java.util.Map;

public class CallMessage extends BaseMessage {

	private Map<String, Object> parameters;
	
	private String replyTo;

	
	public CallMessage(String destination, String origin,
			Map<String, Object> parameters, String replyTo) {
		super(destination, origin);
		this.replyTo = replyTo;
		type = MessageType.CALL;
		this.parameters = parameters;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public String getReplyTo() {
		return replyTo;
	}
}
