/**
*	This class is the core class which sends the decision on the action to be taken
*	to the Interface Agent
*
*
**/

package soc.robot;

import java.util.*;

import soc.message.*;
import soc.util.ApplicationConstants;
import soc.game.*;

import soc.util.Loggers;
import soc.util.PlanSorter;

public class PlanAgent extends Agent implements Runnable {
	
	private Thread PlanAgentThread;
	//private Logger log;
	private int messageStackFromNodeAgents = 0;
	private InterfaceAgent IA;
	private Vector parent = new Vector(); 
	public Vector plan_left = new Vector();
	
	private Message [] node_messages = new Message[ApplicationConstants.Game.TOTAL_NODE_AGENTS];
	
	public PlanAgent(InterfaceAgent IA) {
		
		this.IA = IA;
		
		messages = new Vector();
	
		PlanAgentThread = new Thread(this, "PlanAgent");
		
		log = Loggers.PlanAgentLogger;
		
	}
	
	public PlanAgent(SOCPlayer player, SOCGame game) {
		
		super(player, game);
		
		
	}
	
	public void startThread() {
		
		PlanAgentThread.start();
		
	}
	
	public boolean isAlive() {
		
		return PlanAgentThread.isAlive();
		
	}
	
	
	public void setPlayerSettlements(Vector player_settlements) {
		
		this.player_settlements = player_settlements; 
		
	}
	
	public void setPlayerCities(Vector player_cities) {
		
			this.player_cities = player_cities; 
		
	}
	
	public void setPlayerResources(SOCResourceSet player_resources) {
		
		this.player_resources = player_resources; 
		
	}
	
	
	/**
	 * This method is responsible for sending the messages to all the Node Agents
	 * to submit bid for placing initial settlements
	 * 
	 */
	
	public String buildInitialSettlements() {
		
		try {
			
			NodeAgent [] NA = InterfaceAgent.getNodeAgents();
			
			//Hashtable ResourceFrequencyHash = getFrequenciesForResources(null);
			
			log.info("THERE ARE "+NA.length+" NODE AGENTS");
			
			for(int i = 0; i < NA.length; i++) {
			
				// send messages to the MailAgent to dispatch to NodeAgents to send a bid to PlanAgent
			
				Message message = new Message();
				
				message.setFrom("PlanAgent");
				message.setTo("NodeAgent");
				message.setAgentNo(NA[i].getNode());
				message.setMessage("SUBMIT BID TO PLACE INITIAL SETTLEMENTS");
				
				InterfaceAgent.MA.mailBox(message); // message sent to MailAgent
				
			}
			
			// jus to check the building speed algorithm <basic algorithm>
			
			
			//this.calculateBuildingSpeed(new SOCSettlement(getPlayer(), 105));
			
			} catch(Exception e) {
				
				e.printStackTrace();
				
			}
			
			return null;
		
		
		
	}
	
	
	/**
	 * this method receives a call from the Interface agent at the start of the turn
	 * and returns the result
	 * 
	 *
	 * @return action to be taken
	 */
	
	/*public String handleTurn() {
		
		try {
		
		NodeAgent [] NA = InterfaceAgent.getNodeAgents();
		
		//Hashtable ResourceFrequencyHash = getFrequenciesForResources(null);
		
		for(int i = 0; i < NA.length; i++) {
			
			
			// send the player and game info to each node agent
			
			
			
			// send messages to the MailAgent to dispatch to NodeAgents to send a bid to PlanAgent
		
			Message message = new Message();
			
			message.setFrom("PlanAgent");
			message.setTo("NodeAgent");
			message.setAgentNo(NA[i].getNode());
			message.setMessage("Submit Bid");
		
			//NA[i].setResourceFrequency(ResourceFrequencyHash);
			
			InterfaceAgent.MA.mailBox(message); // message sent to MailAgent
			
			
			
		}
		
		// jus to check the building speed algorithm <basic algorithm>
		
		//SOCCity soc_settlement = new SOCCity(getPlayer(),105);
		
		//calculateBuildingSpeed(soc_settlement);
		
		
		} catch(Exception e) {
			
			e.printStackTrace();
			
		}
		
		return null;
		
	} */
	
	public void mailBox(Message message) {
		
		log.info("IN MAILBOX OF PLANAGENT");
		
		messages.add(message);
		
	}
	

	public void run() {
		/**
		 * 
		 * 	this thread is responsible for reading the incoming messages 
		 * 	and then dispatching the message to appropriate destination 
		 * 
		 */
		
		while(true) {
			
			if(messages.size() > 0) {
			
				log.info("THERE ARE MESSAGES");
				
			for(int i = 0; i < messages.size(); i++) {
				
				Message mail = (Message)messages.get(i);
				
				log.info("MAIL IS FROM :: "+mail.getFrom());
				
				if(mail.getFrom().equals("InterfaceAgent")) {
					
					log.info("MESSAGE RECEIVED FROM INTERFACE AGENT >> "+(String)mail.getMessage());
					
					String message = (String)mail.getMessage();
					
					if(message.equals("PLACE INITIAL SETTLEMENT")) 
						buildInitialSettlements();
					else if(message.equals("PLACE INITIAL ROAD"))
						buildInitialRoads(mail);
					else if(message.equals("MAKE MOVE"))
						planMove(); // this method is responsible for planning the move 
					
					
					
				} else if(mail.getFrom().equals("NodeAgent")) {
					
					NodeMessage nmessage = (NodeMessage)mail.getMessage();
					
					if(nmessage.getResponseHeader().equals("INITIAL SETTLEMENT UTILITY")) {
							
						this.planOfActionForInitialSettlement(mail);
						
					} else if(nmessage.getResponseHeader().equals("INITIAL ROAD UTILITY")) {
						
						this.planOfActionForInitialRoad(mail);
						
					} else if(nmessage.getResponseHeader().equals("MOVE UTILITY")) {
						
						this.planOfActionForBestMove(mail);
						
					}
					
					
				} else if(mail.getFrom().equals("TraderAgent")) {
					
					log.info("MESSAGE RECEIVED FROM TRADER AGENT IN PLAN AGENT");
					
					TraderMessage tmessage = (TraderMessage)mail.getMessage();
					
					if(tmessage.getResponseHeader().equals("TRADE OFFER")) {
						
						log.info("MESSAGE RECEIVED FROM TRADER AGENT TO MAKE AN OFFER");
						
						this.IA.currentplan = tmessage.getPlan();
						
						this.makeTradeOffer(tmessage.getGiveResourceSet(), tmessage.getGetResourceSet());
						
					} else if(tmessage.getResponseHeader().equals("CANNOT OFFER TRADE")) {
						
						noTradeOffer();
				
					} else if(tmessage.getResponseHeader().equals("TWO/THREE ONE TRADE OFFER")) {
						
						this.IA.currentplan = tmessage.getPlan();
						
						log.info("CURRENT PLAN IS :: "+this.IA.currentplan);
						
						makeBankTradeOffer(tmessage.getGiveResourceSet(), tmessage.getGetResourceSet());
						
						
					} 
						
						
					
					
				} else if(mail.getFrom().equals("CardsAgent")) {
					
					if(mail.getMessage() instanceof CardsMessage) {
					
						CardsMessage cmessage = (CardsMessage)mail.getMessage();
					
						if(cmessage.getResponseHeader().equals("CARDS TO DISCARD"))
							this.IA.discard(cmessage.getDiscardResourceSet()); // discard the extra resource cards
						
						else if(cmessage.getResponseHeader().equals("PLAY MONOPOLY CARD")) {
							
							// we set the resource we need to monopolize
							this.IA.resource_to_monopolize = cmessage.getMonopolizedResource();
							
							this.IA.playCard(SOCDevCardConstants.MONO);
							
						} else if(cmessage.getResponseHeader().equals("PLAY DISCOVERY CARD")) {

							// we set the resource set we need to discover
							this.IA.discovery_resource_set = cmessage.getDiscoveryResourceSet();
							
							this.IA.playCard(SOCDevCardConstants.DISC);

							
						} else if(cmessage.getResponseHeader().equals("PLAY ROAD BUILDING CARD")) {
							
							// we set the edges where to build the road
							
							this.IA.edges_to_build_road = cmessage.getEdgesToBuildRoads();
							
							this.IA.playCard(SOCDevCardConstants.ROADS);
							
						}
						
						
					} else if(mail.getMessage() instanceof String) {
						
						if(mail.getMessage().equals("PURCHASE CARD")) {
							
							this.IA.purchaseCard();
							
							// we need to call the method of plan agent 
							// to reduce the resources if we have more than 7
							
							
							
						} else if(mail.getMessage().equals("CANNOT PURCHASE CARD")) {
							
							// if we cannot purchase the card then 
							// check for resource reducing strategy
							
							investResources();
							
						} else if(mail.getMessage().equals("PLAY KNIGHT CARD")) {
							
							// we can play the knight card   
							// we set a boolean to check if the call was from after
							// playing the knight card as we have to continue with the move as well
							
							this.IA.knight_played = true;
							this.IA.playCard(SOCDevCardConstants.KNIGHT);
							
						} else if(mail.getMessage().equals("MAKE MOVE")) {
							
							// message from the cards agent when there is no card to play
							planMove();
						}
								
						
					}
					
				} else if(mail.getFrom().equals("RobberAgent")) {
	
					RobberMessage rmessage = (RobberMessage)mail.getMessage();
					
					if(rmessage.getResponseHeader().equals("PLACE ROBBER ON HEX")) {
					
						int robber_place_hex = rmessage.getRobberHex();
							
						this.IA.moveRobber(robber_place_hex);
					
					} else if(rmessage.getResponseHeader().equals("PLAYER TO STEAL")) {
						
						int player_to_steal = rmessage.getStealPlayer();
						
						this.IA.stealFromPlayer(player_to_steal);
						
					}
					
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
	
	
	/**
	 * This method is used to compute the frequency with which we could get a resource
	 * based on the configuration of the Board at this moment
	 * 
	 * @param SOCPlayingPiece this is the playing piece we wish to forecast the new frequencies with 
	 * 		  if this parameter is null then we dont add any playing piece	
	 * @return
	 */
	
	public Hashtable getFrequenciesForResources(SOCPlayingPiece piece) throws Exception {
	
		SOCPlayer player = getPlayer();
		
		SOCGame game = getGame();
		
		// storage would be key -> value where key is the name of the resource
		// and value is the frequency of that resource
		
		
		int CLAY = 0;
		int ORE = 0;
		int SHEEP = 0;
		int WHEAT = 0;
		int WOOD = 0;
		
		// storage for numbers on which the resources are located
		Vector NUMBERS_CLAY = new Vector();
		Vector NUMBERS_ORE = new Vector(); 
		Vector NUMBERS_SHEEP = new Vector();
		Vector NUMBERS_WHEAT = new Vector();
		Vector NUMBERS_WOOD = new Vector();
		
		boolean has_misc_port = false; // 3:1 port
		
		boolean has_clay_port = false; // 2:1 resource port
		boolean has_ore_port = false; // 2:1 resource port
		boolean has_sheep_port = false; // 2:1 resource port
		boolean has_wheat_port = false; // 2:1 resource port
		boolean has_wood_port = false; // 2:1 resource port
		
		Hashtable ResourceFrequencyHash = new Hashtable();
		
		// first get all types of adjacent hexes to the nodes where we have settlements
		
		Vector player_settlements = (Vector)player.getSettlements().clone();  
		
		log.info("PLAYER SETTLEMENTS BEFORE ADDING A PIECE :: "+player_settlements);
		
		// check if the incoming piece is a Settlement
		
		if(piece != null && piece instanceof SOCSettlement) 
				player_settlements.add(piece);
		
		log.info("PLAYER SETTLEMENTS AFTER ADDING A PIECE :: "+player_settlements);
		
		for(int i = 0; i < player_settlements.size(); i++) { // loop on all the places where we have settlements
			
			SOCSettlement soc_settlement = (SOCSettlement)player_settlements.get(i);
			
			log.info("SOC SETTLEMENT :: "+soc_settlement);
			
			Vector adjHexesToSettlement = soc_settlement.getAdjacentHexes();
			
			for(int j = 0; j < adjHexesToSettlement.size(); j++) { // get the resource type on the adj hexes
				
				Integer hex_coordinate = (Integer)adjHexesToSettlement.get(j);
				
				int type_of_hex = game.getBoard().getHexTypeFromCoord(hex_coordinate); // hex type on this coordinate
				
				log.info("TYPE OF HEX :: "+type_of_hex);
				
				int number_on_hex = game.getBoard().getNumberOnHexFromCoord(hex_coordinate);
				
				log.info("NUMBER ON HEX :: "+number_on_hex);
				// check the type of hex and increment appropriately
				
				switch(type_of_hex) {
					
					case SOCBoard.CLAY_HEX : 
						
						log.info("ITS A CLAY HEX");
						CLAY++; NUMBERS_CLAY.add(new Integer(number_on_hex)); 
						
						break;
					
					case SOCBoard.CLAY_PORT_HEX : 
						log.info("WE HAVE A CLAY PORT");
						has_clay_port = true; 
					
					break;
					
					case SOCBoard.MISC_PORT_HEX : 
						log.info("WE HAVE A 3:1 PORT HEX");
						has_misc_port = true; break;
					
					case SOCBoard.ORE_HEX : 
						log.info("WE HAVE AN ORE HEX");
						ORE++; NUMBERS_ORE.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.ORE_PORT_HEX : 
						log.info("WE HAVE AN ORE PORT");
						has_ore_port = true; break;
					
					case SOCBoard.SHEEP_HEX : 
						log.info("WE HAVE A SHEEP HEX");
						SHEEP++; NUMBERS_SHEEP.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.SHEEP_PORT_HEX : 
						log.info("WE HAVE A SHEEP PORT HEX");
						has_sheep_port = true; break;
					
					case SOCBoard.WHEAT_HEX : 
						log.info("WE HAVE A WHEAT HEX");
						WHEAT++; NUMBERS_WHEAT.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.WHEAT_PORT_HEX : 
						log.info("WE HAVE A WHEAT PORT");
						has_wheat_port = true; break;
					
					case SOCBoard.WOOD_HEX : 
						log.info("WE HAVE A WOOD HEX");
						WOOD++; NUMBERS_WOOD.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.WOOD_PORT_HEX : 
						log.info("WE HAVE A WOOD PORT");
						has_wood_port = true; break;
				
				}
				
			}
			
		}
		
		// first get all types of adjacent hexes to the nodes where we have cities
		
		Vector player_cities = (Vector)player.getCities().clone();
		
		log.info("PLAYER CITIES BEFORE ADDING A PIECE :: "+player_cities);
		
		// check if the incoming piece is not null and a city
		
		if(piece != null && piece instanceof SOCCity)
				player_cities.add(piece);
		
		log.info("PLAYER CITIES AFTER ADDING A PIECE :: "+player_cities);
		
		for(int i = 0; i < player_cities.size(); i++) { // loop on all the places where we have settlements
			
			SOCCity soc_city = (SOCCity)player_cities.get(i);
			
			log.info("PLAYER CITY :: "+soc_city);
			
			Vector adjHexesToCity = soc_city.getAdjacentHexes();
			
			for(int j = 0; j < adjHexesToCity.size(); j++) { // get the resource type on the adj hexes
				
				Integer hex_coordinate = (Integer)adjHexesToCity.get(j);
				
				int type_of_hex = game.getBoard().getHexTypeFromCoord(hex_coordinate); // hex type on this coordinate
				
				log.info("TYPE OF HEX :: "+type_of_hex);
				// check the type of hex and increment appropriately (twice for city as city = 2 resources)
				int number_on_hex = game.getBoard().getNumberOnHexFromCoord(hex_coordinate);
				 
				log.info("NUMBER ON HEX :: "+number_on_hex);
				
				switch(type_of_hex) {
					
					case SOCBoard.CLAY_HEX : 
						log.info("WE HAVE A CLAY HEX NEAR CITY");
						CLAY+=2; NUMBERS_CLAY.add(new Integer(number_on_hex)); NUMBERS_CLAY.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.CLAY_PORT_HEX : 
						log.info("WE HAVE A CLAY PORT HEX NEAR CITY");
						has_clay_port = true; break;
					
					case SOCBoard.MISC_PORT_HEX : 
						log.info("WE HAVE A 3:1 PORT HEX NEAR CITY");
						has_misc_port = true; break;
					
					case SOCBoard.ORE_HEX : 
						log.info("WE HAVE A ORE HEX NEAR CITY");
						ORE+=2; NUMBERS_ORE.add(new Integer(number_on_hex)); NUMBERS_ORE.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.ORE_PORT_HEX : 
						log.info("WE HAVE A ORE PORT HEX NEAR CITY");
						has_ore_port = true; break;
					
					case SOCBoard.SHEEP_HEX : 
						log.info("WE HAVE A SHEEP HEX NEAR CITY");
						SHEEP+=2; NUMBERS_SHEEP.add(new Integer(number_on_hex)); NUMBERS_SHEEP.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.SHEEP_PORT_HEX : 
						log.info("WE HAVE A SHEEP PORT HEX NEAR CITY");
						has_sheep_port = true; break;
					
					case SOCBoard.WHEAT_HEX : 
						log.info("WE HAVE A WHEAT HEX NEAR CITY");
						WHEAT+=2; NUMBERS_WHEAT.add(new Integer(number_on_hex)); NUMBERS_WHEAT.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.WHEAT_PORT_HEX : 
						log.info("WE HAVE A WHEAT PORT HEX NEAR CITY");
						has_wheat_port = true; break;
					
					case SOCBoard.WOOD_HEX : 
						log.info("WE HAVE A WHEAT HEX NEAR CITY");
						WOOD+=2; NUMBERS_WOOD.add(new Integer(number_on_hex)); NUMBERS_WOOD.add(new Integer(number_on_hex)); break;
					
					case SOCBoard.WOOD_PORT_HEX : 
						log.info("WE HAVE A WOOD PORT HEX NEAR CITY");
						has_wood_port = true; break;
				
				}
				
			}
			
		}

		// now we have the count of resources based on the pieces on the board
		// also we have the numbers on which the resources are placed
		// note the numbers are noted twice if a city touches the numbered hex
		// the numbers are noted once if a settlement touches the numbered hex
		// calculate the probabilities for each resource
		
		// clay
		Hashtable DiceProbabilityTable = ApplicationConstants.DiceProbabilities.DiceProbabilityTable; 
		
		// fetch the numbers which have the clay
		double PROBABILITY_CLAY = 0D;
		
		
		for(int i = 0; i < NUMBERS_CLAY.size(); i++) {
			
			Integer number = (Integer)NUMBERS_CLAY.get(i);
			
			PROBABILITY_CLAY += new Double((Double)DiceProbabilityTable.get(number.intValue())).doubleValue(); 
				
		}
		
		// fetch the numbers which have the ore
		double PROBABILITY_ORE = 0D;
		
		
		for(int i = 0; i < NUMBERS_ORE.size(); i++) {
			
			Integer number = (Integer)NUMBERS_ORE.get(i);
			
			PROBABILITY_ORE += new Double((Double)DiceProbabilityTable.get(number.intValue())).doubleValue(); 
				
		}
		
		
		
		// fetch the numbers which have the sheep
		double PROBABILITY_SHEEP = 0D;
		
		for(int i = 0; i < NUMBERS_SHEEP.size(); i++) {
			
			Integer number = (Integer)NUMBERS_SHEEP.get(i);
			
			PROBABILITY_SHEEP += new Double((Double)DiceProbabilityTable.get(number.intValue())).doubleValue(); 
				
		}
		
		
		
		// fetch the numbers which have the wheat
		double PROBABILITY_WHEAT = 0D;
		
		for(int i = 0; i < NUMBERS_WHEAT.size(); i++) {
			
			Integer number = (Integer)NUMBERS_WHEAT.get(i);
			
			PROBABILITY_WHEAT += new Double((Double)DiceProbabilityTable.get(number.intValue())).doubleValue(); 
				
		}
		

		
		// fetch the numbers which have the wood
		double PROBABILITY_WOOD = 0D;
		
		for(int i = 0; i < NUMBERS_WOOD.size(); i++) {
			
			Integer number = (Integer)NUMBERS_WOOD.get(i);
			
			PROBABILITY_WOOD += new Double((Double)DiceProbabilityTable.get(number.intValue())).doubleValue(); 
				
		}
	
		// get the inverse of the probabilities which gives the frequency
		
		double FREQUENCY_CLAY = 0D;
		double FREQUENCY_ORE = 0D;
		double FREQUENCY_SHEEP = 0D;
		double FREQUENCY_WHEAT = 0D;
		double FREQUENCY_WOOD = 0D;
		
		log.info("PROBABILITY OF CLAY :: "+PROBABILITY_CLAY);
		
		FREQUENCY_CLAY = (PROBABILITY_CLAY > 0) ? (1/PROBABILITY_CLAY) : 0;
				
		log.info("FREQUENCY OF CLAY :: "+FREQUENCY_CLAY);
		
		log.info("PROBABILITY OF ORE :: "+PROBABILITY_ORE);
		
		FREQUENCY_ORE = (PROBABILITY_ORE > 0) ? (1/PROBABILITY_ORE) : 0;
		
		log.info("FREQUENCY OF ORE :: "+FREQUENCY_ORE);
		
		log.info("PROBABILITY OF SHEEP :: "+PROBABILITY_SHEEP);
		
		FREQUENCY_SHEEP = (PROBABILITY_SHEEP > 0) ? (1/PROBABILITY_SHEEP) : 0;
		
		log.info("FREQUENCY OF SHEEP :: "+FREQUENCY_SHEEP);
		
		log.info("PROBABILITY OF WHEAT :: "+PROBABILITY_WHEAT);
		
		FREQUENCY_WHEAT = (PROBABILITY_WHEAT > 0) ? (1/PROBABILITY_WHEAT) : 0;
		
		log.info("FREQUENCY OF WHEAT :: "+FREQUENCY_WHEAT);
		
		log.info("PROBABILITY OF WOOD :: "+PROBABILITY_WOOD);
		
		FREQUENCY_WOOD = (PROBABILITY_WOOD > 0) ? (1/PROBABILITY_WOOD) : 0;
		
		log.info("FREQUENCY OF WOOD :: "+FREQUENCY_WOOD);
		
		// CEILING THE FREQUENCIES
		
		ResourceFrequencyHash.put("CLAY", Math.ceil(FREQUENCY_CLAY));
		ResourceFrequencyHash.put("ORE", Math.ceil(FREQUENCY_ORE));
		ResourceFrequencyHash.put("SHEEP", Math.ceil(FREQUENCY_SHEEP));
		ResourceFrequencyHash.put("WHEAT", Math.ceil(FREQUENCY_WHEAT));
		ResourceFrequencyHash.put("WOOD", Math.ceil(FREQUENCY_WOOD));
		
		// ALSO WE ADD IF OUR PIECES TOUCH ANY PORT HEX.
		// THIS WOULD COME IN HANDY WHEN WE ARE CALCULATING THE BUILDING SPEED ESTIMATE
		
		ResourceFrequencyHash.put("CLAY_PORT", has_clay_port);
		ResourceFrequencyHash.put("ORE_PORT", has_ore_port);
		ResourceFrequencyHash.put("SHEEP_PORT", has_sheep_port);
		ResourceFrequencyHash.put("WHEAT_PORT", has_wheat_port);
		ResourceFrequencyHash.put("WOOD_PORT", has_wood_port);
		ResourceFrequencyHash.put("MISC_PORT", has_misc_port);
		
		log.info("RESOURCE FREQUENCY HASH :: "+ResourceFrequencyHash);
		
		return ResourceFrequencyHash;
		
	}
	
	/**
	 * This method is used to calculate the building speed given the current situation
	 * of the Board and a piece we wish to build. At all times the ports are also checked
	 * 
	 * @param piece This can be SETTLEMENT/CITY
	 *
	 */
	
	public int calculateBuildingSpeed(SOCPlayingPiece piece) {
		
		// THIS METHOD CALLS THE FREQUENCY BUILDING TABLE METHOD WHICH GIVES THE 
		// FREQUENCY OF RESOURCES CONSIDERING THAT THIS PIECE HAS BEEN BUILT
		// ALSO WE GET THE INFORMATION OF WE HAVE SOME PORTS TOUCHING 
		// BASED ON THIS INFORMATION AND THE RESOURCES TO START WITH WE CALCULATE THE 
		// BUILDING SPEED
		
		// BUILDING SPEED = SUM OF THE INDIVIDUAL NO OF TURNS REQUIRED TO BUILD SETTLEMENT, CITY, ROAD, DEV CARD
		
		// ALSO IN ORDER NOT TO GO TOO DEEP INTO ITERATIONS, WE LIMIT THE NUMBER OF TURNS 
		// TO A CERTAIN THRESHOLD VALUE. IF THIS VALUE IS REACHED BEFORE BEING ABLE TO 
		// ACCUMULATE THE RESOURCES REQUIRED TO BUILD A PARTICULAR PIECE, WE SIMPLY GIVE 
		// SOME HIGHER TURN VALUE
		
		int noOfTurns = 0;
		
		try {
			
			Hashtable ResourceFrequenciesHash = getFrequenciesForResources(piece);
			
			String [] pieces = {"SETTLEMENT", "CITY", "ROAD", "CARD"};
						
			for(int i = 0; i < pieces.length; i++) {
				
				// for each piece, check the no of turns required to build this resource
				log.info("CALCULATING THE NO OF TURNS FOR "+pieces[i]);
				
				noOfTurns += getNoOfTurns(ResourceFrequenciesHash, pieces[i]);
				
			}
			
			log.info("TOTAL NO OF TURNS REQUIRED TO BUILD THE PIECE "+piece+" >> "+noOfTurns);
	
			
		} catch(Exception e) {
			
			e.printStackTrace();
			
		}
		
		return noOfTurns;
		
	}
	
	 /**
	  * This method is responsible for calculating the no of turns required to build a piece 
	  * given the current frequencies of resources
	  * 
	  * 
	  * @param ResourceFrequenciesHash This Hashtable stores the frequency information
	  * @param piece This is the intended thing we need to build <SETTLEMENT/CITY/ROAD/CARD>
	  * @return The no of turns required to build the piece
	  * 
	  */
	
	public int getNoOfTurns(Hashtable ResourceFrequenciesHash, String piece) {
		
		int noOfTurns = 0;
		
		SOCPlayer player = getPlayer();
		SOCGame game = getGame(); 
		
		log.info("getNoOfTurns() "+piece);
		
		// these variables are responsible for holding the target resources we 
		// need to get based on the incoming piece
		
		int TARGET_CLAY = 0;
		int TARGET_ORE = 0;
		int TARGET_SHEEP = 0;
		int TARGET_WHEAT = 0;
		int TARGET_WOOD = 0;
		
		// getting the values in int instead of double would facilitate the modulus operation
		
		int FREQUENCY_CLAY = new Double((Double)ResourceFrequenciesHash.get("CLAY")).intValue();
		int FREQUENCY_ORE = new Double((Double)ResourceFrequenciesHash.get("ORE")).intValue();
		int FREQUENCY_SHEEP = new Double((Double)ResourceFrequenciesHash.get("SHEEP")).intValue();
		int FREQUENCY_WHEAT = new Double((Double)ResourceFrequenciesHash.get("WHEAT")).intValue();
		int FREQUENCY_WOOD = new Double((Double)ResourceFrequenciesHash.get("WOOD")).intValue();
		
//		System.out.println("PIECE IS :: "+piece);
		
		Hashtable target_resources = (Hashtable)ApplicationConstants.ResourcesRequired.RequiredResourcesTable.get(piece);
		
		TARGET_CLAY = new Integer((Integer)target_resources.get("CLAY")).intValue();
			
		log.info("TARGET_CLAY NEEDED :: "+TARGET_CLAY);
		
		TARGET_ORE = new Integer((Integer)target_resources.get("ORE")).intValue();
		
		log.info("TARGET_ORE NEEDED :: "+TARGET_ORE);
		
		TARGET_SHEEP = new Integer((Integer)target_resources.get("SHEEP")).intValue();
		
		log.info("TARGET_SHEEP NEEDED :: "+TARGET_SHEEP);
		
		TARGET_WHEAT = new Integer((Integer)target_resources.get("WHEAT")).intValue();
		
		log.info("TARGET_WHEAT NEEDED :: "+TARGET_WHEAT);
		
		TARGET_WOOD = new Integer((Integer)target_resources.get("WOOD")).intValue();
		
		log.info("TARGET_WOOD NEEDED :: "+TARGET_WOOD);
		
		// after getting the target values, we can now get the resources we have to start with
		
		SOCResourceSet player_resource_set = player.getResources();
		
		// this loop calculates the no of turns before we have the resources needed to build
		// the target piece 
		
		// a temporary resource set which would be used to keep count of the resources in 
		// the loop
		
		SOCResourceSet player_temp_resource_set = player_resource_set.copy(); 
		
		for(; noOfTurns <= ApplicationConstants.Game.MAX_TURNS_THREASHOLD; noOfTurns++) {
			
			//log.info("TURN NO :: "+noOfTurns+" CLAY :: "+player_temp_resource_set.getAmount(1)+" ORE :: "+player_temp_resource_set.getAmount(2)+" SHEEP :: "+player_temp_resource_set.getAmount(3)+" WHEAT :: "+player_temp_resource_set.getAmount(4)+" WOOD :: "+player_temp_resource_set.getAmount(5));
			
			if(resourcesAchievedToBuildPiece(player_temp_resource_set, TARGET_CLAY, TARGET_ORE, TARGET_SHEEP, TARGET_WHEAT, TARGET_WOOD)) {
				log.info("GOT THE RESOURCES TO BUILD :: "+piece);
				break;
			}
			
			// check to see at what frequency do we get each resource
			// we start with the next turn
		
			if(noOfTurns > 0) {
				
				// increment all those resources which have a matching frequency
				
				if(FREQUENCY_CLAY > 0)
					
					if(noOfTurns % FREQUENCY_CLAY == 0) {
						log.info("ADDED A CLAY");
						player_temp_resource_set.add(1, 1);
					}
			
				
				if(FREQUENCY_ORE > 0) 
					
				if(noOfTurns % FREQUENCY_ORE == 0) {
					
					log.info("ADDED AN ORE");	
					player_temp_resource_set.add(1, 2);
					
					}
				
				
				if(FREQUENCY_SHEEP > 0)
				
					if(noOfTurns % FREQUENCY_SHEEP == 0) {
						log.info("ADDED A SHEEP");
						player_temp_resource_set.add(1, 3);
					}
				
				
				
				if(FREQUENCY_WHEAT > 0)
					if(noOfTurns % FREQUENCY_WHEAT == 0) {
						
						log.info("ADDED A WHEAT");
						player_temp_resource_set.add(1, 4);
					}
				
				
				
				if(FREQUENCY_WOOD > 0) 
					if(noOfTurns % FREQUENCY_WOOD == 0) {
						log.info("ADDED A WOOD");
						player_temp_resource_set.add(1, 5);
					}
				
				// for each resource_port_hex
				// 	check if we have that port hex
				// 	 if the corresponding resource is in +2 addition to what we need 
				//   in the target value, the additional units of this resource can be
				// 	 traded for 1 of the least available resource
				
				
				boolean [] port_hexes = {
											new Boolean((Boolean)ResourceFrequenciesHash.get("CLAY_PORT")).booleanValue(), 
											new Boolean((Boolean)ResourceFrequenciesHash.get("ORE_PORT")).booleanValue(), 
											new Boolean((Boolean)ResourceFrequenciesHash.get("SHEEP_PORT")).booleanValue(), 
											new Boolean((Boolean)ResourceFrequenciesHash.get("WHEAT_PORT")).booleanValue(), 
											new Boolean((Boolean)ResourceFrequenciesHash.get("WOOD_PORT")).booleanValue(),
											new Boolean((Boolean)ResourceFrequenciesHash.get("MISC_PORT")).booleanValue()
									};
				
				
				// loop over all the port hexes where there is a possibility of trade!
				
				// the trading resource would be checked for excessive 
				// and then if feasible traded!
				// if the trading resource is not required in the target list
				// and we have it we can trade all of it at this moment
				// THIS IS THE POINT OF OPTIMAL PLAY!!!
				
				int [] resources_needed = {TARGET_CLAY, TARGET_ORE, TARGET_SHEEP, TARGET_WHEAT, TARGET_WOOD};
				
				each_port : for(int port_hex = 0; port_hex < port_hexes.length; port_hex++) {
					
					// check for those hexes where there we have some piece built
					String trading_resource;
					
					if(port_hexes[port_hex]) {
						
						if(port_hex == 0) 
							log.info("WE HAVE A CLAY PORT");
						else if(port_hex == 1)
							log.info("WE HAVE AN ORE PORT");
						else if(port_hex == 2)
							log.info("WE HAVE A SHEEP PORT");
						else if(port_hex == 3)
							log.info("WE HAVE A WHEAT PORT");
						else if(port_hex == 4)
							log.info("WE HAVE AN WOOD PORT");
						else if(port_hex == 5)
							log.info("WE HAVE A MISC PORT");
						
						// explicit check for the port type 0 -> clay, 1 -> ore, 2 -> sheep, 3 -> wheat, 4 -> wood, 5 -> misc 
						
						int resource_available_for_trade = 0;
						
						
						
						int max = 0;
						String starved = "";
												
						switch(port_hex) {
						
							case 0 : trading_resource = "CLAY"; 
										
							log.info("SWITCH CASE CLAY PORT");
									// check if the resource is needed at all
									// if it is, then subtract the excessive 
									// if not then get the whole
							
									if(TARGET_CLAY > 0) {
										
										log.info("WE NEED "+TARGET_CLAY+" CLAY");
										
										if(player_temp_resource_set.getAmount(1) - TARGET_CLAY >= 2) 
											resource_available_for_trade = 2;
										
									} else if(TARGET_CLAY == 0) {
											resource_available_for_trade = player_temp_resource_set.getAmount(1);
											
									}
									
									resource_available_for_trade = resource_available_for_trade > 2 ? 2 : resource_available_for_trade;
									
									log.info("RESOURCES AVAILABLE FOR TRADE :: "+resource_available_for_trade);
									
									// now we compare the target resources to what we have in pocket
									// we get the resource which is the least we have to accomplish 
									// the target and get one of that resource for this resource
										
									// a loop on all the required resources which are greater than 0
									
									for(int i = 0; i < resources_needed.length; i++) {
										
										// check for those resources that we need the most
										// 0 doesnt make much of a difference :)
										
										if(i == 0)
											continue;
										
										if(resources_needed[i] > 0) { // this resource is needed											
											
											// get the amount needed 
											
											int amount_needed = resources_needed[i] - player_temp_resource_set.getAmount(i+1);
											
											if(amount_needed > max) {
												
												if(i == 1)
													starved = "ORE";
												else if(i == 2) 
													starved = "SHEEP";
												else if(i == 3)
													starved = "WHEAT";
												else if(i == 4)
													starved = "WOOD";
												
												max = amount_needed;
												
											} 
												
											
										} 
										
									}
									
									// starved holds the resource we need the most to get to the target
									
									// check if the resource available for trade is 2 units and 
									// there is a resource we need to fill
									
									log.info("STARVED RESOURCE IS :: "+starved);
									
									if(resource_available_for_trade == 2 && starved.length() > 0) {
										
										log.info("DO THE TRADE");
										
										player_temp_resource_set.subtract(resource_available_for_trade, 1);
										
										if(starved.equals("ORE")) { 
											
											log.info("ADD 1 TO :: "+starved);
											player_temp_resource_set.add(1, 2);
										}
										else if(starved.equals("SHEEP")) {
											 
											 log.info("ADD 1 TO :: "+starved);
											 player_temp_resource_set.add(1, 3);
										 }
										else if(starved.equals("WHEAT")) {
											
											log.info("ADD 1 TO :: "+starved);
											player_temp_resource_set.add(1, 4);
										}
										else if(starved.equals("WOOD")) {
											
											log.info("ADD 1 TO :: "+starved);
											player_temp_resource_set.add(1, 5);
										
										}
										
									}
									
									
							break;

							case 1 : trading_resource = "ORE"; 
							
							log.info("TRADING RESOURCE PORT IS :: ORE");
							// check if the resource is needed at all
							// if it is, then subtract the excessive 
							// if not then get the whole
					
							if(TARGET_ORE > 0) {
								
								log.info("WE NEED ORE");
								
								if(player_temp_resource_set.getAmount(2) - TARGET_ORE >= 2) 
									resource_available_for_trade = 2;
								
							} else if(TARGET_ORE == 0) {
									resource_available_for_trade = player_temp_resource_set.getAmount(2);
									
							}
							
							resource_available_for_trade = resource_available_for_trade > 2 ? 2 : resource_available_for_trade;
							
							log.info("RESOURCE AVAILABLE FOR TRADE :: "+resource_available_for_trade);
				
							// now we compare the target resources to what we have in pocket
							// we get the resource which is the least we have to accomplish 
							// the target and get one of that resource for this resource
								
							// a loop on all the required resources which are greater than 0
							
							for(int i = 0; i < resources_needed.length; i++) {
								
								// check for those resources that we need the most
								// 0 doesnt make much of a difference :)
								
								if(i == 1)
									continue;
								
								if(resources_needed[i] > 0) { // this resource is needed											
									
									// get the amount needed 
									
									int amount_needed = resources_needed[i] - player_temp_resource_set.getAmount(i+1);
									
									if(amount_needed > max) {
										
										if(i == 0)
											starved = "CLAY";
										else if(i == 2) 
											starved = "SHEEP";
										else if(i == 3)
											starved = "WHEAT";
										else if(i == 4)
											starved = "WOOD";
										
										max = amount_needed;
										
									} 
										
									
								} 
								
							}
							
							// starved holds the resource we need the most to get to the target
							
							// check if the resource available for trade is 2 units and 
							// there is a resource we need to fill
							
							log.info("STARVED RESOURCE IS :: "+starved);
							
							if(resource_available_for_trade == 2 && starved.length() > 0) {
								
								log.info("A TRADE CAN BE DONE");
									
								player_temp_resource_set.subtract(resource_available_for_trade, 2);
								
								if(starved.equals("CLAY")) { 
									
									log.info("CLAY IS STARVED");
									player_temp_resource_set.add(1, 1);
								}
								 else if(starved.equals("SHEEP")) {
									
									 log.info("SHEEP IS STARVED");
									 player_temp_resource_set.add(1, 3);
								 }
								 else if(starved.equals("WHEAT")) {
									 
									 log.info("WHEAT IS STARVED");
									 player_temp_resource_set.add(1, 4);
								 
								 }

								 else if(starved.equals("WOOD")) {
									 
									 log.info("WOOD IS STARVED");
									 player_temp_resource_set.add(1, 5);
								
								 }
							}
							
							
							break;
							
							
							case 2 : trading_resource = "SHEEP"; 
							
							log.info("TRADING RESOURCE IS :: SHEEP");
							// check if the resource is needed at all
							// if it is, then subtract the excessive 
							// if not then get the whole
					
							if(TARGET_SHEEP > 0) {
								
								log.info("WE NEED SHEEP");
								
								if(player_temp_resource_set.getAmount(3) - TARGET_SHEEP >= 2) 
									resource_available_for_trade = 2;
								
							} else if(TARGET_SHEEP == 0) {
									resource_available_for_trade = player_temp_resource_set.getAmount(3);
									
							}
							
							resource_available_for_trade = resource_available_for_trade > 2 ? 2 : resource_available_for_trade; 
							
							log.info("RESOURCE AVAILABLE FOR TRADE "+resource_available_for_trade);
					
							// now we compare the target resources to what we have in pocket
							// we get the resource which is the least we have to accomplish 
							// the target and get one of that resource for this resource
								
							
							
							// a loop on all the required resources which are greater than 0
							
							for(int i = 0; i < resources_needed.length; i++) {
								
								// check for those resources that we need the most
								// 0 doesnt make much of a difference :)
								
								if(i == 3)
									continue;
								
								if(resources_needed[i] > 0) { // this resource is needed											
									
									// get the amount needed 
									
									int amount_needed = resources_needed[i] - player_temp_resource_set.getAmount(i+1);
									
									if(amount_needed > max) {
										
										if(i == 0)
											starved = "CLAY";
										else if(i == 1) 
											starved = "ORE";
										else if(i == 3)
											starved = "WHEAT";
										else if(i == 4)
											starved = "WOOD";
										
										max = amount_needed;
										
									} 
										
									
								} 
								
							}
							
							// starved holds the resource we need the most to get to the target
							
							// check if the resource available for trade is 2 units and 
							// there is a resource we need to fill
							
							log.info("STARVED RESOURCE IS :: "+starved);
							
							if(resource_available_for_trade == 2 && starved.length() > 0) {
								
								log.info("READY FOR TRADE");
								
								player_temp_resource_set.subtract(resource_available_for_trade, 3);
								
								if(starved.equals("CLAY")) { 
									
									log.info("CLAY IS STARVED");
									player_temp_resource_set.add(1, 1);
								
								}
								 else if(starved.equals("ORE")) {
									 
									 log.info("ORE IS STARVED");
									 player_temp_resource_set.add(1, 2);
								 }
								 else if(starved.equals("WHEAT")) {
									
									 log.info("WHEAT IS STARVED");
									 player_temp_resource_set.add(1, 4);
								 }
								 else if(starved.equals("WOOD")) {
									 
									 log.info("WOOD IS STARVED");
									 player_temp_resource_set.add(1, 5);
								
								 }
							}
							
							
							break;
							
							
							case 3 : trading_resource = "WHEAT"; 
							
							log.info("TRADING RESOURCE IS :: WHEAT");
							// check if the resource is needed at all
							// if it is, then subtract the excessive 
							// if not then get the whole
					
							if(TARGET_WHEAT > 0) {
								
								log.info("WE NEED WHEAT");
								
								if(player_temp_resource_set.getAmount(4) - TARGET_WHEAT >= 2) 
									resource_available_for_trade = 2;
								
							} else if(TARGET_WHEAT == 0) {
									resource_available_for_trade = player_temp_resource_set.getAmount(4);
									
							}
							
							resource_available_for_trade = resource_available_for_trade > 2 ? 2 : resource_available_for_trade; 
							
							log.info("RESOURCES AVAILABLE FOR TRADE :: "+resource_available_for_trade);
							// now we compare the target resources to what we have in pocket
							// we get the resource which is the least we have to accomplish 
							// the target and get one of that resource for this resource
								
							
							
							// a loop on all the required resources which are greater than 0
							
							for(int i = 0; i < resources_needed.length; i++) {
								
								// check for those resources that we need the most
								// 0 doesnt make much of a difference :)
								
								if(i == 4)
									continue;
								
								if(resources_needed[i] > 0) { // this resource is needed											
									
									// get the amount needed 
									
									int amount_needed = resources_needed[i] - player_temp_resource_set.getAmount(i+1);
									
									if(amount_needed > max) {
										
										if(i == 0)
											starved = "CLAY";
										else if(i == 1) 
											starved = "ORE";
										else if(i == 2)
											starved = "SHEEP";
										else if(i == 4)
											starved = "WOOD";
										
										max = amount_needed;
										
									} 
										
									
								} 
								
							}
							
							// starved holds the resource we need the most to get to the target
							
							// check if the resource available for trade is 2 units and 
							// there is a resource we need to fill
							
							log.info("STARVED RESOURCE IS :: "+starved);
							
							if(resource_available_for_trade == 2 && starved.length() > 0) {
								
								log.info("A TRADE IS DONE");
								
								player_temp_resource_set.subtract(resource_available_for_trade, 4);
								
								if(starved.equals("CLAY")) {  
									
									log.info("CLAY IS STARVED");
									
									player_temp_resource_set.add(1, 1);
								}
								 else if(starved.equals("ORE")) {
									 
									 log.info("ORE IS STARVED");
									 player_temp_resource_set.add(1, 2);
								
								 }
								 else if(starved.equals("SHEEP")) {
									 
									 log.info("SHEEP IS STARVED");
									 player_temp_resource_set.add(1, 3);
								 }
								 else if(starved.equals("WOOD")) {
									 
									 log.info("WOOD IS STARVED");
									 player_temp_resource_set.add(1, 5);
								 
								 }
								
							}

							
							break;
							case 4 : trading_resource = "WOOD"; 
							
							log.info("TRADING RESOURCE IS WOOD");
							// check if the resource is needed at all
							// if it is, then subtract the excessive 
							// if not then get the whole
					
							if(TARGET_WOOD > 0) {
									
								log.info("WE NEED WOOD");
								
								if(player_temp_resource_set.getAmount(5) - TARGET_WOOD >= 2) 
									resource_available_for_trade = 2;
								
							} else if(TARGET_WOOD == 0) {
									resource_available_for_trade = player_temp_resource_set.getAmount(5);
									
							}
							
							resource_available_for_trade = resource_available_for_trade > 2 ? 2 : resource_available_for_trade; 
							
							log.info("RESOURCES AVAILABLE FOR TRADE :: "+resource_available_for_trade);
							// now we compare the target resources to what we have in pocket
							// we get the resource which is the least we have to accomplish 
							// the target and get one of that resource for this resource
								
							
							
							// a loop on all the required resources which are greater than 0
							
							for(int i = 0; i < resources_needed.length; i++) {
								
								// check for those resources that we need the most
								// 0 doesnt make much of a difference :)
								
								if(i == 4)
									continue;
								
								if(resources_needed[i] > 0) { // this resource is needed											
									
									// get the amount needed 
									
									int amount_needed = resources_needed[i] - player_temp_resource_set.getAmount(i+1);
									
									if(amount_needed > max) {
										
										if(i == 0)
											starved = "CLAY";
										else if(i == 1) 
											starved = "ORE";
										else if(i == 3)
											starved = "WHEAT";
										else if(i == 2)
											starved = "SHEEP";
										
										max = amount_needed;
										
									} 
										
									
								} 
								
							}
							
							// starved holds the resource we need the most to get to the target
							
							// check if the resource available for trade is 2 units and 
							// there is a resource we need to fill
							
							log.info("STARVED RESOURCE IS :: "+starved);
							
							if(resource_available_for_trade == 2 && starved.length() > 0) {
								
								log.info("TRADE IS DONE");
								
								player_temp_resource_set.subtract(resource_available_for_trade, 5);
								
								if(starved.equals("CLAY")) { 
									
									log.info("CLAY IS STARVED");
									player_temp_resource_set.add(1, 1);
								}
								 else if(starved.equals("ORE")) {
									
									 log.info("ORE IS STARVED");
									 player_temp_resource_set.add(1, 2);
								 }
								 else if(starved.equals("WHEAT")) {
									
									 log.info("WHEAT IS STARVED");
									 player_temp_resource_set.add(1, 4);
								 }
								 else if(starved.equals("SHEEP")) {
									
									 log.info("SHEEP IS STARVED");
									 player_temp_resource_set.add(1, 3);
								
								 }
							}
							
							
							break;
						
							case 5 : 
							
								// there is a little bit maths in this case 
								// as we can trade any of the 3 available resources
								// we would check which of the resources are in 
								// excess after subtracting 3 from them 
								
								trading_resource = getResourceForMiscPort(player_temp_resource_set, resources_needed);
								
								log.info("TRADING RESOURCE FOR 3:1 IS :: "+trading_resource);
								
								if(trading_resource == null)
									continue each_port;
								
								int TARGET_RESOURCE = 0;
								int TARGET_RESOURCE_NUMBER = 0;
								
								if(trading_resource.equals("CLAY")) {
									
									log.info("TRADING RESOURCE IS CLAY");
									TARGET_RESOURCE = TARGET_CLAY;
									TARGET_RESOURCE_NUMBER = 1;
									
								}
								else if(trading_resource.equals("ORE")) {
									
									log.info("TRADING RESOURCE IS ORE");
									TARGET_RESOURCE = TARGET_ORE;
									TARGET_RESOURCE_NUMBER = 2;
									
								}
								else if(trading_resource.equals("SHEEP")) {
									
									log.info("TRADING RESOURCE IS SHEEP");
									TARGET_RESOURCE = TARGET_SHEEP;
									TARGET_RESOURCE_NUMBER = 3;
									
								}
								else if(trading_resource.equals("WHEAT")) {
									
									log.info("TRADING RESOURCE IS WHEAT");
									TARGET_RESOURCE = TARGET_WHEAT;
									TARGET_RESOURCE_NUMBER = 4;
									
								}
								else if(trading_resource.equals("WOOD")) {
									
									log.info("TRADING RESOURCE IS WOOD");
									TARGET_RESOURCE = TARGET_WOOD; 
									TARGET_RESOURCE_NUMBER = 5;
									
								}
								
								
								// check if the resource is needed at all
								// if it is, then subtract the excessive 
								// if not then get the whole
						
								if(TARGET_RESOURCE > 0) {
									
									log.info("WE NEED THE TRAGET RESOURCE");
									
									if(player_temp_resource_set.getAmount(TARGET_RESOURCE_NUMBER) - TARGET_RESOURCE >= 3) 
										resource_available_for_trade = 3;
									
								} else if(TARGET_RESOURCE == 0) {
										resource_available_for_trade = player_temp_resource_set.getAmount(TARGET_RESOURCE_NUMBER);
										
								}
								
								resource_available_for_trade = resource_available_for_trade > 3 ? 3 : resource_available_for_trade; 
								
								log.info("RESOURCE AVAILABLE FOR TRADE :: "+resource_available_for_trade);
								
								// now we compare the target resources to what we have in pocket
								// we get the resource which is the least we have to accomplish 
								// the target and get one of that resource for this resource
									
								
								
								// a loop on all the required resources which are greater than 0
								
								for(int i = 0; i < resources_needed.length; i++) {
									
									// check for those resources that we need the most
									// 0 doesnt make much of a difference :)
									
									if(i == TARGET_RESOURCE_NUMBER - 1)
										continue;
									
									if(resources_needed[i] > 0) { // this resource is needed											
										
										// get the amount needed 
										
										int amount_needed = resources_needed[i] - player_temp_resource_set.getAmount(i+1);
										
										if(amount_needed > max) {
											
											if(i == 0)
												starved = "CLAY";
											else if(i == 1) 
												starved = "ORE";
											else if(i == 4)
												starved = "WOOD";	
											else if(i == 3)
												starved = "WHEAT";
											else if(i == 2)
												starved = "SHEEP";
											
											max = amount_needed;
											
										} 
											
										
									} 
									
								}
								
								// starved holds the resource we need the most to get to the target
								
								// check if the resource available for trade is 2 units and 
								// there is a resource we need to fill
								
								log.info("RESOUTRCE STARVED IS :: "+starved);
								
								if(resource_available_for_trade == 3 && starved.length() > 0) {
									
									log.info("TRADE IS DONE");
									
									player_temp_resource_set.subtract(resource_available_for_trade, TARGET_RESOURCE_NUMBER);
									
									if(starved.equals("CLAY")) { 
										
										log.info("CLAY IS STARVED");
										
										player_temp_resource_set.add(1, 1);
									}
									 else if(starved.equals("ORE")) {
										 
										 log.info("ORE IS STARVED");
										 player_temp_resource_set.add(1, 2);
									
									 }
									 else if(starved.equals("WHEAT")) {
										 
										 log.info("WHEAT IS STARVED");
										 player_temp_resource_set.add(1, 4);
									 }
									 else if(starved.equals("SHEEP")) {
										 
										 log.info("SHEEP IS STARVED");
										 player_temp_resource_set.add(1, 3);
									
									 }
									 else if(starved.equals("WOOD")) {
										 
										 log.info("WOOD IS STARVED");
										 player_temp_resource_set.add(1, 5);
									 
									 }
									
								}
								
								
								
							break;
						}
						
					} // if we have a port of the particular type
					
				} // end of for loop
				
				// CHECK THE OPTION OF TRADE WITH BANK
				
				
				String trading_resource = getResourceToTradeWithBank(player_temp_resource_set, resources_needed);
				
				log.info("TRADING RESOURCE FOR 4:1 IS :: "+trading_resource);
				
				if(trading_resource != null) {
					
				
				int TARGET_RESOURCE = 0;
				int TARGET_RESOURCE_NUMBER = 0;
				
				if(trading_resource.equals("CLAY")) {
					
					log.info("TRADING RESOURCE IS CLAY");
					TARGET_RESOURCE = TARGET_CLAY;
					TARGET_RESOURCE_NUMBER = 1;
					
				}
				else if(trading_resource.equals("ORE")) {
					
					log.info("TRADING RESOURCE IS ORE");
					TARGET_RESOURCE = TARGET_ORE;
					TARGET_RESOURCE_NUMBER = 2;
					
				}
				else if(trading_resource.equals("SHEEP")) {
					
					log.info("TRADING RESOURCE IS SHEEP");
					TARGET_RESOURCE = TARGET_SHEEP;
					TARGET_RESOURCE_NUMBER = 3;
					
				}
				else if(trading_resource.equals("WHEAT")) {
					
					log.info("TRADING RESOURCE IS WHEAT");
					TARGET_RESOURCE = TARGET_WHEAT;
					TARGET_RESOURCE_NUMBER = 4;
					
				}
				else if(trading_resource.equals("WOOD")) {
					
					log.info("TRADING RESOURCE IS WOOD");
					TARGET_RESOURCE = TARGET_WOOD; 
					TARGET_RESOURCE_NUMBER = 5;
					
				}
				
				
				// check if the resource is needed at all
				// if it is, then subtract the excessive 
				// if not then get the whole
				int resource_available_for_trade = 0;
				
				if(TARGET_RESOURCE > 0) {
					
					log.info("WE NEED THE TARGET RESOURCE");
					
					if(player_temp_resource_set.getAmount(TARGET_RESOURCE_NUMBER) - TARGET_RESOURCE >= 4) 
						resource_available_for_trade = 4;
					
				} else if(TARGET_RESOURCE == 0) {
						resource_available_for_trade = player_temp_resource_set.getAmount(TARGET_RESOURCE_NUMBER);
						
				}
				
				resource_available_for_trade = resource_available_for_trade > 4 ? 4 : resource_available_for_trade; 
				
				log.info("RESOURCE AVAILABLE FOR TRADE :: "+resource_available_for_trade);
				
				// now we compare the target resources to what we have in pocket
				// we get the resource which is the least we have to accomplish 
				// the target and get one of that resource for this resource
					
				int max = 0;
				String starved = "";
				// a loop on all the required resources which are greater than 0
				
				for(int i = 0; i < resources_needed.length; i++) {
					
					// check for those resources that we need the most
					// 0 doesnt make much of a difference :)
					
					if(i == TARGET_RESOURCE_NUMBER - 1)
						continue;
					
					if(resources_needed[i] > 0) { // this resource is needed											
						
						// get the amount needed 
						
						int amount_needed = resources_needed[i] - player_temp_resource_set.getAmount(i+1);
						
						if(amount_needed > max) {
							
							if(i == 0)
								starved = "CLAY";
							else if(i == 1) 
								starved = "ORE";
							else if(i == 4)
								starved = "WOOD";	
							else if(i == 3)
								starved = "WHEAT";
							else if(i == 2)
								starved = "SHEEP";
							
							max = amount_needed;
							
						} 
							
						
					} 
					
				}
				
				// starved holds the resource we need the most to get to the target
				
				// check if the resource available for trade is 2 units and 
				// there is a resource we need to fill
				
				log.info("RESOUTRCE STARVED IS :: "+starved);
				
				if(resource_available_for_trade == 4 && starved.length() > 0) {
					
					log.info("TRADE IS DONE");
					
					player_temp_resource_set.subtract(resource_available_for_trade, TARGET_RESOURCE_NUMBER);
					
					if(starved.equals("CLAY")) { 
						
						log.info("CLAY IS STARVED");
						
						player_temp_resource_set.add(1, 1);
					}
					 else if(starved.equals("ORE")) {
						 
						 log.info("ORE IS STARVED");
						 player_temp_resource_set.add(1, 2);
					
					 }
					 else if(starved.equals("WHEAT")) {
						 
						 log.info("WHEAT IS STARVED");
						 player_temp_resource_set.add(1, 4);
					 }
					 else if(starved.equals("SHEEP")) {
						 
						 log.info("SHEEP IS STARVED");
						 player_temp_resource_set.add(1, 3);
					
					 }
					 else if(starved.equals("WOOD")) {
						 
						 log.info("WOOD IS STARVED");
						 player_temp_resource_set.add(1, 5);
					 
					 }
					
					}
				
				}
				//////////////////////////////////////
				
			} // if no of turns greater than 0
		
			log.info("TURN NO :: "+noOfTurns+" CLAY :: "+player_temp_resource_set.getAmount(1)+" ORE :: "+player_temp_resource_set.getAmount(2)+" SHEEP :: "+player_temp_resource_set.getAmount(3)+" WHEAT :: "+player_temp_resource_set.getAmount(4)+" WOOD :: "+player_temp_resource_set.getAmount(5));
			
			if(resourcesAchievedToBuildPiece(player_temp_resource_set, TARGET_CLAY, TARGET_ORE, TARGET_SHEEP, TARGET_WHEAT, TARGET_WOOD)) {
				log.info("GOT THE RESOURCES TO BUILD :: "+piece);
				break;
			}
			
			
		} // end of main for loop
		
		log.info("NO OF TURNS :: "+noOfTurns);
		
		return noOfTurns;
		
	}
	
	private boolean resourcesAchievedToBuildPiece(SOCResourceSet resource_set, int clay, int ore, int sheep, int wheat, int wood) {
		
		if(resource_set.getAmount(1) >= clay && 
				resource_set.getAmount(2) >= ore &&
					resource_set.getAmount(3) >= sheep &&
						resource_set.getAmount(4) >= wheat &&
							resource_set.getAmount(5) >= wood) { 	
			
			
			return true; 
			
		}
		 
		return false;
		
	} 
		
	private String getResourceForMiscPort(SOCResourceSet player_temp_resource_set, int [] resources_needed) {
		
		String trading_resource = null;
		
		int max = 0;
		
		// loop over all the needed resources and check which one is excess with us
		
		for(int i = 0; i < resources_needed.length; i++) {
			
			if(player_temp_resource_set.getAmount(i+1) - ApplicationConstants.Game.MISC_PORT_RESOURCE_REQUIRED >= resources_needed[i]) {
			
				int excess_amount = player_temp_resource_set.getAmount(i+1) - ApplicationConstants.Game.MISC_PORT_RESOURCE_REQUIRED; // MISC_PORT_RESOURCE_REQUIRED is for misc port trade which is 3:1
			
				if(excess_amount >= max) {
					
					if(i == 0)
						trading_resource = "CLAY";
					else if(i == 1)
						trading_resource = "ORE";
					else if(i == 2)
						trading_resource = "SHEEP";
					else if(i == 3)
						trading_resource = "WHEAT";
					else if(i == 4)
						trading_resource = "WOOD";
					
					max = excess_amount;
					
					}
			
			}
		}
		
		return trading_resource;
		
	}	
	
	private String getResourceToTradeWithBank(SOCResourceSet player_temp_resource_set, int [] resources_needed) {
		
		String trading_resource = null;
		
		int max = 0;
		
		// loop over all the needed resources and check which one is excess with us
		
		for(int i = 0; i < resources_needed.length; i++) {
			
			if(player_temp_resource_set.getAmount(i+1) - ApplicationConstants.Game.RESOURCES_REQUIRED_BY_BANK >= resources_needed[i]) {
			
				int excess_amount = player_temp_resource_set.getAmount(i+1) - ApplicationConstants.Game.RESOURCES_REQUIRED_BY_BANK; // RESOURCES_REQUIRED_BY_BANK is for bank trade which is 4:1
			
				if(excess_amount >= max) {
					
					if(i == 0)
						trading_resource = "CLAY";
					else if(i == 1)
						trading_resource = "ORE";
					else if(i == 2)
						trading_resource = "SHEEP";
					else if(i == 3)
						trading_resource = "WHEAT";
					else if(i == 4)
						trading_resource = "WOOD";
					
					max = excess_amount;
					
					}
			
			}
		}
		
		return trading_resource;
		
		
	}
	
	public void planOfActionForInitialSettlement(Message message) {
		
			this.node_messages[this.messageStackFromNodeAgents] = message;
			
			log.info("WAITING FOR ALL REPLIES :: "+this.messageStackFromNodeAgents);
			
			this.messageStackFromNodeAgents++;
			
			if(this.messageStackFromNodeAgents < ApplicationConstants.Game.TOTAL_NODE_AGENTS)
				return;
			else
				this.messageStackFromNodeAgents = 0;
			
			// when we get all the replies from the NodeAgents execute the plan of action
			// sort based on the utilities and execute the action
			// you see there is not a lot of complexity in the initial stages of the game :)
			
		 	log.info("GOT ALL REPLIES!!!");
			
		 	double min = 20000; // just a maximum value to extract the minimum value 
		 	
		 	Message choosen_action = new Message();
		 	
		 	for(int i = 0; i < this.node_messages.length; i++) {
		 		
		 		Message msg = node_messages[i];
		 		
		 		NodeMessage nmsg = (NodeMessage)msg.getMessage();
		 		
		 		log.info("UTILITY AT NODE :: "+msg.getAgentNo()+" :: "+nmsg.getUtility());
		 		
		 		if(nmsg.getUtility() < min) {
		 			
		 			min = nmsg.getUtility();
		 			
		 			choosen_action = msg;
		 			
		 		}
		 	
		 	}
		 	
		 	double choosen_action_utility = ((NodeMessage)choosen_action.getMessage()).getUtility();
		 	
		 	log.info("CHOOSEN ACTION IS :: "+choosen_action.getAgentNo()+" WITH UTILITY :: "+choosen_action_utility);
		 	
		 	this.IA.buildInitialSettlement(choosen_action.getAgentNo());
		 	
		 	log.info("ALL SETTLEMENTS ON THE BOARD :: "+getGame().getBoard().getSettlements());
		 	
		 
		} 
		
		/**
		 * This method sends the message to build initial road to the appropriate Node Agent
		 * 
		 * @param mail
		 */
		private void buildInitialRoads(Message mail){
			
			// detect the appropriate NodeAgent to send the request to 
			NodeAgent [] node_agents = InterfaceAgent.getNodeAgents();
			
			for(int i = 0 ; i < node_agents.length; i++) {
				
				if(node_agents[i].getNode() == mail.getAgentNo()) {
					
					Message message = new Message();
					
					message.setFrom("PlanAgent");
					message.setTo("NodeAgent");
					message.setAgentNo(mail.getAgentNo());
					
					log.debug("ROUTING REQUEST TO NODE AGENT NO :: "+mail.getAgentNo()+" FOR BUILDING A ROAD");
					
					message.setMessage("SUBMIT BID TO PLACE INITIAL ROADS");
					
					InterfaceAgent.MA.mailBox(message);
					
				}
			}
			
		}
		
		private void planOfActionForInitialRoad(Message message) {
			
			log.info("BUILD INITIAL ROAD AT :: "+((NodeMessage)message.getMessage()).getUtility());
			
			this.IA.buildRoad(((NodeMessage)message.getMessage()).getUtility());
				
		}
		
		/**
		 * This is the CRUX of the system
		 * 
		 *
		 */
		
		private void planMove() {
			
			// first up we send request to Node Agents to submit the utilities along with the actions
			
			NodeAgent [] node_agent = InterfaceAgent.getNodeAgents();
			
			for(int i = 0; i < node_agent.length; i++) {
				
				Message message = new Message();
				
				message.setFrom("PlanAgent");
				message.setTo("NodeAgent");
				message.setAgentNo(node_agent[i].getNode());
				message.setMessage("SUBMIT BID TO MAKE MOVE");
				
				InterfaceAgent.MA.mailBox(message); // message sent to MailAgent
				
			}
			
			
		
		
		} // end of planMove()
		
		private void planOfActionForBestMove(Message message) {
			
			log.info("PLAN OF ACTION FOR BEST MOVE");
			
			// wait for all the messages from the NodeAgents
			
			this.node_messages[this.messageStackFromNodeAgents] = message;
			
			log.info("WAITING FOR ALL REPLIES :: "+this.messageStackFromNodeAgents);
			
			this.messageStackFromNodeAgents++;
			
			if(this.messageStackFromNodeAgents < ApplicationConstants.Game.TOTAL_NODE_AGENTS)
				return;
			else
				this.messageStackFromNodeAgents = 0;
			
			
			/**
			 * after receiving all the replies from the NodeAgents
			 * we sort the utilities in ascending order
			 * pick 1 action, check if we have resources to build it
			 * it we have the required resources, build it
			 * else ask the trader agent for help
			 * 
			 */
			
			PlanSorter plan_sorter = new PlanSorter();
			
			Vector parent = new Vector();
			
			
			for(int i = 0; i < node_messages.length; i++) {
				
				log.info("IN PLAN SORTER LOOP");
				
				// for each message check whether we have set a city 
				// settlement and/or road. We would be storing a Vector of
				// Vectors. 
				
				Message msg = node_messages[i];
				
				NodeMessage nmsg = (NodeMessage)msg.getMessage();
				
				// check if message has settlement set
				
				if(nmsg.getSettlement()) {
					
					Vector temp = new Vector();
					
					temp.add("SETTLEMENT"); // piece
					temp.add(msg.getAgentNo()); // coordinate
					temp.add(nmsg.getUtility()); // utility
					
					parent.add(temp);
										
				} 
				
				if(nmsg.getCity()) {
					
					Vector temp = new Vector();
					
					temp.add("CITY"); // piece
					temp.add(msg.getAgentNo()); // coordinate
					temp.add(nmsg.getUtility()); // utility
					
					parent.add(temp);
					
				}
				
				if(nmsg.getRoad()) {
					
					Vector temp = new Vector();
					
					temp.add("ROAD"); // piece
					temp.add(nmsg.getRoadCoordinate()); // coordinate
					temp.add(nmsg.getRoadUtility()); // utility
					
					parent.add(temp);
					
				}
				
			}
			
			// upto this point we would be having all the plan of actions stored in the 
			// plan sorter. now we sort the plan of actions in non decreasing order
			
			Collections.sort(parent, plan_sorter);
			
			log.info("AFTER SORT :: "+parent);
			
			// now we have the sorted list of actions. 
			// check each action
			// if have the resources execute it, else ask trader for trade
			// end when all the actions have been exhausted
			
			// we shift to another method
			
			this.parent = parent;
			
			actionStack(this.parent);
			
//			for(int i = 0; i < parent.size(); i++) {
//				
//				log.info("TAKE ACTION LOOP");
//				
//				takeAction((Vector)parent.get(i));
//				
//			}
			
			
			
		}
		
		public void actionStack(Vector parent) {
			
			log.info("IN ACTION STACK");
		
			if(parent.size() > 0) {
				
				log.info("TAKE ACTION");
				
				plan_left = parent;
				
				takeAction((Vector)parent.remove(0));
				
				
				
			} else { // we do not have any actions left just ask IA to end turn
				
				// we could check based on the availability of resources 
				// to purchase a card before ending the turn 
				// we could also check if we have more than 7 resources
				// we could preferably inverst into something
				
				// send a message to the cards agent to check 
				// if a card has to be purchased 
				
				Message message = new Message();
				
				message.setFrom("PlanAgent");
				message.setTo("CardsAgent");
				message.setMessage("PURCHASE CARD");
				
				
				// we would end the turn now after invest resources
				
				log.info("END TURN");
				
				endTurn();
				
				
			}
			
		}
		
		
		/**
		 * This method is responsible for checking if the action can be executed
		 * if it is possible, execute it else send to the trader agent for trading
		 * strategy
		 * 
		 * @param parent
		 */
		
		synchronized public void takeAction(Vector plan) { // called by IA as well
			
			log.info("IN TAKE ACTION");
			
			log.info("PLAN LEFT :: "+plan_left);
			
			String piece = (String)plan.elementAt(0); // the piece settlement/city/road
			
			int coordinate = ((Integer)plan.elementAt(1)).intValue();
			
			double utility = ((Double)plan.elementAt(2)).doubleValue();
			
			log.info("PIECE :: "+piece);
			
			log.info("COORDINATE :: "+coordinate);
			
			log.info("UTILITY :: "+utility);
			
			//////////////////// RESOURCES NEEDED TO BUILD A PIECE //////////////////////
	
			Hashtable SETTLEMENTS = (Hashtable)ApplicationConstants.ResourcesRequired.RequiredResourcesTable.get("SETTLEMENT");
			
			log.info("RESOURCES NEEDED FOR SETTLEMENTS :: "+SETTLEMENTS);
			
			int clay_settlement = ((Integer)SETTLEMENTS.get("CLAY")).intValue();
			int ore_settlement = ((Integer)SETTLEMENTS.get("ORE")).intValue();
			int sheep_settlement = ((Integer)SETTLEMENTS.get("SHEEP")).intValue();
			int wheat_settlement = ((Integer)SETTLEMENTS.get("WHEAT")).intValue();
			int wood_settlement = ((Integer)SETTLEMENTS.get("WOOD")).intValue();
			
			Hashtable CITIES = (Hashtable)ApplicationConstants.ResourcesRequired.RequiredResourcesTable.get("CITY");
			
			log.info("RESOURCES NEEDED FOR CITIES :: "+CITIES);
			
			int clay_city = ((Integer)CITIES.get("CLAY")).intValue();
			int ore_city = ((Integer)CITIES.get("ORE")).intValue();
			int sheep_city = ((Integer)CITIES.get("SHEEP")).intValue();
			int wheat_city = ((Integer)CITIES.get("WHEAT")).intValue();
			int wood_city = ((Integer)CITIES.get("WOOD")).intValue();
			
			Hashtable ROADS = (Hashtable)ApplicationConstants.ResourcesRequired.RequiredResourcesTable.get("ROAD");
			
			log.info("RESOURCES NEEDED FOR ROADS :: "+ROADS);
			
			int clay_road = ((Integer)ROADS.get("CLAY")).intValue();
			int ore_road = ((Integer)ROADS.get("ORE")).intValue();
			int sheep_road = ((Integer)ROADS.get("SHEEP")).intValue();
			int wheat_road = ((Integer)ROADS.get("WHEAT")).intValue();
			int wood_road = ((Integer)ROADS.get("WOOD")).intValue();
			
		
			/////////////////////////////////////////////////////////////////////////////
		
			// check if the piece is a settlement, city or road
			// if we have the resources to build the piece, 
			// and we have not already built it, go ahead
			// else ask the trader about it
			
			SOCResourceSet player_resources = getPlayer().getResources(); 
			
			if(piece.equals("SETTLEMENT") && getPlayer().isPotentialSettlement(coordinate)) {
				
				// check if we have sufficient pieces to build the settlement
				
				SOCResourceSet needed_settlement_resource_set = new SOCResourceSet(clay_settlement, ore_settlement, sheep_settlement, wheat_settlement, wood_settlement, 0);
				
				if(player_resources.contains(needed_settlement_resource_set)) {
					
					// execute the action and notify
					log.info("WE DO HAVE RESOURCES TO BUILD THE SETTLEMENT");
					
					this.IA.buildSettlementInMove(coordinate, plan_left);
					
				} else {
					
					// calculate the resources we are lagging and ask the trader to arrange those resources
					
					log.info("WE NEED TO TRADE TO BUILD THE SETTLEMENT");
					
					SOCResourceSet lagging_resource_set = getLaggingResourceSet(needed_settlement_resource_set);
					
					Message message = new Message();
					
					message.setFrom("PlanAgent");
					message.setTo("TraderAgent");
					
					TraderMessage tmessage = new TraderMessage();
					
					tmessage.setBuildingPiece("SETTLEMENT");
					
					tmessage.setLaggingResourceSet(lagging_resource_set);
					
					tmessage.setNeededResourceSet(needed_settlement_resource_set);
					
					tmessage.setPlan(plan);
					
					tmessage.setTradeLoop(0);
					
					message.setMessage(tmessage);
					
					//InterfaceAgent.MA.mailBox(message); // ask the trader to send a trade offer
					
					
				}
					
				
				
			} else if(piece.equals("SETTLEMENT") && !getPlayer().isPotentialSettlement(coordinate)) {
					
				// check the other action in the stack
				
				this.actionStack(plan_left);
				
				
			} else if(piece.equals("CITY") && getPlayer().isPotentialCity(coordinate)) {
				
				SOCResourceSet needed_city_resource_set = new SOCResourceSet(clay_city, ore_city, sheep_city, wheat_city, wood_city, 0);
				
				if(player_resources.contains(needed_city_resource_set)) {
					
					// execute the action and notify
					log.info("WE DO HAVE RESOURCES TO BUILD THE CITY");
					
					this.IA.buildCityInMove(coordinate, plan_left);
								
					//this.actionStack(this.plan_left);
					
				} else {
					
					// calculate the resources we are lagging and ask the trader to arrange
					
					log.info("WE NEED TO TRADE TO BUILD THE CITY");
					
					SOCResourceSet lagging_resource_set = getLaggingResourceSet(needed_city_resource_set);
					
					Message message = new Message();
					
					message.setFrom("PlanAgent");
					message.setTo("TraderAgent");
					
					TraderMessage tmessage = new TraderMessage();
					
					tmessage.setBuildingPiece("CITY");
					
					tmessage.setLaggingResourceSet(lagging_resource_set);
					
					tmessage.setNeededResourceSet(needed_city_resource_set);
					
					tmessage.setTradeLoop(0);
					
					tmessage.setPlan(plan);
					
					message.setMessage(tmessage);
					
					//InterfaceAgent.MA.mailBox(message); // ask the trader to send a trade offer
					
					
				}
				
				
				
			} else if(piece.equals("CITY") && !getPlayer().isPotentialCity(coordinate)) {
				
				this.actionStack(plan_left);
			
			} else if(piece.equals("ROAD") && getPlayer().isPotentialRoad(coordinate)) {
				
				SOCResourceSet needed_road_resource_set = new SOCResourceSet(clay_road, ore_road, sheep_road, wheat_road, wood_road, 0);
				
				if(player_resources.contains(needed_road_resource_set)) {
					
					// execute the action and notify
					log.info("WE DO HAVE RESOURCES TO BUILD THE ROAD");
					
					this.IA.buildRoadInMove(coordinate, plan_left);
					
					//this.actionStack(this.plan_left);
					
				} else {
					
					// calculate the resources we are lagging and ask the trader to arrange
					
					log.info("WE NEED TO TRADE TO BUILD THE ROAD");
					
					SOCResourceSet lagging_resource_set = getLaggingResourceSet(needed_road_resource_set);
					
					Message message = new Message();
					
					message.setFrom("PlanAgent");
					message.setTo("TraderAgent");
					
					TraderMessage tmessage = new TraderMessage();
					
					tmessage.setBuildingPiece("ROAD");
					
					tmessage.setLaggingResourceSet(lagging_resource_set);
					
					tmessage.setNeededResourceSet(needed_road_resource_set);
					
					tmessage.setTradeLoop(0);
					
					tmessage.setPlan(plan);
					
					message.setMessage(tmessage);
					
					//InterfaceAgent.MA.mailBox(message); // ask the trader to send a trade offer
					
					
				}
				
				
			} else if(piece.equals("ROAD") && !getPlayer().isPotentialRoad(coordinate)) {
				
				this.actionStack(plan_left);
				
			}
			
		}
		
		public void releaseAction() {
			
			notify();
			
		}
		
		private SOCResourceSet getLaggingResourceSet(SOCResourceSet needed_settlement_resource_set) {
			
			SOCResourceSet lagging_resource_set = new SOCResourceSet();
			
			SOCResourceSet player_resources = getPlayer().getResources();
			
			// check which and how many of the resource types we need
			
			for(int i = 1; i <= 5; i++) {
				
				if(needed_settlement_resource_set.getAmount(i) > 0) {
					
					if(player_resources.getAmount(i) < needed_settlement_resource_set.getAmount(i))
						lagging_resource_set.setAmount(needed_settlement_resource_set.getAmount(i) - player_resources.getAmount(i), i);
						
						
						
				}
			}
			
			
			return lagging_resource_set;
			
		}
		
		public void makeTradeOffer(SOCResourceSet give_resource_set, SOCResourceSet get_resource_set) {
			
			this.IA.makeTradeOffer(give_resource_set, get_resource_set);
			
		}
		
		public void makeBankTradeOffer(SOCResourceSet give_resource_set, SOCResourceSet get_resource_set) {
			
			this.IA.bankTrade(getGame(), give_resource_set, get_resource_set);
		}
		
		
		private void noTradeOffer() {
			
			// reset tradeloop
			
			this.IA.tradeloop = 0;
			
			this.actionStack(this.parent);
			
		}
		
		private void endTurn() {
			
			this.IA.endTurn(getGame());
			
		}
		
		/**
		 * this method takes care of if we have more than 7 resources
		 * this method would rely on how many times has 7 been rolled
		 * and also the market worth of a specific type of resource
		 * at the end of the method, we can just end the turn
		 * 
		 */ 
		
		public void investResources() {
			
			// we make the logic here where we can invest resources
			// one thing could be to collect the valuable resource,
			// which is being traded regressively.
			// we could check if we can trade with a 2:1 or 3:1 port
			
			
			// we need to set something in order to know that we need to end the turn as well
			
			
			
		} 
		
		
}