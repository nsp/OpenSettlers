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
package soc.client;

import soc.disableDebug.D;

import soc.game.Board;
import soc.game.City;
import soc.game.DevCardSet;
import soc.game.Game;
import soc.game.Player;
import soc.game.PlayingPiece;
import soc.game.ResourceConstants;
import soc.game.ResourceSet;
import soc.game.Road;
import soc.game.Settlement;
import soc.game.TradeOffer;

import soc.message.AcceptOffer;
import soc.message.BCastTextMsg;
import soc.message.BankTrade;
import soc.message.BoardLayout;
import soc.message.BoardLayout2;
import soc.message.BuildRequest;
import soc.message.BuyCardRequest;
import soc.message.CancelBuildRequest;
import soc.message.ChangeFace;
import soc.message.Channels;
import soc.message.ChoosePlayer;
import soc.message.ChoosePlayerRequest;
import soc.message.ClearOffer;
import soc.message.ClearTradeMsg;
import soc.message.DeleteChannel;
import soc.message.DeleteGame;
import soc.message.DevCard;
import soc.message.DevCardCount;
import soc.message.DiceResult;
import soc.message.Discard;
import soc.message.DiscardRequest;
import soc.message.DiscoveryPick;
import soc.message.EndTurn;
import soc.message.FirstPlayer;
import soc.message.GameMembers;
import soc.message.GameState;
import soc.message.GameStats;
import soc.message.GameTextMsg;
import soc.message.Games;
import soc.message.Join;
import soc.message.JoinAuth;
import soc.message.JoinGame;
import soc.message.JoinGameAuth;
import soc.message.LargestArmy;
import soc.message.Leave;
import soc.message.LeaveAll;
import soc.message.LeaveGame;
import soc.message.LongestRoad;
import soc.message.MakeOffer;
import soc.message.Members;
import soc.message.Message;
import soc.message.MonopolyPick;
import soc.message.MoveRobber;
import soc.message.NewChannel;
import soc.message.NewGame;
import soc.message.PlayDevCardRequest;
import soc.message.PlayerElement;
import soc.message.PotentialSettlements;
import soc.message.PutPiece;
import soc.message.RejectConnection;
import soc.message.RejectOffer;
import soc.message.ResetBoardAuth;
import soc.message.ResourceCount;
import soc.message.RollDice;
import soc.message.SetPlayedDevCard;
import soc.message.SetSeatLock;
import soc.message.SetTurn;
import soc.message.SitDown;
import soc.message.StartGame;
import soc.message.StatusMessage;
import soc.message.TextMsg;
import soc.message.Turn;
import soc.message.SOCVersion;
import soc.robot.RobotClient;
import soc.server.genericServer.LocalStringConnection;
import soc.util.Version;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.Socket;

import java.util.Hashtable;


/**
 * GUI-less standalone client for connecting to the SOCServer.
 * If you want another connection port, you have to specify it as the "port"
 * argument in the html source. If you run this as a stand-alone, you have to
 * specify the port.
 *<P>
 * The {@link soc.robot.RobotClient} is based on this client.
 *
 * @author Robert S Thomas
 */
public class DisplaylessPlayerClient implements Runnable
{
    protected static String STATSPREFEX = "  [";
    protected String doc;
    protected String lastMessage;

    protected String host;
    protected int port;
    protected String strSocketName;  // For robots in local practice games
    protected Socket s;
    protected DataInputStream in;
    protected DataOutputStream out;
    protected LocalStringConnection sLocal;  // if strSocketName not null
    /** Server version number, sent soon after connect, or -1 if unknown */
    protected int sVersion, sLocalVersion;
    protected Thread reader = null;
    protected Exception ex = null;
    protected boolean connected = false;

    /** 
     * were we rejected from server? (full or robot name taken)
     */
    protected boolean rejected = false;

    /**
     * the nickname
     */
    protected String nickname = null;

    /**
     * the password
     */
    protected String password = null;

    /**
     * true if we've stored the password
     */
    protected boolean gotPassword;

    /**
     * the channels
     */
    protected Hashtable channels = new Hashtable();

    /**
     * the games we're playing
     */
    protected Hashtable games = new Hashtable();

    /**
     * Create a DisplaylessPlayerClient, which would connect to localhost port 8889.
     * Does not actually connect; subclass must connect, such as {@link soc.robot.RobotClient#init()}
     *<P>
     * <b>Note:</b> The default OpenSettlers server port is 8880.
     */
    public DisplaylessPlayerClient()
    {
        host = null;
        port = 8889;
        strSocketName = null;
        gotPassword = false;
        sVersion = -1;  sLocalVersion = -1;
    }

    /**
     * Constructor for connecting to the specified host, on the specified port.
     * Does not actually connect; subclass must connect, such as {@link soc.robot.RobotClient#init()}
     *
     * @param h  host
     * @param p  port
     * @param visual  true if this client is visual
     */
    public DisplaylessPlayerClient(String h, int p, boolean visual)
    {
        host = h;
        port = p;
        strSocketName = null;
        sVersion = -1;  sLocalVersion = -1;
    }

    /**
     * Constructor for connecting to a local game (practice) on a local stringport.
     * Does not actually connect; subclass must connect, such as {@link soc.robot.RobotClient#init()}
     *
     * @param s    the stringport that the server listens on
     * @param visual  true if this client is visual
     */
    public DisplaylessPlayerClient(String s, boolean visual)
    {
        this();         
        strSocketName = s;
    }

    /**
     * @return the nickname of this user
     */
    public String getNickname()
    {
        return nickname;
    }

    /**
     * continuously read from the net in a separate thread
     */
    public void run()
    {
        // Thread name for debug
        try
        {
            Thread.currentThread().setName("robot-netread-" + nickname);
        }
        catch (Throwable th) {}
        
        try
        {
            while (connected)
            {
                String s;
                if (sLocal == null)
                    s = in.readUTF();
                else
                    s = sLocal.readNext();
                System.out.println("run method in Displayless with string :: "+s);
                
                treat((Message) Message.toMsg(s));
            }
        }
        catch (InterruptedIOException x)
        {
            System.err.println("Socket timeout in run: " + x);
        }
        catch (IOException e)
        {
            if (!connected)
            {
                return;
            }

            ex = e;
            if (! ((e instanceof java.io.EOFException)
                  && (this instanceof RobotClient)))
            {
                System.err.println("could not read from the net: " + ex);
                /**
                 * Robots are periodically disconnected from server;
                 * they will try to reconnect.  Any error message
                 * from that is printed in {@link soc.robot.RobotClient#destroy()}.
                 * So, print nothing here if that's the situation.
                 */
            }
            destroy();
        }
    }

    /**
     * resend the last message
     */
    public void resend()
    {
        put(lastMessage);
    }

    /**
     * write a message to the net
     *
     * @param s  the message
     * @return true if the message was sent, false if not
     */
    public synchronized boolean put(String s)
    {
        lastMessage = s;
        
        System.out.println("put() in SocDisplaylessplayerclient :: message :: "+lastMessage);
        
        D.ebugPrintln("OUT - " + s);

        if ((ex != null) || !connected)
        {
            return false;
        }

        try
        {
            if (sLocal == null)
                out.writeUTF(s);
            else
                sLocal.put(s);
        }
        catch (InterruptedIOException x)
        {
            System.err.println("Socket timeout in put: " + x);
        }
        catch (IOException e)
        {
            ex = e;
            System.err.println("could not write to the net: " + ex);
            destroy();

            return false;
        }

        return true;
    }

    /**
     * Treat the incoming messages.
     * Messages of unknown type are ignored (mes will be null from {@link Message#toMsg(String)}).
     *
     * @param mes    the message
     */
    public void treat(Message mes)
    {
        if (mes == null)
            return;  // Msg parsing error

        D.ebugPrintln(mes.toString());

        try
        {
            switch (mes.getType())
            {
            /**
             * server's version message
             */
            case Message.VERSION:
                handleVERSION((sLocal != null), (SOCVersion) mes);

                break;

            /**
             * status message
             */
            case Message.STATUSMESSAGE:
                handleSTATUSMESSAGE((StatusMessage) mes);

                break;

            /**
             * join channel authorization
             */
            case Message.JOINAUTH:
                handleJOINAUTH((JoinAuth) mes);

                break;

            /**
             * someone joined a channel
             */
            case Message.JOIN:
                handleJOIN((Join) mes);

                break;

            /**
             * list of members for a channel
             */
            case Message.MEMBERS:
                handleMEMBERS((Members) mes);

                break;

            /**
             * a new channel has been created
             */
            case Message.NEWCHANNEL:
                handleNEWCHANNEL((NewChannel) mes);

                break;

            /**
             * list of channels on the server
             */
            case Message.CHANNELS:
                handleCHANNELS((Channels) mes);

                break;

            /**
             * text message
             */
            case Message.TEXTMSG:
                handleTEXTMSG((TextMsg) mes);

                break;

            /**
             * someone left the channel
             */
            case Message.LEAVE:
                handleLEAVE((Leave) mes);

                break;

            /**
             * delete a channel
             */
            case Message.DELETECHANNEL:
                handleDELETECHANNEL((DeleteChannel) mes);

                break;

            /**
             * list of games on the server
             */
            case Message.GAMES:
                handleGAMES((Games) mes);

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
		System.out.println("someone left the game");
                break;

            /**
             * new game has been created
             */
            case Message.NEWGAME:
                handleNEWGAME((NewGame) mes);

                break;

            /**
             * game has been destroyed
             */
            case Message.DELETEGAME:
                handleDELETEGAME((DeleteGame) mes);
		System.out.println("game destroyed");
                break;

            /**
             * list of game members
             */
            case Message.GAMEMEMBERS:
                handleGAMEMEMBERS((GameMembers) mes);

                break;

            /**
             * game stats
             */
            case Message.GAMESTATS:
                handleGAMESTATS((GameStats) mes);

                break;

            /**
             * game text message
             */
            case Message.GAMETEXTMSG:
                handleGAMETEXTMSG((GameTextMsg) mes);

                break;

            /**
             * broadcast text message
             */
            case Message.BCASTTEXTMSG:
                handleBCASTTEXTMSG((BCastTextMsg) mes);

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
                handleBOARDLAYOUT((BoardLayout) mes);

                break;

            /**
             * receive a board layout (new format, as of 20091104 (v 1.1.08))
             */
            case Message.BOARDLAYOUT2:
                handleBOARDLAYOUT2((BoardLayout2) mes);
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
             * the current player has cancelled an initial settlement,
             * or has tried to place a piece illegally. 
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
             * handle the change face message
             */
            case Message.CHANGEFACE:
                handleCHANGEFACE((ChangeFace) mes);

                break;

            /**
             * handle the reject connection message
             */
            case Message.REJECTCONNECTION:
                handleREJECTCONNECTION((RejectConnection) mes);

                break;

            /**
             * handle the longest road message
             */
            case Message.LONGESTROAD:
                handleLONGESTROAD((LongestRoad) mes);

                break;

            /**
             * handle the largest army message
             */
            case Message.LARGESTARMY:
                handleLARGESTARMY((LargestArmy) mes);

                break;

            /**
             * handle the seat lock state message
             */
            case Message.SETSEATLOCK:
                handleSETSEATLOCK((SetSeatLock) mes);

                break;

            /**
             * handle board reset (new game with same players, same game name, new layout).
             */
            case Message.RESETBOARDAUTH:
                handleRESETBOARDAUTH((ResetBoardAuth) mes);

                break;

            }
        }
        catch (Exception e)
        {
            System.out.println("DisplaylessPlayerClient treat ERROR - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * handle the "status message" message
     * @param mes  the message
     */
    protected void handleSTATUSMESSAGE(StatusMessage mes) {}

    /**
     * handle the "join authorization" message
     * @param mes  the message
     */
    protected void handleJOINAUTH(JoinAuth mes)
    {
        gotPassword = true;
    }

    /**
     * Handle the "version" message, server's version report.
     * Reply with client's version.
     *<P>
     * Because DisplaylessPlayerClient is used only for the
     * robot, and the robot should always be the same version as
     * the server, don't ask server for info about
     * {@link soc.game.GameOption game option} deltas between
     * the two versions.
     *
     * @param isLocal Is the server local, or remote?  Client can be connected
     *                only to local, or remote.
     * @param mes  the messsage
     */
    private void handleVERSION(boolean isLocal, SOCVersion mes)
    {
        D.ebugPrintln("handleVERSION: " + mes);
        int vers = mes.getVersionNumber();
        if (isLocal)
            sLocalVersion = vers;
        else
            sVersion = vers;

        // TODO check for minimum,maximum

        // Reply with our own version.
        put(SOCVersion.toCmd(Version.versionNumber(), Version.version(), Version.buildnum()));

        // Don't check for game options different at version, unlike PlayerClient.handleVERSION.
    }

    /**
     * handle the "join channel" message
     * @param mes  the message
     */
    protected void handleJOIN(Join mes) {}

    /**
     * handle the "members" message
     * @param mes  the message
     */
    protected void handleMEMBERS(Members mes) {}

    /**
     * handle the "new channel" message
     * @param mes  the message
     */
    protected void handleNEWCHANNEL(NewChannel mes) {}

    /**
     * handle the "list of channels" message
     * @param mes  the message
     */
    protected void handleCHANNELS(Channels mes) {}

    /**
     * handle a broadcast text message
     * @param mes  the message
     */
    protected void handleBCASTTEXTMSG(BCastTextMsg mes) {}

    /**
     * handle a text message
     * @param mes  the message
     */
    protected void handleTEXTMSG(TextMsg mes) {}

    /**
     * handle the "leave channel" message
     * @param mes  the message
     */
    protected void handleLEAVE(Leave mes) {}

    /**
     * handle the "delete channel" message
     * @param mes  the message
     */
    protected void handleDELETECHANNEL(DeleteChannel mes) {}

    /**
     * handle the "list of games" message
     * @param mes  the message
     */
    protected void handleGAMES(Games mes) {}

    /**
     * handle the "join game authorization" message
     * @param mes  the message
     */
    protected void handleJOINGAMEAUTH(JoinGameAuth mes)
    {
        gotPassword = true;

        Game ga = new Game(mes.getGame());
        games.put(mes.getGame(), ga);
    }

    /**
     * handle the "join game" message
     * @param mes  the message
     */
    protected void handleJOINGAME(JoinGame mes) {}

    /**
     * handle the "leave game" message
     * @param mes  the message
     */
    protected void handleLEAVEGAME(LeaveGame mes)
    {
        String gn = (mes.getGame());
        Game ga = (Game) games.get(gn);

        if (ga != null)
        {
            Player player = ga.getPlayer(mes.getNickname());

            if (player != null)
            {
                //
                //  This user was not a spectator
                //
                ga.removePlayer(mes.getNickname());
            }
        }
    }

    /**
     * handle the "new game" message
     * @param mes  the message
     */
    protected void handleNEWGAME(NewGame mes) {}

    /**
     * handle the "delete game" message
     * @param mes  the message
     */
    protected void handleDELETEGAME(DeleteGame mes) {}

    /**
     * handle the "game members" message
     * @param mes  the message
     */
    protected void handleGAMEMEMBERS(GameMembers mes) {}

    /**
     * handle the "game stats" message
     */
    protected void handleGAMESTATS(GameStats mes) {}

    /**
     * handle the "game text message" message
     * @param mes  the message
     */
    protected void handleGAMETEXTMSG(GameTextMsg mes) {}

    /**
     * handle the "player sitting down" message
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
            ga.takeMonitor();

            try
            {
                ga.addPlayer(mes.getNickname(), mes.getPlayerNumber());

                /**
                 * set the robot flag
                 */
                ga.getPlayer(mes.getPlayerNumber()).setRobotFlag(mes.isRobot(), false);
            }
            catch (Exception e)
            {
                ga.releaseMonitor();
                System.out.println("Exception caught - " + e);
                e.printStackTrace();
            }

            ga.releaseMonitor();
        }
    }

    /**
     * handle the "board layout" message
     * @param mes  the message
     */
    protected void handleBOARDLAYOUT(BoardLayout mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            Board bd = ga.getBoard();
            bd.setHexLayout(mes.getHexLayout());
            bd.setNumberLayout(mes.getNumberLayout());
            bd.setRobberHex(mes.getRobberHex());
        }
    }

    /**
     * handle the "board layout" message, new format
     * @param mes  the message
     * @since 1.1.08
     */
    protected void handleBOARDLAYOUT2(BoardLayout2 mes)
    {
        Game ga = (Game) games.get(mes.getGame());
        if (ga == null)
            return;

        Board bd = ga.getBoard();
        bd.setBoardEncodingFormat(mes.getBoardEncodingFormat());
        bd.setHexLayout(mes.getIntArrayPart("HL"));
        bd.setNumberLayout(mes.getIntArrayPart("NL"));
        bd.setRobberHex(mes.getIntPart("RH"));
        int[] portLayout = mes.getIntArrayPart("PL");
        if (portLayout != null)
            bd.setPortsLayout(portLayout);
    }

    /**
     * handle the "start game" message
     * @param mes  the message
     */
    protected void handleSTARTGAME(StartGame mes) {}

    /**
     * handle the "game state" message
     * @param mes  the message
     */
    protected void handleGAMESTATE(GameState mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setGameState(mes.getState());
        }
    }

    /**
     * handle the "set turn" message
     * @param mes  the message
     */
    protected void handleSETTURN(SetTurn mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setCurrentPlayerNumber(mes.getPlayerNumber());
        }
    }

    /**
     * handle the "first player" message
     * @param mes  the message
     */
    protected void handleFIRSTPLAYER(FirstPlayer mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setFirstPlayer(mes.getPlayerNumber());
        }
    }

    /**
     * handle the "turn" message
     * @param mes  the message
     */
    protected void handleTURN(Turn mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setCurrentPlayerNumber(mes.getPlayerNumber());
            ga.updateAtTurn();
        }
    }

    /**
     * handle the "player information" message
     * @param mes  the message
     */
    protected void handlePLAYERELEMENT(PlayerElement mes)
    {
     
    	
    	
        final Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            final Player pl = ga.getPlayer(mes.getPlayerNumber());

            switch (mes.getElementType())
            {
            case PlayerElement.ROADS:

                handlePLAYERELEMENT_numPieces(mes, pl, PlayingPiece.ROAD);
                break;

            case PlayerElement.SETTLEMENTS:

                handlePLAYERELEMENT_numPieces(mes, pl, PlayingPiece.SETTLEMENT);
                break;

            case PlayerElement.CITIES:

                handlePLAYERELEMENT_numPieces(mes, pl, PlayingPiece.CITY);
                break;

            case PlayerElement.NUMKNIGHTS:

                // PLAYERELEMENT(NUMKNIGHTS) is sent after a Soldier card is played.
                handlePLAYERELEMENT_numKnights(mes, pl, ga);
                break;

            case PlayerElement.CLAY:

                handlePLAYERELEMENT_numRsrc(mes, pl, ResourceConstants.CLAY);
                break;

            case PlayerElement.ORE:

                handlePLAYERELEMENT_numRsrc(mes, pl, ResourceConstants.ORE);
                break;

            case PlayerElement.SHEEP:

                handlePLAYERELEMENT_numRsrc(mes, pl, ResourceConstants.SHEEP);
                break;

            case PlayerElement.WHEAT:

                handlePLAYERELEMENT_numRsrc(mes, pl, ResourceConstants.WHEAT);
                break;

            case PlayerElement.WOOD:

                handlePLAYERELEMENT_numRsrc(mes, pl, ResourceConstants.WOOD);
                break;

            case PlayerElement.UNKNOWN:

                /**
                 * Note: if losing unknown resources, we first
                 * convert player's known resources to unknown resources,
                 * then remove mes's unknown resources from player.
                 */
                handlePLAYERELEMENT_numRsrc(mes, pl, ResourceConstants.UNKNOWN);
                break;

            case PlayerElement.ASK_SPECIAL_BUILD:
                if (0 != mes.getValue())
                {
                    try {
                        ga.askSpecialBuild(pl.getPlayerNumber(), false);  // set per-player, per-game flags
                    }
                    catch (RuntimeException e) {}
                } else {
                    pl.setAskedSpecialBuild(false);
                }
                break;

            }
        }
    }

    /**
     * Update a player's amount of a playing piece, for {@link #handlePLAYERELEMENT(PlayerElement)}.
     * To avoid code duplication, also called from
     * {@link PlayerClient#handlePLAYERELEMENT(PlayerElement)}
     * and {@link soc.robot.RobotBrain#run()}.
     *
     * @param mes       Message with amount and action (SET/GAIN/LOSE)
     * @param pl        Player to update
     * @param pieceType Playing piece type, as in {@link PlayingPiece#ROAD}
     */
    public static void handlePLAYERELEMENT_numPieces
        (PlayerElement mes, final Player pl, int pieceType)
    {
        switch (mes.getAction())
        {
        case PlayerElement.SET:
            pl.setNumPieces(pieceType, mes.getValue());

            break;

        case PlayerElement.GAIN:
            pl.setNumPieces(pieceType, pl.getNumPieces(pieceType) + mes.getValue());

            break;

        case PlayerElement.LOSE:
            pl.setNumPieces(pieceType, pl.getNumPieces(pieceType) - mes.getValue());

            break;
        }
    }

    /**
     * Update a player's amount of knights, and game's largest army,
     * for {@link #handlePLAYERELEMENT(PlayerElement)}.
     * To avoid code duplication, also called from
     * {@link PlayerClient#handlePLAYERELEMENT(PlayerElement)}
     * and {@link soc.robot.RobotBrain#run()}.
     *
     * @param mes  Message with amount and action (SET/GAIN/LOSE)
     * @param pl   Player to update
     * @param ga   Game of player
     */
    public static void handlePLAYERELEMENT_numKnights
        (PlayerElement mes, final Player pl, final Game ga)
    {
        switch (mes.getAction())
        {
        case PlayerElement.SET:
            pl.setNumKnights(mes.getValue());
    
            break;
    
        case PlayerElement.GAIN:
            pl.setNumKnights(pl.getNumKnights() + mes.getValue());
    
            break;
    
        case PlayerElement.LOSE:
            pl.setNumKnights(pl.getNumKnights() - mes.getValue());
    
            break;
        }
    
        ga.updateLargestArmy();
    }
    
    /**
     * Update a player's amount of a resource, for {@link #handlePLAYERELEMENT(PlayerElement)}.
     *<ul>
     *<LI> If this is a {@link PlayerElement#LOSE} action, and the player does not have enough of that type,
     *     the rest are taken from the player's UNKNOWN amount.
     *<LI> If we are losing from type UNKNOWN,
     *     first convert player's known resources to unknown resources
     *     (individual amount information will be lost),
     *     then remove mes's unknown resources from player.
     *</ul>
     *<P>
     * To avoid code duplication, also called from
     * {@link PlayerClient#handlePLAYERELEMENT(PlayerElement)}
     * and {@link soc.robot.RobotBrain#run()}.
     *
     * @param mes    Message with amount and action (SET/GAIN/LOSE)
     * @param pl     Player to update
     * @param rtype  Type of resource, like {@link ResourceConstants#CLAY}
     */
    public static void handlePLAYERELEMENT_numRsrc
        (PlayerElement mes, final Player pl, int rtype)
    {
        final int amount = mes.getValue();

        switch (mes.getAction())
        {
        case PlayerElement.SET:
            pl.getResources().setAmount(amount, rtype);

            break;

        case PlayerElement.GAIN:
            pl.getResources().add(amount, rtype);

            break;

        case PlayerElement.LOSE:

            if (rtype != ResourceConstants.UNKNOWN)
            {
                int playerAmt = pl.getResources().getAmount(rtype); 
                if (playerAmt >= amount)
                {
                    pl.getResources().subtract(amount, rtype);
                }
                else
                {
                    pl.getResources().subtract(amount - playerAmt, ResourceConstants.UNKNOWN);
                    pl.getResources().setAmount(0, rtype);
                }
            }
            else
            {
                ResourceSet rs = pl.getResources();

                /**
                 * first convert player's known resources to unknown resources,
                 * then remove mes's unknown resources from player
                 */
                rs.convertToUnknown();
                pl.getResources().subtract(mes.getValue(), ResourceConstants.UNKNOWN);
            }

            break;
        }
    }

    /**
     * handle "resource count" message
     * @param mes  the message
     */
    protected void handleRESOURCECOUNT(ResourceCount mes)
    {
        final Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            final Player pl = ga.getPlayer(mes.getPlayerNumber());

            if (mes.getCount() != pl.getResources().getTotal())
            {
                ResourceSet rsrcs = pl.getResources();

                if (D.ebugOn)
                {
                    //pi.print(">>> RESOURCE COUNT ERROR: "+mes.getCount()+ " != "+rsrcs.getTotal());
                }

                //
                //  fix it
                //
                if (!pl.getName().equals(nickname))
                {
                    rsrcs.clear();
                    rsrcs.setAmount(mes.getCount(), ResourceConstants.UNKNOWN);
                }
            }
        }
    }

    /**
     * handle the "dice result" message
     * @param mes  the message
     */
    protected void handleDICERESULT(DiceResult mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setCurrentDice(mes.getResult());
        }
    }

    /**
     * handle the "put piece" message
     * @param mes  the message
     */
    protected void handlePUTPIECE(PutPiece mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            Player pl = ga.getPlayer(mes.getPlayerNumber());

            switch (mes.getPieceType())
            {
            case PlayingPiece.ROAD:

                Road rd = new Road(pl, mes.getCoordinates(), null);
                ga.putPiece(rd);

                break;

            case PlayingPiece.SETTLEMENT:

                Settlement se = new Settlement(pl, mes.getCoordinates(), null);
                ga.putPiece(se);

                break;

            case PlayingPiece.CITY:

                City ci = new City(pl, mes.getCoordinates(), null);
                ga.putPiece(ci);

                break;
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
    *      Our client can ignore this case, because the server also sends a text
    *      message that the human player is capable of reading and acting on.
    *
    * @param mes  the message
    */
    protected void handleCANCELBUILDREQUEST(CancelBuildRequest mes)
    {
        Game ga = (Game) games.get(mes.getGame());
        if (ga == null)
            return;

        int sta = ga.getGameState();
        if ((sta != Game.START1B) && (sta != Game.START2B))
        {
            // The human player gets a text message from the server informing
            // about the bad piece placement.  So, we can ignore this message type.
            // The robot player will override this method and react.
            return;
        }
        if (mes.getPieceType() != PlayingPiece.SETTLEMENT)
            return;

        Player pl = ga.getPlayer(ga.getCurrentPlayerNumber());
        Settlement pp = new Settlement(pl, pl.getLastSettlementCoord(), null);
        ga.undoPutInitSettlement(pp);
    }

    /**
     * handle the "robber moved" message
     * @param mes  the message
     */
    protected void handleMOVEROBBER(MoveRobber mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            /**
             * Note: Don't call ga.moveRobber() because that will call the
             * functions to do the stealing.  We just want to say where
             * the robber moved without seeing if something was stolen.
             */
            ga.getBoard().setRobberHex(mes.getCoordinates());
        }
    }

    /**
     * handle the "discard request" message
     * @param mes  the message
     */
    protected void handleDISCARDREQUEST(DiscardRequest mes) {}

    /**
     * handle the "choose player request" message
     * @param mes  the message
     */
    protected void handleCHOOSEPLAYERREQUEST(ChoosePlayerRequest mes)
    {
        boolean[] ch = mes.getChoices();
        int[] choices = new int[ch.length];  // == Game.maxPlayers
        int count = 0;

        for (int i = 0; i < ch.length; i++)
        {
            if (ch[i])
            {
                choices[count] = i;
                count++;
            }
        }
    }

    /**
     * handle the "make offer" message
     * @param mes  the message
     */
    protected void handleMAKEOFFER(MakeOffer mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            TradeOffer offer = mes.getOffer();
            ga.getPlayer(offer.getFrom()).setCurrentOffer(offer);
        }
    }

    /**
     * handle the "clear offer" message
     * @param mes  the message
     */
    protected void handleCLEAROFFER(ClearOffer mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            final int pn = mes.getPlayerNumber();
            if (pn != -1)
            {
                ga.getPlayer(pn).setCurrentOffer(null);
            } else {
                for (int i = 0; i < ga.maxPlayers; ++i)
                    ga.getPlayer(i).setCurrentOffer(null);
            }
        }
    }

    /**
     * handle the "reject offer" message
     * @param mes  the message
     */
    protected void handleREJECTOFFER(RejectOffer mes) {}

    /**
     * handle the "clear trade message" message
     * @param mes  the message
     */
    protected void handleCLEARTRADEMSG(ClearTradeMsg mes) {}

    /**
     * handle the "number of development cards" message
     * @param mes  the message
     */
    protected void handleDEVCARDCOUNT(DevCardCount mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setNumDevCards(mes.getNumDevCards());
        }
    }

    /**
     * handle the "development card action" message
     * @param mes  the message
     */
    protected void handleDEVCARD(DevCard mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            Player player = ga.getPlayer(mes.getPlayerNumber());

            switch (mes.getAction())
            {
            case DevCard.DRAW:
                player.getDevCards().add(1, DevCardSet.NEW, mes.getCardType());

                break;

            case DevCard.PLAY:
                player.getDevCards().subtract(1, DevCardSet.OLD, mes.getCardType());

                break;

            case DevCard.ADDOLD:
                player.getDevCards().add(1, DevCardSet.OLD, mes.getCardType());

                break;

            case DevCard.ADDNEW:
                player.getDevCards().add(1, DevCardSet.NEW, mes.getCardType());

                break;
            }
        }
    }

    /**
     * handle the "set played dev card flag" message
     * @param mes  the message
     */
    protected void handleSETPLAYEDDEVCARD(SetPlayedDevCard mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            Player player = ga.getPlayer(mes.getPlayerNumber());
            player.setPlayedDevCard(mes.hasPlayedDevCard());
        }
    }

    /**
     * handle the "list of potential settlements" message
     * @param mes  the message
     */
    protected void handlePOTENTIALSETTLEMENTS(PotentialSettlements mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            Player player = ga.getPlayer(mes.getPlayerNumber());
            player.setPotentialSettlements(mes.getPotentialSettlements());
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
     * handle the "reject connection" message
     * @param mes  the message
     */
    protected void handleREJECTCONNECTION(RejectConnection mes)
    {
        rejected = true;
        System.err.println("Rejected by server: " + mes.getText());
        disconnect();
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
     * handle the "set seat lock" message
     * @param mes  the message
     */
    protected void handleSETSEATLOCK(SetSeatLock mes)
    {
        Game ga = (Game) games.get(mes.getGame());

        if (ga != null)
        {
            if (mes.getLockState() == true)
            {
                ga.lockSeat(mes.getPlayerNumber());
            }
            else
            {
                ga.unlockSeat(mes.getPlayerNumber());
            }
        }
    }

    /**
     * handle board reset
     * (new game with same players, same game name, new layout).
     * Create new Game object, destroy old one.
     * For human players, the reset message will be followed
     * with others which will fill in the game state.
     * For robots, they must discard game state and ask to re-join.
     *
     * @param mes  the message
     *
     * @see soc.server.SOCServer#resetBoardAndNotify(String, int)
     * @see soc.game.Game#resetAsCopy()
     */
    protected void handleRESETBOARDAUTH(ResetBoardAuth mes)
    {
        String gname = mes.getGame();
        Game ga = (Game) games.get(gname);
        if (ga == null)
            return;  // Not one of our games

        Game greset = ga.resetAsCopy();
        greset.isLocal = ga.isLocal;
        games.put(gname, greset);
        ga.destroyGame();
    }

    /**
     * send a text message to a channel
     *
     * @param ch   the name of the channel
     * @param mes  the message
     */
    public void chSend(String ch, String mes)
    {
        put(TextMsg.toCmd(ch, nickname, mes));
    }

    /**
     * the user leaves the given channel
     *
     * @param ch  the name of the channel
     */
    public void leaveChannel(String ch)
    {
        channels.remove(ch);
        put(Leave.toCmd(nickname, host, ch));
    }

    /**
     * disconnect from the net, and from any local practice server
     */
    protected void disconnect()
    {
        connected = false;

        // reader will die once 'connected' is false, and socket is closed

        if (sLocal != null)
            sLocal.disconnect();

        try
        {
            s.close();
        }
        catch (Exception e)
        {
            ex = e;
        }
    }

    /**
     * request to buy a development card
     *
     * @param ga     the game
     */
    public void buyDevCard(Game ga)
    {
        put(BuyCardRequest.toCmd(ga.getName()));
    }

    /**
     * request to build something
     *
     * @param ga     the game
     * @param piece  the type of piece, from {@link soc.game.PlayingPiece} constants,
     *               or -1 to request the Special Building Phase.
     */
    public void buildRequest(Game ga, int piece)
    {
        put(BuildRequest.toCmd(ga.getName(), piece));
    }

    /**
     * request to cancel building something
     *
     * @param ga     the game
     * @param piece  the type of piece from PlayingPiece
     */
    public void cancelBuildRequest(Game ga, int piece)
    {
        put(CancelBuildRequest.toCmd(ga.getName(), piece));
    }

    /**
     * put a piece on the board
     *
     * @param ga  the game where the action is taking place
     * @param pp  the piece being placed
     */
    public void putPiece(Game ga, PlayingPiece pp)
    {
        /**
         * send the command
         */
		String cmd = PutPiece.toCmd(ga.getName(), pp.getPlayer().getPlayerNumber(), pp.getType(), pp.getCoordinates());
        System.out.printf("Message Sent: %s \n", cmd);
        put(cmd);
    }

    /**
     * the player wants to move the robber
     *
     * @param ga  the game
     * @param pl  the player
     * @param coord  where the player wants the robber
     */
    public void moveRobber(Game ga, Player pl, int coord)
    {
        put(MoveRobber.toCmd(ga.getName(), pl.getPlayerNumber(), coord));
    }

    /**
     * send a text message to the people in the game
     *
     * @param ga   the game
     * @param me   the message
     */
    public void sendText(Game ga, String me)
    {
        put(GameTextMsg.toCmd(ga.getName(), nickname, me));
    }

    /**
     * the user leaves the given game
     *
     * @param ga   the game
     */
    public void leaveGame(Game ga)
    {
        games.remove(ga.getName());
        put(LeaveGame.toCmd(nickname, host, ga.getName()));
    }

    /**
     * the user sits down to play
     *
     * @param ga   the game
     * @param pn   the number of the seat where the user wants to sit
     */
    public void sitDown(Game ga, int pn)
    {
        put(SitDown.toCmd(ga.getName(), "dummy", pn, false));
    }

    /**
     * the user is starting the game
     *
     * @param ga  the game
     */
    public void startGame(Game ga)
    {
        put(StartGame.toCmd(ga.getName()));
    }

    /**
     * the user rolls the dice
     *
     * @param ga  the game
     */
    public void rollDice(Game ga)
    {
        put(RollDice.toCmd(ga.getName()));
    }

    /**
     * the user is done with the turn
     *
     * @param ga  the game
     */
    public void endTurn(Game ga)
    {
        put(EndTurn.toCmd(ga.getName()));
    }

    /**
     * the user wants to discard
     *
     * @param ga  the game
     */
    public void discard(Game ga, ResourceSet rs)
    {
        put(Discard.toCmd(ga.getName(), rs));
    }

    /**
     * the user chose a player to steal from
     *
     * @param ga  the game
     * @param pn  the player id
     */
    public void choosePlayer(Game ga, int pn)
    {
        put(ChoosePlayer.toCmd(ga.getName(), pn));
    }

    /**
     * the user is rejecting the current offers
     *
     * @param ga  the game
     */
    public void rejectOffer(Game ga)
    {
        put(RejectOffer.toCmd(ga.getName(), ga.getPlayer(nickname).getPlayerNumber()));
    }

    /**
     * the user is accepting an offer
     *
     * @param ga  the game
     * @param from the number of the player that is making the offer
     */
    public void acceptOffer(Game ga, int from)
    {
        put(AcceptOffer.toCmd(ga.getName(), ga.getPlayer(nickname).getPlayerNumber(), from));
    }

    /**
     * the user is clearing an offer
     *
     * @param ga  the game
     */
    public void clearOffer(Game ga)
    {
        put(ClearOffer.toCmd(ga.getName(), ga.getPlayer(nickname).getPlayerNumber()));
    }

    /**
     * the user wants to trade with the bank
     *
     * @param ga    the game
     * @param give  what is being offered
     * @param get   what the player wants
     */
    public void bankTrade(Game ga, ResourceSet give, ResourceSet get)
    {
        put(BankTrade.toCmd(ga.getName(), give, get));
    }

    /**
     * the user is making an offer to trade
     *
     * @param ga    the game
     * @param offer the trade offer
     */
    public void offerTrade(Game ga, TradeOffer offer)
    {
        put(MakeOffer.toCmd(ga.getName(), offer));
    }

    /**
     * the user wants to play a development card
     *
     * @param ga  the game
     * @param dc  the type of development card
     */
    public void playDevCard(Game ga, int dc)
    {
        put(PlayDevCardRequest.toCmd(ga.getName(), dc));
    }

    /**
     * the user picked 2 resources to discover
     *
     * @param ga    the game
     * @param rscs  the resources
     */
    public void discoveryPick(Game ga, ResourceSet rscs)
    {
        put(DiscoveryPick.toCmd(ga.getName(), rscs));
    }

    /**
     * the user picked a resource to monopolize
     *
     * @param ga   the game
     * @param res  the resource
     */
    public void monopolyPick(Game ga, int res)
    {
        put(MonopolyPick.toCmd(ga.getName(), res));
    }

    /**
     * the user is changing the face image
     *
     * @param ga  the game
     * @param id  the image id
     */
    public void changeFace(Game ga, int id)
    {
        put(ChangeFace.toCmd(ga.getName(), ga.getPlayer(nickname).getPlayerNumber(), id));
    }

    /**
     * the user is locking a seat
     *
     * @param ga  the game
     * @param pn  the seat number
     */
    public void lockSeat(Game ga, int pn)
    {
        put(SetSeatLock.toCmd(ga.getName(), pn, true));
    }

    /**
     * the user is unlocking a seat
     *
     * @param ga  the game
     * @param pn  the seat number
     */
    public void unlockSeat(Game ga, int pn)
    {
        put(SetSeatLock.toCmd(ga.getName(), pn, false));
    }

    /** destroy the applet */
    public void destroy()
    {
        LeaveAll leaveAllMes = new LeaveAll();
        put(leaveAllMes.toCmd());
        disconnect();
    }

    public boolean isSmartSettlersAgent()
    {
        return false;
    }
    
    /**
     * for stand-alones
     */
    public static void main(String[] args)
    {
        DisplaylessPlayerClient ex1 = new DisplaylessPlayerClient(args[0], Integer.parseInt(args[1]), true);
        new Thread(ex1).start();
        Thread.yield();
    }
}
