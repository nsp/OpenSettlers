/**
*	This agent is used to manage the resource cards
*
**/

package soc.robot;

import soc.message.Message;
import soc.util.Loggers;
import soc.message.CardsMessage;
import soc.game.*;
import soc.util.ApplicationConstants;
import java.util.*;

public class CardsAgent extends Agent implements Runnable {
	
	public CardsAgent() {
		
		log = Loggers.CardsAgentLogger;
		
		new Thread(this, "CardsAgent").start();
		
	}
	
	public void mailBox(Message message) {
		
		messages.add(message);
		
	}
	
	
	public void run() {
		
		while(true) {
			
			if(messages.size() > 0) {
				
				for(int i = 0; i < messages.size(); i++) {
					
					Message msg = (Message)messages.get(i);
					
					if(msg.getFrom().equals("InterfaceAgent")) {
						
						if(msg.getMessage() instanceof CardsMessage) {
						
						CardsMessage cmessage = (CardsMessage)msg.getMessage();
						
						if(cmessage.getResponseHeader().equals("DISCARD CARDS")) {
							
							// check which cards we can afford to discard
							discardCards(cmessage);
							
							
						}
						
						} else if(msg.getMessage() instanceof String) {
							
							if(msg.getMessage().equals("PLAY CARD")) {
								
								playCard();
								
							} else if(msg.getMessage().equals("PURCHASE CARD")) {
								
								purchaseCard();
								
							}
						}
					
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
	
	/**
	 *	this method is responsible for playing the card based on the following 
	 *	prioritizing rules
	 *
	 */
	
	public void playCard() {
		
		/* we try to play cards based on some prioritizing rules 
		 1. we check if we can move the robber from the hex which it is on and if we have
		  	some buildings in its vicinity
		 2. we check if we can monopolize a resource 
		 3.	we check if we can do a discovery
		 4.	we check if we can do a road building
		 */
		
		// we get the order in which to play the cards 
		String [] card_play_order = ApplicationConstants.Cards.CARD_PLAY_ORDER;
		
		boolean card_played = false;
		
		// we check one by one which card we have and then play it 
		
		SOCPlayer player = getPlayer();
		
		for(int i = 0; i < card_play_order.length; i++) {
			
			//public static final String [] CARD_PLAY_ORDER = {"KNIGHT", "MONOPOLY", "DISCOVERY", "ROAD BUILDING"};
			
			if(card_play_order[i] == "KNIGHT") {
				
				// we check if we have the knight card to play
				if(player.getDevCards().getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.KNIGHT) > 0) {
					
					log.info("PLAYING KNIGHT CARD");
					
					// now we check the hex which has the robber on it and if we have some buildings around it
					// this could work in this manner 
					// we check if we have the robber on the hex where we have our resource 
					// in either case, we move the robber to a hex where we find maximum opponents or can do maximum damage
					
					// there are two benefits of playing the knight card
					// you do not accumulate too many knight cards
					// you get a smart chance on getting the largest army award
					
					Message message = new Message();
					
					message.setFrom("CardsAgent");
					message.setTo("PlanAgent");
					message.setMessage("PLAY KNIGHT CARD");
					InterfaceAgent.MA.mailBox(message);
					
					card_played = true;
					break;
					
					
				}
				
				
			} else if(card_play_order[i] == "MONOPOLY") {
				
				// check if we have a monopoly card 
				if(player.getDevCards().getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.MONO) > 0) {
					
					Message message = new Message();
					
					message.setFrom("CardsAgent");
					message.setTo("PlanAgent");
					
					CardsMessage cmessage = new CardsMessage();
					
					cmessage.setResponseHeader("PLAY MONOPOLY CARD");
					
					// we check for the resource which needs to be monopolized 
					
					int resource = getMostWantedResource();
					cmessage.setMonopolizedResource(resource);
					
					InterfaceAgent.MA.mailBox(message);
					
					card_played = true;
					break;
					
					
				}
				
				
			} else if(card_play_order[i] == "DISCOVERY") {
				
				// check if we have a discovery card 
				if(player.getDevCards().getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.DISC) > 0) {
				
					// we need to fetch two of the resources of any kind
					// at the moment we take 1 top resource and 1 second to top resource
					
					SOCResourceSet discovery_resource_set = getResourcesForDiscovery();
					
					Message message = new Message();
					
					message.setFrom("CardsAgent");
					message.setTo("PlanAgent");
					
					CardsMessage cmessage = new CardsMessage();
					
					cmessage.setResponseHeader("PLAY DISCOVERY CARD");
					
					// now we set the discovery resource set in the message
					
					cmessage.setDiscoveryResourceSet(discovery_resource_set);
					
					message.setMessage(cmessage);
					
					InterfaceAgent.MA.mailBox(message);
					
					card_played = true;
					break;
									
				}
			} else if(card_play_order[i] == "ROAD BUILDING") {
				
				if(player.getDevCards().getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.ROADS) > 0) {
				
				// we need to get the coordinates where we need to place the 2 
				// roads
				
				// we check the potential edges where we can build the roads
				
				// we get al those nodes where we have our roads 
				Vector road_touching_nodes = getPlayer().getRoadNodes();
				
				// start by looking each node 1 by 1 
				
				int count_two_roads = 0;
				
				Vector edges_to_build_roads = new Vector(2);
				
				// we loop over all the nodes
			
				main: for(int j = 0; j < road_touching_nodes.size(); j++) {
					
					// we get the adjacent edges connecting this node
					
					Vector adjacent_edges_to_nodes = getGame().getBoard().getAdjacentEdgesToNode(((Integer)road_touching_nodes.get(i)).intValue());
					
					// we check each edge to see if it is a potential road edge
					
					for(int k = 0; k < adjacent_edges_to_nodes.size(); k++) {
						
						int adjacent_edge = ((Integer)adjacent_edges_to_nodes.get(k)).intValue();
						
						// check if this edge is a potential edge 
						
						if(getPlayer().isPotentialRoad(adjacent_edge)) {
							
							// increment the count and set the edge 
							count_two_roads++;
							
							edges_to_build_roads.add(new Integer(adjacent_edge));
							
							if(count_two_roads == 2) 
								break main;
							
						}
						
					}
					
				}
				
				// set the message
				
				Message message = new Message();
				
				message.setFrom("CardsAgent");
				message.setTo("PlanAgent");
				
				CardsMessage cmessage = new CardsMessage();
				
				cmessage.setResponseHeader("PLAY ROAD BUILDING CARD");
				
				// now we set the 2 road edges in the message
				
				cmessage.setEdgesToBuildRoads(edges_to_build_roads);
				//cmessage.setDiscoveryResourceSet(discovery_resource_set);
				
				message.setMessage(cmessage);
				
				InterfaceAgent.MA.mailBox(message);
				
				card_played = true;
				break;
				
				}
			}
			
		}
		
		if(!card_played) {
			
			log.info("SORRY NO CARD TO PLAY");
			// if we do not have a card to play, we can proceed with the move
    		Message msg = new Message();
    		
    		msg.setFrom("CardsAgent");
			msg.setTo("PlanAgent");
			msg.setMessage("MAKE MOVE"); // the complex step of the game
			
			InterfaceAgent.MA.mailBox(msg);
			
			
		}
		 
		
		
	}
	
	/**
	 * 
	 * this method is used to fetch two of the most wanted resource 
	 * 1st is the top resource and the second is the second resource 
	 */
	
	private SOCResourceSet getResourcesForDiscovery() {
		
		SOCResourceSet discovery_resource_set = new SOCResourceSet();
		
		// we run the loop twice 
		// first to fetch the top resource 
		// second time to get the second to top resource
		
		Integer [] resources = {SOCResourceConstants.CLAY, SOCResourceConstants.ORE, SOCResourceConstants.SHEEP, SOCResourceConstants.WHEAT, SOCResourceConstants.WOOD};
		
		int max = 0;
	
		String top_selected_resource = "";
		
		for(int i = 0; i < resources.length; i++) {
			
			int share_value = ((Integer)ApplicationConstants.Game.SHARE_VALUES.get(resources[i])).intValue();
			
			if(share_value > max) {
				
				max = share_value;
					
				switch(resources[i].intValue()) {
				
					case SOCResourceConstants.CLAY : 
					
					// if the resource is clay, increment clay 
					// and set the rest to 0
						discovery_resource_set.setAmount(1, SOCResourceConstants.CLAY);
						discovery_resource_set.setAmount(0, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(0, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WOOD);
						
						top_selected_resource = "CLAY";
						
					break;
					
					case SOCResourceConstants.ORE : 
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.CLAY);
						discovery_resource_set.setAmount(1, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(0, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WOOD);
						
						top_selected_resource = "ORE";
						
					break;
					
					case SOCResourceConstants.SHEEP :
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.CLAY);
						discovery_resource_set.setAmount(0, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(1, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WOOD);
						
						top_selected_resource = "SHEEP";
						
					break;
					
					case SOCResourceConstants.WHEAT :
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.CLAY);
						discovery_resource_set.setAmount(0, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(0, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(1, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WOOD);
						
						top_selected_resource = "WHEAT";
						
					break;
					
					case SOCResourceConstants.WOOD :
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.CLAY);
						discovery_resource_set.setAmount(0, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(0, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(1, SOCResourceConstants.WOOD);
					
						top_selected_resource = "WOOD";
						
					break;	
				}
				
				//resource = resources[i].intValue();
			}
		
		}		
		
		// now we check for the second top resource 
		
		//////////////////
		
		max = 0;
		
		for(int i = 0; i < resources.length; i++) {
			
			int share_value = ((Integer)ApplicationConstants.Game.SHARE_VALUES.get(resources[i])).intValue();
			
			if(share_value > max) {
				
				max = share_value;
					
				switch(resources[i].intValue()) {
				
					case SOCResourceConstants.CLAY : 
					
					// if the resource is clay, increment clay 
					// and set the rest to 0
						
						if(!top_selected_resource.equals("CLAY")) 
							discovery_resource_set.setAmount(1, SOCResourceConstants.CLAY);
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(0, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WOOD);
						
					break;
					
					case SOCResourceConstants.ORE : 
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.CLAY);
						if(!top_selected_resource.equals("ORE"))
							discovery_resource_set.setAmount(1, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(0, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WOOD);
						
					break;
					
					case SOCResourceConstants.SHEEP :
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.CLAY);
						discovery_resource_set.setAmount(0, SOCResourceConstants.ORE);
						if(!top_selected_resource.equals("ORE"))
							discovery_resource_set.setAmount(1, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WOOD);
						
					break;
					
					case SOCResourceConstants.WHEAT :
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.CLAY);
						discovery_resource_set.setAmount(0, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(0, SOCResourceConstants.SHEEP);
						if(!top_selected_resource.equals("WHEAT"))
							discovery_resource_set.setAmount(1, SOCResourceConstants.WHEAT);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WOOD);
						
						
					break;
					
					case SOCResourceConstants.WOOD :
						
						discovery_resource_set.setAmount(0, SOCResourceConstants.CLAY);
						discovery_resource_set.setAmount(0, SOCResourceConstants.ORE);
						discovery_resource_set.setAmount(0, SOCResourceConstants.SHEEP);
						discovery_resource_set.setAmount(0, SOCResourceConstants.WHEAT);
						if(!top_selected_resource.equals("WOOD"))
							discovery_resource_set.setAmount(1, SOCResourceConstants.WOOD);
					
					break;	
				}
				
				//resource = resources[i].intValue();
			}
		
		}		
	
		// now we have the resource set with the topmost resource to get and the second top most resource to get
		
		log.info("DISCOVERY PICK :: "+discovery_resource_set);
		
		
		return discovery_resource_set;
		
	}
	
	/**
	 * 
	 * this method is used to get the most wanted resource 
	 * based on the trade share values 
	 */
	private int getMostWantedResource() {
		
		// we loop over the share values to get the most valuable resource
		// based on the trade history
		
		int resource = 0; // will store the resource we require
		
		Integer [] resources = {SOCResourceConstants.CLAY, SOCResourceConstants.ORE, SOCResourceConstants.SHEEP, SOCResourceConstants.WHEAT, SOCResourceConstants.WOOD};
		
		int max = 0;
		
		for(int i = 0; i < resources.length; i++) {
			
			int share_value = ((Integer)ApplicationConstants.Game.SHARE_VALUES.get(resources[i])).intValue();
			
			if(share_value > max) {
				
				max = share_value;
				
				resource = resources[i].intValue();
				
			}
		
		}
		
		log.info("RESOURCE TO MONOPOLIZE :: "+resource);
		
		return resource;
		
	}
	
	
	/**
	 * 
	 * this is the method which is responsible for purchasing a card 
	 * it checks if we have the resources to purchase a card and then
	 * purchases
	 *
	 */
	
	public void purchaseCard() {
		
		SOCResourceSet player_resources = getPlayer().getResources();
		
		Hashtable dev_card_resources_needed = (Hashtable)ApplicationConstants.ResourcesRequired.RequiredResourcesTable.get("CARD");
		
		int clay_needed = ((Integer)dev_card_resources_needed.get("CLAY")).intValue();
		int ore_needed = ((Integer)dev_card_resources_needed.get("ORE")).intValue();
		int sheep_needed = ((Integer)dev_card_resources_needed.get("SHEEP")).intValue();
		int wheat_needed = ((Integer)dev_card_resources_needed.get("WHEAT")).intValue();
		int wood_needed = ((Integer)dev_card_resources_needed.get("WOOD")).intValue();
		
		SOCResourceSet resources_needed_for_dev_card = new SOCResourceSet(clay_needed, ore_needed, sheep_needed, wheat_needed, wood_needed, 0);
			
		// check if the player has the resources to buy a dev card
		// 
		Message message = new Message();
		message.setFrom("CardsAgent");
		message.setTo("PlanAgent");
		
		if(player_resources.contains(resources_needed_for_dev_card)) {
			
			message.setMessage("PURCHASE CARD");
			
		} else {
			
			message.setMessage("CANNOT PURCHASE CARD");
			
		} 
		
		InterfaceAgent.MA.mailBox(message);
		
	}
	
	public void discardCards(CardsMessage cmessage) {
		
		// this method contains the logic for discarding the cards that we do not need
		
		// two ways here as well 
		// an avg approach is to discard the cards in order clay, ore, sheep, wheat, wood until we get the target
		// a good approach is to keep the valuable resource cards with us depending on some informed strategy
		
		int discardcards = cmessage.getdiscardcards();
		
		log.info("NUMBER OF CARDS TO DISCARD :: "+discardcards);
		
		// at the moment we would be using the initial approach
		
		SOCResourceSet igive_resource_set = new SOCResourceSet(0,0,0,0,0,0);
		
		SOCResourceSet player_resources = getPlayer().getResources();
		
		// in loop, check how much of each resource we have 
		// subtract it from total, when the remaining is 0
		
		int payout = 0;
		
		int [] resources_to_give = {0,0,0,0,0};
		
		for(int i = 1; i <= 5; i++) {
			
			if(player_resources.getAmount(i) > 0) {
				
				resources_to_give[i - 1]++;
				payout++;
			}
			
			if(payout == discardcards)
				break;
			else if(i == 5) 
				i = 1;
		
		}
		
		for(int i = 1; i <= 5; i++) 
			igive_resource_set.setAmount(resources_to_give[i - 1], i);
			
		// send the message to the PlanAgent which would instruct the InterfaceAgent to discard the Resources
		
		Message message = new Message();
		
		message.setFrom("CardsAgent");
		
		message.setTo("PlanAgent");
		
		CardsMessage cmsg = new CardsMessage();
		
		cmsg.setResponseHeader("CARDS TO DISCARD");
		
		cmsg.setDiscardResourceSet(igive_resource_set);
		
		message.setMessage(cmsg);
		
		InterfaceAgent.MA.mailBox(message); // this message has the cards to discard
		
		
	}
	
	
}

