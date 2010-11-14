/**
*	This class represents a Trader Message
*	
**/

package soc.message;

import soc.game.*;
import java.util.*;

public class TraderMessage {
	
	private ResourceSet needed_resource_set;
	private ResourceSet lagging_resource_set;
	private String piece;
	private Vector plan;
	
	private ResourceSet get_resource_set;
	private ResourceSet give_resource_set;
	
	private String response;
	
	private int tradeloop;
	
	public void setNeededResourceSet(ResourceSet needed_resource_set) {
		
		this.needed_resource_set = needed_resource_set;
		
	}
	
	public ResourceSet getNeededResourceSet() {
		
		return this.needed_resource_set;
		
	}
	
	public void setTradeLoop(int tradeloop) {
		
		this.tradeloop = tradeloop;
		
	}
	
	public int getTradeLoop() {
		
		return this.tradeloop;
		
	}
	
	
	public void setLaggingResourceSet(ResourceSet lagging_resource_set) {
		
		this.lagging_resource_set = lagging_resource_set;
		
	}
	
	public ResourceSet getLaggingResourceSet() {
		
		return this.lagging_resource_set;
		
	}

	
	public void setBuildingPiece(String piece) {
		
		this.piece = piece;
	}
	
	public String getBuildingPiece() {
		
		return this.piece;
		
	}
	
	public void setGetResourceSet(ResourceSet get_resource_set) {
		
		this.get_resource_set = get_resource_set;
		
	} 
	
	public ResourceSet getGetResourceSet() {
		
		return this.get_resource_set;
		
	}

	public void setPlan(Vector plan) {
		
		this.plan = plan;
		
	}
	
	public Vector getPlan() {
		
		return this.plan;
		
	}
	
	public void setGiveResourceSet(ResourceSet give_resource_set) {
		
		this.give_resource_set = give_resource_set;
		
	} 
	
	public ResourceSet getGiveResourceSet() {
		
		return this.give_resource_set;
		
	}
	
	public void setResponseHeader(String response) {
		
		this.response = response;
	}
	
	public String getResponseHeader() {
		
		return this.response;
		
	}
	
}