package model;

import java.io.Serializable;

public class Message implements Serializable {

	// Différents types de message:
	// WHOISIN liste user connecté
	// MESSAGE message classique
	// LOGOUT déconnexion
	public static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2, USERNAME =3;
	private int type;
	private String message;
	
	// constructor
	public Message(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	// getters
	public int getType() {
		return type;
	}
	public String getMessage() {
		return message;
	}
		
}
