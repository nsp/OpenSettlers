/**
*	This class holds the message Data to send back and forth
*
**/

package soc.message;

import soc.game.*;
import java.util.*;

public class CardsMessage {
	
	private String response;
	private int discardcards;
	private ResourceSet igive_resource_set;
	private int resource;
	private ResourceSet discovery_resource_set;
	private Vector edges_to_build_roads;
	
	public void setResponseHeader(String response) {
		
		this.response = response;
		
	}
	
	public String getResponseHeader() {
		
		return this.response;
	}
	
	public void setDiscardCards(int discardcards) {
		
		this.discardcards = discardcards;
	}
	
	public int getdiscardcards() {
		
		return this.discardcards;
		
	}
	
	public void setDiscardResourceSet(ResourceSet igive_resource_set) {
		
		this.igive_resource_set = igive_resource_set;
		
	}
	
	public ResourceSet getDiscardResourceSet() {
		
		return this.igive_resource_set;
		
	}
	
	public void setMonopolizedResource(int resource) {
		
		this.resource = resource;
	}
	
	public int getMonopolizedResource() {
		
		return this.resource;
	}
	
	public void setDiscoveryResourceSet(ResourceSet discovery_resource_set) {
		
		this.discovery_resource_set = discovery_resource_set;
	}
	
	public ResourceSet getDiscoveryResourceSet() {
		
		return this.discovery_resource_set;
	}
	 
	public void setEdgesToBuildRoads(Vector edges_to_build_roads) {
		
		this.edges_to_build_roads = edges_to_build_roads;
		
	}
	
	public Vector getEdgesToBuildRoads() {
		
		return this.edges_to_build_roads;
		
	}
	
}