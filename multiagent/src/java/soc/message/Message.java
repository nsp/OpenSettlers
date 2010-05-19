/**
*	This is a Message used to communicate between agents
*
*
**/

package soc.message;

public class Message {
	
	private String from;
	private String to;
	private Object message;
	private int No; // this is only used for NodeAgent
	
	public Message() {}
	
	public void setFrom(String from) {
		
		this.from = from;
	}
	
	public void setAgentNo(int No) {
		
		this.No = No;
		
	}
	
	public int getAgentNo() {
		
		return this.No;
		
	}
	
	public void setTo(String to) {
		
		this.to = to;
		
	}
	
	public void setMessage(Object message) {
		
		this.message = message;
		
	}
	
	public String getFrom() {
		
		return from;
		
	}
	
	public String getTo() {
		
		return to;
		
	}
	
	public Object getMessage() {
		
		return message;
	}
	
}