/**
*	This class stores the configuration parameters for the application
*
*	@author Haseeb Saleem
*
**/

package soc.util;

import java.util.*;

public class ApplicationConstants {
	
	private static Hashtable DiceProbabilitiesHash;
	private static Hashtable ResourceRequirementHash;
	
	static {
		
		System.out.println("STATIC BLOCK");
		
		DiceProbabilitiesHash = new Hashtable();
		
		DiceProbabilitiesHash.put(2, .03);
		DiceProbabilitiesHash.put(12, .03);
		DiceProbabilitiesHash.put(3, .06);
		DiceProbabilitiesHash.put(11, .06);
		DiceProbabilitiesHash.put(4, .08);	
		DiceProbabilitiesHash.put(10, .08);
		DiceProbabilitiesHash.put(5, .11);
		DiceProbabilitiesHash.put(9, .11);
		DiceProbabilitiesHash.put(6, .14);
		DiceProbabilitiesHash.put(8, .14);
		DiceProbabilitiesHash.put(7, .17);
		
		ResourceRequirementHash = new Hashtable();
		
		Hashtable TempHash = new Hashtable();
		
		TempHash.put("CLAY", 1);
		TempHash.put("ORE", 0);
		TempHash.put("SHEEP", 1);
		TempHash.put("WHEAT", 1);
		TempHash.put("WOOD", 1);
		
		ResourceRequirementHash.put("SETTLEMENT", TempHash);
		
		TempHash = new Hashtable();
		
		TempHash.put("CLAY", 0);
		TempHash.put("ORE", 3);
		TempHash.put("SHEEP", 0);
		TempHash.put("WHEAT", 2);
		TempHash.put("WOOD", 0);
		
		ResourceRequirementHash.put("CITY", TempHash);
		
		TempHash = new Hashtable();
		
		TempHash.put("CLAY", 1);
		TempHash.put("ORE", 0);
		TempHash.put("SHEEP", 0);
		TempHash.put("WHEAT", 0);
		TempHash.put("WOOD", 1);
		
		ResourceRequirementHash.put("ROAD", TempHash);
		
		TempHash = new Hashtable();
		
		TempHash.put("CLAY", 0);
		TempHash.put("ORE", 1);
		TempHash.put("SHEEP", 1);
		TempHash.put("WHEAT", 1);
		TempHash.put("WOOD", 0);
		
		ResourceRequirementHash.put("CARD", TempHash);
		
	}
	
	public static class DiceProbabilities {
		
		public static final Hashtable DiceProbabilityTable = DiceProbabilitiesHash;
		
	}
	
	public static class ResourcesRequired {
		
		public static final Hashtable RequiredResourcesTable = ResourceRequirementHash;
		
	}
	
	public static class Game {
		
		public static int ROBBER_FREQUENCY; // incremented when a 7 is rolled
		public static int TOTAL_TURNS; // incremented on each turn
		
		public static final int MAX_TURNS_THREASHOLD = 500;
		public static final int PORT_RESOURCE_REQUIRED = 2;
		public static final int MISC_PORT_RESOURCE_REQUIRED = 3;
		public static final int RESOURCES_REQUIRED_BY_BANK = 4;
		
		public static final int ALL_DIFFERENT_HEX_TYPES_AROUND = 30; // UTILITY DISCOUNT IF ALL HEXES ARE OF DIIFERENT TYPES AROUND THIS COORDINATE
		public static final int TWO_DIFFERENT_HEX_TYPES_AROUND = 10; // UTILITY DISCOUNT IF ATLEAST TWO HEXES ARE OF DIIFERENT TYPES AROUND THIS COORDINATE
			
		public static final int DESERT_HEX_PENALITY = -5;
		public static final int WATER_HEX_PENALITY = -3;
		public static final int TWO_ONE_PORT_UTILITY = 10;
		public static final int THREE_ONE_PORT_UTILITY = 5;
		
		public static final int NUMBERS_HEX_PROVISION_WEIGHT = 10;
		
		public static final int TOTAL_NODE_AGENTS = 54;
		
		public static final int SETTLEMENT_IN_VICINITY_UTILITY = -1;
		public static final int CITY_IN_VICINITY_UTILITY = -2;
		
		public static final int OUR_ROAD_TOUCHING_NODE_UTILITY = -1;
		public static final int OPPONENT_ROAD_TOUCHING_NODE_UTILITY = -2;
		
		public static final int OUR_ROAD_TOUCHING_NODE_UTILITY_POSITIVE = 1;
		public static final int OPPONENT_ROAD_TOUCHING_NODE_UTILITY_POSITIVE = 2;
		
		public static final int HYPOTHETICAL_VALUE_FOR_UTILITY = 1000;
		public static final int HYPOTHETICAL_VALUE_FOR_ROAD_TURNS = 100;
		
		public static final int TRADER_LOOP_2_3_1 = 1; // in this we can check if we have a 2:1 port or a 3:1 port
		
		// a structure which would store the share values of resources
		
		public static Hashtable SHARE_VALUES = new Hashtable();
		
		// clay, ore, sheep, wheat, wood
		
		public static final int SHARE_VALUE_CLAY_UP = 10;
		public static final int SHARE_VALUE_ORE_UP = 10;
		public static final int SHARE_VALUE_SHEEP_UP = 10;
		public static final int SHARE_VALUE_WHEAT_UP = 10;
		public static final int SHARE_VALUE_WOOD_UP = 10;
		
		public static final int SHARE_VALUE_CLAY_DOWN = -10;
		public static final int SHARE_VALUE_ORE_DOWN = -10;
		public static final int SHARE_VALUE_SHEEP_DOWN = -10;
		public static final int SHARE_VALUE_WHEAT_DOWN = -10;
		public static final int SHARE_VALUE_WOOD_DOWN = -10;
		
		
		
	}
	
	public static class Robber {
		
		public static final int OUR_SETTLEMENT_PENALTY = -10;
		public static final int OUR_CITY_PENALTY = -20;
		public static final int OUR_ROAD_PENALTY = -10;
		
		public static final int OPPONENT_SETTLEMENT_ADVANTAGE = 10;
		public static final int OPPONENT_CITY_ADVANTAGE = 20;
		public static final int OPPONENT_ROAD_ADVANTAGE = 10;
			
		public static final int OUR_PLAYER_PENALTY = -10; // IF OUR PLAYER ON THE HEX
	
		public static final int OPPONENT_PLAYER_TOP_ADVANTAGE = 20; // IF THE OPPONENT PLAYER IS ON TOP AT THE MOMENT 
		public static final int OPPONENT_PLAYER_ADVANTAGE = 10; // OPPONENT PLAYER ADVANTAGE
		
	}
	
	public static class Cards {
		
		public static final String [] CARD_PLAY_ORDER = {"KNIGHT", "MONOPOLY", "DISCOVERY", "ROAD BUILDING"};
		
	}
	
	public static class Log {
			
		public static final String STANDARD_LOG = "C:\\output\\output.txt";
			
	}
	
	 
	
	
	
}