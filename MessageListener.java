


/*
 * Interface for classes that want to respond to messages from remote chat clients
 *
 */

public interface MessageListener{
	public void chatMessageArrived(final Client client, final ChatMessage message);

	public void clientMessageArrived(final Client client, final ClientExchangeMessage message)
		public void disconnectMessageArrived(final Client client);
}
