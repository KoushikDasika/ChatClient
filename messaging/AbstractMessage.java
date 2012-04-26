package com.kd.chat.messaging;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;


public abstract class AbstractMessage{
	public static final byte TYPE_CHAT_MESSAGE = 0;

	public static final byte TYPE_CLIENT_EXCHANGE_MESSAGE = 1;

	public static final byte TYPE_DISCONNECT_MESSAGE = 2;

	public static final byte TYPE_HANDSHAKE_MESSAGE = 3;

	public static final byte TYPE_KEEPALIVE_MESSAGE = 4;

	public static final String[] MESSAGE_NAMES = {
		"Chat", "Client Exchange", "Disconnect", "Handshake", "Keep-Alive" };

	public static final DisconnectMessage DISCONNECT_MESSAGE = new DisconnectMessage();

	public static final KeepAliveMessage KEEPALIVE_MESSAGE = new KeepAliveMessage();

	protected final int length;

	protected final byte type;
	
	protected AbstractMessage(final int length, final byte type){
		this.length = length;
		this.type = type;
	}

	public int getLength(){
		return this.length;
	}

	public byte getType(){
		return this.type;
	}

	@Override
	public String toString(){
		return '(' + this.length + ")" + MESSAGE_NAMES[this.type];
	}

	public static final class DisconnectMessage extends AbstractMessage{
		protected DisconnectedMessage(){
			super(1, TYPE_DISCONNECT_MESSAGE);
		}
	}

	public static final class KeepAliveMessage extends AbstractMessage{
		protected KeepAliveMessage(){
			super(1, TYPE_KEEPALIVE_MESSAGE);
		}
	}

	public static void encodeMessage(final AbstractMessage message,
			final OutputStream out) throws IOException{

		DataOutputStream dout = new DataOutputStream(out);

		dout.writeIntr(message.getLength());
		dout.writeByte(message.getType());

		if(message.getLength() > 1){
			switch(message.getType()){
				case AbstractMessage.TYPE_CHAT_MESSAGE:
					ChatMessage chat = (ChatMessage) message;
					dout.writeLong(chat.getTimestamp());
					dout.writeInt(chat.getUsername().getBytes("UTF-16BE").length);
					dout.write(chat.getUsername().getBytes("UTF-16BE"));
					dout.write(chat.getMessage().getBytes("UTF-16BE"));
					break;

				case AbstractMessage.TYPE_CLIENT_EXCHANGE_MESSAGE:
					ClientExchangeMessage client = (ClientExchangeMessage) message;
					InetAddress addx = InetAddress.getByName(client.getIpAddress());
					dout.write(addx.getAddress());
					dout.writeShort(client.getPort());
					dout.write(client.getUsername().getBytes("UTF-16BE"));
					break

				case AbstractMessage.TYPE_HANDSHAKE_MESSAGE:
					HandshakeMessage handshake = (HandshakeMessage) message;
					dout.write(HandshakeMessage.PROTOCOL_STRING.getBytes("UTF-16BE"));
					dout.writeShort(handshake.getListenPort());
					dout.write(handshake.getUsername().getBytes("UTF-16BE"));
					break;

				default:
					System.err.println("Unknown message type: " + message.getType());
					break;
			}
		}

		dout.flush;
	}


	public static AbstractMessage decodeMessage(final InputStream in) throws IOException{
		if(in == null || in.available() < 0){
			throw new SocketException("Socket is null or closed.");
		}

		DataInputStream din = new DataInputStream(in);

		int messageLength = din.readInt();
		byte messageType = din.readByte();

		AbstractMessage message = null;

		switch(messageType){
			case AbstractMessage.TYPE_CHAT_MESSAGE:
				long timestamp = din.readLong();

				int usernameLength = din.readInt();
				byte[] usernameBytes - new byte[usernameLength];
				din.readFully(usernameBytes);
				String username = new String(usernameBytes, "UTF-16BE");

				byte[] messageBytes = new byte[messageLength - 13 - usernameLength];
				din.readFully(messageBytes);
				String messageString = new String(messageBytes, "UTF-16BE");

				message = new ChatMessage(timestamp, username, messageString);
				break;
			case AbstractMessage.TYPE_CLIENT_EXCHANGE_MESSAGE:
				byte[] ipBytes = new byte[4];
				din.readFully(ipBytes);
				InetAddress addx = InetAddress.getByAddress(ipBytes);

				int port = din.readShort() & 0xFFFF;

				byte[] unameBytes = new byte[messageLength -7];
				din.readFully(unameBytes);
				String uname = new String(unameBytes, "UTF-16BE");

				message = new ClientExchangeMessage(addx.getHostAddress(), port, uname);
				break;
			case AbstractMessage.TYPE_HANDSHAKE_MESSAGE:
				byte[] pstrBytes = new byte[HandshakeMessage.PROTOCOL_STRING.getBytes("UTF-16BE");
				din.readFully(pstrBytes);
				String protocolString = new String(pstrBytes, "UTF-16BE");

				if(!protocolString.equals(HandshakeMessage.PROTOCOL_STRING)){
					System.err.println("Recieved invalid handshake protocol string: "
							+ protocolString);
				}

				int listenPort = din.readShort() & 0xFFFF;

				byte[] nameBytes = new byte[messageLength - 3 - HandshakeMessage.PROTOCOL_STRING.getBytes("UTF-16BE").length];

				din.readFully(nameBytes);
				String name = new String(nameBytes, "UTF-16BE");

				message = new HandshakeMessage(name, listenPort);
				break;

			case AbstractMessage.TYPE_DISCONNECT_MESSAGE:
				message = AbstractMessage.DISCONNECT_MESSAGE;
				break;
			case AbstractMessage.TYPE_KEEPALIVE_MESSAGE:
				message = AbstractMessage.KEEPALIVE_MESSAGE;
				break;
			default:
				System.err.println("Unexpected message type when decoding: "+ messageType);
		}
		return message;
	}
}
