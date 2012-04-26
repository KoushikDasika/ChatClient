package com.kd.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import com.kd.chat.messaging.ChatMessage;
import com.kd.chat.messaging.ClientExchangeMessage;
import com.kd.chat.messaging.MessageListener;
import com.kd.chat.ui.ConsoleUI;
import com.kd.chat.ui.UIAdapter;
import com.kd.chat.ui.UserInputListener;

public class ChatClient extends Thread implements MessageListener, UserInputListener{

	//port number for incoming connections
	protected final int listenPort;

	//Local Username
	protected final String username;

	//Socet for accepting incoming connections
	protected ServerSocket listenSocket;

	//Flag to keep running the client
	protected boolean keepRunning = true;

	//List of currently-connected clients
	protected final Collection<Client> clients = new ConcurrentLinkedQueue<Client>();

	//Thread pool for handling incoming connections and new client info
	protected final ExecutorService workers = Executors.newCachedThreadPool();

	//Basic console-based user interface
	protected ConsoleUI userInterface = new ConsoleUI();	

	/*Parse command-line arguments and start a single instance of the ChatClient class
	 * @param args 
	 * local port, username, remote host (optional), remote port (optional)	
	 */

	public static void main(String[] args){
		System.out.println();
		System.out.println("Welcome to the Chat Client Matrix");
		System.out.println();

		if(args == null || args.length < 2){
			printUsage();
			return;
		}

		int listenPort = Integer.MIN_VALUE;
		try{
			listenPort = Integer.parseInt(args[0]);
		}
		catch(NumberFormatException nfe){
			System.err.println("Invalid or missing port number. Client cannot start.");
			return;
		}

		if(listenPort <= 1024 || listenPort > 65535){
			System.err.println("Port number is outside the valid range.  Must be between 1024 and 65535");
			return;
		}

		String username = args[1];

		//Create the application with the listen port and username
		ChatClient myClient = new ChatClient(listenPort, username);
		myClient.start();

		if(args.length == 4){
			//parse the port, pass args[2] in as remote hostname
			int remotePort = Integer.MIN_VALUE;
			try{
				remotePort = Integer.parseInt(args[3]);
				if(remotePort <= 1024 || remotePort > 65535){
					System.err.println("Remote port number is out of range");
				}
				else{
					//Leave blank for remote UserName
					myClient.addClient(args[2], remotePort, null);
				}
			}
			catch(NumberFormatException nfe){
				System.err.println("Invalid port number specified for client, won't connect.");
			}
		}
	}

	/*Creates a new chat client listening on specified port and with the provided username
	 *
	 * @param listenPort
	 * 		the port number for incoming client connections
	 * @param username
	 * 		the username to send to the other clients 
	 */
	
	public ChatClient(final int listenPort, final String username){
			this.listenPort = listenPort;
			this.username = username;

			this.userInterface.addUserInputListener(this);
			this.userInterface.start();
	}

	/*Called when remote clients exchange information about other clients with
	 * this client.  Will check for a duplicate connecction first, test it with a 
	 * keep-alive, and ignore this client if the connection is viable.  If the client
	 * is not matched or the old connection is closed, then it will connect to the client
	 *
	 *@param remoteHost
	 *		the hostname/IP address of the new client
	 *@param port
	 *		the listen port number for the new client
	 *@param username
	 *		the username expected from the remote client
	 */

	protected synchronized void addClient(final String remoteHost, final int port, @SuppressWarnings("hiding") final String username){
		//build a new client object
		Client newClient = this.makeClient(remoteHost, port, username);

		//if null, then an exception was thrown, probably couldn't resolve the hostname

		if(newClient == null){
			return ;
		}

		//Check to see if this client is already known
		Client oldClient = this.findDuplicate(newClient);

		//if we alread have this client in our list, then check to make sure if its
		//still alive

		if(oldClient != null){
			if(this.testClient(oldClient)){
				return;
			}
		}

		//connect the socket to the remote client, discard the client on errors
		try{
			newClient.connect();
		}
		catch(IOException ioe){
			System.err.println("unable to connect to " + newClient +
					": " + ioe.getMessage());
			return;
		}
		//Try to handshake, if it succeeds then notify the UI
		if(newClient.performHandshake()){
			this.registerClient(newClient);
			this.clients.add(newClient);
		}

		/*Registers a new client with the local client. Notifies the UI.
		 *
		 * @param client
		 * 		the client to register
		 *
		 */

		protected void registerClient(Client client){
			client.addMessageListener(this);
			client.start();
			this.userInterface.clientConnected(client);
		}

		/*Tests a client for liveness by sending a Keep-Alive message.  If
		 * the send fails, then the client is disconnected, removed from the list
		 * of clients, and the UI is notified.
		 *
		 * @param client
		 * 		the client to test
		 * @return true if the test succeeds, else false
		 */

		protected boolean testClient(Client client){
			try{
				client.sendKeepAliveMessage();
				return true;
			}
			catch(IOException ioe){
				this.clients.remove(client);
				clients.removeMessageListener(this);
				client.disconnect();
				this.userInterface.clientDisconnected(client, ioe.getMessage());
			}
			return false;
		}

		/*Creates a new client from the supplied arguments. If an IOException is
		 * thrown, then null is returned.
		 *
		 * @param remoteHost
		 * 		the hostname/IP	address of the client
		 * @param port
		 * 		the listen port of the client
		 * @param username
		 * 		the username expected form the client
		 * @return the newly-created Client, or null if an exception was thrown
		 */

		protected Client makeClient(String remoteHost, int port, @SuppressWarnings("hiding") String username){
			Client newClient = new Client(remoteHost, port, username, this.username, this.listenPort);
			return newClient;
		}

		/*
		 * Called when a remote client connects to the local client on the listen socket.
		 * Will check for duplicates (just in case the remote peer made a mistake),
		 * test it with a keep-alive, and close the new connection if the old one 
		 * is still viable.  If the new client is not matched or the old connection is 
		 * closed, then it will register the new client.
		 *
		 * @param socket
		 * 		the socket of the newly-connected client
		 */

		protected synchronized void addClient(final Socket socket){
			Client newClient = new Client(socket, this.username, this.listenPort);

			//Need to handshake first since we need to get the remote port info
			//before checking for duplicates

			if(!newClient.performHandshake()){
					newClient.disconnect();
					return;
			}

			//Try to find an old version of this client (same IP/port)
			Client oldClient = findDuplicate(newClient);

			//If we already have this client in our list, then check to make sure its 
			//still live

			if(oldClient != null){
				if(this.testClient(oldClient)){
					return;
				}
			}

			this.registerClient(newClient);
			this.notifyClients(newClients);
			this.clients.add(newClient);

			}

		/*Sends client exchange messages to currently-connected clients.
		 *
		 * @params newClient
		 * 		the newly-added client
		 *
		 */

		protected void notifyClients(Client newClient){
			//Go through each remote client and send a ClientMessage
			for(Iterator<Client> clientIter = this.clients.iterator();
				clientIter.hasNext();){
					Client client = clientIter.next();
					try{
						//Exchange the client information
						clientIter.remove();
						//stop listening to messages from the client
						client.removeMessageListener(this);
						//Disconnect the client
						client.disconnect();
						//Notify the UI of the disconnect
						this.userInterface.clientDisconnected(client, e.getMessage());
					}
				}
		}


		/*Checks to see if client is a duplicate of an already-connected client.
		 * If it is, returns the client that matches, else return null.
		 *
		 * @param client
		 * 		the client to search for
		 * @return the currently-connected duplicate client, or null if no match is found
		 */

		protected Client findDuplicate(Client client){
			for(Client otherClient : this.clients){
				if(client.equals(otherClient)){
					return otherClient;
				}
			}
			return null;
		}


		/*Prints out the basic usage string to System error */
		protected static final void printUsage(){
			StringBuffer usageString = new StringBuffer();
			usageString.append("Usage: <Listen Port><Username>[<Remote IP><Remote Port>]");
			System.err.println(usageString.toString());
		}

		/*Passes the recieved chat message to the user interface */

		@Override
			public void chatMessageArrived(final Client client, final ChatMessage message){
				this.workers.execute(new Runnable(){
					public void run(){
						ChatClient.this.userInterface.chatMessageRecieved(client,
							message.getTimestamp(), message.getMessage());
					}
				});
			}

		/*Adds the exchange chat client if it is not already connected to the local client*/

		@Override
			public void clientMessageArrived(final Client client, final ClientExchangeMessage message){
				this.workers.execute(new Runnable(){
					public void run(){
						ChatClient.this.addClient(message.getIpAddress(),
							message.getPort(), message.getUsername());
					}
				});
			}
			

		/*Deregisters the client from the local client, disconnects it,
		 * and notifies the user interface.
		 */

		@Override
			public void disconnectMessageArrived(final Client client){
				this.workers.execute(new Runnable(){
					public void run(){
						client.removeMessageListener(ChatClient.this);
						client.disconnect();
						ChatClient.this.clients.remove(client);
						ChatClient.this.userInterface.clientDisconnected(client, "User quit.");
					}
				});
			}

			/*Listens for incoming connections, checking every 250ms 
			 * for user request to exit the chat client. 
			 * Incoming connections are handled by worker threads
			 */

			public void run(){
				try{
				//Bind to the local listen port
				this.listenSocket = new ServerSocket(this.listenPort);
				//wait for 250ms at a time
				this.listenSocket.setsoTimeout(250);
				}
				catch(IOException e){
					System.err.println(e.getMessage());
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("Listening on port " + this.listenPort);
				
				while(this.keepRunning){
						try{
							//this will block for 250ms to allow checking for user exit
							//conditions
							final Socket clientSocket = this.listenSocket.accept();
							/*
							 * Pass the actual work of adding the client to another thread,
							 * freeing this thread to accept new clients
							 */
							this.workers.execute(new Runnable(){
								@Override
								public void run(){
									addClient(clientSocket);
								}
							});
						}
						catch(SocketTimeoutException ste){
							//ignored
						}
						catch(IOException e){
							e.printStackTrace();
						}
				}
				this.doShutdown();
			}

			/*Sends the specified message to all currently-connected clients.
			 * If any exception is thrown while sending the message, then that client
			 * is disconnected. Actual work is handled by the worker thread.
			 * Will notify the UI after all the clients have been sent the message
			 *
			 * @see UIAdapter#chatMessageSent(long, String)
			 */

			@Override
			public void broadcastChatMessage(final String input){
				this.workers.execute(new Runnable(){
					public void run(){
						for(Iterator<Client> clientIter = ChatClient.this.clients.
							iterator(); clientIter.hasNext();){
							Client client = clientIter.next();
							try{
								client.sendMessage(input);
							}catch(IOException e){
								clientIter.remove();
								client.disconnect();
								ChatClient.this.userInterface.clientDisconnected(client,
									"Failed to send broadcast chat message/" + e.getMessage());
							}
							}
						ChatClient.this.userInterface.chatMessageSent(System.currentTimeMillis(), input);
					}
				});
			}


			/*
			 * Sends the message to the specified client. If the message cannot be sent 
			 * due to an exception, then the client is disconnected.
			 */

			@Override
			public void privateChatMessage(final Client client, final String message){
				this.workers.execute(new Runnable(){
					try{
						client.sendMessage(message);
					}
					catch(IOException e){
						ChatClient.this.clients.remove(client);
						client.removeMessageListener(ChatClient.this);
						client.disconnect();
						ChatClient.this.userInterface.clientDisconnected(client,
							"Failed to send private chat message/" +
							e.getMessage());
					}
				}
			});
	}

	/*
	 * Sets the run flag to false, which should occur within 250ms.
	 * @see ChatClient#run()
	 */

	@Override
	public void userRequestedShutdown(){
		this.keepRunning = false;
	}
