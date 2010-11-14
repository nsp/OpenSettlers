/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * This file copyright (C) 2003-2004  Robert S. Thomas
 * Portions of this file copyright (C) 2009 Jeremy D Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package soc.robot;

import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import soc.disableDebug.D;
import soc.game.Board;
import soc.game.City;
import soc.game.DevCardConstants;
import soc.game.DevCardSet;
import soc.game.Game;
import soc.game.LRPathData;
import soc.game.Player;
import soc.game.PlayingPiece;
import soc.game.ResourceSet;
import soc.game.Road;
import soc.game.Settlement;
import soc.util.CutoffExceededException;
import soc.util.NodeLenVis;
import soc.util.Pair;
import soc.util.Queue;
import soc.util.RobotParameters;

/**
 * Moved the routines that pick what to build
 * next out of RobotBrain.  Didn't want
 * to call this SOCRobotPlanner because it
 * doesn't really plan, but you could think
 * of it that way.  DM = Decision Maker
 *
 * @author Robert S. Thomas
 */

public class RobotDM {

  protected static final DecimalFormat df1 = new DecimalFormat("###0.00");

  protected int maxGameLength = 300;
	
  protected int maxETA = 99;

  protected float etaBonusFactor = (float)0.8;
	
  protected float adversarialFactor = (float)1.5;

  protected float leaderAdversarialFactor = (float)3.0;

  protected float devCardMultiplier = (float)2.0;

  protected float threatMultiplier = (float)1.1;

  protected static final int LA_CHOICE = 0;
  protected static final int LR_CHOICE = 1;
  protected static final int CITY_CHOICE = 2;
  protected static final int SETTLEMENT_CHOICE = 3;

  /**
   * used in planStuff
   */
  protected static final int TWO_SETTLEMENTS = 11;
  protected static final int TWO_CITIES = 12;
  protected static final int ONE_OF_EACH = 13;
  protected static final int WIN_LA = 14;
  protected static final int WIN_LR = 15;

  /**
   * used for describing strategies
   */
  public static final int SMART_STRATEGY = 0;
  public static final int FAST_STRATEGY = 1;

  protected RobotBrain brain;
  protected HashMap playerTrackers;
  protected PlayerTracker ourPlayerTracker;
  protected Player ourPlayerData;
  protected Stack buildingPlan;
  protected Game game;
  protected Vector threatenedRoads;
  protected Vector goodRoads;
  protected PossibleRoad favoriteRoad;
  protected Vector threatenedSettlements;
  protected Vector goodSettlements;
  protected PossibleSettlement favoriteSettlement;    
  protected PossibleCity favoriteCity;
  protected PossibleCard possibleCard;


  /**
   * constructor
   *
   * @param br  the robot brain
   */
  public RobotDM(RobotBrain br) {
    brain = br;
    playerTrackers = brain.getPlayerTrackers();
    ourPlayerTracker = brain.getOurPlayerTracker();
    ourPlayerData = brain.getOurPlayerData();
    buildingPlan = brain.getBuildingPlan();
    game = brain.getGame();

    threatenedRoads = new Vector();
    goodRoads = new Vector();
    threatenedSettlements = new Vector();
    goodSettlements = new Vector();
    RobotParameters params = brain.getRobotParameters();
    maxGameLength = params.getMaxGameLength();
    maxETA = params.getMaxETA();
    etaBonusFactor = params.getETABonusFactor();
    adversarialFactor = params.getAdversarialFactor();
    leaderAdversarialFactor = params.getLeaderAdversarialFactor();
    devCardMultiplier = params.getDevCardMultiplier();
    threatMultiplier = params.getThreatMultiplier();
  }
	
	
  /**
   * constructor
   * 
   * this is if you don't want to use a brain
   *
   * @param params  the robot parameters
   * @param pt   the player trackers
   * @param opt  our player tracker
   * @param opd  our player data
   * @param bp   our building plan
   */
  public RobotDM(RobotParameters params,
		    HashMap pt,
		    PlayerTracker opt,
		    Player opd,
		    Stack bp) {
    brain = null;
    playerTrackers = pt;
    ourPlayerTracker = opt;
    ourPlayerData = opd;
    buildingPlan = bp;
    game = ourPlayerData.getGame();

    maxGameLength = params.getMaxGameLength();
    maxETA = params.getMaxETA();
    etaBonusFactor = params.getETABonusFactor();
    adversarialFactor = params.getAdversarialFactor();
    leaderAdversarialFactor = params.getLeaderAdversarialFactor();
    devCardMultiplier = params.getDevCardMultiplier();
    threatMultiplier = params.getThreatMultiplier();

    threatenedRoads = new Vector();
    goodRoads = new Vector();
    threatenedSettlements = new Vector();
    goodSettlements = new Vector();
  }


  /**
   * @return favorite settlement
   */
  public PossibleSettlement getFavoriteSettlement() {
    return favoriteSettlement;
  }
	
  /**
   * @return favorite city
   */
  public PossibleCity getFavoriteCity() {
    return favoriteCity;
  }

  /**
   * @return favorite road
   */
  public PossibleRoad getFavoriteRoad() {
    return favoriteRoad;
  }

  /**
   * @return possible card
   */
  public PossibleCard getPossibleCard() {
    return possibleCard;
  }

  /**
   * make some building plans.
   * Calls either {@link #smartGameStrategy(int[])} or {@link #dumbFastGameStrategy(int[])}.
   * Both of these will check whether this is our normal turn, or if
   * it's the 6-player board's {@link Game#SPECIAL_BUILDING Special Building Phase}.
   *
   * @param strategy  an integer that determines which strategy is used (SMART_STRATEGY | FAST_STRATEGY)
   */
  public void planStuff(int strategy) {
      //long startTime = System.currentTimeMillis();
    D.ebugPrintln("PLANSTUFF");
	  
    BuildingSpeedEstimate currentBSE = new BuildingSpeedEstimate(ourPlayerData.getNumbers());
    int currentBuildingETAs[] = currentBSE.getEstimatesFromNowFast(ourPlayerData.getResources(), ourPlayerData.getPortFlags());

    threatenedSettlements.removeAllElements();
    goodSettlements.removeAllElements();
    threatenedRoads.removeAllElements();
    goodRoads.removeAllElements();

    favoriteRoad = null;
    favoriteSettlement = null;    
    favoriteCity = null;

    //PlayerTracker.playerTrackersDebug(playerTrackers);

    ///
    /// update ETAs for LR, LA, and WIN
    ///
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      // clear the table
      brain.getDRecorder().eraseAllRecords();
      // record our current resources
      brain.getDRecorder().startRecording(RobotClient.CURRENT_RESOURCES);
      brain.getDRecorder().record(ourPlayerData.getResources().toShortString());
      brain.getDRecorder().stopRecording();
      // start recording the current players' plans
      brain.getDRecorder().startRecording(RobotClient.CURRENT_PLANS);
    } 

    if (strategy == SMART_STRATEGY) {
      PlayerTracker.updateWinGameETAs(playerTrackers);
    }
    
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      // stop recording
      brain.getDRecorder().stopRecording();
    } 

    int leadersCurrentWGETA = ourPlayerTracker.getWinGameETA();
    Iterator trackersIter = playerTrackers.values().iterator();
    while (trackersIter.hasNext()) {
      PlayerTracker tracker = (PlayerTracker)trackersIter.next();
      int wgeta = tracker.getWinGameETA();
      if (wgeta < leadersCurrentWGETA) {
	leadersCurrentWGETA = wgeta;
      }
    }

    //PlayerTracker.playerTrackersDebug(playerTrackers);

    ///
    /// reset scores and biggest threats for everything
    ///
    Iterator posPiecesIter;
    PossiblePiece posPiece;
    posPiecesIter = ourPlayerTracker.getPossibleCities().values().iterator();
    while (posPiecesIter.hasNext()) {
      posPiece = (PossiblePiece)posPiecesIter.next();
      posPiece.resetScore();
      posPiece.clearBiggestThreats();
    }
    posPiecesIter = ourPlayerTracker.getPossibleSettlements().values().iterator();
    while (posPiecesIter.hasNext()) {
      posPiece = (PossiblePiece)posPiecesIter.next();
      posPiece.resetScore();
      posPiece.clearBiggestThreats();
    }
    posPiecesIter = ourPlayerTracker.getPossibleRoads().values().iterator();
    while (posPiecesIter.hasNext()) {
      posPiece = (PossiblePiece)posPiecesIter.next();
      posPiece.resetScore();
      posPiece.clearBiggestThreats();
    }

    switch (strategy) {
    case SMART_STRATEGY:
      smartGameStrategy(currentBuildingETAs);
      break;
      
    case FAST_STRATEGY:
      dumbFastGameStrategy(currentBuildingETAs);
      break;
    }


    ///
    /// if we have a road building card, make sure 
    /// we build two roads first
    ///
    if ((strategy == SMART_STRATEGY) &&
	!ourPlayerData.hasPlayedDevCard() &&
	ourPlayerData.getNumPieces(PlayingPiece.ROAD) >= 2 &&
	ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.ROADS) > 0) {
      PossibleRoad secondFavoriteRoad = null;
      Enumeration threatenedRoadEnum;
      Enumeration goodRoadEnum;
      D.ebugPrintln("*** making a plan for road building");

      ///
      /// we need to pick two roads
      ///
      if (favoriteRoad != null) {
	//
	//  pretend to put the favorite road down, 
	//  and then score the new pos roads
	//
	Road tmpRoad = new Road(ourPlayerData, favoriteRoad.getCoordinates(), null);
	
	HashMap trackersCopy = PlayerTracker.tryPutPiece(tmpRoad, game, playerTrackers);
	PlayerTracker.updateWinGameETAs(trackersCopy);
				
	PlayerTracker ourPlayerTrackerCopy = (PlayerTracker)trackersCopy.get(new Integer(ourPlayerData.getPlayerNumber()));

	int ourCurrentWGETACopy = ourPlayerTrackerCopy.getWinGameETA();
	D.ebugPrintln("ourCurrentWGETACopy = "+ourCurrentWGETACopy);
				
	int leadersCurrentWGETACopy = ourCurrentWGETACopy;
	Iterator trackersCopyIter = trackersCopy.values().iterator();
	while (trackersCopyIter.hasNext()) {
	  PlayerTracker tracker = (PlayerTracker)trackersCopyIter.next();
	  int wgeta = tracker.getWinGameETA();
	  if (wgeta < leadersCurrentWGETACopy) {
	    leadersCurrentWGETACopy = wgeta;
	  }
	}

	Enumeration newPosEnum = favoriteRoad.getNewPossibilities().elements();
	while (newPosEnum.hasMoreElements()) {
	  PossiblePiece newPos = (PossiblePiece)newPosEnum.nextElement();
	  if (newPos.getType() == PossiblePiece.ROAD) {
	    newPos.resetScore();
	    // float wgetaScore = getWinGameETABonusForRoad((PossibleRoad)newPos, currentBuildingETAs[BuildingSpeedEstimate.ROAD], leadersCurrentWGETACopy, trackersCopy);


	    D.ebugPrintln("$$$ new pos road at "+Integer.toHexString(newPos.getCoordinates())+" has a score of "+newPos.getScore());

	    if (favoriteRoad.getCoordinates() != newPos.getCoordinates()) {
	      if (secondFavoriteRoad == null) {
		secondFavoriteRoad = (PossibleRoad)newPos;
	      } else {
		if (newPos.getScore() > secondFavoriteRoad.getScore()) {
		  secondFavoriteRoad = (PossibleRoad)newPos;
		}
	      }
	    }
	  }
	}
				
	threatenedRoadEnum = threatenedRoads.elements();
	while (threatenedRoadEnum.hasMoreElements()) {
	  PossibleRoad threatenedRoad = (PossibleRoad)threatenedRoadEnum.nextElement();
	  D.ebugPrintln("$$$ threatened road at "+Integer.toHexString(threatenedRoad.getCoordinates()));
					
	  //
	  // see how building this piece impacts our winETA
	  //
	  threatenedRoad.resetScore();
	  // float wgetaScore = getWinGameETABonusForRoad(threatenedRoad, currentBuildingETAs[BuildingSpeedEstimate.ROAD], leadersCurrentWGETA, playerTrackers);

	  D.ebugPrintln("$$$  final score = "+threatenedRoad.getScore());
					
	  if (favoriteRoad.getCoordinates() != threatenedRoad.getCoordinates()) {
	    if (secondFavoriteRoad == null) {
	      secondFavoriteRoad = threatenedRoad;
	    } else {
	      if (threatenedRoad.getScore() > secondFavoriteRoad.getScore()) {
	      secondFavoriteRoad = threatenedRoad;
	      }
	    }
	  }
	}
	goodRoadEnum = goodRoads.elements();
	while (goodRoadEnum.hasMoreElements()) {
	  PossibleRoad goodRoad = (PossibleRoad)goodRoadEnum.nextElement();
	  D.ebugPrintln("$$$ good road at "+Integer.toHexString(goodRoad.getCoordinates()));
	  //
	  // see how building this piece impacts our winETA
	  //
	  goodRoad.resetScore();
	  // float wgetaScore = getWinGameETABonusForRoad(goodRoad, currentBuildingETAs[BuildingSpeedEstimate.ROAD], leadersCurrentWGETA, playerTrackers);

	  D.ebugPrintln("$$$  final score = "+goodRoad.getScore());

	  if (favoriteRoad.getCoordinates() != goodRoad.getCoordinates()) {
	    if (secondFavoriteRoad == null) {
	      secondFavoriteRoad = goodRoad;
	    } else {
	      if (goodRoad.getScore() > secondFavoriteRoad.getScore()) {
		secondFavoriteRoad = goodRoad;
	      }
	    }
	  }
	}
				
	PlayerTracker.undoTryPutPiece(tmpRoad, game);

	if (!buildingPlan.empty()) {
	  PossiblePiece planPeek = (PossiblePiece)buildingPlan.peek();
	  if ((planPeek == null) ||
	      (planPeek.getType() != PlayingPiece.ROAD)) {
	    if (secondFavoriteRoad != null) {
	      D.ebugPrintln("### SECOND FAVORITE ROAD IS AT "+Integer.toHexString(secondFavoriteRoad.getCoordinates()));
	      D.ebugPrintln("###   WITH A SCORE OF "+secondFavoriteRoad.getScore());
	      D.ebugPrintln("$ PUSHING "+secondFavoriteRoad);
	      buildingPlan.push(secondFavoriteRoad);
	      D.ebugPrintln("$ PUSHING "+favoriteRoad);
	      buildingPlan.push(favoriteRoad);
	    }
	  } else if (secondFavoriteRoad != null) {
	    PossiblePiece tmp = (PossiblePiece)buildingPlan.pop();
	    D.ebugPrintln("$ POPPED OFF");
	    D.ebugPrintln("### SECOND FAVORITE ROAD IS AT "+Integer.toHexString(secondFavoriteRoad.getCoordinates()));
	    D.ebugPrintln("###   WITH A SCORE OF "+secondFavoriteRoad.getScore());
	    D.ebugPrintln("$ PUSHING "+secondFavoriteRoad);
	    buildingPlan.push(secondFavoriteRoad);
	    D.ebugPrintln("$ PUSHING "+tmp);
	    buildingPlan.push(tmp);
	  }
	}     
      } 
    } 
    //long endTime = System.currentTimeMillis();
    //System.out.println("plan time: "+(endTime-startTime));
  }
  
  /**
   * dumbFastGameStrategy
   * uses rules to determine what to build next
   *
   * @param buildingETAs  the etas for building something
   */
  protected void dumbFastGameStrategy(int[] buildingETAs)
  {
    D.ebugPrintln("***** dumbFastGameStrategy *****");

    // If this game is on the 6-player board, check whether we're planning for
    // the Special Building Phase.  Can't buy cards or trade in that phase.
    final boolean forSpecialBuildingPhase =
        game.isSpecialBuilding() || (game.getCurrentPlayerNumber() != ourPlayerData.getPlayerNumber());

    int bestETA = 500;
    BuildingSpeedEstimate ourBSE = new BuildingSpeedEstimate(ourPlayerData.getNumbers());

    if (ourPlayerData.getTotalVP() < 5) {
      //
      // less than 5 points, don't consider LR or LA
      //

      //
      // score possible cities
      //
      if (ourPlayerData.getNumPieces(PlayingPiece.CITY) > 0) {
	Iterator posCitiesIter = ourPlayerTracker.getPossibleCities().values().iterator();
	while (posCitiesIter.hasNext()) {
	  PossibleCity posCity = (PossibleCity)posCitiesIter.next();
	  D.ebugPrintln("Estimate speedup of city at "+game.getBoard().nodeCoordToString(posCity.getCoordinates()));
	  D.ebugPrintln("Speedup = "+posCity.getSpeedupTotal());
	  D.ebugPrintln("ETA = "+buildingETAs[BuildingSpeedEstimate.CITY]);
	  if ((brain != null) && (brain.getDRecorder().isOn())) {
	    brain.getDRecorder().startRecording("CITY"+posCity.getCoordinates());
	    brain.getDRecorder().record("Estimate speedup of city at "+game.getBoard().nodeCoordToString(posCity.getCoordinates()));
	    brain.getDRecorder().record("Speedup = "+posCity.getSpeedupTotal());
	    brain.getDRecorder().record("ETA = "+buildingETAs[BuildingSpeedEstimate.CITY]);
	    brain.getDRecorder().stopRecording();
	  }
	  if ((favoriteCity == null) ||
	      (posCity.getSpeedupTotal() > favoriteCity.getSpeedupTotal())) {
	    favoriteCity = posCity;
	    bestETA = buildingETAs[BuildingSpeedEstimate.CITY];
	  }
	}
      }

      //
      // score the possible settlements
      //
      scoreSettlementsForDumb(buildingETAs[BuildingSpeedEstimate.SETTLEMENT], ourBSE);

      //
      // pick something to build
      //
      Iterator posSetsIter = ourPlayerTracker.getPossibleSettlements().values().iterator();
      while (posSetsIter.hasNext()) {
	PossibleSettlement posSet = (PossibleSettlement)posSetsIter.next();
	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().startRecording("SETTLEMENT"+posSet.getCoordinates());
	  brain.getDRecorder().record("Estimate speedup of stlmt at "+game.getBoard().nodeCoordToString(posSet.getCoordinates()));
	  brain.getDRecorder().record("Speedup = "+posSet.getSpeedupTotal());
	  brain.getDRecorder().record("ETA = "+posSet.getETA());
	  Stack roadPath = posSet.getRoadPath();
	  if (roadPath!= null) {
	    brain.getDRecorder().record("Path:");
	    Iterator rpIter = roadPath.iterator();
	    while (rpIter.hasNext()) {
	      PossibleRoad posRoad = (PossibleRoad)rpIter.next();
	      brain.getDRecorder().record("Road at "+game.getBoard().edgeCoordToString(posRoad.getCoordinates()));
	    }
	  }
	  brain.getDRecorder().stopRecording();
	}
	if (posSet.getETA() < bestETA) {
	  bestETA = posSet.getETA();
	  favoriteSettlement = posSet;
	} else if (posSet.getETA() == bestETA) {
	  if (favoriteSettlement == null) {
	    if ((favoriteCity == null) || 
		(posSet.getSpeedupTotal() > favoriteCity.getSpeedupTotal())) {
	      favoriteSettlement = posSet;
	    }
	  } else {
	    if (posSet.getSpeedupTotal() > favoriteSettlement.getSpeedupTotal()) {
	      favoriteSettlement = posSet;
	    }
	  }
	}
      }
      
      if (favoriteSettlement != null) {
	//
	// we want to build a settlement
	//
	D.ebugPrintln("Picked favorite settlement at "+game.getBoard().nodeCoordToString(favoriteSettlement.getCoordinates()));
	buildingPlan.push(favoriteSettlement);
	if (!favoriteSettlement.getNecessaryRoads().isEmpty()) {
	  //
	  // we need to build roads first
	  //	  
	  Stack roadPath = favoriteSettlement.getRoadPath();
	  while (!roadPath.empty()) {
	    buildingPlan.push(roadPath.pop());
	  }
	}
      } else if (favoriteCity != null) {
	//
	// we want to build a city
	//
	D.ebugPrintln("Picked favorite city at "+game.getBoard().nodeCoordToString(favoriteCity.getCoordinates()));
	buildingPlan.push(favoriteCity);
      } else {
	//
	// we can't build a settlement or city
	//
        if ((game.getNumDevCards() > 0) && ! forSpecialBuildingPhase)
        {
            //
            // buy a card if there are any left
            //
            D.ebugPrintln("Buy a card");
            PossibleCard posCard = new PossibleCard(ourPlayerData, buildingETAs[BuildingSpeedEstimate.CARD]);
            buildingPlan.push(posCard);
        }
      }
    } else {
      //
      // we have more than 4 points
      //
      int choice = -1;
      //
      // consider Largest Army
      //
      D.ebugPrintln("Calculating Largest Army ETA");
      int laETA = 500;
      int laSize = 0;
      Player laPlayer = game.getPlayerWithLargestArmy();
      if (laPlayer == null) {
	///
	/// no one has largest army
	///
	laSize = 3;
      } else if (laPlayer.getPlayerNumber() == ourPlayerData.getPlayerNumber()) {
	///
	/// we have largest army
	///
	D.ebugPrintln("We have largest army");
      } else {
	laSize = laPlayer.getNumKnights() + 1;
      }
      ///
      /// figure out how many knights we need to buy
      ///
      int knightsToBuy = 0;
      if ((ourPlayerData.getNumKnights() + 
	   ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.KNIGHT) +
	   ourPlayerData.getDevCards().getAmount(DevCardSet.NEW, DevCardConstants.KNIGHT))
	  < laSize) {
	knightsToBuy = laSize - (ourPlayerData.getNumKnights() +
				 ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.KNIGHT));
      }
      D.ebugPrintln("knightsToBuy = "+knightsToBuy);
      if (ourPlayerData.getGame().getNumDevCards() >= knightsToBuy) {      
	///
	/// figure out how long it takes to buy this many knights
	///
	ResourceSet targetResources = new ResourceSet();
	for (int i = 0; i < knightsToBuy; i++) {
	  targetResources.add(Game.CARD_SET);
	}
	try {
	  ResSetBuildTimePair timePair = ourBSE.calculateRollsFast(ourPlayerData.getResources(), targetResources, 100, ourPlayerData.getPortFlags());
	  laETA = timePair.getRolls();
	} catch (CutoffExceededException ex) {
	  laETA = 100;
	}      
      } else {
	///
	/// not enough dev cards left
	///
      }
      if ((laETA < bestETA) && ! forSpecialBuildingPhase)
      {
	bestETA = laETA;
	choice = LA_CHOICE;
      }
      D.ebugPrintln("laETA = "+laETA);
      
      //
      // consider Longest Road
      //
      D.ebugPrintln("Calculating Longest Road ETA");
      int lrETA = 500;
      Stack bestLRPath = null;
      int lrLength;
      Player lrPlayer = game.getPlayerWithLongestRoad();
      if ((lrPlayer != null) && 
	  (lrPlayer.getPlayerNumber() == ourPlayerData.getPlayerNumber())) {
	///
	/// we have longest road
	///
	D.ebugPrintln("We have longest road");
      } else {
	if (lrPlayer == null) {
	  ///
	  /// no one has longest road
	  ///
	  lrLength = Math.max(4, ourPlayerData.getLongestRoadLength());
	} else {
	  lrLength = lrPlayer.getLongestRoadLength();
	}
	Iterator lrPathsIter = ourPlayerData.getLRPaths().iterator();
	int depth;
	while (lrPathsIter.hasNext()) {
	  Stack path;
	  LRPathData pathData = (LRPathData)lrPathsIter.next();
	  depth = Math.min(((lrLength + 1) - pathData.getLength()), ourPlayerData.getNumPieces(PlayingPiece.ROAD));
	  path = recalcLongestRoadETAAux(pathData.getBeginning(), pathData.getLength(), lrLength, depth);
	  if ((path != null) &&
	      ((bestLRPath == null) ||
	       (path.size() < bestLRPath.size()))) {
	    bestLRPath = path;
	  }
	  path = recalcLongestRoadETAAux(pathData.getEnd(), pathData.getLength(), lrLength, depth);
	  if ((path != null) &&
	      ((bestLRPath == null) ||
	       (path.size() < bestLRPath.size()))) {
	    bestLRPath = path;
	  }
	}
	if (bestLRPath != null) {
	  //
	  // calculate LR eta
	  //
	  D.ebugPrintln("Number of roads: "+bestLRPath.size());
	  ResourceSet targetResources = new ResourceSet();
	  for (int i = 0; i < bestLRPath.size(); i++) {
	    targetResources.add(Game.ROAD_SET);
	  }
	  try {
	    ResSetBuildTimePair timePair = ourBSE.calculateRollsFast(ourPlayerData.getResources(), targetResources, 100, ourPlayerData.getPortFlags());
	    lrETA = timePair.getRolls();
	  } catch (CutoffExceededException ex) {
	    lrETA = 100;
	  } 
	}
      }
      if (lrETA < bestETA) {
	bestETA = lrETA;
	choice = LR_CHOICE;
      }
      D.ebugPrintln("lrETA = "+lrETA);
      
      //
      // consider possible cities
      //
      if ((ourPlayerData.getNumPieces(PlayingPiece.CITY) > 0) &&
	  (buildingETAs[BuildingSpeedEstimate.CITY] <= bestETA)) {
	Iterator posCitiesIter = ourPlayerTracker.getPossibleCities().values().iterator();
	while (posCitiesIter.hasNext()) {
	  PossibleCity posCity = (PossibleCity)posCitiesIter.next();
	  if ((brain != null) && (brain.getDRecorder().isOn())) {
	    brain.getDRecorder().startRecording("CITY"+posCity.getCoordinates());
	    brain.getDRecorder().record("Estimate speedup of city at "+game.getBoard().nodeCoordToString(posCity.getCoordinates()));
	    brain.getDRecorder().record("Speedup = "+posCity.getSpeedupTotal());
	    brain.getDRecorder().record("ETA = "+buildingETAs[BuildingSpeedEstimate.CITY]);
	    brain.getDRecorder().stopRecording();
	  }
	  if ((favoriteCity == null) ||
	      (posCity.getSpeedupTotal() > favoriteCity.getSpeedupTotal())) {
	    favoriteCity = posCity;
	    bestETA = buildingETAs[BuildingSpeedEstimate.CITY];
	    choice = CITY_CHOICE;
	  }
	}
      }

      //
      // consider possible settlements
      //
      if (ourPlayerData.getNumPieces(PlayingPiece.SETTLEMENT) > 0) {
	scoreSettlementsForDumb(buildingETAs[BuildingSpeedEstimate.SETTLEMENT], ourBSE);
	Iterator posSetsIter = ourPlayerTracker.getPossibleSettlements().values().iterator();
	while (posSetsIter.hasNext()) {
	  PossibleSettlement posSet = (PossibleSettlement)posSetsIter.next();
	  if ((brain != null) && (brain.getDRecorder().isOn())) {
	    brain.getDRecorder().startRecording("SETTLEMENT"+posSet.getCoordinates());
	    brain.getDRecorder().record("Estimate speedup of stlmt at "+game.getBoard().nodeCoordToString(posSet.getCoordinates()));
	    brain.getDRecorder().record("Speedup = "+posSet.getSpeedupTotal());
	    brain.getDRecorder().record("ETA = "+posSet.getETA());
	    Stack roadPath = posSet.getRoadPath();
	    if (roadPath!= null) {
	      brain.getDRecorder().record("Path:");
	      Iterator rpIter = roadPath.iterator();
	      while (rpIter.hasNext()) {
		PossibleRoad posRoad = (PossibleRoad)rpIter.next();
		brain.getDRecorder().record("Road at "+game.getBoard().edgeCoordToString(posRoad.getCoordinates()));
	      }
	    }
	    brain.getDRecorder().stopRecording();
	  }
	  if ((posSet.getRoadPath() == null) ||
	      (ourPlayerData.getNumPieces(PlayingPiece.ROAD) >= posSet.getRoadPath().size())) {
	    if (posSet.getETA() < bestETA) {
	      bestETA = posSet.getETA();
	      favoriteSettlement = posSet;
	      choice = SETTLEMENT_CHOICE;
	    } else if (posSet.getETA() == bestETA) {
	      if (favoriteSettlement == null) {
		if ((favoriteCity == null) ||
		    (posSet.getSpeedupTotal() > favoriteCity.getSpeedupTotal())) {
		  favoriteSettlement = posSet;
		  choice = SETTLEMENT_CHOICE;
		}
	      } else {
		if (posSet.getSpeedupTotal() > favoriteSettlement.getSpeedupTotal()) {
		  favoriteSettlement = posSet;
		}
	      }
	    }
	  }
	}
      }
      
      //
      // pick something to build
      //
      switch (choice) {
      case LA_CHOICE:
        D.ebugPrintln("Picked LA");
        if (! forSpecialBuildingPhase)
        {
            for (int i = 0; i < knightsToBuy; i++)
            {
                PossibleCard posCard = new PossibleCard(ourPlayerData, 1);
                buildingPlan.push(posCard);
            }
        }
        break;
	
      case LR_CHOICE:
	D.ebugPrintln("Picked LR");
	while (!bestLRPath.empty()) {
	  PossibleRoad pr = (PossibleRoad)bestLRPath.pop();
	  D.ebugPrintln("LR road at "+game.getBoard().edgeCoordToString(pr.getCoordinates()));
	  buildingPlan.push(pr);
	}
	break;

      case CITY_CHOICE:
	D.ebugPrintln("Picked favorite city at "+game.getBoard().nodeCoordToString(favoriteCity.getCoordinates()));
	buildingPlan.push(favoriteCity);
	break;
	
      case SETTLEMENT_CHOICE:
	D.ebugPrintln("Picked favorite settlement at "+game.getBoard().nodeCoordToString(favoriteSettlement.getCoordinates()));
	buildingPlan.push(favoriteSettlement);
	if (!favoriteSettlement.getNecessaryRoads().isEmpty()) {
	  //
	  // we need to build roads first
	  //	  
	  Stack roadPath = favoriteSettlement.getRoadPath();
	  while (!roadPath.empty()) {
	    PossibleRoad pr = (PossibleRoad)roadPath.pop();
	    D.ebugPrintln("Nec road at "+game.getBoard().edgeCoordToString(pr.getCoordinates()));
	    buildingPlan.push(pr);
	  }
	}
      }
    }
  }
  
  /**
   * score all possible settlements by getting their speedup total
   * calculate ETA by finding shortest path and then using a
   * BuildingSpeedEstimate to find the ETA
   *
   * @param settlementETA  eta for building a settlement from now
   */
  protected void scoreSettlementsForDumb(int settlementETA, BuildingSpeedEstimate ourBSE) {
    D.ebugPrintln("-- scoreSettlementsForDumb --");
    Queue queue = new Queue();
    Iterator posSetsIter = ourPlayerTracker.getPossibleSettlements().values().iterator();
    while (posSetsIter.hasNext()) {
      PossibleSettlement posSet = (PossibleSettlement)posSetsIter.next();
      D.ebugPrintln("Estimate speedup of stlmt at "+game.getBoard().nodeCoordToString(posSet.getCoordinates()));
      D.ebugPrintln("***    speedup total = "+posSet.getSpeedupTotal());
	
      ///
      /// find the shortest path to this settlement
      ///
      Vector necRoadVec = posSet.getNecessaryRoads();
      if (!necRoadVec.isEmpty()) {
	queue.clear();
	Iterator necRoadsIter = necRoadVec.iterator();
	while (necRoadsIter.hasNext()) {
	  PossibleRoad necRoad = (PossibleRoad)necRoadsIter.next();
	  D.ebugPrintln("-- queuing necessary road at "+game.getBoard().edgeCoordToString(necRoad.getCoordinates()));
	  queue.put(new Pair(necRoad, null));
	}
	//
	// Do a BFS of the necessary road paths looking for the shortest one.
	//
	while (!queue.empty()) {
	  Pair dataPair = (Pair)queue.get();
	  PossibleRoad curRoad = (PossibleRoad)dataPair.getA();
	  D.ebugPrintln("-- current road at "+game.getBoard().edgeCoordToString(curRoad.getCoordinates()));
	  Vector necRoads = curRoad.getNecessaryRoads();
	  if (necRoads.isEmpty()) {
	    //
	    // we have a path 
	    //
	    D.ebugPrintln("Found a path!");
	    Stack path = new Stack();
	    path.push(curRoad);
	    Pair curPair = (Pair)dataPair.getB();
	    D.ebugPrintln("curPair = "+curPair);
	    while (curPair != null) {
	      path.push(curPair.getA());
	      curPair = (Pair)curPair.getB();
	    }
	    posSet.setRoadPath(path);
	    queue.clear();
	    D.ebugPrintln("Done setting path.");
	  } else {
	    necRoadsIter = necRoads.iterator();
	    while (necRoadsIter.hasNext()) {
	      PossibleRoad necRoad2 = (PossibleRoad)necRoadsIter.next();
	      D.ebugPrintln("-- queuing necessary road at "+game.getBoard().edgeCoordToString(necRoad2.getCoordinates()));
	      queue.put(new Pair(necRoad2, dataPair));
	    }
	  }
	}
	D.ebugPrintln("Done searching for path.");

	//
	// calculate ETA
	//
	ResourceSet targetResources = new ResourceSet();
	targetResources.add(Game.SETTLEMENT_SET);
	int pathLength = 0;
	Stack path = posSet.getRoadPath();
	if (path != null) {
	  pathLength = path.size();
	}
	for (int i = 0; i < pathLength; i++) {
	  targetResources.add(Game.ROAD_SET);
	}
	try {
	  ResSetBuildTimePair timePair = ourBSE.calculateRollsFast(ourPlayerData.getResources(), targetResources, 100, ourPlayerData.getPortFlags());
	  posSet.setETA(timePair.getRolls());
	} catch (CutoffExceededException ex) {
	  posSet.setETA(100);
	}
      } else {
	//
	// no roads are necessary
	//
	posSet.setRoadPath(null);
	posSet.setETA(settlementETA);
      }
      D.ebugPrintln("Settlement ETA = "+posSet.getETA());
    }
  }

  /**
   * Does a depth first search from the end point of the longest
   * path in a graph of nodes and returns how many roads would 
   * need to be built to take longest road.
   *
   * @param startNode     the path endpoint
   * @param pathLength    the length of that path
   * @param lrLength      length of longest road in the game
   * @param searchDepth   how many roads out to search
   *
   * @return a stack containing the path of roads with the last one on top, or null if it can't be done
   */
  private Stack recalcLongestRoadETAAux(int startNode, int pathLength, int lrLength, int searchDepth) {
    D.ebugPrintln("=== recalcLongestRoadETAAux("+Integer.toHexString(startNode)+","+pathLength+","+lrLength+","+searchDepth+")");
    
    //
    // we're doing a depth first search of all possible road paths 
    //
    int longest = 0;
    int numRoads = 500;
    Pair bestPathNode = null;
    Stack pending = new Stack();
    pending.push(new Pair(new NodeLenVis(startNode, pathLength, new Vector()), null));

    while (!pending.empty()) {
      Pair dataPair = (Pair)pending.pop();
      NodeLenVis curNode = (NodeLenVis)dataPair.getA();
      D.ebugPrintln("curNode = "+curNode);
      int coord = curNode.node;
      int len = curNode.len;
      Vector visited = curNode.vis;
      boolean pathEnd = false;
      
      //
      // check for road blocks 
      //
      Enumeration pEnum = game.getBoard().getPieces().elements();
      while (pEnum.hasMoreElements()) {
	PlayingPiece p = (PlayingPiece)pEnum.nextElement();
	if ((len > 0) &&
	    (p.getPlayer().getPlayerNumber() != ourPlayerData.getPlayerNumber()) &&
	    ((p.getType() == PlayingPiece.SETTLEMENT) ||
	     (p.getType() == PlayingPiece.CITY)) &&
	    (p.getCoordinates() == coord)) {
	  pathEnd = true;
	  D.ebugPrintln("^^^ path end at "+Integer.toHexString(coord));
	  break;
	}
      }

      if (!pathEnd) {
	// 
	// check if we've connected to another road graph
	//
	Iterator lrPathsIter = ourPlayerData.getLRPaths().iterator();
	while (lrPathsIter.hasNext()) {
	  LRPathData pathData = (LRPathData)lrPathsIter.next();
	  if ((startNode != pathData.getBeginning() &&
	       startNode != pathData.getEnd()) &&
	      (coord == pathData.getBeginning() ||
	       coord == pathData.getEnd())) {
	    pathEnd = true;
	    len += pathData.getLength();
	    D.ebugPrintln("connecting to another path: "+pathData);
	    D.ebugPrintln("len = "+len);
	    break;
	  }      
	}
      }
      
      if (!pathEnd) {
	//
	// (len - pathLength) = how many new roads we've built
	//
	if ((len - pathLength) >= searchDepth) {
	  pathEnd = true;
	}
	D.ebugPrintln("Reached search depth");
      }

      if (!pathEnd) {
	pathEnd = true;

	int j;		
	Integer edge;
	boolean match;

	j = coord - 0x11;
	edge = new Integer(j);
	match = false;
	if ((j >= Board.MINEDGE) && (j <= Board.MAXEDGE) &&
	    (ourPlayerData.isLegalRoad(j))) {
	  for (Enumeration ev = visited.elements();
	       ev.hasMoreElements(); ) {
	    Integer vis = (Integer)ev.nextElement();
	    if (vis.equals(edge)) {
	      match = true;
	      break;
	    }
	  }	
	  if (!match) {
	    Vector newVis = (Vector)visited.clone();
	    newVis.addElement(edge);
	    // node coord and edge coord are the same
	    pending.push(new Pair(new NodeLenVis(j, len+1, newVis), dataPair));
	    pathEnd = false;
	  }
	}
	j = coord;
	edge = new Integer(j);
	match = false;
	if ((j >= Board.MINEDGE) && (j <= Board.MAXEDGE) &&
	    (ourPlayerData.isLegalRoad(j))) {
	  for (Enumeration ev = visited.elements();
	       ev.hasMoreElements(); ) {
	    Integer vis = (Integer)ev.nextElement();
	    if (vis.equals(edge)) {
	      match = true;
	      break;
	    }
	  }	
	  if (!match) {
	    Vector newVis = (Vector)visited.clone();
	    newVis.addElement(edge);
	    // coord for node = edge + 0x11
	    j += 0x11;
	    pending.push(new Pair(new NodeLenVis(j, len+1, newVis), dataPair));
	    pathEnd = false;
	  }
	}
	j = coord - 0x01;
	edge = new Integer(j);
	match = false;
	if ((j >= Board.MINEDGE) && (j <= Board.MAXEDGE) &&
	    (ourPlayerData.isLegalRoad(j))) {
	  for (Enumeration ev = visited.elements();
	       ev.hasMoreElements(); ) {
	    Integer vis = (Integer)ev.nextElement();
	    if (vis.equals(edge)) {
	      match = true;
	      break;
	    }
	  }	
	  if (!match) {
	    Vector newVis = (Vector)visited.clone();
	    newVis.addElement(edge);
	    // node coord = edge coord + 0x10
	    j += 0x10;
	    pending.push(new Pair(new NodeLenVis(j, len+1, newVis), dataPair));
	    pathEnd = false;
	  }
	}	 
	j = coord - 0x10;
	edge = new Integer(j);
	match = false;
	if ((j >= Board.MINEDGE) && (j <= Board.MAXEDGE) &&
	    (ourPlayerData.isLegalRoad(j))) {
	  for (Enumeration ev = visited.elements();
	       ev.hasMoreElements(); ) {
	    Integer vis = (Integer)ev.nextElement();
	    if (vis.equals(edge)) {
	      match = true;
	      break;
	    }
	  }	
	  if (!match) {
	    Vector newVis = (Vector)visited.clone();
	    newVis.addElement(edge);
	    // node coord = edge coord + 0x01
	    j += 0x01;
	    pending.push(new Pair(new NodeLenVis(j, len+1, newVis), dataPair));
	    pathEnd = false;
	  }
	}
      }		
      
      if (pathEnd) {
	if (len > longest) {
	  longest = len;
	  numRoads = curNode.len - pathLength;
	  bestPathNode = dataPair;
	} else if ((len == longest) &&
		   (curNode.len < numRoads)) {
	  numRoads = curNode.len - pathLength;
	  bestPathNode = dataPair;
	}
      }
    }
    if ((longest > lrLength) &&
	(bestPathNode != null)) {
      D.ebugPrintln("Converting nodes to road coords.");
      //
      // return the path in a stack with the last road on top
      //
      //
      // convert pairs of node coords to road coords
      //
      Stack temp = new Stack();
      PossibleRoad posRoad;
      int coordA, coordB, test;
      Pair cur, parent;
      cur = bestPathNode;
      parent = (Pair)bestPathNode.getB();
      while (parent != null) {
	coordA = ((NodeLenVis)cur.getA()).node;
	coordB = ((NodeLenVis)parent.getA()).node;
	test = coordA - coordB;
	if (test == 0x11) {
	  // it is a '\' road
	  D.ebugPrintln(game.getBoard().nodeCoordToString(coordB));
	  posRoad = new PossibleRoad(ourPlayerData, coordB, new Vector());
	} else if (test == -0x11) {
	  // it is a '/' road
	  D.ebugPrintln(game.getBoard().nodeCoordToString(coordA));
	  posRoad = new PossibleRoad(ourPlayerData, coordA, new Vector());
	} else if (test == 0x0F) {
	  // it is a '|' road for an A node
	  D.ebugPrintln(game.getBoard().nodeCoordToString((coordA - 0x10)));
	  posRoad = new PossibleRoad(ourPlayerData, (coordA - 0x10), new Vector());
	} else {
	  // it is a '|' road for a Y node
	  D.ebugPrintln(game.getBoard().nodeCoordToString((coordA - 0x01)));
	  posRoad = new PossibleRoad(ourPlayerData, (coordA - 0x01), new Vector());
	}
	temp.push(posRoad);
	cur = parent;
	parent = (Pair)parent.getB();
      }
      //
      // reverse the order of the roads so that the last one is on top
      //
      Stack path = new Stack();
      while (!temp.empty()) {
	path.push(temp.pop());
      }
      return path;
    } else {
      return null;
    }
  }

  /**
   * smart game strategy
   * use WGETA to determine best move
   *
   * @param buildingETAs  the etas for building something
   */
  protected void smartGameStrategy(int[] buildingETAs)
  {
    D.ebugPrintln("***** smartGameStrategy *****");

    // If this game is on the 6-player board, check whether we're planning for
    // the Special Building Phase.  Can't buy cards or trade in that phase.
    final boolean forSpecialBuildingPhase =
        game.isSpecialBuilding() || (game.getCurrentPlayerNumber() != ourPlayerData.getPlayerNumber());

    //
    // save the lr paths list to restore later
    //
    Vector savedLRPaths[] = new Vector[game.maxPlayers];
    for (int pn = 0; pn < game.maxPlayers; pn++) {
      savedLRPaths[pn] = (Vector)game.getPlayer(pn).getLRPaths().clone();
    }
    
    int ourCurrentWGETA = ourPlayerTracker.getWinGameETA();
    D.ebugPrintln("ourCurrentWGETA = "+ourCurrentWGETA);

    int leadersCurrentWGETA = ourCurrentWGETA;
    Iterator trackersIter = playerTrackers.values().iterator();
    while (trackersIter.hasNext()) {
      PlayerTracker tracker = (PlayerTracker)trackersIter.next();
      int wgeta = tracker.getWinGameETA();
      if (wgeta < leadersCurrentWGETA) {
	leadersCurrentWGETA = wgeta;
      }
    }

    ///
    /// score the possible settlements
    ///
    if (ourPlayerData.getNumPieces(PlayingPiece.SETTLEMENT) > 0) {
      scorePossibleSettlements(buildingETAs[BuildingSpeedEstimate.SETTLEMENT], leadersCurrentWGETA);
    }

    ///
    /// collect roads that we can build now
    ///
    if (ourPlayerData.getNumPieces(PlayingPiece.ROAD) > 0) {
      Iterator posRoadsIter = ourPlayerTracker.getPossibleRoads().values().iterator();
      while (posRoadsIter.hasNext()) {
	PossibleRoad posRoad = (PossibleRoad)posRoadsIter.next();
	if ((posRoad.getNecessaryRoads().isEmpty()) &&
	    (!threatenedRoads.contains(posRoad)) &&
	    (!goodRoads.contains(posRoad))) {
	  goodRoads.addElement(posRoad);
	}
      }
    }

    /*
    ///
    /// check everything
    ///
    Enumeration threatenedSetEnum = threatenedSettlements.elements();
    while (threatenedSetEnum.hasMoreElements()) {
      PossibleSettlement threatenedSet = (PossibleSettlement)threatenedSetEnum.nextElement();
      D.ebugPrintln("*** threatened settlement at "+Integer.toHexString(threatenedSet.getCoordinates())+" has a score of "+threatenedSet.getScore());
      if (threatenedSet.getNecessaryRoads().isEmpty() &&
	  !ourPlayerData.isPotentialSettlement(threatenedSet.getCoordinates())) {
	D.ebugPrintln("POTENTIAL SETTLEMENT ERROR");
	//System.exit(0);
      } 
    }
    Enumeration goodSetEnum = goodSettlements.elements();
    while (goodSetEnum.hasMoreElements()) {
      PossibleSettlement goodSet = (PossibleSettlement)goodSetEnum.nextElement();
      D.ebugPrintln("*** good settlement at "+Integer.toHexString(goodSet.getCoordinates())+" has a score of "+goodSet.getScore());
      if (goodSet.getNecessaryRoads().isEmpty() &&
	  !ourPlayerData.isPotentialSettlement(goodSet.getCoordinates())) {
	D.ebugPrintln("POTENTIAL SETTLEMENT ERROR");
	//System.exit(0);
      } 
    }    
    Enumeration threatenedRoadEnum = threatenedRoads.elements();
    while (threatenedRoadEnum.hasMoreElements()) {
      PossibleRoad threatenedRoad = (PossibleRoad)threatenedRoadEnum.nextElement();
      D.ebugPrintln("*** threatened road at "+Integer.toHexString(threatenedRoad.getCoordinates())+" has a score of "+threatenedRoad.getScore());      	
      if (threatenedRoad.getNecessaryRoads().isEmpty() &&
	  !ourPlayerData.isPotentialRoad(threatenedRoad.getCoordinates())) {
	D.ebugPrintln("POTENTIAL ROAD ERROR");
	//System.exit(0);
      }
    }
    Enumeration goodRoadEnum = goodRoads.elements();
    while (goodRoadEnum.hasMoreElements()) {
      PossibleRoad goodRoad = (PossibleRoad)goodRoadEnum.nextElement();
      D.ebugPrintln("*** good road at "+Integer.toHexString(goodRoad.getCoordinates())+" has a score of "+goodRoad.getScore());
      if (goodRoad.getNecessaryRoads().isEmpty() &&
	  !ourPlayerData.isPotentialRoad(goodRoad.getCoordinates())) {
	D.ebugPrintln("POTENTIAL ROAD ERROR");
	//System.exit(0);
      }
    }  
    */

    D.ebugPrintln("PICKING WHAT TO BUILD");

    ///
    /// pick what we want to build
    ///
		
    ///
    /// pick a settlement that can be built now
    ///
    if (ourPlayerData.getNumPieces(PlayingPiece.SETTLEMENT) > 0) {
      Iterator threatenedSetIter = threatenedSettlements.iterator();
      while (threatenedSetIter.hasNext()) {
	PossibleSettlement threatenedSet = (PossibleSettlement)threatenedSetIter.next();
	if (threatenedSet.getNecessaryRoads().isEmpty()) {
	  D.ebugPrintln("$$$$$ threatened settlement at "+Integer.toHexString(threatenedSet.getCoordinates())+" has a score of "+threatenedSet.getScore());

	  if ((favoriteSettlement == null) ||
	      (threatenedSet.getScore() > favoriteSettlement.getScore())) {
	    favoriteSettlement = threatenedSet;
	  }
	}
      } 

      Iterator goodSetIter = goodSettlements.iterator();
      while (goodSetIter.hasNext()) {
	PossibleSettlement goodSet = (PossibleSettlement)goodSetIter.next();
	if (goodSet.getNecessaryRoads().isEmpty()) {
	  D.ebugPrintln("$$$$$ good settlement at "+Integer.toHexString(goodSet.getCoordinates())+" has a score of "+goodSet.getScore());

	  if ((favoriteSettlement == null) ||
	      (goodSet.getScore() > favoriteSettlement.getScore())) {
	    favoriteSettlement = goodSet;
	  }
	}
      }
    }

    //
    // restore the LRPath list
    //
    D.ebugPrintln("%%% RESTORING LRPATH LIST %%%");
    for (int pn = 0; pn < game.maxPlayers; pn++) {
      game.getPlayer(pn).setLRPaths(savedLRPaths[pn]);
    } 
    
    ///
    /// pick a road that can be built now
    ///
    if (ourPlayerData.getNumPieces(PlayingPiece.ROAD) > 0) {
      Iterator threatenedRoadIter = threatenedRoads.iterator();
      while (threatenedRoadIter.hasNext()) {
	PossibleRoad threatenedRoad = (PossibleRoad)threatenedRoadIter.next();
	D.ebugPrintln("$$$$$ threatened road at "+Integer.toHexString(threatenedRoad.getCoordinates()));

	if ((brain != null) && (brain.getDRecorder().isOn())) {	  
	  brain.getDRecorder().startRecording("ROAD"+threatenedRoad.getCoordinates());
	  brain.getDRecorder().record("Estimate value of road at "+game.getBoard().edgeCoordToString(threatenedRoad.getCoordinates()));
	} 
	
	//
	// see how building this piece impacts our winETA
	//
	threatenedRoad.resetScore();
	float wgetaScore = getWinGameETABonusForRoad(threatenedRoad, buildingETAs[BuildingSpeedEstimate.ROAD], leadersCurrentWGETA, playerTrackers);
	if ((brain != null) && (brain.getDRecorder().isOn())) {	  
	  brain.getDRecorder().stopRecording();
	} 
		
	D.ebugPrintln("wgetaScore = "+wgetaScore);

	if (favoriteRoad == null) {
	  favoriteRoad = threatenedRoad;
	} else {
	  if (threatenedRoad.getScore() > favoriteRoad.getScore()) {
	    favoriteRoad = threatenedRoad;
	  }
	}
      }
      Iterator goodRoadIter = goodRoads.iterator();
      while (goodRoadIter.hasNext()) {
	PossibleRoad goodRoad = (PossibleRoad)goodRoadIter.next();
	D.ebugPrintln("$$$$$ good road at "+Integer.toHexString(goodRoad.getCoordinates()));

	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().startRecording("ROAD"+goodRoad.getCoordinates());
	  brain.getDRecorder().record("Estimate value of road at "+game.getBoard().edgeCoordToString(goodRoad.getCoordinates()));
	} 

	//
	// see how building this piece impacts our winETA
	//
	goodRoad.resetScore();
	float wgetaScore = getWinGameETABonusForRoad(goodRoad, buildingETAs[BuildingSpeedEstimate.ROAD], leadersCurrentWGETA, playerTrackers);
	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().stopRecording();
	} 
	
	D.ebugPrintln("wgetaScore = "+wgetaScore);					

	if (favoriteRoad == null) {
	  favoriteRoad = goodRoad;
	} else {
	  if (goodRoad.getScore() > favoriteRoad.getScore()) {
	    favoriteRoad = goodRoad;
	  }
	}
      }
    }

    //
    // restore the LRPath list
    //
    D.ebugPrintln("%%% RESTORING LRPATH LIST %%%");
    for (int pn = 0; pn < game.maxPlayers; pn++) {
      game.getPlayer(pn).setLRPaths(savedLRPaths[pn]);
    }  
    
    ///
    /// pick a city that can be built now
    ///
    if (ourPlayerData.getNumPieces(PlayingPiece.CITY) > 0) {
      HashMap trackersCopy = PlayerTracker.copyPlayerTrackers(playerTrackers);
      PlayerTracker ourTrackerCopy = (PlayerTracker)trackersCopy.get(new Integer(ourPlayerData.getPlayerNumber()));
      int originalWGETAs[] = new int[game.maxPlayers];	 
      int WGETAdiffs[] = new int[game.maxPlayers];	 
      Vector leaders = new Vector();
      int bestWGETA = 1000;
      // int bonus = 0;
				
      Iterator posCitiesIter = ourPlayerTracker.getPossibleCities().values().iterator();
      while (posCitiesIter.hasNext()) {
	PossibleCity posCity = (PossibleCity)posCitiesIter.next();
	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().startRecording("CITY"+posCity.getCoordinates());
	  brain.getDRecorder().record("Estimate value of city at "+game.getBoard().nodeCoordToString(posCity.getCoordinates()));
	} 
	
	//
	// see how building this piece impacts our winETA
	//
	leaders.clear();
	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().suspend();
	}
	PlayerTracker.updateWinGameETAs(trackersCopy);
	Iterator trackersBeforeIter = trackersCopy.values().iterator();
	while (trackersBeforeIter.hasNext()) {
	  PlayerTracker trackerBefore = (PlayerTracker)trackersBeforeIter.next();
	  D.ebugPrintln("$$$ win game ETA for player "+trackerBefore.getPlayer().getPlayerNumber()+" = "+trackerBefore.getWinGameETA());
	  originalWGETAs[trackerBefore.getPlayer().getPlayerNumber()] = trackerBefore.getWinGameETA();
	  WGETAdiffs[trackerBefore.getPlayer().getPlayerNumber()] = trackerBefore.getWinGameETA();
	  if (trackerBefore.getWinGameETA() < bestWGETA) {
	    bestWGETA = trackerBefore.getWinGameETA();
	    leaders.removeAllElements();
	    leaders.addElement(trackerBefore);
	  } else if (trackerBefore.getWinGameETA() == bestWGETA) {
	    leaders.addElement(trackerBefore);
	  }
	}		
	D.ebugPrintln("^^^^ bestWGETA = "+bestWGETA);
	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().resume();
	}
	//
	// place the city
	//
	City tmpCity = new City(ourPlayerData, posCity.getCoordinates(), null);
	game.putTempPiece(tmpCity);

	ourTrackerCopy.addOurNewCity(tmpCity);
				
	PlayerTracker.updateWinGameETAs(trackersCopy);

	float wgetaScore = calcWGETABonusAux(originalWGETAs, trackersCopy, leaders);

	//
	// remove the city
	//
	ourTrackerCopy.undoAddOurNewCity(posCity);
	game.undoPutTempPiece(tmpCity);

	D.ebugPrintln("*** ETA for city = "+buildingETAs[BuildingSpeedEstimate.CITY]);
	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().record("ETA = "+buildingETAs[BuildingSpeedEstimate.CITY]);
	} 	

	float etaBonus = getETABonus(buildingETAs[BuildingSpeedEstimate.CITY], leadersCurrentWGETA, wgetaScore);
	D.ebugPrintln("etaBonus = "+etaBonus);
	
	posCity.addToScore(etaBonus);
	//posCity.addToScore(wgetaScore);

	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().record("WGETA score = "+df1.format(wgetaScore));
	  brain.getDRecorder().record("Total city score = "+df1.format(etaBonus));
	  brain.getDRecorder().stopRecording();
	} 

	D.ebugPrintln("$$$  final score = "+posCity.getScore());

	D.ebugPrintln("$$$$$ possible city at "+Integer.toHexString(posCity.getCoordinates())+" has a score of "+posCity.getScore());

	if ((favoriteCity == null) ||
	    (posCity.getScore() > favoriteCity.getScore())) {
	  favoriteCity = posCity;
	}
      }
    }
         
    if (favoriteSettlement != null) {
      D.ebugPrintln("### FAVORITE SETTLEMENT IS AT "+Integer.toHexString(favoriteSettlement.getCoordinates()));
      D.ebugPrintln("###   WITH A SCORE OF "+favoriteSettlement.getScore());
      D.ebugPrintln("###   WITH AN ETA OF "+buildingETAs[BuildingSpeedEstimate.SETTLEMENT]);
      D.ebugPrintln("###   WITH A TOTAL SPEEDUP OF "+favoriteSettlement.getSpeedupTotal());
    }

    if (favoriteCity != null) {
      D.ebugPrintln("### FAVORITE CITY IS AT "+Integer.toHexString(favoriteCity.getCoordinates()));
      D.ebugPrintln("###   WITH A SCORE OF "+favoriteCity.getScore());
      D.ebugPrintln("###   WITH AN ETA OF "+buildingETAs[BuildingSpeedEstimate.CITY]);
      D.ebugPrintln("###   WITH A TOTAL SPEEDUP OF "+favoriteCity.getSpeedupTotal());
    }

    if (favoriteRoad != null) {
      D.ebugPrintln("### FAVORITE ROAD IS AT "+Integer.toHexString(favoriteRoad.getCoordinates()));
      D.ebugPrintln("###   WITH AN ETA OF "+buildingETAs[BuildingSpeedEstimate.ROAD]);
      D.ebugPrintln("###   WITH A SCORE OF "+favoriteRoad.getScore());
    }
    int pick = -1;
    ///
    /// if the best settlement can wait, and the best road can wait,
    /// and the city is the best speedup and eta, then build the city
    ///
    if ((favoriteCity != null) &&
	(ourPlayerData.getNumPieces(PlayingPiece.CITY) > 0) &&
	(favoriteCity.getScore() > 0) &&
	((favoriteSettlement == null) ||
	 (ourPlayerData.getNumPieces(PlayingPiece.SETTLEMENT) == 0) || 
	 (favoriteCity.getScore() > favoriteSettlement.getScore()) ||
	 ((favoriteCity.getScore() == favoriteSettlement.getScore()) &&
	  (buildingETAs[BuildingSpeedEstimate.CITY] < buildingETAs[BuildingSpeedEstimate.SETTLEMENT]))) &&
	((favoriteRoad == null) ||
	 (ourPlayerData.getNumPieces(PlayingPiece.ROAD) == 0) ||
	 (favoriteCity.getScore() > favoriteRoad.getScore()) ||
	 ((favoriteCity.getScore() == favoriteRoad.getScore()) &&
	  (buildingETAs[BuildingSpeedEstimate.CITY] < buildingETAs[BuildingSpeedEstimate.ROAD])))) {
      D.ebugPrintln("### PICKED FAVORITE CITY");
      pick = PlayingPiece.CITY;
      D.ebugPrintln("$ PUSHING "+favoriteCity);
      buildingPlan.push(favoriteCity);
    } 
    ///
    /// if there is a road with a better score than
    /// our favorite settlement, then build the road, 
    /// else build the settlement
    ///
    else if ((favoriteRoad != null) &&
	     (ourPlayerData.getNumPieces(PlayingPiece.ROAD) > 0) &&
	     (favoriteRoad.getScore() > 0) &&
	     ((favoriteSettlement == null) ||
	      (ourPlayerData.getNumPieces(PlayingPiece.SETTLEMENT) == 0) ||
	      (favoriteSettlement.getScore() < favoriteRoad.getScore()))) {
      D.ebugPrintln("### PICKED FAVORITE ROAD");
      pick = PlayingPiece.ROAD;
      D.ebugPrintln("$ PUSHING "+favoriteRoad);
      buildingPlan.push(favoriteRoad);
    } else if ((favoriteSettlement != null) &&
	       (ourPlayerData.getNumPieces(PlayingPiece.SETTLEMENT) > 0)) {
      D.ebugPrintln("### PICKED FAVORITE SETTLEMENT");
      pick = PlayingPiece.SETTLEMENT;
      D.ebugPrintln("$ PUSHING "+favoriteSettlement);
      buildingPlan.push(favoriteSettlement);
    }
    ///
    /// if buying a card is better than building...
    ///
			
    //
    // see how buying a card improves our win game ETA
    //
    if ((game.getNumDevCards() > 0) && ! forSpecialBuildingPhase)
    {
      if ((brain != null) && (brain.getDRecorder().isOn())) {
	brain.getDRecorder().startRecording("DEVCARD");
	brain.getDRecorder().record("Estimate value of a dev card");
      } 
      
      possibleCard = getDevCardScore(buildingETAs[BuildingSpeedEstimate.CARD], leadersCurrentWGETA);
      float devCardScore = possibleCard.getScore();
      D.ebugPrintln("### DEV CARD SCORE: "+devCardScore);
      if ((brain != null) && (brain.getDRecorder().isOn())) {
	brain.getDRecorder().stopRecording();
      } 
      
      if ((pick == -1) ||
	  ((pick == PlayingPiece.CITY) &&
	   (devCardScore > favoriteCity.getScore())) ||
	  ((pick == PlayingPiece.ROAD) &&
	   (devCardScore > favoriteRoad.getScore())) ||
	  ((pick == PlayingPiece.SETTLEMENT) &&
	   (devCardScore > favoriteSettlement.getScore()))) {
	D.ebugPrintln("### BUY DEV CARD");
				
	if (pick != -1) {
	  buildingPlan.pop();
	  D.ebugPrintln("$ POPPED OFF SOMETHING");
	}
		 
	D.ebugPrintln("$ PUSHING "+possibleCard);
	buildingPlan.push(possibleCard);
      }
    }
  }


  /**
   * score possible settlements for smartStrategy
   */
  protected void scorePossibleSettlements(int settlementETA, int leadersCurrentWGETA) {
    D.ebugPrintln("****** scorePossibleSettlements");

    Iterator posSetsIter = ourPlayerTracker.getPossibleSettlements().values().iterator();
    while (posSetsIter.hasNext()) {
      PossibleSettlement posSet = (PossibleSettlement)posSetsIter.next();
      D.ebugPrintln("*** scoring possible settlement at "+Integer.toHexString(posSet.getCoordinates()));
      if (!threatenedSettlements.contains(posSet)) {
	threatenedSettlements.addElement(posSet);
      } else if (!goodSettlements.contains(posSet)) {
	goodSettlements.addElement(posSet);
      }
      //
      // only consider settlements we can build now
      //
      Vector necRoadVec = posSet.getNecessaryRoads();
      if (necRoadVec.isEmpty()) {
	D.ebugPrintln("*** no roads needed");
	//
	//  no roads needed
	//
	//
	//  get wgeta score
	//
        Board board = game.getBoard();
	Settlement tmpSet = new Settlement(ourPlayerData, posSet.getCoordinates(), board);
	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().startRecording("SETTLEMENT"+posSet.getCoordinates());
	  brain.getDRecorder().record("Estimate value of settlement at "+board.nodeCoordToString(posSet.getCoordinates()));
	} 
	
	HashMap trackersCopy = PlayerTracker.tryPutPiece(tmpSet, game, playerTrackers);
	PlayerTracker.updateWinGameETAs(trackersCopy);
	float wgetaScore = calcWGETABonus(playerTrackers, trackersCopy);
	D.ebugPrintln("***  wgetaScore = "+wgetaScore);

	D.ebugPrintln("*** ETA for settlement = "+settlementETA);
	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().record("ETA = "+settlementETA);
	} 
	
	float etaBonus = getETABonus(settlementETA, leadersCurrentWGETA, wgetaScore);
	D.ebugPrintln("etaBonus = "+etaBonus);
	
	//posSet.addToScore(wgetaScore);
	posSet.addToScore(etaBonus);

	if ((brain != null) && (brain.getDRecorder().isOn())) {
	  brain.getDRecorder().record("WGETA score = "+df1.format(wgetaScore));
	  brain.getDRecorder().record("Total settlement score = "+df1.format(etaBonus));
	  brain.getDRecorder().stopRecording();
	} 
	
	PlayerTracker.undoTryPutPiece(tmpSet, game);
      }
    }
  }
  
  /**
   * add a bonus to the possible piece score based 
   * on the change in win game ETA
   *
   * @param posPiece  the possible piece that we're scoring
   */
  protected float getWinGameETABonus(PossiblePiece posPiece) {
    HashMap trackersCopy = null;
    Settlement tmpSet = null;
    City tmpCity = null;
    Road tmpRoad = null;
    float bonus = 0;
		

    D.ebugPrintln("--- before [start] ---");
    //PlayerTracker.playerTrackersDebug(playerTrackers);
    D.ebugPrintln("our player numbers = "+ourPlayerData.getNumbers());
    D.ebugPrintln("--- before [end] ---");
    switch (posPiece.getType()) {
    case PossiblePiece.SETTLEMENT:
      tmpSet = new Settlement(ourPlayerData, 
				 posPiece.getCoordinates(), null);
      trackersCopy = PlayerTracker.tryPutPiece(tmpSet, game, playerTrackers);
      break;

    case PossiblePiece.CITY:
      trackersCopy = PlayerTracker.copyPlayerTrackers(playerTrackers);
      tmpCity = new City(ourPlayerData, 
			    posPiece.getCoordinates(), null);
      game.putTempPiece(tmpCity);
      PlayerTracker trackerCopy = (PlayerTracker)trackersCopy.get(new Integer(ourPlayerData.getPlayerNumber()));
      if (trackerCopy != null) {
	trackerCopy.addOurNewCity(tmpCity);
      }
      break;
			
    case PossiblePiece.ROAD:
      tmpRoad = new Road(ourPlayerData, 
			    posPiece.getCoordinates(), null);
      trackersCopy = PlayerTracker.tryPutPiece(tmpRoad, game, playerTrackers);
      break;
    }

    //trackersCopyIter = trackersCopy.iterator();
    //while (trackersCopyIter.hasNext()) {
    //	PlayerTracker trackerCopy = (PlayerTracker)trackersCopyIter.next();
    //	trackerCopy.updateThreats(trackersCopy);
    //}

    D.ebugPrintln("--- after [start] ---");
    //PlayerTracker.playerTrackersDebug(trackersCopy);
    PlayerTracker.updateWinGameETAs(trackersCopy);

    float WGETABonus = calcWGETABonus(playerTrackers, trackersCopy);
    D.ebugPrintln("$$$ win game ETA bonus : +"+WGETABonus);
    bonus = WGETABonus;
		
    D.ebugPrintln("our player numbers = "+ourPlayerData.getNumbers());
    D.ebugPrintln("--- after [end] ---");
    switch (posPiece.getType()) {
    case PossiblePiece.SETTLEMENT:			
      PlayerTracker.undoTryPutPiece(tmpSet, game);
      break;

    case PossiblePiece.CITY:
      game.undoPutTempPiece(tmpCity);
      break;

    case PossiblePiece.ROAD:
      PlayerTracker.undoTryPutPiece(tmpRoad, game);
      break;
    }

    D.ebugPrintln("our player numbers = "+ourPlayerData.getNumbers());
    D.ebugPrintln("--- cleanup done ---");
		
    return bonus;
  }


  /**
   * add a bonus to the road score based on the change in 
   * win game ETA for this one road
   *
   * @param posRoad  the possible piece that we're scoring
   * @param roadETA  the eta for the road
   * @param leadersCurrentWGETA  the leaders current WGETA
   * @param playerTrackers  the player trackers (passed in as an argument for figuring out road building plan)
   */
  protected float getWinGameETABonusForRoad(PossibleRoad posRoad, int roadETA, int leadersCurrentWGETA, HashMap playerTrackers) {
    D.ebugPrintln("--- addWinGameETABonusForRoad");
    int ourCurrentWGETA = ourPlayerTracker.getWinGameETA();
    D.ebugPrintln("ourCurrentWGETA = "+ourCurrentWGETA);


    HashMap trackersCopy = null;
    Road tmpRoad1 = null;

    D.ebugPrintln("--- before [start] ---");
    ResourceSet originalResources = ourPlayerData.getResources().copy();
    BuildingSpeedEstimate estimate = new BuildingSpeedEstimate(ourPlayerData.getNumbers());
    //PlayerTracker.playerTrackersDebug(playerTrackers);
    D.ebugPrintln("--- before [end] ---");
    try {
      ResSetBuildTimePair btp = estimate.calculateRollsFast(ourPlayerData.getResources(), Game.ROAD_SET, 50, ourPlayerData.getPortFlags());
      btp.getResources().subtract(Game.ROAD_SET);
      ourPlayerData.getResources().setAmounts(btp.getResources());
    } catch (CutoffExceededException e) {
      D.ebugPrintln("crap in getWinGameETABonusForRoad - "+e);
    }
    tmpRoad1 = new Road(ourPlayerData, posRoad.getCoordinates(), null);
    trackersCopy = PlayerTracker.tryPutPiece(tmpRoad1, game, playerTrackers);
    PlayerTracker.updateWinGameETAs(trackersCopy);
    float score = calcWGETABonus(playerTrackers, trackersCopy);

    if (!posRoad.getThreats().isEmpty()) {
      score *= threatMultiplier;
      D.ebugPrintln("***  (THREAT MULTIPLIER) score * "+threatMultiplier+" = "+score);
    }
    D.ebugPrintln("*** ETA for road = "+roadETA);
    float etaBonus = getETABonus(roadETA, leadersCurrentWGETA, score);
    D.ebugPrintln("$$$ score = "+score);
    D.ebugPrintln("etaBonus = "+etaBonus);
    posRoad.addToScore(etaBonus);

    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("ETA = "+roadETA);
      brain.getDRecorder().record("WGETA Score = "+df1.format(score));
      brain.getDRecorder().record("Total road score = "+df1.format(etaBonus));
    } 
    
    D.ebugPrintln("--- after [end] ---");
    PlayerTracker.undoTryPutPiece(tmpRoad1, game);
    ourPlayerData.getResources().clear();
    ourPlayerData.getResources().add(originalResources);
    D.ebugPrintln("--- cleanup done ---");
		
    return etaBonus;
  }

  /**
   * calc the win game eta bonus
   *
   * @param  trackersBefore   list of player trackers before move
   * @param  trackersAfter    list of player trackers after move
   */
  protected float calcWGETABonus(HashMap trackersBefore, HashMap trackersAfter) {
    D.ebugPrintln("^^^^^ calcWGETABonus");
    int originalWGETAs[] = new int[game.maxPlayers];	 
    int WGETAdiffs[] = new int[game.maxPlayers];	 
    Vector leaders = new Vector();
    int bestWGETA = 1000;
    float bonus = 0;

    Iterator trackersBeforeIter = trackersBefore.values().iterator();
    while (trackersBeforeIter.hasNext()) {
      PlayerTracker trackerBefore = (PlayerTracker)trackersBeforeIter.next();
      D.ebugPrintln("$$$ win game ETA for player "+trackerBefore.getPlayer().getPlayerNumber()+" = "+trackerBefore.getWinGameETA());
      originalWGETAs[trackerBefore.getPlayer().getPlayerNumber()] = trackerBefore.getWinGameETA();
      WGETAdiffs[trackerBefore.getPlayer().getPlayerNumber()] = trackerBefore.getWinGameETA();

      if (trackerBefore.getWinGameETA() < bestWGETA) {
	bestWGETA = trackerBefore.getWinGameETA();
	leaders.removeAllElements();
	leaders.addElement(trackerBefore);
      } else if (trackerBefore.getWinGameETA() == bestWGETA) {
	leaders.addElement(trackerBefore);
      }
    }
		
    D.ebugPrintln("^^^^ bestWGETA = "+bestWGETA);

    bonus = calcWGETABonusAux(originalWGETAs, trackersAfter, leaders);

    D.ebugPrintln("^^^^ final bonus = "+bonus);

    return bonus;
  }

  /**
   * calcWGETABonusAux
   *
   * @param originalWGETAs   the original WGETAs
   * @param trackersAfter    the playerTrackers after the change
   * @param leaders          a list of leaders
   */
  public float calcWGETABonusAux(int[] originalWGETAs, HashMap trackersAfter, 
				 Vector leaders) {
    int WGETAdiffs[] = new int[game.maxPlayers];	
    int bestWGETA = 1000;
    float bonus = 0;
		
    for (int i = 0; i < game.maxPlayers; i++) {
      WGETAdiffs[i] = originalWGETAs[i];
      if (originalWGETAs[i] < bestWGETA) {
	bestWGETA = originalWGETAs[i];
      }
    }
		
    Iterator trackersAfterIter = trackersAfter.values().iterator();
    while (trackersAfterIter.hasNext()) {
      PlayerTracker trackerAfter = (PlayerTracker)trackersAfterIter.next();
      WGETAdiffs[trackerAfter.getPlayer().getPlayerNumber()] -= trackerAfter.getWinGameETA();
      D.ebugPrintln("$$$ win game ETA diff for player "+trackerAfter.getPlayer().getPlayerNumber()+" = "+WGETAdiffs[trackerAfter.getPlayer().getPlayerNumber()]);
      if (trackerAfter.getPlayer().getPlayerNumber() == ourPlayerData.getPlayerNumber()) {
	if (trackerAfter.getWinGameETA() == 0) {
	  D.ebugPrintln("$$$$ adding win game bonus : +"+(100 / game.maxPlayers));
	  bonus += (100.0f / (float) game.maxPlayers);
	  if ((brain != null) && (brain.getDRecorder().isOn())) {
	    brain.getDRecorder().record("Adding Win Game bonus :"+df1.format(bonus));
	  } 
	}
      }
    }

    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("WGETA Diffs: "+WGETAdiffs[0]+" "+WGETAdiffs[1]+" "+WGETAdiffs[2]+" "+WGETAdiffs[3]);
    } 
    
    //
    // bonus is based on lowering your WGETA
    // and increaseing the leaders' WGETA
    //
    if ((originalWGETAs[ourPlayerData.getPlayerNumber()] > 0) &&
	(bonus == 0)) {
      bonus += ((100.0f / (float) game.maxPlayers) * ((float)WGETAdiffs[ourPlayerData.getPlayerNumber()] / (float)originalWGETAs[ourPlayerData.getPlayerNumber()]));
    }			
		
    D.ebugPrintln("^^^^ our current bonus = "+bonus);
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("WGETA bonus for only myself = "+df1.format(bonus));
    } 
		
    //
    //  try adding takedown bonus for all other players
    //  other than the leaders
    //
    for (int pn = 0; pn < game.maxPlayers; pn++) {
      Enumeration leadersEnum = leaders.elements();
      while (leadersEnum.hasMoreElements()) {
	PlayerTracker leader = (PlayerTracker)leadersEnum.nextElement();
	if ((pn != ourPlayerData.getPlayerNumber()) &&
	    (pn != leader.getPlayer().getPlayerNumber())) {
	  if (originalWGETAs[pn] > 0) {
	    float takedownBonus = -1.0f * (100.0f / (float) game.maxPlayers) * adversarialFactor * ((float)WGETAdiffs[pn] / (float)originalWGETAs[pn]) * ((float)bestWGETA / (float)originalWGETAs[pn]);
	    bonus += takedownBonus;
	    D.ebugPrintln("^^^^ added takedown bonus for player "+pn+" : "+takedownBonus);
	    if (((brain != null) && (brain.getDRecorder().isOn())) && (takedownBonus != 0)) {
	      brain.getDRecorder().record("Bonus for AI with "+pn+" : "+df1.format(takedownBonus));
	    } 
	  } else if (WGETAdiffs[pn] < 0) {
	    float takedownBonus = (100.0f / (float) game.maxPlayers) * adversarialFactor;
	    bonus += takedownBonus;
	    D.ebugPrintln("^^^^ added takedown bonus for player "+pn+" : "+takedownBonus);
	    if (((brain != null) && (brain.getDRecorder().isOn())) && (takedownBonus != 0)) {
	      brain.getDRecorder().record("Bonus for AI with "+pn+" : "+df1.format(takedownBonus));
	    } 
	  }
	}
      }
    }
		
    //
    //  take down bonus for leaders
    //
    Enumeration leadersEnum = leaders.elements();
    while (leadersEnum.hasMoreElements()) {
      PlayerTracker leader = (PlayerTracker)leadersEnum.nextElement();
      if (leader.getPlayer().getPlayerNumber() != ourPlayerData.getPlayerNumber()) {
	if (originalWGETAs[leader.getPlayer().getPlayerNumber()] > 0) {
	  float takedownBonus = -1.0f * (100.0f / (float) game.maxPlayers) * leaderAdversarialFactor * ((float)WGETAdiffs[leader.getPlayer().getPlayerNumber()] / (float)originalWGETAs[leader.getPlayer().getPlayerNumber()]);
	  bonus += takedownBonus;
	  D.ebugPrintln("^^^^ added takedown bonus for leader "+leader.getPlayer().getPlayerNumber()+" : +"+takedownBonus);
	  if (((brain != null) && (brain.getDRecorder().isOn())) && (takedownBonus != 0)){
	    brain.getDRecorder().record("Bonus for LI with "+leader.getPlayer().getName()+" : +"+df1.format(takedownBonus));
	  } 
	  
	} else if (WGETAdiffs[leader.getPlayer().getPlayerNumber()] < 0) {
	  float takedownBonus = (100.0f / (float) game.maxPlayers) * leaderAdversarialFactor;
	  bonus += takedownBonus;
	  D.ebugPrintln("^^^^ added takedown bonus for leader "+leader.getPlayer().getPlayerNumber()+" : +"+takedownBonus);
	  if (((brain != null) && (brain.getDRecorder().isOn())) && (takedownBonus != 0)) {
	    brain.getDRecorder().record("Bonus for LI with "+leader.getPlayer().getName()+" : +"+df1.format(takedownBonus));
	  } 
	}
      }
    }
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("WGETA bonus = "+df1.format(bonus));
    } 
    
    return bonus;
  }

	
  /**
   * calc dev card score
   */
  public PossibleCard getDevCardScore(int cardETA, int leadersCurrentWGETA) {
    float devCardScore = 0;
    D.ebugPrintln("$$$ devCardScore = +"+devCardScore);
    D.ebugPrintln("--- before [start] ---");
    // int ourCurrentWGETA = ourPlayerTracker.getWinGameETA();
    int WGETAdiffs[] = new int[game.maxPlayers];
    int originalWGETAs[] = new int[game.maxPlayers];	 
    int bestWGETA = 1000;
    Vector leaders = new Vector();
    Iterator trackersIter = playerTrackers.values().iterator();
    while (trackersIter.hasNext()) {
      PlayerTracker tracker = (PlayerTracker)trackersIter.next();
      originalWGETAs[tracker.getPlayer().getPlayerNumber()] = tracker.getWinGameETA();
      WGETAdiffs[tracker.getPlayer().getPlayerNumber()] = tracker.getWinGameETA();
      D.ebugPrintln("$$$$ win game ETA for player "+tracker.getPlayer().getPlayerNumber()+" = "+tracker.getWinGameETA());

      if (tracker.getWinGameETA() < bestWGETA) {
	bestWGETA = tracker.getWinGameETA();
	leaders.removeAllElements();
	leaders.addElement(tracker);
      } else if (tracker.getWinGameETA() == bestWGETA) {
	leaders.addElement(tracker);
      }
    }

    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("Estimating Knight card value ...");
    } 
    
    ourPlayerData.getGame().saveLargestArmyState();
    D.ebugPrintln("--- before [end] ---");
    ourPlayerData.setNumKnights(ourPlayerData.getNumKnights()+1);
    ourPlayerData.getGame().updateLargestArmy();
    D.ebugPrintln("--- after [start] ---");
    PlayerTracker.updateWinGameETAs(playerTrackers);

    float bonus = calcWGETABonusAux(originalWGETAs, playerTrackers, leaders);
	 
    //
    //  adjust for knight card distribution
    //
    D.ebugPrintln("^^^^ raw bonus = "+bonus);
					
    bonus *= 0.58f;
    D.ebugPrintln("^^^^ adjusted bonus = "+bonus);
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("Bonus * 0.58 = "+df1.format(bonus));
    } 

    D.ebugPrintln("^^^^ bonus for +1 knight = "+bonus);
    devCardScore += bonus;
	 
    D.ebugPrintln("--- after [end] ---");
    ourPlayerData.setNumKnights(ourPlayerData.getNumKnights()-1);
    ourPlayerData.getGame().restoreLargestArmyState();
    D.ebugPrintln("--- cleanup done ---");

    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("Estimating vp card value ...");
    } 
    
    //
    // see what a vp card does to our win game eta
    //
    D.ebugPrintln("--- before [start] ---");
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().suspend();
    }
    PlayerTracker.updateWinGameETAs(playerTrackers);
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().resume();
    }
    D.ebugPrintln("--- before [end] ---");
    ourPlayerData.getDevCards().add(1, DevCardSet.NEW, DevCardConstants.CAP);
    D.ebugPrintln("--- after [start] ---");
    PlayerTracker.updateWinGameETAs(playerTrackers);

    bonus = calcWGETABonusAux(originalWGETAs, playerTrackers, leaders);
		
    D.ebugPrintln("^^^^ our current bonus = "+bonus);

    //
    //  adjust for +1 vp card distribution
    //
    bonus *= 0.21f;
    D.ebugPrintln("^^^^ adjusted bonus = "+bonus);
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("Bonus * 0.21 = "+df1.format(bonus));
    } 
    
    D.ebugPrintln("$$$ win game ETA bonus for +1 vp: "+bonus);
    devCardScore += bonus;
		
    D.ebugPrintln("--- after [end] ---");
    ourPlayerData.getDevCards().subtract(1, DevCardSet.NEW, DevCardConstants.CAP);
    D.ebugPrintln("--- cleanup done ---");

    //
    // add misc bonus
    //
    devCardScore += devCardMultiplier;
    D.ebugPrintln("^^^^ misc bonus = "+devCardMultiplier);
    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("Misc bonus = "+df1.format(devCardMultiplier));
    } 
			
    float score = getETABonus(cardETA, leadersCurrentWGETA, devCardScore);
		
    D.ebugPrintln("$$$$$ devCardScore = "+devCardScore);
    D.ebugPrintln("$$$$$ devCardETA = "+cardETA);
    D.ebugPrintln("$$$$$ final score = "+score);

    if ((brain != null) && (brain.getDRecorder().isOn())) {
      brain.getDRecorder().record("ETA = "+cardETA);
      brain.getDRecorder().record("dev card score = "+df1.format(devCardScore));
      brain.getDRecorder().record("Total dev card score = "+df1.format(score));
    } 
    
    PossibleCard posCard = new PossibleCard(ourPlayerData, cardETA);
    posCard.addToScore(score);

    return posCard;
  }

	
  /**
   * calc eta bonus
   *
   * @param leadWGETA  the wgeta of the leader
   * @param eta  the building eta
   * @return the eta bonus
   */
  public float getETABonus(int eta, int leadWGETA, float bonus) {
    D.ebugPrintln("**** getETABonus ****");
    //return Math.round(etaBonusFactor * ((100f * ((float)(maxGameLength - leadWGETA - eta) / (float)maxGameLength)) * (1.0f - ((float)leadWGETA / (float)maxGameLength))));

    if (D.ebugOn) {
      D.ebugPrintln("etaBonusFactor = "+etaBonusFactor);
      D.ebugPrintln("etaBonusFactor * 100.0 = "+(etaBonusFactor * 100.0f));
      D.ebugPrintln("eta = "+eta);
      D.ebugPrintln("maxETA = "+maxETA);
      D.ebugPrintln("eta / maxETA = "+((float)eta / (float)maxETA));
      D.ebugPrintln("1.0 - ((float)eta / (float)maxETA) = "+(1.0f - ((float)eta / (float)maxETA)));
      D.ebugPrintln("leadWGETA = "+leadWGETA);
      D.ebugPrintln("maxGameLength = "+maxGameLength);
      D.ebugPrintln("1.0 - ((float)leadWGETA / (float)maxGameLength) = "+(1.0f - ((float)leadWGETA / (float)maxGameLength)));
    }
		

    //return etaBonusFactor * 100.0f * ((1.0f - ((float)eta / (float)maxETA)) * (1.0f - ((float)leadWGETA / (float)maxGameLength)));

    return (bonus / (float)Math.pow((1+etaBonusFactor), eta));

    //return (bonus * (float)Math.pow(etaBonusFactor, ((float)(eta*eta*eta)/(float)1000.0)));
  }
 


}		







