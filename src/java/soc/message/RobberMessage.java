/**
*	This class holds the message Data to send to the Plan agent from the NodeAgent
*
**/

package soc.message;

public class RobberMessage {
	
	String request;
	String response;
	boolean[] choices;
	
	int hex = 0;
	int player = 0;
	
	public RobberMessage() {}
	
	public void setResponseHeader(String response) {
		
		this.response = response;
		
	}
	
	public String getResponseHeader() {
		
		return this.response;
		
	}
	
	public void setRequestHeader(String request) {
		
		this.request = request;
	
	}
	
	public String getRequestHeader() {
		
		return this.request;
		
	} 
	
	public void setRobberHex(int hex) {
		
		this.hex = hex;
	}
	
	public int getRobberHex() {
		
		return this.hex;
		
	}
	
	public void setStealPlayer(int stealPlayer) {
		
		this.player = stealPlayer;
	
	}
	
	public int getStealPlayer() {
		
		return this.player;
	}
	
	public void setStealPlayerChoices(boolean[] choices) {
		
		this.choices = choices;
	
	}
	
	public boolean[] getStealPlayerChoices() {
		
		return this.choices;
		
	}
	
} 