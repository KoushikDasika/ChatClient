package com.kd.chat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.kd.chat.messaging.AbstractMessage;
import com.kd.chat.messaging.ChatMessage;
import com.kd.chat.messaging.ClientExchangeMessage;
import com.kd.chat.messaging.HandshakeMessage;
import com.kd.chat.messaging.MessageListener;


/*
 * Representation of a remote chat client connected to this client.
 * Handles reading messages from the remote client, which are passed to any
 * registered MessageListener interfaces.
 *
 * @author Robert Moore
 *
 */

public class Client extends Thread{
	//The socket connected to the remote client
	protected final Socket socket;
	//username of this client
	protected String username;
	//IP address of this client
	protected final String ipAddress;
	//listen port of this client the value may not be known until after the 
	//handshake is recieved
	protected int port = -1;
	//the username of the local client. Used for generating handshake message
	protected final String localUsername;
	//listen port of the local client. used for generating handshake messages
	protected final int localPort;
	//Collection of MessageListener interfaces that should be notified of
	//recieved messages
	protected final Collection<MessageListener> listeners = new ConcurrentLinkedQueue<MessageListener>();

	protected boolean keepRunning = true;

	/*
	 * Creates a new Client with the specified parameters.  Does not connect to the remote
	 * client until {@link #connect()} is called
	 *
	 * @param ipAddress
	 * 		the IP address or hostname of this client.
	 * @param port
	 * 		the listen port of this client
	 * @param username
	 * 		the username of this client, may be null if its not known
	 * @param localUsername
	 * 		the username of the local client
	 * @param localPort
	 * 		the listen port of the local client
	 */

	public Client(final String ipAddress, final int port,
					final String username, final String localUsername,
					final int localPort){
		this.ipAddress = ipAddress;
		this.port = port;
		this.username = username;
		this.localUsername = localUsername;
		this.localPort = localPort;
		this.socket = new Socket();
	}

	public Client(final Socket socket, final String localUsername,
					final int localPort){
		this.socket = socket;
		this.localUsername = localUsername;
		this.localPort = localPort;
		//Grab the actual address in case a hostname was provided
		this.ipAddress = this.socket.getInetAddress().getHostAddress();
	}

	public void connect() throws IOException{
		if(this.socket != null && !this.socket.isConnected()){
			this.socket.connect(new InetSocketAddress(this.ipAddress, this.port));
		}
	}

	public synchronized boolean performHandshake(){
		HandshakeMessage sentMessage = null;

		try{
			sentMessage = new HandshakeMessage(this.localUsername, this.localPort);
		}catch(UnsupportedEncodingException uee){
			System.err.println("Unable to encode handshake");
			System.err.println(uee.getMessage());
			uee.printStackTrace(System.err);
			return false;
		}

		AbstractMessage recievedMessage = null;

		//keep reading messages until a HandshakeMessage is recieved

		do{
			try{
				recievedMessage = AbstractMessage.decodeMessage(this.socket.getInputStream());
				if(recievedMessage == null){
					//Allow other threads to issue before trying again
					Thread.yield();
					continue;
				}
			}catch(IOException e){
				System.err.println("Unable to read handshake from remote client");
				System.err.println(e.getMessage());
				e.printStackTrace();
				return false;
			}
		}
		//Keep looping until we've recieved a handshake from a client
		while(recievedMessage == null);

		//Didn't know the username (probably created from a socket) so just assign it

		if(this.username == null){
			this.username = ((HandshakeMessage) recievedMessage).getUsername();
		}

		//Verify that the username matches the expected value
		else{
			if(!this.username.equals(((HandshakeMessage) recievedMessage).getUsername())){
				System.err.println("Handshake username did not match " + this.username + " <->" + ((HandshakeMessage) recievedMessage).getUsername());
				return false;
			}
		}

		if(this.port < 0){
			this.port = ((HandshakeMessage) recievedMessage).getListenPort();
		}
		else if(this.port != ((HandshakeMessage) recievedMessage).getListenPort()){
			System.err.println("Handshake ports don't match " + this.port +
					"<->" + ((HandshakeMessage) recievedMessage).getListenPort());
			return false;
		}
		return true;
	}

	public void disconnect(){
		this.keepRunning = false;

		if(this.socket != null && !this.socket.isClosed()){
			try{
				this.socket.close();
			}
			catch(IOException e){
			}
		}
		else{
			System.err.println("Already disconnected");
		}
	}

	public synchronized void sendMessage(final String message) throws IOException{
		ChatMessage cMessage = new ChatMessage(System.currentTimeMillis(), 
			this.localUsername, message);
		AbstractMessage.encodeMessage(cMessage, this.socket.getOutputStream());
	}
	
	public synchronized void sendClient(final Client otherClient) throws IOException{
		ClientExchangeMessage cMessage = new ClientExchangeMessage(otherClient.getIpAddress(), otherClient.getPort(), otherClient.getUsername());
		AbstractMessage.encodeMessage(cMessage, this.socket.getOutputStream());
	}

	public synchronized void sendKeepAliveMessage() throws IOException{
		AbstractMessage.encodeMessage(AbstractMessage.KEEPALIVE_MESSAGE,
			this.socket.getOutputStream());
	}

	public void addMessageListener(final MessageListener listener){
		this.listeners.add(listener);
	}

	public void removeMessageListener(final MessageListener listener){
		this.listeners.remove(listener);
	}

	@Override
	public void run(){
		while(this.keepRunning){
			try{
			final AbstractMessage message = AbstractMessage.decodeMessage(this.socket.getInputStream());
			if(message == null){
				try{
					Thread.sleep(5);
				}
				catch(InterruptedException ie){
				}
				continue;
			}
			if(message.getType() == AbstractMessage.TYPE_CHAT_MESSAGE){
				for(MessageListener listener: Client.this.listeners){
					listener.chatMessageArrived(Client.this,
							(ChatMessage) message);
				}
			}
			else if(message.getType() == AbstractMessage.TYPE_CLIENT_EXCHANGE_MESSAGE){
				for(MessageListener listener : Client.this.listeners){
					listener.clientMessageArrived(Client.this,
							(ClientExchangeMessage) message);
				}
			}
			else if(message.getType() == AbstractMessage.TYPE_DISCONNECT_MESSAGE){
				for(MessageListener listener: Client.this.listeners){
					listener.disconnectMessageArrived(Client.this);
				}
			}

			}
			catch(Exception e){
				this.keepRunning = false;
				System.err.println(this + ": Caught exception while reading from client.");
				System.err.println(e.getMessage());
				e.printStackTrace(System.err);

				for(MessageListener listener: Client.this.listeners){
					listener.disconnectMessageArrived(this);
				}
			}
		}
	}

	public String getUsername(){
		return this.username;
	}

	public Socket getSocket(){
		return this.socket();
	}

	public String getIpAddress(){
		return this.ipAddress;
	}

	public int getPort(){
		return this.port;
	}

	@Override
	public boolean equals(Object o){
		if(o instanceof Client){
			return this.equals((Client) o);
		}
			return super.equals(o);
	}

	public boolean equals(Client client){
		try{
			InetAddress myAddress = InetAddress.getByName(this.ipAddress);
			InetAddress otherAddress = InetAddress.getByName(client.ipAddress);
			if(myAddress.equals(otherAddress) && this.port == client.port){
				return true;
			}
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		return false;
	}

	@Override
	public int hashCode(){
		//a hacky hack to stope compiler from complaining
		return this.socket.hashCode() ^ this.port;
	}

	@Override
	public String toString(){
		return (this.username == null ? "Unknown client" : this.username) + "@" 
			+ this.ipAddress + ":" + this.port;
	}
}
