/**
*	This class is the class which delivers common stuff to Agents
*
*
**/

package soc.robot;

import java.util.Vector;

import org.apache.log4j.Logger;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCResourceSet;
import soc.message.Message;

public class Agent {
	
	private static SOCPlayer player;
	private static SOCGame game;
	protected Vector messages;
	protected Logger log;
	protected SOCResourceSet player_resources;
	protected Vector player_settlements;
	protected Vector player_cities;
	
	
	public Agent() {
		
		messages = new Vector();
	}
	
	public Agent(SOCPlayer player, SOCGame game) {
		
		Agent.game = game;
		Agent.player = player;
		
		messages = new Vector();
	
	}
	
	public void mailBox(Message message) {
		
		messages.add(message);
		
	}
	
	
	public void setPlayer(SOCPlayer player) {
		
		Agent.player = player;
	}
	
	public void setGame(SOCGame game) {
		
		Agent.game = game;
		
	}
	
	public SOCPlayer getPlayer() {
		
		return Agent.player;
		
	}
	
	public SOCGame getGame() {
		
		return Agent.game;
		
	}
	
	 

}