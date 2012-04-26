
import java.io.UnsupportedEncodingException;

public class ClientExchangeMessage extends AbstractMessage{
	protected final String ipAddress;

	protected final int port;

	protected final String username;

	public ClientExchangeMessage(final String ipAddress, 
			final int port, final String username) throws UnsupportedEncodingException{
		
		super(7+username.getBytes("UTF-16BE").length, AbstractMessage.TYPE_CLIENT_EXCHANGE_MESSAGE);
		this.ipAddress = ipAddress;
		this.port = port;
		this.username = username;
	}

	public String getIpAddress(){
		return this.ipAddress;
	}

	public int getPort(){
		return this.port;
	}

	public String getUsername(){
		return this.username;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString()).append(' ').append(this.username).
			append('@').append(this.ipAddress).append(':').append(this.port);

		return sb.toString();
	}
}
