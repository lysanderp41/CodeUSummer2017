package codeu.chat.common;

import java.io.IOException;
import codeu.chat.util.Uuid;
import codeu.chat.util.Time;

public final class ServerInfo {
  
  private final static String SERVER_VERSION = "1.0.0";
  public Uuid version;
  public final Time startTime;

  public ServerInfo(){
  	this.version = null;
    this.startTime = Time.now();
	try {
	  this.version = Uuid.parse(SERVER_VERSION);
	  return;
	} catch (Exception e) {
	  this.version = null;
	}	
}
  public ServerInfo(Uuid version) {
    this.version = version;
    this.startTime = null;
	}
  
  public ServerInfo(Time startTime) {
    this.startTime = startTime;
   }
}
