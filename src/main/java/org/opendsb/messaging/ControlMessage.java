package org.opendsb.messaging;

import java.util.HashMap;
import java.util.Map;

import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;

public class ControlMessage extends BaseMessage {

	private ControlMessageType controlMessageType;

	private Map<String, String> controlInfo = new HashMap<>();

	private ControlMessage(String origin, ControlMessageType controlMessageType, Map<String, String> controlInfo) {
		super(origin);
		this.type = MessageType.CONTROL;
		this.controlMessageType = controlMessageType;
		this.controlInfo = controlInfo;
	}
	
	private ControlMessage(String origin, String destination, ControlMessageType controlMessageType, Map<String, String> controlInfo) {
		super(destination, origin);
		this.type = MessageType.CONTROL;
		this.controlMessageType = controlMessageType;
		this.controlInfo = controlInfo;
	}

	public ControlMessageType getControlMessageType() {
		return controlMessageType;
	}

	public boolean containsControlToken(String controlToken) {
		return controlInfo.containsKey(controlToken);
	}

	public String getControlInfo(String controlToken) throws IllegalArgumentException {
		if (controlInfo.containsKey(controlToken)) {
			return controlInfo.get(controlToken);
		} else {
			throw new IllegalArgumentException("Token '" + controlToken + "' not found within the message.");
		}
	}

	public static class Builder {

		public ConnectionRequestMessageBuilder createConnectionRequestMessage(String transactionId, String origin) {
			return new ConnectionRequestMessageBuilder(transactionId, origin);
		}

		public ConnectionReplyMessageBuilder createConnectionReplyMessage(String transactionId, String origin) {
			return new ConnectionReplyMessageBuilder(transactionId, origin);
		}
		
		public CallAckMessageBuilder createCallAckMessage(String transactionId, String origin, String destination) {
			return new CallAckMessageBuilder(transactionId, origin, destination);
		}

		public abstract class ControlBuilder {

			protected ControlMessageType controlMessageType = ControlMessageType.CONNECTION_REQUEST;
			protected Map<String, String> controlInfo = new HashMap<>();
			private String origin;
			private String destination = null;

			private ControlBuilder(String transactionId, String origin) {
				this.origin = origin;
				controlInfo.put(ControlTokens.TRANSACTION_ID, transactionId);
			}
			
			private ControlBuilder(String transactionId, String origin, String destination) {
				this.origin = origin;
				this.destination = destination;
				controlInfo.put(ControlTokens.TRANSACTION_ID, transactionId);
			}

			public ControlMessage build() {
				if (destination != null) {
					return new ControlMessage(origin, destination, controlMessageType, controlInfo);
				} else {
					return new ControlMessage(origin, controlMessageType, controlInfo);
				}
			}
		}
		
		public class CallAckMessageBuilder extends ControlBuilder {
			public CallAckMessageBuilder(String transactionId, String origin, String destination) {
				super(transactionId, origin, destination);
				controlMessageType = ControlMessageType.CALL_ACK;
			}
		}

		public class ConnectionRequestMessageBuilder extends ControlBuilder {

			private ConnectionRequestMessageBuilder(String transactionId, String origin) {
				super(transactionId, origin);
				controlMessageType = ControlMessageType.CONNECTION_REQUEST;
			}

			public ConnectionRequestMessageBuilder addClientId(String clientId) {
				controlInfo.put(ControlTokens.CLIENT_ID, clientId);
				return this;
			}
		}

		public class ConnectionReplyMessageBuilder extends ControlBuilder {

			private ConnectionReplyMessageBuilder(String transactionId, String origin) {
				super(transactionId, origin);
				controlMessageType = ControlMessageType.CONNECTION_REPLY;
			}

			public ConnectionReplyMessageBuilder addServerId(String serverId) {
				controlInfo.put(ControlTokens.SERVER_ID, serverId);
				return this;
			}
		}
	}
}
