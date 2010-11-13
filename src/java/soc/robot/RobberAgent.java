/**
*	This agent is used to plan which hex to place the Robber on 
**/

package soc.robot;

import soc.message.Message;
import soc.message.RobberMessage;
import soc.util.ApplicationConstants;
import soc.util.Loggers;
import soc.util.HashComparator;
import soc.game.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import java.util.Hashtable;

import java.util.Iterator;
import java.util.Map;

public class RobberAgent extends Agent implements Runnable {
	
	public RobberAgent() {
		
		log = Loggers.RobberAgentLogger;
		
		new Thread(this, "RobberAgent").start();
		
		
		
	}
	
	public void mailBox(Message message) {
		
		messages.add(message);
		
	}
	
	
	public void run() {
		
		while(true) {
			
			if(messages.size() > 0) {
				
				for(int i = 0; i < messages.size(); i++) {
					
					Message message = (Message)messages.get(i);
					
					if(message.getMessage() instanceof String) {
						if(message.getMessage().equals("MOVE ROBBER"))
							moveRobber();
					}
					else if(message.getMessage() instanceof RobberMessage) {
						
						RobberMessage rmessage = (RobberMessage)message.getMessage();
						
						if(rmessage.getRequestHeader().equals("STEAL FROM PLAYER"))
							stealFromPlayer(rmessage.getStealPlayerChoices());
					}
					
					messages.remove(i);
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
	
	private void moveRobber() {
		
		/**
		 * 	This method is responsible for getting the coordinate where to place the hex
		 *  
		 *  Loop over all the hexes and on a particular hex build the utility as follows
		 *  if our building not around, better
		 *  if opponent building around, check type and no of buildings around
		 *  check how many players are in lead position 
		 *  check if this is a high probability hex 
		 *  
		 *  then we can get the hex with the highest utility and move the Robber to that hex
		 *  
		 */
		
		Hashtable hex_utility_map = new Hashtable(); 
		
		log.info("ROBBER AGENT :: MOVE ROBBER");
		
        int[] hexes =
        {
            0x33, 0x35, 0x37, 0x53, 0x55, 0x57, 0x59, 0x73, 0x75, 0x77, 0x79,
            0x7B, 0x95, 0x97, 0x99, 0x9B, 0xB7, 0xB9, 0xBB
        };
	    
        // for each hex increment the utility based on various paramters
        
        // loop over all the hexes
        
        for(int hex = 0; hex < hexes.length; hex++) {
        	
        	// for each hex, calculate the utility of the hex and store in hashtable    
        	log.info("calculate utility for hex :: "+hexes[hex]);
        	
        	int utility_at_hex = 0;
  
        	// first we check the coordinates around this hex
        	// to see if we have our building around 
        		
        	int robber_hex = getGame().getBoard().getRobberHex();
        	
        	log.info("Robber hex :: "+robber_hex);
        	
        	if(robber_hex == hexes[hex] || getGame().getBoard().getHexTypeFromCoord(hexes[hex]) == SOCBoard.DESERT_HEX) 
        		continue;
              	
        	utility_at_hex += checkBuildingsAround(hexes[hex]); // this method returns the utility based on the buildings around this hex 
        	
        	log.info("hex addition for hex <buildings around> :: "+hexes[hex]+" "+utility_at_hex);
        	
        	// now we get the players around this hex. 
        	// it would be based on the following rule
        	// if we have our player around this hex, we subtract 
        	// if there is opponent player, we check his current total VP 
        	// if we get a lead player, we increase the utility 
        	// also, then we check how many opponents are there (3 max as 4 total, 4th one is me)
        	
        	// for each opponent player we increase the utility
        	// (Note later in the game we could vary the utility based on the victory points of the opponents)
        	
        	utility_at_hex += checkOpponentsAround(hexes[hex]);
        	
        	log.info("hex addition for hex <check opponents around> :: "+hexes[hex]+" "+utility_at_hex);
        	
        	// now we see the frequency of the hex number 
        	// we get the number on the hex and multiply it with 100 which makes our utility for this hex
        		
        	log.info("NUMBER ON HEX :: "+hexes[hex]+" is :: "+getGame().getBoard().getNumberOnHexFromCoord(hexes[hex]));
        
        	Double probability_of_hex = (Double)ApplicationConstants.DiceProbabilities.DiceProbabilityTable.get(getGame().getBoard().getNumberOnHexFromCoord(hexes[hex]));
        	
        	utility_at_hex += (int)(probability_of_hex.doubleValue() * 100);
        	
        	log.info("hex addition for probability of hex :: "+utility_at_hex);
        	// now we have the utility for this hex which should be bound to the hex number itself 
        	
        	hex_utility_map.put(hexes[hex], utility_at_hex);
        		
        	log.info("final utility at hex :: "+hexes[hex]+" is :: "+utility_at_hex);
        	
        }
        
    	// now we sort the hashtable based on the values and then
    	// the top element gives us the hex # to move the robber to 
    	
     	// Put keys and values in to an arraylist using entryset
    	ArrayList myArrayList=new ArrayList(hex_utility_map.entrySet()); 
    	
    	// Sort the values based on values first and then keys.
    	Collections.sort(myArrayList, new HashComparator()); 
    	
    	// check the result
    	
    	// Show sorted results
    	Iterator itr=myArrayList.iterator();
    	int key=0;
    	int value=0;
    	
    	while(itr.hasNext()){ 
    		
    		Map.Entry e=(Map.Entry)itr.next(); 

    		key = ((Integer)e.getKey()).intValue();
    		value = ((Integer)e.getValue()).intValue();

    		log.info("FINAL HASHTABLE :: "+key+", "+value);
    			
    		// create the message to move the robber and send to plan agent
    		
    		Message message = new Message();
    		
    		message.setFrom("RobberAgent");
    		message.setTo("PlanAgent");
    		
    		RobberMessage rmessage = new RobberMessage(); 
    		
    		rmessage.setResponseHeader("PLACE ROBBER ON HEX");
    		rmessage.setRobberHex(key);
    		
    		message.setMessage(rmessage); // the hex no to place the robber
    		
    		InterfaceAgent.MA.mailBox(message);
    		
    	}
        
    	
        
	}
	
	
	/**
	 * for each building found on the adjacent edges or nodes, this method adds penalty to utility
	 * 
	 */
	
	private int checkBuildingsAround(int hex) {
		
		log.info("checkBuildingsAround :: "+hex);
		
		int utility_at_hex = 0;
		
		// the coordinates below can tell us if we have our building 
		// on the coordinates below
		
		int top_coordinate = hex + 1;
		
		int top_left_coordinate = hex -16;
		
		int top_right_coordinate = hex +18;
		
		int bottom_left_coordinate = hex -1;		
	
		int bottom_right_coordinate = hex +33;
		
		int bottom_coordinate = hex +16;
		
		// the coordinates below are the edges to find if our road is on the edge
		
		// you see a pattern out there :)  
		
		int top_left_edge = hex - 16;
		
		int top_right_edge = hex + 1;
		
		int middle_left_edge = hex - 17;
		
		int middle_right_edge = hex + 17;
		
		int bottom_left_edge = hex - 1;
		
		int bottom_right_edge = hex + 16;
		
		// now we need to check whether we have our building or opponents building around 
		// it would be calculated for each hex,
		// check if we have our building and what type
		// if opponents what type
		
		utility_at_hex+= building_utility_at_hex(top_coordinate);
		utility_at_hex+= building_utility_at_hex(top_left_coordinate);
		utility_at_hex+= building_utility_at_hex(top_right_coordinate);
		utility_at_hex+= building_utility_at_hex(bottom_left_coordinate);
		utility_at_hex+= building_utility_at_hex(bottom_right_coordinate);
		utility_at_hex+= building_utility_at_hex(bottom_coordinate);
		
		// now we check for if we have our or opponents road on the edge
		
		utility_at_hex+= road_utility_at_hex(top_left_edge);
		utility_at_hex+= road_utility_at_hex(top_right_edge);
		utility_at_hex+= road_utility_at_hex(middle_left_edge);
		utility_at_hex+= road_utility_at_hex(middle_right_edge);
		utility_at_hex+= road_utility_at_hex(bottom_left_edge);
		utility_at_hex+= road_utility_at_hex(bottom_right_edge);
		
		log.info("buildings around utility for hex :: "+hex+" is "+utility_at_hex);
		
		return utility_at_hex;
		
	}
	
	/** 
	 * this method checks if there is our or opponent building on this coordinate
	 * and returns the appropriate utility
	 */
	
	private int building_utility_at_hex(int node_coordinate) {

		if(!getPlayer().isPotentialSettlement(node_coordinate)) {
			
			Vector settlements = getPlayer().getSettlements();
		
			for(int i = 0; i < settlements.size(); i++) {
				
				SOCSettlement soc_settlement = (SOCSettlement)settlements.get(i);
			
				int settlement_coordinate = soc_settlement.getCoordinates();
				
				if(settlement_coordinate == node_coordinate) { // if it is our settlement
					
					//log.info("SETTLEMENT IN VICINITY");
					
					log.info("BUILDING UTILITY AT COORDINATE (for settlement) :: "+node_coordinate+" is "+ApplicationConstants.Robber.OUR_SETTLEMENT_PENALTY);
					
					return ApplicationConstants.Robber.OUR_SETTLEMENT_PENALTY; 

					//break;
				} 
					
				
			}
			
			
			
		}

		if(!getPlayer().isPotentialCity(node_coordinate)) {
			
			Vector cities = getPlayer().getCities();
			
			for(int i = 0; i < cities.size(); i++) {
				
				SOCCity soc_city = (SOCCity)cities.get(i);
				
				int cities_coordinate = soc_city.getCoordinates();
				
				if(cities_coordinate == node_coordinate) {
					
					//utility = ApplicationConstants.Game.CITY_IN_VICINITY_UTILITY;
					//log.info("CITY IN VICINITY");
					log.info("BUILDING UTILITY AT COORDINATE (for city) :: "+node_coordinate+" is "+ApplicationConstants.Game.CITY_IN_VICINITY_UTILITY);
					
					return ApplicationConstants.Robber.OUR_CITY_PENALTY;
					
					//break;
				} 
					
				
			}
			
		}
		
		// check if opponent has a settlement or city on this hex.
		// for this we fetch all the settlements or cities and if we find
		// something it means its opponents building and then we have an 
		// advantage
		
		Vector settlements_on_board = getGame().getBoard().getSettlements();
		
		// loop over all the settlements and check if the coordinate is in the list of settlements
		
		for(int i = 0; i < settlements_on_board.size(); i++) {
			
			SOCSettlement soc_settlement = (SOCSettlement)settlements_on_board.get(i);
			
			if(soc_settlement.getCoordinates() == node_coordinate) {
				
				log.info("BUILDING UTILITY AT COORDINATE (for settlement opponent) :: "+node_coordinate+" is "+ApplicationConstants.Robber.OPPONENT_SETTLEMENT_ADVANTAGE);
				return ApplicationConstants.Robber.OPPONENT_SETTLEMENT_ADVANTAGE;
				
			} 
		}
		
		// loop over all the cities and check if the settlement is in the list of cities
		
		Vector cities_on_board = getGame().getBoard().getCities();
		
		for(int i = 0; i < cities_on_board.size(); i++) {
			
			SOCCity soc_city = (SOCCity)cities_on_board.get(i);
			
			if(soc_city.getCoordinates() == node_coordinate) {
				
				log.info("BUILDING UTILITY AT COORDINATE (for city opponent) :: "+node_coordinate+" is "+ApplicationConstants.Robber.OPPONENT_CITY_ADVANTAGE);
				return ApplicationConstants.Robber.OPPONENT_CITY_ADVANTAGE;
				
			}
			
		}
		
		return 0;
	
	}

	/** 
	 * this method checks if there is our or opponent road on this coordinate
	 * and returns the appropriate utility
	 */
	
	private int road_utility_at_hex(int edge_coordinate) {
		
		log.info("CALLING ROAD UTILITY AT EDGE :: "+edge_coordinate);
		
		if(!getPlayer().isPotentialRoad(edge_coordinate)) {
			
			Vector roads = getPlayer().getRoads();
		
			for(int i = 0; i < roads.size(); i++) {
				
				SOCRoad soc_road = (SOCRoad)roads.get(i);
			
				int road_coordinate = soc_road.getCoordinates();
				
				if(road_coordinate == edge_coordinate) { // if it is our settlement
					
					log.info("ROAD IN VICINITY");
					
					log.info("ROAD BUILDING UTILITY AT EDGE COORDINATE (OUR ROAD) :: "+edge_coordinate+" is "+ApplicationConstants.Robber.OUR_ROAD_PENALTY);
					return ApplicationConstants.Robber.OUR_ROAD_PENALTY; 
			
				} 
					
				
			}
			
			
			
		}

		
		// check if opponent has a settlement or city on this hex.
		// for this we fetch all the settlements or cities and if we find
		// something it means its opponents building and then we have an 
		// advantage
		
		Vector roads_on_board = getGame().getBoard().getRoads();
		
		// loop over all the roads and check if the edge is in the list of roads
		
		for(int i = 0; i < roads_on_board.size(); i++) {
			
			SOCRoad soc_road = (SOCRoad)roads_on_board.get(i);
			
			if(soc_road.getCoordinates() == edge_coordinate) {
				
				log.info("ROAD BUILDING UTILITY AT EDGE COORDINATE (OPPONENT ROAD) :: "+edge_coordinate+" is "+ApplicationConstants.Robber.OPPONENT_ROAD_ADVANTAGE);
				return ApplicationConstants.Robber.OPPONENT_ROAD_ADVANTAGE;
				
			} 
		}
		
			
		return 0;
	
	}
	
	/**
	 * This method checks for which players are around and increases the utility 
	 * based on the information
	 * 
	 * could be upgraded to check the current total VPs of the opponents to 
	 * calculate the utility for each opponent 
	 *  
	 */
	
	private int checkOpponentsAround(int hex) {
		
		log.info("OPPONENT AROUND HEX :: "+hex);
		
		int utility = 0;
		
		Vector players_on_hex = getGame().getPlayersOnHex(hex);
		
		// loop on the players on this hex
		
		for(int players = 0; players < players_on_hex.size(); players++) {
			
		
			SOCPlayer player = (SOCPlayer)players_on_hex.get(players);
			
			// check if this player is our
			if(player.getPlayerNumber() == getPlayer().getPlayerNumber()) {
				
				utility	+= ApplicationConstants.Robber.OUR_PLAYER_PENALTY;	
				
				
			} else { // if this is the opponent player
				
				// check if this player has the most VPs, else just the default advantage
					
				boolean topOpponent = checkIfTopOpponent(player);
				
				if(topOpponent) {
					
					log.info("WE FOUND TOP OPPONENT AROUND :: hex :: "+hex);
					
					utility+= ApplicationConstants.Robber.OPPONENT_PLAYER_TOP_ADVANTAGE;
				}
				else
					utility+= ApplicationConstants.Robber.OPPONENT_PLAYER_ADVANTAGE;
				
			} 
				 
		}

		log.info("OPPONENT AROUND THE HEX UTILITY FOR HEX :: "+hex+" is "+utility);
		
		return utility;
		
	}
	
	private boolean checkIfTopOpponent(SOCPlayer player) {
		
		SOCPlayer[] players = getGame().getPlayers();
		
		SOCPlayer topPlayer = null;
		
		// loop over all the players and find the player with the top VPs
		
		for(int playerr = 0; playerr < players.length; playerr++) {
			
			int maxVP = 0; // store max VPs
			
			if(players[playerr].getTotalVP() > maxVP) { 
				
				maxVP = players[playerr].getTotalVP();
				
				topPlayer = players[playerr];
				
			}
			
		}
		
		// now topPlayer holds the player with max VPs
		
		if(topPlayer.getPlayerNumber() == player.getPlayerNumber())
			return true;
		
		return false;
	
	}
	
	/**
	 * 
	 * this is the method which determines which is the player we need to steal resource
	 * from
	 *
	 */
	
	public void stealFromPlayer(boolean[] steal_player_choices) {
		
		// an initial attempt is to steal from the top player
		
		int topPlayer = 0;
		
		int max = 0;
		
		for(int i = 0; i < steal_player_choices.length; i++) {
			
			
			
			if(i == getPlayer().getPlayerNumber())
				continue;
			
			if(steal_player_choices[i]) { // if we can steal from this player
				
				if(getGame().getPlayer(i).getTotalVP() > max) {
					
					max = getGame().getPlayer(i).getTotalVP();
					topPlayer = getGame().getPlayer(i).getPlayerNumber();
					log.info("PLAYER :: "+i+" is the highest player");
					
				}
				
				
			}
			
		}
		
//		SOCPlayer [] players = getGame().getPlayers();
//		
//		for(int i = 0; i < players.length; i++) { // loop over all the players and fetch the player with the highest VPs except our player
//			
//			if(players[i].getPlayerNumber() == getPlayer().getPlayerNumber()) {
//				log.info("THIS IS MY PLAYER SO CONTINUE");
//				continue;
//			}
//				
//			if(players[i].getTotalVP() > max) {
//				
//				max = players[i].getTotalVP();
//				topPlayer = players[i].getPlayerNumber();
//				log.info("PLAYER :: "+i+" is the highest player");
//				
//			}
//			
//		}
		
		// send the message with the player number
		
		Message message = new Message();
		
		message.setFrom("RobberAgent");
		message.setTo("PlanAgent");
		
		RobberMessage rmessage = new RobberMessage();
		
		rmessage.setResponseHeader("PLAYER TO STEAL");
		rmessage.setStealPlayer(topPlayer);
		
		log.info("PLAYER TO STEAL RESOURCE FROM :: "+topPlayer);
		
		message.setMessage(rmessage);
		
		InterfaceAgent.MA.mailBox(message);
		
		
		
	}
		
}