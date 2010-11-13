/**
*	This is the agent responsible for communication among the agents
*	It basically checks the message and routes the message to the appropriate agent 
*	
**/

package soc.robot;

import java.util.*;

import soc.util.Loggers;

import soc.message.*;

public class MailAgent extends Agent implements Runnable {
	
	private Thread MailAgentThread;
	
	public MailAgent() {
		
		messages = new Vector();
		
		MailAgentThread = new Thread(this, "MailAgent");
		
		MailAgentThread.start();
		
		log = Loggers.MailAgentLogger;
		
	}
	
	public void startThread() {
		
		MailAgentThread.start();
		
	}
	
	public void mailBox(Message message) {
		
		messages.add(message);
		
	}
	
	
	public void run() {
		
		/**
		 * 
		 * 	This thread constantly checks for message <mailbox> and dispatches the message
		 * 	to the intended agent <deliverer>
		 */
		
		while(true) {
	
			
		if(messages.size() > 0) {
			
			
			for(int i = 0; i < messages.size(); i++) {
				
			Message mail = (Message)messages.get(i);
			
			if(mail.getTo().equals("PlanAgent")) {
				
				if(mail.getFrom().equals("TraderAgent"))
					log.info("MESSAGE RECEIVED FROM TRADER AGENT TO PLAN AGENT");
				
				InterfaceAgent.PA.mailBox(mail);
			}
				
			
			
			// since there are multiple NodeAgents we check for which node agent is the message
			
			else if(mail.getTo().equals("NodeAgent")) { 
				
				NodeAgent NA = (NodeAgent)InterfaceAgent.hash.get(mail.getAgentNo());
				NA.mailBox(mail); 
					
			} else if(mail.getTo().equals("TraderAgent")) {
				
				InterfaceAgent.TA.mailBox(mail);
			
			} else if(mail.getTo().equals("CardsAgent")) {
				
				InterfaceAgent.CA.mailBox(mail);
				
			} else if(mail.getTo().equals("RobberAgent")) {
				
				InterfaceAgent.RA.mailBox(mail);
			
			} 
			
				
			
			messages.removeElementAt(i);
			
				}
			
			} else {
				
				try {
				Thread.sleep(100);
				} catch (Exception e) {
					
					e.printStackTrace();
				}
			} 
		
		}
			
		
	}
	
}
