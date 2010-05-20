/**
*	This is the trader agent responsible for handling the trade
*
**/

package soc.robot;

import soc.message.Message;
import soc.message.TraderMessage;
import soc.util.Loggers;
import soc.util.ApplicationConstants;
import java.util.Vector;

import soc.game.*;

public class TraderAgent extends Agent implements Runnable {
	
	
	private SOCResourceSet needed_resource_set;
	private SOCResourceSet lagging_resource_set;
	private String building_piece;
	private Vector plan;
	//private Thread TraderAgentThread;
	
	
	public TraderAgent() {
		
		
		log = Loggers.TraderAgentLogger;
		
		new Thread(this).start();
		
	}
	
	public void mailBox(Message message) {
		
		messages.add(message);
		
	}
	
	public void setNeededResourceSet(SOCResourceSet needed_resource_set) {
		
		this.needed_resource_set = needed_resource_set;
		
	}
	
	public SOCResourceSet getNeededResourceSet() {
		
		return this.needed_resource_set;
		
	}
	
	public void setLaggingResourceSet(SOCResourceSet lagging_resource_set) {
		
		this.lagging_resource_set = lagging_resource_set;
		
	}
	
	public SOCResourceSet getLaggingResourceSet() {
		
		return this.lagging_resource_set;
		
	}
	
	
	public void setBuildingPiece(String building_piece) {
		
		this.building_piece = building_piece;
	}
	
	public String getBuildingPiece() {
		
		return this.building_piece;
		
	}
	
	public void setPlan(Vector plan) {
		
		this.plan = plan;
		
	}
	
	public Vector getPlan() {
		
		return this.plan;
		
	}
	
	public void run() {
		
		while(true) {
			
			if(messages.size() > 0) {
				
				for(int i = 0; i < messages.size(); i++) {
					
					Message message = (Message)messages.get(i);
					
					if(message.getFrom().equals("PlanAgent")) {
						
						makeOptimalTradePlan(message);
						
					} else if(message.getFrom().equals("InterfaceAgent"))
						
						makeOptimalTradePlan(message); // this message comes when a trade was initiated and was not accepted
					
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
	
		private void makeOptimalTradePlan(Message message) {
			
			/**
			 * check what we need and send a trade plan back to plan agent
			 * the trade strategy would be build incrementally
			 * first up we would try
			 * 1. trade with 1 resource for the resources required
			 * 2. trade with the 2:1 port if we have 1 
			 * 3. trade with a 4:1 port if we have a resource in too much excess
			 *  
			 * 
			 **/
			
			// just for testing purpose do a thing try to get the resources we need 
			// for a 1 resource that we do not need to build this thing
		
			TraderMessage tmessage = (TraderMessage)message.getMessage();
			
			log.info("PLAN IN TRADER AGENT IS :: "+tmessage.getPlan());
			
			SOCResourceSet player_resources = getPlayer().getResources();
			
			// the traderloop controls which strategy to opt and which resource set to use
			
			// if traderloop is 0, then we get the values from the message itself
			// if it is greater, we get it from the local copy
			
			int traderloop = tmessage.getTradeLoop();
			
			if(traderloop == 0) {
				
				this.setBuildingPiece(tmessage.getBuildingPiece());
				this.setLaggingResourceSet(tmessage.getLaggingResourceSet());
				this.setNeededResourceSet(tmessage.getNeededResourceSet());
				this.setPlan(tmessage.getPlan());
				
			}
			
			log.info("LAGGING RESOURCE SET :: "+this.getLaggingResourceSet());
			log.info("NEEDED RESOURCE SET :: "+this.getNeededResourceSet());
			log.info("PLAYER RESOURCE SET :: "+player_resources);
			
			log.info("TRADER LOOP IS :: "+traderloop);
			
			String piece_to_build = traderloop == 0 ? tmessage.getBuildingPiece() : this.getBuildingPiece();
			
			//String piece_to_build = tmessage.getBuildingPiece();
			SOCResourceSet needed_resource_set = traderloop == 0 ? tmessage.getNeededResourceSet() : this.getNeededResourceSet();
			
			//SOCResourceSet needed_resource_set = tmessage.getNeededResourceSet();
			
			SOCResourceSet lagging_resource_set = traderloop == 0 ? tmessage.getLaggingResourceSet() : this.getLaggingResourceSet();
			
			//SOCResourceSet lagging_resource_set = tmessage.getLaggingResourceSet();
			Vector plan = traderloop == 0 ? tmessage.getPlan() : this.getPlan();
			
			
			// The trader knows which piece is needed to be built
			// which resources we need to build the piece
			// and how many of the resources we are lagging
			
			// check for a piece which is 0 in the needed list  
			
			SOCResourceSet give_resource_set = new SOCResourceSet(0,0,0,0,0,0);
			
			boolean something_to_give = false;
			
			for(int i = 1; i <= 5; i++ ) {
				
				if(needed_resource_set.getAmount(i) == 0) {
					
					if(getPlayer().getResources().getAmount(i) > 0) { 
						give_resource_set.setAmount(1, i);
						something_to_give = true;
						break;
					}
				}
					
			}
			
			// an additional check from within the needed resource set is 
			// is to get only that resource which we need and do not already have
			// we would try for the first resource that we want and do not have
			
			// check in player resources, if we already do not have the resource, 
			// provide it for exchange
			
			SOCResourceSet need_resource_set = new SOCResourceSet(0,0,0,0,0,0);
			
			for(int i = 1; i <= 5; i++) {
				
				if(player_resources.getAmount(i) < needed_resource_set.getAmount(i)) {
					
					need_resource_set.setAmount(1, i);
					break;
				}
				
			}
			
			
			if(something_to_give && traderloop == 0) {
				
				log.info("WE CAN GIVE SOMETHING IN TRADE");
				
				Message msg = new Message();
				
				msg.setFrom("TraderAgent");
				msg.setTo("PlanAgent");
				
				TraderMessage tmsg = new TraderMessage();
				
				tmsg.setResponseHeader("TRADE OFFER");
				
				tmsg.setGiveResourceSet(give_resource_set);
				
				tmsg.setGetResourceSet(need_resource_set); // slightly modified
				
				tmsg.setPlan(plan);
				
				msg.setMessage(tmsg);
				
				InterfaceAgent.MA.mailBox(msg); // send a message to make a trade to Plan agent
				
				//InterfaceAgent.PA.releaseAction();
				
			} else if(traderloop == ApplicationConstants.Game.TRADER_LOOP_2_3_1) {
				
				log.info("TRYING FOR A 2:1 3:1 TRADE");
				
				// check if we can make a 2:1 trade or a 3:1 trade to get closer to what we need
				
				// first up check for the 2:1 trade
				// check for the resource we need the most 
				// this means we need to check which resource has a greater difference 
				// between the needed resource and lagging resource
				// this means that we need to purchase this resource
				
				// check for the resource that we have the most 
				// it means that we can give this resource
				
				int max = 0;
				
				int badly_needed_resource = 0;
				
				for(int i = 1; i <= 5; i++) {
					
					log.info("LAGGING RESOURCE SET FOR i = "+i+" IS :: "+lagging_resource_set.getAmount(i));
					log.info("NEEDED RESOURCE SET FOR i = "+i+" IS :: "+needed_resource_set.getAmount(i));
					
					if(lagging_resource_set.getAmount(i) > 0 && (needed_resource_set.getAmount(i) - lagging_resource_set.getAmount(i) >= max)) {
						
						max = needed_resource_set.getAmount(i) - lagging_resource_set.getAmount(i);
						
						badly_needed_resource = i;
						
					}
				}
				
				SOCResourceSet badly_needed_resource_set = new SOCResourceSet(0,0,0,0,0,0);
				
				for(int i = 1; i <= 5; i++) {
					
					// check which is the badly needed resource and set it
					
					if(i == badly_needed_resource) {
						
						badly_needed_resource_set.setAmount(1, i);
						break;
					}
						
				}
				
				// now check which resource we could be able to give
				
				SOCResourceSet igive_resource_set = new SOCResourceSet(0,0,0,0,0,0);
				
				//int igive_resource = 0;
				
				boolean two_one_trade = false;
				
				for(int i = 1; i <= 5; i++) {
					
					log.info("PLAYER RESOURCE :: "+i+" AVAILABLE IS :: "+player_resources.getAmount(i));
					log.info("NEEDED RESOURCE :: "+i+" AVAILABLE IS :: "+needed_resource_set.getAmount(i));
					
					if(player_resources.getAmount(i) - needed_resource_set.getAmount(i) >= ApplicationConstants.Game.PORT_RESOURCE_REQUIRED) {
						
						if(getPlayer().getPortFlag(i)) { // adding 7 makes it the hex equivalent port
							
							log.info("WE HAVE A 2:1 PORT");
							
							//igive_resource = i;
							igive_resource_set.setAmount(ApplicationConstants.Game.PORT_RESOURCE_REQUIRED, i);
							two_one_trade = true;
							break;
							
						}
						
					}
				}
				
				boolean three_one_trade = false;
				
				if(!two_one_trade) { // check for 3:1 trade
					
					for(int i = 1; i <= 5; i++) {
						
						log.info("PLAYER RESOURCE :: "+i+" AVAILABLE IS :: "+player_resources.getAmount(i));
						log.info("NEEDED RESOURCE :: "+i+" AVAILABLE IS :: "+needed_resource_set.getAmount(i));
						
						if(player_resources.getAmount(i) - needed_resource_set.getAmount(i) >= ApplicationConstants.Game.MISC_PORT_RESOURCE_REQUIRED) {
							
							if(getPlayer().getPortFlag(SOCBoard.MISC_PORT)) {
								
								log.info("WE HAVE A 3:1 PORT");
								
								igive_resource_set.setAmount(ApplicationConstants.Game.MISC_PORT_RESOURCE_REQUIRED, i);
								three_one_trade = true;
								break;
								
							}
							
						}
					}
					
				}
				
				if(two_one_trade) {
					
					log.info("TWO ONE TRADE CAN BE DONE");
					
					log.info("I GIVE RESOURCE SET :: "+igive_resource_set);
					log.info("I GET RESOURCE SET :: "+badly_needed_resource_set);
					
					Message msg = new Message();
					
					msg.setFrom("TraderAgent");
					msg.setTo("PlanAgent");
					
					TraderMessage tmsg = new TraderMessage();
					
					tmsg.setResponseHeader("TWO/THREE ONE TRADE OFFER");
					
					tmsg.setGiveResourceSet(igive_resource_set);
					
					tmsg.setGetResourceSet(badly_needed_resource_set);
					
					tmsg.setPlan(plan);
					
					msg.setMessage(tmsg);
					
					InterfaceAgent.MA.mailBox(msg); // send a message to make a trade to Plan agent

					
				} else if(three_one_trade) {
					
					log.info("THREE ONE TRADE CAN BE DONE");
					
					Message msg = new Message();
					
					msg.setFrom("TraderAgent");
					msg.setTo("PlanAgent");
					
					TraderMessage tmsg = new TraderMessage();
					
					tmsg.setResponseHeader("TWO/THREE ONE TRADE OFFER");
					
					tmsg.setGiveResourceSet(igive_resource_set);
					
					tmsg.setGetResourceSet(badly_needed_resource_set);
					
					tmsg.setPlan(plan);
					
					msg.setMessage(tmsg);
					
					InterfaceAgent.MA.mailBox(msg); // send a message to make a trade to Plan agent
					
					
				} else if(!two_one_trade && !three_one_trade) {
					
					log.info("TWO ONE / THREE ONE TRADE CANNOT BE DONE");
					
					Message msg = new Message();
					
					msg.setFrom("TraderAgent");
					msg.setTo("PlanAgent");
					
					TraderMessage tmsg = new TraderMessage();
					
					tmsg.setResponseHeader("CANNOT OFFER TRADE");
					
					//tmsg.setGiveResourceSet(igive_resource_set);
					
					//tmsg.setGetResourceSet(badly_needed_resource_set);
					
					//tmsg.setPlan(tmessage.getPlan());
					
					msg.setMessage(tmsg);
					
					InterfaceAgent.MA.mailBox(msg); // send a message to make a trade to Plan agent
					
					
				}
				
				
			} 
			
			
			else if((!something_to_give) || traderloop > ApplicationConstants.Game.TRADER_LOOP_2_3_1){
				
				log.info("WE CANNOT GIVE ANYTHING IN TRADE :(");
				
				Message msg = new Message();
				
				msg.setFrom("TraderAgent");
				msg.setTo("PlanAgent");
				
				TraderMessage tmsg = new TraderMessage();
				
				tmsg.setResponseHeader("CANNOT OFFER TRADE");
				
				msg.setMessage(tmsg);
				
				InterfaceAgent.MA.mailBox(msg);
				
			}
			
			
			
			
			
			
		}
	
	
}