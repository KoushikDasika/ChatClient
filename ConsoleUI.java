

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;



public class ConsoleUI extends Thread implements UIAdapter{
	protected final Collection<UserInputListener> listeners =
		new ConcurrentLinkedQueue<Client>();

	protected boolean keepRunning = true;

	public void addUserInputListeners(UserInputListener listener){
		this.listeners.add(listener);
	}

	public void removeUserInputListener(UserInputListener listener){
		this.listeners.remove(listener);
	}

	public void chatMessageRecieved(Client fromClient, long timestamp, String message){
		System.out.println(fromClient.getUsername() + ": " + message);
	}


	@Override
	public void run(){
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Send broadcast messages by typing and htting <Enter>");
		System.out.println("Send private messages like this: @username message to send ");
		System.out.println("Type \"quit\" to exit");

		while(this.keepRunning){
			try{
				String line = reader.readLine();
				if("quit".equalsIgnoreCase(line)){
					for(UserInputListener listener: this.listeners){
						listener.userRequestedShutdown();
					}
					break;
				}
				if(line == null){
					continue;
				}
				if(line.startsWith("@")){
					int usernameBreak = line.indexOf(' ');
					String username = line.substring(1, usernameBreak);
					String message = line.substring(usernameBreak + 1, line.length());
					Client theClient = null;
					for(Client client : this.knownClients){
						if(client.getUsername().equals(username)){
							theClient = client;
							break;
						}
					}
					if(theClient == null){
						System.out.println("Could not find user \" + username + "\".");
						continue;
					}

					for(UserInputListener listener: this.listeners){
						listener.privateChatMessage(theClient, message);
					}
				}
				else{
					for(UserInputListener listener : this.listeners){
						listeners.broadcastChatMessage(line);
					}
				}
			}
			catch(IOException e){
				e.printStackTrace();
				continue;
			}
		}
	}

	@Override 
	public void chatMessageSent(long timestamp, String message){
	}

	@Override
	public void clientConnected(Client connectedClient){
		System.out.println("Connected to " + connectedClient);
		this.knownClients.add(connectedClient);
	}

	@Override
	public void messageNotSent(Client client, String message, String reason){
		System.out.println("The following message could not be sent to "
				+ client + ": \n"+ message+
				(reason == null ? "\n" : "\n\nReason: " + reason));
	}
}
