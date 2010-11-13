/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.robot;


import soc.client.SOCDisplaylessPlayerClient;

import soc.disableDebug.D;

import soc.game.*;

import soc.message.*;

import soc.util.CappedQueue;
import soc.util.CutoffExceededException;
import soc.util.SOCRobotParameters;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;

import java.util.Hashtable;
import java.util.Vector;
import soc.util.Loggers;
import soc.util.ApplicationConstants;

import org.apache.log4j.*;

/**
 * This is a Bot that can play Settlers of Catan.
 *
 * @author Haseeb Saleem
 *
 */

public class InterfaceAgent extends SOCDisplaylessPlayerClient
{
    /**
     * constants for debug recording
     */
    public static final String CURRENT_PLANS = "CURRENT_PLANS";
    public static final String CURRENT_RESOURCES = "RESOURCES";
    /**
     * Name of built-in robot brain class.
     * This robot is the original robot, distributed with the JSettlers server,
     * which permits some optimized communications.
     * Other (3rd-party) robots must use a different class in their IMAROBOT messages.
     * See the class javadoc for more details.
     * @since 1.1.09
     */
    public static final String RBCLASS = "soc.robot.InterfaceAgent";

    /**
     * the thread the reads incomming messages
     */
    private Thread reader;

    /**
     * the current robot parameters for robot brains
     */
    private SOCRobotParameters currentRobotParameters;

    /**
     * the robot's "brains" for diferent games
     */
    private Hashtable robotBrains = new Hashtable();

    /**
     * the message queues for the different brains
     */
    private Hashtable brainQs = new Hashtable();

    /**
     * a table of requests from the server to sit at games
     */
    private Hashtable seatRequests = new Hashtable();

    /**
     * number of games this bot has played
     */
    protected int gamesPlayed;

    /**
     * number of games finished
     */
    protected int gamesFinished;

    /**
     * number of games this bot has won
     */
    protected int gamesWon;

    /**
     * number of clean brain kills
     */
    protected int cleanBrainKills;

    /**
     * start time
     */
    protected long startTime;

    /**
     * used to maintain connection
     */
    SOCRobotResetThread resetThread;

    private SOCGame game;
    private SOCPlayer player;
    public static PlanAgent PA;
    public static MailAgent MA; // accessible by all the agents for message passing
    public static NodeAgent [] NA;
    public static TraderAgent TA;
    public static RobberAgent RA;
    public static CardsAgent CA;
    public static Hashtable hash; // structure for getting NodeAgents
    public static ApplicationConstants appConstants;
    
    private boolean settlementAllowed = false;
    private boolean rollDice = false;
    private boolean moveRobber = false;
    
    private Logger log;
    
    private boolean rejections [] = new boolean[4]; // this array stores the rejections from the other players
    
    public int tradeloop = 0; // this is a loop that makes the turns in strategies choosen 
    
    public Vector currentplan;
    
    public boolean knight_played = false; // this variable tells whether we need to proceed with the move once the knight is played
    	
    public int resource_to_monopolize = 0; // this variable holds the resource to monopolize
    
    public SOCResourceSet discovery_resource_set = null; // this variable holds the resource set which we need as a result of playing the discovery card
    
    public Vector edges_to_build_road = null; // this variable holds the edges to build the road after playing the road building card
    
    /**
     * Constructor for connecting to the specified host, on the specified port
     *
     * @param h  host
     * @param p  port
     * @param nn nickname for robot
     * @param pw password for robot
     */
    public InterfaceAgent(String h, int p, String nn, String pw)
    {
        gamesPlayed = 0;
        gamesFinished = 0;
        gamesWon = 0;
        cleanBrainKills = 0;
        startTime = System.currentTimeMillis();
        host = h;
        port = p;
        nickname = nn;
        password = pw;
	System.out.println("constructor called");
    }

    /**
     * Initialize the robot player
     */
    public void init()
    {
        try
        {
            s = new Socket(host, port);
            s.setSoTimeout(300000);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
            connected = true;
            reader = new Thread(this, "InterfaceAgent-reader");
            reader.start();

            //resetThread = new SOCRobotResetThread(this);
            //resetThread.start();
            put(SOCImARobot.toCmd(nickname, RBCLASS));
            
            
            
            // initialize the PlanAgent, MailAgent, NodeAgents as well
            Loggers.init();
            	
            log = Loggers.InterfaceAgentLogger;
            
            PA = new PlanAgent(this);
            
            PA.startThread();
           
            MA = new MailAgent();
            
            TA = new TraderAgent();
            
            RA = new RobberAgent();
            
            CA = new CardsAgent();
            
            appConstants = new ApplicationConstants();
            
            hash = new Hashtable();
            
            
            //BasicConfigurator.configure();

             int [] nodes = {0x27, 0x38, 0x49, 0x5A, 0x6B, 0x7C, 0x8D,
                    		0x25, 0x36, 0x47, 0x58, 0x69, 0x7A, 0x8B, 0x9C, 0xAD,
                    		0x23, 0x34, 0x45, 0x56, 0x67, 0x78, 0x89, 0x9A, 0xAB, 0xBC, 0xCD,
                    		0x32, 0x43, 0x54, 0x65, 0x76, 0x87, 0x98, 0xA9, 0xBA, 0xCB, 0xDC,
                    		0x52, 0x63, 0x74, 0x85, 0x96, 0xA7, 0xB8, 0xC9, 0xDA,
                    		0x72, 0x83, 0x94, 0xA5, 0xB6, 0xC7, 0xD8
            				};
            
             NA = new NodeAgent[nodes.length];
            
            for(int i = 0; i < nodes.length; i++) {
            	
            	// initialize and run the NodeAgents
            	
            	NA[i] = new NodeAgent(nodes[i]);
            	
            	NA[i].startThread();
            	
            	hash.put(nodes[i], NA[i]); // to retreive based on the node no
            	
            	}      
        	}
        
        catch (Exception e)
        {
            ex = e;
            ex.printStackTrace();
            
            System.err.println("Could not connect to the server: " + ex);
        }
    }

    public static NodeAgent [] getNodeAgents() {
    	
    	return NA;
    	
    }
    
    
    /**
     * disconnect and then try to reconnect
     */
    public void disconnectReconnect()
    {
        D.ebugPrintln("(*)(*)(*)(*)(*)(*)(*) disconnectReconnect()");
        ex = null;

        try
        {
            connected = false;
            s.close();
            s = new Socket(host, port);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
            connected = true;
            reader = new Thread(this, "InterfaceAgent-reader");
            reader.start();

            //resetThread = new SOCRobotResetThread(this);
            //resetThread.start();
            put(SOCImARobot.toCmd(nickname, RBCLASS));
        }
        catch (Exception e)
        {
            ex = e;
            System.err.println("disconnectReconnect error: " + ex);
        }
    }

    /**
     * Treat the incoming messages
     *
     * @param mes    the message
     */
    public void treat(SOCMessage mes)
    {
    	
    	D.ebugPrintln("IN - " + mes);
        
        System.out.println("treat message InterfaceAgent :: "+mes.getType());
        
        try
        {
            switch (mes.getType())
            {
            /**
             * server ping
             */
            case SOCMessage.SERVERPING:
            	System.out.println("SERVER PING");
                handleSERVERPING((SOCServerPing) mes);

                break;
            
            case SOCMessage.ROLLDICEREQUEST:
            	System.out.println("ROLL THE DICE REQUEST");
            	
            case SOCMessage.ROLLDICE:
            	System.out.println("ROLL THE DICE");
            	
            case SOCMessage.ENDTURN:
            	/** should have no effect**/
            	System.out.println("END TURN");
            	handleENDTURN((SOCEndTurn) mes);
               
            /**
             * admin ping
             */
            case SOCMessage.ADMINPING:
            	System.out.println("ADMIN PING");
                handleADMINPING((SOCAdminPing) mes);

                break;

            /**
             * admin reset
             */
            case SOCMessage.ADMINRESET:
            	System.out.println("ADMIN RESET");
                handleADMINRESET((SOCAdminReset) mes);

                break;

            /**
             * update the current robot parameters
             */
            case SOCMessage.UPDATEROBOTPARAMS:
            	System.out.println("UPDATE ROBOT PARAMS");
                handleUPDATEROBOTPARAMS((SOCUpdateRobotParams) mes);

                break;

            /**
             * join game authorization
             */
            case SOCMessage.JOINGAMEAUTH:
            	System.out.println("JOIN GAME AUTH");
                handleJOINGAMEAUTH((SOCJoinGameAuth) mes);

                break;

            /**
             * someone joined a game
             */
            case SOCMessage.JOINGAME:
            	System.out.println("JOIN GAME");
                handleJOINGAME((SOCJoinGame) mes);

                break;

            /**
             * someone left a game
             */
            case SOCMessage.LEAVEGAME:
            	System.out.println("LEAVE GAME");
                handleLEAVEGAME((SOCLeaveGame) mes);

                break;

            /**
             * game has been destroyed
             */
            case SOCMessage.DELETEGAME:
            	System.out.println("DELETE GAME");
                handleDELETEGAME((SOCDeleteGame) mes);

                break;

            /**
             * list of game members
             */
            case SOCMessage.GAMEMEMBERS:
            	System.out.println("GAME MEMBERS");
                handleGAMEMEMBERS((SOCGameMembers) mes);

                break;

            /**
             * game text message
             */
            case SOCMessage.GAMETEXTMSG:
            	System.out.println("GAME TEXT MESSAGE");
                handleGAMETEXTMSG((SOCGameTextMsg) mes);

                break;

            /**
             * someone is sitting down
             */
            case SOCMessage.SITDOWN:
            	System.out.println("SIT DOWN");
            	handleSITDOWN((SOCSitDown) mes);

                break;

            /**
             * receive a board layout
             */
            case SOCMessage.BOARDLAYOUT:
            	System.out.println("BOARD LAYOUT");
                handleBOARDLAYOUT((SOCBoardLayout) mes);

                break;

            /**
             * message that the game is starting
             */
            case SOCMessage.STARTGAME:
            	System.out.println("START GAME");
                handleSTARTGAME((SOCStartGame) mes);

                break;

            /**
             * update the state of the game
             */
            case SOCMessage.GAMESTATE:
            	System.out.println("GAME STATE");
                handleGAMESTATE((SOCGameState) mes);

                break;

            /**
             * set the current turn
             */
            case SOCMessage.SETTURN:
            	System.out.println("SET TURN");
                handleSETTURN((SOCSetTurn) mes);

                break;

            /**
             * set who the first player is
             */
            case SOCMessage.FIRSTPLAYER:
            	System.out.println("FIRST PLAYER");
                handleFIRSTPLAYER((SOCFirstPlayer) mes);

                break;

            /**
             * update who's turn it is
             */
            case SOCMessage.TURN:
            	System.out.println("TURN");
                handleTURN((SOCTurn) mes);

                break;

            /**
             * receive player information
             */
            case SOCMessage.PLAYERELEMENT:
            	System.out.println("PLAYER ELEMENT");
                handlePLAYERELEMENT((SOCPlayerElement) mes);

                break;

            /**
             * receive resource count
             */
            case SOCMessage.RESOURCECOUNT:
            	System.out.println("RESOURCE COUNT");
                handleRESOURCECOUNT((SOCResourceCount) mes);

                break;

            /**
             * the latest dice result
             */
            case SOCMessage.DICERESULT:
            	System.out.println("DICE RESULT");
                handleDICERESULT((SOCDiceResult) mes);

                break;

            /**
             * a player built something
             */
            case SOCMessage.PUTPIECE:
            	System.out.println("PUT PIECE");
                handlePUTPIECE((SOCPutPiece) mes);

                break;

            /**
             * the robber moved
             */
            case SOCMessage.MOVEROBBER:
            	System.out.println("MOVE ROBBER");
                handleMOVEROBBER((SOCMoveRobber) mes);

                break;

            /**
             * the server wants this player to discard
             */
            case SOCMessage.DISCARDREQUEST:
            	System.out.println("DISCARD CARD REQUEST");
                handleDISCARDREQUEST((SOCDiscardRequest) mes);

                break;

            /**
             * the server wants this player to choose a player to rob
             */
            case SOCMessage.CHOOSEPLAYERREQUEST:
            	System.out.println("CHOOSE PLAYER REQUEST");
                handleCHOOSEPLAYERREQUEST((SOCChoosePlayerRequest) mes);

                break;

            /**
             * a player has made an offer
             */
            case SOCMessage.MAKEOFFER:
            	System.out.println("MAKE OFFER");
                handleMAKEOFFER((SOCMakeOffer) mes);

                break;

            /**
             * a player has cleared her offer
             */
            case SOCMessage.CLEAROFFER:
            	System.out.println("CLEAR OFFER");
                handleCLEAROFFER((SOCClearOffer) mes);

                break;

            /**
             * a player has rejected an offer
             */
            case SOCMessage.REJECTOFFER:
            	System.out.println("REJECT OFFER");
                handleREJECTOFFER((SOCRejectOffer) mes);

                break;

            /**
             * a player has accepted an offer
             */
            case SOCMessage.ACCEPTOFFER:
            	System.out.println("ACCEPT OFFER");
            	handleACCEPTOFFER((SOCAcceptOffer) mes);

                break;

            /**
             * the trade message needs to be cleared
             */
            case SOCMessage.CLEARTRADEMSG:
            	System.out.println("CLEAR TRADE MESSAGE");
                handleCLEARTRADEMSG((SOCClearTradeMsg) mes);

                break;

            /**
             * the current number of development cards
             */
            case SOCMessage.DEVCARDCOUNT:
            	System.out.println("DEV CARD COUNT");
            	log.info("CURRENT NUMBER OF DEVELOPMENT CARDS");
                handleDEVCARDCOUNT((SOCDevCardCount) mes);
                
                break;

            /**
             * a dev card action, either draw, play, or add to hand
             */
            case SOCMessage.DEVCARD:
            	System.out.println("DEV CARD");
            	log.info("DEV CARD ACTION (ONE FROM DRAW, PLAY OR ADD TO HAND)");
                handleDEVCARD((SOCDevCard) mes);

                break;

            /**
             * set the flag that tells if a player has played a
             * development card this turn
             */
            case SOCMessage.SETPLAYEDDEVCARD:
            	System.out.println("SET PLAYED DEV CARD");
                handleSETPLAYEDDEVCARD((SOCSetPlayedDevCard) mes);

                break;

            /**
             * get a list of all the potential settlements for a player
             */
            case SOCMessage.POTENTIALSETTLEMENTS:
            	System.out.println("POTENTIAL SETTLEMENTS");
                handlePOTENTIALSETTLEMENTS((SOCPotentialSettlements) mes);

                break;

            /**
             * the server is requesting that we join a game
             */
            case SOCMessage.JOINGAMEREQUEST:
            	System.out.println("JOIN GAME REQUEST");
                handleJOINGAMEREQUEST((SOCJoinGameRequest) mes);

                break;

            /**
             * message that means the server wants us to leave the game
             */
            case SOCMessage.ROBOTDISMISS:
            	System.out.println("ROBOT DISMISS");
                handleROBOTDISMISS((SOCRobotDismiss) mes);

                break;
            }
            
            
            
        }
        catch (Exception e)
        {
            System.out.println("SOCRobotClient treat ERROR - " + e.getMessage());
            e.printStackTrace();
        }
        
    }

    /**
     * handle the server ping message
     * @param mes  the message
     */
    protected void handleSERVERPING(SOCServerPing mes)
    {
    	System.out.println("server pinged");
        /*
           D.ebugPrintln("(*)(*) ServerPing message = "+mes);
           D.ebugPrintln("(*)(*) ServerPing sleepTime = "+mes.getSleepTime());
           D.ebugPrintln("(*)(*) resetThread = "+resetThread);
           resetThread.sleepMore();
         */
    }
    
    protected void handleENDTURN(SOCEndTurn mes) {
    	
    	//put(SOCEndTurn.toCmd(mes.getGame()));
    	
    }
    /**
     * handle the admin ping message
     * @param mes  the message
     */
    protected void handleADMINPING(SOCAdminPing mes)
    {
        D.ebugPrintln("*** Admin Ping message = " + mes);

        SOCGame ga = (SOCGame) games.get(mes.getGame());

        //
        //  if the robot hears a PING and is in the game
        //  where the admin is, then just say "OK".
        //  otherwise, join the game that the admin is in
        //
        //  note: this is a hack because the bot never 
        //        leaves the game and the game must be 
        //        killed by the admin
        //
        if (ga != null)
        {
            sendText(ga, "OK");
        }
        else
        {
            put(SOCJoinGame.toCmd(nickname, password, host, mes.getGame()));
        }
    }

    /**
     * handle the admin reset message
     * @param mes  the message
     */
    protected void handleADMINRESET(SOCAdminReset mes)
    {
        D.ebugPrintln("*** Admin Reset message = " + mes);
        disconnectReconnect();
    }

    /**
     * handle the update robot params message
     * @param mes  the message
     */
    protected void handleUPDATEROBOTPARAMS(SOCUpdateRobotParams mes)
    {
        currentRobotParameters = new SOCRobotParameters(mes.getRobotParameters());
        D.ebugPrintln("*** current robot parameters = " + currentRobotParameters);
    }

    /**
     * handle the "join game request" message
     * @param mes  the message
     */
    protected void handleJOINGAMEREQUEST(SOCJoinGameRequest mes)
    {
        D.ebugPrintln("**** handleJOINGAMEREQUEST ****");
        seatRequests.put(mes.getGame(), new Integer(mes.getPlayerNumber()));

        if (put(SOCJoinGame.toCmd(nickname, password, host, mes.getGame())))
        {
            D.ebugPrintln("**** sent SOCJoinGame ****");
        }
    }

    /**
     * handle the "join game authorization" message
     * @param mes  the message
     */
    protected void handleJOINGAMEAUTH(SOCJoinGameAuth mes)
    {
    	
        gamesPlayed++;
        
        game = new SOCGame(mes.getGame(), true);
        
        //player = new SOCPlayer
        
        put(SOCJoinGame.toCmd(nickname, password, host, mes.getGame()));
        //games.put(mes.getGame(), ga);
        	
        //CappedQueue brainQ = new CappedQueue();
        //brainQs.put(mes.getGame(), brainQ);

        //SOCRobotBrain rb = new SOCRobotBrain(this, currentRobotParameters, ga, brainQ);
        //robotBrains.put(mes.getGame(), rb);
    }

    /**
     * handle the "join game" message
     * @param mes  the message
     */
    protected void handleJOINGAME(SOCJoinGame mes) {}

    /**
     * handle the "leave game" message
     * @param mes  the message
     */
    protected void handleLEAVEGAME(SOCLeaveGame mes) {}

    /**
     * handle the "game members" message
     * @param mes  the message
     */
    protected void handleGAMEMEMBERS(SOCGameMembers mes)
    {
        /**
         * sit down to play
         */
    	
    	System.out.println("I GOT A SEAT");
    	
        Integer pn = (Integer) seatRequests.get(mes.getGame());

        try
        {
            //wait(Math.round(Math.random()*1000));
        }
        catch (Exception e)
        {
            ;
        }

        put(SOCSitDown.toCmd(mes.getGame(), nickname, pn.intValue(), true));
    }

    /**
     * handle the "game text message" message
     * @param mes  the message
     */
    protected void handleGAMETEXTMSG(SOCGameTextMsg mes)
    {
    	
    	System.out.println("GAMETEXTMESSAGE");
    	System.out.println(mes.getText());
    	
    	
    	if(mes.getText().equals("It's " + game.getPlayer(nickname).getName() + "'s turn to roll the dice.")) {
    		
    		rollDice(game);
    		    		
    	} else if(mes.getText().equals("It's " + game.getPlayer(nickname).getName() + "'s turn to build a road.")){
    		
    		SOCPlayer pl = game.getPlayer(nickname);
    		
    		// if its a request to build an initial road, send the request with the 
    		// last settlement coordinate to Plan agent which sends to the appropriate
    		// Node Agent
    		
    		PA.setGame(game);
    		
    		// create a message to send to the PlanAgent
    		
    		Message message = new Message();
    		
    		message.setFrom("InterfaceAgent");
    		message.setTo("PlanAgent");
    		message.setAgentNo(pl.getLastSettlementCoord());
    		message.setMessage("PLACE INITIAL ROAD");
    		
    		MA.mailBox(message);
    		
    		//System.out.println("last settlement on "+pl.getLastSettlementCoord());
    		
//    		if(pl.isPotentialRoad(182)) {
//    			
//        	put(SOCPutPiece.toCmd(mes.getGame(), pl.getPlayerNumber(), SOCPlayingPiece.ROAD, 182));
//        	
//        	game.putPiece(new SOCRoad(pl,182));
//    		
//    		} else {
//    			
//    			
//    			
//    		}
        	
    		
    		
    	} else if(mes.getText().equals("It's " + game.getPlayer(nickname).getName() + "'s turn to build a settlement.") && settlementAllowed) {
    		
    		//System.out.println("IN TEXT MESSAGE BUILD SETTLEMENT");
    		
			Message message = new Message();
			
			message.setFrom("InterfaceAgent");
			message.setTo("PlanAgent");
			message.setMessage("PLACE INITIAL SETTLEMENT");
			
			MA.mailBox(message);
    		    		
    		
        	//put(SOCPutPiece.toCmd(mes.getGame(), pl.getPlayerNumber(), SOCPlayingPiece.SETTLEMENT, 150));
        	
        	//game.putPiece(new SOCSettlement(pl, 150));
    		
    		settlementAllowed = false;
    		
    	} else if(mes.getText().equals(game.getPlayer(nickname).getName() + " will move the robber.")){
    		
    		//moveRobber = true;
    		
    		//System.out.println("MOVE ROBBER CONDITION");
    		
    		// after moving the robber perhaps a message can be send to make the best move
    		
    		//put(SOCMoveRobber.toCmd(game.getName(), game.getCurrentPlayerNumber(), 153));
    		
    		// here we need to make a call to plan agent which would call the robber agent
    		// and the robber agent would move the robber
    		
    		Message message = new Message();
    		
    		message.setFrom("InterfaceAgent");
    		message.setTo("RobberAgent");
    		
    		message.setMessage("MOVE ROBBER");
    		
    		MA.mailBox(message);
    		
    		
    		
    		} else if(mes.getText().startsWith(nickname+" traded ")) {
    		
    		// make a direct call to execute the Plan again if feasible
    			
    		InterfaceAgent.PA.takeAction(currentplan);
    		
    	} else if(mes.getText().equals(nickname+" built a road.")) {
    		
    		log.info("IA built a road in game text message");
    		
    		//this.PA.actionStack(this.PA.plan_left);
    		
    	} else if(mes.getText().startsWith(nickname+" monopolized")) {
    		
    		// now we call the make move 
			Message message = new Message();
			
			message.setFrom("InterfaceAgent");
			message.setTo("PlanAgent");
			message.setMessage("MAKE MOVE");
			
			MA.mailBox(message);
    		
    		
    	} else if(mes.getText().startsWith(nickname+" received")) {
    		
    		// now we call the make move 
			Message message = new Message();
			
			message.setFrom("InterfaceAgent");
			message.setTo("PlanAgent");
			message.setMessage("MAKE MOVE");
			
			MA.mailBox(message);
    		
    		
    	}
    	
    	
    	
        //D.ebugPrintln(mes.getNickname()+": "+mes.getText());
//        if (mes.getText().startsWith(nickname + ":debug-off"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if (brain != null)
//            {
//                brain.turnOffDRecorder();
//                sendText(ga, "Debug mode OFF");
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":debug-on"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if (brain != null)
//            {
//                brain.turnOnDRecorder();
//                sendText(ga, "Debug mode ON");
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":current-plans") || mes.getText().startsWith(nickname + ":cp"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                Vector record = brain.getDRecorder().getRecord(CURRENT_PLANS);
//
//                if (record != null)
//                {
//                    Enumeration planEnum = record.elements();
//
//                    while (planEnum.hasMoreElements())
//                    {
//                        String str = (String) planEnum.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":current-resources") || mes.getText().startsWith(nickname + ":cr"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                Vector record = brain.getDRecorder().getRecord(CURRENT_RESOURCES);
//
//                if (record != null)
//                {
//                    Enumeration resEnum = record.elements();
//
//                    while (resEnum.hasMoreElements())
//                    {
//                        String str = (String) resEnum.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":last-plans") || mes.getText().startsWith(nickname + ":lp"))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                Vector record = brain.getOldDRecorder().getRecord(CURRENT_PLANS);
//
//                if (record != null)
//                {
//                    SOCGame ga = (SOCGame) games.get(mes.getGame());
//                    Enumeration planEnum = record.elements();
//
//                    while (planEnum.hasMoreElements())
//                    {
//                        String str = (String) planEnum.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":last-resources") || mes.getText().startsWith(nickname + ":lr"))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                Vector record = brain.getOldDRecorder().getRecord(CURRENT_RESOURCES);
//
//                if (record != null)
//                {
//                    SOCGame ga = (SOCGame) games.get(mes.getGame());
//                    Enumeration resEnum = record.elements();
//
//                    while (resEnum.hasMoreElements())
//                    {
//                        String str = (String) resEnum.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":last-move") || mes.getText().startsWith(nickname + ":lm"))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getOldDRecorder().isOn()))
//            {
//                SOCPossiblePiece lastMove = brain.getLastMove();
//
//                if (lastMove != null)
//                {
//                    String key = null;
//
//                    switch (lastMove.getType())
//                    {
//                    case SOCPossiblePiece.CARD:
//                        key = "DEVCARD";
//
//                        break;
//
//                    case SOCPossiblePiece.ROAD:
//                        key = "ROAD" + lastMove.getCoordinates();
//
//                        break;
//
//                    case SOCPossiblePiece.SETTLEMENT:
//                        key = "SETTLEMENT" + lastMove.getCoordinates();
//
//                        break;
//
//                    case SOCPossiblePiece.CITY:
//                        key = "CITY" + lastMove.getCoordinates();
//
//                        break;
//                    }
//
//                    Vector record = brain.getOldDRecorder().getRecord(key);
//
//                    if (record != null)
//                    {
//                        SOCGame ga = (SOCGame) games.get(mes.getGame());
//                        Enumeration locEnum = record.elements();
//
//                        while (locEnum.hasMoreElements())
//                        {
//                            String str = (String) locEnum.nextElement();
//                            sendText(ga, str);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":consider-move ") || mes.getText().startsWith(nickname + ":cm "))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getOldDRecorder().isOn()))
//            {
//                String[] tokens = mes.getText().split(" ");
//                String key = null;
//
//                if (tokens[1].trim().equals("card"))
//                {
//                    key = "DEVCARD";
//                }
//                else if (tokens[1].equals("road"))
//                {
//                    key = "ROAD" + tokens[2].trim();
//                }
//                else if (tokens[1].equals("settlement"))
//                {
//                    key = "SETTLEMENT" + tokens[2].trim();
//                }
//                else if (tokens[1].equals("city"))
//                {
//                    key = "CITY" + tokens[2].trim();
//                }
//
//                Vector record = brain.getOldDRecorder().getRecord(key);
//
//                if (record != null)
//                {
//                    SOCGame ga = (SOCGame) games.get(mes.getGame());
//                    Enumeration locEnum = record.elements();
//
//                    while (locEnum.hasMoreElements())
//                    {
//                        String str = (String) locEnum.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":last-target") || mes.getText().startsWith(nickname + ":lt"))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                SOCPossiblePiece lastTarget = brain.getLastTarget();
//
//                if (lastTarget != null)
//                {
//                    String key = null;
//
//                    switch (lastTarget.getType())
//                    {
//                    case SOCPossiblePiece.CARD:
//                        key = "DEVCARD";
//
//                        break;
//
//                    case SOCPossiblePiece.ROAD:
//                        key = "ROAD" + lastTarget.getCoordinates();
//
//                        break;
//
//                    case SOCPossiblePiece.SETTLEMENT:
//                        key = "SETTLEMENT" + lastTarget.getCoordinates();
//
//                        break;
//
//                    case SOCPossiblePiece.CITY:
//                        key = "CITY" + lastTarget.getCoordinates();
//
//                        break;
//                    }
//
//                    Vector record = brain.getDRecorder().getRecord(key);
//
//                    if (record != null)
//                    {
//                        SOCGame ga = (SOCGame) games.get(mes.getGame());
//                        Enumeration locEnum = record.elements();
//
//                        while (locEnum.hasMoreElements())
//                        {
//                            String str = (String) locEnum.nextElement();
//                            sendText(ga, str);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":consider-target ") || mes.getText().startsWith(nickname + ":ct "))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                String[] tokens = mes.getText().split(" ");
//                String key = null;
//
//                if (tokens[1].trim().equals("card"))
//                {
//                    key = "DEVCARD";
//                }
//                else if (tokens[1].equals("road"))
//                {
//                    key = "ROAD" + tokens[2].trim();
//                }
//                else if (tokens[1].equals("settlement"))
//                {
//                    key = "SETTLEMENT" + tokens[2].trim();
//                }
//                else if (tokens[1].equals("city"))
//                {
//                    key = "CITY" + tokens[2].trim();
//                }
//
//                Vector record = brain.getDRecorder().getRecord(key);
//
//                if (record != null)
//                {
//                    SOCGame ga = (SOCGame) games.get(mes.getGame());
//                    Enumeration locEnum = record.elements();
//
//                    while (locEnum.hasMoreElements())
//                    {
//                        String str = (String) locEnum.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":stats"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            sendText(ga, "Games played:" + gamesPlayed);
//            sendText(ga, "Games finished:" + gamesFinished);
//            sendText(ga, "Games won:" + gamesWon);
//            sendText(ga, "Clean brain kills:" + cleanBrainKills);
//            sendText(ga, "Brains running: " + robotBrains.size());
//
//            Runtime rt = Runtime.getRuntime();
//            sendText(ga, "Total Memory:" + rt.totalMemory());
//            sendText(ga, "Free Memory:" + rt.freeMemory());
//        }
//
//        if (mes.getText().startsWith(nickname + ":gc"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            Runtime rt = Runtime.getRuntime();
//            rt.gc();
//            sendText(ga, "Free Memory:" + rt.freeMemory());
//        }
//
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "someone is sitting down" message
     * @param mes  the message
     */
    protected void handleSITDOWN(SOCSitDown mes)
    {
        /**
         * tell the game that a player is sitting
         */
    	System.out.println("SERVER SOMEONE JOINING THE GAME");
    
    	System.out.println("JOINING PLAYER NICKNAME :: "+mes.getNickname());
    	System.out.println("OUR PLAYER NICKNAME :: "+nickname);
    	
    	if(nickname.equals(mes.getNickname()))
    		System.out.println("OUR PLAYER");
    	else
    		System.out.println("NO ITS OPPONENT");
 
    	game.addPlayer(mes.getNickname(), mes.getPlayerNumber());
    	
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            ga.addPlayer(mes.getNickname(), mes.getPlayerNumber());

            /**
             * set the robot flag
             */
            ga.getPlayer(mes.getPlayerNumber()).setRobotFlag(mes.isRobot(), false);

            /**
             * let the robot brain find our player object if we sat down
             */
            if (nickname.equals(mes.getNickname()))
            {
                SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
                brain.setOurPlayerData();
                brain.start();

                /**
                 * change our face to the robot face
                 */
                put(SOCChangeFace.toCmd(ga.getName(), mes.getPlayerNumber(), 0));
            }
        }
    }

    /**
     * handle the "board layout" message
     * @param mes  the message
     */
    protected void handleBOARDLAYOUT(SOCBoardLayout mes)
    {
       
        if (game != null)
        {
            SOCBoard bd = game.getBoard();
            bd.setHexLayout(mes.getHexLayout());
            bd.setNumberLayout(mes.getNumberLayout());
            bd.setRobberHex(mes.getRobberHex());
            
        }
    }

    /**
     * handle the "start game" message
     * @param mes  the message
     */
    protected void handleSTARTGAME(SOCStartGame mes) {}

    /**
     * handle the "delete game" message
     * @param mes  the message
     */
    protected void handleDELETEGAME(SOCDeleteGame mes)
    {
        SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());

        if (brain != null)
        {
            SOCGame ga = (SOCGame) games.get(mes.getGame());

            if (ga != null)
            {
                if (ga.getGameState() == SOCGame.OVER)
                {
                    gamesFinished++;

                    if (ga.getPlayer(nickname).getTotalVP() >= 10)
                    {
                        gamesWon++;
                    }
                }

                brain.kill();
                robotBrains.remove(mes.getGame());
                brainQs.remove(mes.getGame());
                games.remove(mes.getGame());
            }
        }
    }

    /**
     * handle the "game state" message
     * @param mes  the message
     */
    protected void handleGAMESTATE(SOCGameState mes)
    {
    	
    	game.setGameState(mes.getState());
    	
    	System.out.println("PLAYER NUMBER :: "+game.getCurrentPlayerNumber());
    	
    	if(game.getCurrentPlayerNumber() == game.getPlayer(nickname).getPlayerNumber()) {
    		
    		switch(mes.getState()) {
    		
    		case SOCGame.PLAY : 
    			
    			log.info("PLAY YOUR TURN BRO");
    			
    			System.out.println("PLAY YOUR TURN BRO");
    			
    			break;
    		
    			case SOCGame.WAITING_FOR_MONOPOLY : 
    				System.out.println("GO AHEAD AND MONOPOLIZE A RESOURCE"); 
    				put(SOCMonopolyPick.toCmd(game.getName(), this.resource_to_monopolize));
    				
    				this.resource_to_monopolize = 0;
    				
    			break;
    			
    			case SOCGame.WAITING_FOR_DISCOVERY : 
    				
    				System.out.println("DISCOVER 2 RESOURCES");
    				put(SOCDiscoveryPick.toCmd(game.getName(), this.discovery_resource_set));
    				
    				this.discovery_resource_set = null;
    			
    			break;
    			
    			case SOCGame.PLACING_FREE_ROAD1 :
    				
    				System.out.println("place your first free road");
    				
    				if(edges_to_build_road.size() > 0) {
    					
    					int edge = ((Integer)edges_to_build_road.elementAt(0)).intValue();
    					put(SOCPutPiece.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber(), SOCPlayingPiece.ROAD, edge));
    					edges_to_build_road.remove(0);
    					
    				} else {
    					
    					// proceed with the move
    	        		Message msg = new Message();
    	        		
    	        		msg.setFrom("InterfaceAgent");
    	    			msg.setTo("PlanAgent");
    	    			msg.setMessage("MAKE MOVE"); // the complex step of the game
    	    			
    	    			MA.mailBox(msg);
    					    					
    					
    				}
    				
    			break;

    			case SOCGame.PLACING_FREE_ROAD2 :
    				
    				System.out.println("place your second free road");
    				
    				if(edges_to_build_road.size() > 0) {
    					
    					int edge = ((Integer)edges_to_build_road.elementAt(0)).intValue();
    					put(SOCPutPiece.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber(), SOCPlayingPiece.ROAD, edge));
    					edges_to_build_road.remove(0);
    					
    				} else {
    					
    					// proceed with the move
    	        		Message msg = new Message();
    	        		
    	        		msg.setFrom("InterfaceAgent");
    	    			msg.setTo("PlanAgent");
    	    			msg.setMessage("MAKE MOVE"); // the complex step of the game
    	    			
    	    			MA.mailBox(msg);
    					    					
    					
    				}
    				
    			break;
    				
    			
    		
    		}
    		
    	} 
    			
//        SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//        if (ga != null)
//        {
//            CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//            if (brainQ != null)
//            {
//                try
//                {
//                    brainQ.put(mes);
//                }
//                catch (CutoffExceededException exc)
//                {
//                    D.ebugPrintln("CutoffExceededException" + exc);
//                }
//            }
//        }
    }

    /**
     * handle the "set turn" message
     * @param mes  the message
     */
    protected void handleSETTURN(SOCSetTurn mes)
    {
    	
    	game.setCurrentPlayerNumber(((SOCSetTurn) mes).getPlayerNumber());
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "set first player" message
     * @param mes  the message
     */
    protected void handleFIRSTPLAYER(SOCFirstPlayer mes)
    {
    	
    	game.setFirstPlayer(((SOCFirstPlayer) mes).getPlayerNumber());
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    protected void handleTURN(SOCGameTextMsg mes) {
    	
    	SOCPlayer pl = game.getPlayer(mes.getNickname());
    	
    	System.out.println("Player name is :: "+pl.getName());
    	
    	if(pl.getName().equals(nickname)) {
    	
    	Hashtable t = ApplicationConstants.DiceProbabilities.DiceProbabilityTable;
    	
    	System.out.println("PROBABLE 2 IS :: "+(Double)t.get(2));
    	
    	System.out.println("PLAYER NAME :: "+pl.getName());
    	
    	System.out.println("LAST SETTLEMENT COORD :: "+pl.getLastSettlementCoord());
        
    	//SOCBoard board = game.getBoard();
    	
    	Vector adj = game.getBoard().getAdjacentEdgesToNode(0xC7);
    	
    	System.out.println(adj);
    	
    	PA.setGame(game);
    	
    	PA.setPlayer(pl);
    	
    	//PA.handleTurn();
    	
    	}
    	
    }
    
    /**
     * handle the "turn" message
     * @param mes  the message
     */
    protected void handleTURN(SOCTurn mes)
    {
    	
    	int [] hex_layout = game.getBoard().getNumberLayout();
    	
    	for(int i = 0; i < hex_layout.length; i++) {
    		
    		log.info("HEX LAYOUT :: "+hex_layout[i]);
    		
    	}
    	
    	//put(SOCGameTextMsg.toCmd(game.getName(), nickname, "*BOTLIST*"));
    	System.out.println("IN TURN METHOD");
    	
    	System.out.println("GAME STATE IS :: "+game.getGameState());
    	
    	//System.out.println("DICE RESULT :: "+game.getCurrentDice());
//    	if(moveRobber) {
//    		
//    		System.out.println("WE HAVE TO PLACE THE ROBBER");
//    		
//    		moveRobber = false;
//    		
//    	}
    	
    	// just a check //
    	Vector mysettlements = game.getPlayer(nickname).getSettlements();
    	
    	for(int i = 0; i < mysettlements.size(); i++) {
    		
    		SOCSettlement socsettlement = (SOCSettlement)mysettlements.get(i);
    		
    		log.info("SETTLEMENT COORDINATE IS :: "+socsettlement.getCoordinates());
    		
            log.info("ADJACENT HEXES TO SETTLEMENT IS :: "+SOCBoard.getAdjacentHexesToNode(socsettlement.getCoordinates()));
    		
    	}
    	
    	System.out.println("cities :: "+game.getBoard().getCities());
    	
    	System.out.println("settlements :: "+game.getBoard().getSettlements());
 
    	SOCPlayer pl = game.getPlayer(mes.getPlayerNumber());
    	
    	game.getPlayer(((SOCTurn) mes).getPlayerNumber()).getDevCards().newToOld();
    	
    	System.out.println("The nodes touching the roads in play :: "+game.getPlayer(nickname).getRoadNodes());
    	
    	if(game.getPlayer(mes.getPlayerNumber()).getName().equals(nickname)) {
    		
    		log.info("I HAVE "+game.getPlayer(nickname).getNumPieces(SOCPlayingPiece.SETTLEMENT)+" SETTLEMENTS");
    		log.info("I HAVE "+game.getPlayer(nickname).getNumPieces(SOCPlayingPiece.ROAD)+" ROADS");
    		
    		Message message = new Message();
    		
    		System.out.println("MY PLAYERS TURN");
    		
    		PA.setGame(game);
    		
        	PA.setPlayer(pl);
        	
        	PA.setPlayerSettlements(pl.getSettlements());
        	
        	PA.setPlayerCities(pl.getCities());
        	
        	PA.setPlayerResources(pl.getResources());

    	
    	switch(game.getGameState()) {
    	
    		case SOCGame.START1A : System.out.println("FIRST ATTEMPT SETTLEMENT"); settlementAllowed = true; 
    			
    			// we need to build the initial settlement
    			
    			
    			
    			message.setFrom("InterfaceAgent");
    			message.setTo("PlanAgent");
    			message.setMessage("PLACE INITIAL SETTLEMENT");
    			
    			MA.mailBox(message);
    			
    		break;
    		case SOCGame.START2A : System.out.println("SECOND ATTEMPT SETTLEMENT"); 
    	
    		message.setFrom("InterfaceAgent");
			message.setTo("PlanAgent");
			message.setMessage("PLACE INITIAL SETTLEMENT");
			
			MA.mailBox(message);
    			
			settlementAllowed = false;
			
    		break;
    		
    		case SOCGame.START1B: 
    			
        		message = new Message();
        		
        		message.setFrom("InterfaceAgent");
        		message.setTo("PlanAgent");
        		message.setAgentNo(pl.getLastSettlementCoord());
        		message.setMessage("PLACE INITIAL ROAD");
        		
        		MA.mailBox(message);
    			
    			
    			break;
    		case SOCGame.START2B : 
    			
        		message = new Message();
        		
        		message.setFrom("InterfaceAgent");
        		message.setTo("PlanAgent");
        		message.setAgentNo(pl.getLastSettlementCoord());
        		message.setMessage("PLACE INITIAL ROAD");
        		
        		MA.mailBox(message);
    			
        		break;
    		
    		case SOCGame.PLAY : 
    			
    			System.out.println("MAKE YOUR MOVE MAN");
    			
    			log.info("ITS MY TURN TO PLAY THE GAME");
    		
    			// here we now insert a call to Cards Agent which would check
    			// if we can play a card and then when its done we could make the
    			// call to do the normal move
    			
    			// temporarily disable the play card logic
    			
    			message.setFrom("InterfaceAgent");
    			message.setTo("CardsAgent");
    			message.setMessage("PLAY CARD");
    			
    			MA.mailBox(message);
    			
    			// we would move the make move after playing a card
    			
    			/*message.setFrom("InterfaceAgent");
    			message.setTo("PlanAgent");
    			message.setMessage("MAKE MOVE"); // the complex step of the game
    			
    			MA.mailBox(message);*/
    			
    			break;
    		
    		
    		case SOCGame.PLACING_ROBBER : 
    			
    			System.out.println("PLACING ROBBER");
    			
    			break;
    			
    		}
    	 	 
    	}

    	
    	    	
//    	if(pl.getName().equals(nickname)) {
//    		
//    		
//        	
//        	PA.handleTurn();
//    		
//    	}
    	
    	
    	
    		
    	
    	
    	
    	/*CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());

        if (brainQ != null)
        {
            try
            {
                brainQ.put(mes);
            }
            catch (CutoffExceededException exc)
            {
                D.ebugPrintln("CutoffExceededException" + exc);
            }
        } */
    	
    	//System.out.println("LAST COORDINATE FOR SETTLEMENT BY PLAYER :: "+pl.getLastSettlementCoord());
    	
    	if(pl.getName().equals(nickname)) {
    	
    	if(pl.isPotentialSettlement(199)) {
    		System.out.println("199 IS POTENTIAL SETTLEMENT PLACE");
    	
    	//put(SOCPutPiece.toCmd(mes.getGame(), mes.getPlayerNumber(), SOCPlayingPiece.SETTLEMENT, 199));
    	//put(SOCPutPiece.toCmd(mes.getGame(), mes.getPlayerNumber(), SOCPlayingPiece.ROAD, 182));
    	
    	//game.putPiece(new SOCSettlement(pl, 199));
    	//game.putPiece(new SOCRoad(pl,182));
    		
    	} else {
    		System.out.println("199 IS NOT A POTENTIAL SETTLEMENT PLACE");
    		
    	//put(SOCPutPiece.toCmd(mes.getGame(), mes.getPlayerNumber(), SOCPlayingPiece.SETTLEMENT, 150));
    	//put(SOCPutPiece.toCmd(mes.getGame(), mes.getPlayerNumber(), SOCPlayingPiece.ROAD, 150));
    	
    	//game.putPiece(new SOCSettlement(pl, 150));
    	//game.putPiece(new SOCRoad(pl,150));
    		
    	}
    	
    	}
    }

    /**
     * handle the "player element" message
     * @param mes  the message
     */
    protected void handlePLAYERELEMENT(SOCPlayerElement mes)
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
                	
                	log.info(nickname+" LOST "+mes.getValue()+" ROAD");
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
                	log.info(nickname+" LOST "+mes.getValue()+" SETTLEMENT");
                	
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
//            if (game.getGameState() == SOCGame.PLAY)
//            {
//                negotiator.resetIsSelling();
//            }
            	
    	
    	
    }

    /**
     * handle "resource count" message
     * @param mes  the message
     */
    protected void handleRESOURCECOUNT(SOCResourceCount mes)
    {
    	
    	//SOCPlayer player = game.getPlayer(mes.getPlayerNumber());
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "dice result" message
     * @param mes  the message
     */
    protected void handleDICERESULT(SOCDiceResult mes)
    {
    	
    	game.setCurrentDice(((SOCDiceResult) mes).getResult());
    	
    	System.out.println("the dice result is :: "+game.getCurrentDice());
    		
    	if(game.getCurrentDice() == 7) 
    			ApplicationConstants.Game.ROBBER_FREQUENCY++;
    	
    	ApplicationConstants.Game.TOTAL_TURNS++;
    	
    	
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "put piece" message
     * @param mes  the message
     */
    protected void handlePUTPIECE(SOCPutPiece mes)
    {
    	
    	SOCPlayer pl = game.getPlayer(((SOCPutPiece) mes).getPlayerNumber());
    	
    	switch (((SOCPutPiece) mes).getPieceType())
        {
        case SOCPlayingPiece.ROAD:

            SOCRoad rd = new SOCRoad(pl, ((SOCPutPiece) mes).getCoordinates(), null);
            game.putPiece(rd);
            	
            pl.updatePotentials(new SOCRoad(pl, mes.getCoordinates(), null));
            
            if(pl.getName().equals(nickname))
            	PA.setPlayer(pl);
            
            break;

        case SOCPlayingPiece.SETTLEMENT:
        	
        	
            SOCSettlement se = new SOCSettlement(pl, ((SOCPutPiece) mes).getCoordinates(), null);
            //System.out.println("put piece :: settlement1");
            game.putPiece(se);
            //System.out.println("put piece :: settlement2");

            pl.updatePotentials(new SOCSettlement(pl, mes.getCoordinates(), null));

            if(pl.getName().equals(nickname))
            	PA.setPlayer(pl);
            
            break;

        case SOCPlayingPiece.CITY:

            SOCCity ci = new SOCCity(pl, ((SOCPutPiece) mes).getCoordinates(), null);
            game.putPiece(ci);
            
            pl.updatePotentials(new SOCCity(pl, mes.getCoordinates(), null));
           
            if(pl.getName().equals(nickname))
            	PA.setPlayer(pl);
            
            break;
        	
        	}
    
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//            if (ga != null)
//            {
//                SOCPlayer pl = ga.getPlayer(mes.getPlayerNumber());
//            }
//        }
    }

    /**
     * handle the "move robber" message
     * @param mes  the message
     */
    protected void handleMOVEROBBER(SOCMoveRobber mes)
    {
    	//game.getBoard().setRobberHex(((SOCMoveRobber) mes).getCoordinates());
    	if(mes.getPlayerNumber() == game.getPlayer(nickname).getPlayerNumber()) {
    		
    		// this is a call back from server which tells that the robber was moved
    		
    		// we check the knight_played variable
    		// if it is true then we call the make move 
    		
    		if(knight_played) {
    			
    			knight_played = false;
    			
        		Message msg = new Message();
        		
        		msg.setFrom("InterfaceAgent");
    			msg.setTo("PlanAgent");
    			msg.setMessage("MAKE MOVE"); // the complex step of the game
    			
    			MA.mailBox(msg);
    			
    		}
    		
//    		Message message = new Message();
//    		
//    		message.setFrom("InterfaceAgent");
//    		message.setTo("RobberAgent");
//    		
//    		message.setMessage("MOVE ROBBER");
//    		
//    		MA.mailBox(message);
    		
    		
    		
    		// it means we moved the robber now go with the original plan 
//    		Message msg = new Message();
//    		
//    		msg.setFrom("InterfaceAgent");
//			msg.setTo("PlanAgent");
//			msg.setMessage("MAKE MOVE"); // the complex step of the game
//			
//			MA.mailBox(msg);
    		
    	}
    	
    	// its here that a message could be issued to plan the move
    	
    	
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "discard request" message
     * @param mes  the message
     */
    protected void handleDISCARDREQUEST(SOCDiscardRequest mes)
    {
    
    	System.out.println("DISCARD CARDS LOGIC?");
    	
    	log.info("DISCARD "+mes.getNumberOfDiscards()+" CARDS");
    	
    	//call to CardsAgent to discard the resource cards
    	
    	Message message = new Message();
    	
    	message.setFrom("InterfaceAgent");
    	
    	message.setTo("CardsAgent");
    	
    	CardsMessage cmessage = new CardsMessage();
    	
    	cmessage.setResponseHeader("DISCARD CARDS");
    	
    	cmessage.setDiscardCards(mes.getNumberOfDiscards());
    	
    	message.setMessage(cmessage);
    	
    	InterfaceAgent.MA.mailBox(message); // ask the cards agent to discard the cards
    	
    	
    	//discard(((SOCDiscardRequest) mes).getNumberOfDiscards());
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "choose player request" message
     * @param mes  the message
     */
    protected void handleCHOOSEPLAYERREQUEST(SOCChoosePlayerRequest mes)
    {
    	
    	System.out.println("CHOOSE PLAYER TO STEAL FROM");
    	
    	Message message = new Message();
    	
    	message.setFrom("InterfaceAgent");
    	message.setTo("RobberAgent");
    	
    	RobberMessage rmessage = new RobberMessage();
    	
    	rmessage.setRequestHeader("STEAL FROM PLAYER");
    	rmessage.setStealPlayerChoices(mes.getChoices());
    	
    	message.setMessage(rmessage);
    	
    	MA.mailBox(message);
    	
    	//chooseRobberVictim(((SOCChoosePlayerRequest) mes).getChoices());
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            { 
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "make offer" message
     * @param mes  the message
     */
    protected void handleMAKEOFFER(SOCMakeOffer mes)
    {
    	
    	// we check which resource is offered and which is being given 
    	
    	// get the resourceset given (share value down)
    	SOCResourceSet given_resource_set = mes.getOffer().getGiveSet();
    	
    	// get the resourceset wanted (share value up)
    	
    	SOCResourceSet asked_resource_set = mes.getOffer().getGetSet();
    	
    	// clay, ore, sheep, wheat, wood
    	
    	int clay = 0;
    	int ore = 0;
    	int sheep = 0;
    	int wheat = 0;
    	int wood = 0;
    	
    	clay += (given_resource_set.getAmount(SOCResourceConstants.CLAY) * ApplicationConstants.Game.SHARE_VALUE_CLAY_DOWN);    
    	ore += (given_resource_set.getAmount(SOCResourceConstants.ORE) * ApplicationConstants.Game.SHARE_VALUE_ORE_DOWN);
    	sheep += (given_resource_set.getAmount(SOCResourceConstants.SHEEP) * ApplicationConstants.Game.SHARE_VALUE_SHEEP_DOWN);
    	wheat += (given_resource_set.getAmount(SOCResourceConstants.WHEAT) * ApplicationConstants.Game.SHARE_VALUE_WHEAT_DOWN);
    	wood += (given_resource_set.getAmount(SOCResourceConstants.WOOD) * ApplicationConstants.Game.SHARE_VALUE_WHEAT_DOWN);
    	
    	clay += (asked_resource_set.getAmount(SOCResourceConstants.CLAY) * ApplicationConstants.Game.SHARE_VALUE_CLAY_UP);    
    	ore += (asked_resource_set.getAmount(SOCResourceConstants.ORE) * ApplicationConstants.Game.SHARE_VALUE_ORE_UP);
    	sheep += (asked_resource_set.getAmount(SOCResourceConstants.SHEEP) * ApplicationConstants.Game.SHARE_VALUE_SHEEP_UP);
    	wheat += (asked_resource_set.getAmount(SOCResourceConstants.WHEAT) * ApplicationConstants.Game.SHARE_VALUE_WHEAT_UP);
    	wood += (asked_resource_set.getAmount(SOCResourceConstants.WOOD) * ApplicationConstants.Game.SHARE_VALUE_WOOD_UP);
    	
    	// check if the resource key already exists in the hashtable
    	// if it exists, then remove and insert it again
    	// if it doesnt exist, then you can add it into the hashtable 
    	// which is the first time
    	
    	int total_clay = clay;
    	int total_ore = ore; 
    	int total_sheep = sheep;
    	int total_wheat = wheat;
    	int total_wood = wood;
    	
       	if(null != ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.CLAY))) {
       		total_clay += ((Integer)ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.CLAY))).intValue();
    		ApplicationConstants.Game.SHARE_VALUES.remove(new Integer(SOCResourceConstants.CLAY));
       	}
    	
    	ApplicationConstants.Game.SHARE_VALUES.put(new Integer(SOCResourceConstants.CLAY), total_clay);
    	
    	if(null != ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.ORE))) {
    		total_ore += ((Integer)ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.ORE))).intValue();
			ApplicationConstants.Game.SHARE_VALUES.remove(new Integer(SOCResourceConstants.ORE));
    	}
    	
    	ApplicationConstants.Game.SHARE_VALUES.put(new Integer(SOCResourceConstants.ORE), total_ore);
    	
    	if(null != ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.SHEEP))) {
    		total_sheep += ((Integer)ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.SHEEP))).intValue();
			ApplicationConstants.Game.SHARE_VALUES.remove(new Integer(SOCResourceConstants.SHEEP));
    	}
    	
    	ApplicationConstants.Game.SHARE_VALUES.put(new Integer(SOCResourceConstants.SHEEP), total_sheep);
    	
    	if(null != ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.WHEAT))) {
    		total_wheat += ((Integer)ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.WHEAT))).intValue();
			ApplicationConstants.Game.SHARE_VALUES.remove(new Integer(SOCResourceConstants.WHEAT));
    	}
    	
    	ApplicationConstants.Game.SHARE_VALUES.put(new Integer(SOCResourceConstants.WHEAT), total_wheat);
    	
    	if(null != ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.WOOD))) {
    		total_wood += ((Integer)ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.WOOD))).intValue();
			ApplicationConstants.Game.SHARE_VALUES.remove(new Integer(SOCResourceConstants.WOOD));
    	}
    	
    	ApplicationConstants.Game.SHARE_VALUES.put(new Integer(SOCResourceConstants.WOOD), total_wood);
    	
    	// now we have all the values in the hashtable
    	
    	log.info("SHARE VALUE CLAY :: "+ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.CLAY)));
    	log.info("SHARE VALUE ORE :: "+ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.ORE)));
    	log.info("SHARE VALUE SHEEP :: "+ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.SHEEP)));
    	log.info("SHARE VALUE WHEAT :: "+ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.WHEAT)));
    	log.info("SHARE VALUE WOOD :: "+ApplicationConstants.Game.SHARE_VALUES.get(new Integer(SOCResourceConstants.WOOD)));
    	
    	
    	
    	if(mes.getOffer().getFrom() != game.getPlayer(nickname).getPlayerNumber())
    		put(SOCRejectOffer.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber()));
    	
    }

    /**
     * handle the "clear offer" message
     * @param mes  the message
     */
    protected void handleCLEAROFFER(SOCClearOffer mes)
    {
        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());

        if (brainQ != null)
        {
            try
            {
                brainQ.put(mes);
            }
            catch (CutoffExceededException exc)
            {
                D.ebugPrintln("CutoffExceededException" + exc);
            }
        }
    }

    /**
     * handle the "reject offer" message
     * @param mes  the message
     */
    protected void handleREJECTOFFER(SOCRejectOffer mes)
    {
    	
    	log.info(mes.getPlayerNumber()+" HAS REJECTED THE OFFER");
    	
    	rejections[mes.getPlayerNumber()] = true;
    	
    	// check if everyone has rejected the offer and clearoffer
    	
    	boolean notAll = false;
    	
    	for(int i = 0; i < 4; i++) {
    		
    		if(i != game.getPlayer(nickname).getPlayerNumber() && !rejections[i]) {
    			notAll = true;
    			break;
    		}
    			
    	}
    	
    	if(!notAll) {
    		
    		// clear the rejections array
    		
    		for(int i=0; i < rejections.length; )
    				rejections[i++] = false;
    		
    		put(SOCClearOffer.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber()));
    		
    		// a message also goes to the trader agent via the plan agent to devise a new strategy
    		
    		Message message = new Message();
    		
    		message.setFrom("InterfaceAgent");
    		message.setTo("TraderAgent");
    		    		
    		TraderMessage tmessage = new TraderMessage();
    		
    		tmessage.setResponseHeader("TRADE OFFER REJECTED");
    		
    		tmessage.setTradeLoop(++tradeloop);
    		
    		log.info("IA SENDS THE TRADE LOOP :: "+tradeloop);
    		
    		message.setMessage(tmessage);
    		
    		//InterfaceAgent.MA.mailBox(message);
    		
    		
    	}
    	
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "accept offer" message
     * @param mes  the message
     */
    protected void handleACCEPTOFFER(SOCAcceptOffer mes)
    {
    	SOCPlayer pl = new SOCPlayer(mes.getAcceptingNumber(), game);
    
		log.info(pl.getName()+" HAS ACCEPTED THE OFFER");
		
		this.tradeloop = 0; // just for assurance
		
		// execute the plan which is the current one
		
		InterfaceAgent.PA.takeAction(currentplan);
		
		
		
		
		
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "clear trade" message
     * @param mes  the message
     */
    protected void handleCLEARTRADEMSG(SOCClearTradeMsg mes) {
    	
    	log.info("CLEAR TRADE MESSAGE");
    
    	//put(SOCClearOffer.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber()));
    	
    }

    /**
     * handle the "development card count" message
     * @param mes  the message
     */
    protected void handleDEVCARDCOUNT(SOCDevCardCount mes)
    {
    	
    	game.setNumDevCards(((SOCDevCardCount) mes).getNumDevCards());
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "development card action" message
     * @param mes  the message
     */
    protected void handleDEVCARD(SOCDevCard mes)
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
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "set played development card" message
     * @param mes  the message
     */
    protected void handleSETPLAYEDDEVCARD(SOCSetPlayedDevCard mes)
    {
    	
    	if(mes.getPlayerNumber() == game.getPlayer(nickname).getPlayerNumber()) {
    	
    		SOCPlayer player = game.getPlayer(((SOCSetPlayedDevCard) mes).getPlayerNumber());
    		player.setPlayedDevCard(((SOCSetPlayedDevCard) mes).hasPlayedDevCard());

    	}
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "dismiss robot" message
     * @param mes  the message
     */
    protected void handleROBOTDISMISS(SOCRobotDismiss mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());
        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());

        if ((ga != null) && (brainQ != null))
        {
            try
            {
                brainQ.put(mes);
            }
            catch (CutoffExceededException exc)
            {
                D.ebugPrintln("CutoffExceededException" + exc);
            }

            /**
             * if the brain isn't alive, then we need to leave
             * the game
             */
            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());

            if ((brain == null) || (!brain.isAlive()))
            {
                leaveGame((SOCGame) games.get(mes.getGame()));
            }
        }
    }

    /**
     * handle the "potential settlements" message
     * @param mes  the message
     */
    protected void handlePOTENTIALSETTLEMENTS(SOCPotentialSettlements mes)
    {
    	
    	SOCPlayer player = game.getPlayer(((SOCPotentialSettlements) mes).getPlayerNumber());
    	player.setPotentialSettlements(((SOCPotentialSettlements) mes).getPotentialSettlements());
    	
    	if(player.getName().equals(nickname))
    		PA.setPlayer(player);
    	
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
    }

    /**
     * handle the "change face" message
     * @param mes  the message
     */
    protected void handleCHANGEFACE(SOCChangeFace mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
            player.setFaceId(mes.getFaceId());
        }
    }

    /**
     * handle the "longest road" message
     * @param mes  the message
     */
    protected void handleLONGESTROAD(SOCLongestRoad mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            if (mes.getPlayerNumber() == -1)
            {
                ga.setPlayerWithLongestRoad((SOCPlayer) null);
            }
            else
            {
                ga.setPlayerWithLongestRoad(ga.getPlayer(mes.getPlayerNumber()));
            }
        }
    }

    /**
     * handle the "largest army" message
     * @param mes  the message
     */
    protected void handleLARGESTARMY(SOCLargestArmy mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            if (mes.getPlayerNumber() == -1)
            {
                ga.setPlayerWithLargestArmy((SOCPlayer) null);
            }
            else
            {
                ga.setPlayerWithLargestArmy(ga.getPlayer(mes.getPlayerNumber()));
            }
        }
    }

    /**
     * the user leaves the given game
     *
     * @param ga   the game
     */
    public void leaveGame(SOCGame ga)
    {
        if (ga != null)
        {
            robotBrains.remove(ga.getName());
            brainQs.remove(ga.getName());
            games.remove(ga.getName());
            put(SOCLeaveGame.toCmd(nickname, host, ga.getName()));
        }
    }

    /**
     * add one the the number of clean brain kills
     */
    public void addCleanKill()
    {
        cleanBrainKills++;
    }

    /** destroy the applet */
    public void destroy()
    {
        SOCLeaveAll leaveAllMes = new SOCLeaveAll();
        put(leaveAllMes.toCmd());
        disconnectReconnect();
    }

    /**
     * for stand-alones
     */
    public static void main(String[] args)
    {
    	//Systen,out
    	
		if (args.length < 4)
		{
			System.err.println("usage: java soc.robot.SOCRobotClient host port_number userid password");

			return;
		}
    	
        InterfaceAgent ex1 = new InterfaceAgent(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        ex1.init();
    }
    
    public void buildInitialSettlement(int coordinate) {
    	
    	put(SOCPutPiece.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber(), SOCPlayingPiece.SETTLEMENT, coordinate));
    	
    	//game.putPiece(new SOCSettlement(game.getPlayer(nickname), coordinate));
    	
    	System.out.println("SETTLEMENTS LEFT :: "+game.getPlayer(nickname).getNumPieces(SOCPlayingPiece.SETTLEMENT));
    	
    	//PA.releaseAction();
    	
    }

    public void buildCityInMove(int coordinate, Vector plan_left) {
    	
    	put(SOCBuildRequest.toCmd(game.getName(), SOCPlayingPiece.CITY));
    	
    	put(SOCPutPiece.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber(), SOCPlayingPiece.CITY, coordinate));
    	
    	//game.putPiece(new SOCSettlement(game.getPlayer(nickname), coordinate));
    	
    	System.out.println("CITIES LEFT :: "+game.getPlayer(nickname).getNumPieces(SOCPlayingPiece.CITY));
    	System.out.println("SETTLEMENTS LEFT :: "+game.getPlayer(nickname).getNumPieces(SOCPlayingPiece.SETTLEMENT));
    	
    	InterfaceAgent.PA.actionStack(plan_left);
    	
    }
    
    
    public void buildRoad(double coordinate) {
    	
    	put(SOCPutPiece.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber(), SOCPlayingPiece.ROAD, new Double(coordinate).intValue()));
    	
    	//game.putPiece(new SOCRoad(game.getPlayer(nickname), coordinate));
    	
    	System.out.println("ROADS LEFT :: "+game.getPlayer(nickname).getNumPieces(SOCPlayingPiece.ROAD));
    	
    	//PA.releaseAction();
    	
    }
    
    public void buildSettlementInMove(int coordinate, Vector plan_left) {
    	
    	put(SOCBuildRequest.toCmd(game.getName(), SOCPlayingPiece.SETTLEMENT));
    	
    	put(SOCPutPiece.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber(), SOCPlayingPiece.SETTLEMENT, coordinate));
    	
    	//game.putPiece(new SOCSettlement(game.getPlayer(nickname), coordinate));
    	
    	System.out.println("SETTLEMENTS LEFT :: "+game.getPlayer(nickname).getNumPieces(SOCPlayingPiece.SETTLEMENT));
    	
    	InterfaceAgent.PA.actionStack(plan_left);
    	
//    	PA.releaseAction();
    	
    	
    	
    }
    
    public void buildRoadInMove(int coordinate, Vector plan_left) {

    	put(SOCBuildRequest.toCmd(game.getName(), SOCPlayingPiece.ROAD));
    	
    	put(SOCPutPiece.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber(), SOCPlayingPiece.ROAD, coordinate));
    	
    	//game.putPiece(new SOCRoad(game.getPlayer(nickname), coordinate));
    
    	System.out.println("ROADS LEFT :: "+game.getPlayer(nickname).getNumPieces(SOCPlayingPiece.ROAD));
    	
    	log.info("PLAN SIZE :: "+plan_left.size());
    	
    	log.info("CALLING ACTION STACK AFTER BUILDING A ROAD");
    	
    	InterfaceAgent.PA.actionStack(plan_left);
    	
    }
    
    // this method is called to purchase card
    
    public void purchaseCard() {
    	
    	put(SOCBuyCardRequest.toCmd(game.getName()));
   
    }
    
    public void makeTradeOffer(SOCResourceSet give_resource_set, SOCResourceSet get_resource_set) {
    	
    	SOCPlayer [] players = game.getPlayers();
    	
    	// get all the player numbers except this number
    	
    	boolean to [] = new boolean[4];
    	
    	for(int i = 0; i < players.length; i++) {
    		
    		if(players[i].getName().equals(nickname)) { // if not this player
    			
    			to[i] = false;
    			
    		} else
    			to[i] = true;
    	
    	}
    	
    	SOCTradeOffer offer = new SOCTradeOffer(game.getName(), game.getPlayer(nickname).getPlayerNumber(), to, give_resource_set, get_resource_set);
    	
    	put(SOCMakeOffer.toCmd(game.getName(), offer));
    	
    } 
    
    public void moveRobber(int hex) {
    	
    	put(SOCMoveRobber.toCmd(game.getName(), game.getPlayer(nickname).getPlayerNumber(), hex));
    
    }
    
    public void stealFromPlayer(int playerNo) {
    	
    	put(SOCChoosePlayer.toCmd(game.getName(), playerNo));
    	
    }
    
    public void discard(SOCResourceSet rs) {
    	
    	put(SOCDiscard.toCmd(game.getName(), rs));
    	
    }
    
    public void playCard(int cardType) {
    	
    	put(SOCPlayDevCardRequest.toCmd(game.getName(), cardType));
    	
    }
    
}
