/**
*	This class holds the message Data to send to the Plan agent from the NodeAgent
*
**/

package soc.message;

import java.util.*;

public class NodeMessage {
	
	private Hashtable proposedAction; 
	private String response;
	private double utility;

	private double road_utility;
	
	private int road_edge_coordinate;
	
	private boolean settlement = false;
	private boolean city = false;
	private boolean road = false;
	
	public NodeMessage() {
		
		proposedAction = new Hashtable();
		
	}
	
	public void setResponseHeader(String response) {
		
		this.response = response;
		
	}
	
	public String getResponseHeader() {
		
		return this.response;
		
	}
	
	
	
	public void setUtility(double utility) {
		
		this.utility = utility;
	}
	
	public double getUtility() {
		
		return this.utility;
		
	}
	
	public void setSettlement(boolean settlement) {
		
		this.settlement = settlement;
		
	}

	public void setCity(boolean city) {
		
		this.city = city;
		
	}
	
	public void setRoad(boolean road) {
		
		this.road = road;
		
	}
	
	public boolean getSettlement() {
		
		return this.settlement;
		
	}

	public boolean getCity() {
		
		return this.city;
		
	}
	
	public boolean getRoad() {
		
		return this.road;
		
	}
	
	public void setRoadUtility(double roadUtility) {
		
		this.road_utility = roadUtility;
		
	}
	
	public double getRoadUtility() {
		
		return this.road_utility;
		
	}
	
	public void setRoadCoordinate(int roadCoordinate) {
		
		this.road_edge_coordinate = roadCoordinate;
		
	}
	
	public int getRoadCoordinate() {
		
		return this.road_edge_coordinate;
		
	}
	

	public void setProposedAction(int coordinates, String piece) {
		
		proposedAction.put(coordinates, piece);
		
	}
	
	public Hashtable getProposedAction() {
		
		return proposedAction;
		
	}
	
} 