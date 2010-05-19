
package soc.robot;

import java.util.*;

class Server extends Thread {
	
	Vector message = new Vector();
	
	public void run() {
		
		while(true) {
			
			createMessage();
			
//			try {
//				
//				Thread.sleep(2000);
//				
//			} catch(Exception e) {}
		}
		
	}
	
	synchronized void createMessage() {
		
		message.add("haseeb");
		
		try {
		wait();
		} catch(Exception e) {
			
			
		}
		
	}
	
	synchronized void readMessage() {
		
		if(message.size() > 0) {
		System.out.println("READ MESSAGE :: "+(String)message.remove(0));
			
			notify();
		
		}
	
	}

}

public class Waiter extends Thread {
	
	Server s;
	
	public Waiter(Server s) {
		
		this.s = s;
	}
	public void run() {
		
		while(true) {
			
			s.readMessage();
			
			try {
			Thread.sleep(10000);
			} catch(Exception e) {}
		}
	}
	
	public static void main(String []ss) {
		
		Server s = new Server();
		
		s.start();
		
		Waiter w = new Waiter(s);
		
		w.start();
		
	}
	
}