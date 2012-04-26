package com.kd.chat.ui;
import com.kd.chat.Client;


public interface UIAdapter{
	public void chatMessageRecieved(Client fromClient, long timestamp, String message);

	public void chatMessageSent(long timestamp, String message);

	public void messageNotSent(Client client, String message, String reason);

	public void clientConnected(Client connectedClient);

	public void clientDisconnected(Client disconnectedClient, String reason);
}
