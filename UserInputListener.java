
public interface UserInputListener{
	public void broadcastChatMessage(String message);

	public void privateChatMessage(Client client, String message);

	public void userRequestedShutdown();
}
