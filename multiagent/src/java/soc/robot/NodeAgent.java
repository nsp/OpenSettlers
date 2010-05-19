/**
 * 	This Agent is responsible for submitting the bids to the Plan agent which 
 * 	makes the plan
 * 
**/

package soc.robot;

import java.util.*;
import soc.message.Message;
import soc.message.NodeMessage;
import soc.game.*;

import java.io.*;
import soc.util.Loggers;
import soc.util.ApplicationConstants;
import soc.util.ApplicationConstants.Game;

import org.apache.log4j.*;



public class NodeAgent extends Agent implements Runnable {
	
	private int node;
	private Thread NodeAgentThread;
	private Hashtable NodeValidMoves;
	
	private SOCPlayer player;
	private SOCGame game;
	private Hashtable ResourceFrequencyHash; 
	
	
	public NodeAgent(int node) {
		
		this.node = node;
		
		NodeAgentThread = new Thread(this);
		
		NodeValidMoves = new Hashtable();
		
		messages = new Vector();
		
		log = Loggers.NodeAgentLogger;
		
	}
	
	public void setResourceFrequency(Hashtable ResourceFrequencyHash) {
		
		this.ResourceFrequencyHash = ResourceFrequencyHash;
		
	}
	
	public Hashtable getFrequenciesForResources() {
		
		return ResourceFrequencyHash;
		
	}
	
	/**
	 * This method is responsible for adding a proposed valid plan of action for NodeAgent
	 * 
	 * @param coordinate This is the coordinate on the Board where something can be built
	 * @param piece This is the piece which can be settlement, city or road
	 */
	
	public void addPlan(int coordinate, String piece) {
		
		NodeValidMoves.put(coordinate, piece);
		
	} 
	
	
	public int getNode() {
		
		return node;
		
	}
	
	public void mailBox(Message message) {
		
		messages.add(message);
		
		
		
	}
	
	public boolean isAliveThread() {
		
		return NodeAgentThread.isAlive();
		
	}
	
	public void startThread() {
		
		NodeAgentThread.start();
		
	}
		
	public void run() {
		
		player = getPlayer();
		game = getGame();
		
		while(true) {
		
			if(messages.size() > 0) {
				
				for(int i = 0; i < messages.size(); i++) {
					
					Message mail = (Message)messages.get(i);
					
					// read the message and take the appropriate action
						
					if(mail.getFrom().equals("PlanAgent") && ((String)mail.getMessage()).equals("SUBMIT BID TO PLACE INITIAL SETTLEMENTS")) {
						
						log.info("MESSAGE FROM PLAN AGENT TO NODE AGENT :: "+mail.getAgentNo()+" >> SUBMIT BID TO PLACE INITIAL SETTLEMENTS");
						Message message = sendInitialSettlementsPlan();
						
						InterfaceAgent.MA.mailBox(message); // this message now contains the UTILITY for building an initial settlement at this node coordinate
					
					} else if(mail.getFrom().equals("PlanAgent") && ((String)mail.getMessage()).equals("SUBMIT BID TO PLACE INITIAL ROADS")) {
						
						log.info("MESSAGE FROM PLAN AGENT TO NODE AGENT :: "+mail.getAgentNo()+" >> SUBMIT BID TO PLACE INITIAL ROADS");
						
						Message message = sendInitialRoadsPlan(mail);
						
						InterfaceAgent.MA.mailBox(message);
						
					} else if(mail.getFrom().equals("PlanAgent") && ((String)mail.getMessage()).equals("SUBMIT BID TO MAKE MOVE")) {
						
						planBestMoveAtNode();
						
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
	 * This method is used to build the initial roads 
	 * The method returns the 
	 * 
	 * @return
	 */
	
	private Message sendInitialRoadsPlan(Message msg) {
		
		
		SOCGame game = getGame();
		
		/*
		 * here we compute the utility as 
		 * if we have an edge which connects any of our roads that is highly preferred as that can lead us to 2 VP for longest road
		 * if we somehow are blocking an opponent, that also is nice
		 * if one side of edge is water that is also nice	
		 * 
		 * each edge is checked if it is a potential road edge
		 * 
		 */
		
		
		Message message = new Message();
		
		int UTILITY = 0; // here the UTILITY is the edge where the road should be built 
		
		Hashtable utility_table = new Hashtable();
		
		log.info("SEND INITIAL ROADS PLAN METHOD CALLED");
		
		
		Vector adjNodes = game.getBoard().getAdjacentNodesToNode(msg.getAgentNo());
		
		for(int i = 0; i < adjNodes.size(); i++) {
			
			Integer ii = (Integer)adjNodes.get(i);
			
			log.info("ADJACENT NODES TO NODE :: "+msg.getAgentNo()+" :: "+ii.intValue());
			
		}
		
		Vector adjEdges = game.getBoard().getAdjacentEdgesToNode(msg.getAgentNo());
		
		log.info("ADJACENT EDGES TO NODE :: "+msg.getAgentNo()+" ARE :: "+adjEdges);
		
		int [] edge_coordinates = new int[adjEdges.size()];
		
		for(int i = 0; i < adjEdges.size(); i++) {
			
			Integer ii = (Integer)adjEdges.get(i);
			
			// check if the adjacent edge is a potential road location
			
			int edge_coordinate = ii.intValue();
			
			edge_coordinates[i] = edge_coordinate;
			
			if(getPlayer().isPotentialRoad(edge_coordinate)) {
				
				log.info("ADJACENT EDGE :: "+edge_coordinate+" TO NODE :: "+msg.getAgentNo()+" IS A POTENTIAL ROAD LOCATION");
				
				Vector adjacent_nodes_to_edge = game.getBoard().getAdjacentNodesToEdge(edge_coordinate);
				
				log.info("ADJACENT NODES TO edge_coordinate :: "+edge_coordinate+" ARE :: "+adjacent_nodes_to_edge);
				// now get the node coordinate other than the one we have
				
				int selected_adjacent_node = 0;
				
				for(int k = 0; k < adjacent_nodes_to_edge.size(); k++) {
					
					Integer adjacent_node = (Integer)adjacent_nodes_to_edge.get(k);
					
					if(adjacent_node.intValue() != msg.getAgentNo()) 
						selected_adjacent_node = adjacent_node.intValue();
					
				}
				
				log.info("SELECTED ADJACENT NODE TO edge_coordinate :: "+edge_coordinate+" IS :: "+selected_adjacent_node);
				
				// now we need to get the adjacent edges to the selected adjacent node
				
				Vector adjEdgesToSelectedNode = game.getBoard().getAdjacentEdgesToNode(selected_adjacent_node);
				
				log.info("ADJACENT EDGES TO SELECTED NODE :: "+selected_adjacent_node+" ARE :: "+adjEdgesToSelectedNode);
				// now check for each edge whether there is our road or opponent road
				
				for(int l = 0; l < adjEdgesToSelectedNode.size(); l++) {
					
					Integer adjacentEdge = (Integer)adjEdgesToSelectedNode.get(l);
					
					if(adjacentEdge.intValue() != edge_coordinate) {
						
						log.info("VALID SELECTED EDGE TO SELECTED NODE :: "+selected_adjacent_node+" IS :: "+adjacentEdge.intValue());
						// check whether this edge is our road or the opponent road
						
						//log.info("THE SELECTED EDGE :: "+adjacentEdge.intValue()+" TO SELECTED NODE :: "+selected_adjacent_node+" IS :: NOT POTENTIAL");
							// check whether it is our or opponents
							if(this.isOurRoad(adjacentEdge.intValue()).equals("MY")) {
								
								log.info("THE SELECTED EDGE :: "+adjacentEdge.intValue()+" TO SELECTED NODE :: "+selected_adjacent_node+" IS :: OUR ROAD");
								utility_table.put(edge_coordinate, 100);
								break;						
							} else if (this.isOurRoad(adjacentEdge.intValue()).equals("OPPONENT")){
								
								log.info("THE SELECTED EDGE :: "+adjacentEdge.intValue()+" TO SELECTED NODE :: "+selected_adjacent_node+" IS :: OPPONENTS ROAD");
								utility_table.put(edge_coordinate, 50);
								break;
								
							} else if(this.isOurRoad(adjacentEdge.intValue()).equals("AVAILABLE")){
							
							log.info("THE SELECTED EDGE :: "+adjacentEdge.intValue()+" TO SELECTED NODE :: "+selected_adjacent_node+" IS :: AVAILABLE");
							utility_table.put(edge_coordinate, 0);
						
							}
							
						}	
					
					}
				
				}
				
				
			}
			
		if(utility_table.size() == 0) {
			
			// randomly select an edge coordinate as UTILITY
			log.info("RANDOMLY SELECT AN EDGE TO BUILD A ROAD");
			
			int random = new Random(2).nextInt();
			
			UTILITY = edge_coordinates[random];
			
		} else {
			
			// if we have something in the utility_table, get the edge with maximum benefit
			log.info("SELECT THE HIGHEST UTILITY ROAD EDGE");
			
			int max = 0;
			
			Enumeration iterator = utility_table.keys();
			
			while(iterator.hasMoreElements()) {
				
				int edge_coordinate = ((Integer)iterator.nextElement()).intValue();
				
				log.debug("EDGE COORDINATE IS :: "+edge_coordinate+" AND VALUE IS :: "+((Integer)utility_table.get(edge_coordinate)).intValue());
				
				if(((Integer)utility_table.get(edge_coordinate)).intValue() >= max ) {
					
					max = ((Integer)utility_table.get(edge_coordinate)).intValue();
					
					UTILITY = edge_coordinate;
					
				}
						
			}
			
//			for(int jj = 0; jj < edge_coordinates.length; jj++) {
//				
//				if(((Integer)utility_table.get(edge_coordinates[jj])).intValue() >= max) {
//					
//					max = ((Integer)utility_table.get(edge_coordinates[jj])).intValue();
//					
//					UTILITY = edge_coordinates[jj];
//					
//				} 
//					
//			}
//			
		}
		
		log.info("UTILITY TO BUILD A ROAD IS :: "+UTILITY);
		
		message.setFrom("NodeAgent");
		
		message.setTo("PlanAgent");
		
		message.setAgentNo(msg.getAgentNo());
		
		NodeMessage nmessage = new NodeMessage();
		
		nmessage.setResponseHeader("INITIAL ROAD UTILITY");
		
		nmessage.setUtility(UTILITY); // here utility is the edge to make the road on
		
		message.setMessage(nmessage);
		
		return message;
		
	}
	
	/**
	 * this method is used to submit the overall utility of  
	 * building a settlement at this spot	 
	 * 	
	 * @return utility
	 */
	
	private Message sendInitialSettlementsPlan() {
		
		SOCGame game = getGame();
		
		SOCPlayer player = getPlayer();
		
		Message message = new Message(); // the plan of action is returned as a Message to the PlanAgent
		
		/**
		 * there are multiple factors kept in mind while sending an educated utility 
		 * of building the initial settlements
		 * we need to check whether this is a potential settlement
		 * if it is not then we simply send a message saying that the settlement cannot be built
		 * if we can build a settlement here we need to consider certain factors
		 * the building speed considering that this settlement has been built <less no of turns is preferred>
		 * Note :: for building the initial settlements and roads we dont consider taking a 
		 * ratio as we dont have to pay any resource cards to purchase the piece
		 * 
		 * the type of hexes around the coordinate <all different hexes are preferred>
		 * additional points for 2:1 and 3:1 port hexes, they give an edge in later stages to avoid trading with other players and sending resources to bank
		 * 
		 * the number on the touching hexes <high probability hexes are preferred>
		 * 
		 * calculation is done as follows:
		 * we need to keep the utility to a minimum following the pattern of min no of turns
		 * 
		 * priority to all different hexes > two same hexes > all same hexes
		 * 
		 * the sum of the probabilities of numbers is multiplied by 10 and rounded 
		 * the higher the result, the better
		 * 
		 */
		
		message.setFrom("NodeAgent");
		message.setTo("PlanAgent");
		message.setAgentNo(getNode());

		int UTILITY = 0;
		
		// the NodeMessage would determine if there is a possible action or no
		
		if(player.isPotentialSettlement(getNode())) {
			
			log.info("IS POTENTIAL SETTLEMENT AT NODE :: "+getNode());
			
			SOCSettlement soc_settlement = new SOCSettlement(player, getNode());
			
			// total estimated building speed by building this settlement at this coordinate
			
			int building_speed = InterfaceAgent.PA.calculateBuildingSpeed(soc_settlement);
			
			log.info("TOTAL BUILDING SPEED AT NODE FOR INITIAL SETTLEMENT AT NODE :: "+getNode()+" :: "+building_speed);
			
			// check what are the adjacent hexes around 
			
			Vector adjHexes = game.getBoard().getAdjacentHexesToNode(getNode());
			
			int [] numbers_on_hexes = new int[3]; // array to store numbers on the adjacent hexes
			
			int [] types_of_hexes = new int[3]; // array to store the type of hexes around
				
			for(int i = 0; i < adjHexes.size(); i++) {
				
				Integer ii = (Integer)adjHexes.get(i); // get the adjacent hex
				
				numbers_on_hexes[i] = game.getBoard().getNumberOnHexFromCoord(ii.intValue()); // number on the adjacent hex
				
				types_of_hexes[i] = game.getBoard().getHexTypeFromCoord(ii.intValue());
				
			}
			
			log.info("NUMBERS ON ADJACENT HEXES TO :: "+getNode()+" :: "+numbers_on_hexes);
			
			log.info("TYPES ON ADJACENT HEXES TO :: "+getNode()+" :: "+types_of_hexes);
			
			
			int type_hex_provision = 0;
			
			// based on the types of the hexes around we would be able to come up with 
			// this number that adds or subtracts from the total 
			
			// type hex number is generated based on the following rules
			
			// THE DESERT HEX AND WATER HEX ARE PENALIZED WHILE PORT HEXES ARE PREFERRED
			
			// CHECK IF ALL THE HEXES ARE DIFFERENT TYPES
			
			if(types_of_hexes[0] != types_of_hexes[1] && types_of_hexes[1] != types_of_hexes[2] && types_of_hexes[0] != types_of_hexes[2]) {
				
				log.info("ALL THE HEXES ADJACENT TO :: "+getNode()+" ARE DIFFERENT");
				
				type_hex_provision += ApplicationConstants.Game.ALL_DIFFERENT_HEX_TYPES_AROUND;
				
			} 
			else if(types_of_hexes[0] == types_of_hexes[1] && types_of_hexes[1] == types_of_hexes[2] && types_of_hexes[0] == types_of_hexes[2]) {
				
				log.info("ALL THE HEXES ADJACENT TO :: "+getNode()+" ARE SAME");
				
			}
				
			else { // by this time we found out that two are same and 1 is different 
				
				log.info("ATLEAST TWO HEXES ADJACENT TO :: "+getNode()+" ARE DIFFERENT");
				
				type_hex_provision += ApplicationConstants.Game.TWO_DIFFERENT_HEX_TYPES_AROUND;
				
			}
			
			// now we would check if we have adjacent desert hex, water hex, 2:1 port hex or 3:1 port hex and add weights appropriately
			
			for(int i = 0; i < types_of_hexes.length; i++) {
				
				switch(types_of_hexes[i]) {
				
					case SOCBoard.DESERT_HEX : 
						log.info("THERE IS A DESERT HEX ADJACENT TO :: "+getNode());
						type_hex_provision += ApplicationConstants.Game.DESERT_HEX_PENALITY; break;
					case SOCBoard.WATER_HEX : 
						log.info("THERE IS A WATER HEX ADJACENT TO :: "+getNode());
						type_hex_provision += ApplicationConstants.Game.WATER_HEX_PENALITY; break;
					case SOCBoard.CLAY_PORT_HEX :
						case SOCBoard.ORE_PORT_HEX : 
							case SOCBoard.SHEEP_PORT_HEX : 
								case SOCBoard.WHEAT_PORT_HEX :
									case SOCBoard.WOOD_PORT_HEX :  	
										log.info("THERE IS A 2:1 PORT HEX ADJACENT TO :: "+getNode());
										type_hex_provision += ApplicationConstants.Game.TWO_ONE_PORT_UTILITY; break;
				
					case SOCBoard.MISC_PORT_HEX : 
						log.info("THERE IS A 3:1 PORT HEX ADJACENT TO :: "+getNode());
						type_hex_provision += ApplicationConstants.Game.THREE_ONE_PORT_UTILITY; break;
					
				}
				
			}
			
			// finally type_hex_provision has the value assigned to the types of hexes around
			
			// as far as the numbers on the adjacent hexes are concerned
			// we add up all the probabilities of the non zero numbers and multiply them 
			// with a judged weight of 10
			
			double numbers_hex_provision = 0;
			
			for(int i = 0; i < numbers_on_hexes.length; i++) { 
				
				if(numbers_on_hexes[i] > 0) { // dont take 0 into consideration
					
					Double ii = (Double)ApplicationConstants.DiceProbabilities.DiceProbabilityTable.get(numbers_on_hexes[i]);
					
					numbers_hex_provision += ii.doubleValue();
					
				}
				
			}
			
			// finally multiply the numbers_hex_provision with a weighted factor 
			
			numbers_hex_provision *= ApplicationConstants.Game.NUMBERS_HEX_PROVISION_WEIGHT;
			
			log.info("NUMBERS HEX PROVISION ADJACENT TO :: "+getNode()+" IS "+((Double)Math.ceil(numbers_hex_provision)).intValue());
			// finally we compile all these values to get the utility of building a piece at this coordinate
			
			log.warn("BUILDING SPEED ON NODE :: "+getNode()+" IS "+building_speed);
			log.warn("TYPE HEX PROVISION ON NODE :: "+getNode()+" IS "+type_hex_provision);
			log.warn("NUMBER HEX PROVISION ON NODE :: "+getNode()+" IS "+((Double)Math.ceil(numbers_hex_provision)).intValue());
			
			UTILITY = building_speed - type_hex_provision - ((Double)Math.ceil(numbers_hex_provision)).intValue();
			
			// send the calculated utility back to Plan Agent
			
		}
		else { 
			
			log.info("IS NOT POTENTIAL SETTLEMENT :: "+getNode());
			
			UTILITY = 20000; // such a high utility means no chance to be selected!!!
			
		}
		
		NodeMessage nmessage = new NodeMessage();
		
		nmessage.setResponseHeader("INITIAL SETTLEMENT UTILITY");
		
		nmessage.setUtility(UTILITY);
		
		message.setMessage(nmessage);
		
		return message; 
		
	}
	
	private NodeMessage makeOptimalNodePlan() throws IOException {
		
		NodeMessage message = new NodeMessage();
	
		// check for valid moves and set them in node message
		// get the frequencies for each resource
		// check for both settlements and cities
		// also check if any of the settlement or city touches the 
		// port set that to true as well
		
		//Hashtable rf = getFrequenciesForResources();
		
		
//		Vector adjNodes = game.getBoard().getAdjacentNodesToNode(getNode());
//		
//		System.out.println("adjNodes to Node :: "+getNode()+" :: "+adjNodes);
//		
//		Vector adjHexes = game.getBoard().getAdjacentHexesToNode(getNode());
//	
//		System.out.println("adjHexes to Node :: "+getNode()+" :: "+adjHexes);
//
//		game.getBoard().setNumberLayout(game.getBoard().getNumberLayout());
		
//		for(int i = 0; i < adjHexes.size(); i++) {
//			
//			Integer ii = (Integer)adjHexes.get(i);
//			System.out.println("number on hex :: "+adjHexes.get(i)+" :: "+game.getBoard().getNumberOnHexFromCoord(ii.intValue()));
//			
//		}
		
	 
		
		//player.getSettlements();
		
		
		
//		for(int i=0; i < hex.length; i++) {
//			
//			Vector adjhexes = game.getBoard().getAdjacentHexesToNode(hex[i]);
//			
//			for(int j = 0; j < adjhexes.size(); j++) {
//				
//				Integer ii = (Integer)adjhexes.elementAt(j);
//				
//				System.out.println("HEX TYPE TO :: "+hex[i]+" is "+game.getBoard().getHexTypeFromCoord(ii.intValue()));
//				
//				writer.write("HEX TYPE TO :: "+hex[i]+" is "+game.getBoard().getHexTypeFromCoord(ii.intValue())+"\n");
//				
//			}
//			
//			
//			
//		}
//		
//		writer.close();
		
//		Vector Settlements = player.getSettlements();
//		
//		for(int i = 0; i < Settlements.size(); i++) {
//			
//			SOCSettlement socsettlement = (SOCSettlement)Settlements.elementAt(i);
//			
//			Vector adjhexes = socsettlement.getAdjacentHexes();
//			
//			// get the resource types from the adjhexes 
//			// this tells us which resources could be produced
//			
//			for(int j = 0; j < adjhexes.size(); j++){
//				
//				Integer ii = (Integer)adjhexes.elementAt(j);
//				
//				System.out.println("HEX TYPE IS :: "+game.getBoard().getHexTypeFromCoord(ii.intValue()));
//				
//				
//			}
//			
//			
//		}
		
		
		return message;
		
	}
	
	
	private String isOurRoad(int edge) {
		
		String road = "AVAILABLE";
		
		Vector myroads = getPlayer().getRoads();
		
		for(int i = 0; i < myroads.size(); i++) {
			
			SOCRoad edge_of_my_road = (SOCRoad)myroads.get(i);
			
			if(edge_of_my_road.getCoordinates() == edge) 
				return "MY";
		
		}
		
		Vector roads = getGame().getBoard().getRoads();
		
		for(int j = 0; j < roads.size(); j++) {
			
			SOCRoad all_roads = (SOCRoad)roads.get(j);
			
			if(all_roads.getCoordinates() == edge) 
				return "OPPONENT";
		}
		
		return road;
	
	}
	
	/*	This method is responsible for sending the best possible move at this node to the Plan Agent
	 * 
	 * 
	 */
	
	private void planBestMoveAtNode() {
		
		SOCPlayer player = getPlayer();
		SOCGame game = getGame();
		
	/**
	 * we check if we can build a settlement. if we can, we check what are the other pieces 
	 * around. if our pieces, thats a benefit. if opponents blockage thats even better.
	 * similarly if we already have our settlement built on this node, check for converting
	 * to city which is always good obviously keeping the above mentioned utility weights 
	 * in mind. 
	 * also, check to see what good would it do to build a road.
	 * a road could be checked for potentiality and then it could be thought of building here
	 * 
	 * ACTUALLY THERE ARE TWO ALTERNATE SCENARIOS HERE. 
	 * ONE BEING THAT WE COULD JUST SEND THE BEST OPTION OUT THE AVAILABLE AT THIS NODE
	 * THE SECOND BEING IF WE COULD SEND MULTIPLE OPTIONS FROM HERE
	 * WE GO FOR THE SECOND ONE AS THAT OPENS MORE OPTIONS AT THE PLAN AGENT END TO SEE
	 * IF A PLAN IS FEASIBLE FOR EXECUTION WHICH MIGHT HAVE JUST BEEN DROPPED OUT HERE
	 * 
	 * the calculation is done based on the cost versus utility ratio
	 * 
	 * the minimum of cost versus utility ratio gets the precedence of execution if feasible
	 * 
	 * the cost is calculated based on the 1 + <cards we need> - <cards we have>
	 * the overall utility in case of cities/settlements is 1000 - no of turns - utility from environment
	 * the overall utility in case of roads is 1000 - <a hypothetical turns estimation> - utility from environment 
	 * divide and get a ratio which is the actual utility
	 */
		
		
		SOCResourceSet player_resources = player.getResources();
		
		log.info("MY RESOURCES ARE :: "+player_resources);
		
		// initialize resource sets needed to build settlement, city and roads
		
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
		
		SOCResourceSet settlement_needed_resource_set = new SOCResourceSet(clay_settlement, ore_settlement, sheep_settlement, wheat_settlement, wood_settlement, 0);
		
		log.info("RESOURCES NEEDED FOR SETTLEMENTS :: "+settlement_needed_resource_set);
		
		SOCResourceSet city_needed_resource_set = new SOCResourceSet(clay_city, ore_city, sheep_city, wheat_city, wood_city, 0);
		
		log.info("RESOURCES NEEDED FOR CITY :: "+city_needed_resource_set);
		
		SOCResourceSet road_needed_resource_set = new SOCResourceSet(clay_road, ore_road, sheep_road, wheat_road, wood_road, 0);
		
		log.info("RESOURCES NEEDED FOR ROADS :: "+road_needed_resource_set);
		
		log.info("PLAN BEST MOVE AT NODE :: "+getNode());
		
		double UTILITY_SETTLEMENT = 0;
		double UTILITY_CITY = 0;
		double UTILITY_ROAD = 0;
		
		// first up we calculate the UTILITY OF BUILDING A SETTLEMENT HERE
		
		
		if(player.isPotentialSettlement(getNode()))
				log.info("Node :: "+getNode()+" IS A POTENTIAL SETTLEMENT");
		else 
			log.info("Node :: "+getNode()+" IS A NOT POTENTIAL SETTLEMENT");
		
		if(player.getNumPieces(SOCPlayingPiece.SETTLEMENT) > 0 && player.isPotentialSettlement(getNode())) {
		
			log.info("WE HAVE "+player.getNumPieces(SOCPlayingPiece.SETTLEMENT)+" SETTLEMENTS LEFT");
			// check if we can build a settlement here  
			
				
				log.info("THE NODE :: "+getNode()+" IS A POTENTIAL SETTLEMENT");
				
				SOCSettlement soc_settlement = new SOCSettlement(player, getNode());
				
				// total estimated building speed by building this settlement at this coordinate
				
				int building_speed = InterfaceAgent.PA.calculateBuildingSpeed(soc_settlement);
				
				log.info("BUILDING SPEED OF SETTLEMENT ON NODE :: "+getNode()+" IS "+building_speed);
				
				UTILITY_SETTLEMENT += building_speed;
				
				// check if we have some of our pieces on any of the nodes on the adjacent 
				// hexes
				
				int UTIL = checkIfOurPieceOnNodesOnAdjacentHexes();
				UTILITY_SETTLEMENT += UTIL;
				
				log.info("OUR PIECE IN VICINITY OF SETTLEMENT UTILITY :: "+UTIL);
				
				// if we have a road connecting to this node or an opponent has a road
				// connecting to this node
				
				UTIL = checkIfOurRoadConnectingThisNode();
				
				UTILITY_SETTLEMENT += UTIL;
				
				log.info("OUR / THEIR ROAD CONNECTING UTILITY :: "+UTIL);
				
				UTILITY_SETTLEMENT = ApplicationConstants.Game.HYPOTHETICAL_VALUE_FOR_UTILITY - UTILITY_SETTLEMENT; 
				
				log.info("TOTAL UTILITY OF SETTLEMENT IS :: "+UTILITY_SETTLEMENT);
				
				
			} else if(player.getNumPieces(SOCPlayingPiece.CITY) > 0 && player.isPotentialCity(getNode())) { 
				
				log.info("WE HAVE "+player.getNumPieces(SOCPlayingPiece.CITY)+" CITIES LEFT");
				
				// if a city could be built here
					
				log.info("UPGRADE TO A CITY AT NODE :: "+getNode());
				
				SOCCity soc_city = new SOCCity(player, getNode());
				
				// total estimated building speed by building this settlement at this coordinate
				
				int building_speed = InterfaceAgent.PA.calculateBuildingSpeed(soc_city);
				
				UTILITY_CITY += building_speed;
				
				// check if we have some of our pieces on any of the nodes on the adjacent 
				// hexes
				
				int UTIL = 0;
				
				UTIL = checkIfOurPieceOnNodesOnAdjacentHexes();
				
				UTILITY_CITY += UTIL;
				
				log.info("OUR PIECE IN VICINITY OF SETTLEMENT UTILITY :: "+UTIL);
				
				// if we have a road connecting to this node or an opponent has a road
				// connecting to this node
				
				UTIL = checkIfOurRoadConnectingThisNode();
				
				UTILITY_CITY += UTIL;
				
				log.info("OUR / THEIR ROAD CONNECTING UTILITY :: "+UTIL);
				
				UTILITY_CITY = ApplicationConstants.Game.HYPOTHETICAL_VALUE_FOR_UTILITY - UTILITY_CITY;
				
				log.info("TOTAL UTILITY OF CITY IS :: "+UTILITY_CITY);
				
			}
			
			// check for if we can build a road on any of the edges connecting this node
			
			// we need to check the edge where there is a high enough potential to build 
			// a road. there are two factors here. if we have our rd on the other edge as well
			
			Hashtable edge_utility = new Hashtable();
			
			if(player.getNumPieces(SOCPlayingPiece.ROAD) > 0)
				edge_utility = this.checkHighUtilityRoadEdge();
			
			log.info("ROAD EDGE UTILITIES ARE :: "+edge_utility);
			
			
			// edge_utility now has the potential edge coordinates along with their utility 
			// we could just extract the high utility edge and send to PlanAgent
			
			// CHECK WHETHER THE SETTLEMENT UTILITY OR CITY UTILITY IS HIGHER
			// SEND ONE OF SETTLEMENT/CITY UTILITY AND THE ROAD UTILITY TO PLAN AGENT
			
			NodeMessage nmessage = new NodeMessage();
			
			int max = 0;
			
			Enumeration iterator = edge_utility.keys();
			
			// calculate the cost needed to build the road
			
			int cost = 0;
			
			if(player_resources.contains(road_needed_resource_set))
				cost = 1;
			else 
				cost = 1 + fetchNumberOfResourcesNeeded(road_needed_resource_set);
			
			log.info("COST TO BUILD THE ROAD IS :: "+cost);
			
			double cost_utility_ratio = 0;
			
			while(iterator.hasMoreElements()) {
				
				int edge_coordinate = ((Integer)iterator.nextElement()).intValue();
				
				log.debug("EDGE COORDINATE IS :: "+edge_coordinate+" AND VALUE IS :: "+((Integer)edge_utility.get(edge_coordinate)).intValue());
				
				if(((Integer)edge_utility.get(edge_coordinate)).intValue() >= max) {
					
					max = ((Integer)edge_utility.get(edge_coordinate)).intValue();
					
					nmessage.setRoad(true);
					nmessage.setRoadCoordinate(edge_coordinate);
					
					// the road utility is set as 1000 - <hypothetical value for turns> - edge_utility
					
					UTILITY_ROAD = ApplicationConstants.Game.HYPOTHETICAL_VALUE_FOR_UTILITY - ApplicationConstants.Game.HYPOTHETICAL_VALUE_FOR_ROAD_TURNS - max;
					
					cost_utility_ratio = cost / UTILITY_ROAD;
					
					log.info("COST TO UTILITY RATIO FOR BUILDING ROAD IS :: "+cost_utility_ratio);
					
					nmessage.setRoadUtility(cost_utility_ratio);
					
					log.info("EGDE COORDINATE WITH HIGHEST UTILITY AT NODE :: "+getNode()+" IS "+edge_coordinate);
				
				}
						
			}
			
			int costt = 0;
			
			if(UTILITY_SETTLEMENT > 0) {
				
				
				nmessage.setSettlement(true); 
				
				/**
				 * calculate the actual utility ratio now
				 * we calculate the cost with respect to 1 + the no of cards needed
				 */
				
				
				if(player_resources.contains(settlement_needed_resource_set))
					costt = 1;
				else 
					costt = 1 + fetchNumberOfResourcesNeeded(settlement_needed_resource_set);
				
				log.info("COST TO BUILD SETTLEMENT IS :: "+costt);
				
				double s_cost_utility_ratio = costt / UTILITY_SETTLEMENT;
				
				log.info("SETTLEMENT CAN BE BUILT AT NODE :: "+getNode()+" WITH UTILITY :: "+s_cost_utility_ratio);
				
				nmessage.setUtility(s_cost_utility_ratio);
				
			} else if(UTILITY_CITY > 0) {
				
				log.info("SETTLEMENT CAN BE UPGRADED TO CITY AT NODE :: "+getNode()+" WITH UTILITY :: "+UTILITY_CITY);
				nmessage.setCity(true);
				
				if(player_resources.contains(city_needed_resource_set))
					costt = 1;
				else 
					costt = 1 + fetchNumberOfResourcesNeeded(city_needed_resource_set);
				
				log.info("COST TO BUILD SETTLEMENT IS :: "+costt);
				
				double c_cost_utility_ratio = costt / UTILITY_CITY;
				
				log.info("CITY CAN BE BUILT AT NODE :: "+getNode()+" WITH UTILITY :: "+c_cost_utility_ratio);
				
				nmessage.setUtility(c_cost_utility_ratio);
				
			}
			
			nmessage.setResponseHeader("MOVE UTILITY");
			
			Message message = new Message();
			
			message.setFrom("NodeAgent");
			
			message.setTo("PlanAgent");
			
			message.setAgentNo(getNode());
			
			message.setMessage(nmessage);
			
			InterfaceAgent.MA.mailBox(message);
			
		
	}// end of method
	
	/*
	 * this method is responsible for calculating if we have our pieces on any of the 
	 * nodes which are on the adjacent hexes to this node
	 * 
	 * subtract two per city found in the vicinites
	 * subtract one per settlement found in the vicinities
	 * 
	 */
	
	private int checkIfOurPieceOnNodesOnAdjacentHexes() {
		
		SOCPlayer player = getPlayer();
		SOCGame game = getGame();
		
		int pieces_utility = 0;
		
		Vector adjHexes = game.getBoard().getAdjacentHexesToNode(getNode());
		
		log.info("HEXES ADJACENT TO NODE :: "+getNode()+" ARE :: "+adjHexes);
		
		for(int i = 0 ; i < adjHexes.size(); i++) {
			
			// now for each hex we get the nodes attached to it and for each node except
			// this node we check if we have our city/settlement on it
			
			int adjHexNo = ((Integer)adjHexes.get(i)).intValue();
			
			// the top coordinate is retreived by + 1
			// the top left coordinate is retreived by - 16
			// the top right is + 18
			// the bottom left is -1
			// the bottom right is +33
			// the bottom is +16
			
			// get each coordinate and check if it has our city or settlement on it
			
			int top_coordinate = adjHexNo + 1;
			
			if(top_coordinate != getNode())
				pieces_utility += checkIfOurSettlementOrCity(top_coordinate);
			
			int top_left_coordinate = adjHexNo -16;
			
			if(top_left_coordinate != getNode())
				pieces_utility += checkIfOurSettlementOrCity(top_left_coordinate);
			
			int top_right_coordinate = adjHexNo +18;
			
			if(top_right_coordinate != getNode())
				pieces_utility += checkIfOurSettlementOrCity(top_right_coordinate);

			int bottom_left_coordinate = adjHexNo -1;
			
			if(bottom_left_coordinate != getNode())
				pieces_utility += checkIfOurSettlementOrCity(bottom_left_coordinate);
		
			int bottom_right_coordinate = adjHexNo +33;
			
			if(bottom_right_coordinate != getNode())
				pieces_utility += checkIfOurSettlementOrCity(bottom_right_coordinate);
			
			int bottom_coordinate = adjHexNo +16;
			
			if(bottom_coordinate != getNode())
				pieces_utility += checkIfOurSettlementOrCity(bottom_coordinate);
		
		}
		
		log.info("SETTLEMENT OR CITY ADJACENT TO NODE "+getNode()+" UTILITY IS :: "+pieces_utility);
		
		return pieces_utility;
		
	}
	
	private int checkIfOurSettlementOrCity(int top_coordinate) {
		
		int utility = 0;
		
		if(!getPlayer().isPotentialSettlement(top_coordinate)) {
			
			Vector settlements = getPlayer().getSettlements();
		
			for(int i = 0; i < settlements.size(); i++) {
				
				SOCSettlement soc_settlement = (SOCSettlement)settlements.get(i);
				
				int settlement_coordinate = soc_settlement.getCoordinates();
				
				if(settlement_coordinate == top_coordinate) {
					
					utility = ApplicationConstants.Game.SETTLEMENT_IN_VICINITY_UTILITY;
					
					log.info("SETTLEMENT IN VICINITY");
					
					break;
				} 
					
				
			}
			
			
			
		}

		if(!getPlayer().isPotentialCity(top_coordinate)) {
			
			Vector cities = getPlayer().getCities();
			
			for(int i = 0; i < cities.size(); i++) {
				
				SOCCity soc_city = (SOCCity)cities.get(i);
				
				int cities_coordinate = soc_city.getCoordinates();
				
				if(cities_coordinate == top_coordinate) {
					
					utility = ApplicationConstants.Game.CITY_IN_VICINITY_UTILITY;
					log.info("CITY IN VICINITY");
					break;
				} 
					
				
			}
			
			
			
		}
		
		
		return utility;
	}
	
	private int checkIfOurRoadConnectingThisNode() {
		
		int utility = 0; 
		
		Vector adjEdges = getGame().getBoard().getAdjacentEdgesToNode(getNode());
		
		for(int i=0 ; i < adjEdges.size(); i++) {
			
			// check if we have a road on this edge
			int adjEdge = ((Integer)adjEdges.get(i)).intValue();
			
			if(this.isOurRoad(adjEdge).equals("MY")) {
				utility = ApplicationConstants.Game.OUR_ROAD_TOUCHING_NODE_UTILITY;
				log.info("MY ROAD TOUCHING NODE :: "+getNode());
			}
			else if(this.isOurRoad(adjEdge).equals("OPPONENT"))	{
				utility = ApplicationConstants.Game.OPPONENT_ROAD_TOUCHING_NODE_UTILITY;
				log.info("OPPONENT ROAD TOUCHING NODE :: "+getNode());
			}
		}
		
		log.info("THE UTILITY OF ROAD CONNECTING NODE :: "+getNode()+" :: "+utility);
		
		return utility;
		
		
	}
	
	private Hashtable checkHighUtilityRoadEdge() {
		
		Hashtable edge_utility = new Hashtable();
		
		SOCGame game = getGame();
		
		// for all of the potential edges, get the other node coordinate and get the edges of those node coordinates
		// check if we have a road built 
		
		Vector adjEdges = game.getBoard().getAdjacentEdgesToNode(getNode());
		
		for(int i = 0; i < adjEdges.size(); i++) {
			
			int edge_coordinate = ((Integer)adjEdges.get(i)).intValue();
			
			// check if this edge coordinate is a potential road building location
			
			if(getPlayer().isPotentialRoad(edge_coordinate)) {
				
				// now get the adjacent nodes to this edge
				
				Vector adjNodesToEdgeCoordinate = game.getBoard().getAdjacentNodesToEdge(edge_coordinate);
				
				int selected_node = 0;
				
				for(int j = 0; j < adjNodesToEdgeCoordinate.size(); j++) {
					
					// get the node which is other than the original node
					selected_node = ((Integer)adjNodesToEdgeCoordinate.get(j)).intValue();
					
					if(selected_node != getNode())
						break;
					
				}
				
				// now get the adjacent edges <except for the edge_coordinate> to the selected node and check whether it is 
				// our road or opponents road
				
				Vector adjEdgesToSelectedNode = game.getBoard().getAdjacentEdgesToNode(selected_node);
				
				int utility_of_edge = 0;
				
				for(int k = 0 ; k < adjEdgesToSelectedNode.size(); k++) {
					
					// check if this edge is not the same as edge_coordinate
					
					int edge = ((Integer)adjEdgesToSelectedNode.get(k)).intValue();
					
					if(edge != edge_coordinate) {
						
						// check if we have our road at this edge
						if(this.isOurRoad(edge).equals("MY"))
							utility_of_edge += ApplicationConstants.Game.OUR_ROAD_TOUCHING_NODE_UTILITY_POSITIVE;
						else if(this.isOurRoad(edge).equals("OPPONENT")) 
							utility_of_edge += ApplicationConstants.Game.OPPONENT_ROAD_TOUCHING_NODE_UTILITY_POSITIVE;
						
						
					}
					
				}
				
				
				edge_utility.put(edge_coordinate, utility_of_edge);
				
			} else { // if this edge_coordinate is not potential
				
				edge_utility.put(edge_coordinate, -1000);
				
			}
			
			
		}
		
		
		return edge_utility;
		
	}
	
	private int fetchNumberOfResourcesNeeded(SOCResourceSet needed_resource_set) {
		
		int resources_needed = 0;
		
		SOCResourceSet player_resources = getPlayer().getResources();
				
			for(int i = 1; i <= 5; i++) {
				
				// check for each resource if we need it
				// if we have less than required check how many we need
				
				if(needed_resource_set.getAmount(i) > player_resources.getAmount(i)) 
						resources_needed += (needed_resource_set.getAmount(i) - player_resources.getAmount(i));
						
				
			}
			
		return resources_needed;
		
	}
	
	
}