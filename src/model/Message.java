package model;

import java.util.Date;

public class Message {

	public String username;
	public Date time;
	public String message;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	public Message(){};
	
	public Message(String username, Date time, String message) {
		super();
		this.username = username;
		this.time = time;
		this.message = message;
	}
		
}
