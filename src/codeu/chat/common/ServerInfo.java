package codeu.chat.common;

import java.io.IOException;
import codeu.chat.util.Uuid;

public final class ServerInfo {
	private final static String SERVER_VERSION = "1.0.0";

	public final Uuid version;
	public ServerInfo(){
		try {
			 Uuid.parse(SERVER_VERSION);
			 this.version = Uuid.parse(SERVER_VERSION);
			 return;
		}catch(Exception e){
			this.version = null;
		}
		
	}
	public ServerInfo(Uuid version){
		this.version = version;
	}
}