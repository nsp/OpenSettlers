/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2007-2010 Jeremy D. Monin <jeremy@nand.net>
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

import soc.client.DisplaylessPlayerClient;
import soc.disableDebug.D;

import soc.game.Board;
import soc.game.City;
import soc.game.DevCardConstants;
import soc.game.DevCardSet;
import soc.game.Game;
import soc.game.Player;
import soc.game.PlayerNumbers;
import soc.game.PlayingPiece;
import soc.game.ResourceConstants;
import soc.game.ResourceSet;
import soc.game.Road;
import soc.game.Settlement;
import soc.game.TradeOffer;

import soc.message.AcceptOffer;
import soc.message.CancelBuildRequest;
import soc.message.ChoosePlayerRequest;
import soc.message.ClearOffer;
import soc.message.DevCard;
import soc.message.DevCardCount;
import soc.message.DiceResult;
import soc.message.DiscardRequest;
import soc.message.FirstPlayer;
import soc.message.GameState;
import soc.message.GameTextMsg;
import soc.message.MakeOffer;
import soc.message.Message;
import soc.message.MoveRobber;
import soc.message.PlayerElement;
import soc.message.PotentialSettlements;
import soc.message.PutPiece;
import soc.message.RejectOffer;
import soc.message.ResourceCount;
import soc.message.SetPlayedDevCard;
import soc.message.SetTurn;
import soc.message.Turn;

import soc.server.SOCServer;

import soc.util.CappedQueue;
import soc.util.CutoffExceededException;
import soc.util.DebugRecorder;
import soc.util.Queue;
import soc.util.RobotParameters;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;


/**
 * AI for playing Settlers of Catan.
 * Represents a robot player within 1 game.
 *
 * @author Robert S Thomas
 */
public class RobotBrain extends Thread
{
    /**
     * The robot parameters
     */
    RobotParameters robotParameters;

    /**
     * Flag for wheather or not we're alive
     */
    protected boolean alive;

    /**
     * Flag for wheather or not it is our turn
     */
    protected boolean ourTurn;

    /**
     * Timer for turn taking
     */
    protected int turnTime;

    /**
     * {@link #pause(int) Pause} for less time;
     * speeds up response in 6-player games.
     * @since 1.1.09
     */
    private boolean pauseFaster;

    /**
     * Our current state
     */
    protected int curState;

    /**
     * Random number generator
     */
    protected Random rand = new Random();

    /**
     * The client we are hooked up to
     */
    protected RobotClient client;

    /**
     * The game we are playing
     */
    protected Game game;

    /**
     * The {@link #game} we're playing is on the 6-player board.
     * @since 1.1.08
     */
    final private boolean gameIs6Player;

    /**
     * Our player data
     */
    protected Player ourPlayerData;
    
    /**
     * Dummy player for cancelling bad placements
     */
    protected Player dummyCancelPlayerData;

    /**
     * The queue of game messages
     */
    protected CappedQueue gameEventQ;

    /**
     * A counter used to measure passage of time
     */
    protected int counter;

    /**
     * During this turn, which is another player's turn,
     * have we yet decided whether to do the Special Building phase
     * (for the 6-player board)?
     * @since 1.1.08
     */
    private boolean decidedIfSpecialBuild;

    /**
     * true when we're waiting for our requested Special Building phase
     * (for the 6-player board).
     * @since 1.1.08
     */
    private boolean waitingForSpecialBuild;

    /**
     * This is what we want to build
     */
    protected PlayingPiece whatWeWantToBuild;

    /**
     * This is our current building plan, a stack of {@link PossiblePiece}.
     */
    protected Stack buildingPlan;

    /**
     * This is what we tried building this turn,
     * but the server said it was an illegal move
     * (due to a bug in our robot).
     * 
     * @see #whatWeWantToBuild
     * @see #failedBuildingAttempts
     */
    protected PlayingPiece whatWeFailedToBuild;

    /**
     * Track how many illegal placement requests we've
     * made this turn.  Avoid infinite turn length, by
     * preventing robot from alternately choosing two
     * wrong things when the server denies a bad build.
     * 
     * @see #whatWeFailedToBuild
     * @see #MAX_DENIED_BUILDING_PER_TURN
     */
    protected int failedBuildingAttempts;
    
    /**
     * If, during a turn, we make this many illegal build
     * requests that the server denies, stop trying.
     * 
     * @see #failedBuildingAttempts
     */
    public static int MAX_DENIED_BUILDING_PER_TURN = 3;

    /**
     * these are the two resources that we want
     * when we play a discovery dev card
     */
    protected ResourceSet resourceChoices;

    /**
     * this is the resource we want to monopolize
     */
    protected int monopolyChoice;

    /**
     * our player tracker
     */
    protected PlayerTracker ourPlayerTracker;

    /**
     * trackers for all players (one per player, including this robot)
     */
    protected HashMap playerTrackers;

    /**
     * the thing that determines what we want to build next
     */
    protected RobotDM decisionMaker;

    /**
     * the thing that determines how we negotiate
     */
    protected RobotNegotiator negotiator;

    /**
     * true if we're expecting the START1A state
     */
    protected boolean expectSTART1A;

    /**
     * true if we're expecting the START1B state
     */
    protected boolean expectSTART1B;

    /**
     * true if we're expecting the START2A state
     */
    protected boolean expectSTART2A;

    /**
     * true if we're expecting the START2B state
     */
    protected boolean expectSTART2B;

    /**
     * true if we're expecting the PLAY state
     */
    protected boolean expectPLAY;

    /**
     * true if we're expecting the PLAY1 state
     */
    protected boolean expectPLAY1;

    /**
     * true if we're expecting the PLACING_ROAD state
     */
    protected boolean expectPLACING_ROAD;

    /**
     * true if we're expecting the PLACING_SETTLEMENT state
     */
    protected boolean expectPLACING_SETTLEMENT;

    /**
     * true if we're expecting the PLACING_CITY state
     */
    protected boolean expectPLACING_CITY;

    /**
     * true if we're expecting the PLACING_ROBBER state
     */
    protected boolean expectPLACING_ROBBER;

    /**
     * true if we're expecting the PLACING_FREE_ROAD1 state
     */
    protected boolean expectPLACING_FREE_ROAD1;

    /**
     * true if we're expecting the PLACING_FREE_ROAD2 state
     */
    protected boolean expectPLACING_FREE_ROAD2;

    /**
     * true if were expecting a PUTPIECE message after
     * a START1A game state
     */
    protected boolean expectPUTPIECE_FROM_START1A;

    /**
     * true if were expecting a PUTPIECE message after
     * a START1B game state
     */
    protected boolean expectPUTPIECE_FROM_START1B;

    /**
     * true if were expecting a PUTPIECE message after
     * a START1A game state
     */
    protected boolean expectPUTPIECE_FROM_START2A;

    /**
     * true if were expecting a PUTPIECE message after
     * a START1A game state
     */
    protected boolean expectPUTPIECE_FROM_START2B;

    /**
     * true if we're expecting a DICERESULT message
     */
    protected boolean expectDICERESULT;

    /**
     * true if we're expecting a DISCARDREQUEST message
     */
    protected boolean expectDISCARD;

    /**
     * true if we're expecting to have to move the robber
     */
    protected boolean expectMOVEROBBER;

    /**
     * true if we're expecting to pick two resources
     */
    protected boolean expectWAITING_FOR_DISCOVERY;

    /**
     * true if we're expecting to pick a monopoly
     */
    protected boolean expectWAITING_FOR_MONOPOLY;

    /**
     * true if we're waiting for a GAMESTATE message from the server.
     * This is set after a robot action or requested action is sent to server,
     * or just before ending our turn (which also sets waitingForOurTurn == true).
     */
    protected boolean waitingForGameState;

    /**
     * true if we're waiting for a TURN message from the server
     * when it's our turn
     */
    protected boolean waitingForOurTurn;

    /**
     * true when we're waiting for the results of a trade
     */
    protected boolean waitingForTradeMsg;

    /**
     * true when we're waiting to receive a dev card
     */
    protected boolean waitingForDevCard;

    /**
     * true when the robber will move because a seven was rolled
     */
    protected boolean moveRobberOnSeven;

    /**
     * true if we're waiting for a response to our trade message
     */
    protected boolean waitingForTradeResponse;

    /**
     * true if we're done trading
     */
    protected boolean doneTrading;

    /**
     * true if the player with that player number has rejected our offer
     */
    protected boolean[] offerRejections;

    /**
     * the game state before the current one
     */
    protected int oldGameState;

    /**
     * used to cache resource estimates for the board
     */
    protected int[] resourceEstimates;

    /**
     * used in planning where to put our first and second settlements
     */
    protected int firstSettlement;

    /**
     * used in planning where to put our first and second settlements
     */
    protected int secondSettlement;

    /**
     * During START states, coordinate of our most recently placed road or settlement.
     * Used to avoid repeats in {@link #cancelWrongPiecePlacement(CancelBuildRequest)}.
     * @since 1.1.09
     */
    private int lastStartingPieceCoord;

    /**
     * During START1B and START2B states, coordinate of the potential settlement node
     * towards which we're building, as calculated by {@link #placeInitRoad()}.
     * Used to avoid repeats in {@link #cancelWrongPiecePlacementLocal(PlayingPiece)}.
     * @since 1.1.09
     */
    private int lastStartingRoadTowardsNode;

    /**
     * a thread that sends ping messages to this one
     */
    protected RobotPinger pinger;

    /**
     * an object for recording debug information that can
     * be accessed interactively
     */
    protected DebugRecorder[] dRecorder;

    /**
     * keeps track of which dRecorder is current
     */
    protected int currentDRecorder;

    /**
     * keeps track of the last thing we bought for debugging purposes
     */
    protected PossiblePiece lastMove;

    /**
     * keeps track of the last thing we wanted for debugging purposes
     */
    protected PossiblePiece lastTarget;

    /**
     * Create a robot brain to play a game.
     *<P>
     * Depending on {@link Game#getGameOptions() game options},
     * constructor might copy and alter the robot parameters
     * (for example, to clear {@link RobotParameters#getTradeFlag()}).
     *
     * @param rc  the robot client
     * @param params  the robot parameters
     * @param ga  the game we're playing
     * @param mq  the message queue
     */
    public RobotBrain(RobotClient rc, RobotParameters params, Game ga, CappedQueue mq)
    {
        client = rc;
        robotParameters = params.copyIfOptionChanged(ga.getGameOptions());
        game = ga;
        gameIs6Player = (ga.maxPlayers > 4);
        pauseFaster = gameIs6Player;
        gameEventQ = mq;
        alive = true;
        counter = 0;
        expectSTART1A = true;
        expectSTART1B = false;
        expectSTART2A = false;
        expectSTART2B = false;
        expectPLAY = false;
        expectPLAY1 = false;
        expectPLACING_ROAD = false;
        expectPLACING_SETTLEMENT = false;
        expectPLACING_CITY = false;
        expectPLACING_ROBBER = false;
        expectPLACING_FREE_ROAD1 = false;
        expectPLACING_FREE_ROAD2 = false;
        expectPUTPIECE_FROM_START1A = false;
        expectPUTPIECE_FROM_START1B = false;
        expectPUTPIECE_FROM_START2A = false;
        expectPUTPIECE_FROM_START2B = false;
        expectDICERESULT = false;
        expectDISCARD = false;
        expectMOVEROBBER = false;
        expectWAITING_FOR_DISCOVERY = false;
        expectWAITING_FOR_MONOPOLY = false;
        ourTurn = false;
        oldGameState = game.getGameState();
        waitingForGameState = false;
        waitingForOurTurn = false;
        waitingForTradeMsg = false;
        waitingForDevCard = false;
        waitingForSpecialBuild = false;
        decidedIfSpecialBuild = false;
        moveRobberOnSeven = false;
        waitingForTradeResponse = false;
        doneTrading = false;
        offerRejections = new boolean[game.maxPlayers];
        for (int i = 0; i < game.maxPlayers; i++)
        {
            offerRejections[i] = false;
        }

        buildingPlan = new Stack();
        resourceChoices = new ResourceSet();
        resourceChoices.add(2, ResourceConstants.CLAY);
        monopolyChoice = ResourceConstants.SHEEP;
        pinger = new RobotPinger(gameEventQ, client.getNickname() + "-" + game.getName());
        dRecorder = new DebugRecorder[2];
        dRecorder[0] = new DebugRecorder();
        dRecorder[1] = new DebugRecorder();
        currentDRecorder = 0;
    }

    /**
     * @return the robot parameters
     */
    public RobotParameters getRobotParameters()
    {
        return robotParameters;
    }

    /**
     * @return the player client
     */
    public RobotClient getClient()
    {
        return client;
    }

    /**
     * @return the player trackers (one per player, including this robot)
     */
    public HashMap getPlayerTrackers()
    {
        return playerTrackers;
    }

    /**
     * @return our player tracker
     */
    public PlayerTracker getOurPlayerTracker()
    {
        return ourPlayerTracker;
    }

    /**
     * A player has sat down and been added to the game,
     * during game formation. Create a PlayerTracker for them.
     *<p>
     * Called when SITDOWN received from server; one SITDOWN is
     * sent for every player, and our robot player might not be the
     * first or last SITDOWN.
     *<p>
     * Since our playerTrackers are initialized when our robot's
     * SITDOWN is received (robotclient calls setOurPlayerData()),
     * and seats may be vacant at that time (because SITDOWN not yet
     * received for those seats), we must add a PlayerTracker for
     * each SITDOWN received after our player's.
     *
     * @param pn Player number
     */
    public void addPlayerTracker(int pn)
    {
        if (null == playerTrackers)
        {
            // SITDOWN hasn't been sent for our own player yet.
            // When it is, playerTrackers will be initialized for
            // each non-vacant player, including pn.

            return;
        }
        if (null == playerTrackers.get(new Integer(pn)))
        {
            PlayerTracker tracker = new PlayerTracker(game.getPlayer(pn), this);
            playerTrackers.put(new Integer(pn), tracker);
        }
    }

    /**
     * @return the game data
     */
    public Game getGame()
    {
        return game;
    }

    /**
     * @return our player data
     */
    public Player getOurPlayerData()
    {
        return ourPlayerData;
    }

    /**
     * @return the building plan
     */
    public Stack getBuildingPlan()
    {
        return buildingPlan;
    }

    /**
     * @return the decision maker
     */
    public RobotDM getDecisionMaker()
    {
        return decisionMaker;
    }

    /**
     * turns the debug recorders on
     */
    public void turnOnDRecorder()
    {
        dRecorder[0].turnOn();
        dRecorder[1].turnOn();
    }

    /**
     * turns the debug recorders off
     */
    public void turnOffDRecorder()
    {
        dRecorder[0].turnOff();
        dRecorder[1].turnOff();
    }

    /**
     * @return the debug recorder
     */
    public DebugRecorder getDRecorder()
    {
        return dRecorder[currentDRecorder];
    }

    /**
     * @return the old debug recorder
     */
    public DebugRecorder getOldDRecorder()
    {
        return dRecorder[(currentDRecorder + 1) % 2];
    }

    /**
     * @return the last move we made
     */
    public PossiblePiece getLastMove()
    {
        return lastMove;
    }

    /**
     * @return our last target piece
     */
    public PossiblePiece getLastTarget()
    {
        return lastTarget;
    }

    /**
     * Find our player data using our nickname
     */
    public void setOurPlayerData()
    {
        ourPlayerData = game.getPlayer(client.getNickname());
        ourPlayerTracker = new PlayerTracker(ourPlayerData, this);
        int opn = ourPlayerData.getPlayerNumber();
        playerTrackers = new HashMap();
        playerTrackers.put(new Integer(opn), ourPlayerTracker);

        for (int pn = 0; pn < game.maxPlayers; pn++)
        {
            if ((pn != opn) && ! game.isSeatVacant(pn))
            {
                PlayerTracker tracker = new PlayerTracker(game.getPlayer(pn), this);
                playerTrackers.put(new Integer(pn), tracker);
            }
        }

        decisionMaker = new RobotDM(this);
        negotiator = new RobotNegotiator(this);
        dummyCancelPlayerData = new Player(-2, game);

        // Verify expected face (fast or smart robot)
        int faceId;
        switch (getRobotParameters().getStrategyType())
        {
        case RobotDM.SMART_STRATEGY:
            faceId = -1;  // smarter robot face
            break;

        default:
            faceId = 0;   // default robot face
        }
        if (ourPlayerData.getFaceId() != faceId)
        {
            ourPlayerData.setFaceId(faceId);
            // robotclient will handle sending it to server
        }
    }

    /**
     * Here is the run method.  Just keep receiving game events
     * and deal with each one.
     * Remember that we're sent a {@link GameTextMsg}(<tt>"*PING*"</tt>) once per second.
     */
    public void run()
    {
        // Thread name for debug
        try
        {
            Thread.currentThread().setName("robotBrain-" + client.getNickname() + "-" + game.getName());
        }
        catch (Throwable th) {}

        if (pinger != null)
        {
            pinger.start();

            try
            {
                /** Our player number */
                final int ourPN = ourPlayerData.getPlayerNumber();

                //
                // Along with actual game events, the pinger sends a GameTextMsg
                // once per second, to aid the robot's timekeeping counter.
                //

                while (alive)
                {
                    Message mes;

                    //if (!gameEventQ.empty()) {
                    mes = (Message) gameEventQ.get();  // Sleeps until message received

                    //} else {
                    //mes = null;
                    //}
                    final int mesType;

                    if (mes != null)
                    {
                        mesType = mes.getType();
                        if(mesType != Message.GAMETEXTMSG)
                            D.ebugPrintln("mes - " + mes);

                        // Debug aid: when looking at message contents: avoid pings:
                        // check here for (mesType != Message.GAMETEXTMSG).
                    }
                    else
                    {
                        mesType = -1;
                    }

                    if (waitingForTradeMsg && (counter > 10))
                    {
                        waitingForTradeMsg = false;
                        counter = 0;
                    }

                    if (waitingForTradeResponse && (counter > 100))
                    {
                        // Remember other players' responses, call client.clearOffer,
                        // clear waitingForTradeResponse and counter.
                        tradeStopWaitingClearOffer();
                    }

                    if (waitingForGameState && (counter > 10000))
                    {
                        //D.ebugPrintln("counter = "+counter);
                        //D.ebugPrintln("RESEND");
                        counter = 0;
                        client.resend();
                    }

                    if (mesType == Message.GAMESTATE)
                    {
                        waitingForGameState = false;
                        oldGameState = game.getGameState();
                        game.setGameState(((GameState) mes).getState());
                    }

                    else if (mesType == Message.FIRSTPLAYER)
                    {
                        game.setFirstPlayer(((FirstPlayer) mes).getPlayerNumber());
                    }

                    else if (mesType == Message.SETTURN)
                    {
                        game.setCurrentPlayerNumber(((SetTurn) mes).getPlayerNumber());
                    }

                    else if (mesType == Message.TURN)
                    {
                        game.setCurrentPlayerNumber(((Turn) mes).getPlayerNumber());
                        game.updateAtTurn();

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
                        // check or reset any special-building-phase decisions
                        //
                        decidedIfSpecialBuild = false;
                        if (game.getGameState() == Game.SPECIAL_BUILDING)
                        {
                            if (waitingForSpecialBuild && ! buildingPlan.isEmpty())
                            {
                                // Keep the building plan.
                                // Will ask during loop body to build.
                            } else {
                                // We have no plan, but will call planBuilding()
                                // during the loop body.  If buildingPlan still empty,
                                // bottom of loop will end our Special Building turn,
                                // just as it would in gamestate PLAY1.  Otherwise,
                                // will ask to build after planBuilding.
                            }
                        } else {
                            //
                            // reset any plans we had
                            //
                            buildingPlan.clear();
                        }
                        negotiator.resetTargetPieces();
                    }

                    if (game.getCurrentPlayerNumber() == ourPN)
                    {
                        ourTurn = true;
                        waitingForSpecialBuild = false;
                    }
                    else
                    {
                        ourTurn = false;
                    }

                    if ((mesType == Message.TURN) && (ourTurn))
                    {
                        waitingForOurTurn = false;

                        // Clear some per-turn variables.
                        // For others, find the code which calls game.updateAtTurn().
                        whatWeFailedToBuild = null;
                        failedBuildingAttempts = 0;
                    }

                    /**
                     * Handle some message types early.
                     */
                    switch (mesType)
                    {
                    case Message.PLAYERELEMENT:
                        {
                        handlePLAYERELEMENT((PlayerElement) mes);

                        // If this during the PLAY state, also updates the
                        // negotiator's is-selling flags.

                        // If our player is losing a resource needed for the buildingPlan, 
                        // clear the plan if this is for the Special Building Phase (on the 6-player board).
                        // In normal game play, we clear the building plan at the start of each turn.
                        }
                        break;

                    case Message.RESOURCECOUNT:
                        {
                        Player pl = game.getPlayer(((ResourceCount) mes).getPlayerNumber());

                        if (((ResourceCount) mes).getCount() != pl.getResources().getTotal())
                        {
                            ResourceSet rsrcs = pl.getResources();

                            if (D.ebugOn)
                            {
                                client.sendText(game, ">>> RESOURCE COUNT ERROR FOR PLAYER " + pl.getPlayerNumber() + ": " + ((ResourceCount) mes).getCount() + " != " + rsrcs.getTotal());
                            }

                            //
                            //  fix it
                            //
                            if (pl.getPlayerNumber() != ourPN)
                            {
                                rsrcs.clear();
                                rsrcs.setAmount(((ResourceCount) mes).getCount(), ResourceConstants.UNKNOWN);
                            }
                        }
                        }
                        break;

                    case Message.DICERESULT:
                        game.setCurrentDice(((DiceResult) mes).getResult());
                        break;

                    case Message.PUTPIECE:
                        handlePUTPIECE_updateGameData((PutPiece) mes);
                        // For initial roads, also tracks their initial settlement in PlayerTracker.
                        break;

                    case Message.CANCELBUILDREQUEST:
                        handleCANCELBUILDREQUEST((CancelBuildRequest) mes);
                        break;

                    case Message.MOVEROBBER:
                        {
                        //
                        // Note: Don't call ga.moveRobber() because that will call the 
                        // functions to do the stealing.  We just want to set where 
                        // the robber moved, without seeing if something was stolen.
                        // MOVEROBBER will be followed by PLAYERELEMENT messages to
                        // report the gain/loss of resources.
                        //
                        moveRobberOnSeven = false;
                        game.getBoard().setRobberHex(((MoveRobber) mes).getCoordinates());
                        }
                        break;

                    case Message.MAKEOFFER:
                        if (robotParameters.getTradeFlag() == 1)
                            handleMAKEOFFER((MakeOffer) mes);
                        break;

                    case Message.CLEAROFFER:
                        if (robotParameters.getTradeFlag() == 1)
                        {
                            final int pn = ((ClearOffer) mes).getPlayerNumber();
                            if (pn != -1)
                            {
                                game.getPlayer(pn).setCurrentOffer(null);
                            } else {
                                for (int i = 0; i < game.maxPlayers; ++i)
                                    game.getPlayer(i).setCurrentOffer(null);
                            }
                        }
                        break;

                    case Message.ACCEPTOFFER:
                        if (waitingForTradeResponse && (robotParameters.getTradeFlag() == 1))
                        {
                            if ((ourPN == (((AcceptOffer) mes).getOfferingNumber()))
                                || (ourPN == ((AcceptOffer) mes).getAcceptingNumber()))
                            {
                                waitingForTradeResponse = false;
                            }
                        }
                        break;

                    case Message.REJECTOFFER:
                        if (robotParameters.getTradeFlag() == 1)
                            handleREJECTOFFER((RejectOffer) mes);
                        break;

                    case Message.DEVCARDCOUNT:
                        game.setNumDevCards(((DevCardCount) mes).getNumDevCards());
                        break;

                    case Message.DEVCARD:
                        handleDEVCARD((DevCard) mes);
                        break;

                    case Message.SETPLAYEDDEVCARD:
                        {
                        Player player = game.getPlayer(((SetPlayedDevCard) mes).getPlayerNumber());
                        player.setPlayedDevCard(((SetPlayedDevCard) mes).hasPlayedDevCard());
                        }
                        break;

                    case Message.POTENTIALSETTLEMENTS:
                        {
                        Player player = game.getPlayer(((PotentialSettlements) mes).getPlayerNumber());
                        player.setPotentialSettlements(((PotentialSettlements) mes).getPotentialSettlements());
                        }
                        break;

                    }  // switch(mesType)

                    debugInfo();

                    if ((game.getGameState() == Game.PLAY) && (!waitingForGameState))
                    {
                        rollOrPlayKnightOrExpectDice();

                        // On our turn, ask client to roll dice or play a knight;
                        // on other turns, update flags to expect dice result.
                        // Clears expectPLAY to false.
                        // Sets either expectDICERESULT, or expectPLACING_ROBBER and waitingForGameState.
                    }

                    if ((game.getGameState() == Game.PLACING_ROBBER) && (!waitingForGameState))
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

                                    if (oldGameState == Game.PLAY)
                                    {
                                        expectPLAY = true;
                                    }
                                    else if (oldGameState == Game.PLAY1)
                                    {
                                        expectPLAY1 = true;
                                    }
                                }

                                counter = 0;
                                moveRobber();
                            }
                        }
                    }

                    if ((game.getGameState() == Game.WAITING_FOR_DISCOVERY) && (!waitingForGameState))
                    {
                        expectWAITING_FOR_DISCOVERY = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPLAY1) && (counter < 4000))
                            {
                                waitingForGameState = true;
                                expectPLAY1 = true;
                                counter = 0;
                                client.discoveryPick(game, resourceChoices); //!!!
                                pause(1500);
                            }
                        }
                    }

                    if ((game.getGameState() == Game.WAITING_FOR_MONOPOLY) && (!waitingForGameState))
                    {
                        expectWAITING_FOR_MONOPOLY = false;

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPLAY1) && (counter < 4000))
                            {
                                waitingForGameState = true;
                                expectPLAY1 = true;
                                counter = 0;
                                client.monopolyPick(game, monopolyChoice); //!!!
                                pause(1500);
                            }
                        }
                    }

                    if (waitingForTradeMsg && (mesType == Message.GAMETEXTMSG) && (((GameTextMsg) mes).getNickname().equals(SOCServer.SERVERNAME)))
                    {
                        //
                        // This might be the trade message we've been waiting for
                        //
                        if (((GameTextMsg) mes).getText().startsWith(client.getNickname() + " traded"))
                        {
                            waitingForTradeMsg = false;
                        }
                    }

                    if (waitingForDevCard && (mesType == Message.GAMETEXTMSG) && (((GameTextMsg) mes).getNickname().equals(SOCServer.SERVERNAME)))
                    {
                        //
                        // This might be the dev card message we've been waiting for
                        //
                        if (((GameTextMsg) mes).getText().equals(client.getNickname() + " bought a development card."))
                        {
                            waitingForDevCard = false;
                        }
                    }

                    if (((game.getGameState() == Game.PLAY1) || (game.getGameState() == Game.SPECIAL_BUILDING))
                        && (!waitingForGameState) && (!waitingForTradeMsg) && (!waitingForTradeResponse) && (!waitingForDevCard)
                        && (!expectPLACING_ROAD) && (!expectPLACING_SETTLEMENT) && (!expectPLACING_CITY) && (!expectPLACING_ROBBER) && (!expectPLACING_FREE_ROAD1) && (!expectPLACING_FREE_ROAD2) && (!expectWAITING_FOR_DISCOVERY) && (!expectWAITING_FOR_MONOPOLY))
                    {
                        // Time to decide to build, or take other normal actions.

                        expectPLAY1 = false;

                        // 6-player: check Special Building Phase
                        // during other players' turns.
                        if ((! ourTurn) && waitingForOurTurn && gameIs6Player
                             && (! decidedIfSpecialBuild) && (!expectPLACING_ROBBER))
                        {
                            decidedIfSpecialBuild = true;

                            /**
                             * It's not our turn.  We're not doing anything else right now.
                             * Gamestate has passed PLAY, so we know what resources to expect.
                             * Do we want to Special Build?  Check the same conditions as during our turn.
                             * Make a plan if we don't have one,
                             * and if we haven't given up building
                             * attempts this turn.
                             */

                            if ((buildingPlan.empty()) && (ourPlayerData.getResources().getTotal() > 1) && (failedBuildingAttempts < MAX_DENIED_BUILDING_PER_TURN))
                            {
                                planBuilding();

                                /*
                                 * planBuilding takes these actions:
                                 *
                                decisionMaker.planStuff(robotParameters.getStrategyType());

                                if (!buildingPlan.empty())
                                {
                                    lastTarget = (PossiblePiece) buildingPlan.peek();
                                    negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), (PossiblePiece) buildingPlan.peek());
                                }
                                 */

                                if ( ! buildingPlan.empty())
                                {
                                    // Do we have the resources right now?
                                    final PossiblePiece targetPiece = (PossiblePiece) buildingPlan.peek();
                                    final ResourceSet targetResources = PlayingPiece.getResourcesToBuild(targetPiece.getType());

                                    if ((ourPlayerData.getResources().contains(targetResources)))
                                    {
                                        // Ask server for the Special Building Phase.
                                        // (TODO) if FAST_STRATEGY: Maybe randomly don't ask?
                                        waitingForSpecialBuild = true;
                                        client.buildRequest(game, -1);
                                        pause(100);
                                    }
                                }
                            }
                        }

                        if ((!waitingForOurTurn) && (ourTurn))
                        {
                            if (!(expectPLAY && (counter < 4000)))
                            {
                                counter = 0;

                                //D.ebugPrintln("DOING PLAY1");
                                if (D.ebugOn)
                                {
                                    client.sendText(game, "================================");

                                    // for each player in game:
                                    //    sendText and debug-prn game.getPlayer(i).getResources()
                                    printResources();
                                }

                                /**
                                 * if we haven't played a dev card yet,
                                 * and we have a knight, and we can get
                                 * largest army, play the knight.
                                 * If we're in SPECIAL_BUILDING (not PLAY1),
                                 * can't trade or play development cards.
                                 */
                                if ((game.getGameState() == Game.PLAY1) && ! ourPlayerData.hasPlayedDevCard())
                                {
                                    Player laPlayer = game.getPlayerWithLargestArmy();

                                    if (((laPlayer != null) && (laPlayer.getPlayerNumber() != ourPN)) || (laPlayer == null))
                                    {
                                        int larmySize;

                                        if (laPlayer == null)
                                        {
                                            larmySize = 3;
                                        }
                                        else
                                        {
                                            larmySize = laPlayer.getNumKnights() + 1;
                                        }

                                        if (((ourPlayerData.getNumKnights() + ourPlayerData.getDevCards().getAmount(DevCardSet.NEW, DevCardConstants.KNIGHT) + ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.KNIGHT)) >= larmySize) && (ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.KNIGHT) > 0))
                                        {
                                            /**
                                             * play a knight card
                                             */
                                            expectPLACING_ROBBER = true;
                                            waitingForGameState = true;
                                            counter = 0;
                                            client.playDevCard(game, DevCardConstants.KNIGHT);
                                            pause(1500);
                                        }
                                    }
                                }

                                /**
                                 * make a plan if we don't have one,
                                 * and if we haven't given up building
                                 * attempts this turn.
                                 */
                                if (!expectPLACING_ROBBER && (buildingPlan.empty()) && (ourPlayerData.getResources().getTotal() > 1) && (failedBuildingAttempts < MAX_DENIED_BUILDING_PER_TURN))
                                {
                                    planBuilding();

                                    /*
                                     * planBuilding takes these actions:
                                     *
                                    decisionMaker.planStuff(robotParameters.getStrategyType());

                                    if (!buildingPlan.empty())
                                    {
                                        lastTarget = (PossiblePiece) buildingPlan.peek();
                                        negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), (PossiblePiece) buildingPlan.peek());
                                    }
                                     */
                                }

                                //D.ebugPrintln("DONE PLANNING");
                                if (!expectPLACING_ROBBER && !buildingPlan.empty())
                                {
                                    // Time to build something.

                                    // Either ask to build a piece, or use trading or development
                                    // cards to get resources to build it.  See javadoc for flags set.
                                    buildOrGetResourceByTradeOrCard();
                                }

                                /**
                                 * see if we're done with our turn
                                 */
                                if (!(expectPLACING_SETTLEMENT || expectPLACING_FREE_ROAD1 || expectPLACING_FREE_ROAD2 || expectPLACING_ROAD || expectPLACING_CITY || expectWAITING_FOR_DISCOVERY || expectWAITING_FOR_MONOPOLY || expectPLACING_ROBBER || waitingForTradeMsg || waitingForTradeResponse || waitingForDevCard))
                                {
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

                                    //D.ebugPrintln("!!! ENDING TURN !!!");
                                    negotiator.resetIsSelling();
                                    negotiator.resetOffersMade();
                                    buildingPlan.clear();
                                    negotiator.resetTargetPieces();
                                    pause(1500);
                                    client.endTurn(game);
                                }
                            }
                        }
                    }

                    /**
                     * Placement: Make various putPiece calls; server has told us it's OK to buy them.
                     * Call client.putPiece.
                     * Works when it's our turn and we have an expect flag set
                     * (such as expectPLACING_SETTLEMENT, in these game states:
                     * START1A - START2B
                     * PLACING_SETTLEMENT, PLACING_ROAD, PLACING_CITY
                     * PLACING_FREE_ROAD1, PLACING_FREE_ROAD2
                     */
                    if (! waitingForGameState)
                    {
                        placeIfExpectPlacing();
                    }

                    /**
                     * End of various putPiece placement calls.
                     */

                    /*
                       if (game.getGameState() == Game.OVER) {
                       client.leaveGame(game);
                       alive = false;
                       }
                     */

                    /**
                     * Handle various message types here at bottom of loop.
                     */
                    switch (mesType)
                    {
                    case Message.SETTURN:
                        game.setCurrentPlayerNumber(((SetTurn) mes).getPlayerNumber());
                        break;

                    case Message.PUTPIECE:
                        /**
                         * this is for player tracking
                         */
                        handlePUTPIECE_updateTrackers((PutPiece) mes);

                        // For initial placement of our own pieces, also checks
                        // and clears expectPUTPIECE_FROM_START1A,
                        // and sets expectSTART1B, etc.  The final initial putpiece
                        // clears expectPUTPIECE_FROM_START2B and sets expectPLAY.

                        break;

                    case Message.DICERESULT:
                        if (expectDICERESULT)
                        {
                            expectDICERESULT = false;
    
                            if (((DiceResult) mes).getResult() == 7)
                            {
                                moveRobberOnSeven = true;
    
                                if (ourPlayerData.getResources().getTotal() > 7)
                                    expectDISCARD = true;

                                else if (ourTurn)
                                    expectPLACING_ROBBER = true;
                            }
                            else
                            {
                                expectPLAY1 = true;
                            }
                        }
                        break;

                    case Message.DISCARDREQUEST:
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
                        discard(((DiscardRequest) mes).getNumberOfDiscards());

                        //	}
                        break;

                    case Message.CHOOSEPLAYERREQUEST:
                        chooseRobberVictim(((ChoosePlayerRequest) mes).getChoices());
                        break;

                    case Message.ROBOTDISMISS:
                        if ((!expectDISCARD) && (!expectPLACING_ROBBER))
                        {
                            client.leaveGame(game, "dismiss msg", false);
                            alive = false;
                        }
                        break;

                    case Message.GAMETEXTMSG:
                        if (((GameTextMsg) mes).getText().equals("*PING*"))
                        {
                            // Once-per-second message from the pinger thread
                            counter++;
                        }
                        break;

                    }  // switch (mesType) - for some types, at bottom of loop body

                    if (counter > 15000)
                    {
                        // We've been waiting too long, commit suicide.
                        client.leaveGame(game, "counter 15000", false);
                        alive = false;
                    }

                    if ((failedBuildingAttempts > (2 * MAX_DENIED_BUILDING_PER_TURN))
                        && (game.getGameState() <= Game.START2B))
                    {
                        // Apparently can't decide where we can initially place:
                        // Leave the game.
                        client.leaveGame(game, "failedBuildingAttempts at start", false);
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
                    Thread.yield();
                }
            }
            catch (Throwable e)
            {
                // Ignore errors due to game reset in another thread
                if (alive && ((game == null) || (game.getGameState() != Game.RESET_OLD)))
                {
                    D.ebugPrintln("*** Robot caught an exception - " + e);
                    System.out.println("*** Robot caught an exception - " + e);
                    e.printStackTrace();
                }
            }
        }
        else
        {
            System.out.println("AGG! NO PINGER!");
        }

        //D.ebugPrintln("STOPPING AND DEALLOCATING");
        gameEventQ = null;
        client.addCleanKill();
        client = null;
        game = null;
        ourPlayerData = null;
        dummyCancelPlayerData = null;
        whatWeWantToBuild = null;
        whatWeFailedToBuild = null;
        resourceChoices = null;
        ourPlayerTracker = null;
        playerTrackers = null;
        pinger.stopPinger();
        pinger = null;
    }

    /**
     * Stop waiting for responses to a trade offer.
     * Remember other players' responses,
     * Call {@link RobotClient#clearOffer(Game) client.clearOffer},
     * clear {@link #waitingForTradeResponse} and {@link #counter}.
     * @since 1.1.09
     */
    private void tradeStopWaitingClearOffer()
    {
        ///
        /// record which players said no by not saying anything
        ///
        TradeOffer ourCurrentOffer = ourPlayerData.getCurrentOffer();

        if (ourCurrentOffer != null)
        {
            boolean[] offeredTo = ourCurrentOffer.getTo();
            ResourceSet getSet = ourCurrentOffer.getGetSet();

            for (int rsrcType = ResourceConstants.CLAY;
                    rsrcType <= ResourceConstants.WOOD;
                    rsrcType++)
            {
                if (getSet.getAmount(rsrcType) > 0)
                {
                    for (int pn = 0; pn < game.maxPlayers; pn++)
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

    /**
     * If it's our turn and we have an expect flag set
     * (such as {@link #expectPLACING_SETTLEMENT}), then
     * call {@link RobotClient#putPiece(Game, PlayingPiece) client.putPiece}.
     *<P>
     * Looks for one of these game states:
     *<UL>
     * <LI> {@link Game#START1A} - {@link Game#START2B}
     * <LI> {@link Game#PLACING_SETTLEMENT}
     * <LI> {@link Game#PLACING_ROAD}
     * <LI> {@link Game#PLACING_CITY}
     * <LI> {@link Game#PLACING_FREE_ROAD1}
     * <LI> {@link Game#PLACING_FREE_ROAD2}
     *</UL>
     * @since 1.1.09
     */
    private void placeIfExpectPlacing()
    {
        if (waitingForGameState)
            return;

        switch (game.getGameState())
        {
            case Game.PLACING_SETTLEMENT:
            {
                if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_SETTLEMENT))
                {
                    expectPLACING_SETTLEMENT = false;
                    waitingForGameState = true;
                    counter = 0;
                    expectPLAY1 = true;
    
                    //D.ebugPrintln("!!! PUTTING PIECE "+whatWeWantToBuild+" !!!");
                    pause(500);
                    client.putPiece(game, whatWeWantToBuild);
                    pause(1000);
                }
            }
            break;

            case Game.PLACING_ROAD:
            {
                if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_ROAD))
                {
                    expectPLACING_ROAD = false;
                    waitingForGameState = true;
                    counter = 0;
                    expectPLAY1 = true;

                    pause(500);
                    client.putPiece(game, whatWeWantToBuild);
                    pause(1000);
                }
            }
            break;

            case Game.PLACING_CITY:
            {
                if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_CITY))
                {
                    expectPLACING_CITY = false;
                    waitingForGameState = true;
                    counter = 0;
                    expectPLAY1 = true;

                    pause(500);
                    client.putPiece(game, whatWeWantToBuild);
                    pause(1000);
                }
            }
            break;

            case Game.PLACING_FREE_ROAD1:
            {
                if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_FREE_ROAD1))
                {
                    expectPLACING_FREE_ROAD1 = false;
                    waitingForGameState = true;
                    counter = 0;
                    expectPLACING_FREE_ROAD2 = true;
                    // D.ebugPrintln("!!! PUTTING PIECE 1 " + whatWeWantToBuild + " !!!");
                    pause(500);
                    client.putPiece(game, whatWeWantToBuild);
                    pause(1000);
                }
            }
            break;

            case Game.PLACING_FREE_ROAD2:
            {
                if ((ourTurn) && (!waitingForOurTurn) && (expectPLACING_FREE_ROAD2))
                {
                    expectPLACING_FREE_ROAD2 = false;
                    waitingForGameState = true;
                    counter = 0;
                    expectPLAY1 = true;
    
                    PossiblePiece posPiece = (PossiblePiece) buildingPlan.pop();
    
                    if (posPiece.getType() == PossiblePiece.ROAD)
                    {
                        // D.ebugPrintln("posPiece = " + posPiece);
                        whatWeWantToBuild = new Road(ourPlayerData, posPiece.getCoordinates(), null);
                        // D.ebugPrintln("$ POPPED OFF");
                        // D.ebugPrintln("!!! PUTTING PIECE 2 " + whatWeWantToBuild + " !!!");
                        pause(500);
                        client.putPiece(game, whatWeWantToBuild);
                        pause(1000);
                    }
                }
            }
            break;

            case Game.START1A:
            {
                expectSTART1A = false;
    
                if ((!waitingForOurTurn) && (ourTurn) && (!(expectPUTPIECE_FROM_START1A && (counter < 4000))))
                {
                    expectPUTPIECE_FROM_START1A = true;
                    counter = 0;
                    waitingForGameState = true;
                    planInitialSettlements();
                    placeFirstSettlement();
                }
            }
            break;

            case Game.START1B:
            {
                expectSTART1B = false;
    
                if ((!waitingForOurTurn) && (ourTurn) && (!(expectPUTPIECE_FROM_START1B && (counter < 4000))))
                {
                    expectPUTPIECE_FROM_START1B = true;
                    counter = 0;
                    waitingForGameState = true;
                    pause(1500);
                    placeInitRoad();
                }
            }
            break;

            case Game.START2A:
            {
                expectSTART2A = false;
    
                if ((!waitingForOurTurn) && (ourTurn) && (!(expectPUTPIECE_FROM_START2A && (counter < 4000))))
                {
                    expectPUTPIECE_FROM_START2A = true;
                    counter = 0;
                    waitingForGameState = true;
                    planSecondSettlement();
                    placeSecondSettlement();
                }
            }
            break;

            case Game.START2B:
            {
                expectSTART2B = false;
    
                if ((!waitingForOurTurn) && (ourTurn) && (!(expectPUTPIECE_FROM_START2B && (counter < 4000))))
                {
                    expectPUTPIECE_FROM_START2B = true;
                    counter = 0;
                    waitingForGameState = true;
                    pause(1500);
                    placeInitRoad();
                }
            }
            break;

        }
    }

    /**
     * On our turn, ask client to roll dice or play a knight;
     * on other turns, update flags to expect dice result.
     *<P>
     * Call when gameState {@link Game#PLAY} && ! {@link #waitingForGameState}.
     *<P>
     * Clears {@link #expectPLAY} to false.
     * Sets either {@link #expectDICERESULT}, or {@link #expectPLACING_ROBBER} and {@link #waitingForGameState}.
     *
     * @since 1.1.08
     */
    private void rollOrPlayKnightOrExpectDice()
    {
        expectPLAY = false;

        if ((!waitingForOurTurn) && (ourTurn))
        {
            if (!expectPLAY1 && !expectDISCARD && !expectPLACING_ROBBER && !(expectDICERESULT && (counter < 4000)))
            {
                /**
                 * if we have a knight card and the robber
                 * is on one of our numbers, play the knight card
                 */
                if ((ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.KNIGHT) > 0)
                    && ! (ourPlayerData.getNumbers().getNumberResourcePairsForHex(game.getBoard().getRobberHex())).isEmpty())
                {
                    expectPLACING_ROBBER = true;
                    waitingForGameState = true;
                    counter = 0;
                    client.playDevCard(game, DevCardConstants.KNIGHT);
                    pause(1500);
                }
                else
                {
                    expectDICERESULT = true;
                    counter = 0;

                    //D.ebugPrintln("!!! ROLLING DICE !!!");
                    client.rollDice(game);
                }
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

    /**
     * Either ask to build a piece, or use trading or development cards to get resources to build it.
     * Examines {@link #buildingPlan} for the next piece wanted.
     *<P>
     * Call when these conditions are all true:
     * <UL>
     *<LI> gameState {@link Game#PLAY1} or {@link Game#SPECIAL_BUILDING}
     *<LI> <tt>waitingFor...</tt> flags all false ({@link #waitingForGameState}, etc) except possibly {@link #waitingForSpecialBuild}
     *<LI> <tt>expect...</tt> flags all false ({@link #expectPLACING_ROAD}, etc)
     *<LI> ! {@link #waitingForOurTurn}
     *<LI> {@link #ourTurn}
     *<LI> ! ({@link #expectPLAY} && (counter < 4000))
     *<LI> ! {@link #buildingPlan}.empty()
     *</UL>
     *<P>
     * May set any of these flags:
     * <UL>
     *<LI> {@link #waitingForGameState}, and {@link #expectWAITING_FOR_DISCOVERY} or {@link #expectWAITING_FOR_MONOPOLY}
     *<LI> {@link #waitingForTradeMsg} or {@link #waitingForTradeResponse} or {@link #doneTrading}
     *<LI> {@link #waitingForDevCard}, or {@link #waitingForGameState} and {@link #expectPLACING_SETTLEMENT} (etc).
     *</UL>
     *
     * @since 1.1.08
     */
    private void buildOrGetResourceByTradeOrCard()
    {
        /**
         * If we're in SPECIAL_BUILDING (not PLAY1),
         * can't trade or play development cards.
         */
        final boolean gameStatePLAY1 = (game.getGameState() == Game.PLAY1);

        /**
         * check to see if this is a Road Building plan
         */
        boolean roadBuildingPlan = false;

        if (gameStatePLAY1 && (! ourPlayerData.hasPlayedDevCard()) && (ourPlayerData.getNumPieces(PlayingPiece.ROAD) >= 2) && (ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.ROADS) > 0))
        {
            //D.ebugPrintln("** Checking for Road Building Plan **");
            PossiblePiece topPiece = (PossiblePiece) buildingPlan.pop();

            //D.ebugPrintln("$ POPPED "+topPiece);
            if ((topPiece != null) && (topPiece.getType() == PossiblePiece.ROAD) && (!buildingPlan.empty()))
            {
                PossiblePiece secondPiece = (PossiblePiece) buildingPlan.peek();

                //D.ebugPrintln("secondPiece="+secondPiece);
                if ((secondPiece != null) && (secondPiece.getType() == PossiblePiece.ROAD))
                {
                    roadBuildingPlan = true;
                    whatWeWantToBuild = new Road(ourPlayerData, topPiece.getCoordinates(), null);
                    if (! whatWeWantToBuild.equals(whatWeFailedToBuild))
                    {
                        waitingForGameState = true;
                        counter = 0;
                        expectPLACING_FREE_ROAD1 = true;

                        //D.ebugPrintln("!! PLAYING ROAD BUILDING CARD");
                        client.playDevCard(game, DevCardConstants.ROADS);
                    } else {
                        // We already tried to build this.
                        roadBuildingPlan = false;
                        cancelWrongPiecePlacementLocal(whatWeWantToBuild);
                        // cancel sets whatWeWantToBuild = null;
                    }
                }
                else
                {
                    //D.ebugPrintln("$ PUSHING "+topPiece);
                    buildingPlan.push(topPiece);
                }
            }
            else
            {
                //D.ebugPrintln("$ PUSHING "+topPiece);
                buildingPlan.push(topPiece);
            }
        }

        if (! roadBuildingPlan)
        {
            ///
            /// figure out what resources we need
            ///
            PossiblePiece targetPiece = (PossiblePiece) buildingPlan.peek();
            ResourceSet targetResources = PlayingPiece.getResourcesToBuild(targetPiece.getType());

            //D.ebugPrintln("^^^ targetPiece = "+targetPiece);
            //D.ebugPrintln("^^^ ourResources = "+ourPlayerData.getResources());

            negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), targetPiece);

            ///
            /// if we have a 2 free resources card and we need
            /// at least 2 resources, play the card
            ///
            if (gameStatePLAY1 && (! ourPlayerData.hasPlayedDevCard()) && (ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.DISC) > 0))
            {
                ResourceSet ourResources = ourPlayerData.getResources();
                int numNeededResources = 0;

                for (int resource = ResourceConstants.CLAY;
                        resource <= ResourceConstants.WOOD;
                        resource++)
                {
                    int diff = targetResources.getAmount(resource) - ourResources.getAmount(resource);

                    if (diff > 0)
                    {
                        numNeededResources += diff;
                    }
                }

                if (numNeededResources == 2)
                {
                    chooseFreeResources(targetResources);

                    ///
                    /// play the card
                    ///
                    expectWAITING_FOR_DISCOVERY = true;
                    waitingForGameState = true;
                    counter = 0;
                    client.playDevCard(game, DevCardConstants.DISC);
                    pause(1500);
                }
            }

            if (!expectWAITING_FOR_DISCOVERY)
            {
                ///
                /// if we have a monopoly card, play it
                /// and take what there is most of
                ///
                if (gameStatePLAY1 && (! ourPlayerData.hasPlayedDevCard()) && (ourPlayerData.getDevCards().getAmount(DevCardSet.OLD, DevCardConstants.MONO) > 0) && chooseMonopoly())
                {
                    ///
                    /// play the card
                    ///
                    expectWAITING_FOR_MONOPOLY = true;
                    waitingForGameState = true;
                    counter = 0;
                    client.playDevCard(game, DevCardConstants.MONO);
                    pause(1500);
                }

                if (!expectWAITING_FOR_MONOPOLY)
                {
                    if (gameStatePLAY1 && (!doneTrading) && (!ourPlayerData.getResources().contains(targetResources)))
                    {
                        waitingForTradeResponse = false;

                        if (robotParameters.getTradeFlag() == 1)
                        {
                            makeOffer(targetPiece);
                            // makeOffer will set waitingForTradeResponse or doneTrading.
                        }
                    }

                    if (gameStatePLAY1 && !waitingForTradeResponse)
                    {
                        /**
                         * trade with the bank/ports
                         */
                        if (tradeToTarget2(targetResources))
                        {
                            counter = 0;
                            waitingForTradeMsg = true;
                            pause(1500);
                        }
                    }

                    ///
                    /// build if we can
                    ///
                    if (!waitingForTradeMsg && !waitingForTradeResponse && ourPlayerData.getResources().contains(targetResources))
                    {
                        // Calls buildingPlan.pop().
                        // Checks against whatWeFailedToBuild to see if server has rejected this already.
                        // Calls client.buyDevCard or client.buildRequest.
                        // Sets waitingForDevCard, or waitingForGameState and expectPLACING_SETTLEMENT (etc).

                        buildRequestPlannedPiece(targetPiece);
                    }
                }
            }
        }
    }

    /**
     * Handle a PUTPIECE for this game, by updating game data.
     * For initial roads, also track their initial settlement in PlayerTracker.
     * In general, most tracking is done a bit later in {@link #handlePUTPIECE_updateTrackers(PutPiece)}.
     * @since 1.1.08
     */
    private void handlePUTPIECE_updateGameData(PutPiece mes)
    {
        final Player pl = game.getPlayer(mes.getPlayerNumber());
        final int coord = mes.getCoordinates();

        switch (mes.getPieceType())
        {
        case PlayingPiece.ROAD:

            if ((game.getGameState() == Game.START1B) || (game.getGameState() == Game.START2B))
            {
                //
                // Before processing this road, track the settlement that goes with it.
                // This was deferred until road placement, in case a human player decides
                // to cancel their settlement and place it elsewhere.
                //
                PlayerTracker tr = (PlayerTracker) playerTrackers.get
                    (new Integer(mes.getPlayerNumber()));
                Settlement se = tr.getPendingInitSettlement();
                if (se != null)
                    trackNewSettlement(se, false);
            }
            Road rd = new Road(pl, coord, null);
            game.putPiece(rd);
            break;

        case PlayingPiece.SETTLEMENT:

            Settlement se = new Settlement(pl, coord, null);
            game.putPiece(se);
            break;

        case PlayingPiece.CITY:

            City ci = new City(pl, coord, null);
            game.putPiece(ci);
            break;
        }
    }

    /**
     * Handle a CANCELBUILDREQUEST for this game.
     *<P>
     *<b> During game startup</b> (START1B or START2B): <BR>
     *    When sent from server to client, CANCELBUILDREQUEST means the current
     *    player wants to undo the placement of their initial settlement.
     *<P>
     *<b> During piece placement</b> (PLACING_ROAD, PLACING_CITY, PLACING_SETTLEMENT,
     *                         PLACING_FREE_ROAD1, or PLACING_FREE_ROAD2): <BR>
     *    When sent from server to client, CANCELBUILDREQUEST means the player
     *    has sent an illegal PUTPIECE (bad building location). 
     *    Humans can probably decide a better place to put their road,
     *    but robots must cancel the build request and decide on a new plan.
     *
     * @since 1.1.08
     */
    private void handleCANCELBUILDREQUEST(CancelBuildRequest mes)
    {
        final int gstate = game.getGameState();
        switch (gstate)
        {
        case Game.START1A:
        case Game.START2A:
            if (ourTurn)
            {
                cancelWrongPiecePlacement(mes);
            }
            break;

        case Game.START1B:
        case Game.START2B:
            if (ourTurn)
            {
                cancelWrongPiecePlacement(mes);
            }
            else
            {
                //
                // human player placed, then cancelled placement.
                // Our robot wouldn't do that, and if it's ourTurn,
                // the cancel happens only if we try an illegal placement.
                //
                final int pnum = game.getCurrentPlayerNumber();
                Player pl = game.getPlayer(pnum);
                Settlement pp = new Settlement(pl, pl.getLastSettlementCoord(), null);
                game.undoPutInitSettlement(pp);
                //
                // "forget" to track this cancelled initial settlement.
                // Wait for human player to place a new one.
                //
                PlayerTracker tr = (PlayerTracker) playerTrackers.get
                    (new Integer(pnum));
                tr.setPendingInitSettlement(null);
            }
            break;

        case Game.PLAY1:  // asked to build, hasn't given location yet -> resources
        case Game.PLACING_ROAD:        // has given location -> is bad location
        case Game.PLACING_SETTLEMENT:
        case Game.PLACING_CITY:
        case Game.PLACING_FREE_ROAD1:  // JM TODO how to break out?
        case Game.PLACING_FREE_ROAD2:  // JM TODO how to break out?
            //
            // We've asked for an illegal piece placement.
            // (Must be a bug.) Cancel and invalidate this
            // planned piece, make a new plan.
            //
            cancelWrongPiecePlacement(mes);
            break;

        case Game.SPECIAL_BUILDING:
            //
            // Same as above, but in special building.
            // Sometimes happens if another player has placed since we
            // requested special building.  If our PUTPIECE request is
            // denied, server sends us CANCELBUILDREQUEST during SPECIAL_BUILDING.
            // This will cancel the placement, and also will
            // set variables to end our turn.
            //
            cancelWrongPiecePlacement(mes);
            break;

        default:
            if (game.isSpecialBuilding())
            {
                cancelWrongPiecePlacement(mes);
            } else {
                // Should not occur
                D.ebugPrintln("Unexpected CANCELBUILDREQUEST at state " + gstate);
            }

        }  // switch (gameState)
    }

    /**
     * Handle a MAKEOFFER for this game.
     * if another player makes an offer, that's the
     * same as a rejection, but still wants to deal.
     * Call {@link #considerOffer(TradeOffer)}, and if
     * we accept, clear our {@link #buildingPlan} so we'll replan it.
     * Ignore our own MAKEOFFERs echoed from server.
     * @since 1.1.08
     */
    private void handleMAKEOFFER(MakeOffer mes)
    {
        TradeOffer offer = mes.getOffer();
        game.getPlayer(offer.getFrom()).setCurrentOffer(offer);

        if ((offer.getFrom() == ourPlayerData.getPlayerNumber()))
        {
            return;  // <---- Ignore our own offers ----
        }

        ///
        /// record that this player wants to sell me the stuff
        ///
        ResourceSet giveSet = offer.getGiveSet();

        for (int rsrcType = ResourceConstants.CLAY;
                rsrcType <= ResourceConstants.WOOD;
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
        ResourceSet getSet = offer.getGetSet();

        for (int rsrcType = ResourceConstants.CLAY;
                rsrcType <= ResourceConstants.WOOD;
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

                for (int i = 0; i < game.maxPlayers; i++)
                {
                    D.ebugPrintln("offerRejections[" + i + "]=" + offerRejections[i]);

                    if (offeredTo[i] && !offerRejections[i])
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

        ///
        /// consider the offer
        ///
        int ourResponseToOffer = considerOffer(offer);

        D.ebugPrintln("%%% ourResponseToOffer = " + ourResponseToOffer);

        if (ourResponseToOffer < 0)
            return;

        int delayLength = Math.abs(rand.nextInt() % 500) + 3500;
        if (gameIs6Player && ! waitingForTradeResponse)
        {
            delayLength *= 2;  // usually, pause is half-length in 6-player
        }
        pause(delayLength);

        switch (ourResponseToOffer)
        {
        case RobotNegotiator.ACCEPT_OFFER:
            client.acceptOffer(game, offer.getFrom());

            ///
            /// clear our building plan, so that we replan
            ///
            buildingPlan.clear();
            negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), null);

            break;

        case RobotNegotiator.REJECT_OFFER:

            if (!waitingForTradeResponse)
                client.rejectOffer(game);

            break;

        case RobotNegotiator.COUNTER_OFFER:

            if (!makeCounterOffer(offer))
                client.rejectOffer(game);

            break;
        }
    }

    /**
     * Handle a REJECTOFFER for this game.
     * watch rejections of other players' offers, and of our offers.
     * @since 1.1.08
     */
    private void handleREJECTOFFER(RejectOffer mes)
    {
        ///
        /// see if everyone has rejected our offer
        ///
        int rejector = mes.getPlayerNumber();

        if ((ourPlayerData.getCurrentOffer() != null) && (waitingForTradeResponse))
        {
            D.ebugPrintln("%%%%%%%%% REJECT OFFER %%%%%%%%%%%%%");

            ///
            /// record which player said no
            ///
            ResourceSet getSet = ourPlayerData.getCurrentOffer().getGetSet();

            for (int rsrcType = ResourceConstants.CLAY;
                    rsrcType <= ResourceConstants.WOOD;
                    rsrcType++)
            {
                if ((getSet.getAmount(rsrcType) > 0) && (!negotiator.wantsAnotherOffer(rejector, rsrcType)))
                    negotiator.markAsNotSelling(rejector, rsrcType);
            }

            offerRejections[mes.getPlayerNumber()] = true;

            boolean everyoneRejected = true;
            D.ebugPrintln("ourPlayerData.getCurrentOffer() = " + ourPlayerData.getCurrentOffer());

            boolean[] offeredTo = ourPlayerData.getCurrentOffer().getTo();

            for (int i = 0; i < game.maxPlayers; i++)
            {
                D.ebugPrintln("offerRejections[" + i + "]=" + offerRejections[i]);

                if (offeredTo[i] && !offerRejections[i])
                    everyoneRejected = false;
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

            for (int pn = 0; pn < game.maxPlayers; pn++)
            {
                TradeOffer offer = game.getPlayer(pn).getCurrentOffer();

                if (offer != null)
                {
                    boolean[] offeredTo = offer.getTo();

                    if (offeredTo[rejector])
                    {
                        //
                        // I think they were rejecting this offer
                        // mark them as not selling what was asked for
                        //
                        ResourceSet getSet = offer.getGetSet();

                        for (int rsrcType = ResourceConstants.CLAY;
                                rsrcType <= ResourceConstants.WOOD;
                                rsrcType++)
                        {
                            if ((getSet.getAmount(rsrcType) > 0) && (!negotiator.wantsAnotherOffer(rejector, rsrcType)))
                                negotiator.markAsNotSelling(rejector, rsrcType);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle a DEVCARD for this game.
     * No brain-specific action.
     * @since 1.1.08
     */
    private void handleDEVCARD(DevCard mes)
    {
        DevCardSet plCards = game.getPlayer(mes.getPlayerNumber()).getDevCards();
        final int cardType = mes.getCardType();

        switch (mes.getAction())
        {
        case DevCard.DRAW:
            plCards.add(1, DevCardSet.NEW, cardType);
            break;

        case DevCard.PLAY:
            plCards.subtract(1, DevCardSet.OLD, cardType);
            break;

        case DevCard.ADDOLD:
            plCards.add(1, DevCardSet.OLD, cardType);
            break;

        case DevCard.ADDNEW:
            plCards.add(1, DevCardSet.NEW, cardType);
            break;
        }
    }

    /**
     * Handle a PUTPIECE for this game, by updating {@link PlayerTracker}s.
     *<P>
     * For initial placement of our own pieces, this method also checks
     * and clears expectPUTPIECE_FROM_START1A, and sets expectSTART1B, etc.
     * The final initial putpiece clears expectPUTPIECE_FROM_START2B and sets expectPLAY.
     *<P>
     * For initial settlements, won't track here:
     * Delay tracking until the corresponding road is placed,
     * in {@link #handlePUTPIECE_updateGameData(PutPiece)}.
     * This prevents the need for tracker "undo" work if a human
     * player changes their mind on where to place the settlement.
     *
     * @since 1.1.08
     */
    private void handlePUTPIECE_updateTrackers(PutPiece mes)
    {
        final int pn = mes.getPlayerNumber();
        final int coord = mes.getCoordinates();
        final int pieceType = mes.getPieceType();

        switch (pieceType)
        {
        case PlayingPiece.ROAD:

            Road newRoad = new Road(game.getPlayer(pn), coord, null);
            trackNewRoad(newRoad, false);

            break;

        case PlayingPiece.SETTLEMENT:

            Player newSettlementPl = game.getPlayer(pn);
            Settlement newSettlement = new Settlement(newSettlementPl, coord, null);
            if ((game.getGameState() == Game.START1B) || (game.getGameState() == Game.START2B))
            {
                // Track it after the road is placed
                PlayerTracker tr = (PlayerTracker) playerTrackers.get
                    (new Integer(newSettlementPl.getPlayerNumber()));
                tr.setPendingInitSettlement(newSettlement);
            }
            else
            {
                // Track it now
                trackNewSettlement(newSettlement, false);
            }                            

            break;

        case PlayingPiece.CITY:

            City newCity = new City(game.getPlayer(pn), coord, null);
            trackNewCity(newCity, false);

            break;
        }

        if (D.ebugOn)
        {
            PlayerTracker.playerTrackersDebug(playerTrackers);
        }

        if (pn != ourPlayerData.getPlayerNumber())
        {
            return;  // <---- Not our piece ----
        }

        /**
         * Update expect-vars during initial placement of our pieces.
         */

        if (expectPUTPIECE_FROM_START1A && (pieceType == PlayingPiece.SETTLEMENT) && (mes.getCoordinates() == ourPlayerData.getLastSettlementCoord()))
        {
            expectPUTPIECE_FROM_START1A = false;
            expectSTART1B = true;
        }

        if (expectPUTPIECE_FROM_START1B && (pieceType == PlayingPiece.ROAD) && (mes.getCoordinates() == ourPlayerData.getLastRoadCoord()))
        {
            expectPUTPIECE_FROM_START1B = false;
            expectSTART2A = true;
        }

        if (expectPUTPIECE_FROM_START2A && (pieceType == PlayingPiece.SETTLEMENT) && (mes.getCoordinates() == ourPlayerData.getLastSettlementCoord()))
        {
            expectPUTPIECE_FROM_START2A = false;
            expectSTART2B = true;
        }

        if (expectPUTPIECE_FROM_START2B && (pieceType == PlayingPiece.ROAD) && (mes.getCoordinates() == ourPlayerData.getLastRoadCoord()))
        {
            expectPUTPIECE_FROM_START2B = false;
            expectPLAY = true;
        }

    }

    /**
     * Have the client ask to build this piece, unless we've already
     * been told by the server to not build it.
     * Calls {@link #buildingPlan}.pop().
     * Checks against {@link #whatWeFailedToBuild} to see if server has rejected this already.
     * Calls <tt>client.buyDevCard()</tt> or <tt>client.buildRequest()</tt>.
     * Sets {@link #waitingForDevCard}, or {@link #waitingForGameState} and
     * {@link #expectPLACING_SETTLEMENT} (etc).
     *
     * @param targetPiece  This should be the top piece of {@link #buildingPlan}.
     * @since 1.1.08
     */
    private void buildRequestPlannedPiece(PossiblePiece targetPiece)
    {
        buildingPlan.pop();
        D.ebugPrintln("$ POPPED " + targetPiece);
        lastMove = targetPiece;
        currentDRecorder = (currentDRecorder + 1) % 2;
        negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), targetPiece);

        switch (targetPiece.getType())
        {
        case PossiblePiece.CARD:
            client.buyDevCard(game);
            waitingForDevCard = true;

            break;

        case PossiblePiece.ROAD:
            waitingForGameState = true;
            counter = 0;
            expectPLACING_ROAD = true;
            whatWeWantToBuild = new Road(ourPlayerData, targetPiece.getCoordinates(), null);
            if (! whatWeWantToBuild.equals(whatWeFailedToBuild))
            {
                D.ebugPrintln("!!! BUILD REQUEST FOR A ROAD AT " + Integer.toHexString(targetPiece.getCoordinates()) + " !!!");
                client.buildRequest(game, PlayingPiece.ROAD);
            } else {
                // We already tried to build this.
                cancelWrongPiecePlacementLocal(whatWeWantToBuild);
                // cancel sets whatWeWantToBuild = null;
            }
            
            break;

        case PlayingPiece.SETTLEMENT:
            waitingForGameState = true;
            counter = 0;
            expectPLACING_SETTLEMENT = true;
            whatWeWantToBuild = new Settlement(ourPlayerData, targetPiece.getCoordinates(), null);
            if (! whatWeWantToBuild.equals(whatWeFailedToBuild))
            {
                D.ebugPrintln("!!! BUILD REQUEST FOR A SETTLEMENT " + Integer.toHexString(targetPiece.getCoordinates()) + " !!!");
                client.buildRequest(game, PlayingPiece.SETTLEMENT);
            } else {
                // We already tried to build this.
                cancelWrongPiecePlacementLocal(whatWeWantToBuild);
                // cancel sets whatWeWantToBuild = null;
            }
            
            break;

        case PlayingPiece.CITY:
            waitingForGameState = true;
            counter = 0;
            expectPLACING_CITY = true;
            whatWeWantToBuild = new City(ourPlayerData, targetPiece.getCoordinates(), null);
            if (! whatWeWantToBuild.equals(whatWeFailedToBuild))
            {
                D.ebugPrintln("!!! BUILD REQUEST FOR A CITY " + Integer.toHexString(targetPiece.getCoordinates()) + " !!!");
                client.buildRequest(game, PlayingPiece.CITY);
            } else {
                // We already tried to build this.
                cancelWrongPiecePlacementLocal(whatWeWantToBuild);
                // cancel sets whatWeWantToBuild = null;
            }

            break;
        }
    }

    /**
     * Plan the next building plan and target.
     * Should be called from {@link #run()} under these conditions: <BR>
     * (!expectPLACING_ROBBER && (buildingPlan.empty()) && (ourPlayerData.getResources().getTotal() > 1) && (failedBuildingAttempts < MAX_DENIED_BUILDING_PER_TURN))
     *<P>
     * Sets these fields/actions: <BR>
     *  {@link RobotDM#planStuff(int)} <BR>
     *  {@link #buildingPlan} <BR>
     *  {@link #lastTarget} <BR>
     *  {@link RobotNegotiator#setTargetPiece(int, PossiblePiece)}
     *
     * @since 1.1.08
     */
    private final void planBuilding()
    {
        decisionMaker.planStuff(robotParameters.getStrategyType());

        if (!buildingPlan.empty())
        {
            lastTarget = (PossiblePiece) buildingPlan.peek();
            negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), (PossiblePiece) buildingPlan.peek());
        }
    }

    /**
     * Handle a PLAYERELEMENT for this game.
     * Update a player's amount of a resource or a building type.
     *<P>
     * If this during the {@link Game#PLAY} state, then update the
     * {@link RobotNegotiator}'s is-selling flags.
     *<P>
     * If our player is losing a resource needed for the {@link #buildingPlan}, 
     * clear the plan if this is for the Special Building Phase (on the 6-player board).
     * In normal game play, we clear the building plan at the start of each turn.
     *<P>
     * Otherwise, only the game data is updated, nothing brain-specific.
     *
     * @since 1.1.08
     */
    private void handlePLAYERELEMENT(PlayerElement mes)
    {
        Player pl = game.getPlayer(mes.getPlayerNumber());

        switch (mes.getElementType())
        {
        case PlayerElement.ROADS:

            DisplaylessPlayerClient.handlePLAYERELEMENT_numPieces
                (mes, pl, PlayingPiece.ROAD);
            break;

        case PlayerElement.SETTLEMENTS:

            DisplaylessPlayerClient.handlePLAYERELEMENT_numPieces
                (mes, pl, PlayingPiece.SETTLEMENT);
            break;

        case PlayerElement.CITIES:

            DisplaylessPlayerClient.handlePLAYERELEMENT_numPieces
                (mes, pl, PlayingPiece.CITY);
            break;

        case PlayerElement.NUMKNIGHTS:

            // PLAYERELEMENT(NUMKNIGHTS) is sent after a Soldier card is played.
            DisplaylessPlayerClient.handlePLAYERELEMENT_numKnights
                (mes, pl, game);
            break;

        case PlayerElement.CLAY:

            handlePLAYERELEMENT_numRsrc
                (mes, pl, ResourceConstants.CLAY, "CLAY");
            break;

        case PlayerElement.ORE:
            
            handlePLAYERELEMENT_numRsrc
                (mes, pl, ResourceConstants.ORE, "ORE");
            break;

        case PlayerElement.SHEEP:

            handlePLAYERELEMENT_numRsrc
                (mes, pl, ResourceConstants.SHEEP, "SHEEP");
            break;

        case PlayerElement.WHEAT:

            handlePLAYERELEMENT_numRsrc
                (mes, pl, ResourceConstants.WHEAT, "WHEAT");
            break;

        case PlayerElement.WOOD:

            handlePLAYERELEMENT_numRsrc
                (mes, pl, ResourceConstants.WOOD, "WOOD");
            break;

        case PlayerElement.UNKNOWN:

            /**
             * Note: if losing unknown resources, we first
             * convert player's known resources to unknown resources,
             * then remove mes's unknown resources from player.
             */
            handlePLAYERELEMENT_numRsrc
                (mes, pl, ResourceConstants.UNKNOWN, "UNKNOWN");
            break;

        case PlayerElement.ASK_SPECIAL_BUILD:
            if (0 != mes.getValue())
            {
                try {
                    game.askSpecialBuild(pl.getPlayerNumber(), false);  // set per-player, per-game flags
                }
                catch (RuntimeException e) {}
            } else {
                pl.setAskedSpecialBuild(false);
            }
            break;

        }

        ///
        /// if this during the PLAY state, then update the is selling flags
        ///
        if (game.getGameState() == Game.PLAY)
        {
            negotiator.resetIsSelling();
        }
    }

    /**
     * Update a player's amount of a resource.
     *<ul>
     *<LI> If this is a {@link PlayerElement#LOSE} action, and the player does not have enough of that type,
     *     the rest are taken from the player's UNKNOWN amount.
     *<LI> If we are losing from type UNKNOWN,
     *     first convert player's known resources to unknown resources
     *     (individual amount information will be lost),
     *     then remove mes's unknown resources from player.
     *<LI> If this is a SET action, and it's for our own robot player,
     *     check the amount against {@link #ourPlayerData}, and debug print
     *     if they don't match already.
     *</ul>
     *<P>
     * If our player is losing a resource needed for the {@link #buildingPlan}, 
     * clear the plan if this is for the Special Building Phase (on the 6-player board).
     * In normal game play, we clear the building plan at the start of each turn.
     *<P>
     *
     * @param mes      Message with amount and action (SET/GAIN/LOSE)
     * @param pl       Player to update
     * @param rtype    Type of resource, like {@link ResourceConstants#CLAY}
     * @param rtypeStr Resource type name, for debugging
     */
    protected void handlePLAYERELEMENT_numRsrc
        (PlayerElement mes, Player pl, int rtype, String rtypeStr)
    {
        /**
         * for SET, check the amount of unknown resources against
         * what we think we know about our player.
         */
        if (D.ebugOn && (pl == ourPlayerData) && (mes.getAction() == PlayerElement.SET)) 
        {
            if (mes.getValue() != ourPlayerData.getResources().getAmount(rtype))
            {
                client.sendText(game, ">>> RSRC ERROR FOR " + rtypeStr
                    + ": " + mes.getValue() + " != " + ourPlayerData.getResources().getAmount(rtype));
            }
        }

        /**
         * Update game data.
         */
        DisplaylessPlayerClient.handlePLAYERELEMENT_numRsrc
            (mes, pl, rtype);

        /**
         * Clear building plan, if we just lost a resource we need.
         * Only necessary for Special Building Phase (6-player board),
         * because in normal game play, we clear the building plan
         * at the start of each turn.
         */
        if (waitingForSpecialBuild && (pl == ourPlayerData)
            && (mes.getAction() != PlayerElement.GAIN)
            && ! buildingPlan.isEmpty())
        {
            final PossiblePiece targetPiece = (PossiblePiece) buildingPlan.peek();
            final ResourceSet targetResources = PlayingPiece.getResourcesToBuild(targetPiece.getType());

            if (! ourPlayerData.getResources().contains(targetResources))
            {
                buildingPlan.clear();

                // The buildingPlan is clear, so we'll calculate
                // a new plan when our Special Building turn begins.
                // Don't clear decidedIfSpecialBuild flag, to prevent
                // needless plan calculation before our turn begins,
                // especially from multiple PLAYERELEMENT(LOSE),
                // as may happen for a discard.
            }
        }

    }

    /**
     * Run a newly placed settlement through the playerTrackers.
     *<P>
     * During initial board setup, settlements aren't tracked when placed.
     * They are deferred until their corresponding road placement, in case
     * a human player decides to cancel their settlement and place it elsewhere.
     *
     * During normal play, the settlements are tracked immediately when placed.
     *
     * (Code previously in body of the run method.)
     * Placing the code in its own method allows tracking that settlement when the
     * road's putPiece message arrives.
     *
     * @param newSettlement The newly placed settlement for the playerTrackers
     * @param isCancel Is this our own robot's settlement placement, rejected by the server?
     *     If so, this method call will cancel its placement within the game data / robot data. 
     */
    protected void trackNewSettlement(Settlement newSettlement, final boolean isCancel)
    {
        Iterator trackersIter;
        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();
            if (! isCancel)
                tracker.addNewSettlement(newSettlement, playerTrackers);
            else
                tracker.cancelWrongSettlement(newSettlement);
        }

        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();
            Iterator posRoadsIter = tracker.getPossibleRoads().values().iterator();

            while (posRoadsIter.hasNext())
            {
                ((PossibleRoad) posRoadsIter.next()).clearThreats();
            }

            Iterator posSetsIter = tracker.getPossibleSettlements().values().iterator();

            while (posSetsIter.hasNext())
            {
                ((PossibleSettlement) posSetsIter.next()).clearThreats();
            }
        }

        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();
            tracker.updateThreats(playerTrackers);
        }

        if (isCancel)
        {
            return;  // <--- Early return, nothing else to do ---
        }

        ///
        /// see if this settlement bisected someone else's road
        ///
        int[] roadCount = { 0, 0, 0, 0, 0, 0 };  // Length should be Game.MAXPLAYERS
        Board board = game.getBoard();
        Enumeration adjEdgeEnum = board.getAdjacentEdgesToNode(newSettlement.getCoordinates()).elements();

        while (adjEdgeEnum.hasMoreElements())
        {
            Integer adjEdge = (Integer) adjEdgeEnum.nextElement();
            Enumeration roadEnum = board.getRoads().elements();

            while (roadEnum.hasMoreElements())
            {
                Road road = (Road) roadEnum.nextElement();

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
                                PlayerTracker tracker = (PlayerTracker) trackersIter.next();

                                if (tracker.getPlayer().getPlayerNumber() == road.getPlayer().getPlayerNumber())
                                {
                                    //D.ebugPrintln("$$ updating LR Value for player "+tracker.getPlayer().getPlayerNumber());
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
        
        int pNum = newSettlement.getPlayer().getPlayerNumber();

        ///
        /// update the speedups from possible settlements
        ///
        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();

            if (tracker.getPlayer().getPlayerNumber() == pNum)
            {
                Iterator posSetsIter = tracker.getPossibleSettlements().values().iterator();

                while (posSetsIter.hasNext())
                {
                    ((PossibleSettlement) posSetsIter.next()).updateSpeedup();
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
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();

            if (tracker.getPlayer().getPlayerNumber() == pNum)
            {
                Iterator posCitiesIter = tracker.getPossibleCities().values().iterator();

                while (posCitiesIter.hasNext())
                {
                    ((PossibleCity) posCitiesIter.next()).updateSpeedup();
                }

                break;
            }
        }
    }

    /**
     * Run a newly placed city through the PlayerTrackers.
     * @param newCity  The newly placed city
     * @param isCancel Is this our own robot's city placement, rejected by the server?
     *     If so, this method call will cancel its placement within the game data / robot data. 
     */
    private void trackNewCity(City newCity, final boolean isCancel)
    {
        Iterator trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();

            if (tracker.getPlayer().getPlayerNumber() == newCity.getPlayer().getPlayerNumber())
            {
                if (! isCancel)
                    tracker.addOurNewCity(newCity);
                else
                    tracker.cancelWrongCity(newCity);
                break;
            }
        }

        if (isCancel)
        {
            return;  // <--- Early return, nothing else to do ---
        }

        ///
        /// update the speedups from possible settlements
        ///
        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();

            if (tracker.getPlayer().getPlayerNumber() == newCity.getPlayer().getPlayerNumber())
            {
                Iterator posSetsIter = tracker.getPossibleSettlements().values().iterator();

                while (posSetsIter.hasNext())
                {
                    ((PossibleSettlement) posSetsIter.next()).updateSpeedup();
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
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();

            if (tracker.getPlayer().getPlayerNumber() == newCity.getPlayer().getPlayerNumber())
            {
                Iterator posCitiesIter = tracker.getPossibleCities().values().iterator();

                while (posCitiesIter.hasNext())
                {
                    ((PossibleCity) posCitiesIter.next()).updateSpeedup();
                }

                break;
            }
        }
    }

    /**
     * Run a newly placed road through the playerTrackers.
     * 
     * @param newRoad  The newly placed road
     * @param isCancel Is this our own robot's road placement, rejected by the server?
     *     If so, this method call will cancel its placement within the game data / robot data. 
     */
    protected void trackNewRoad(Road newRoad, final boolean isCancel)
    {
        Iterator trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();
            tracker.takeMonitor();

            try
            {
                if (! isCancel)
                    tracker.addNewRoad(newRoad, playerTrackers);
                else
                    tracker.cancelWrongRoad(newRoad);
            }
            catch (Exception e)
            {
                tracker.releaseMonitor();
                if (alive)
                {
                    System.out.println("Exception caught - " + e);
                    e.printStackTrace();
                }
            }

            tracker.releaseMonitor();
        }

        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();
            tracker.takeMonitor();

            try
            {
                Iterator posRoadsIter = tracker.getPossibleRoads().values().iterator();

                while (posRoadsIter.hasNext())
                {
                    ((PossibleRoad) posRoadsIter.next()).clearThreats();
                }

                Iterator posSetsIter = tracker.getPossibleSettlements().values().iterator();

                while (posSetsIter.hasNext())
                {
                    ((PossibleSettlement) posSetsIter.next()).clearThreats();
                }
            }
            catch (Exception e)
            {
                tracker.releaseMonitor();
                if (alive)
                {
                    System.out.println("Exception caught - " + e);
                    e.printStackTrace();
                }
            }

            tracker.releaseMonitor();
        }

        ///
        /// update LR values and ETA
        ///
        trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();
            tracker.updateThreats(playerTrackers);
            tracker.takeMonitor();

            try
            {
                if (tracker.getPlayer().getPlayerNumber() == newRoad.getPlayer().getPlayerNumber())
                {
                    //D.ebugPrintln("$$ updating LR Value for player "+tracker.getPlayer().getPlayerNumber());
                    //tracker.updateLRValues();
                }

                //tracker.recalcLongestRoadETA();
            }
            catch (Exception e)
            {
                tracker.releaseMonitor();
                if (alive)
                {
                    System.out.println("Exception caught - " + e);
                    e.printStackTrace();
                }
            }

            tracker.releaseMonitor();
        }
    }
    
    /**
     *  We've asked for an illegal piece placement.
     *  Cancel and invalidate this planned piece, make a new plan.
     *  If {@link Game#isSpecialBuilding()}, will set variables to
     *  force the end of our special building turn.
     *  Also handles illegal requests to buy development cards
     *  (piece type -2 in {@link CancelBuildRequest}).
     *<P>
     *  This method increments {@link #failedBuildingAttempts},
     *  but won't leave the game if we've failed too many times.
     *  The brain's run loop should make that decision.
     *
     * @param mes Cancelmessage from server, including piece type
     */
    protected void cancelWrongPiecePlacement(CancelBuildRequest mes)
    {
        final boolean cancelBuyDevCard = (mes.getPieceType() == -2);
        if (cancelBuyDevCard)
        {
            waitingForDevCard = false;
        } else {
            whatWeFailedToBuild = whatWeWantToBuild;
            ++failedBuildingAttempts;
        }

        final int gameState = game.getGameState();

        /**
         * if true, server denied us due to resources, not due to building plan.
         */
        final boolean gameStateIsPLAY1 = (gameState == Game.PLAY1);

        if (! (gameStateIsPLAY1 || cancelBuyDevCard))
        {
            int coord = -1;
            switch (gameState)
            {
            case Game.START1A:
            case Game.START1B:
            case Game.START2A:
            case Game.START2B:
                coord = lastStartingPieceCoord;
                break;

            default:
                if (whatWeWantToBuild != null)
                    coord = whatWeWantToBuild.getCoordinates();
            }
            if (coord != -1)
            {
                PlayingPiece cancelPiece;
    
                /**
                 * First, invalidate that piece in trackers, so we don't try again to
                 * build it. If we treat it like another player's new placement, we
                 * can remove any of our planned pieces depending on this one.
                 */
                switch (mes.getPieceType())
                {
                case PlayingPiece.ROAD:
                    cancelPiece = new Road(dummyCancelPlayerData, coord, null);
                    break;
    
                case PlayingPiece.SETTLEMENT:
                    cancelPiece = new Settlement(dummyCancelPlayerData, coord, null);
                    break;
    
                case PlayingPiece.CITY:
                    cancelPiece = new City(dummyCancelPlayerData, coord, null);
                    break;
    
                default:
                    cancelPiece = null;  // To satisfy javac
                }
    
                cancelWrongPiecePlacementLocal(cancelPiece);
            }
        } else {
            /**
             *  stop trying to build it now, but don't prevent
             *  us from trying later to build it.
             */ 
            whatWeWantToBuild = null;
            buildingPlan.clear();
        }

        /**
         * we've invalidated that piece in trackers.
         * - clear whatWeWantToBuild, buildingPlan
         * - set expectPLAY1, waitingForGameState
         * - reset counter = 0
         * - send CANCEL _to_ server, so all players get PLAYERELEMENT & GAMESTATE(PLAY1) messages.
         * - wait for the play1 message, then can re-plan another piece.
         * - update javadoc of this method (TODO)
         */

        if (gameStateIsPLAY1 || game.isSpecialBuilding())
        {
            // Shouldn't have asked to build this piece at this time.
            // End our confusion by ending our current turn. Can re-plan on next turn.
            failedBuildingAttempts = MAX_DENIED_BUILDING_PER_TURN;
            expectPLACING_ROAD = false;
            expectPLACING_SETTLEMENT = false;
            expectPLACING_CITY = false;
            decidedIfSpecialBuild = true;
            waitingForGameState = false;  // otherwise, will wait forever for PLACING_ state
        }
        else if (gameState <= Game.START2B)
        {
            switch (gameState)
            {
            case Game.START1A:
                expectPUTPIECE_FROM_START1A = false;
                expectSTART1A = true;
                break;

            case Game.START1B:
                expectPUTPIECE_FROM_START1B = false;
                expectSTART1B = true;
                break;

            case Game.START2A:
                expectPUTPIECE_FROM_START2A = false;
                expectSTART2A = true;
                break;

            case Game.START2B:
                expectPUTPIECE_FROM_START2B = false;
                expectSTART2B = true;
                break;
            }
            waitingForGameState = false;
            // The run loop will check if failedBuildingAttempts > (2 * MAX_DENIED_BUILDING_PER_TURN).
            // This bot will leave the game there if it can't recover.
        } else {
            expectPLAY1 = true;
            waitingForGameState = true;
            counter = 0;
            client.cancelBuildRequest(game, mes.getPieceType());
            // Now wait for the play1 message, then can re-plan another piece.
        }
    }

    /**
     * Remove our incorrect piece placement, it's been rejected by the server.
     * Take this piece out of trackers, without sending any response back to the server.
     *<P>
     * This method invalidates that piece in trackers, so we don't try again to
     * build it. Since we treat it like another player's new placement, we
     * can remove any of our planned pieces depending on this one.
     *<P>
     * Also calls {@link Player#clearPotentialSettlement(int)},
     * clearPotentialRoad, or clearPotentialCity.
     *
     * @param cancelPiece Type and coordinates of the piece to cancel; null is allowed but not very useful.
     */
    protected void cancelWrongPiecePlacementLocal(PlayingPiece cancelPiece)
    {
        if (cancelPiece != null)
        {
            final int coord = cancelPiece.getCoordinates();

            switch (cancelPiece.getType())
            {
            case PlayingPiece.ROAD:
                trackNewRoad((Road) cancelPiece, true);
                ourPlayerData.clearPotentialRoad(coord);
                if (game.getGameState() <= Game.START2B)
                {
                    // needed for placeInitRoad() calculations
                    ourPlayerData.clearPotentialSettlement(lastStartingRoadTowardsNode);
                }
                break;

            case PlayingPiece.SETTLEMENT:
                trackNewSettlement((Settlement) cancelPiece, true);
                ourPlayerData.clearPotentialSettlement(coord);
                break;

            case PlayingPiece.CITY:
                trackNewCity((City) cancelPiece, true);
                ourPlayerData.clearPotentialCity(coord);
                break;
            }
        }

        whatWeWantToBuild = null;
        buildingPlan.clear();
    }

    /**
     * kill this brain
     */
    public void kill()
    {
        alive = false;

        try
        {
            gameEventQ.put(null);
        }
        catch (Exception exc) {}
    }

    /**
     * pause for a bit.
     *<P>
     * In a 6-player game, pause only 75% as long, to shorten the overall game delay,
     * except if {@link #waitingForTradeResponse}.
     * This is indicated by the {@link #pauseFaster} flag.
     *
     * @param msec  number of milliseconds to pause
     */
    public void pause(int msec)
    {
        if (pauseFaster && ! waitingForTradeResponse)
            msec = (msec / 2) + (msec / 4);

        try
        {
            Thread.yield();
            Thread.sleep(msec);
        }
        catch (InterruptedException exc) {}
    }

    /**
     * figure out where to place the two settlements
     */
    protected void planInitialSettlements()
    {
        D.ebugPrintln("--- planInitialSettlements");

        int[] rolls;
        Enumeration hexes;
        int speed;
        boolean allTheWay;
        firstSettlement = 0;
        secondSettlement = 0;

        int bestSpeed = 4 * BuildingSpeedEstimate.DEFAULT_ROLL_LIMIT;
        Board board = game.getBoard();
        ResourceSet emptySet = new ResourceSet();
        PlayerNumbers playerNumbers = new PlayerNumbers(board.getBoardEncodingFormat());
        int probTotal;
        int bestProbTotal;
        boolean[] ports = new boolean[Board.WOOD_PORT + 1];
        BuildingSpeedEstimate estimate = new BuildingSpeedEstimate();
        int[] prob = NumberProbabilities.INT_VALUES;

        bestProbTotal = 0;

        for (int firstNode = board.getMinNode(); firstNode <= Board.MAXNODE; firstNode++)
        {
            if (ourPlayerData.isPotentialSettlement(firstNode))
            {
                Integer firstNodeInt = new Integer(firstNode);

                //
                // this is just for testing purposes
                //
                D.ebugPrintln("FIRST NODE -----------");
                D.ebugPrintln("firstNode = " + board.nodeCoordToString(firstNode));
                D.ebugPrint("numbers:[");
                playerNumbers.clear();
                probTotal = 0;
                hexes = Board.getAdjacentHexesToNode(firstNode).elements();

                while (hexes.hasMoreElements())
                {
                    Integer hex = (Integer) hexes.nextElement();
                    int number = board.getNumberOnHexFromCoord(hex.intValue());
                    int resource = board.getHexTypeFromCoord(hex.intValue());
                    playerNumbers.addNumberForResource(number, resource, hex.intValue());
                    probTotal += prob[number];
                    D.ebugPrint(number + " ");
                }

                D.ebugPrintln("]");
                D.ebugPrint("ports: ");

                for (int portType = Board.MISC_PORT;
                        portType <= Board.WOOD_PORT; portType++)
                {
                    if (board.getPortCoordinates(portType).contains(firstNodeInt))
                    {
                        ports[portType] = true;
                    }
                    else
                    {
                        ports[portType] = false;
                    }

                    D.ebugPrint(ports[portType] + "  ");
                }

                D.ebugPrintln();
                D.ebugPrintln("probTotal = " + probTotal);
                estimate.recalculateEstimates(playerNumbers);
                speed = 0;
                allTheWay = false;

                try
                {
                    speed += estimate.calculateRollsFast(emptySet, Game.SETTLEMENT_SET, 300, ports).getRolls();
                    speed += estimate.calculateRollsFast(emptySet, Game.CITY_SET, 300, ports).getRolls();
                    speed += estimate.calculateRollsFast(emptySet, Game.CARD_SET, 300, ports).getRolls();
                    speed += estimate.calculateRollsFast(emptySet, Game.ROAD_SET, 300, ports).getRolls();
                }
                catch (CutoffExceededException e) {}

                rolls = estimate.getEstimatesFromNothingFast(ports, 300);
                D.ebugPrint(" road: " + rolls[BuildingSpeedEstimate.ROAD]);
                D.ebugPrint(" stlmt: " + rolls[BuildingSpeedEstimate.SETTLEMENT]);
                D.ebugPrint(" city: " + rolls[BuildingSpeedEstimate.CITY]);
                D.ebugPrintln(" card: " + rolls[BuildingSpeedEstimate.CARD]);
                D.ebugPrintln("speed = " + speed);

                //
                // end test
                //
                for (int secondNode = firstNode + 1; secondNode <= Board.MAXNODE;
                        secondNode++)
                {
                    if ((ourPlayerData.isPotentialSettlement(secondNode)) && (! board.getAdjacentNodesToNode(secondNode).contains(firstNodeInt)))
                    {
                        D.ebugPrintln("firstNode = " + board.nodeCoordToString(firstNode));
                        D.ebugPrintln("secondNode = " + board.nodeCoordToString(secondNode));

                        Integer secondNodeInt = new Integer(secondNode);

                        /**
                         * get the numbers for these settlements
                         */
                        D.ebugPrint("numbers:[");
                        playerNumbers.clear();
                        probTotal = 0;
                        hexes = Board.getAdjacentHexesToNode(firstNode).elements();

                        while (hexes.hasMoreElements())
                        {
                            Integer hex = (Integer) hexes.nextElement();
                            int number = board.getNumberOnHexFromCoord(hex.intValue());
                            int resource = board.getHexTypeFromCoord(hex.intValue());
                            playerNumbers.addNumberForResource(number, resource, hex.intValue());
                            probTotal += prob[number];
                            D.ebugPrint(number + " ");
                        }

                        D.ebugPrint("] [");
                        hexes = Board.getAdjacentHexesToNode(secondNode).elements();

                        while (hexes.hasMoreElements())
                        {
                            Integer hex = (Integer) hexes.nextElement();
                            int number = board.getNumberOnHexFromCoord(hex.intValue());
                            int resource = board.getHexTypeFromCoord(hex.intValue());
                            playerNumbers.addNumberForResource(number, resource, hex.intValue());
                            probTotal += prob[number];
                            D.ebugPrint(number + " ");
                        }

                        D.ebugPrintln("]");

                        /**
                         * see if the settlements are on any ports
                         */
                        D.ebugPrint("ports: ");

                        for (int portType = Board.MISC_PORT;
                                portType <= Board.WOOD_PORT; portType++)
                        {
                            if ((board.getPortCoordinates(portType).contains(firstNodeInt)) || (board.getPortCoordinates(portType).contains(secondNodeInt)))
                            {
                                ports[portType] = true;
                            }
                            else
                            {
                                ports[portType] = false;
                            }

                            D.ebugPrint(ports[portType] + "  ");
                        }

                        D.ebugPrintln();
                        D.ebugPrintln("probTotal = " + probTotal);

                        /**
                         * estimate the building speed for this pair
                         */
                        estimate.recalculateEstimates(playerNumbers);
                        speed = 0;
                        allTheWay = false;

                        try
                        {
                            speed += estimate.calculateRollsFast(emptySet, Game.SETTLEMENT_SET, bestSpeed, ports).getRolls();

                            if (speed < bestSpeed)
                            {
                                speed += estimate.calculateRollsFast(emptySet, Game.CITY_SET, bestSpeed, ports).getRolls();

                                if (speed < bestSpeed)
                                {
                                    speed += estimate.calculateRollsFast(emptySet, Game.CARD_SET, bestSpeed, ports).getRolls();

                                    if (speed < bestSpeed)
                                    {
                                        speed += estimate.calculateRollsFast(emptySet, Game.ROAD_SET, bestSpeed, ports).getRolls();
                                        allTheWay = true;
                                    }
                                }
                            }
                        }
                        catch (CutoffExceededException e)
                        {
                            speed = bestSpeed;
                        }

                        rolls = estimate.getEstimatesFromNothingFast(ports, bestSpeed);
                        D.ebugPrint(" road: " + rolls[BuildingSpeedEstimate.ROAD]);
                        D.ebugPrint(" stlmt: " + rolls[BuildingSpeedEstimate.SETTLEMENT]);
                        D.ebugPrint(" city: " + rolls[BuildingSpeedEstimate.CITY]);
                        D.ebugPrintln(" card: " + rolls[BuildingSpeedEstimate.CARD]);
                        D.ebugPrintln("allTheWay = " + allTheWay);
                        D.ebugPrintln("speed = " + speed);

                        /**
                         * keep the settlements with the best speed
                         */
                        if (speed < bestSpeed)
                        {
                            firstSettlement = firstNode;
                            secondSettlement = secondNode;
                            bestSpeed = speed;
                            bestProbTotal = probTotal;
                            D.ebugPrintln("bestSpeed = " + bestSpeed);
                            D.ebugPrintln("bestProbTotal = " + bestProbTotal);
                        }
                        else if ((speed == bestSpeed) && allTheWay)
                        {
                            if (probTotal > bestProbTotal)
                            {
                                D.ebugPrintln("Equal speed, better prob");
                                firstSettlement = firstNode;
                                secondSettlement = secondNode;
                                bestSpeed = speed;
                                bestProbTotal = probTotal;
                                D.ebugPrintln("firstSettlement = " + Integer.toHexString(firstSettlement));
                                D.ebugPrintln("secondSettlement = " + Integer.toHexString(secondSettlement));
                                D.ebugPrintln("bestSpeed = " + bestSpeed);
                                D.ebugPrintln("bestProbTotal = " + bestProbTotal);
                            }
                        }
                    }
                }
            }
        }

        /**
         * choose which settlement to place first
         */
        playerNumbers.clear();
        hexes = Board.getAdjacentHexesToNode(firstSettlement).elements();

        while (hexes.hasMoreElements())
        {
            int hex = ((Integer) hexes.nextElement()).intValue();
            int number = board.getNumberOnHexFromCoord(hex);
            int resource = board.getHexTypeFromCoord(hex);
            playerNumbers.addNumberForResource(number, resource, hex);
        }

        Integer firstSettlementInt = new Integer(firstSettlement);

        for (int portType = Board.MISC_PORT; portType <= Board.WOOD_PORT;
                portType++)
        {
            if (board.getPortCoordinates(portType).contains(firstSettlementInt))
            {
                ports[portType] = true;
            }
            else
            {
                ports[portType] = false;
            }
        }

        estimate.recalculateEstimates(playerNumbers);

        int firstSpeed = 0;
        int cutoff = 100;

        try
        {
            firstSpeed += estimate.calculateRollsFast(emptySet, Game.SETTLEMENT_SET, cutoff, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            firstSpeed += cutoff;
        }

        try
        {
            firstSpeed += estimate.calculateRollsFast(emptySet, Game.CITY_SET, cutoff, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            firstSpeed += cutoff;
        }

        try
        {
            firstSpeed += estimate.calculateRollsFast(emptySet, Game.CARD_SET, cutoff, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            firstSpeed += cutoff;
        }

        try
        {
            firstSpeed += estimate.calculateRollsFast(emptySet, Game.ROAD_SET, cutoff, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            firstSpeed += cutoff;
        }

        playerNumbers.clear();
        hexes = Board.getAdjacentHexesToNode(secondSettlement).elements();

        while (hexes.hasMoreElements())
        {
            int hex = ((Integer) hexes.nextElement()).intValue();
            int number = board.getNumberOnHexFromCoord(hex);
            int resource = board.getHexTypeFromCoord(hex);
            playerNumbers.addNumberForResource(number, resource, hex);
        }

        Integer secondSettlementInt = new Integer(secondSettlement);

        for (int portType = Board.MISC_PORT; portType <= Board.WOOD_PORT;
                portType++)
        {
            if (board.getPortCoordinates(portType).contains(secondSettlementInt))
            {
                ports[portType] = true;
            }
            else
            {
                ports[portType] = false;
            }
        }

        estimate.recalculateEstimates(playerNumbers);

        int secondSpeed = 0;

        try
        {
            secondSpeed += estimate.calculateRollsFast(emptySet, Game.SETTLEMENT_SET, bestSpeed, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            secondSpeed += cutoff;
        }

        try
        {
            secondSpeed += estimate.calculateRollsFast(emptySet, Game.CITY_SET, bestSpeed, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            secondSpeed += cutoff;
        }

        try
        {
            secondSpeed += estimate.calculateRollsFast(emptySet, Game.CARD_SET, bestSpeed, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            secondSpeed += cutoff;
        }

        try
        {
            secondSpeed += estimate.calculateRollsFast(emptySet, Game.ROAD_SET, bestSpeed, ports).getRolls();
        }
        catch (CutoffExceededException e)
        {
            secondSpeed += cutoff;
        }

        if (firstSpeed > secondSpeed)
        {
            int tmp = firstSettlement;
            firstSettlement = secondSettlement;
            secondSettlement = tmp;
        }

        D.ebugPrintln(board.nodeCoordToString(firstSettlement) + ":" + firstSpeed + ", " + board.nodeCoordToString(secondSettlement) + ":" + secondSpeed);
    }

    /**
     * figure out where to place the second settlement
     */
    protected void planSecondSettlement()
    {
        D.ebugPrintln("--- planSecondSettlement");

        int bestSpeed = 4 * BuildingSpeedEstimate.DEFAULT_ROLL_LIMIT;
        Board board = game.getBoard();
        ResourceSet emptySet = new ResourceSet();
        PlayerNumbers playerNumbers = new PlayerNumbers(board.getBoardEncodingFormat());
        boolean[] ports = new boolean[Board.WOOD_PORT + 1];
        BuildingSpeedEstimate estimate = new BuildingSpeedEstimate();
        int probTotal;
        int bestProbTotal;
        final int[] prob = NumberProbabilities.INT_VALUES;
        final int firstNode = firstSettlement;
        final Integer firstNodeInt = new Integer(firstNode);

        bestProbTotal = 0;
        secondSettlement = -1;

        for (int secondNode = board.getMinNode(); secondNode <= Board.MAXNODE; secondNode++)
        {
            if ((ourPlayerData.isPotentialSettlement(secondNode)) && (! board.getAdjacentNodesToNode(secondNode).contains(firstNodeInt)))
            {
                Integer secondNodeInt = new Integer(secondNode);

                /**
                 * get the numbers for these settlements
                 */
                D.ebugPrint("numbers: ");
                playerNumbers.clear();
                probTotal = 0;

                Enumeration hexes = Board.getAdjacentHexesToNode(firstNode).elements();

                while (hexes.hasMoreElements())
                {
                    final int hex = ((Integer) hexes.nextElement()).intValue();
                    int number = board.getNumberOnHexFromCoord(hex);
                    int resource = board.getHexTypeFromCoord(hex);
                    playerNumbers.addNumberForResource(number, resource, hex);
                    probTotal += prob[number];
                    D.ebugPrint(number + " ");
                }

                hexes = Board.getAdjacentHexesToNode(secondNode).elements();

                while (hexes.hasMoreElements())
                {
                    final int hex = ((Integer) hexes.nextElement()).intValue();
                    int number = board.getNumberOnHexFromCoord(hex);
                    int resource = board.getHexTypeFromCoord(hex);
                    playerNumbers.addNumberForResource(number, resource, hex);
                    probTotal += prob[number];
                    D.ebugPrint(number + " ");
                }

                /**
                 * see if the settlements are on any ports
                 */
                D.ebugPrint("ports: ");

                for (int portType = Board.MISC_PORT;
                        portType <= Board.WOOD_PORT; portType++)
                {
                    if ((board.getPortCoordinates(portType).contains(firstNodeInt)) || (board.getPortCoordinates(portType).contains(secondNodeInt)))
                    {
                        ports[portType] = true;
                    }
                    else
                    {
                        ports[portType] = false;
                    }

                    D.ebugPrint(ports[portType] + "  ");
                }

                D.ebugPrintln();
                D.ebugPrintln("probTotal = " + probTotal);

                /**
                 * estimate the building speed for this pair
                 */
                estimate.recalculateEstimates(playerNumbers);

                int speed = 0;

                try
                {
                    speed += estimate.calculateRollsFast(emptySet, Game.SETTLEMENT_SET, bestSpeed, ports).getRolls();

                    if (speed < bestSpeed)
                    {
                        speed += estimate.calculateRollsFast(emptySet, Game.CITY_SET, bestSpeed, ports).getRolls();

                        if (speed < bestSpeed)
                        {
                            speed += estimate.calculateRollsFast(emptySet, Game.CARD_SET, bestSpeed, ports).getRolls();

                            if (speed < bestSpeed)
                            {
                                speed += estimate.calculateRollsFast(emptySet, Game.ROAD_SET, bestSpeed, ports).getRolls();
                            }
                        }
                    }
                }
                catch (CutoffExceededException e)
                {
                    speed = bestSpeed;
                }

                D.ebugPrintln(Integer.toHexString(firstNode) + ", " + Integer.toHexString(secondNode) + ":" + speed);

                /**
                 * keep the settlements with the best speed
                 */
                if ((speed < bestSpeed) || (secondSettlement < 0))
                {
                    firstSettlement = firstNode;
                    secondSettlement = secondNode;
                    bestSpeed = speed;
                    bestProbTotal = probTotal;
                    D.ebugPrintln("firstSettlement = " + Integer.toHexString(firstSettlement));
                    D.ebugPrintln("secondSettlement = " + Integer.toHexString(secondSettlement));

                    int[] rolls = estimate.getEstimatesFromNothingFast(ports);
                    D.ebugPrint("road: " + rolls[BuildingSpeedEstimate.ROAD]);
                    D.ebugPrint(" stlmt: " + rolls[BuildingSpeedEstimate.SETTLEMENT]);
                    D.ebugPrint(" city: " + rolls[BuildingSpeedEstimate.CITY]);
                    D.ebugPrintln(" card: " + rolls[BuildingSpeedEstimate.CARD]);
                    D.ebugPrintln("bestSpeed = " + bestSpeed);
                }
                else if (speed == bestSpeed)
                {
                    if (probTotal > bestProbTotal)
                    {
                        firstSettlement = firstNode;
                        secondSettlement = secondNode;
                        bestSpeed = speed;
                        bestProbTotal = probTotal;
                        D.ebugPrintln("firstSettlement = " + Integer.toHexString(firstSettlement));
                        D.ebugPrintln("secondSettlement = " + Integer.toHexString(secondSettlement));

                        int[] rolls = estimate.getEstimatesFromNothingFast(ports);
                        D.ebugPrint("road: " + rolls[BuildingSpeedEstimate.ROAD]);
                        D.ebugPrint(" stlmt: " + rolls[BuildingSpeedEstimate.SETTLEMENT]);
                        D.ebugPrint(" city: " + rolls[BuildingSpeedEstimate.CITY]);
                        D.ebugPrintln(" card: " + rolls[BuildingSpeedEstimate.CARD]);
                        D.ebugPrintln("bestSpeed = " + bestSpeed);
                    }
                }
            }
        }
    }

    /**
     * place planned first settlement
     */
    protected void placeFirstSettlement()
    {
        //D.ebugPrintln("BUILD REQUEST FOR SETTLEMENT AT "+Integer.toHexString(firstSettlement));
        pause(500);
        lastStartingPieceCoord = firstSettlement;
        client.putPiece(game, new Settlement(ourPlayerData, firstSettlement, null));
        pause(1000);
    }

    /**
     * place planned second settlement
     */
    protected void placeSecondSettlement()
    {
        if (secondSettlement == -1)
        {
            // This could mean that the server (incorrectly) asked us to
            // place another second settlement, after we've cleared the
            // potentialSettlements contents.
            System.err.println("robot assert failed: secondSettlement -1, " + ourPlayerData.getName() + " leaving game " + game.getName());
            failedBuildingAttempts = 2 + (2 * MAX_DENIED_BUILDING_PER_TURN);
            waitingForGameState = false;
            return;
        }

        //D.ebugPrintln("BUILD REQUEST FOR SETTLEMENT AT "+Integer.toHexString(secondSettlement));
        pause(500);
        lastStartingPieceCoord = secondSettlement;
        client.putPiece(game, new Settlement(ourPlayerData, secondSettlement, null));
        pause(1000);
    }

    /**
     * Plan and place a road attached to our most recently placed initial settlement,
     * in game states {@link Game#START1B START1B}, {@link Game#START2B START2B}.
     *<P>
     * Road choice is based on the best nearby potential settlements, and doesn't
     * directly check {@link Player#isPotentialRoad(int) ourPlayerData.isPotentialRoad(edgeCoord)}.
     * If the server rejects our road choice, then {@link #cancelWrongPiecePlacementLocal(PlayingPiece)}
     * will need to know which settlement node we were aiming for,
     * and call {@link Player#clearPotentialSettlement(int) ourPlayerData.clearPotentialSettlement(nodeCoord)}.
     * The {@link #lastStartingRoadTowardsNode} field holds this coordinate.
     */
    public void placeInitRoad()
    {
        final int settlementNode = ourPlayerData.getLastSettlementCoord();

        /**
         * Score the nearby nodes to build road towards: Key = coord Integer; value = Integer score towards "best" node.
         */
        Hashtable twoAway = new Hashtable();

        D.ebugPrintln("--- placeInitRoad");

        /**
         * look at all of the nodes that are 2 away from the
         * last settlement, and pick the best one
         */
        Board board = game.getBoard();
        int tmp;

        tmp = settlementNode - 0x20;  // NW direction (northwest)

        if (board.isNodeOnBoard(tmp) && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode + 0x02;  // NE

        if (board.isNodeOnBoard(tmp) && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode + 0x22;  // E

        if (board.isNodeOnBoard(tmp) && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode + 0x20;  // SE

        if (board.isNodeOnBoard(tmp) && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode - 0x02;  // SW

        if (board.isNodeOnBoard(tmp) && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        tmp = settlementNode - 0x22;  // W direction (west)

        if (board.isNodeOnBoard(tmp) && ourPlayerData.isPotentialSettlement(tmp))
        {
            twoAway.put(new Integer(tmp), new Integer(0));
        }

        scoreNodesForSettlements(twoAway, 3, 5, 10);

        D.ebugPrintln("Init Road for " + client.getNickname());

        /**
         * create a dummy player to calculate possible places to build
         * taking into account where other players will build before
         * we can.
         */
        Player dummy = new Player(ourPlayerData.getPlayerNumber(), game);

        if (game.getGameState() == Game.START1B)
        {
            /**
             * do a look ahead so we don't build toward a place
             * where someone else will build first.
             */
            int numberOfBuilds = numberOfEnemyBuilds();
            D.ebugPrintln("Other players will build " + numberOfBuilds + " settlements before I get to build again.");

            if (numberOfBuilds > 0)
            {
                /**
                 * rule out where other players are going to build
                 */
                Hashtable allNodes = new Hashtable();
                final int minNode = board.getMinNode();

                for (int i = minNode; i <= Board.MAXNODE; i++)
                {
                    if (ourPlayerData.isPotentialSettlement(i))
                    {
                        D.ebugPrintln("-- potential settlement at " + Integer.toHexString(i));
                        allNodes.put(new Integer(i), new Integer(0));
                    }
                }

                /**
                 * favor spots with the most high numbers
                 */
                bestSpotForNumbers(allNodes, 100);

                /**
                 * favor spots near good ports
                 */
                /**
                 * check 3:1 ports
                 */
                Vector miscPortNodes = game.getBoard().getPortCoordinates(Board.MISC_PORT);
                bestSpot2AwayFromANodeSet(allNodes, miscPortNodes, 5);

                /**
                 * check out good 2:1 ports
                 */
                for (int portType = Board.CLAY_PORT;
                        portType <= Board.WOOD_PORT; portType++)
                {
                    /**
                     * if the chances of rolling a number on the resource is better than 1/3,
                     * then it's worth looking at the port
                     */
                    if (resourceEstimates[portType] > 33)
                    {
                        Vector portNodes = game.getBoard().getPortCoordinates(portType);
                        int portWeight = (resourceEstimates[portType] * 10) / 56;
                        bestSpot2AwayFromANodeSet(allNodes, portNodes, portWeight);
                    }
                }

                /*
                 * create a list of potential settlements that takes into account
                 * where other players will build
                 */
                Vector psList = new Vector();

                for (int j = minNode; j <= Board.MAXNODE; j++)
                {
                    if (ourPlayerData.isPotentialSettlement(j))
                    {
                        D.ebugPrintln("- potential settlement at " + Integer.toHexString(j));
                        psList.addElement(new Integer(j));
                    }
                }

                dummy.setPotentialSettlements(psList);

                for (int builds = 0; builds < numberOfBuilds; builds++)
                {
                    BoardNodeScorePair bestNodePair = new BoardNodeScorePair(0, 0);
                    Enumeration nodesEnum = allNodes.keys();

                    while (nodesEnum.hasMoreElements())
                    {
                        Integer nodeCoord = (Integer) nodesEnum.nextElement();
                        final int score = ((Integer) allNodes.get(nodeCoord)).intValue();
                        D.ebugPrintln("NODE = " + Integer.toHexString(nodeCoord.intValue()) + " SCORE = " + score);

                        if (bestNodePair.getScore() < score)
                        {
                            bestNodePair.setScore(score);
                            bestNodePair.setNode(nodeCoord.intValue());
                        }
                    }

                    /**
                     * pretend that someone has built a settlement on the best spot
                     */
                    dummy.updatePotentials(new Settlement(ourPlayerData, bestNodePair.getNode(), null));

                    /**
                     * remove this spot from the list of best spots
                     */
                    allNodes.remove(new Integer(bestNodePair.getNode()));
                }
            }
        }

        /**
         * Find the best scoring node
         */
        BoardNodeScorePair bestNodePair = new BoardNodeScorePair(0, 0);
        Enumeration keynum = twoAway.keys();

        while (keynum.hasMoreElements())
        {
            Integer coord = (Integer) keynum.nextElement();
            final int score = ((Integer) twoAway.get(coord)).intValue();

            D.ebugPrintln("Considering " + Integer.toHexString(coord.intValue()) + " with a score of " + score);

            if (dummy.isPotentialSettlement(coord.intValue()))
            {
                if (bestNodePair.getScore() < score)
                {
                    bestNodePair.setScore(score);
                    bestNodePair.setNode(coord.intValue());
                }
            }
            else
            {
                D.ebugPrintln("Someone is bound to ruin that spot.");
            }
        }

        final int destination = bestNodePair.getNode();  // coordinate of future settlement
        final int roadEdge;

        /**
         * if the coords are (even, odd), then
         * the node is 'Y'.
         */
        if (((settlementNode >> 4) % 2) == 0)
        {
            if ((destination == (settlementNode - 0x02)) || (destination == (settlementNode + 0x20)))
            {
                roadEdge = settlementNode - 0x01;
            }
            else if (destination < settlementNode)
            {
                roadEdge = settlementNode - 0x11;
            }
            else
            {
                roadEdge = settlementNode;
            }
        }
        else
        {
            if ((destination == (settlementNode - 0x20)) || (destination == (settlementNode + 0x02)))
            {
                roadEdge = settlementNode - 0x10;
            }
            else if (destination > settlementNode)
            {
                roadEdge = settlementNode;
            }
            else
            {
                roadEdge = settlementNode - 0x11;
            }
        }

        //D.ebugPrintln("!!! PUTTING INIT ROAD !!!");
        pause(500);

        //D.ebugPrintln("Trying to build a road at "+Integer.toHexString(roadEdge));
        lastStartingPieceCoord = roadEdge;
        lastStartingRoadTowardsNode = destination;
        client.putPiece(game, new Road(ourPlayerData, roadEdge, null));
        pause(1000);

        dummy.destroyPlayer();
    }

    /**
     * Estimate the rarity of each resource, given this board's resource locations vs dice numbers.
     * Cached after the first call.
     *
     * @return an array of rarity numbers where
     *         estimates[Board.CLAY_HEX] == the clay rarity,
     *         as an integer percentage 0-100 of dice rolls.
     */
    protected int[] estimateResourceRarity()
    {
        if (resourceEstimates == null)
        {
            Board board = game.getBoard();
            final int[] numberWeights = NumberProbabilities.INT_VALUES;

            resourceEstimates = new int[ResourceConstants.UNKNOWN];  // uses 1 to 5 (CLAY to WOOD)
            resourceEstimates[0] = 0;

            // look at each hex
            final int L = board.getNumberLayout().length;
            for (int i = 0; i < L; i++)
            {
                final int hexNumber = board.getNumberOnHexFromNumber(i);
                if (hexNumber > 0)
                    resourceEstimates[board.getHexTypeFromNumber(i)] += numberWeights[hexNumber];
            }
        }

        //D.ebugPrint("Resource Estimates = ");
        //for (int i = 1; i < 6; i++)
        //{
            //D.ebugPrint(i+":"+resourceEstimates[i]+" ");
        //}

        //D.ebugPrintln();
        return resourceEstimates;
    }

    /**
     * Takes a table of nodes and adds a weighted score to
     * each node score in the table.  Nodes touching hexes
     * with better numbers get better scores.
     *
     * @param nodes    the table of nodes with scores
     * @param weight   a number that is multiplied by the score
     */
    protected void bestSpotForNumbers(Hashtable nodes, int weight)
    {
        int[] numRating = NumberProbabilities.INT_VALUES;
        Board board = game.getBoard();
        int oldScore;
        Enumeration nodesEnum = nodes.keys();

        while (nodesEnum.hasMoreElements())
        {
            Integer node = (Integer) nodesEnum.nextElement();

            //D.ebugPrintln("BSN - looking at node "+Integer.toHexString(node.intValue()));
            oldScore = ((Integer) nodes.get(node)).intValue();

            int score = 0;
            Enumeration hexesEnum = Board.getAdjacentHexesToNode(node.intValue()).elements();

            while (hexesEnum.hasMoreElements())
            {
                int hex = ((Integer) hexesEnum.nextElement()).intValue();
                score += numRating[board.getNumberOnHexFromCoord(hex)];

                //D.ebugPrintln(" -- -- Adding "+numRating[board.getNumberOnHexFromCoord(hex)]);
            }

            /*
             * normalize score and multiply by weight
             * 40 is highest practical score
             * lowest score is 0
             */
            int nScore = ((score * 100) / 40) * weight;
            Integer finalScore = new Integer(nScore + oldScore);
            nodes.put(node, finalScore);

            //D.ebugPrintln("BSN -- put node "+Integer.toHexString(node.intValue())+" with old score "+oldScore+" + new score "+nScore);
        }
    }

    /**
     * Takes a table of nodes and adds a weighted score to
     * each node score in the table.  Nodes touching hexes
     * with better numbers get better scores.  Also numbers
     * that the player isn't touching yet are better than ones
     * that the player is already touching.
     *
     * @param nodes    the table of nodes with scores. key = Int node, value = Int score, to be modified in this method
     * @param player   the player that we are doing the rating for
     * @param weight   a number that is multiplied by the score
     */
    protected void bestSpotForNumbers(Hashtable nodes, Player player, int weight)
    {
        int[] numRating = NumberProbabilities.INT_VALUES;
        Board board = game.getBoard();
        int oldScore;
        Enumeration nodesEnum = nodes.keys();

        while (nodesEnum.hasMoreElements())
        {
            Integer node = (Integer) nodesEnum.nextElement();

            //D.ebugPrintln("BSN - looking at node "+Integer.toHexString(node.intValue()));
            oldScore = ((Integer) nodes.get(node)).intValue();

            int score = 0;
            Enumeration hexesEnum = Board.getAdjacentHexesToNode(node.intValue()).elements();

            while (hexesEnum.hasMoreElements())
            {
                final int hex = ((Integer) hexesEnum.nextElement()).intValue();
                final int number = board.getNumberOnHexFromCoord(hex);
                score += numRating[number];

                if ((number != 0) && (!player.getNumbers().hasNumber(number)))
                {
                    /**
                     * add a bonus for numbers that the player doesn't already have
                     */

                    //D.ebugPrintln("ADDING BONUS FOR NOT HAVING "+number);
                    score += numRating[number];
                }

                //D.ebugPrintln(" -- -- Adding "+numRating[board.getNumberOnHexFromCoord(hex)]);
            }

            /*
             * normalize score and multiply by weight
             * 80 is highest practical score
             * lowest score is 0
             */
            int nScore = ((score * 100) / 80) * weight;
            Integer finalScore = new Integer(nScore + oldScore);
            nodes.put(node, finalScore);

            //D.ebugPrintln("BSN -- put node "+Integer.toHexString(node.intValue())+" with old score "+oldScore+" + new score "+nScore);
        }
    }

    /**
     * Takes a table of nodes and adds a weighted score to
     * each node score in the table.  A vector of nodes that
     * we want to be near is also taken as an argument.
     * Here are the rules for scoring:
     * If a node is two away from a node in the desired set of nodes it gets 100.
     * Otherwise it gets 0.
     *
     * @param nodesIn   the table of nodes to evaluate
     * @param nodeSet   the set of desired nodes
     * @param weight    the score multiplier
     */
    protected void bestSpot2AwayFromANodeSet(Hashtable nodesIn, Vector nodeSet, int weight)
    {
        Enumeration nodesInEnum = nodesIn.keys();

        while (nodesInEnum.hasMoreElements())
        {
            Integer nodeCoord = (Integer) nodesInEnum.nextElement();
            int node = nodeCoord.intValue();
            int score = 0;
            final int oldScore = ((Integer) nodesIn.get(nodeCoord)).intValue();

            Enumeration nodeSetEnum = nodeSet.elements();

            while (nodeSetEnum.hasMoreElements())
            {
                int target = ((Integer) nodeSetEnum.nextElement()).intValue();

                if (node == target)
                {
                    break;
                }
                else if (node == (target - 0x20))
                {
                    score = 100;
                }
                else if (node == (target + 0x02))
                {
                    score = 100;
                }
                else if (node == (target + 0x22))
                {
                    score = 100;
                }
                else if (node == (target + 0x20))
                {
                    score = 100;
                }
                else if (node == (target - 0x02))
                {
                    score = 100;
                }
                else if (node == (target - 0x22))
                {
                    score = 100;
                }
            }

            /**
             * multiply by weight
             */
            score *= weight;

            nodesIn.put(nodeCoord, new Integer(oldScore + score));

            //D.ebugPrintln("BS2AFANS -- put node "+Integer.toHexString(node)+" with old score "+oldScore+" + new score "+score);
        }
    }

    /**
     * Takes a table of nodes and adds a weighted score to
     * each node score in the table.  A vector of nodes that
     * we want to be on is also taken as an argument.
     * Here are the rules for scoring:
     * If a node is in the desired set of nodes it gets 100.
     * Otherwise it gets 0.
     *
     * @param nodesIn   the table of nodes to evaluate
     * @param nodeSet   the set of desired nodes
     * @param weight    the score multiplier
     */
    protected void bestSpotInANodeSet(Hashtable nodesIn, Vector nodeSet, int weight)
    {
        Enumeration nodesInEnum = nodesIn.keys();

        while (nodesInEnum.hasMoreElements())
        {
            Integer nodeCoord = (Integer) nodesInEnum.nextElement();
            int node = nodeCoord.intValue();
            int score = 0;
            final int oldScore = ((Integer) nodesIn.get(nodeCoord)).intValue();

            Enumeration nodeSetEnum = nodeSet.elements();

            while (nodeSetEnum.hasMoreElements())
            {
                int target = ((Integer) nodeSetEnum.nextElement()).intValue();

                if (node == target)
                {
                    score = 100;

                    break;
                }
            }

            /**
             * multiply by weight
             */
            score *= weight;

            nodesIn.put(nodeCoord, new Integer(oldScore + score));

            //D.ebugPrintln("BSIANS -- put node "+Integer.toHexString(node)+" with old score "+oldScore+" + new score "+score);
        }
    }

    /**
     * move the robber
     */
    protected void moveRobber()
    {
        D.ebugPrintln("%%% MOVEROBBER");

        final int[] hexes = game.getBoard().getHexLandCoords();

        int robberHex = game.getBoard().getRobberHex();

        /**
         * decide which player we want to thwart
         */
        int[] winGameETAs = new int[game.maxPlayers];
        for (int i = game.maxPlayers - 1; i >= 0; --i)
            winGameETAs[i] = 100;
        Iterator trackersIter = playerTrackers.values().iterator();

        while (trackersIter.hasNext())
        {
            PlayerTracker tracker = (PlayerTracker) trackersIter.next();
            D.ebugPrintln("%%%%%%%%% TRACKER FOR PLAYER " + tracker.getPlayer().getPlayerNumber());

            try
            {
                tracker.recalcWinGameETA();
                winGameETAs[tracker.getPlayer().getPlayerNumber()] = tracker.getWinGameETA();
                D.ebugPrintln("winGameETA = " + tracker.getWinGameETA());
            }
            catch (NullPointerException e)
            {
                D.ebugPrintln("Null Pointer Exception calculating winGameETA");
                winGameETAs[tracker.getPlayer().getPlayerNumber()] = 500;
            }
        }

        int victimNum = -1;

        for (int pnum = 0; pnum < game.maxPlayers; pnum++)
        {
            if (! game.isSeatVacant(pnum))
            {
                if ((victimNum < 0) && (pnum != ourPlayerData.getPlayerNumber()))
                {
                    // The first pick
                    D.ebugPrintln("Picking a robber victim: pnum=" + pnum);
                    victimNum = pnum;
                }
                else if ((pnum != ourPlayerData.getPlayerNumber()) && (winGameETAs[pnum] < winGameETAs[victimNum]))
                {
                    // A better pick
                    D.ebugPrintln("Picking a better robber victim: pnum=" + pnum);
                    victimNum = pnum;
                }
            }
        }
        // Postcondition: victimNum != -1 due to "First pick" in loop.

        /**
         * figure out the best way to thwart that player
         */
        Player victim = game.getPlayer(victimNum);
        BuildingSpeedEstimate estimate = new BuildingSpeedEstimate();
        int bestHex = robberHex;
        int worstSpeed = 0;
        final boolean skipDeserts = game.isGameOptionSet("RD");  // can't move robber to desert
        Board gboard = (skipDeserts ? game.getBoard() : null);

        for (int i = 0; i < hexes.length; i++)
        {
            /**
             * only check hexes that we're not touching,
             * and not the robber hex, and possibly not desert hexes
             */
            if ((hexes[i] != robberHex)
                    && ourPlayerData.getNumbers().getNumberResourcePairsForHex(hexes[i]).isEmpty()
                    && ! (skipDeserts && (gboard.getHexTypeFromCoord(hexes[i]) == Board.DESERT_HEX )))
            {
                estimate.recalculateEstimates(victim.getNumbers(), hexes[i]);

                int[] speeds = estimate.getEstimatesFromNothingFast(victim.getPortFlags());
                int totalSpeed = 0;

                for (int j = BuildingSpeedEstimate.MIN;
                        j < BuildingSpeedEstimate.MAXPLUSONE; j++)
                {
                    totalSpeed += speeds[j];
                }

                D.ebugPrintln("total Speed = " + totalSpeed);

                if (totalSpeed > worstSpeed)
                {
                    bestHex = hexes[i];
                    worstSpeed = totalSpeed;
                    D.ebugPrintln("bestHex = " + Integer.toHexString(bestHex));
                    D.ebugPrintln("worstSpeed = " + worstSpeed);
                }
            }
        }

        D.ebugPrintln("%%% bestHex = " + Integer.toHexString(bestHex));

        /**
         * pick a spot at random if we can't decide.
         * Don't pick deserts if the game option is set.
         * Don't pick one of our hexes if at all possible.
         * It's not likely we'll need to pick one of our hexes
         * (we try 30 times to avoid it), so there isn't code here
         * to pick the 'least bad' one.
         * (TODO) consider that: it would be late in the game.
         *       Use similar algorithm as picking for opponent,
         *       but apply it worst vs best.
         */
        if (bestHex == robberHex)
        {
            int numRand = 0;
            while ((bestHex == robberHex)
                    || (skipDeserts
                            && (gboard.getHexTypeFromCoord(bestHex) == Board.DESERT_HEX ))
                    || ((numRand < 30)
                            && ourPlayerData.getNumbers().getNumberResourcePairsForHex(bestHex).isEmpty()))
            {
                bestHex = hexes[Math.abs(rand.nextInt()) % hexes.length];
                // D.ebugPrintln("%%% random pick = " + Integer.toHexString(bestHex));
                System.err.println("%%% random pick = " + Integer.toHexString(bestHex));
                ++numRand;
            }
        }

        D.ebugPrintln("!!! MOVING ROBBER !!!");
        client.moveRobber(game, ourPlayerData, bestHex);
        pause(2000);
    }

    /**
     * discard some resources
     *
     * @param numDiscards  the number of resources to discard
     */
    protected void discard(int numDiscards)
    {
        //D.ebugPrintln("DISCARDING...");

        /**
         * if we have a plan, then try to keep the resources
         * needed for that plan, otherwise discard at random
         */
        ResourceSet discards = new ResourceSet();

        /**
         * make a plan if we don't have one
         */
        if (buildingPlan.empty())
        {
            decisionMaker.planStuff(robotParameters.getStrategyType());
        }

        if (!buildingPlan.empty())
        {
            PossiblePiece targetPiece = (PossiblePiece) buildingPlan.peek();
            negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), targetPiece);

            //D.ebugPrintln("targetPiece="+targetPiece);
            ResourceSet targetResources = PlayingPiece.getResourcesToBuild(targetPiece.getType());

            /**
             * figure out what resources are NOT the ones we need
             */
            ResourceSet leftOvers = ourPlayerData.getResources().copy();

            for (int rsrc = ResourceConstants.CLAY;
                    rsrc <= ResourceConstants.WOOD; rsrc++)
            {
                if (leftOvers.getAmount(rsrc) > targetResources.getAmount(rsrc))
                {
                    leftOvers.subtract(targetResources.getAmount(rsrc), rsrc);
                }
                else
                {
                    leftOvers.setAmount(0, rsrc);
                }
            }

            ResourceSet neededRsrcs = ourPlayerData.getResources().copy();
            neededRsrcs.subtract(leftOvers);

            /**
             * figure out the order of resources from
             * easiest to get to hardest
             */

            //D.ebugPrintln("our numbers="+ourPlayerData.getNumbers());
            BuildingSpeedEstimate estimate = new BuildingSpeedEstimate(ourPlayerData.getNumbers());
            int[] rollsPerResource = estimate.getRollsPerResource();
            int[] resourceOrder = 
            {
                ResourceConstants.CLAY, ResourceConstants.ORE,
                ResourceConstants.SHEEP, ResourceConstants.WHEAT,
                ResourceConstants.WOOD
            };

            for (int j = 4; j >= 0; j--)
            {
                for (int i = 0; i < j; i++)
                {
                    if (rollsPerResource[resourceOrder[i]] < rollsPerResource[resourceOrder[i + 1]])
                    {
                        int tmp = resourceOrder[i];
                        resourceOrder[i] = resourceOrder[i + 1];
                        resourceOrder[i + 1] = tmp;
                    }
                }
            }

            /**
             * pick the discards
             */
            int curRsrc = 0;

            while (discards.getTotal() < numDiscards)
            {
                /**
                 * choose from the left overs
                 */
                while ((discards.getTotal() < numDiscards) && (curRsrc < 5))
                {
                    //D.ebugPrintln("(1) dis.tot="+discards.getTotal()+" curRsrc="+curRsrc);
                    if (leftOvers.getAmount(resourceOrder[curRsrc]) > 0)
                    {
                        discards.add(1, resourceOrder[curRsrc]);
                        leftOvers.subtract(1, resourceOrder[curRsrc]);
                    }
                    else
                    {
                        curRsrc++;
                    }
                }

                curRsrc = 0;

                /**
                 * choose from what we need
                 */
                while ((discards.getTotal() < numDiscards) && (curRsrc < 5))
                {
                    //D.ebugPrintln("(2) dis.tot="+discards.getTotal()+" curRsrc="+curRsrc);
                    if (neededRsrcs.getAmount(resourceOrder[curRsrc]) > 0)
                    {
                        discards.add(1, resourceOrder[curRsrc]);
                        neededRsrcs.subtract(1, resourceOrder[curRsrc]);
                    }
                    else
                    {
                        curRsrc++;
                    }
                }
            }

            if (curRsrc == 5)
            {
                System.err.println("PROBLEM IN DISCARD - curRsrc == 5");
            }
        }
        else
        {
            /**
             *  choose discards at random
             */
            Game.discardPickRandom(ourPlayerData.getResources(), numDiscards, discards, rand);
        }

        //D.ebugPrintln("!!! DISCARDING !!!");
        //D.ebugPrintln("discards="+discards);
        client.discard(game, discards);
    }

    /**
     * choose a robber victim
     *
     * @param choices a boolean array representing which players are possible victims
     */
    protected void chooseRobberVictim(boolean[] choices)
    {
        int choice = -1;

        /**
         * choose the player with the smallest WGETA
         */
        for (int i = 0; i < game.maxPlayers; i++)
        {
            if (! game.isSeatVacant (i))
            {
                if (choices[i])
                {
                    if (choice == -1)
                    {
                        choice = i;
                    }
                    else
                    {
                        PlayerTracker tracker1 = (PlayerTracker) playerTrackers.get(new Integer(i));
                        PlayerTracker tracker2 = (PlayerTracker) playerTrackers.get(new Integer(choice));
    
                        if ((tracker1 != null) && (tracker2 != null) && (tracker1.getWinGameETA() < tracker2.getWinGameETA()))
                        {
                            //D.ebugPrintln("Picking a robber victim: pnum="+i+" VP="+game.getPlayer(i).getPublicVP());
                            choice = i;
                        }
                    }
                }
            }
        }

        /**
         * choose victim at random
         *
           do {
           choice = Math.abs(rand.nextInt() % Game.MAXPLAYERS);
           } while (!choices[choice]);
         */
        client.choosePlayer(game, choice);
    }

    /**
     * calculate the number of builds before the next turn during init placement
     *
     */
    protected int numberOfEnemyBuilds()
    {
        int numberOfBuilds = 0;
        int pNum = game.getCurrentPlayerNumber();

        /**
         * This is the clockwise direction
         */
        if ((game.getGameState() == Game.START1A) || (game.getGameState() == Game.START1B))
        {
            do
            {
                /**
                 * look at the next player
                 */
                pNum++;

                if (pNum >= game.maxPlayers)
                {
                    pNum = 0;
                }

                if ((pNum != game.getFirstPlayer()) && ! game.isSeatVacant (pNum))
                {
                    numberOfBuilds++;
                }
            }
            while (pNum != game.getFirstPlayer());
        }

        /**
         * This is the counter-clockwise direction
         */
        do
        {
            /**
             * look at the next player
             */
            pNum--;

            if (pNum < 0)
            {
                pNum = game.maxPlayers - 1;
            }

            if ((pNum != game.getCurrentPlayerNumber()) && ! game.isSeatVacant (pNum))
            {
                numberOfBuilds++;
            }
        }
        while (pNum != game.getCurrentPlayerNumber());

        return numberOfBuilds;
    }

    /**
     * given a table of nodes/edges with scores, return the
     * best scoring pair
     *
     * @param nodes  the table of nodes/edges
     * @return the best scoring pair
     */
    protected BoardNodeScorePair findBestScoringNode(Hashtable nodes)
    {
        BoardNodeScorePair bestNodePair = new BoardNodeScorePair(0, -1);
        Enumeration nodesEnum = nodes.keys();

        while (nodesEnum.hasMoreElements())
        {
            Integer nodeCoord = (Integer) nodesEnum.nextElement();
            Integer score = (Integer) nodes.get(nodeCoord);

            //D.ebugPrintln("Checking:"+Integer.toHexString(nodeCoord.intValue())+" score:"+score);
            if (bestNodePair.getScore() < score.intValue())
            {
                bestNodePair.setScore(score.intValue());
                bestNodePair.setNode(nodeCoord.intValue());
            }
        }

        return bestNodePair;
    }

    /**
     * this is a function more for convience
     * given a set of nodes, run a bunch of metrics across them
     * to find which one is best for building a
     * settlement
     *
     * @param nodes          a hashtable of nodes, the scores in the table will be modified.
     *                            Key = coord Integer; value = score Integer.
     * @param numberWeight   the weight given to nodes on good numbers
     * @param miscPortWeight the weight given to nodes on 3:1 ports
     * @param portWeight     the weight given to nodes on good 2:1 ports
     */
    protected void scoreNodesForSettlements(Hashtable nodes, final int numberWeight, final int miscPortWeight, final int portWeight)
    {
        /**
         * favor spots with the most high numbers
         */
        bestSpotForNumbers(nodes, ourPlayerData, numberWeight);

        /**
         * favor spots on good ports:
         */
        /**
         * check if this is on a 3:1 ports, only if we don't have one
         */
        if (!ourPlayerData.getPortFlag(Board.MISC_PORT))
        {
            Vector miscPortNodes = game.getBoard().getPortCoordinates(Board.MISC_PORT);
            bestSpotInANodeSet(nodes, miscPortNodes, miscPortWeight);
        }

        /**
         * check out good 2:1 ports that we don't have
         */
        int[] resourceEstimates = estimateResourceRarity();

        for (int portType = Board.CLAY_PORT; portType <= Board.WOOD_PORT;
                portType++)
        {
            /**
             * if the chances of rolling a number on the resource is better than 1/3,
             * then it's worth looking at the port
             */
            if ((resourceEstimates[portType] > 33) && (!ourPlayerData.getPortFlag(portType)))
            {
                Vector portNodes = game.getBoard().getPortCoordinates(portType);
                int estimatedPortWeight = (resourceEstimates[portType] * portWeight) / 56;
                bestSpotInANodeSet(nodes, portNodes, estimatedPortWeight);
            }
        }
    }

    /**
     * do some trading
     */
    protected void tradeStuff()
    {
        /**
         * make a tree of all the possible trades that we can
         * make with the bank or ports
         */
        TradeTree treeRoot = new TradeTree(ourPlayerData.getResources(), (TradeTree) null);
        Hashtable treeNodes = new Hashtable();
        treeNodes.put(treeRoot.getResourceSet(), treeRoot);

        Queue queue = new Queue();
        queue.put(treeRoot);

        while (!queue.empty())
        {
            TradeTree currentTreeNode = (TradeTree) queue.get();

            //D.ebugPrintln("%%% Expanding "+currentTreeNode.getResourceSet());
            expandTradeTreeNode(currentTreeNode, treeNodes);

            Enumeration childrenEnum = currentTreeNode.getChildren().elements();

            while (childrenEnum.hasMoreElements())
            {
                TradeTree child = (TradeTree) childrenEnum.nextElement();

                //D.ebugPrintln("%%% Child "+child.getResourceSet());
                if (child.needsToBeExpanded())
                {
                    /**
                     * make a new table entry
                     */
                    treeNodes.put(child.getResourceSet(), child);
                    queue.put(child);
                }
            }
        }

        /**
         * find the best trade result and then perform the trades
         */
        ResourceSet bestTradeOutcome = null;
        int bestTradeScore = -1;
        Enumeration possibleTrades = treeNodes.keys();

        while (possibleTrades.hasMoreElements())
        {
            ResourceSet possibleTradeOutcome = (ResourceSet) possibleTrades.nextElement();

            //D.ebugPrintln("%%% "+possibleTradeOutcome);
            int score = scoreTradeOutcome(possibleTradeOutcome);

            if (score > bestTradeScore)
            {
                bestTradeOutcome = possibleTradeOutcome;
                bestTradeScore = score;
            }
        }

        /**
         * find the trade outcome in the tree, then follow
         * the chain of parents until you get to the root
         * all the while pushing the outcomes onto a stack.
         * then pop outcomes off of the stack and perfoem
         * the trade to get each outcome
         */
        Stack stack = new Stack();
        TradeTree cursor = (TradeTree) treeNodes.get(bestTradeOutcome);

        while (cursor != treeRoot)
        {
            stack.push(cursor);
            cursor = cursor.getParent();
        }

        ResourceSet give = new ResourceSet();
        ResourceSet get = new ResourceSet();
        TradeTree currTreeNode;
        TradeTree prevTreeNode;
        prevTreeNode = treeRoot;

        while (!stack.empty())
        {
            currTreeNode = (TradeTree) stack.pop();
            give.setAmounts(prevTreeNode.getResourceSet());
            give.subtract(currTreeNode.getResourceSet());
            get.setAmounts(currTreeNode.getResourceSet());
            get.subtract(prevTreeNode.getResourceSet());

            /**
             * get rid of the negative numbers
             */
            for (int rt = ResourceConstants.CLAY;
                    rt <= ResourceConstants.WOOD; rt++)
            {
                if (give.getAmount(rt) < 0)
                {
                    give.setAmount(0, rt);
                }

                if (get.getAmount(rt) < 0)
                {
                    get.setAmount(0, rt);
                }
            }

            //D.ebugPrintln("Making bank trade:");
            //D.ebugPrintln("give: "+give);
            //D.ebugPrintln("get: "+get);
            client.bankTrade(game, give, get);
            pause(2000);
            prevTreeNode = currTreeNode;
        }
    }

    /**
     * expand a trade tree node
     *
     * @param currentTreeNode   the tree node that we're expanding
     * @param table  the table of all of the nodes in the tree except this one
     */
    protected void expandTradeTreeNode(TradeTree currentTreeNode, Hashtable table)
    {
        /**
         * the resources that we have to work with
         */
        ResourceSet rSet = currentTreeNode.getResourceSet();

        /**
         * go through the resources one by one, and generate all possible
         * resource sets that result from trading that type of resource
         */
        for (int giveResource = ResourceConstants.CLAY;
                giveResource <= ResourceConstants.WOOD; giveResource++)
        {
            /**
             * find the ratio at which we can trade
             */
            int tradeRatio;

            if (ourPlayerData.getPortFlag(giveResource))
            {
                tradeRatio = 2;
            }
            else if (ourPlayerData.getPortFlag(Board.MISC_PORT))
            {
                tradeRatio = 3;
            }
            else
            {
                tradeRatio = 4;
            }

            /**
             * make sure we have enough resources to trade
             */
            if (rSet.getAmount(giveResource) >= tradeRatio)
            {
                /**
                 * trade the resource that we're looking at for one
                 * of every other resource
                 */
                for (int getResource = ResourceConstants.CLAY;
                        getResource <= ResourceConstants.WOOD;
                        getResource++)
                {
                    if (getResource != giveResource)
                    {
                        ResourceSet newTradeResult = rSet.copy();
                        newTradeResult.subtract(tradeRatio, giveResource);
                        newTradeResult.add(1, getResource);

                        TradeTree newTree = new TradeTree(newTradeResult, currentTreeNode);

                        /**
                         * if the trade results in a set of resources that is
                         * equal to or worse than a trade we've already seen,
                         * then we don't want to expand this tree node
                         */
                        Enumeration tableEnum = table.keys();

                        while (tableEnum.hasMoreElements())
                        {
                            ResourceSet oldTradeResult = (ResourceSet) tableEnum.nextElement();

                            /*
                               //D.ebugPrintln("%%%     "+newTradeResult);
                               //D.ebugPrintln("%%%  <= "+oldTradeResult+" : "+
                               ResourceSet.lte(newTradeResult, oldTradeResult));
                             */
                            if (ResourceSet.lte(newTradeResult, oldTradeResult))
                            {
                                newTree.setNeedsToBeExpanded(false);

                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * evaluate a trade outcome by calculating how much you could build with it
     *
     * @param tradeOutcome  a set of resources that would be the result of trading
     */
    protected int scoreTradeOutcome(ResourceSet tradeOutcome)
    {
        int score = 0;
        ResourceSet tempTO = tradeOutcome.copy();

        if ((ourPlayerData.getNumPieces(PlayingPiece.SETTLEMENT) >= 1) && (ourPlayerData.hasPotentialSettlement()))
        {
            while (tempTO.contains(Game.SETTLEMENT_SET))
            {
                score += 2;
                tempTO.subtract(Game.SETTLEMENT_SET);
            }
        }

        if ((ourPlayerData.getNumPieces(PlayingPiece.ROAD) >= 1) && (ourPlayerData.hasPotentialRoad()))
        {
            while (tempTO.contains(Game.ROAD_SET))
            {
                score += 1;
                tempTO.subtract(Game.ROAD_SET);
            }
        }

        if ((ourPlayerData.getNumPieces(PlayingPiece.CITY) >= 1) && (ourPlayerData.hasPotentialCity()))
        {
            while (tempTO.contains(Game.CITY_SET))
            {
                score += 2;
                tempTO.subtract(Game.CITY_SET);
            }
        }

        //D.ebugPrintln("Score for "+tradeOutcome+" : "+score);
        return score;
    }

    /**
     * make trades to get the target resources
     *
     * @param targetResources  the resources that we want
     * @return true if we sent a request to trade
     */
    protected boolean tradeToTarget2(ResourceSet targetResources)
    {
        if (ourPlayerData.getResources().contains(targetResources))
        {
            return false;
        }

        TradeOffer bankTrade = negotiator.getOfferToBank(targetResources, ourPlayerData.getResources());

        if ((bankTrade != null) && (ourPlayerData.getResources().contains(bankTrade.getGiveSet())))
        {
            client.bankTrade(game, bankTrade.getGiveSet(), bankTrade.getGetSet());
            pause(2000);

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * consider an offer made by another player
     *
     * @param offer  the offer to consider
     * @return a code that represents how we want to respond
     * note: a negative result means we do nothing
     */
    protected int considerOffer(TradeOffer offer)
    {
        int response = -1;

        Player offeringPlayer = game.getPlayer(offer.getFrom());

        if ((offeringPlayer.getCurrentOffer() != null) && (offer == offeringPlayer.getCurrentOffer()))
        {
            boolean[] offeredTo = offer.getTo();

            if (offeredTo[ourPlayerData.getPlayerNumber()])
            {
                response = negotiator.considerOffer2(offer, ourPlayerData.getPlayerNumber());
            }
        }

        return response;
    }

    /**
     * make an offer to another player.
     * Will set {@link #waitingForTradeResponse} or {@link #doneTrading}.
     *
     * @param target  the resources that we want
     * @return true if we made an offer
     */
    protected boolean makeOffer(PossiblePiece target)
    {
        boolean result = false;
        TradeOffer offer = negotiator.makeOffer(target);
        ourPlayerData.setCurrentOffer(offer);
        negotiator.resetWantsAnotherOffer();

        if (offer != null)
        {
            ///
            ///  reset the offerRejections flag
            ///
            for (int i = 0; i < game.maxPlayers; i++)
            {
                offerRejections[i] = false;
            }

            waitingForTradeResponse = true;
            counter = 0;
            client.offerTrade(game, offer);
            result = true;
        }
        else
        {
            doneTrading = true;
            waitingForTradeResponse = false;
        }

        return result;
    }

    /**
     * make a counter offer to another player
     *
     * @param offer their offer
     * @return true if we made an offer
     */
    protected boolean makeCounterOffer(TradeOffer offer)
    {
        boolean result = false;
        TradeOffer counterOffer = negotiator.makeCounterOffer(offer);
        ourPlayerData.setCurrentOffer(counterOffer);

        if (counterOffer != null)
        {
            ///
            ///  reset the offerRejections flag
            ///
            offerRejections[offer.getFrom()] = false;
            waitingForTradeResponse = true;
            counter = 0;
            client.offerTrade(game, counterOffer);
            result = true;
        }
        else
        {
            doneTrading = true;
            waitingForTradeResponse = false;
        }

        return result;
    }

    /**
     * this means that we want to play a discovery development card
     */
    protected void chooseFreeResources(ResourceSet targetResources)
    {
        /**
         * clear our resource choices
         */
        resourceChoices.clear();

        /**
         * find the most needed resource by looking at
         * which of the resources we still need takes the
         * longest to aquire
         */
        ResourceSet rsCopy = ourPlayerData.getResources().copy();
        BuildingSpeedEstimate estimate = new BuildingSpeedEstimate(ourPlayerData.getNumbers());
        int[] rollsPerResource = estimate.getRollsPerResource();

        for (int resourceCount = 0; resourceCount < 2; resourceCount++)
        {
            int mostNeededResource = -1;

            for (int resource = ResourceConstants.CLAY;
                    resource <= ResourceConstants.WOOD; resource++)
            {
                if (rsCopy.getAmount(resource) < targetResources.getAmount(resource))
                {
                    if (mostNeededResource < 0)
                    {
                        mostNeededResource = resource;
                    }
                    else
                    {
                        if (rollsPerResource[resource] > rollsPerResource[mostNeededResource])
                        {
                            mostNeededResource = resource;
                        }
                    }
                }
            }

            resourceChoices.add(1, mostNeededResource);
            rsCopy.add(1, mostNeededResource);
        }
    }

    /**
     * choose a resource to monopolize
     * @return true if playing the card is worth it
     */
    protected boolean chooseMonopoly()
    {
        int bestResourceCount = 0;
        int bestResource = 0;

        for (int resource = ResourceConstants.CLAY;
                resource <= ResourceConstants.WOOD; resource++)
        {
            //D.ebugPrintln("$$ resource="+resource);
            int freeResourceCount = 0;
            boolean twoForOne = false;
            boolean threeForOne = false;

            if (ourPlayerData.getPortFlag(resource))
            {
                twoForOne = true;
            }
            else if (ourPlayerData.getPortFlag(Board.MISC_PORT))
            {
                threeForOne = true;
            }

            int resourceTotal = 0;

            for (int pn = 0; pn < game.maxPlayers; pn++)
            {
                if (ourPlayerData.getPlayerNumber() != pn)
                {
                    resourceTotal += game.getPlayer(pn).getResources().getAmount(resource);

                    //D.ebugPrintln("$$ resourceTotal="+resourceTotal);
                }
            }

            if (twoForOne)
            {
                freeResourceCount = resourceTotal / 2;
            }
            else if (threeForOne)
            {
                freeResourceCount = resourceTotal / 3;
            }
            else
            {
                freeResourceCount = resourceTotal / 4;
            }

            //D.ebugPrintln("freeResourceCount="+freeResourceCount);
            if (freeResourceCount > bestResourceCount)
            {
                bestResourceCount = freeResourceCount;
                bestResource = resource;
            }
        }

        if (bestResourceCount > 2)
        {
            monopolyChoice = bestResource;

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * this is for debugging
     */
    protected void debugInfo()
    {
        /*
           if (D.ebugOn) {
           //D.ebugPrintln("$===============");
           //D.ebugPrintln("gamestate = "+game.getGameState());
           //D.ebugPrintln("counter = "+counter);
           //D.ebugPrintln("resources = "+ourPlayerData.getResources().getTotal());
           if (expectSTART1A)
           //D.ebugPrintln("expectSTART1A");
           if (expectSTART1B)
           //D.ebugPrintln("expectSTART1B");
           if (expectSTART2A)
           //D.ebugPrintln("expectSTART2A");
           if (expectSTART2B)
           //D.ebugPrintln("expectSTART2B");
           if (expectPLAY)
           //D.ebugPrintln("expectPLAY");
           if (expectPLAY1)
           //D.ebugPrintln("expectPLAY1");
           if (expectPLACING_ROAD)
           //D.ebugPrintln("expectPLACING_ROAD");
           if (expectPLACING_SETTLEMENT)
           //D.ebugPrintln("expectPLACING_SETTLEMENT");
           if (expectPLACING_CITY)
           //D.ebugPrintln("expectPLACING_CITY");
           if (expectPLACING_ROBBER)
           //D.ebugPrintln("expectPLACING_ROBBER");
           if (expectPLACING_FREE_ROAD1)
           //D.ebugPrintln("expectPLACING_FREE_ROAD1");
           if (expectPLACING_FREE_ROAD2)
           //D.ebugPrintln("expectPLACING_FREE_ROAD2");
           if (expectPUTPIECE_FROM_START1A)
           //D.ebugPrintln("expectPUTPIECE_FROM_START1A");
           if (expectPUTPIECE_FROM_START1B)
           //D.ebugPrintln("expectPUTPIECE_FROM_START1B");
           if (expectPUTPIECE_FROM_START2A)
           //D.ebugPrintln("expectPUTPIECE_FROM_START2A");
           if (expectPUTPIECE_FROM_START2B)
           //D.ebugPrintln("expectPUTPIECE_FROM_START2B");
           if (expectDICERESULT)
           //D.ebugPrintln("expectDICERESULT");
           if (expectDISCARD)
           //D.ebugPrintln("expectDISCARD");
           if (expectMOVEROBBER)
           //D.ebugPrintln("expectMOVEROBBER");
           if (expectWAITING_FOR_DISCOVERY)
           //D.ebugPrintln("expectWAITING_FOR_DISCOVERY");
           if (waitingForGameState)
           //D.ebugPrintln("waitingForGameState");
           if (waitingForOurTurn)
           //D.ebugPrintln("waitingForOurTurn");
           if (waitingForTradeMsg)
           //D.ebugPrintln("waitingForTradeMsg");
           if (waitingForDevCard)
           //D.ebugPrintln("waitingForDevCard");
           if (moveRobberOnSeven)
           //D.ebugPrintln("moveRobberOnSeven");
           if (waitingForTradeResponse)
           //D.ebugPrintln("waitingForTradeResponse");
           if (doneTrading)
           //D.ebugPrintln("doneTrading");
           if (ourTurn)
           //D.ebugPrintln("ourTurn");
           //D.ebugPrintln("whatWeWantToBuild = "+whatWeWantToBuild);
           //D.ebugPrintln("#===============");
           }
         */
    }

    /**
     * For each player in game:
     * client.sendText, and debug-print to console, game.getPlayer(i).getResources()
     */
    private void printResources()
    {
        if (D.ebugOn)
        {
            for (int i = 0; i < game.maxPlayers; i++)
            {
                ResourceSet rsrcs = game.getPlayer(i).getResources();
                String resourceMessage = "PLAYER " + i + " RESOURCES: ";
                resourceMessage += (rsrcs.getAmount(ResourceConstants.CLAY) + " ");
                resourceMessage += (rsrcs.getAmount(ResourceConstants.ORE) + " ");
                resourceMessage += (rsrcs.getAmount(ResourceConstants.SHEEP) + " ");
                resourceMessage += (rsrcs.getAmount(ResourceConstants.WHEAT) + " ");
                resourceMessage += (rsrcs.getAmount(ResourceConstants.WOOD) + " ");
                resourceMessage += (rsrcs.getAmount(ResourceConstants.UNKNOWN) + " ");
                client.sendText(game, resourceMessage);
                D.ebugPrintln(resourceMessage);
            }
        }
    }
}
