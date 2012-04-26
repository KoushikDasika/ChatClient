
import java.io.UnsupportedEncodingException;
import java.util.Date;

public class ChatMessage extends AbstractMessage{
	protected final long timestamp;

	protected final String username;

	protected final String message;

	public ChatMessage(final long timestamp, final String username,
				final String message) throws UnsupportedEncodingException{

		super(13 + username.getBytes("UTF-16BE").length +
				message.getBytes("UTF-16BE").length,
				AbstractMessage.TYPE_CHAT_MESSAGE);
		
		this.timestamp = timestamp;
		this.username = username;
		this.message = message;
	}

	public long getTimestamp(){
		return this.timestamp;
	}

	public String getUsername(){
		return this.username;
	}

	public String getMessage(){
		return this.message;
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append(' ').append(this.username).append('@').append(
				new Date(this.timestamp)).append(": ").append*this.message);
		return sb.toString();
	}
}
