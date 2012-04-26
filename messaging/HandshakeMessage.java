package com.kd.chat.messaging;


import java.io.UnsupportedEncodingException;

public class HandshakeMessage extends AbstractMessage{

	public static final String PROTOCOL_STRING = "Super Chat Master";

	protected final String username;

	protected final int listenPort;

	public int getListenPort(){
		return this.listenPort;
	}

	public HandshakeMessage(final String username, final int listenPort)
	throws UnsupportedEncodingException {
		super(3+HandshakeMessage.PROTOCOL_STRING.getBytes("UTF-16BE").
				length + username.getBytes("UTF-16BE").length,
				AbstractMessage.TYPE_HANDSHAKE_MESSAGE);
		this.username = username;
		this.listenPort = listenPort;
	}

	public String getUsername(){
		return this.username;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();

		sb.append(super.toString()).append(',').
			append(PROTOCOL_STRING).append(',').
			append(this.username).append(',').
			append(this.listenPort);

		return sb.toString();
	}
}
