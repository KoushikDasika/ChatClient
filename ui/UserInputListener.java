package com.kd.chat.ui;
import com.kd.chat.Client;


public interface UserInputListener{
	public void broadcastChatMessage(String message);

	public void privateChatMessage(Client client, String message);

	public void userRequestedShutdown();
}
