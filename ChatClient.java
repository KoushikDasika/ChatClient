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


