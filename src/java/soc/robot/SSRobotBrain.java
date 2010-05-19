/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package soc.robot;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import smartsettlers.boardlayout.GameStateConstants;
import soc.debug.D;

import smartsettlers.player.*;
import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCDevCardSet;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.game.SOCTradeOffer;
import soc.message.*;
import soc.server.SOCServer;
import soc.util.CappedQueue;
import soc.util.SOCRobotParameters;

/**
 *
 * @author szityu
 */
public class SSRobotBrain extends SOCRobotBrain implements GameStateConstants {
    
    

    public SSRobotBrain(SOCRobotClient rc, SOCRobotParameters params, SOCGame ga, CappedQueue mq)
    {
        super(rc,params,ga,mq);
    }
    
    
    /**
     * place planned first settlement
     */
    @Override
    protected void placeFirstSettlement()
    {
        SSRobotClient rc = (SSRobotClient)client;
        rc.sendStateToSmartSettlers(game,S_SETTLEMENT1);
        Player p = rc.bl.player[game.getCurrentPlayerNumber()];
        rc.bl.possibilities.Clear();
        p.listInitSettlementPossibilities(rc.bl.state);
        p.selectAction(rc.bl.state, rc.bl.action);
        p.performAction(rc.bl.state, rc.bl.action);
        System.out.printf("Performing action: [%d %d %d %d %d]", rc.bl.action[0], rc.bl.action[1], rc.bl.action[2], rc.bl.action[3], rc.bl.action[4]);        
        firstSettlement = rc.translateVertexToJSettlers(rc.bl.action[1]);

        System.out.println("BUILD REQUEST FOR SETTLEMENT AT "+Integer.toHexString(firstSettlement));
        pause(2000);
        client.putPiece(game, new SOCSettlement(ourPlayerData, firstSettlement, null));
        pause(1000);
    }

    /**
     * place planned second settlement
     */
    @Override
    protected void placeSecondSettlement()
    {
        SSRobotClient rc = (SSRobotClient)client;
        rc.sendStateToSmartSettlers(game,S_SETTLEMENT2);
        Player p = rc.bl.player[game.getCurrentPlayerNumber()];
        rc.bl.possibilities.Clear();
        p.listInitSettlementPossibilities(rc.bl.state);
        p.selectAction(rc.bl.state, rc.bl.action);
        p.performAction(rc.bl.state, rc.bl.action);
        System.out.printf("Performing action: [%d %d %d %d %d]", rc.bl.action[0], rc.bl.action[1], rc.bl.action[2], rc.bl.action[3], rc.bl.action[4]);        
        secondSettlement = rc.translateVertexToJSettlers(rc.bl.action[1]);
 
        System.out.println("BUILD REQUEST FOR SETTLEMENT AT "+Integer.toHexString(secondSettlement));
        pause(2000);
        client.putPiece(game, new SOCSettlement(ourPlayerData, secondSettlement, null));
        pause(1000);
    }

 
    /**
     * place a road attached to the last initial settlement
     */
    @Override
    public void placeInitRoad()
    {
        SSRobotClient rc = (SSRobotClient)client;
        rc.sendStateToSmartSettlers(game,S_ROAD1); // does not matter if ROAD1 or ROAD2
        Player p = rc.bl.player[game.getCurrentPlayerNumber()];
        rc.bl.possibilities.Clear();
        p.listInitRoadPossibilities(rc.bl.state);
        p.selectAction(rc.bl.state, rc.bl.action);
        p.performAction(rc.bl.state, rc.bl.action);
        System.out.printf("Performing action: [%d %d %d %d %d]", rc.bl.action[0], rc.bl.action[1], rc.bl.action[2], rc.bl.action[3], rc.bl.action[4]);        
        int roadEdge = rc.translateEdgeToJSettlers(rc.bl.action[1]);


        System.out.println("!!! PUTTING INIT ROAD !!!");
        pause(2000);

        System.out.println("Trying to build a road at "+Integer.toHexString(roadEdge));
        client.putPiece(game, new SOCRoad(ourPlayerData, roadEdge, null));
        pause(1000);

        //dummy.destroyPlayer();
    }
    
 
    /**
     * consider an offer made by another player
     *
     * @param offer  the offer to consider
     * @return a code that represents how we want to respond
     * note: a negative result means we do nothing
     */
    @Override
    protected int considerOffer(SOCTradeOffer offer)
    {
        //super.considerOffer(offer); // otherwise the game hangs...
        
        // currently we reject everything!
        int response = SOCRobotNegotiator.REJECT_OFFER;
        return response;
    }
    

    int robberVictim = -1;
    
    @Override
    protected void moveRobber()
    {
        System.out.printf("move robber.\n");        
        pause(1000);
        SSRobotClient rc = (SSRobotClient)client;
        rc.sendStateToSmartSettlers(game,S_ROBBERAT7); 
        System.out.printf("move robber.\n");        
        Player p = rc.bl.player[game.getCurrentPlayerNumber()];
        rc.bl.possibilities.Clear();
        p.listRobberPossibilities(rc.bl.state,A_PLACEROBBER);
        System.out.printf("move robber.\n");        
        p.selectAction(rc.bl.state, rc.bl.action);
        System.out.printf("move robber.\n");        
        p.performAction(rc.bl.state, rc.bl.action);
        System.out.printf("Performing action: [%d %d %d %d %d]", rc.bl.action[0], rc.bl.action[1], rc.bl.action[2], rc.bl.action[3], rc.bl.action[4]);        

        int bestHex = rc.translateHexToJSettlers(rc.bl.action[1]);
        System.out.printf(" besthex: %X\n", bestHex);        
        robberVictim = rc.bl.action[2];
        //robberVictim = -1;
        D.ebugPrintln("!!! MOVING ROBBER !!!");
        client.moveRobber(game, ourPlayerData, bestHex);
        int xn = (int) rc.bl.hextiles[rc.bl.action[1]].pos.x;
        int yn = (int) rc.bl.hextiles[rc.bl.action[1]].pos.y;
        System.out.printf("MOVE robber to hex %x (hex %d, coord: %d,%d), steal from %d \n",
                bestHex,rc.bl.action[1], xn,yn, robberVictim);
        pause(2000);
    }

    @Override
    protected void chooseRobberVictim(boolean[] choices)
    {
        pause(1000);
        client.choosePlayer(game, robberVictim);
        pause(1000);
    }

    public void getActionForPLAY()
    {
        SSRobotClient rc = (SSRobotClient)client;
        rc.sendStateToSmartSettlers(game,S_BEFOREDICE); 
                printCardState();
        Player p = rc.bl.player[game.getCurrentPlayerNumber()];
        rc.bl.possibilities.Clear();
        p.listPossibilities(rc.bl.state);
        p.selectAction(rc.bl.state, rc.bl.action);
        p.performAction(rc.bl.state, rc.bl.action);
        System.out.printf("Performing action: [%d %d %d %d %d]", rc.bl.action[0], rc.bl.action[1], rc.bl.action[2], rc.bl.action[3], rc.bl.action[4]);        
        switch (rc.bl.action[0])
        {
            // !!! TODO: to permit these, have to remember last state (PLAy or PLAY1?)
            // !!! oldState is not enough: for free roads, have to revert to a stae 2 steps back
//            case A_PLAYCARD_MONOPOLY:
//                monopolyChoice = rc.translateResToJSettlers(rc.bl.action[1]);
//
//                expectWAITING_FOR_MONOPOLY = true;
//                waitingForGameState = true;
//                counter = 0;
//                client.playDevCard(game, SOCDevCardConstants.MONO);
//                System.out.println("MONOPOLY played on "+rc.bl.action[1] );
//                break;
//            case A_PLAYCARD_FREERESOURCE:
//
//                int a1 = rc.bl.action[1];
//                int a2 = rc.bl.action[2];
//                int cl = ((a1==RES_CLAY ) ?1:0)  + ((a2==RES_CLAY ) ?1:0);
//                int or = ((a1==RES_STONE) ?1:0)  + ((a2==RES_STONE) ?1:0);
//                int sh = ((a1==RES_SHEEP) ?1:0)  + ((a2==RES_SHEEP) ?1:0);
//                int wh = ((a1==RES_WHEAT) ?1:0)  + ((a2==RES_WHEAT) ?1:0);
//                int wo = ((a1==RES_WOOD ) ?1:0)  + ((a2==RES_WOOD ) ?1:0);
//                resourceChoices = new SOCResourceSet(cl, or, sh, wh, wo, 0);
//                //chooseFreeResources(targetResources);
//                
//                expectWAITING_FOR_DISCOVERY = true;
//                waitingForGameState = true;
//                counter = 0;
//                client.playDevCard(game, SOCDevCardConstants.DISC);
//                System.out.printf("FREE RESOURCE to get %d,%d \n",rc.bl.action[1],rc.bl.action[2] );
//                break;
//            case A_PLAYCARD_FREEROAD:
//                waitingForGameState = true;
//                counter = 0;
//                expectPLACING_FREE_ROAD1 = true;
//
//                System.out.println("!! PLAYING ROAD BUILDING CARD");
//                client.playDevCard(game, SOCDevCardConstants.ROADS);
//                System.out.printf("FREE ROADS played \n");
//                break;
            case A_PLAYCARD_KNIGHT:
                expectPLACING_ROBBER = true;
                waitingForGameState = true;
                counter = 0;
                client.playDevCard(game, SOCDevCardConstants.KNIGHT);
                pause(1500);
                
                break;
            case A_THROWDICE:
                expectDICERESULT = true;
                counter = 0;

                System.out.println("!!! ROLLING DICE !!!");
                client.rollDice(game);
                break;
        }
}

    public void printCardState()
    {
        int i;
        SSRobotClient rc = (SSRobotClient)client;
        int[] st = rc.bl.state;
        int fsmlevel = st[OFS_FSMLEVEL];
        int pl = st[OFS_FSMPLAYER+fsmlevel]; 
        Player p = rc.bl.player[pl];

        String s = String.format("--- Card state according to SmartSettlers:\n"
                + "pl:%d  cardplayed: %d,  new: %d %d %d %d %d,   old: %d %d %d %d %d \n", 
                pl, st[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD],
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_KNIGHT], 
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_FREEROAD],
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_FREERESOURCE],
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_MONOPOLY],
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_ONEPOINT],
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_KNIGHT], 
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREEROAD],
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREERESOURCE],
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_MONOPOLY],
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_ONEPOINT]);
        System.out.print(s);
        int spl = game.getCurrentPlayerNumber();
        SOCPlayer sp = game.getPlayer(spl);
        SOCDevCardSet ds = sp.getDevCards();
        
        s = String.format("--- Card state according to JSettlers:\n"
                + "pl:%d  cardplayed: %d,  new: %d %d %d %d %d,   old: %d %d %d %d %d \n", 
                spl,  sp.hasPlayedDevCard() ?1:0,
                    ds.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.KNIGHT),
                    ds.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.ROADS),
                    ds.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.DISC),
                    ds.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.MONO),
                    0,
                    ds.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.KNIGHT),
                    ds.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.ROADS),
                    ds.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.DISC),
                    ds.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.MONO),
                    ds.getNumVPCards());
        System.out.print(s);
        
    }
    
    public void getActionForPLAY1()    
    {
        SSRobotClient rc = (SSRobotClient)client;
        rc.sendStateToSmartSettlers(game,S_NORMAL); 
                printCardState();
        Player p = rc.bl.player[game.getCurrentPlayerNumber()];
        rc.bl.possibilities.Clear();
        p.listNormalPossibilities(rc.bl.state);
        p.selectAction(rc.bl.state, rc.bl.action);
        p.performAction(rc.bl.state, rc.bl.action);
        
        int coord;
        SOCPossiblePiece targetPiece;
        switch (rc.bl.action[0])
        {
            case A_BUILDROAD:
                coord = rc.translateEdgeToJSettlers(rc.bl.action[1]);
                targetPiece = new SOCPossibleRoad(ourPlayerData, coord, new Vector());
                lastMove = targetPiece;

                waitingForGameState = true;
                counter = 0;
                expectPLACING_ROAD = true;
                whatWeWantToBuild = new SOCRoad(ourPlayerData, targetPiece.getCoordinates(), null);
                D.ebugPrintln("!!! BUILD REQUEST FOR A ROAD AT " + Integer.toHexString(targetPiece.getCoordinates()) + " !!!");
                client.buildRequest(game, SOCPlayingPiece.ROAD);
                System.out.println("ROAD built ");
                break;
            case A_BUILDSETTLEMENT:
                coord = rc.translateVertexToJSettlers(rc.bl.action[1]);
                targetPiece = new SOCPossibleSettlement(ourPlayerData, coord, new Vector());
                lastMove = targetPiece;

                waitingForGameState = true;
                counter = 0;
                expectPLACING_SETTLEMENT = true;
                whatWeWantToBuild = new SOCSettlement(ourPlayerData, targetPiece.getCoordinates(), null);
                D.ebugPrintln("!!! BUILD REQUEST FOR A SETTLEMENT " + Integer.toHexString(targetPiece.getCoordinates()) + " !!!");
                client.buildRequest(game, SOCPlayingPiece.SETTLEMENT);
                System.out.println("SETTLEMENT built ");
                break;
            case A_BUILDCITY:
                coord = rc.translateVertexToJSettlers(rc.bl.action[1]);
                targetPiece = new SOCPossibleCity(ourPlayerData, coord);
                lastMove = targetPiece;

                waitingForGameState = true;
                counter = 0;
                expectPLACING_CITY = true;
                whatWeWantToBuild = new SOCCity(ourPlayerData, targetPiece.getCoordinates(), null);
                D.ebugPrintln("!!! BUILD REQUEST FOR A CITY " + Integer.toHexString(targetPiece.getCoordinates()) + " !!!");
                client.buildRequest(game, SOCPlayingPiece.CITY);
                System.out.println("CITY built ");
               break;
            case A_BUYCARD:
                targetPiece = new SOCPossibleCard(ourPlayerData, 1);
                lastMove = targetPiece;
                
                
                client.buyDevCard(game);
                printCardState();
                waitingForDevCard = true;
                System.out.println("CARD bought ");
                break;
            case A_PLAYCARD_MONOPOLY:
                monopolyChoice = rc.translateResToJSettlers(rc.bl.action[1]);

                expectWAITING_FOR_MONOPOLY = true;
                waitingForGameState = true;
                counter = 0;
                client.playDevCard(game, SOCDevCardConstants.MONO);
                System.out.println("MONOPOLY played on "+rc.bl.action[1] );
                break;
            case A_PLAYCARD_FREERESOURCE:

                int a1 = rc.bl.action[1];
                int a2 = rc.bl.action[2];
                int cl = ((a1==RES_CLAY ) ?1:0)  + ((a2==RES_CLAY ) ?1:0);
                int or = ((a1==RES_STONE) ?1:0)  + ((a2==RES_STONE) ?1:0);
                int sh = ((a1==RES_SHEEP) ?1:0)  + ((a2==RES_SHEEP) ?1:0);
                int wh = ((a1==RES_WHEAT) ?1:0)  + ((a2==RES_WHEAT) ?1:0);
                int wo = ((a1==RES_WOOD ) ?1:0)  + ((a2==RES_WOOD ) ?1:0);
                resourceChoices = new SOCResourceSet(cl, or, sh, wh, wo, 0);
                //chooseFreeResources(targetResources);
                
                expectWAITING_FOR_DISCOVERY = true;
                waitingForGameState = true;
                counter = 0;
                client.playDevCard(game, SOCDevCardConstants.DISC);
                System.out.printf("FREE RESOURCE to get %d,%d \n",rc.bl.action[1],rc.bl.action[2] );
                break;
            case A_PLAYCARD_FREEROAD:
                waitingForGameState = true;
                counter = 0;
                expectPLACING_FREE_ROAD1 = true;

                System.out.println("!! PLAYING ROAD BUILDING CARD");
                client.playDevCard(game, SOCDevCardConstants.ROADS);
                System.out.printf("FREE ROADS played \n");
                break;
            case A_PLAYCARD_KNIGHT:
                expectPLACING_ROBBER = true;
                waitingForGameState = true;
                counter = 0;
                client.playDevCard(game, SOCDevCardConstants.KNIGHT);
                pause(1500);
                
                break;
            case A_PORTTRADE:
                counter = 0;
                waitingForTradeMsg = true;
                boolean[] to = new boolean[SOCGame.MAXPLAYERS];
                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                    to[i] = false;
                SOCResourceSet give = new SOCResourceSet();
                SOCResourceSet get = new SOCResourceSet();
                give.add(rc.bl.action[1], rc.translateResToJSettlers(rc.bl.action[2]));
                get.add(rc.bl.action[3], rc.translateResToJSettlers(rc.bl.action[4]));
                SOCTradeOffer bankTrade = 
                        new SOCTradeOffer(game.getName(), ourPlayerData.getPlayerNumber(), to, give, get);
                
                client.bankTrade(game, bankTrade.getGiveSet(), bankTrade.getGetSet());
                pause(2000);
                break;
            case A_ENDTURN:
                waitingForGameState = true;
                counter = 0;
                expectPLAY = true;
                waitingForOurTurn = true;

                if (robotParameters.getTradeFlag() == 1)
                {
                    doneTrading = false;
                }
                else
                {
                    doneTrading = true;
                }

                System.out.println("!!! ENDING TURN !!!");
                negotiator.resetIsSelling();
                negotiator.resetOffersMade();
                buildingPlan.clear();
                negotiator.resetTargetPieces();
                pause(1500);
                client.endTurn(game);
                break;
            default:
        }

    
    }
    
    
    public void getActionForFREEROAD()
    {
        SSRobotClient rc = (SSRobotClient)client;
        rc.sendStateToSmartSettlers(game,S_FREEROAD1); 
        Player p = rc.bl.player[game.getCurrentPlayerNumber()];
        rc.bl.possibilities.Clear();
        p.listRoadPossibilities(rc.bl.state);
        p.selectAction(rc.bl.state, rc.bl.action);
        p.performAction(rc.bl.state, rc.bl.action);
        System.out.printf("Performing action: [%d %d %d %d %d]", rc.bl.action[0], rc.bl.action[1], rc.bl.action[2], rc.bl.action[3], rc.bl.action[4]);        

        int coord;
        SOCPossiblePiece targetPiece;
        coord = rc.translateEdgeToJSettlers(rc.bl.action[1]);
        targetPiece = new SOCPossibleRoad(ourPlayerData, coord, new Vector());
        whatWeWantToBuild = new SOCRoad(ourPlayerData, targetPiece.getCoordinates(), null);
        D.ebugPrintln("!!! FREE ROAD ROAD AT " + Integer.toHexString(targetPiece.getCoordinates()) + " !!!");
//        client.buildRequest(game, SOCPlayingPiece.ROAD);
        System.out.println("FREE ROAD built ");

        pause(500);
        client.putPiece(game, whatWeWantToBuild);
        pause(1000);
    }
    
    /**
     * Here is the run method.  Just keep receiving game events
     * and deal with each one.
     */
    @Override
    public void run()
    {
        if (pinger != null)
        {
            pinger.start();

            try
            {
                while (alive)
                {
                    SOCMessage mes;

                    //if (!gameEventQ.empty()) {
                    mes = (SOCMessage) gameEventQ.get();

                    //} else {
                    //mes = null;
                    //}
                    int mesType;

                    ((SSRobotClient)client).sendStateToSmartSettlers(game, S_GAME);
                    if (mes != null)
                    {
                        mesType = mes.getType();
                        D.ebugPrintln("mes - " + mes);
                    }
                    else
                    {
                        mesType = -1;
                    }

//                    if (waitingForTradeMsg && (counter > 100))
//                    {
//                        waitingForTradeMsg = false;
//                        counter = 0;
//                    }

                    if (waitingForTradeResponse && (counter > 100))
                    {
                        System.out.println("NOT WAITING ANY MORE FOR TRADE RESPONSE");
                        ///
                        /// record which players said no by not saying anything
                        ///
                        SOCTradeOffer ourCurrentOffer = ourPlayerData.getCurrentOffer();

                        if (ourCurrentOffer != null)
                        {
                            boolean[] offeredTo = ourCurrentOffer.getTo();
                            SOCResourceSet getSet = ourCurrentOffer.getGetSet();

                            for (int rsrcType = SOCResourceConstants.CLAY;
                                    rsrcType <= SOCResourceConstants.WOOD;
                                    rsrcType++)
                            {
                                if (getSet.getAmount(rsrcType) > 0)
                                {
                                    for (int pn = 0; pn < SOCGame.MAXPLAYERS;
                                            pn++)
                                    {
                                        if (offeredTo[pn])
                                        {
                                            negotiator.markAsNotSelling(pn, rsrcType);
                                            negotiator.markAsNotWantingAnotherOffer(pn, rsrcType);
                                        }
                                    }
                                }
                            }

                            pause(1500);
                            client.clearOffer(game);
                            pause(500);
                        }

                        counter = 0;
                        waitingForTradeResponse = false;
                    }

                    if (waitingForGameState && (counter > 10000))
                    {
                        System.out.println("counter = "+counter);
                        System.out.println("RESEND");
                        counter = 0;
                        client.resend();
                    }

                    if (mesType == SOCMessage.GAMESTATE)
                    {
                        waitingForGameState = false;
                        oldGameState = game.getGameState();
                        game.setGameState(((SOCGameState) mes).getState());
                    }

                    else if (mesType == SOCMessage.FIRSTPLAYER)
                    {
                        game.setFirstPlayer(((SOCFirstPlayer) mes).getPlayerNumber());
                    }

                    else if (mesType == SOCMessage.SETTURN)
                    {
                        game.setCurrentPlayerNumber(((SOCSetTurn) mes).getPlayerNumber());
                    }

                    else if (mesType == SOCMessage.TURN)
                    {
                        //
                        // check if this is the first player
                        ///
                        if (game.getFirstPlayer() == -1)
                        {
                            game.setFirstPlayer(((SOCTurn) mes).getPlayerNumber());
                        }

                        game.setCurrentPlayerNumber(((SOCTurn) mes).getPlayerNumber());
                        game.getPlayer(((SOCTurn) mes).getPlayerNumber()).getDevCards().newToOld();

                        //
                        // remove any expected states
                        //
                        expectPLAY = false;
                        expectPLAY1 = false;
                        expectPLACING_ROAD = false;
                        expectPLACING_SETTLEMENT = false;
                        expectPLACING_CITY = false;
                        expectPLACING_ROBBER = false;
                        expectPLACING_FREE_ROAD1 = false;
                        expectPLACING_FREE_ROAD2 = false;
                        expectDICERESULT = false;
                        expectDISCARD = false;
                        expectMOVEROBBER = false;
                        expectWAITING_FOR_DISCOVERY = false;
                        expectWAITING_FOR_MONOPOLY = false;

                        //
                        // reset the selling flags and offers history
                        //
                        if (robotParameters.getTradeFlag() == 1)
                        {
                            doneTrading = false;
                        }
                        else
                        {
                            doneTrading = true;
                        }

                        waitingForTradeMsg = false;
                        waitingForTradeResponse = false;
                        negotiator.resetIsSelling();
                        negotiator.resetOffersMade();

                        //
                        // reset any plans we had
                        //
                        buildingPlan.clear();
                        negotiator.resetTargetPieces();
                    }

                    if (game.getCurrentPlayerNumber() == ourPlayerData.getPlayerNumber())
                    {
                        ourTurn = true;
                    }
                    else
                    {
                        ourTurn = false;
                    }

                    if ((mesType == SOCMessage.TURN) && (ourTurn))
                    {
                        waitingForOurTurn = false;
                    }

                    // <editor-fold defaultstate="collapsed" desc="PLAYERELEMENT">
                    if (mesType == SOCMessage.PLAYERELEMENT)
                    {
                        SOCPlayer pl = game.getPlayer(((SOCPlayerElement) mes).getPlayerNumber());

                        switch (((SOCPlayerElement) mes).getElementType())
                        {
                        case SOCPlayerElement.ROADS:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:
                                pl.setNumPieces(SOCPlayingPiece.ROAD, ((SOCPlayerElement) mes).getValue());

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.setNumPieces(SOCPlayingPiece.ROAD, pl.getNumPieces(SOCPlayingPiece.ROAD) + ((SOCPlayerElement) mes).getValue());

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.setNumPieces(SOCPlayingPiece.ROAD, pl.getNumPieces(SOCPlayingPiece.ROAD) - ((SOCPlayerElement) mes).getValue());

                                break;
                            }

                            break;

                        case SOCPlayerElement.SETTLEMENTS:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:
                                pl.setNumPieces(SOCPlayingPiece.SETTLEMENT, ((SOCPlayerElement) mes).getValue());

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.setNumPieces(SOCPlayingPiece.SETTLEMENT, pl.getNumPieces(SOCPlayingPiece.SETTLEMENT) + ((SOCPlayerElement) mes).getValue());

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.setNumPieces(SOCPlayingPiece.SETTLEMENT, pl.getNumPieces(SOCPlayingPiece.SETTLEMENT) - ((SOCPlayerElement) mes).getValue());

                                break;
                            }

                            break;

                        case SOCPlayerElement.CITIES:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:
                                pl.setNumPieces(SOCPlayingPiece.CITY, ((SOCPlayerElement) mes).getValue());

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.setNumPieces(SOCPlayingPiece.CITY, pl.getNumPieces(SOCPlayingPiece.CITY) + ((SOCPlayerElement) mes).getValue());

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.setNumPieces(SOCPlayingPiece.CITY, pl.getNumPieces(SOCPlayingPiece.CITY) - ((SOCPlayerElement) mes).getValue());

                                break;
                            }

                            break;

                        case SOCPlayerElement.NUMKNIGHTS:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:
                                pl.setNumKnights(((SOCPlayerElement) mes).getValue());

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.setNumKnights(pl.getNumKnights() + ((SOCPlayerElement) mes).getValue());

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.setNumKnights(pl.getNumKnights() - ((SOCPlayerElement) mes).getValue());

                                break;
                            }

                            game.updateLargestArmy();

                            break;

                        case SOCPlayerElement.CLAY:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:

                                if (D.ebugOn)
                                {
                                    if (((SOCPlayerElement) mes).getValue() != ourPlayerData.getResources().getAmount(SOCResourceConstants.CLAY))
                                    {
                                        client.sendText(game, ">>> RSRC ERROR FOR CLAY: " + ((SOCPlayerElement) mes).getValue() + " != " + ourPlayerData.getResources().getAmount(SOCResourceConstants.CLAY));
                                    }
                                }

                                pl.getResources().setAmount(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.CLAY);

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.getResources().add(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.CLAY);

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.getResources().subtract(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.CLAY);

                                break;
                            }

                            break;

                        case SOCPlayerElement.ORE:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:

                                if (D.ebugOn)
                                {
                                    if (((SOCPlayerElement) mes).getValue() != ourPlayerData.getResources().getAmount(SOCResourceConstants.ORE))
                                    {
                                        client.sendText(game, ">>> RSRC ERROR FOR ORE: " + ((SOCPlayerElement) mes).getValue() + " != " + ourPlayerData.getResources().getAmount(SOCResourceConstants.ORE));
                                    }
                                }

                                pl.getResources().setAmount(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.ORE);

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.getResources().add(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.ORE);

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.getResources().subtract(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.ORE);

                                break;
                            }

                            break;

                        case SOCPlayerElement.SHEEP:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:

                                if (D.ebugOn)
                                {
                                    if (((SOCPlayerElement) mes).getValue() != ourPlayerData.getResources().getAmount(SOCResourceConstants.SHEEP))
                                    {
                                        client.sendText(game, ">>> RSRC ERROR FOR SHEEP: " + ((SOCPlayerElement) mes).getValue() + " != " + ourPlayerData.getResources().getAmount(SOCResourceConstants.SHEEP));
                                    }
                                }

                                pl.getResources().setAmount(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.SHEEP);

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.getResources().add(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.SHEEP);

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.getResources().subtract(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.SHEEP);

                                break;
                            }

                            break;

                        case SOCPlayerElement.WHEAT:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:

                                if (D.ebugOn)
                                {
                                    if (((SOCPlayerElement) mes).getValue() != ourPlayerData.getResources().getAmount(SOCResourceConstants.WHEAT))
                                    {
                                        client.sendText(game, ">>> RSRC ERROR FOR WHEAT: " + ((SOCPlayerElement) mes).getValue() + " != " + ourPlayerData.getResources().getAmount(SOCResourceConstants.WHEAT));
                                    }
                                }

                                pl.getResources().setAmount(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.WHEAT);

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.getResources().add(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.WHEAT);

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.getResources().subtract(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.WHEAT);

                                break;
                            }

                            break;

                        case SOCPlayerElement.WOOD:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:

                                if (D.ebugOn)
                                {
                                    if (((SOCPlayerElement) mes).getValue() != ourPlayerData.getResources().getAmount(SOCResourceConstants.WOOD))
                                    {
                                        client.sendText(game, ">>> RSRC ERROR FOR WOOD: " + ((SOCPlayerElement) mes).getValue() + " != " + ourPlayerData.getResources().getAmount(SOCResourceConstants.WOOD));
                                    }
                                }

                                pl.getResources().setAmount(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.WOOD);

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.getResources().add(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.WOOD);

                                break;

                            case SOCPlayerElement.LOSE:
                                pl.getResources().subtract(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.WOOD);

                                break;
                            }

                            break;

                        case SOCPlayerElement.UNKNOWN:

                            switch (((SOCPlayerElement) mes).getAction())
                            {
                            case SOCPlayerElement.SET:

                                /**
                                 * set the ammount of unknown resources
                                 */
                                if (D.ebugOn)
                                {
                                    if (((SOCPlayerElement) mes).getValue() != ourPlayerData.getResources().getAmount(SOCResourceConstants.UNKNOWN))
                                    {
                                        client.sendText(game, ">>> RSRC ERROR FOR UNKNOWN: " + ((SOCPlayerElement) mes).getValue() + " != " + ourPlayerData.getResources().getAmount(SOCResourceConstants.UNKNOWN));
                                    }
                                }

                                pl.getResources().setAmount(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.UNKNOWN);

                                break;

                            case SOCPlayerElement.GAIN:
                                pl.getResources().add(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.UNKNOWN);

                                break;

                            case SOCPlayerElement.LOSE:

                                SOCResourceSet rs = pl.getResources();

                                //
                                // first convert known resources to unknown resources
                                //
                                rs.add(rs.getAmount(SOCResourceConstants.CLAY), SOCResourceConstants.UNKNOWN);
                                rs.setAmount(0, SOCResourceConstants.CLAY);
                                rs.add(rs.getAmount(SOCResourceConstants.ORE), SOCResourceConstants.UNKNOWN);
                                rs.setAmount(0, SOCResourceConstants.ORE);
                                rs.add(rs.getAmount(SOCResourceConstants.SHEEP), SOCResourceConstants.UNKNOWN);
                                rs.setAmount(0, SOCResourceConstants.SHEEP);
                                rs.add(rs.getAmount(SOCResourceConstants.WHEAT), SOCResourceConstants.UNKNOWN);
                                rs.setAmount(0, SOCResourceConstants.WHEAT);
                                rs.add(rs.getAmount(SOCResourceConstants.WOOD), SOCResourceConstants.UNKNOWN);
                                rs.setAmount(0, SOCResourceConstants.WOOD);

                                /**
                                 * then remove the unknown resources
                                 */
                                pl.getResources().subtract(((SOCPlayerElement) mes).getValue(), SOCResourceConstants.UNKNOWN);

                                break;
                            }

                            break;
                        }

                        ///
                        /// if this during the PLAY state, then update the is selling flags
                        ///
                        if (game.getGameState() == SOCGame.PLAY)
                        {
                            negotiator.resetIsSelling();
                        }
                    }
                    // </editor-fold>
                    
                    else if (mesType == SOCMessage.RESOURCECOUNT)
                    {
                        SOCPlayer pl = game.getPlayer(((SOCResourceCount) mes).getPlayerNumber());

                        if (((SOCResourceCount) mes).getCount() != pl.getResources().getTotal())
                        {
                            SOCResourceSet rsrcs = pl.getResources();

                            if (D.ebugOn)
                            {
                                client.sendText(game, ">>> RESOURCE COUNT ERROR FOR PLAYER " + pl.getPlayerNumber() + ": " + ((SOCResourceCount) mes).getCount() + " != " + rsrcs.getTotal());
                            }

                            //
                            //  fix it
                            //
                            if (pl.getPlayerNumber() != ourPlayerData.getPlayerNumber())
                            {
                                rsrcs.clear();
                                rsrcs.setAmount(((SOCResourceCount) mes).getCount(), SOCResourceConstants.UNKNOWN);
                            }
                        }
                    }

                    else if (mesType == SOCMessage.DICERESULT)
                    {
                        game.setCurrentDice(((SOCDiceResult) mes).getResult());
                    }

                    else if (mesType == SOCMessage.PUTPIECE)
                    {
                        D.ebugPrintln("*** PUTPIECE for game ***");

                        SOCPlayer pl = game.getPlayer(((SOCPutPiece) mes).getPlayerNumber());

                        switch (((SOCPutPiece) mes).getPieceType())
                        {
                        case SOCPlayingPiece.ROAD:

                            SOCRoad rd = new SOCRoad(pl, ((SOCPutPiece) mes).getCoordinates(), null);
                            game.putPiece(rd);

                            break;

                        case SOCPlayingPiece.SETTLEMENT:

                            SOCSettlement se = new SOCSettlement(pl, ((SOCPutPiece) mes).getCoordinates(), null);
                            game.putPiece(se);

                            break;

                        case SOCPlayingPiece.CITY:

                            SOCCity ci = new SOCCity(pl, ((SOCPutPiece) mes).getCoordinates(), null);
                            game.putPiece(ci);

                            break;
                        }
                    }

                    else if (mesType == SOCMessage.MOVEROBBER)
                    {
                        //
                        // Note: Don't call ga.moveRobber() because that will call the 
                        // functions to do the stealing.  We just want to say where 
                        // the robber moved without seeing if something was stolen.
                        //
                        moveRobberOnSeven = false;
                        game.getBoard().setRobberHex(((SOCMoveRobber) mes).getCoordinates());
                    }

                    // <editor-fold defaultstate="collapsed" desc="MAKEOFFER">
                    else if ((robotParameters.getTradeFlag() == 1) && (mesType == SOCMessage.MAKEOFFER))
                    {
                        SOCTradeOffer offer = ((SOCMakeOffer) mes).getOffer();
                        game.getPlayer(offer.getFrom()).setCurrentOffer(offer);

                        ///
                        /// if another player makes an offer, that's the
                        /// same as a rejection, but still wants to deal
                        ///				
                        if ((offer.getFrom() != ourPlayerData.getPlayerNumber()))
                        {
                            ///
                            /// record that this player wants to sell me the stuff
                            ///
                            SOCResourceSet giveSet = offer.getGiveSet();

                            for (int rsrcType = SOCResourceConstants.CLAY;
                                    rsrcType <= SOCResourceConstants.WOOD;
                                    rsrcType++)
                            {
                                if (giveSet.getAmount(rsrcType) > 0)
                                {
                                    D.ebugPrintln("%%% player " + offer.getFrom() + " wants to sell " + rsrcType);
                                    negotiator.markAsWantsAnotherOffer(offer.getFrom(), rsrcType);
                                }
                            }

                            ///
                            /// record that this player is not selling the resources 
                            /// he is asking for
                            ///
                            SOCResourceSet getSet = offer.getGetSet();

                            for (int rsrcType = SOCResourceConstants.CLAY;
                                    rsrcType <= SOCResourceConstants.WOOD;
                                    rsrcType++)
                            {
                                if (getSet.getAmount(rsrcType) > 0)
                                {
                                    D.ebugPrintln("%%% player " + offer.getFrom() + " wants to buy " + rsrcType + " and therefore does not want to sell it");
                                    negotiator.markAsNotSelling(offer.getFrom(), rsrcType);
                                }
                            }

                            if (waitingForTradeResponse)
                            {
                                offerRejections[offer.getFrom()] = true;

                                boolean everyoneRejected = true;
                                D.ebugPrintln("ourPlayerData.getCurrentOffer() = " + ourPlayerData.getCurrentOffer());

                                if (ourPlayerData.getCurrentOffer() != null)
                                {
                                    boolean[] offeredTo = ourPlayerData.getCurrentOffer().getTo();

                                    for (int i = 0; i < SOCGame.MAXPLAYERS;
                                            i++)
                                    {
                                        D.ebugPrintln("offerRejections[" + i + "]=" + offerRejections[i]);

                                        if (offeredTo[i] && !offerRejections[i])
                                        {
                                            everyoneRejected = false;
                                        }
                                    }
                                }

                                D.ebugPrintln("everyoneRejected=" + everyoneRejected);

                                if (everyoneRejected)
                                {
                                    negotiator.addToOffersMade(ourPlayerData.getCurrentOffer());
                                    client.clearOffer(game);
                                    waitingForTradeResponse = false;
                                }
                            }

                            ///
                            /// consider the offer
                            ///
                            int ourResponseToOffer = considerOffer(offer);

                            D.ebugPrintln("%%% ourResponseToOffer = " + ourResponseToOffer);

                            if (ourResponseToOffer >= 0)
                            {
                                int delayLength = Math.abs(rand.nextInt() % 500) + 3500;
                                pause(delayLength);

                                switch (ourResponseToOffer)
                                {
                                case SOCRobotNegotiator.ACCEPT_OFFER:
                                    client.acceptOffer(game, offer.getFrom());

                                    ///
                                    /// clear our building plan, so that we replan
                                    ///
                                    buildingPlan.clear();
                                    negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), null);

                                    break;

                                case SOCRobotNegotiator.REJECT_OFFER:

                                    if (!waitingForTradeResponse)
                                    {
                                        client.rejectOffer(game);
                                    }

                                    break;

                                case SOCRobotNegotiator.COUNTER_OFFER:

                                    if (!makeCounterOffer(offer))
                                    {
                                        client.rejectOffer(game);
                                    }

                                    break;
                                }
                            }
                        }
                    }
                    // </editor-fold>
                    
                    else if ((robotParameters.getTradeFlag() == 1) && (mesType == SOCMessage.CLEAROFFER))
                    {
                        game.getPlayer(((SOCClearOffer) mes).getPlayerNumber()).setCurrentOffer(null);
                    }

                    else if ((robotParameters.getTradeFlag() == 1) && (mesType == SOCMessage.ACCEPTOFFER))
                    {
                        if (((((SOCAcceptOffer) mes).getOfferingNumber() == ourPlayerData.getPlayerNumber()) || (((SOCAcceptOffer) mes).getAcceptingNumber() == ourPlayerData.getPlayerNumber())) && waitingForTradeResponse)
                        {
                            waitingForTradeResponse = false;
                        }
                    }

                    // <editor-fold defaultstate="collapsed" desc="REJECTOFFER">
                    else if ((robotParameters.getTradeFlag() == 1) && (mesType == SOCMessage.REJECTOFFER))
                    {
                        ///
                        /// see if everyone has rejected our offer
                        ///
                        int rejector = ((SOCRejectOffer) mes).getPlayerNumber();

                        if ((ourPlayerData.getCurrentOffer() != null) && (waitingForTradeResponse))
                        {
                            D.ebugPrintln("%%%%%%%%% REJECT OFFER %%%%%%%%%%%%%");

                            ///
                            /// record which player said no
                            ///
                            SOCResourceSet getSet = ourPlayerData.getCurrentOffer().getGetSet();

                            for (int rsrcType = SOCResourceConstants.CLAY;
                                    rsrcType <= SOCResourceConstants.WOOD;
                                    rsrcType++)
                            {
                                if ((getSet.getAmount(rsrcType) > 0) && (!negotiator.wantsAnotherOffer(rejector, rsrcType)))
                                {
                                    negotiator.markAsNotSelling(rejector, rsrcType);
                                }
                            }

                            offerRejections[((SOCRejectOffer) mes).getPlayerNumber()] = true;

                            boolean everyoneRejected = true;
                            D.ebugPrintln("ourPlayerData.getCurrentOffer() = " + ourPlayerData.getCurrentOffer());

                            boolean[] offeredTo = ourPlayerData.getCurrentOffer().getTo();

                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                D.ebugPrintln("offerRejections[" + i + "]=" + offerRejections[i]);

                                if (offeredTo[i] && !offerRejections[i])
                                {
                                    everyoneRejected = false;
                                }
                            }

                            D.ebugPrintln("everyoneRejected=" + everyoneRejected);

                            if (everyoneRejected)
                            {
                                negotiator.addToOffersMade(ourPlayerData.getCurrentOffer());
                                client.clearOffer(game);
                                waitingForTradeResponse = false;
                            }
                        }
                        else
                        {
                            ///
                            /// we also want to watch rejections of other players' offers
                            ///
                            D.ebugPrintln("%%%% ALT REJECT OFFER %%%%");

                            for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++)
                            {
                                SOCTradeOffer offer = game.getPlayer(pn).getCurrentOffer();

                                if (offer != null)
                                {
                                    boolean[] offeredTo = offer.getTo();

                                    if (offeredTo[rejector])
                                    {
                                        //
                                        // I think they were rejecting this offer
                                        // mark them as not selling what was asked for
                                        //
                                        SOCResourceSet getSet = offer.getGetSet();

                                        for (int rsrcType = SOCResourceConstants.CLAY;
                                                rsrcType <= SOCResourceConstants.WOOD;
                                                rsrcType++)
                                        {
                                            if ((getSet.getAmount(rsrcType) > 0) && (!negotiator.wantsAnotherOffer(rejector, rsrcType)))
                                            {
                                                negotiator.markAsNotSelling(rejector, rsrcType);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // </editor-fold>
                    
                    else if (mesType == SOCMessage.DEVCARDCOUNT)
                    {
                        game.setNumDevCards(((SOCDevCardCount) mes).getNumDevCards());
                    }

                    else if (mesType == SOCMessage.DEVCARD)
                    {
                        SOCPlayer player = game.getPlayer(((SOCDevCard) mes).getPlayerNumber());

                        switch (((SOCDevCard) mes).getAction())
                        {
                        case SOCDevCard.DRAW:
                            player.getDevCards().add(1, SOCDevCardSet.NEW, ((SOCDevCard) mes).getCardType());

                            break;

                        case SOCDevCard.PLAY:
                            player.getDevCards().subtract(1, SOCDevCardSet.OLD, ((SOCDevCard) mes).getCardType());

                            break;

                        case SOCDevCard.ADDOLD:
                            player.getDevCards().add(1, SOCDevCardSet.OLD, ((SOCDevCard) mes).getCardType());

                            break;

                        case SOCDevCard.ADDNEW:
                            player.getDevCards().add(1, SOCDevCardSet.NEW, ((SOCDevCard) mes).getCardType());

                            break;
                        }
                    }

                    else if (mesType == SOCMessage.SETPLAYEDDEVCARD)
                    {
                        SOCPlayer player = game.getPlayer(((SOCSetPlayedDevCard) mes).getPlayerNumber());
                        player.setPlayedDevCard(((SOCSetPlayedDevCard) mes).hasPlayedDevCard());
                    }

                    else if (mesType == SOCMessage.POTENTIALSETTLEMENTS)
                    {
                        SOCPlayer player = game.getPlayer(((SOCPotentialSettlements) mes).getPlayerNumber());
                        player.setPotentialSettlements(((SOCPotentialSettlements) mes).getPotentialSettlements());
                    }

                    debugInfo();

                    if ((game.getGameState() == SOCGame.PLAY) && (!waitingForGameState))
                    {
                        expectPLAY = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!expectPLAY1 && !expectDISCARD && !expectPLACING_ROBBER && !(expectDICERESULT && (counter < 4000)))
                            {
                                getActionForPLAY();
                            }
                        }
                        else
                        {
                            /**
                             * not our turn
                             */
                            expectDICERESULT = true;
                        }
                    }

                    if ((game.getGameState() == SOCGame.PLACING_ROBBER) && (!waitingForGameState))
                    {
                        expectPLACING_ROBBER = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!((expectPLAY || expectPLAY1) && (counter < 4000)))
                            {
                                if (moveRobberOnSeven == true)
                                {
                                    moveRobberOnSeven = false;
                                    waitingForGameState = true;
                                    counter = 0;
                                    expectPLAY1 = true;
                                }
                                else
                                {
                                    waitingForGameState = true;
                                    counter = 0;

                                    if (oldGameState == SOCGame.PLAY)
                                    {
                                        expectPLAY = true;
                                    }
                                    else if (oldGameState == SOCGame.PLAY1)
                                    {
                                        expectPLAY1 = true;
                                    }
                                }

                                counter = 0;
                                moveRobber();
                            }
                        }
                    }

                    if ((game.getGameState() == SOCGame.WAITING_FOR_DISCOVERY) && (!waitingForGameState))
                    {
                        expectWAITING_FOR_DISCOVERY = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPLAY1) && (counter < 4000))
                            {
                                waitingForGameState = true;
                                expectPLAY1 = true;
                                counter = 0;
                                client.discoveryPick(game, resourceChoices);
                                pause(1500);
                            }
                        }
                    }

                    if ((game.getGameState() == SOCGame.WAITING_FOR_MONOPOLY) && (!waitingForGameState))
                    {
                        expectWAITING_FOR_MONOPOLY = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPLAY1) && (counter < 4000))
                            {
                                waitingForGameState = true;
                                expectPLAY1 = true;
                                counter = 0;
                                client.monopolyPick(game, monopolyChoice); 
                                pause(1500);
                            }
                        }
                    }

                    if (waitingForTradeMsg && (mesType == SOCMessage.GAMETEXTMSG) && (((SOCGameTextMsg) mes).getNickname().equals(SOCServer.SERVERNAME)))
                    {
                        //
                        // This might be the trade message we've been waiting for
                        //
                        if (((SOCGameTextMsg) mes).getText().startsWith(client.getNickname() + " traded"))
                        {
                            waitingForTradeMsg = false;
                        }
                    }

                    if (waitingForDevCard && (mesType == SOCMessage.GAMETEXTMSG) && (((SOCGameTextMsg) mes).getNickname().equals(SOCServer.SERVERNAME)))
                    {
                        //
                        // This might be the dev card message we've been waiting for
                        //
                        if (((SOCGameTextMsg) mes).getText().equals(client.getNickname() + " bought a development card."))
                        {
                            waitingForDevCard = false;
                        }
                    }

                    if ((game.getGameState() == SOCGame.PLAY1) && (!waitingForGameState) && (!waitingForTradeMsg) && (!waitingForTradeResponse) && (!waitingForDevCard) && (!expectPLACING_ROAD) && (!expectPLACING_SETTLEMENT) && (!expectPLACING_CITY) && (!expectPLACING_ROBBER) && (!expectPLACING_FREE_ROAD1) && (!expectPLACING_FREE_ROAD2) && (!expectWAITING_FOR_DISCOVERY) && (!expectWAITING_FOR_MONOPOLY))
                    {
                        expectPLAY1 = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPLAY && (counter < 4000)))
                            {
                                counter = 0;

                                System.out.println("DOING PLAY1");
                                if (D.ebugOn)
                                {
                                    client.sendText(game, "================================");

                                    for (int i = 0; i < SOCGame.MAXPLAYERS;
                                            i++)
                                    {
                                        SOCResourceSet rsrcs = game.getPlayer(i).getResources();
                                        String resourceMessage = "PLAYER " + i + " RESOURCES: ";
                                        resourceMessage += (rsrcs.getAmount(SOCResourceConstants.CLAY) + " ");
                                        resourceMessage += (rsrcs.getAmount(SOCResourceConstants.ORE) + " ");
                                        resourceMessage += (rsrcs.getAmount(SOCResourceConstants.SHEEP) + " ");
                                        resourceMessage += (rsrcs.getAmount(SOCResourceConstants.WHEAT) + " ");
                                        resourceMessage += (rsrcs.getAmount(SOCResourceConstants.WOOD) + " ");
                                        resourceMessage += (rsrcs.getAmount(SOCResourceConstants.UNKNOWN) + " ");
                                        client.sendText(game, resourceMessage);
                                        D.ebugPrintln(resourceMessage);
                                    }
                                }

                                getActionForPLAY1();
                                
                                
//                                if (!expectPLACING_ROBBER && !buildingPlan.empty())
//                                {
//                                    /**
//                                     * check to see if this is a Road Building plan
//                                     */
//
//
//                                    if (!roadBuildingPlan)
//                                    {
//                                        }
//
//                                        if (!expectWAITING_FOR_DISCOVERY)
//                                        {
//
//                                            if (!expectWAITING_FOR_MONOPOLY)
//                                            {
//                                                if ((!doneTrading) && (!ourPlayerData.getResources().contains(targetResources)))
//                                                {
//                                                    waitingForTradeResponse = false;
//
//                                                    if (robotParameters.getTradeFlag() == 1)
//                                                    {
//                                                        makeOffer(targetPiece);
//                                                    }
//                                                }
//
//                                                if (!waitingForTradeResponse)
//                                                {
//                                                    /**
//                                                     * trade with the bank/ports
//                                                     */
//                                                    if (tradeToTarget2(targetResources))
//                                                    {
//                                                        counter = 0;
//                                                        waitingForTradeMsg = true;
//                                                        pause(1500);
//                                                    }
//                                                }
//
//                                            }
//                                        }
//                                    }
//                                }

//                                /**
//                                 * see if we're done with our turn
//                                 */
//                                if (!(expectPLACING_SETTLEMENT || expectPLACING_FREE_ROAD1 || expectPLACING_FREE_ROAD2 || expectPLACING_ROAD || expectPLACING_CITY || expectWAITING_FOR_DISCOVERY || expectWAITING_FOR_MONOPOLY || expectPLACING_ROBBER || waitingForTradeMsg || waitingForTradeResponse || waitingForDevCard))
//                                {
//                                    waitingForGameState = true;
//                                    counter = 0;
//                                    expectPLAY = true;
//                                    waitingForOurTurn = true;
//
//                                    if (robotParameters.getTradeFlag() == 1)
//                                    {
//                                        doneTrading = false;
//                                    }
//                                    else
//                                    {
//                                        doneTrading = true;
//                                    }
//
//                                    System.out.println("!!! ENDING TURN !!!");
//                                    negotiator.resetIsSelling();
//                                    negotiator.resetOffersMade();
//                                    buildingPlan.clear();
//                                    negotiator.resetTargetPieces();
//                                    pause(1500);
//                                    client.endTurn(game);
//                                }
                            }
                        }
                    }

                    if ((game.getGameState() == SOCGame.PLACING_SETTLEMENT) && (!waitingForGameState))
                    {
                        if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_SETTLEMENT))
                        {
                            expectPLACING_SETTLEMENT = false;
                            waitingForGameState = true;
                            counter = 0;
                            expectPLAY1 = true;

                            System.out.println("!!! PUTTING PIECE "+whatWeWantToBuild+" !!!");
                            pause(500);
                            client.putPiece(game, whatWeWantToBuild);
                            pause(1000);
                        }
                    }

                    if ((game.getGameState() == SOCGame.PLACING_ROAD) && (!waitingForGameState))
                    {
                        if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_ROAD))
                        {
                            expectPLACING_ROAD = false;
                            waitingForGameState = true;
                            counter = 0;
                            expectPLAY1 = true;

                            System.out.println("!!! PUTTING PIECE "+whatWeWantToBuild+" !!!");
                            pause(500);
                            client.putPiece(game, whatWeWantToBuild);
                            pause(1000);
                        }
                    }

                    if ((game.getGameState() == SOCGame.PLACING_CITY) && (!waitingForGameState))
                    {
                        if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_CITY))
                        {
                            expectPLACING_CITY = false;
                            waitingForGameState = true;
                            counter = 0;
                            expectPLAY1 = true;

                            System.out.println("!!! PUTTING PIECE "+whatWeWantToBuild+" !!!");
                            pause(500);
                            client.putPiece(game, whatWeWantToBuild);
                            pause(1000);
                        }
                    }

                    if ((game.getGameState() == SOCGame.PLACING_FREE_ROAD1) && (!waitingForGameState))
                    {
                        if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_FREE_ROAD1))
                        {
                            expectPLACING_FREE_ROAD1 = false;
                            waitingForGameState = true;
                            counter = 0;
                            expectPLACING_FREE_ROAD2 = true;

                            getActionForFREEROAD();
                        }
                    }

                    if ((game.getGameState() == SOCGame.PLACING_FREE_ROAD2) && (!waitingForGameState))
                    {
                        if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_FREE_ROAD2))
                        {
                            expectPLACING_FREE_ROAD2 = false;
                            waitingForGameState = true;
                            counter = 0;
                            expectPLAY1 = true;

                            getActionForFREEROAD();

                        }
                    }

                    if ((game.getGameState() == SOCGame.START1A) && (!waitingForGameState))
                    {
                        expectSTART1A = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPUTPIECE_FROM_START1A && (counter < 4000)))
                            {
                                expectPUTPIECE_FROM_START1A = true;
                                counter = 0;
                                waitingForGameState = true;
                                planInitialSettlements();
                                placeFirstSettlement();
                            }
                        }
                    }

                    if ((game.getGameState() == SOCGame.START1B) && (!waitingForGameState))
                    {
                        expectSTART1B = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPUTPIECE_FROM_START1B && (counter < 4000)))
                            {
                                expectPUTPIECE_FROM_START1B = true;
                                counter = 0;
                                waitingForGameState = true;
                                placeInitRoad();
                                pause(1500);
                            }
                        }
                    }

                    if ((game.getGameState() == SOCGame.START2A) && (!waitingForGameState))
                    {
                        expectSTART2A = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPUTPIECE_FROM_START2A && (counter < 4000)))
                            {
                                expectPUTPIECE_FROM_START2A = true;
                                counter = 0;
                                waitingForGameState = true;
                                planSecondSettlement();
                                placeSecondSettlement();
                            }
                        }
                    }

                    if ((game.getGameState() == SOCGame.START2B) && (!waitingForGameState))
                    {
                        expectSTART2B = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPUTPIECE_FROM_START2B && (counter < 4000)))
                            {
                                expectPUTPIECE_FROM_START2B = true;
                                counter = 0;
                                waitingForGameState = true;
                                placeInitRoad();
                                pause(1500);
                           }
                        }
                    }

                    
//                       if (game.getGameState() == SOCGame.OVER) {
//                       client.leaveGame(game);
//                       alive = false;
//                       }
                     
                    if (mesType == SOCMessage.SETTURN)
                    {
                        game.setCurrentPlayerNumber(((SOCSetTurn) mes).getPlayerNumber());
                    }

                    /**
                     * this is for player tracking
                     */
                    if (mesType == SOCMessage.PUTPIECE)
                    {
                        D.ebugPrintln("*** PUTPIECE for playerTrackers ***");

                        switch (((SOCPutPiece) mes).getPieceType())
                        {
                        case SOCPlayingPiece.ROAD:

                            SOCRoad newRoad = new SOCRoad(game.getPlayer(((SOCPutPiece) mes).getPlayerNumber()), ((SOCPutPiece) mes).getCoordinates(), null);
                            Iterator trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();
                                tracker.takeMonitor();

                                try
                                {
                                    tracker.addNewRoad(newRoad, playerTrackers);
                                }
                                catch (Exception e)
                                {
                                    tracker.releaseMonitor();
                                    System.out.println("Exception caught - " + e);
                                    e.printStackTrace();
                                }

                                tracker.releaseMonitor();
                            }

                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();
                                tracker.takeMonitor();

                                try
                                {
                                    Iterator posRoadsIter = tracker.getPossibleRoads().values().iterator();

                                    while (posRoadsIter.hasNext())
                                    {
                                        ((SOCPossibleRoad) posRoadsIter.next()).clearThreats();
                                    }

                                    Iterator posSetsIter = tracker.getPossibleSettlements().values().iterator();

                                    while (posSetsIter.hasNext())
                                    {
                                        ((SOCPossibleSettlement) posSetsIter.next()).clearThreats();
                                    }
                                }
                                catch (Exception e)
                                {
                                    tracker.releaseMonitor();
                                    System.out.println("Exception caught - " + e);
                                    e.printStackTrace();
                                }

                                tracker.releaseMonitor();
                            }

                            ///
                            /// update LR values and ETA
                            ///
                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();
                                tracker.updateThreats(playerTrackers);
                                tracker.takeMonitor();

                                try
                                {
                                    if (tracker.getPlayer().getPlayerNumber() == ((SOCPutPiece) mes).getPlayerNumber())
                                    {
                                        System.out.println("$$ updating LR Value for player "+tracker.getPlayer().getPlayerNumber());
                                        //tracker.updateLRValues();
                                    }

                                    //tracker.recalcLongestRoadETA();
                                }
                                catch (Exception e)
                                {
                                    tracker.releaseMonitor();
                                    System.out.println("Exception caught - " + e);
                                    e.printStackTrace();
                                }

                                tracker.releaseMonitor();
                            }

                            break;

                        case SOCPlayingPiece.SETTLEMENT:

                            SOCSettlement newSettlement = new SOCSettlement(game.getPlayer(((SOCPutPiece) mes).getPlayerNumber()), ((SOCPutPiece) mes).getCoordinates(), null);
                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();
                                tracker.addNewSettlement(newSettlement, playerTrackers);
                            }

                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();
                                Iterator posRoadsIter = tracker.getPossibleRoads().values().iterator();

                                while (posRoadsIter.hasNext())
                                {
                                    ((SOCPossibleRoad) posRoadsIter.next()).clearThreats();
                                }

                                Iterator posSetsIter = tracker.getPossibleSettlements().values().iterator();

                                while (posSetsIter.hasNext())
                                {
                                    ((SOCPossibleSettlement) posSetsIter.next()).clearThreats();
                                }
                            }

                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();
                                tracker.updateThreats(playerTrackers);
                            }

                            ///
                            /// see if this settlement bisected someone elses road
                            ///
                            int[] roadCount = { 0, 0, 0, 0 };
                            Enumeration adjEdgeEnum = ourPlayerData.getGame().getBoard().getAdjacentEdgesToNode(((SOCPutPiece) mes).getCoordinates()).elements();

                            while (adjEdgeEnum.hasMoreElements())
                            {
                                Integer adjEdge = (Integer) adjEdgeEnum.nextElement();
                                Enumeration roadEnum = game.getBoard().getRoads().elements();

                                while (roadEnum.hasMoreElements())
                                {
                                    SOCRoad road = (SOCRoad) roadEnum.nextElement();

                                    if (road.getCoordinates() == adjEdge.intValue())
                                    {
                                        roadCount[road.getPlayer().getPlayerNumber()]++;

                                        if (roadCount[road.getPlayer().getPlayerNumber()] == 2)
                                        {
                                            if (road.getPlayer().getPlayerNumber() != ourPlayerData.getPlayerNumber())
                                            {
                                                ///
                                                /// this settlement bisects another players road
                                                ///
                                                trackersIter = playerTrackers.values().iterator();

                                                while (trackersIter.hasNext())
                                                {
                                                    SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();

                                                    if (tracker.getPlayer().getPlayerNumber() == road.getPlayer().getPlayerNumber())
                                                    {
                                                        System.out.println("$$ updating LR Value for player "+tracker.getPlayer().getPlayerNumber());
                                                        //tracker.updateLRValues();
                                                    }

                                                    //tracker.recalcLongestRoadETA();
                                                }
                                            }

                                            break;
                                        }
                                    }
                                }
                            }

                            ///
                            /// update the speedups from possible settlements
                            ///
                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();

                                if (tracker.getPlayer().getPlayerNumber() == ((SOCPutPiece) mes).getPlayerNumber())
                                {
                                    Iterator posSetsIter = tracker.getPossibleSettlements().values().iterator();

                                    while (posSetsIter.hasNext())
                                    {
                                        ((SOCPossibleSettlement) posSetsIter.next()).updateSpeedup();
                                    }

                                    break;
                                }
                            }

                            ///
                            /// update the speedups from possible cities
                            ///
                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();

                                if (tracker.getPlayer().getPlayerNumber() == ((SOCPutPiece) mes).getPlayerNumber())
                                {
                                    Iterator posCitiesIter = tracker.getPossibleCities().values().iterator();

                                    while (posCitiesIter.hasNext())
                                    {
                                        ((SOCPossibleCity) posCitiesIter.next()).updateSpeedup();
                                    }

                                    break;
                                }
                            }

                            break;

                        case SOCPlayingPiece.CITY:

                            SOCCity newCity = new SOCCity(game.getPlayer(((SOCPutPiece) mes).getPlayerNumber()), ((SOCPutPiece) mes).getCoordinates(), null);
                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();

                                if (tracker.getPlayer().getPlayerNumber() == ((SOCPutPiece) mes).getPlayerNumber())
                                {
                                    tracker.addOurNewCity(newCity);

                                    break;
                                }
                            }

                            ///
                            /// update the speedups from possible settlements
                            ///
                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();

                                if (tracker.getPlayer().getPlayerNumber() == ((SOCPutPiece) mes).getPlayerNumber())
                                {
                                    Iterator posSetsIter = tracker.getPossibleSettlements().values().iterator();

                                    while (posSetsIter.hasNext())
                                    {
                                        ((SOCPossibleSettlement) posSetsIter.next()).updateSpeedup();
                                    }

                                    break;
                                }
                            }

                            ///
                            /// update the speedups from possible cities
                            ///
                            trackersIter = playerTrackers.values().iterator();

                            while (trackersIter.hasNext())
                            {
                                SOCPlayerTracker tracker = (SOCPlayerTracker) trackersIter.next();

                                if (tracker.getPlayer().getPlayerNumber() == ((SOCPutPiece) mes).getPlayerNumber())
                                {
                                    Iterator posCitiesIter = tracker.getPossibleCities().values().iterator();

                                    while (posCitiesIter.hasNext())
                                    {
                                        ((SOCPossibleCity) posCitiesIter.next()).updateSpeedup();
                                    }

                                    break;
                                }
                            }

                            break;
                        }

                        if (D.ebugOn)
                        {
                            SOCPlayerTracker.playerTrackersDebug(playerTrackers);
                        }
                    }

                    if (expectPUTPIECE_FROM_START1A && (mesType == SOCMessage.PUTPIECE) && (((SOCPutPiece) mes).getPlayerNumber() == ourPlayerData.getPlayerNumber()) && (((SOCPutPiece) mes).getPieceType() == SOCPlayingPiece.SETTLEMENT) && (((SOCPutPiece) mes).getCoordinates() == ourPlayerData.getLastSettlementCoord()))
                    {
                        expectPUTPIECE_FROM_START1A = false;
                        expectSTART1B = true;
                    }

                    if (expectPUTPIECE_FROM_START1B && (mesType == SOCMessage.PUTPIECE) && (((SOCPutPiece) mes).getPlayerNumber() == ourPlayerData.getPlayerNumber()) && (((SOCPutPiece) mes).getPieceType() == SOCPlayingPiece.ROAD) && (((SOCPutPiece) mes).getCoordinates() == ourPlayerData.getLastRoadCoord()))
                    {
                        expectPUTPIECE_FROM_START1B = false;
                        expectSTART2A = true;
                    }

                    if (expectPUTPIECE_FROM_START2A && (mesType == SOCMessage.PUTPIECE) && (((SOCPutPiece) mes).getPlayerNumber() == ourPlayerData.getPlayerNumber()) && (((SOCPutPiece) mes).getPieceType() == SOCPlayingPiece.SETTLEMENT) && (((SOCPutPiece) mes).getCoordinates() == ourPlayerData.getLastSettlementCoord()))
                    {
                        expectPUTPIECE_FROM_START2A = false;
                        expectSTART2B = true;
                    }

                    if (expectPUTPIECE_FROM_START2B && (mesType == SOCMessage.PUTPIECE) && (((SOCPutPiece) mes).getPlayerNumber() == ourPlayerData.getPlayerNumber()) && (((SOCPutPiece) mes).getPieceType() == SOCPlayingPiece.ROAD) && (((SOCPutPiece) mes).getCoordinates() == ourPlayerData.getLastRoadCoord()))
                    {
                        expectPUTPIECE_FROM_START2B = false;
                        expectPLAY = true;
                    }

                    if (expectDICERESULT && (mesType == SOCMessage.DICERESULT))
                    {
                        expectDICERESULT = false;

                        if (((SOCDiceResult) mes).getResult() == 7)
                        {
                            moveRobberOnSeven = true;

                            if (ourPlayerData.getResources().getTotal() > 7)
                            {
                                expectDISCARD = true;
                            }
                            else if (ourTurn)
                            {
                                expectPLACING_ROBBER = true;
                            }
                        }
                        else
                        {
                            expectPLAY1 = true;
                        }
                    }

                    if (mesType == SOCMessage.DISCARDREQUEST)
                    {
                        expectDISCARD = false;

                        /**
                         * If we haven't recently discarded...
                         */

                        //	if (!((expectPLACING_ROBBER || expectPLAY1) &&
                        //	      (counter < 4000))) {
                        if ((game.getCurrentDice() == 7) && (ourTurn))
                        {
                            expectPLACING_ROBBER = true;
                        }
                        else
                        {
                            expectPLAY1 = true;
                        }

                        counter = 0;
                        discard(((SOCDiscardRequest) mes).getNumberOfDiscards());

                        //	}
                    }

                    if (mesType == SOCMessage.CHOOSEPLAYERREQUEST)
                    {
                        chooseRobberVictim(((SOCChoosePlayerRequest) mes).getChoices());
                    }

                    if ((mesType == SOCMessage.ROBOTDISMISS) && (!expectDISCARD) && (!expectPLACING_ROBBER))
                    {
                        client.leaveGame(game);
                        alive = false;
                    }

                    if ((mesType == SOCMessage.GAMETEXTMSG) && (((SOCGameTextMsg) mes).getText().equals("*PING*")))
                    {
                        counter++;
                    }

                    if (counter > 1500000)
                    {
                        // We've been waiting too long, commit suicide.
                        System.out.println("Suicide.");
                        client.leaveGame(game);
                        alive = false;
                    }

                    /*
                       if (D.ebugOn) {
                       if (mes != null) {
                       debugInfo();
                       D.ebugPrintln("~~~~~~~~~~~~~~~~");
                       }
                       }
                     */
                    yield();
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("*** Caught an exception - " + e);
                System.out.println("*** Caught an exception - " + e);
                e.printStackTrace();
            }
        }
        else
        {
            System.out.println("AGG! NO PINGER!");
        }

        System.out.println("STOPPING AND DEALLOCATING");
        gameEventQ = null;
        client.addCleanKill();
        client = null;
        game = null;
        ourPlayerData = null;
        whatWeWantToBuild = null;
        resourceChoices = null;
        ourPlayerTracker = null;
        playerTrackers = null;
        pinger.stopPinger();
        pinger = null;
    }

    /**
     * this is for debugging
     */
    @Override
    protected void debugInfo()
    {
     
        if (ourTurn)
        {
//           System.out.println("$===============");
//           System.out.println("gamestate = "+game.getGameState());
//           System.out.println("counter = "+counter);
//           System.out.println("resources = "+ourPlayerData.getResources().getTotal());
           if (expectSTART1A)
           System.out.println("expectSTART1A");
           if (expectSTART1B)
           System.out.println("expectSTART1B");
           if (expectSTART2A)
           System.out.println("expectSTART2A");
           if (expectSTART2B)
           System.out.println("expectSTART2B");
           if (expectPLAY)
           System.out.println("expectPLAY");
           if (expectPLAY1)
           System.out.println("expectPLAY1");
           if (expectPLACING_ROAD)
           System.out.println("expectPLACING_ROAD");
           if (expectPLACING_SETTLEMENT)
           System.out.println("expectPLACING_SETTLEMENT");
           if (expectPLACING_CITY)
           System.out.println("expectPLACING_CITY");
           if (expectPLACING_ROBBER)
           System.out.println("expectPLACING_ROBBER");
           if (expectPLACING_FREE_ROAD1)
           System.out.println("expectPLACING_FREE_ROAD1");
           if (expectPLACING_FREE_ROAD2)
           System.out.println("expectPLACING_FREE_ROAD2");
           if (expectPUTPIECE_FROM_START1A)
           System.out.println("expectPUTPIECE_FROM_START1A");
           if (expectPUTPIECE_FROM_START1B)
           System.out.println("expectPUTPIECE_FROM_START1B");
           if (expectPUTPIECE_FROM_START2A)
           System.out.println("expectPUTPIECE_FROM_START2A");
           if (expectPUTPIECE_FROM_START2B)
           System.out.println("expectPUTPIECE_FROM_START2B");
           if (expectDICERESULT)
           System.out.println("expectDICERESULT");
           if (expectDISCARD)
           System.out.println("expectDISCARD");
           if (expectMOVEROBBER)
           System.out.println("expectMOVEROBBER");
           if (expectWAITING_FOR_DISCOVERY)
           System.out.println("expectWAITING_FOR_DISCOVERY");
           if (waitingForGameState)
           System.out.println("waitingForGameState");
//           if (waitingForOurTurn)
//           System.out.println("waitingForOurTurn");
           if (waitingForTradeMsg)
           System.out.println("waitingForTradeMsg");
           if (waitingForDevCard)
           System.out.println("waitingForDevCard");
           if (moveRobberOnSeven)
           System.out.println("moveRobberOnSeven");
           if (waitingForTradeResponse)
           System.out.println("waitingForTradeResponse");
           if (doneTrading)
           System.out.println("doneTrading");
//           if (ourTurn) 
//           System.out.println("ourTurn");
//           System.out.println("whatWeWantToBuild = "+whatWeWantToBuild);
//           System.out.println("#===============");
        }
    }

    
}
