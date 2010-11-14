/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2007-2010 Jeremy D Monin <jeremy@nand.net>
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

import soc.game.Game;
import soc.game.GameOption;
import soc.game.Player;

import soc.message.AcceptOffer;
import soc.message.AdminPing;
import soc.message.AdminReset;
import soc.message.BoardLayout;
import soc.message.BoardLayout2;
import soc.message.CancelBuildRequest;
import soc.message.ChangeFace;
import soc.message.ChoosePlayerRequest;
import soc.message.ClearOffer;
import soc.message.ClearTradeMsg;
import soc.message.DeleteGame;
import soc.message.DevCard;
import soc.message.DevCardCount;
import soc.message.DiceResult;
import soc.message.DiscardRequest;
import soc.message.FirstPlayer;
import soc.message.GameMembers;
import soc.message.GameState;
import soc.message.GameTextMsg;
import soc.message.ImARobot;
import soc.message.JoinGame;
import soc.message.JoinGameAuth;
import soc.message.JoinGameRequest;
import soc.message.LargestArmy;
import soc.message.LeaveAll;
import soc.message.LeaveGame;
import soc.message.LongestRoad;
import soc.message.MakeOffer;
import soc.message.Message;
import soc.message.MoveRobber;
import soc.message.PlayerElement;
import soc.message.PotentialSettlements;
import soc.message.PutPiece;
import soc.message.RejectConnection;
import soc.message.RejectOffer;
import soc.message.ResetBoardAuth;
import soc.message.ResourceCount;
import soc.message.RobotDismiss;
import soc.message.ServerPing;
import soc.message.SetPlayedDevCard;
import soc.message.SetTurn;
import soc.message.SitDown;
import soc.message.StartGame;
import soc.message.StatusMessage;
import soc.message.Turn;
import soc.message.UpdateRobotParams;
import soc.message.SOCVersion;

import soc.server.genericServer.LocalStringServerSocket;

import soc.util.CappedQueue;
import soc.util.CutoffExceededException;
import soc.util.RobotParameters;
import soc.util.Version;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * This is a client that can play Settlers of Catan.
 *
 * @author Robert S Thomas
 */
public class RobotClient extends DisplaylessPlayerClient
{
    /**
     * constants for debug recording
     */
    public static final String CURRENT_PLANS = "CURRENT_PLANS";
    public static final String CURRENT_RESOURCES = "RESOURCES";

    /**
     * the thread the reads incoming messages
     */
    protected Thread reader;

    /**
     * the current robot parameters for robot brains
     */
    protected RobotParameters currentRobotParameters;

    /**
     * the robot's "brains" for different games
     */
    public Hashtable robotBrains = new Hashtable();

    /**
     * the message queues for the different brains
     */
    protected Hashtable brainQs = new Hashtable();

    /**
     * a table of requests from the server to sit at games
     */
    protected Hashtable seatRequests = new Hashtable();

    /**
     * options for all games on the server we've been asked to join.
     * Some games may have no options, so will have no entry here,
     * although they will have an entry in {@link #games} once joined.
     * Key = game name, Value = hashtable of {@link GameOption}.
     * Entries are added in {@link #handleJOINGAMEREQUEST(JoinGameRequest)}.
     * Since the robot and server are the same version, the
     * set of "known options" will always be in sync.
     */
    private Hashtable gameOptions = new Hashtable();

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
    RobotResetThread resetThread;

    /**
     * Have we printed the initial welcome msg from server?
     * Suppress further ones (disconnect-reconnect).
     * @since 1.1.06
     */
    boolean printedInitialWelcome = false;

    /**
     * Constructor for connecting to the specified host, on the specified port
     *
     * @param h  host
     * @param p  port
     * @param nn nickname for robot
     * @param pw password for robot
     */
    public RobotClient(String h, int p, String nn, String pw)
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
        strSocketName = null;
    }

    /**
     * Constructor for connecting to a local game (practice) on a local stringport.
     *
     * @param s    the stringport that the server listens on
     * @param nn   nickname for robot
     * @param pw   password for robot
     */
    public RobotClient(String s, String nn, String pw)
    {
        this(null, 0, nn, pw);
        strSocketName = s;
    }

    /**
     * Initialize the robot player; connect to server, send first messages
     */
    public void init_noImARobot()
    {
        try
        {
            if (strSocketName == null)
            {
                s = new Socket(host, port);
                s.setSoTimeout(300000);
                in = new DataInputStream(s.getInputStream());
                out = new DataOutputStream(s.getOutputStream());
            }
            else
            {
                sLocal = LocalStringServerSocket.connectTo(strSocketName);
            }               
            connected = true;
            reader = new Thread(this);
            reader.start();

            //resetThread = new RobotResetThread(this);
            //resetThread.start();
            put(SOCVersion.toCmd(Version.versionNumber(), Version.version(), Version.buildnum()));
            put(ImARobot.toCmd(nickname, ImARobot.RBCLASS_BUILTIN)); 
        }
        catch (Exception e)
        {
            ex = e;
            System.err.println("Could not connect to the server: " + ex);
        }
    }
    
    public void init()
    {
        init_noImARobot();
        try
        {
        put(ImARobot.toCmd(nickname, ImARobot.RBCLASS_BUILTIN));
        }
        catch (Exception e)
        {
            ex = e;
            System.err.println("Could not connect to the server: " + ex);
        }
    }

    /**
     * disconnect and then try to reconnect.
     * If the reconnect fails, {@link #ex} is set. Otherwise ex is null.
     */
    public void disconnectReconnect()
    {
        D.ebugPrintln("(*)(*)(*)(*)(*)(*)(*) disconnectReconnect()");
        ex = null;

        try
        {
            connected = false;
            if (strSocketName == null)
            {
                s.close();
                s = new Socket(host, port);
                in = new DataInputStream(s.getInputStream());
                out = new DataOutputStream(s.getOutputStream());
            }
            else
            {
                sLocal.disconnect();
                sLocal = LocalStringServerSocket.connectTo(strSocketName);
            }
            connected = true;
            reader = new Thread(this);
            reader.start();

            //resetThread = new RobotResetThread(this);
            //resetThread.start();
            put(SOCVersion.toCmd(Version.versionNumber(), Version.version(), Version.buildnum()));
            put(ImARobot.toCmd(nickname, ImARobot.RBCLASS_BUILTIN));
        }
        catch (Exception e)
        {
            ex = e;
            System.err.println("disconnectReconnect error: " + ex);
        }
    }

    /**
     * Treat the incoming messages.
     * Messages of unknown type are ignored (mes will be null from {@link Message#toMsg(String)}).
     *
     * @param mes    the message
     */
    @Override
    public void treat(Message mes)
    {
        if (mes == null)
            return;  // Message syntax error or unknown type

        D.ebugPrintln("IN - " + mes);

        try
        {
            switch (mes.getType())
            {
            /**
             * status message
             */
            case Message.STATUSMESSAGE:
                handleSTATUSMESSAGE((StatusMessage) mes);

                break;

            /**
             * server ping
             */
            case Message.SERVERPING:
                handleSERVERPING((ServerPing) mes);

                break;

            /**
             * admin ping
             */
            case Message.ADMINPING:
                handleADMINPING((AdminPing) mes);

                break;

            /**
             * admin reset
             */
            case Message.ADMINRESET:
                handleADMINRESET((AdminReset) mes);

                break;

            /**
             * update the current robot parameters
             */
            case Message.UPDATEROBOTPARAMS:
                handleUPDATEROBOTPARAMS((UpdateRobotParams) mes);

                break;

            /**
             * join game authorization
             */
            case Message.JOINGAMEAUTH:
                handleJOINGAMEAUTH((JoinGameAuth) mes);

                break;

            /**
             * someone joined a game
             */
            case Message.JOINGAME:
                handleJOINGAME((JoinGame) mes);

                break;

            /**
             * someone left a game
             */
            case Message.LEAVEGAME:
                handleLEAVEGAME((LeaveGame) mes);

                break;

            /**
             * game has been destroyed
             */
            case Message.DELETEGAME:
                handleDELETEGAME((DeleteGame) mes);

                break;

            /**
             * list of game members
             */
            case Message.GAMEMEMBERS:
                handleGAMEMEMBERS((GameMembers) mes);

                break;

            /**
             * game text message
             */
            case Message.GAMETEXTMSG:
                handleGAMETEXTMSG((GameTextMsg) mes);

                break;

            /**
             * someone is sitting down
             */
            case Message.SITDOWN:
                handleSITDOWN((SitDown) mes);

                break;

            /**
             * receive a board layout
             */
            case Message.BOARDLAYOUT:
                handleBOARDLAYOUT((BoardLayout) mes);  // in soc.client.DisplaylessPlayerClient
                break;

            /**
             * receive a board layout (new format, as of 20091104 (v 1.1.08))
             */
            case Message.BOARDLAYOUT2:
                handleBOARDLAYOUT2((BoardLayout2) mes);  // in soc.client.DisplaylessPlayerClient
                break;

            /**
             * message that the game is starting
             */
            case Message.STARTGAME:
                handleSTARTGAME((StartGame) mes);

                break;

            /**
             * update the state of the game
             */
            case Message.GAMESTATE:
                handleGAMESTATE((GameState) mes);

                break;

            /**
             * set the current turn
             */
            case Message.SETTURN:
                handleSETTURN((SetTurn) mes);

                break;

            /**
             * set who the first player is
             */
            case Message.FIRSTPLAYER:
                handleFIRSTPLAYER((FirstPlayer) mes);

                break;

            /**
             * update who's turn it is
             */
            case Message.TURN:
                handleTURN((Turn) mes);

                break;

            /**
             * receive player information
             */
            case Message.PLAYERELEMENT:
                handlePLAYERELEMENT((PlayerElement) mes);

                break;

            /**
             * receive resource count
             */
            case Message.RESOURCECOUNT:
                handleRESOURCECOUNT((ResourceCount) mes);

                break;

            /**
             * the latest dice result
             */
            case Message.DICERESULT:
                handleDICERESULT((DiceResult) mes);

                break;

            /**
             * a player built something
             */
            case Message.PUTPIECE:
                handlePUTPIECE((PutPiece) mes);

                break;

            /**
             * the current player has cancelled an initial settlement
             */
            case Message.CANCELBUILDREQUEST:
                handleCANCELBUILDREQUEST((CancelBuildRequest) mes);

                break;

            /**
             * the robber moved
             */
            case Message.MOVEROBBER:
                handleMOVEROBBER((MoveRobber) mes);

                break;

            /**
             * the server wants this player to discard
             */
            case Message.DISCARDREQUEST:
                handleDISCARDREQUEST((DiscardRequest) mes);

                break;

            /**
             * the server wants this player to choose a player to rob
             */
            case Message.CHOOSEPLAYERREQUEST:
                handleCHOOSEPLAYERREQUEST((ChoosePlayerRequest) mes);

                break;

            /**
             * a player has made an offer
             */
            case Message.MAKEOFFER:
                handleMAKEOFFER((MakeOffer) mes);

                break;

            /**
             * a player has cleared her offer
             */
            case Message.CLEAROFFER:
                handleCLEAROFFER((ClearOffer) mes);

                break;

            /**
             * a player has rejected an offer
             */
            case Message.REJECTOFFER:
                handleREJECTOFFER((RejectOffer) mes);

                break;

            /**
             * a player has accepted an offer
             */
            case Message.ACCEPTOFFER:
                handleACCEPTOFFER((AcceptOffer) mes);

                break;

            /**
             * the trade message needs to be cleared
             */
            case Message.CLEARTRADEMSG:
                handleCLEARTRADEMSG((ClearTradeMsg) mes);

                break;

            /**
             * the current number of development cards
             */
            case Message.DEVCARDCOUNT:
                handleDEVCARDCOUNT((DevCardCount) mes);

                break;

            /**
             * a dev card action, either draw, play, or add to hand
             */
            case Message.DEVCARD:
                handleDEVCARD((DevCard) mes);

                break;

            /**
             * set the flag that tells if a player has played a
             * development card this turn
             */
            case Message.SETPLAYEDDEVCARD:
                handleSETPLAYEDDEVCARD((SetPlayedDevCard) mes);

                break;

            /**
             * get a list of all the potential settlements for a player
             */
            case Message.POTENTIALSETTLEMENTS:
                handlePOTENTIALSETTLEMENTS((PotentialSettlements) mes);

                break;

            /**
             * the server is requesting that we join a game
             */
            case Message.JOINGAMEREQUEST:
                handleJOINGAMEREQUEST((JoinGameRequest) mes);

                break;

            /**
             * message that means the server wants us to leave the game
             */
            case Message.ROBOTDISMISS:
                handleROBOTDISMISS((RobotDismiss) mes);

                break;

            /**
             * handle the reject connection message - JM TODO: placement within switch? (vs displaylesscli, playercli) 
             */
            case Message.REJECTCONNECTION:
                handleREJECTCONNECTION((RejectConnection) mes);

                break;

            /**
             * handle board reset (new game with same players, same game name, new layout).
             */
            case Message.RESETBOARDAUTH:
                handleRESETBOARDAUTH((ResetBoardAuth) mes);

                break;
            }
        }
        catch (Throwable e)
        {
            System.err.println("RobotClient treat ERROR - " + e + " " + e.getMessage());
            e.printStackTrace();
            while (e.getCause() != null)
            {
                e = e.getCause();
                System.err.println(" -> nested: " + e.getClass());
                e.printStackTrace();
            }
            System.err.println("-- end stacktrace --");
        }
    }

    /**
     * handle the server ping message.
     * Echo back to server, to ensure we're still connected.
     * (ignored before version 1.1.08)
     *
     * @param mes  the message
     */
    protected void handleSERVERPING(ServerPing mes)
    {
        put(mes.toCmd());
        /*
           D.ebugPrintln("(*)(*) ServerPing message = "+mes);
           D.ebugPrintln("(*)(*) ServerPing sleepTime = "+mes.getSleepTime());
           D.ebugPrintln("(*)(*) resetThread = "+resetThread);
           resetThread.sleepMore();
         */
    }

    /**
     * handle the admin ping message
     * @param mes  the message
     */
    protected void handleADMINPING(AdminPing mes)
    {
        D.ebugPrintln("*** Admin Ping message = " + mes);

        Game ga = (Game) games.get(mes.getGame());

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
            put(JoinGame.toCmd(nickname, password, host, mes.getGame()));
        }
    }

    /**
     * handle the admin reset message
     * @param mes  the message
     */
    protected void handleADMINRESET(AdminReset mes)
    {
        D.ebugPrintln("*** Admin Reset message = " + mes);
        disconnectReconnect();
    }

    /**
     * handle the update robot params message
     * @param mes  the message
     */
    protected void handleUPDATEROBOTPARAMS(UpdateRobotParams mes)
    {
        currentRobotParameters = new RobotParameters(mes.getRobotParameters());
        D.ebugPrintln("*** current robot parameters = " + currentRobotParameters);
    }

    /**
     * handle the "join game request" message.
     * Remember the game options, and record in {@link #seatRequests}.
     * Send a {@link JoinGame JOINGAME} to server in response.
     * Server will reply with {@link JoinGameAuth JOINGAMEAUTH}.
     *<P>
     * Board resets are handled similarly.
     * @param mes  the message
     *
     * @see #handleRESETBOARDAUTH(ResetBoardAuth)
     */
    protected void handleJOINGAMEREQUEST(JoinGameRequest mes)
    {
        D.ebugPrintln("**** handleJOINGAMEREQUEST ****");
	final String gaName = mes.getGame();
	Hashtable gaOpts = mes.getOptions();
	if (gaOpts != null)
	    gameOptions.put(gaName, gaOpts);

        seatRequests.put(gaName, new Integer(mes.getPlayerNumber()));
        if (put(JoinGame.toCmd(nickname, password, host, gaName)))
        {
            D.ebugPrintln("**** sent JoinGame ****");
        }
    }

    /**
     * handle the "status message" message by printing it to System.err;
     * messages with status value 0 are ignored (no problem is being reported)
     * once the initial welcome message has been printed.
     * @param mes  the message
     */
    protected void handleSTATUSMESSAGE(StatusMessage mes)
    {
        final int sv = mes.getStatusValue();
        if ((sv != 0) || ! printedInitialWelcome)
        {
            System.err.println("Robot " + getNickname() + ": Status "
                + sv + " from server: " + mes.getStatus());
            if (sv == 0)
                printedInitialWelcome = true;
        }
    }

    /**
     * handle the "join game authorization" message
     * @param mes  the message
     */
    @Override
    protected void handleJOINGAMEAUTH(JoinGameAuth mes)
    {
        gamesPlayed++;

	final String gaName = mes.getGame();

	Game ga = new Game(gaName, true, (Hashtable) gameOptions.get(gaName));
        games.put(gaName, ga);

        CappedQueue brainQ = new CappedQueue();
        brainQs.put(gaName, brainQ);

        RobotBrain rb = new RobotBrain(this, currentRobotParameters, ga, brainQ);
        robotBrains.put(gaName, rb);
    }

    /**
     * handle the "join game" message
     * @param mes  the message
     */
    @Override
    protected void handleJOINGAME(JoinGame mes) {}

    /**
     * handle the "game members" message, which indicates the entire game state has now been sent.
     * If we have a {@link #seatRequests} for this game, sit down now.
     * @param mes  the message
     */
    protected void handleGAMEMEMBERS(GameMembers mes)
    {
        /**
         * sit down to play
         */
        Integer pn = (Integer) seatRequests.get(mes.getGame());

        try
        {
            //wait(Math.round(Math.random()*1000));
        }
        catch (Exception e)
        {
            ;
        }

        if (pn != null)
        {
            put(SitDown.toCmd(mes.getGame(), nickname, pn.intValue(), true));
        } else {
            System.err.println("** Cannot sit down: Assert failed: null pn for game " + mes.getGame());
        }
    }

    /**
     * handle the "game text message" message
     * @param mes  the message
     */
    protected void handleGAMETEXTMSG(GameTextMsg mes)
    {
        //D.ebugPrintln(mes.getNickname()+": "+mes.getText());
        if (mes.getText().startsWith(nickname + ":debug-off"))
        {
            Game ga = (Game) games.get(mes.getGame());
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if (brain != null)
            {
                brain.turnOffDRecorder();
                sendText(ga, "Debug mode OFF");
            }
        }

        if (mes.getText().startsWith(nickname + ":debug-on"))
        {
            Game ga = (Game) games.get(mes.getGame());
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if (brain != null)
            {
                brain.turnOnDRecorder();
                sendText(ga, "Debug mode ON");
            }
        }

        if (mes.getText().startsWith(nickname + ":current-plans") || mes.getText().startsWith(nickname + ":cp"))
        {
            Game ga = (Game) games.get(mes.getGame());
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain != null) && (brain.getDRecorder().isOn()))
            {
                sendRecordsText(ga, brain.getDRecorder().getRecord(CURRENT_PLANS));
            }
        }

        if (mes.getText().startsWith(nickname + ":current-resources") || mes.getText().startsWith(nickname + ":cr"))
        {
            Game ga = (Game) games.get(mes.getGame());
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain != null) && (brain.getDRecorder().isOn()))
            {
                sendRecordsText(ga, brain.getDRecorder().getRecord(CURRENT_RESOURCES));
            }
        }

        if (mes.getText().startsWith(nickname + ":last-plans") || mes.getText().startsWith(nickname + ":lp"))
        {
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain != null) && (brain.getDRecorder().isOn()))
            {
                Vector record = brain.getOldDRecorder().getRecord(CURRENT_PLANS);

                if (record != null)
                {
                    Game ga = (Game) games.get(mes.getGame());
                    sendRecordsText(ga, record);
                }
            }
        }

        if (mes.getText().startsWith(nickname + ":last-resources") || mes.getText().startsWith(nickname + ":lr"))
        {
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain != null) && (brain.getDRecorder().isOn()))
            {
                Vector record = brain.getOldDRecorder().getRecord(CURRENT_RESOURCES);

                if (record != null)
                {
                    Game ga = (Game) games.get(mes.getGame());
                    sendRecordsText(ga, record);
                }
            }
        }

        if (mes.getText().startsWith(nickname + ":last-move") || mes.getText().startsWith(nickname + ":lm"))
        {
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain != null) && (brain.getOldDRecorder().isOn()))
            {
                PossiblePiece lastMove = brain.getLastMove();

                if (lastMove != null)
                {
                    String key = null;

                    switch (lastMove.getType())
                    {
                    case PossiblePiece.CARD:
                        key = "DEVCARD";

                        break;

                    case PossiblePiece.ROAD:
                        key = "ROAD" + lastMove.getCoordinates();

                        break;

                    case PossiblePiece.SETTLEMENT:
                        key = "SETTLEMENT" + lastMove.getCoordinates();

                        break;

                    case PossiblePiece.CITY:
                        key = "CITY" + lastMove.getCoordinates();

                        break;
                    }

                    Vector record = brain.getOldDRecorder().getRecord(key);

                    if (record != null)
                    {
                        Game ga = (Game) games.get(mes.getGame());
                        sendRecordsText(ga, record);
                    }
                }
            }
        }

        if (mes.getText().startsWith(nickname + ":consider-move ") || mes.getText().startsWith(nickname + ":cm "))
        {
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain != null) && (brain.getOldDRecorder().isOn()))
            {
                String[] tokens = mes.getText().split(" ");
                String key = null;

                if (tokens[1].trim().equals("card"))
                {
                    key = "DEVCARD";
                }
                else if (tokens[1].equals("road"))
                {
                    key = "ROAD" + tokens[2].trim();
                }
                else if (tokens[1].equals("settlement"))
                {
                    key = "SETTLEMENT" + tokens[2].trim();
                }
                else if (tokens[1].equals("city"))
                {
                    key = "CITY" + tokens[2].trim();
                }

                Vector record = brain.getOldDRecorder().getRecord(key);

                if (record != null)
                {
                    Game ga = (Game) games.get(mes.getGame());
                    sendRecordsText(ga, record);
                }
            }
        }

        if (mes.getText().startsWith(nickname + ":last-target") || mes.getText().startsWith(nickname + ":lt"))
        {
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain != null) && (brain.getDRecorder().isOn()))
            {
                PossiblePiece lastTarget = brain.getLastTarget();

                if (lastTarget != null)
                {
                    String key = null;

                    switch (lastTarget.getType())
                    {
                    case PossiblePiece.CARD:
                        key = "DEVCARD";

                        break;

                    case PossiblePiece.ROAD:
                        key = "ROAD" + lastTarget.getCoordinates();

                        break;

                    case PossiblePiece.SETTLEMENT:
                        key = "SETTLEMENT" + lastTarget.getCoordinates();

                        break;

                    case PossiblePiece.CITY:
                        key = "CITY" + lastTarget.getCoordinates();

                        break;
                    }

                    Vector record = brain.getDRecorder().getRecord(key);

                    if (record != null)
                    {
                        Game ga = (Game) games.get(mes.getGame());
                        sendRecordsText(ga, record);
                    }
                }
            }
        }

        if (mes.getText().startsWith(nickname + ":consider-target ") || mes.getText().startsWith(nickname + ":ct "))
        {
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain != null) && (brain.getDRecorder().isOn()))
            {
                String[] tokens = mes.getText().split(" ");
                String key = null;

                if (tokens[1].trim().equals("card"))
                {
                    key = "DEVCARD";
                }
                else if (tokens[1].equals("road"))
                {
                    key = "ROAD" + tokens[2].trim();
                }
                else if (tokens[1].equals("settlement"))
                {
                    key = "SETTLEMENT" + tokens[2].trim();
                }
                else if (tokens[1].equals("city"))
                {
                    key = "CITY" + tokens[2].trim();
                }

                Vector record = brain.getDRecorder().getRecord(key);

                if (record != null)
                {
                    Game ga = (Game) games.get(mes.getGame());
                    sendRecordsText(ga, record);
                }
            }
        }

        if (mes.getText().startsWith(nickname + ":stats"))
        {
            Game ga = (Game) games.get(mes.getGame());
            sendText(ga, "Games played:" + gamesPlayed);
            sendText(ga, "Games finished:" + gamesFinished);
            sendText(ga, "Games won:" + gamesWon);
            sendText(ga, "Clean brain kills:" + cleanBrainKills);
            sendText(ga, "Brains running: " + robotBrains.size());
 
            Runtime rt = Runtime.getRuntime();
            sendText(ga, "Total Memory:" + rt.totalMemory());
            sendText(ga, "Free Memory:" + rt.freeMemory());
        }

        if (mes.getText().startsWith(nickname + ":gc"))
        {
            Game ga = (Game) games.get(mes.getGame());
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            sendText(ga, "Free Memory:" + rt.freeMemory());
        }

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
     * handle the "someone is sitting down" message
     * @param mes  the message
     */
    protected void handleSITDOWN(SitDown mes)
    {
        /**
         * tell the game that a player is sitting
         */
        Game ga = (Game) games.get(mes.getGame());

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
                RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

                /**
                 * retrieve the proper face for our strategy
                 */
                int faceId;
                switch (brain.getRobotParameters().getStrategyType())
                {
                case RobotDM.SMART_STRATEGY:
                    faceId = -1;  // smarter robot face
                    break;

                default:
                    faceId = 0;   // default robot face
                }

                brain.setOurPlayerData();
                brain.start();

                /**
                 * change our face to the robot face
                 */
                put(ChangeFace.toCmd(ga.getName(), mes.getPlayerNumber(), faceId));
            }
            else
            {
                /**
                 * add tracker for player in previously vacant seat
                 */
                RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

                if (brain != null)
                {
                    brain.addPlayerTracker(mes.getPlayerNumber());
                }
            }
        }
    }

    /**
     * handle the "start game" message
     * @param mes  the message
     */
    protected void handleSTARTGAME(StartGame mes) {}

    /**
     * handle the "delete game" message
     * @param mes  the message
     */
    protected void handleDELETEGAME(DeleteGame mes)
    {
        RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

        if (brain != null)
        {
            Game ga = (Game) games.get(mes.getGame());

            if (ga != null)
            {
                if (ga.getGameState() == Game.OVER)
                {
                    gamesFinished++;

                    if (ga.getPlayer(nickname).getTotalVP() >= Game.VP_WINNER)
                    {
                        gamesWon++;
                        // TODO: hardcoded, assumes 10 to win (VP_WINNER)
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
    protected void handleGAMESTATE(GameState mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
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
    }

    /**
     * handle the "set turn" message
     * @param mes  the message
     */
    protected void handleSETTURN(SetTurn mes)
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
     * handle the "set first player" message
     * @param mes  the message
     */
    protected void handleFIRSTPLAYER(FirstPlayer mes)
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
     * handle the "turn" message
     * @param mes  the message
     */
    protected void handleTURN(Turn mes)
    {
    	System.out.println("SOCROBOTCLIENT HANDLE TURN");
    	
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
     * handle the "player element" message
     * @param mes  the message
     */
    protected void handlePLAYERELEMENT(PlayerElement mes)
    {
    	System.out.println("handle player element in the method");
    	
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
     * handle "resource count" message
     * @param mes  the message
     */
    protected void handleRESOURCECOUNT(ResourceCount mes)
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
     * handle the "dice result" message
     * @param mes  the message
     */
    protected void handleDICERESULT(DiceResult mes)
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
     * handle the "put piece" message
     * @param mes  the message
     */
    protected void handlePUTPIECE(PutPiece mes)
    {
    	
    	System.out.println("PUT PIECE CALLED IN RobotClient");
    	
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

            Game ga = (Game) games.get(mes.getGame());

            if (ga != null)
            {
                // Player pl = ga.getPlayer(mes.getPlayerNumber());
                // JDM TODO - Was this in stock client?
            }
        }
    }

    /**
     * handle the rare "cancel build request" message; usually not sent from
     * server to client.
     *<P>
     * - When sent from client to server, CANCELBUILDREQUEST means the player has changed
     *   their mind about spending resources to build a piece.  Only allowed during normal
     *   game play (PLACING_ROAD, PLACING_SETTLEMENT, or PLACING_CITY).
     *<P>
     *  When sent from server to client:
     *<P>
     * - During game startup (START1B or START2B): <BR>
     *       Sent from server, CANCELBUILDREQUEST means the current player
     *       wants to undo the placement of their initial settlement.  
     *<P>
     * - During piece placement (PLACING_ROAD, PLACING_CITY, PLACING_SETTLEMENT,
     *                           PLACING_FREE_ROAD1 or PLACING_FREE_ROAD2):
     *<P>
     *      Sent from server, CANCELBUILDREQUEST means the player has sent
     *      an illegal PUTPIECE (bad building location). Humans can probably
     *      decide a better place to put their road, but robots must cancel
     *      the build request and decide on a new plan.
     *<P>
     *      Our robot client sends this to the brain to act on.
     *
     * @param mes  the message
     */
    protected void handleCANCELBUILDREQUEST(CancelBuildRequest mes)
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
     * handle the "move robber" message
     * @param mes  the message
     */
    protected void handleMOVEROBBER(MoveRobber mes)
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
     * handle the "discard request" message
     * @param mes  the message
     */
    protected void handleDISCARDREQUEST(DiscardRequest mes)
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
     * handle the "choose player request" message
     * @param mes  the message
     */
    protected void handleCHOOSEPLAYERREQUEST(ChoosePlayerRequest mes)
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
     * handle the "make offer" message
     * @param mes  the message
     */
    protected void handleMAKEOFFER(MakeOffer mes)
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
     * handle the "clear offer" message
     * @param mes  the message
     */
    protected void handleCLEAROFFER(ClearOffer mes)
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
    protected void handleREJECTOFFER(RejectOffer mes)
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
     * handle the "accept offer" message
     * @param mes  the message
     */
    protected void handleACCEPTOFFER(AcceptOffer mes)
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
     * handle the "clear trade" message
     * @param mes  the message
     */
    protected void handleCLEARTRADEMSG(ClearTradeMsg mes) {}

    /**
     * handle the "development card count" message
     * @param mes  the message
     */
    protected void handleDEVCARDCOUNT(DevCardCount mes)
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
     * handle the "development card action" message
     * @param mes  the message
     */
    protected void handleDEVCARD(DevCard mes)
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
     * handle the "set played development card" message
     * @param mes  the message
     */
    protected void handleSETPLAYEDDEVCARD(SetPlayedDevCard mes)
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
     * handle the "dismiss robot" message
     * @param mes  the message
     */
    protected void handleROBOTDISMISS(RobotDismiss mes)
    {
        Game ga = (Game) games.get(mes.getGame());
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
            RobotBrain brain = (RobotBrain) robotBrains.get(mes.getGame());

            if ((brain == null) || (!brain.isAlive()))
            {
                leaveGame((Game) games.get(mes.getGame()), "brain not alive", false);
            }
        }
    }

    /**
     * handle the "potential settlements" message
     * @param mes  the message
     */
    protected void handlePOTENTIALSETTLEMENTS(PotentialSettlements mes)
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
     * handle the "change face" message
     * @param mes  the message
     */
    protected void handleCHANGEFACE(ChangeFace mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            Player player = ga.getPlayer(mes.getPlayerNumber());
            player.setFaceId(mes.getFaceId());
        }
    }

    /**
     * handle the "longest road" message
     * @param mes  the message
     */
    protected void handleLONGESTROAD(LongestRoad mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            if (mes.getPlayerNumber() == -1)
            {
                ga.setPlayerWithLongestRoad((Player) null);
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
    protected void handleLARGESTARMY(LargestArmy mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            if (mes.getPlayerNumber() == -1)
            {
                ga.setPlayerWithLargestArmy((Player) null);
            }
            else
            {
                ga.setPlayerWithLargestArmy(ga.getPlayer(mes.getPlayerNumber()));
            }
        }
    }

    /**
     * handle board reset
     * (new game with same players, same game name).
     * Destroy old Game object.
     * Take robotbrain out of old game, don't yet put it in new game.
     * Let server know we've done so, by sending LEAVEGAME via {@link #leaveGame(Game, String, boolean)}.
     * Server will soon send a JOINGAMEREQUEST if we should join the new game.
     *
     * @param mes  the message
     * 
     * @see soc.server.SOCServer#resetBoardAndNotify(String, int)
     * @see soc.game.Game#resetAsCopy()
     * @see #handleJOINGAMEREQUEST(JoinGameRequest)
     */
    protected void handleRESETBOARDAUTH(ResetBoardAuth mes)
    {
        D.ebugPrintln("**** handleRESETBOARDAUTH ****");

        String gname = mes.getGame();
        Game ga = (Game) games.get(gname);
        if (ga == null)
            return;  // Not one of our games

        RobotBrain brain = (RobotBrain) robotBrains.get(gname);
        if (brain != null)
            brain.kill();
        leaveGame(ga, "resetboardauth", false);  // Same as in handleROBOTDISMISS
        ga.destroyGame();
    }

    /**
     * Call sendText on each string element of record.
     * @param ga Game to sendText to
     * @param record Strings to send, or null
     */
    protected void sendRecordsText(Game ga, Vector record)
    {
        if (record != null)
        {
            Enumeration renum = record.elements();

            while (renum.hasMoreElements())
            {
                String str = (String) renum.nextElement();
                sendText(ga, str);
            }
        }
    }

    /**
     * the user leaves the given game
     *
     * @param ga   the game
     * @param leaveReason reason for leaving
     */
    public void leaveGame(Game ga, String leaveReason, boolean showDebugTrace)
    {
        if (ga != null)
        {
            robotBrains.remove(ga.getName());
            brainQs.remove(ga.getName());
            games.remove(ga.getName());
            System.err.println("L1833 robot " + nickname + " leaving game " + ga + " due to " + leaveReason);
            if (showDebugTrace)
            {
                soc.debug.D.ebugPrintStackTrace(null, "Leaving game here");
                System.err.flush();
            }
            put(LeaveGame.toCmd(nickname, host, ga.getName()));
        }
    }

    /**
     * add one the the number of clean brain kills
     */
    public void addCleanKill()
    {
        cleanBrainKills++;
    }

    /** losing connection to server; leave all games, then try to reconnect */
    public void destroy()
    {
        LeaveAll leaveAllMes = new LeaveAll();
        put(leaveAllMes.toCmd());
        disconnectReconnect();
        if (ex != null)
            System.err.println("Reconnect to server failed: " + ex);
    }

    /**
     * for stand-alones
     */
    public static void main(String[] args)
    {
        if (args.length < 4)
        {
            System.err.println("Java Settlers robotclient " + Version.version() +
                    ", build " + Version.buildnum());
            System.err.println("usage: java soc.robot.RobotClient host port_number userid password");
            return;
        }

        RobotClient ex1 = new RobotClient(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        ex1.init();
    }
}
