/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.server;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import soc.disableDebug.D;

import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCDevCardSet;
import soc.game.SOCGame;
import soc.game.SOCMoveRobberResult;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.game.SOCTradeOffer;

import soc.message.SOCAcceptOffer;
import soc.message.SOCAdminPing;
import soc.message.SOCAdminReset;
import soc.message.SOCBCastTextMsg;
import soc.message.SOCBankTrade;
import soc.message.SOCBoardLayout;
import soc.message.SOCBuildRequest;
import soc.message.SOCBuyCardRequest;
import soc.message.SOCCancelBuildRequest;
import soc.message.SOCChangeFace;
import soc.message.SOCChannels;
import soc.message.SOCChoosePlayer;
import soc.message.SOCChoosePlayerRequest;
import soc.message.SOCClearOffer;
import soc.message.SOCClearTradeMsg;
import soc.message.SOCCreateAccount;
import soc.message.SOCDeleteChannel;
import soc.message.SOCDeleteGame;
import soc.message.SOCDevCard;
import soc.message.SOCDevCardCount;
import soc.message.SOCDiceResult;
import soc.message.SOCDiscard;
import soc.message.SOCDiscardRequest;
import soc.message.SOCDiscoveryPick;
import soc.message.SOCEndTurn;
import soc.message.SOCFirstPlayer;
import soc.message.SOCGameMembers;
import soc.message.SOCGameState;
import soc.message.SOCGameTextMsg;
import soc.message.SOCGames;
import soc.message.SOCImARobot;
import soc.message.SOCJoin;
import soc.message.SOCJoinAuth;
import soc.message.SOCJoinGame;
import soc.message.SOCJoinGameAuth;
import soc.message.SOCJoinGameRequest;
import soc.message.SOCLargestArmy;
import soc.message.SOCLastSettlement;
import soc.message.SOCLeave;
import soc.message.SOCLeaveGame;
import soc.message.SOCLongestRoad;
import soc.message.SOCMakeOffer;
import soc.message.SOCMembers;
import soc.message.SOCMessage;
import soc.message.SOCMonopolyPick;
import soc.message.SOCMoveRobber;
import soc.message.SOCNewChannel;
import soc.message.SOCNewGame;
import soc.message.SOCPlayDevCardRequest;
import soc.message.SOCPlayerElement;
import soc.message.SOCPotentialSettlements;
import soc.message.SOCPutPiece;
import soc.message.SOCRejectConnection;
import soc.message.SOCRejectOffer;
import soc.message.SOCResourceCount;
import soc.message.SOCRobotDismiss;
import soc.message.SOCRollDice;
import soc.message.SOCSetPlayedDevCard;
import soc.message.SOCSetSeatLock;
import soc.message.SOCSetTurn;
import soc.message.SOCSitDown;
import soc.message.SOCStartGame;
import soc.message.SOCStatusMessage;
import soc.message.SOCTextMsg;
import soc.message.SOCTurn;
import soc.message.SOCUpdateRobotParams;

import soc.server.database.SOCDBHelper;

import soc.server.genericServer.Connection;
import soc.server.genericServer.Server;

import soc.util.IntPair;
import soc.util.SOCRobotParameters;
import soc.util.Version;

import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import soc.message.SOCLeaveAll;
import soc.robot.SOCRobotBrain;
import soc.robot.SSRobotClient;

/**
 * A server for Settlers of Catan
 *
 * @author  Robert S. Thomas
 *
 * Note: This is an attempt at being more modular. 5/13/99 RST
 * Note: Hopfully fixed all of the deadlock problems. 12/27/01 RST
 */
public class SOCServer extends Server
{
    /**
     * Name used when sending messages from the server.
     */
    public static final String SERVERNAME = "Server";

    /**
     * So we can get random numbers.
     */
    private Random rand = new Random();

    /**
     * The TCP port we listen on.
     */
    public int port;

    /**
     * Maximum number of connections allowed
     */
    protected int maxConnections;

    /**
     * A list of robots connected to this server
     */
    protected Vector robots = new Vector();

    /**
     * list of chat channels
     */
    protected SOCChannelList channelList = new SOCChannelList();

    /**
     * list of soc games
     */
    protected SOCGameList gameList = new SOCGameList();

    /**
     * table of requests for robots to join games
     */
    protected Hashtable robotJoinRequests = new Hashtable();

    /**
     * table of requestst for robots to leave games
     */
    protected Hashtable robotDismissRequests = new Hashtable();

    /**
     * table of game data files
     */
    protected Hashtable gameDataFiles = new Hashtable();

    /**
     * the current game event record
     */

    //protected SOCGameEventRecord currentGameEventRecord;

    /**
     * the time that this server was started
     */
    protected long startTime;

    /**
     * the total number of games that have been started
     */
    protected int numberOfGamesStarted;

    /**
     * the total number of games finished
     */
    protected int numberOfGamesFinished;

    /**
     * total number of users
     */
    protected int numberOfUsers;

    /**
     * server robot pinger
     */
    SOCServerRobotPinger serverRobotPinger;

    /**
     * game timeout checker
     */
    SOCGameTimeoutChecker gameTimeoutChecker;
    String databaseUserName;
    String databasePassword;
    
    SSRobotClient loggerClient;

    /**
     * Create a Settlers of Catan server listening on port p.
     *
     * @param p    the port that the server listens on
     * @param mc   the maximum number of connections allowed
     * @param databaseUserName  the user name for accessing the database
     * @param databasePassword  the password for the user
     */
    public SOCServer(int p, int mc, String databaseUserName, String databasePassword)
    {
        super(p);
        maxConnections = mc;

        System.err.println("Java Settlers Server " + Version.version() +
                           ", " + Version.copyright());
        System.err.println("Network layer based on code by Cristian Bogdan.");

        try
        {
            SOCDBHelper.initialize(databaseUserName, databasePassword);
            System.err.println("User database initialized.");
        }
        catch (SQLException x) // just a warning
        {
            System.err.println("No user database available: " +
                               x.getMessage());
            System.err.println("Users will not be authenticated.");
        }

        port = p;
        startTime = System.currentTimeMillis();
        numberOfGamesStarted = 0;
        numberOfGamesFinished = 0;
        numberOfUsers = 0;
        serverRobotPinger = new SOCServerRobotPinger(robots);
        serverRobotPinger.start();
        gameTimeoutChecker = new SOCGameTimeoutChecker(this);
        gameTimeoutChecker.start();
        this.databaseUserName = databaseUserName;
        this.databasePassword = databasePassword;
    }

    /**
     * Adds a connection to a chat channel.
     *
     * WARNING: MUST HAVE THE channelList.takeMonitorForChannel(ch)
     * before calling this method
     *
     * @param c    the Connection to be added
     * @param ch   the name of the channel
     *
     */
    public void connectToChannel(Connection c, String ch)
    {
        if (c != null)
        {
            if (channelList.isChannel(ch))
            {
                if (!channelList.isMember(c, ch))
                {
                    c.put(SOCMembers.toCmd(ch, channelList.getMembers(ch)));
                    D.ebugPrintln("*** " + c.data + " joined the channel " + ch);
                    channelList.addMember(c, ch);
                }
            }
        }
    }

    /**
     * the connection c leaves the channel ch
     *
     * WARNING: MUST HAVE THE channelList.takeMonitorForChannel(ch) before
     * calling this method
     *
     * @param c  the connection
     * @param ch the channel
     * @param channelListLock  true if we have the channelList monitor
     * @return true if we destroyed the channel
     */
    public boolean leaveChannel(Connection c, String ch, boolean channelListLock)
    {
        D.ebugPrintln("leaveChannel: " + c.data + " " + ch + " " + channelListLock);

        boolean result = false;

        if (c != null)
        {
            if (channelList.isMember(c, ch))
            {
                channelList.removeMember(c, ch);

                SOCLeave leaveMessage = new SOCLeave((String) c.data, c.host(), ch);
                messageToChannelWithMon(ch, leaveMessage);
                D.ebugPrintln("*** " + (String) c.data + " left the channel " + ch);
            }

            if (channelList.isChannelEmpty(ch))
            {
                if (channelListLock)
                {
                    channelList.deleteChannel(ch);
                }
                else
                {
                    channelList.takeMonitor();

                    try
                    {
                        channelList.deleteChannel(ch);
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintln("Exception in leaveChannel - " + e);
                    }

                    channelList.releaseMonitor();
                }

                result = true;
            }
        }

        return result;
    }

    /**
     * Adds a connection to a game.
     *
     * @param c    the Connection to be added
     * @param ga   the name of the game
     *
     * @return     true if c was not a member of ch before
     */
    public boolean connectToGame(Connection c, String ga)
    {
        boolean result = false;

        if (c != null)
        {
            boolean gameExists = false;
            gameList.takeMonitor();

            try
            {
                gameExists = gameList.isGame(ga);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Excepetion in connectToGame - " + e);
            }

            gameList.releaseMonitor();

            if (gameExists)
            {
                gameList.takeMonitorForGame(ga);

                try
                {
                    if (gameList.isMember(c, ga))
                    {
                        result = false;
                    }
                    else
                    {
                        gameList.addMember(c, ga);
                        result = true;
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Excepetion in connectToGame (isMember) - " + e);
                }

                gameList.releaseMonitorForGame(ga);
            }
            else
            {
                /**
                 * the game did not exist, create it
                 */
                gameList.takeMonitor();

                boolean monitorReleased = false;

                try
                {
                    gameList.createGame(ga);
                    gameList.addMember(c, ga);

                    // must release monitor before we broadcast
                    gameList.releaseMonitor();
                    monitorReleased = true;
                    broadcast(SOCNewGame.toCmd(ga));
                    result = true;
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Excepetion in connectToGame - " + e);
                }

                if (!monitorReleased)
                {
                    gameList.releaseMonitor();
                }
            }

            return result;
        }
        else
        {
            return false;
        }
    }

    /**
     * the connection c leaves the game gm
     *
     * WARNING: MUST HAVE THE gameList.takeMonitorForGame(gm) before
     * calling this method
     *
     * @param c  the connection
     * @param gm the game
     * @param gameListLock  true if we have the gameList.takeMonitor() lock
     * @return true if the game was destroyed
     */
    public boolean leaveGame(Connection c, String gm, boolean gameListLock)
    {
        boolean gameDestroyed = false;

        if (c != null)
        {
            gameList.removeMember(c, gm);

            boolean isPlayer = false;
            int playerNumber = 0;
            SOCGame cg = gameList.getGameData(gm);

            boolean gameHasHumanPlayer = false;
            //boolean gameHasObserver = false;
            boolean gameHasObserver = true;

            if (cg != null)
            {
                for (playerNumber = 0; playerNumber < SOCGame.MAXPLAYERS;
                        playerNumber++)
                {
                    SOCPlayer player = cg.getPlayer(playerNumber);

                    if ((player != null) && (player.getName() != null))
                    {
                        if (player.getName().equals((String) c.data))
                        {
                            isPlayer = true;
                            cg.removePlayer((String) c.data);

                            //broadcastGameStats(cg);
                            break;
                        }
                    }
                }

                SOCLeaveGame leaveMessage = new SOCLeaveGame((String) c.data, c.host(), gm);
                messageToGameWithMon(gm, leaveMessage);
                //recordGameEvent(leaveMessage, gm, leaveMessage.toCmd());

                D.ebugPrintln("*** " + (String) c.data + " left the game " + gm);
                messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, (String) c.data + " left the game"));

                /**
                 * check if there is at least one person playing the game
                 */
                for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++)
                {
                    if (cg != null)
                    {
                        SOCPlayer player = cg.getPlayer(pn);

                        if ((player != null) && (player.getName() != null) && (!cg.isSeatVacant(pn)) && (!player.isRobot()))
                        {
                            gameHasHumanPlayer = true;

                            break;
                        }
                    }
                }

                //D.ebugPrintln("*** gameHasHumanPlayer = "+gameHasHumanPlayer+" for "+gm);

                /**
                 * check if there is at least one person watching the game
                 */
                if ((cg != null) && !gameHasHumanPlayer && !gameList.isGameEmpty(gm))
                {
                    Enumeration membersEnum = gameList.getMembers(gm).elements();

                    while (membersEnum.hasMoreElements())
                    {
                        Connection member = (Connection) membersEnum.nextElement();

                        //D.ebugPrintln("*** "+member.data+" is a member of "+gm);
                        boolean nameMatch = false;

                        for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++)
                        {
                            SOCPlayer player = cg.getPlayer(pn);

                            if ((player != null) && (player.getName() != null) && (player.getName().equals((String) member.data)))
                            {
                                nameMatch = true;

                                break;
                            }
                        }

                        if (!nameMatch)
                        {
                            gameHasObserver = true;

                            break;
                        }
                    }
                }

                //D.ebugPrintln("*** gameHasObserver = "+gameHasObserver+" for "+gm);

                /**
                 * if the leaving member was playing the game, and
                 * it wasn't a robot, and the game isn't over, then...
                 */
                if (isPlayer && (gameHasHumanPlayer || gameHasObserver) && (cg != null) && (!cg.getPlayer(playerNumber).isRobot()) && (cg.getGameState() != SOCGame.OVER) && !(cg.getGameState() < SOCGame.START1A))
                {
                    /**
                     * get a robot to replace this player
                     */
                    messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, "Fetching a robot player..."));

                    if (robots.isEmpty())
                    {
                        messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, "Sorry, no robots on this server."));
                    }
                    else
                    {
                        /**
                         * request a robot that isn't already playing this game or
                         * is not already requested to play in this game
                         */
                        boolean nameMatch = false;
                        Connection robotConn = null;

                        ///
                        /// shuffle the indexes to distribute load
                        ///
                        int[] robotIndexes = new int[robots.size()];

                        for (int i = 0; i < robots.size(); i++)
                        {
                            robotIndexes[i] = i;
                        }

                        for (int j = 0; j < 3; j++)
                        {
                            for (int i = 0; i < robotIndexes.length; i++) //
                            {
                                // Swap a random card below the ith robot with the ith robot
                                int idx = Math.abs(rand.nextInt() % (robotIndexes.length - i));
                                int tmp = robotIndexes[idx];
                                robotIndexes[idx] = robotIndexes[i];
                                robotIndexes[i] = tmp;
                            }
                        }

                        if (D.ebugOn)
                        {
                            for (int i = 0; i < robots.size(); i++)
                            {
                                D.ebugPrintln("^^^ robotIndexes[" + i + "]=" + robotIndexes[i]);
                            }
                        }

                        Vector requests = (Vector) robotJoinRequests.get(gm);

                        for (int idx = 0; idx < robots.size(); idx++)
                        {
                            robotConn = (Connection) robots.get(robotIndexes[idx]);
                            nameMatch = false;

                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                if (cg != null)
                                {
                                    SOCPlayer pl = cg.getPlayer(i);

                                    if (pl != null)
                                    {
                                        String pname = pl.getName();

                                        D.ebugPrintln("CHECKING " + (String) robotConn.data + " == " + pname);

                                        if ((pname != null) && (pname.equals((String) robotConn.data)))
                                        {
                                            nameMatch = true;

                                            break;
                                        }
                                    }
                                }
                            }

                            if ((!nameMatch) && (requests != null))
                            {
                                Enumeration requestsEnum = requests.elements();

                                while (requestsEnum.hasMoreElements())
                                {
                                    Connection tempCon = (Connection) requestsEnum.nextElement();

                                    D.ebugPrintln("CHECKING " + robotConn + " == " + tempCon);

                                    if (tempCon == robotConn)
                                    {
                                        nameMatch = true;
                                    }

                                    break;
                                }
                            }

                            if (!nameMatch)
                            {
                                break;
                            }
                        }

                        if (!nameMatch && (cg != null))
                        {
                            /**
                             * make the request
                             */
                            D.ebugPrintln("@@@ JOIN GAME REQUEST for " + (String) robotConn.data);

                            if (robotConn.put(SOCJoinGameRequest.toCmd(gm, playerNumber)))
                            {
                                /**
                                 * record the request
                                 */
                                if (requests == null)
                                {
                                    requests = new Vector();
                                    requests.addElement(robotConn);
                                    robotJoinRequests.put(gm, requests);
                                }
                                else
                                {
                                    requests.addElement(robotConn);
                                }
                            }
                            else
                            {
                                // !!! won't ever happen now because put is asynchronous
                                messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, "*** Error on robot request! ***"));
                            }
                        }
                        else
                        {
                            messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, "*** Can't find a robot! ***"));
                        }
                    }
                }
            }

            /**
             * if the game has no players, or if they're all
             * robots, then end the game and write the data
             * to disk.
             */
            boolean emptyGame = false;
            emptyGame = gameList.isGameEmpty(gm);

            if (emptyGame || (!gameHasHumanPlayer && !gameHasObserver))
            {
                if (gameListLock)
                {
                    destroyGame(gm);
                }
                else
                {
                    gameList.takeMonitor();

                    try
                    {
                        destroyGame(gm);
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintln("Exception in leaveGame (destroyGame) - " + e);
                    }

                    gameList.releaseMonitor();
                }

                gameDestroyed = true;
            }
        }

        //D.ebugPrintln("*** gameDestroyed = "+gameDestroyed+" for "+gm);
        return gameDestroyed;
    }

    /**
     * destroy the game
     *
     * WARNING: MUST HAVE THE gameList.takeMonitor() before
     * calling this method
     *
     * @param gm  the name of the game
     */
    public void destroyGame(String gm)
    {
        //D.ebugPrintln("***** destroyGame("+gm+")");
        SOCGame cg = null;

        //recordGameEvent(mes, mes.getGame(), mes.toCmd()); //!!!
        cg = gameList.getGameData(gm);

        if (cg != null)
        {
            if (cg.getGameState() == SOCGame.OVER)
            {
                numberOfGamesFinished++;
            }

            ///
            /// write out game data
            ///

            /*
               currentGameEventRecord.setSnapshot(cg);
               saveCurrentGameEventRecord(gm);
               SOCGameRecord gr = (SOCGameRecord)gameRecords.get(gm);
               writeGameRecord(gm, gr);
             */

//            //storeGameScores(cg);
//            ///
//            /// tell all robots to leave
//            ///
//            Vector members = null;
//            members = gameList.getMembers(gm);
//
//            if (members != null)
//            {
//                Enumeration conEnum = members.elements();
//
//                while (conEnum.hasMoreElements())
//                {
//                    Connection con = (Connection) conEnum.nextElement();
//                    con.put(SOCRobotDismiss.toCmd(gm));
//                }
//            }

            gameList.deleteGame(gm);
        }
    }

    /**
     * the connection c leaves all channels it was in
     *
     * @param c  the connection
     * @return   the channels it was in
     */
    public Vector leaveAllChannels(Connection c)
    {
        if (c != null)
        {
            Vector ret = new Vector();
            Vector destroyed = new Vector();

            channelList.takeMonitor();

            try
            {
                for (Enumeration k = channelList.getChannels();
                        k.hasMoreElements();)
                {
                    String ch = (String) k.nextElement();

                    if (channelList.isMember(c, ch))
                    {
                        boolean thisChannelDestroyed = false;
                        channelList.takeMonitorForChannel(ch);

                        try
                        {
                            thisChannelDestroyed = leaveChannel(c, ch, true);
                        }
                        catch (Exception e)
                        {
                            D.ebugPrintln("Exception in leaveAllChannels (leaveChannel) - " + e);
                        }

                        channelList.releaseMonitorForChannel(ch);

                        if (thisChannelDestroyed)
                        {
                            destroyed.addElement(ch);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in leaveAllChannels - " + e);
            }

            channelList.releaseMonitor();

            /**
             * let everyone know about the destroyed channels
             */
            for (Enumeration de = destroyed.elements(); de.hasMoreElements();)
            {
                String ga = (String) de.nextElement();
                broadcast(SOCDeleteChannel.toCmd(ga));
            }

            return ret;
        }
        else
        {
            return null;
        }
    }

    /**
     * the connection c leaves all games it was in
     *
     * @param c  the connection
     * @return   the games it was in
     */
    public Vector leaveAllGames(Connection c)
    {
        if (c != null)
        {
            Vector ret = new Vector();
            Vector destroyed = new Vector();

            gameList.takeMonitor();

            try
            {
                for (Enumeration k = gameList.getGames(); k.hasMoreElements();)
                {
                    String ga = (String) k.nextElement();
                    Vector v = (Vector) gameList.getMembers(ga);

                    if (v.contains(c))
                    {
                        boolean thisGameDestroyed = false;
                        gameList.takeMonitorForGame(ga);

                        try
                        {
                            thisGameDestroyed = leaveGame(c, ga, true);
                        }
                        catch (Exception e)
                        {
                            D.ebugPrintln("Exception in leaveAllGames (leaveGame) - " + e);
                        }

                        gameList.releaseMonitorForGame(ga);

                        if (thisGameDestroyed)
                        {
                            destroyed.addElement(ga);
                        }

                        ret.addElement(ga);
                    }
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in leaveAllGames - " + e);
            }

            gameList.releaseMonitor();

            /**
             * let everyone know about the destroyed games
             */
            for (Enumeration de = destroyed.elements(); de.hasMoreElements();)
            {
                String ga = (String) de.nextElement();
                D.ebugPrintln("** Broadcasting SOCDeleteGame " + ga);
                broadcast(SOCDeleteGame.toCmd(ga));
            }

            return ret;
        }
        else
        {
            return null;
        }
    }

    /**
     * Send a message to the given channel
     *
     * @param ch  the name of the channel
     * @param mes the message to send
     */
    public void messageToChannel(String ch, SOCMessage mes)
    {
        channelList.takeMonitorForChannel(ch);

        try
        {
            Vector v = channelList.getMembers(ch);

            if (v != null)
            {
                Enumeration enum1 = v.elements();

                while (enum1.hasMoreElements())
                {
                    Connection c = (Connection) enum1.nextElement();

                    if (c != null)
                    {
                        c.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in messageToChannel - " + e);
        }

        channelList.releaseMonitorForChannel(ch);
    }

    /**
     * Send a message to the given channel
     *
     * WARNING: MUST HAVE THE gameList.takeMonitorForChannel(ch) before
     * calling this method
     *
     * @param ch  the name of the channel
     * @param mes the message to send
     */
    public void messageToChannelWithMon(String ch, SOCMessage mes)
    {
        Vector v = channelList.getMembers(ch);

        if (v != null)
        {
            Enumeration enum1 = v.elements();

            while (enum1.hasMoreElements())
            {
                Connection c = (Connection) enum1.nextElement();

                if (c != null)
                {
                    c.put(mes.toCmd());
                }
            }
        }
    }

    /**
     * Send a message to a player and record it
     *
     * @param c   the player connection
     * @param mes the message to send
     */
    public void messageToPlayer(Connection c, SOCMessage mes)
    {
        if ((c != null) && (mes != null))
        {
            //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", c.data));
            c.put(mes.toCmd());
        }
    }

    /**
     * Send a message to the given game
     *
     * @param ga  the name of the game
     * @param mes the message to send
     */
    public void messageToGame(String ga, SOCMessage mes)
    {
        gameList.takeMonitorForGame(ga);

        try
        {
            Vector v = gameList.getMembers(ga);

            if (v != null)
            {
                //D.ebugPrintln("M2G - "+mes);
                Enumeration enum1 = v.elements();

                while (enum1.hasMoreElements())
                {
                    Connection c = (Connection) enum1.nextElement();

                    if (c != null)
                    {
                        //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", c.data));
                        c.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in messageToGame - " + e);
        }

        gameList.releaseMonitorForGame(ga);
    }

    /**
     * Send a message to the given game
     *
     * WARNING: MUST HAVE THE gameList.takeMonitorForGame(ga) before
     * calling this method
     *
     * @param ga  the name of the game
     * @param mes the message to send
     */
    public void messageToGameWithMon(String ga, SOCMessage mes)
    {
        Vector v = gameList.getMembers(ga);

        if (v != null)
        {
            //D.ebugPrintln("M2G - "+mes);
            Enumeration enum1 = v.elements();

            while (enum1.hasMoreElements())
            {
                Connection c = (Connection) enum1.nextElement();

                if (c != null)
                {
                    //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", c.data));
                    c.put(mes.toCmd());
                }
            }
        }
    }

    /**
     * Send a message to all the connections in a game
     * excluding some.
     *
     * @param gn  the name of the game
     * @param ex  the list of exceptions
     * @param mes the message
     */
    public void messageToGameExcept(String gn, Vector ex, SOCMessage mes)
    {
        gameList.takeMonitorForGame(gn);

        try
        {
            Vector v = gameList.getMembers(gn);

            if (v != null)
            {
                //D.ebugPrintln("M2GE - "+mes);
                Enumeration enum1 = v.elements();

                while (enum1.hasMoreElements())
                {
                    Connection con = (Connection) enum1.nextElement();

                    if ((con != null) && (!ex.contains(con)))
                    {
                        //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", con.data));
                        con.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in messageToGameExcept - " + e);
        }

        gameList.releaseMonitorForGame(gn);
    }

    /**
     * things to do when the connection c leaves
     *
     * @param c  the connection
     */
    @Override
    public void leaveConnection(Connection c)
    {
        if (c != null)
        {
            leaveAllChannels(c);
            leaveAllGames(c);

            /**
             * if it is a robot, remove it from the list
             */
            robots.removeElement(c);
        }
    }

    /**
     * Things to do when a new connection comes
     *
     * @param c  the new Connection
     */
    public void newConnection(Connection c)
    {
        if (c != null)
        {
            /**
             * see if we are under the connection limit
             */
            try
            {
                if (this.connectionCount() >= maxConnections)
                {
                    SOCRejectConnection rcCommand = new SOCRejectConnection("Too many connections, please try another server.");
                    c.put(rcCommand.toCmd());

                    return;
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("Caught exception in SOCServer.newConnection(Connection) - " + e);
                e.printStackTrace(System.out);

                return;
            }

            try
            {
                /**
                 * prevent someone from connecting twice from
                 * the same machine
                 */
                boolean hostMatch = false;
                Enumeration allConnections = this.getConnections();

                /*
                   while(allConnections.hasMoreElements()) {
                   Connection tempCon = (Connection)allConnections.nextElement();
                   if (!(c.host().equals("pippen")) && (tempCon.host().equals(c.host()))) {
                   hostMatch = true;
                   break;
                   }
                   }
                 */
                if (hostMatch)
                {
                    SOCRejectConnection rcCommand = new SOCRejectConnection("Can't connect to the server more than once from one machine.");
                    c.put(rcCommand.toCmd());
                }
                else
                {
                    Vector cl = new Vector();
                    channelList.takeMonitor();

                    try
                    {
                        Enumeration clEnum = channelList.getChannels();

                        while (clEnum.hasMoreElements())
                        {
                            cl.addElement(clEnum.nextElement());
                        }
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintln("Exception in newConnection (channelList) - " + e);
                    }

                    channelList.releaseMonitor();

                    c.put(SOCChannels.toCmd(cl));

                    Vector gl = new Vector();
                    gameList.takeMonitor();

                    try
                    {
                        Enumeration gaEnum = gameList.getGames();

                        while (gaEnum.hasMoreElements())
                        {
                            gl.addElement(gaEnum.nextElement());
                        }
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintln("Exception in newConnection (gameList) - " + e);
                    }

                    gameList.releaseMonitor();
                    c.put(SOCGames.toCmd(gl));

                    /*
                       gaEnum = gameList.getGames();
                       int scores[] = new int[SOCGame.MAXPLAYERS];
                       boolean robots[] = new boolean[SOCGame.MAXPLAYERS];
                       while (gaEnum.hasMoreElements()) {
                       String gameName = (String)gaEnum.nextElement();
                       SOCGame theGame = gameList.getGameData(gameName);
                       for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
                       SOCPlayer player = theGame.getPlayer(i);
                       if (player != null) {
                       if (theGame.isSeatVacant(i)) {
                       scores[i] = -1;
                       robots[i] = false;
                       } else {
                       scores[i] = player.getPublicVP();
                       robots[i] = player.isRobot();
                       }
                       } else {
                       scores[i] = 0;
                       }
                       }
                       c.put(SOCGameStats.toCmd(gameName, scores, robots));
                       }
                     */
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("Caught exception in SOCServer.newConnection(Connection) - " + e);
                e.printStackTrace(System.out);
            }
        }
    }

    /**
     * check if a name is ok
     * a name is ok if it hasn't been used yet
     *
     * @param n  the name
     * @return   true if the name is ok
     */
    private boolean checkNickname(String n)
    {
        if (n.equals(SERVERNAME))
        {
            return false;
        }

        Enumeration connsEnum = getConnections();

        while (connsEnum.hasMoreElements())
        {
            Connection con = (Connection) connsEnum.nextElement();

            if ((con != null) && (n.equals((String) con.data)))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Treat the incomming messages
     *
     * Note: When there is a choice, always use local information
     *       over information from the message.  For example, use
     *       the nickname from the connection to get the player
     *       information rather than the player information from
     *       the message.  This makes it harder to send false
     *       messages making players do things they didn't want
     *       to do.
     *
     * @param s    String containing the message
     * @param c    the Connection that sent the Message
     */
    public void processCommand(String s, Connection c)
    {
        try
        {
            SOCMessage mes = (SOCMessage) SOCMessage.toMsg(s);

            //D.ebugPrintln(c.data+" - "+mes);
            if (mes != null)
            {
                //recordGameEvent(mes, mes.getGame(), mes.toCmd());
                SOCGame ga;

                switch (mes.getType())
                {
                /**
                 * "join a channel" message
                 */
                case SOCMessage.JOIN:
                    handleJOIN(c, (SOCJoin) mes);

                    break;

                /**
                 * "leave a channel" message
                 */
                case SOCMessage.LEAVE:
                    handleLEAVE(c, (SOCLeave) mes);

                    break;

                /**
                 * "leave all channels" message
                 */
                case SOCMessage.LEAVEALL:
                    removeConnection(c);
                    removeConnectionCleanup(c);

                    break;

                /**
                 * text message
                 */
                case SOCMessage.TEXTMSG:

                    SOCTextMsg textMsgMes = (SOCTextMsg) mes;

                    if (c.data.equals("debug"))
                    {
                        if (textMsgMes.getText().startsWith("*KILLCHANNEL*"))
                        {
                            messageToChannel(textMsgMes.getChannel(), new SOCTextMsg(textMsgMes.getChannel(), SERVERNAME, "********** " + (String) c.data + " KILLED THE CHANNEL **********"));
                            channelList.takeMonitor();

                            try
                            {
                                channelList.deleteChannel(textMsgMes.getChannel());
                            }
                            catch (Exception e)
                            {
                                D.ebugPrintln("Exception in KILLCHANNEL - " + e);
                            }

                            channelList.releaseMonitor();
                            broadcast(SOCDeleteChannel.toCmd(textMsgMes.getChannel()));
                        }
                        else
                        {
                            /**
                             * Send the message to the members of the channel
                             */
                            messageToChannel(textMsgMes.getChannel(), mes);
                        }
                    }
                    else
                    {
                        /**
                         * Send the message to the members of the channel
                         */
                        messageToChannel(textMsgMes.getChannel(), mes);
                    }

                    break;

                /**
                 * a robot has connected to this server
                 */
                case SOCMessage.IMAROBOT:
                    handleIMAROBOT(c, (SOCImARobot) mes);

                    break;

                /**
                 * text message from a game
                 */
                case SOCMessage.GAMETEXTMSG:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    SOCGameTextMsg gameTextMsgMes = (SOCGameTextMsg) mes;
                    //recordGameEvent(mes, gameTextMsgMes.getGame(), gameTextMsgMes.toCmd());

                    ga = gameList.getGameData(gameTextMsgMes.getGame());

                    //currentGameEventRecord.setSnapshot(ga);
                    ///
                    /// command to add time to a game
                    ///
                    if ((gameTextMsgMes.getText().startsWith("*ADDTIME*")) || (gameTextMsgMes.getText().startsWith("*addtime*")) || (gameTextMsgMes.getText().startsWith("ADDTIME")) || (gameTextMsgMes.getText().startsWith("addtime")))
                    {
                        SOCGame gameData = gameList.getGameData(gameTextMsgMes.getGame());

                        if (gameData != null)
                        {
                            // add 30 min. to the expiration date
                            gameData.setExpiration(gameData.getExpiration() + 1800000);
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> This game will expire in " + ((gameData.getExpiration() - System.currentTimeMillis()) / 60000) + " minutes."));
                        }
                    }

                    ///
                    /// Check the time remaining for this game
                    ///
                    if (gameTextMsgMes.getText().startsWith("*CHECKTIME*"))
                    {
                        SOCGame gameData = gameList.getGameData(gameTextMsgMes.getGame());
                        messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> This game will expire in " + ((gameData.getExpiration() - System.currentTimeMillis()) / 60000) + " minutes."));
                    }
                    else if (gameTextMsgMes.getText().startsWith("*WHO*"))
                    {
                        Vector gameMembers = null;
                        gameList.takeMonitorForGame(gameTextMsgMes.getGame());

                        try
                        {
                            gameMembers = gameList.getMembers(gameTextMsgMes.getGame());
                        }
                        catch (Exception e)
                        {
                            D.ebugPrintln("Exception in *WHO* (gameMembers) - " + e);
                        }

                        gameList.releaseMonitorForGame(gameTextMsgMes.getGame());

                        if (gameMembers != null)
                        {
                            Enumeration membersEnum = gameMembers.elements();

                            while (membersEnum.hasMoreElements())
                            {
                                Connection conn = (Connection) membersEnum.nextElement();
                                messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> " + conn.data));
                            }
                        }
                    }

                    //
                    // useful for debuging 
                    //
                    if (c.data.equals("debug"))
                    {
                        if (gameTextMsgMes.getText().startsWith("rsrcs:"))
                        {
                            giveResources(gameTextMsgMes.getText(), ga);
                        }
                        else if (gameTextMsgMes.getText().startsWith("dev:"))
                        {
                            giveDevCard(gameTextMsgMes.getText(), ga);
                        }
                        else if (gameTextMsgMes.getText().startsWith("*KILLGAME*"))
                        {
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "********** " + (String) c.data + " KILLED THE GAME!!! **********"));
                            gameList.takeMonitor();

                            try
                            {
                                destroyGame(gameTextMsgMes.getGame());
                            }
                            catch (Exception e)
                            {
                                D.ebugPrintln("Exception in KILLGAME - " + e);
                            }

                            gameList.releaseMonitor();
                            broadcast(SOCDeleteGame.toCmd(gameTextMsgMes.getGame()));
                        }
                        else if (gameTextMsgMes.getText().startsWith("*STATS*"))
                        {
                            long diff = System.currentTimeMillis() - startTime;
                            long hours = diff / (60 * 60 * 1000);
                            long minutes = (diff - (hours * 60 * 60 * 1000)) / (60 * 1000);
                            long seconds = (diff - (hours * 60 * 60 * 1000) - (minutes * 60 * 1000)) / 1000;
                            Runtime rt = Runtime.getRuntime();
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Uptime: " + hours + ":" + minutes + ":" + seconds));
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Total connections: " + numberOfConnections));
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Current connections: " + connectionCount()));
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Total Users: " + numberOfUsers));
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Games started: " + numberOfGamesStarted));
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Games finished: " + numberOfGamesFinished));
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Total Memory: " + rt.totalMemory()));
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Free Memory: " + rt.freeMemory()));
                        }
                        else if (gameTextMsgMes.getText().startsWith("*GC*"))
                        {
                            Runtime rt = Runtime.getRuntime();
                            rt.gc();
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> GARBAGE COLLECTING DONE"));
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Free Memory: " + rt.freeMemory()));
                        }
                        else if (gameTextMsgMes.getText().startsWith("*STOP*"))
                        {
                            try
                            {
                                SOCDBHelper.cleanup();
                            }
                            catch (SQLException x) { }

                            stopServer();
                            System.exit(0);
                        }
                        else if (gameTextMsgMes.getText().startsWith("*BCAST* "))
                        {
                            ///
                            /// broadcast to all chat channels and games
                            ///
                            broadcast(SOCBCastTextMsg.toCmd(gameTextMsgMes.getText().substring(8)));
                        }
                        else if (gameTextMsgMes.getText().startsWith("*BOTLIST*"))
                        {
                            Enumeration robotsEnum = robots.elements();

                            while (robotsEnum.hasMoreElements())
                            {
                                Connection robotConn = (Connection) robotsEnum.nextElement();
                                messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> Robot: " + robotConn.data));
                                robotConn.put(SOCAdminPing.toCmd((gameTextMsgMes.getGame())));
                            }
                        }
                        else if (gameTextMsgMes.getText().startsWith("*RESETBOT* "))
                        {
                            String botName = gameTextMsgMes.getText().substring(11).trim();
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> botName = '" + botName + "'"));

                            Enumeration robotsEnum = robots.elements();

                            while (robotsEnum.hasMoreElements())
                            {
                                Connection robotConn = (Connection) robotsEnum.nextElement();
                                D.ebugPrintln("&&& '" + botName + "' == '" + robotConn.data + "' is " + (botName.equals((String) robotConn.data)));

                                if (botName.equals((String) robotConn.data))
                                {
                                    messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> SENDING RESET COMMAND TO " + botName));

                                    SOCAdminReset resetCmd = new SOCAdminReset();
                                    robotConn.put(resetCmd.toCmd());

                                    break;
                                }
                            }
                        }
                        else if (gameTextMsgMes.getText().startsWith("*KILLBOT* "))
                        {
                            String botName = gameTextMsgMes.getText().substring(10).trim();
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> botName = '" + botName + "'"));

                            Enumeration robotsEnum = robots.elements();

                            while (robotsEnum.hasMoreElements())
                            {
                                Connection robotConn = (Connection) robotsEnum.nextElement();
                                D.ebugPrintln("&&& '" + botName + "' == '" + robotConn.data + "' is " + (botName.equals((String) robotConn.data)));

                                if (botName.equals((String) robotConn.data))
                                {
                                    messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> DISCONNECTING " + botName));
                                    removeConnection(robotConn);
                                    removeConnectionCleanup(robotConn);

                                    break;
                                }
                            }
                        }
                        else
                        {
                            //
                            // Send the message to the members of the game
                            //
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), (String) c.data, gameTextMsgMes.getText()));
                        }
                    }
                    else
                    {
                        //
                        // Send the message to the members of the game
                        //
                        messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), (String) c.data, gameTextMsgMes.getText()));
                    }

                    //saveCurrentGameEventRecord(gameTextMsgMes.getGame());
                    break;

                /**
                 * "join a game" message
                 */
                case SOCMessage.JOINGAME:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleJOINGAME(c, (SOCJoinGame) mes);

                    //ga = (SOCGame)gamesData.get(((SOCJoinGame)mes).getGame());
                    //if (ga != null) {
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCJoinGame)mes).getGame());
                    //}
                    break;

                /**
                 * "leave a game" message
                 */
                case SOCMessage.LEAVEGAME:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleLEAVEGAME(c, (SOCLeaveGame) mes);

                    //ga = (SOCGame)gamesData.get(((SOCLeaveGame)mes).getGame());
                    //if (ga != null) {
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCLeaveGame)mes).getGame());
                    //}
                    break;

                /**
                 * someone wants to sit down
                 */
                case SOCMessage.SITDOWN:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleSITDOWN(c, (SOCSitDown) mes);

                    //ga = (SOCGame)gamesData.get(((SOCSitDown)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCSitDown)mes).getGame());
                    break;

                /**
                 * someone put a piece on the board
                 */
                case SOCMessage.PUTPIECE:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handlePUTPIECE(c, (SOCPutPiece) mes);

                    //ga = (SOCGame)gamesData.get(((SOCPutPiece)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCPutPiece)mes).getGame());
                    break;

                /**
                 * a player is moving the robber
                 */
                case SOCMessage.MOVEROBBER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleMOVEROBBER(c, (SOCMoveRobber) mes);

                    //ga = (SOCGame)gamesData.get(((SOCMoveRobber)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCMoveRobber)mes).getGame());
                    break;

                /**
                 * someone is starting a game
                 */
                case SOCMessage.STARTGAME:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleSTARTGAME(c, (SOCStartGame) mes);

                    //ga = (SOCGame)gamesData.get(((SOCStartGame)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCStartGame)mes).getGame());
                    break;

                case SOCMessage.ROLLDICE:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleROLLDICE(c, (SOCRollDice) mes);

                    //ga = (SOCGame)gamesData.get(((SOCRollDice)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCRollDice)mes).getGame());
                    break;

                case SOCMessage.DISCARD:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleDISCARD(c, (SOCDiscard) mes);

                    //ga = (SOCGame)gamesData.get(((SOCDiscard)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCDiscard)mes).getGame());
                    break;

                case SOCMessage.ENDTURN:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleENDTURN(c, (SOCEndTurn) mes);

                    //ga = (SOCGame)gamesData.get(((SOCEndTurn)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCEndTurn)mes).getGame());
                    break;

                case SOCMessage.CHOOSEPLAYER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleCHOOSEPLAYER(c, (SOCChoosePlayer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCChoosePlayer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCChoosePlayer)mes).getGame());
                    break;

                case SOCMessage.MAKEOFFER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleMAKEOFFER(c, (SOCMakeOffer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCMakeOffer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCMakeOffer)mes).getGame());
                    break;

                case SOCMessage.CLEAROFFER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleCLEAROFFER(c, (SOCClearOffer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCClearOffer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCClearOffer)mes).getGame());
                    break;

                case SOCMessage.REJECTOFFER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleREJECTOFFER(c, (SOCRejectOffer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCRejectOffer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCRejectOffer)mes).getGame());
                    break;

                case SOCMessage.ACCEPTOFFER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleACCEPTOFFER(c, (SOCAcceptOffer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCAcceptOffer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCAcceptOffer)mes).getGame());
                    break;

                case SOCMessage.BANKTRADE:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleBANKTRADE(c, (SOCBankTrade) mes);

                    //ga = (SOCGame)gamesData.get(((SOCBankTrade)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCBankTrade)mes).getGame());
                    break;

                case SOCMessage.BUILDREQUEST:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleBUILDREQUEST(c, (SOCBuildRequest) mes);

                    //ga = (SOCGame)gamesData.get(((SOCBuildRequest)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCBuildRequest)mes).getGame());
                    break;

                case SOCMessage.CANCELBUILDREQUEST:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleCANCELBUILDREQUEST(c, (SOCCancelBuildRequest) mes);

                    //ga = (SOCGame)gamesData.get(((SOCCancelBuildRequest)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCCancelBuildRequest)mes).getGame());
                    break;

                case SOCMessage.BUYCARDREQUEST:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleBUYCARDREQUEST(c, (SOCBuyCardRequest) mes);

                    //ga = (SOCGame)gamesData.get(((SOCBuyCardRequest)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCBuyCardRequest)mes).getGame());
                    break;

                case SOCMessage.PLAYDEVCARDREQUEST:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handlePLAYDEVCARDREQUEST(c, (SOCPlayDevCardRequest) mes);

                    //ga = (SOCGame)gamesData.get(((SOCPlayDevCardRequest)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCPlayDevCardRequest)mes).getGame());
                    break;

                case SOCMessage.DISCOVERYPICK:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleDISCOVERYPICK(c, (SOCDiscoveryPick) mes);

                    //ga = (SOCGame)gamesData.get(((SOCDiscoveryPick)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCDiscoveryPick)mes).getGame());
                    break;

                case SOCMessage.MONOPOLYPICK:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.data, "SERVER"));
                    handleMONOPOLYPICK(c, (SOCMonopolyPick) mes);

                    //ga = (SOCGame)gamesData.get(((SOCMonopolyPick)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCMonopolyPick)mes).getGame());
                    break;

                case SOCMessage.CHANGEFACE:
                    handleCHANGEFACE(c, (SOCChangeFace) mes);

                    break;

                case SOCMessage.SETSEATLOCK:
                    handleSETSEATLOCK(c, (SOCSetSeatLock) mes);

                    break;

                case SOCMessage.CREATEACCOUNT:
                    handleCREATEACCOUNT(c, (SOCCreateAccount) mes);

                    break;
                }                
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("ERROR -> " + e);
            e.printStackTrace();
        }
    }

    /**
     * authenticate the user
     * see if the user is in the db, if so then check the password
     * if they're not in the db, but they supplied a password
     * then send a message
     * if they're not in the db, and no password, then ok
     *
     * @param c         the user's connection
     * @param userName  the user's nickname
     * @param password  the user's password
     * @return true if the user has been authenticated
     */
    private boolean authenticateUser(Connection c, String userName, String password)
    {
        String userPassword = null;

        try
        {
            userPassword = SOCDBHelper.getUserPassword(userName);
        }
        catch (SQLException sqle)
        {
            // Indicates a db problem: don't authenticate empty password
            c.put(SOCStatusMessage.toCmd("Problem connecting to database, please try again later."));
            return false;
        }

        if (userPassword != null)
        {
            if (!userPassword.equals(password))
            {
                c.put(SOCStatusMessage.toCmd("Incorrect password for '" + userName + "'."));

                return false;
            }
        }
        else if (!password.equals(""))
        {
            c.put(SOCStatusMessage.toCmd("No user with the nickname '" + userName + "' is registered with the system."));

            return false;
        }

        //
        // Update the last login time
        //
        Date currentTime = new Date();

        //SOCDBHelper.updateLastlogin(userName, currentTime.getTime());
        //
        // Record the login info for this user
        //
        //SOCDBHelper.recordLogin(userName, c.host(), currentTime.getTime());
        return true;
    }

    /**
     * Handle the "join a channel" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleJOIN(Connection c, SOCJoin mes)
    {
        if (c != null)
        {
            D.ebugPrintln("handleJOIN: " + mes);

            /**
             * Check that the nickname is ok
             */
            if ((c.data == null) && (!checkNickname(mes.getNickname())))
            {
                c.put(SOCStatusMessage.toCmd("Someone with that nickname is already logged into the system."));

                return;
            }

            if ((c.data == null) && (!authenticateUser(c, mes.getNickname(), mes.getPassword())))
            {
                return;
            }

            /**
             * Check that the channel name is ok
             */

            /*
               if (!checkChannelName(mes.getChannel())) {
               return;
               }
             */
            if (c.data == null)
            {
                c.data = mes.getNickname();
                numberOfUsers++;
            }

            /**
             * Tell the client that everything is good to go
             */
            c.put(SOCJoinAuth.toCmd(mes.getNickname(), mes.getChannel()));
            c.put(SOCStatusMessage.toCmd("Welcome to Java Settlers of Catan!"));

            /**
             * Add the Connection to the channel
             */
            String ch = mes.getChannel();

            if (channelList.takeMonitorForChannel(ch))
            {
                try
                {
                    connectToChannel(c, ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleJOIN (connectToChannel) - " + e);
                }

                channelList.releaseMonitorForChannel(ch);
            }
            else
            {
                /**
                 * the channel did not exist, create it
                 */
                channelList.takeMonitor();

                try
                {
                    channelList.createChannel(ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleJOIN (createChannel) - " + e);
                }

                channelList.releaseMonitor();
                broadcast(SOCNewChannel.toCmd(ch));
                c.put(SOCMembers.toCmd(ch, channelList.getMembers(ch)));
                D.ebugPrintln("*** " + c.data + " joined the channel " + ch);
                channelList.takeMonitorForChannel(ch);

                try
                {
                    channelList.addMember(c, ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleJOIN (addMember) - " + e);
                }

                channelList.releaseMonitorForChannel(ch);
            }

            /**
             * let everyone know about the change
             */
            messageToChannel(ch, new SOCJoin(mes.getNickname(), "", "dummyhost", ch));
        }
    }

    /**
     * Handle the "leave a channel" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleLEAVE(Connection c, SOCLeave mes)
    {
        D.ebugPrintln("handleLEAVE: " + mes);

        if (c != null)
        {
            boolean destroyedChannel = false;
            channelList.takeMonitorForChannel(mes.getChannel());

            try
            {
                destroyedChannel = leaveChannel(c, mes.getChannel(), false);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in handleLEAVE - " + e);
            }

            channelList.releaseMonitorForChannel(mes.getChannel());

            if (destroyedChannel)
            {
                broadcast(SOCDeleteChannel.toCmd(mes.getChannel()));
            }
        }
    }

    /**
     * Handle the "I'm a robot" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleIMAROBOT(Connection c, SOCImARobot mes)
    {
        if (c != null)
        {
            SOCRobotParameters params = null;
            //
            // send the current robot parameters
            //
            try
            {
                params = SOCDBHelper.retrieveRobotParams(mes.getNickname());
                D.ebugPrintln("*** Robot Parameters for " + mes.getNickname() + " = " + params);
            }
            catch (SQLException sqle)
            {
                System.err.println("Error retrieving robot parameters from db: Using defaults.");
            }

            if (params == null)
            {
                params = new SOCRobotParameters(120, 35, 0.13f, 1.0f, 1.0f, 3.0f, 1.0f, 1, 1);
            }

            c.put(SOCUpdateRobotParams.toCmd(params));

            //
            // add this connection to the robot list
            //
            c.data = mes.getNickname();
            robots.addElement(c);
        }
    }

    /**
     * Handle the "join a game" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleJOINGAME(Connection c, SOCJoinGame mes)
    {
        if (c != null)
        {
            D.ebugPrintln("handleJOINGAME: " + mes);

            /**
             * Check that the nickname is ok
             */
            if ((c.data == null) && (!checkNickname(mes.getNickname())))
            {
                c.put(SOCStatusMessage.toCmd("Someone with that nickname is already logged into the system."));

                return;
            }

            if ((c.data == null) && (!authenticateUser(c, mes.getNickname(), mes.getPassword())))
            {
                return;
            }

            if (c != null)
            {
                if (c.data == null)
                {
                    c.data = mes.getNickname();
                    numberOfUsers++;
                }
            }

            /**
             * Check that the game name is ok
             */

            /*
               if (!checkGameName(mes.getGame())) {
               return;
               }
             */

            /**
             * Tell the client that everything is good to go
             */
            if (connectToGame(c, mes.getGame()))
            {
                String gameName = mes.getGame();

                /**
                 * send the entire state of the game
                 */
                SOCGame gameData = gameList.getGameData(gameName);

                if (gameData != null)
                {
                    c.put(SOCJoinGameAuth.toCmd(mes.getGame()));
                    c.put(SOCStatusMessage.toCmd("Welcome to Java Settlers of Catan!"));

                    //c.put(SOCGameState.toCmd(gameName, gameData.getGameState()));
                    for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                    {
                        SOCPlayer pl = gameData.getPlayer(i);

                        if ((pl.getName() != null) && (!gameData.isSeatVacant(i)))
                        {
                            c.put(SOCSitDown.toCmd(gameName, pl.getName(), i, pl.isRobot()));
                        }

                        /**
                         * send the seat lock information
                         */
                        messageToPlayer(c, new SOCSetSeatLock(gameName, i, gameData.isSeatLocked(i)));
                    }

                    SOCBoardLayout bl = getBoardLayoutMessage(gameData);
                    c.put(bl.toCmd());

                    for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                    {
                        SOCPlayer pl = gameData.getPlayer(i);

                        if (pl.getName() != null)
                        {
                            Enumeration piecesEnum = pl.getPieces().elements();

                            while (piecesEnum.hasMoreElements())
                            {
                                SOCPlayingPiece piece = (SOCPlayingPiece) piecesEnum.nextElement();

                                if (piece.getType() == SOCPlayingPiece.CITY)
                                {
                                    c.put(SOCPutPiece.toCmd(gameName, i, SOCPlayingPiece.SETTLEMENT, piece.getCoordinates()));
                                }

                                c.put(SOCPutPiece.toCmd(gameName, i, piece.getType(), piece.getCoordinates()));
                            }

                            /**
                             * send potential settlement list
                             */
                            Vector psList = new Vector();

                            for (int j = 0x23; j <= 0xDC; j++)
                            {
                                if (pl.isPotentialSettlement(j))
                                {
                                    psList.addElement(new Integer(j));
                                }
                            }

                            c.put(SOCPotentialSettlements.toCmd(gameName, i, psList));

                            /**
                             * send coords of the last settlement
                             */
                            c.put(SOCLastSettlement.toCmd(gameName, i, pl.getLastSettlementCoord()));

                            /**
                             * send number of playing pieces in hand
                             */
                            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.ROADS, pl.getNumPieces(SOCPlayingPiece.ROAD)));
                            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.SETTLEMENTS, pl.getNumPieces(SOCPlayingPiece.SETTLEMENT)));
                            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.CITIES, pl.getNumPieces(SOCPlayingPiece.CITY)));

                            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.UNKNOWN, pl.getResources().getTotal()));

                            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.NUMKNIGHTS, pl.getNumKnights()));

                            int numDevCards = pl.getDevCards().getTotal();

                            for (int j = 0; j < numDevCards; j++)
                            {
                                c.put(SOCDevCard.toCmd(gameName, i, SOCDevCard.ADDOLD, SOCDevCardConstants.UNKNOWN));
                            }

                            c.put(SOCFirstPlayer.toCmd(gameName, gameData.getFirstPlayer()));

                            c.put(SOCDevCardCount.toCmd(gameName, gameData.getNumDevCards()));

                            c.put(SOCChangeFace.toCmd(gameName, i, pl.getFaceId()));

                            c.put(SOCDiceResult.toCmd(gameName, gameData.getCurrentDice()));
                        }
                    }

                    /// 
                    /// send who has longest road
                    ///
                    SOCPlayer lrPlayer = gameData.getPlayerWithLongestRoad();
                    int lrPlayerNum = -1;

                    if (lrPlayer != null)
                    {
                        lrPlayerNum = lrPlayer.getPlayerNumber();
                    }

                    c.put(SOCLongestRoad.toCmd(gameName, lrPlayerNum));

                    ///
                    /// send who has largest army
                    ///
                    SOCPlayer laPlayer = gameData.getPlayerWithLargestArmy();
                    int laPlayerNum = -1;

                    if (laPlayer != null)
                    {
                        laPlayerNum = laPlayer.getPlayerNumber();
                    }

                    c.put(SOCLargestArmy.toCmd(gameName, laPlayerNum));

                    String membersCommand = null;
                    gameList.takeMonitorForGame(gameName);

                    try
                    {
                        Vector gameMembers = gameList.getMembers(gameName);
                        membersCommand = SOCGameMembers.toCmd(gameName, gameMembers);
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintln("Exception in handleJOINGAME (gameMembers) - " + e);
                    }

                    gameList.releaseMonitorForGame(gameName);
                    c.put(membersCommand);
                    c.put(SOCSetTurn.toCmd(gameName, gameData.getCurrentPlayerNumber()));
                    c.put(SOCGameState.toCmd(gameName, gameData.getGameState()));
                    D.ebugPrintln("*** " + c.data + " joined the game " + gameName);

                    //messageToGame(gameName, new SOCGameTextMsg(gameName, SERVERNAME, n+" joined the game"));
                    /**
                     * Let everyone else know about the change
                     */
                    messageToGame(mes.getGame(), new SOCJoinGame(mes.getNickname(), "", "dummyhost", mes.getGame()));
                }
            }
        }
    }

    /**
     * Handle the "leave game" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleLEAVEGAME(Connection c, SOCLeaveGame mes)
    {
        if (c != null)
        {
            recordGameEvent(mes, mes.getGame(), mes.toCmd());
            boolean isMember = false;
            gameList.takeMonitorForGame(mes.getGame());

            try
            {
                isMember = gameList.isMember(c, mes.getGame());
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in handleLEAVEGAME (isMember) - " + e);
            }

            gameList.releaseMonitorForGame(mes.getGame());

            if (isMember)
            {
                boolean gameDestroyed = false;
                gameList.takeMonitorForGame(mes.getGame());

                try
                {
                    gameDestroyed = leaveGame(c, mes.getGame(), false);
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleLEAVEGAME (leaveGame) - " + e);
                }

                gameList.releaseMonitorForGame(mes.getGame());

                if (gameDestroyed)
                {
                    broadcast(SOCDeleteGame.toCmd(mes.getGame()));
                }
                else
                {
                    /*
                       SOCLeaveGame leaveMessage = new SOCLeaveGame((String)c.data, c.host(), mes.getGame());
                       messageToGame(mes.getGame(), leaveMessage);
                       //recordGameEvent(mes, mes.getGame(), leaveMessage.toCmd());
                     */
                }

                /**
                 * if it's a robot, remove it from the request list
                 */
                Vector requests = (Vector) robotDismissRequests.get(mes.getGame());

                if (requests != null)
                {
                    Enumeration reqEnum = requests.elements();
                    SOCReplaceRequest req = null;

                    while (reqEnum.hasMoreElements())
                    {
                        SOCReplaceRequest tempReq = (SOCReplaceRequest) reqEnum.nextElement();

                        if (tempReq.getLeaving() == c)
                        {
                            req = tempReq;

                            break;
                        }
                    }

                    if (req != null)
                    {
                        requests.removeElement(req);

                        /**
                         * let the person replacing the robot sit down
                         */
                        SOCGame ga = gameList.getGameData(mes.getGame());
                        sitDown(ga, req.getArriving(), req.getSitDownMessage().getPlayerNumber(), req.getSitDownMessage().isRobot());
                    }
                }
            }
        }
    }

    /**
     * handle "sit down" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleSITDOWN(Connection c, SOCSitDown mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                /**
                 * make sure this player isn't already sitting
                 */
                boolean canSit = true;

                /*
                   for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
                   if (ga.getPlayer(i).getName() == (String)c.data) {
                   canSit = false;
                   break;
                   }
                   }
                 */
                //D.ebugPrintln("ga.isSeatVacant(mes.getPlayerNumber()) = "+ga.isSeatVacant(mes.getPlayerNumber()));
                /**
                 * make sure a person isn't sitting here already
                 */
                ga.takeMonitor();

                try
                {
                    if (!ga.isSeatVacant(mes.getPlayerNumber()))
                    {
                        SOCPlayer seatedPlayer = ga.getPlayer(mes.getPlayerNumber());

                        if (seatedPlayer.isRobot() && (!ga.isSeatLocked(mes.getPlayerNumber())) && (ga.getCurrentPlayerNumber() != mes.getPlayerNumber()))
                        {
                            /**
                             * boot the robot out of the game
                             */
                            Connection robotCon = null;
                            Enumeration conEnum = conns.elements();

                            while (conEnum.hasMoreElements())
                            {
                                Connection con = (Connection) conEnum.nextElement();

                                if (seatedPlayer.getName().equals((String) con.data))
                                {
                                    robotCon = con;

                                    break;
                                }
                            }

                            robotCon.put(SOCRobotDismiss.toCmd(mes.getGame()));

                            /**
                             * this connection has to wait for the robot to leave
                             * and then it can sit down
                             */
                            Vector disRequests = (Vector) robotDismissRequests.get(mes.getGame());
                            SOCReplaceRequest req = new SOCReplaceRequest(c, robotCon, mes);

                            if (disRequests == null)
                            {
                                disRequests = new Vector();
                                disRequests.addElement(req);
                                robotDismissRequests.put(mes.getGame(), disRequests);
                            }
                            else
                            {
                                disRequests.addElement(req);
                            }
                        }

                        canSit = false;
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleSITDOWN - " + e);
                }

                ga.releaseMonitor();

                /**
                 * if this is a robot, remove it from the request list
                 */
                Vector joinRequests = (Vector) robotJoinRequests.get(mes.getGame());

                if (joinRequests != null)
                {
                    joinRequests.removeElement(c);
                }

                //D.ebugPrintln("canSit 2 = "+canSit);
                if (canSit)
                {
                    sitDown(ga, c, mes.getPlayerNumber(), mes.isRobot());
                }
                else
                {
                    /**
                     * if the robot can't sit, tell it to go away
                     */
                    if (mes.isRobot())
                    {
                        c.put(SOCRobotDismiss.toCmd(mes.getGame()));
                    }
                }
            }
        }
    }

    /**
     * handle "put piece" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handlePUTPIECE(Connection c, SOCPutPiece mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCPlayer player = ga.getPlayer((String) c.data);

                    /**
                     * make sure the player can do it
                     */
                    if (checkTurn(c, ga))
                    {
                        /*
                           if (D.ebugOn) {
                           D.ebugPrintln("BEFORE");
                           for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                           SOCPlayer tmpPlayer = ga.getPlayer(pn);
                           D.ebugPrintln("Player # "+pn);
                           for (int i = 0x22; i < 0xCC; i++) {
                           if (tmpPlayer.isPotentialRoad(i))
                           D.ebugPrintln("### POTENTIAL ROAD AT "+Integer.toHexString(i));
                           }
                           }
                           }
                         */
                        switch (mes.getPieceType())
                        {
                        case SOCPlayingPiece.ROAD:

                            SOCRoad rd = new SOCRoad(player, mes.getCoordinates());

                            if ((ga.getGameState() == SOCGame.START1B) || (ga.getGameState() == SOCGame.START2B) || (ga.getGameState() == SOCGame.PLACING_ROAD) || (ga.getGameState() == SOCGame.PLACING_FREE_ROAD1) || (ga.getGameState() == SOCGame.PLACING_FREE_ROAD2))
                            {
                                if (player.isPotentialRoad(mes.getCoordinates()))
                                {
                                    ga.putPiece(rd);

                                    /*
                                       if (D.ebugOn) {
                                       D.ebugPrintln("AFTER");
                                       for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                                       SOCPlayer tmpPlayer = ga.getPlayer(pn);
                                       D.ebugPrintln("Player # "+pn);
                                       for (int i = 0x22; i < 0xCC; i++) {
                                       if (tmpPlayer.isPotentialRoad(i))
                                       D.ebugPrintln("### POTENTIAL ROAD AT "+Integer.toHexString(i));
                                       }
                                       }
                                       }
                                     */
                                    messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, (String) c.data + " built a road."));
                                    messageToGame(ga.getName(), new SOCPutPiece(mes.getGame(), player.getPlayerNumber(), SOCPlayingPiece.ROAD, mes.getCoordinates()));
                                    broadcastGameStats(ga);
                                    sendGameState(ga);
                                    recordGameEvent(mes, mes.getGame(), mes.toCmd());

                                    if (!checkTurn(c, ga))
                                    {
                                        sendTurn(ga);
                                    }
                                }
                                else
                                {
                                    D.ebugPrintln("ILLEGAL ROAD");
                                    c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a road there."));
                                }
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a road right now."));
                            }

                            break;

                        case SOCPlayingPiece.SETTLEMENT:

                            SOCSettlement se = new SOCSettlement(player, mes.getCoordinates());

                            if ((ga.getGameState() == SOCGame.START1A) || (ga.getGameState() == SOCGame.START2A) || (ga.getGameState() == SOCGame.PLACING_SETTLEMENT))
                            {
                                if (player.isPotentialSettlement(mes.getCoordinates()))
                                {
                                    ga.putPiece(se);
                                    recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                    messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, (String) c.data + " built a settlement."));
                                    messageToGame(ga.getName(), new SOCPutPiece(mes.getGame(), player.getPlayerNumber(), SOCPlayingPiece.SETTLEMENT, mes.getCoordinates()));
                                    broadcastGameStats(ga);
                                    sendGameState(ga);

                                    if (!checkTurn(c, ga))
                                    {
                                        sendTurn(ga);
                                    }
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a settlement there."));
                                }
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a settlement right now."));
                            }

                            break;

                        case SOCPlayingPiece.CITY:

                            SOCCity ci = new SOCCity(player, mes.getCoordinates());

                            if (ga.getGameState() == SOCGame.PLACING_CITY)
                            {
                                if (player.isPotentialCity(mes.getCoordinates()))
                                {
                                    ga.putPiece(ci);
                                    recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                    messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, (String) c.data + " built a city."));
                                    messageToGame(ga.getName(), new SOCPutPiece(mes.getGame(), player.getPlayerNumber(), SOCPlayingPiece.CITY, mes.getCoordinates()));
                                    broadcastGameStats(ga);
                                    sendGameState(ga);

                                    if (!checkTurn(c, ga))
                                    {
                                        sendTurn(ga);
                                    }
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a city there."));
                                }
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a city right now."));
                            }

                            break;
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "move robber" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleMOVEROBBER(Connection c, SOCMoveRobber mes)
    {
        if (c != null)
        {
            String gn = mes.getGame();
            SOCGame ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCPlayer player = ga.getPlayer((String) c.data);

                    /**
                     * make sure the player can do it
                     */
                    if (ga.canMoveRobber(player.getPlayerNumber(), mes.getCoordinates()))
                    {
                        SOCMoveRobberResult result = ga.moveRobber(player.getPlayerNumber(), mes.getCoordinates());
                        recordGameEvent(mes, mes.getGame(), mes.toCmd());
                        messageToGame(ga.getName(), new SOCMoveRobber(ga.getName(), player.getPlayerNumber(), mes.getCoordinates()));
                        messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, (String) c.data + " moved the robber."));

                        Vector victims = result.getVictims();

                        /** only one possible victim */
                        if (victims.size() == 1)
                        {
                            /**
                             * report what was stolen
                             */
                            SOCPlayer victim = (SOCPlayer) victims.firstElement();
                            reportRobbery(ga, player, victim, result.getLoot());
                            SOCChoosePlayer robmes = new SOCChoosePlayer(gn, victim.getPlayerNumber());
                            recordGameEvent(robmes, robmes.getGame(), robmes.toCmd());
                        }

                        /**
                         * else, the player needs to choose a victim
                         */
                        sendGameState(ga);
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't move the robber."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "start game" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleSTARTGAME(Connection c, SOCStartGame mes)
    {
        if (c != null)
        {
            String gn = mes.getGame();
            SOCGame ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (ga.getGameState() == SOCGame.NEW)
                    {
                        Vector requests = new Vector();

                        boolean seatsFull = true;

                        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                        {
                            if (ga.isSeatVacant(i))
                            {
                                seatsFull = false;

                                break;
                            }
                        }

                        if (!seatsFull)
                        {
                            if (robots.isEmpty())
                            {
                                messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, "No robots on this server, please fill all seats before starting."));
                            }
                            else
                            {
                                //
                                // count the number of empty seats
                                //
                                int numEmpty = 0;

                                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                                {
                                    if (ga.isSeatVacant(i))
                                    {
                                        numEmpty++;
                                    }
                                }

                                //
                                // make sure there are enough robots connected
                                //
                                if (numEmpty > robots.size())
                                {
                                    String m = "Sorry, not enough robots to fill all the seats.  Only " + robots.size() + " robots are available.";
                                    messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, m));
                                }
                                else
                                {
                                    ga.setGameState(SOCGame.READY);

                                    /**
                                     * Fill all the empty seats with robots
                                     */

                                    ///
                                    /// shuffle the indexes to distribute load
                                    ///
                                    int[] robotIndexes = new int[robots.size()];

                                    for (int i = 0; i < robots.size(); i++)
                                    {
                                        robotIndexes[i] = i;
                                    }

                                    for (int j = 0; j < 3; j++)
                                    {
                                        for (int i = 0; i < robotIndexes.length; i++)
                                        {
                                            int idx = Math.abs(rand.nextInt() % (robotIndexes.length - i));
                                            int tmp = robotIndexes[idx];
                                            robotIndexes[idx] = robotIndexes[i];
                                            robotIndexes[i] = tmp;
                                        }
                                    }

                                    if (D.ebugOn)
                                    {
                                        for (int i = 0; i < robots.size();
                                                i++)
                                        {
                                            D.ebugPrintln("^^^ robotIndexes[" + i + "]=" + robotIndexes[i]);
                                        }
                                    }

                                    int idx = 0;

                                    for (int i = 0; i < SOCGame.MAXPLAYERS;
                                            i++)
                                    {
                                        if (ga.isSeatVacant(i))
                                        {
                                            /**
                                             * fetch a robot player
                                             */
                                            if (idx < robots.size())
                                            {
                                                messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, "Fetching a robot player..."));

                                                Connection robotConn = (Connection) robots.get(robotIndexes[idx]);
                                                idx++;

                                                /**
                                                 * make the request
                                                 */
                                                if (robotConn.put(SOCJoinGameRequest.toCmd(gn, i)))
                                                {
                                                    /**
                                                     * record the request
                                                     */
                                                    D.ebugPrintln("@@@ JOIN GAME REQUEST for " + (String) robotConn.data);
                                                    requests.addElement(robotConn);
                                                }
                                                else
                                                {
                                                    // !!! won't ever happen
                                                    messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, "*** Error on robot request! ***"));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        /**
                         * If this doesn't need robots, then start the game.
                         * Otherwise wait for them to sit before starting the game.
                         */
                        if (seatsFull)
                        {
                            startGame(ga);
                        }
                        else
                        {
                            if (!requests.isEmpty())
                            {
                                robotJoinRequests.put(gn, requests);
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "roll dice" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleROLLDICE(Connection c, SOCRollDice mes)
    {
        if (c != null)
        {
            String gn = mes.getGame();
            SOCGame ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (ga.canRollDice(ga.getPlayer((String) c.data).getPlayerNumber()))
                    {
                        IntPair dice = ga.rollDice();
                        recordGameEvent(mes, mes.getGame(), mes.toCmd());
                        messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, (String) c.data + " rolled a " + dice.getA() + " and a " + dice.getB() + "."));
                        messageToGame(gn, new SOCDiceResult(gn, ga.getCurrentDice()));

                        /**
                         * if the roll is not 7, tell players what they got
                         */
                        if (ga.getCurrentDice() != 7)
                        {
                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                SOCResourceSet rsrcs = ga.getResourcesGainedFromRoll(ga.getPlayer(i), ga.getCurrentDice());

                                if (rsrcs.getTotal() == 0)
                                {
                                    messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, ga.getPlayer(i).getName() + " got nothing."));
                                }
                                else
                                {
                                    String message = ga.getPlayer(i).getName() + " got ";
                                    int cl;
                                    int or;
                                    int sh;
                                    int wh;
                                    int wo;
                                    cl = rsrcs.getAmount(SOCResourceConstants.CLAY);
                                    or = rsrcs.getAmount(SOCResourceConstants.ORE);
                                    sh = rsrcs.getAmount(SOCResourceConstants.SHEEP);
                                    wh = rsrcs.getAmount(SOCResourceConstants.WHEAT);
                                    wo = rsrcs.getAmount(SOCResourceConstants.WOOD);

                                    if (cl > 0)
                                    {
                                        messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, cl));
                                        message += (cl + " clay");

                                        if ((or + sh + wh + wo) > 0)
                                        {
                                            message += ", ";
                                        }
                                    }

                                    if (or > 0)
                                    {
                                        messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.GAIN, SOCPlayerElement.ORE, or));
                                        message += (or + " ore");

                                        if ((sh + wh + wo) > 0)
                                        {
                                            message += ", ";
                                        }
                                    }

                                    if (sh > 0)
                                    {
                                        messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, sh));
                                        message += (sh + " sheep");

                                        if ((wh + wo) > 0)
                                        {
                                            message += ", ";
                                        }
                                    }

                                    if (wh > 0)
                                    {
                                        messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, wh));
                                        message += (wh + " wheat");

                                        if (wo > 0)
                                        {
                                            message += ", ";
                                        }
                                    }

                                    if (wo > 0)
                                    {
                                        messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, wo));
                                        message += (wo + " wood");
                                    }

                                    message += ".";
                                    messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, message));
                                }

                                //
                                //  send all resource info for accuracy
                                //
                                Connection playerCon = null;
                                Enumeration conEnum = conns.elements();

                                while (conEnum.hasMoreElements())
                                {
                                    Connection con = (Connection) conEnum.nextElement();

                                    if (ga.getPlayer(i).getName().equals((String) con.data))
                                    {
                                        playerCon = con;

                                        break;
                                    }
                                }

                                if (playerCon != null)
                                {
                                    SOCResourceSet resources = ga.getPlayer(i).getResources();
                                    messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.CLAY, resources.getAmount(SOCPlayerElement.CLAY)));
                                    messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.ORE, resources.getAmount(SOCPlayerElement.ORE)));
                                    messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.SHEEP, resources.getAmount(SOCPlayerElement.SHEEP)));
                                    messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.WHEAT, resources.getAmount(SOCPlayerElement.WHEAT)));
                                    messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.WOOD, resources.getAmount(SOCPlayerElement.WOOD)));
                                    messageToGame(ga.getName(), new SOCResourceCount(ga.getName(), i, resources.getTotal()));
                                }
                            }

                            /*
                               if (D.ebugOn) {
                               for (int i=0; i < SOCGame.MAXPLAYERS; i++) {
                               SOCResourceSet rsrcs = ga.getPlayer(i).getResources();
                               String resourceMessage = "PLAYER "+i+" RESOURCES: ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.CLAY)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.ORE)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.SHEEP)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.WHEAT)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.WOOD)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.UNKNOWN)+" ";
                               messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, resourceMessage));
                               }
                               }
                             */
                        }
                        else
                        {
                            /**
                             * player rolled 7
                             */
                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                if (ga.getPlayer(i).getResources().getTotal() > 7)
                                {
                                    Enumeration coEnum = getConnections();

                                    while (coEnum.hasMoreElements())
                                    {
                                        Connection con = (Connection) coEnum.nextElement();

                                        if (ga.getPlayer(i).getName().equals((String) con.data))
                                        {
                                            con.put(SOCDiscardRequest.toCmd(ga.getName(), ga.getPlayer(i).getResources().getTotal() / 2));

                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        sendGameState(ga);
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gn, SERVERNAME, "You can't roll right now."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleROLLDICE - " + e);
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "discard" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleDISCARD(Connection c, SOCDiscard mes)
    {
        if (c != null)
        {
            String gn = mes.getGame();
            SOCGame ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCPlayer player = ga.getPlayer((String) c.data);

                    if (ga.canDiscard(player.getPlayerNumber(), mes.getResources()))
                    {
                        ga.discard(player.getPlayerNumber(), mes.getResources());
                        recordGameEvent(mes, mes.getGame(), mes.toCmd());

                        /**
                         * tell the player client that the player discarded the resources
                         */
                        int cl;

                        /**
                         * tell the player client that the player discarded the resources
                         */
                        int or;

                        /**
                         * tell the player client that the player discarded the resources
                         */
                        int sh;

                        /**
                         * tell the player client that the player discarded the resources
                         */
                        int wh;

                        /**
                         * tell the player client that the player discarded the resources
                         */
                        int wo;
                        cl = mes.getResources().getAmount(SOCResourceConstants.CLAY);
                        or = mes.getResources().getAmount(SOCResourceConstants.ORE);
                        sh = mes.getResources().getAmount(SOCResourceConstants.SHEEP);
                        wh = mes.getResources().getAmount(SOCResourceConstants.WHEAT);
                        wo = mes.getResources().getAmount(SOCResourceConstants.WOOD);

                        if (cl > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, cl));
                        }

                        if (or > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, or));
                        }

                        if (sh > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, sh));
                        }

                        if (wh > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, wh));
                        }

                        if (wo > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, wo));
                        }

                        /**
                         * tell everyone else that the player discarded unknown resources
                         */
                        Vector exceptions = new Vector(1);
                        exceptions.addElement(c);
                        messageToGameExcept(gn, exceptions, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.UNKNOWN, mes.getResources().getTotal()));
                        messageToGame(gn, new SOCGameTextMsg(ga.getName(), SERVERNAME, (String) c.data + " discarded " + mes.getResources().getTotal() + " resources."));
                        //recordGameEvent(mes, mes.getGame(), mes.toCmd());
                        sendGameState(ga);
                    }
                    else
                    {
                        /**
                         * there could be a better feedback message here
                         */
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't discard that many cards."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "end turn" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleENDTURN(Connection c, SOCEndTurn mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.canEndTurn(ga.getPlayer((String) c.data).getPlayerNumber()))
                        {
                            ga.endTurn();
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            sendGameState(ga);

                            /**
                             * clear any trade offers
                             */
                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                messageToGame(ga.getName(), new SOCClearOffer(ga.getName(), i));
                            }

                            /**
                             * send who's turn it is
                             */
                            sendTurn(ga);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't end your turn yet."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "choose player" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleCHOOSEPLAYER(Connection c, SOCChoosePlayer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.canChoosePlayer(mes.getChoice()))
                        {
                            int rsrc = ga.stealFromPlayer(mes.getChoice());
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            reportRobbery(ga, ga.getPlayer((String) c.data), ga.getPlayer(mes.getChoice()), rsrc);
                            sendGameState(ga);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't steal from that player."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "make offer" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleMAKEOFFER(Connection c, SOCMakeOffer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCTradeOffer offer = mes.getOffer();

                    /**
                     * remake the offer with data that we know is accurate,
                     * namely the 'from' datum
                     */
                    SOCPlayer player = ga.getPlayer((String) c.data);

                    if (player != null)
                    {
                        SOCTradeOffer remadeOffer = new SOCTradeOffer(ga.getName(), player.getPlayerNumber(), offer.getTo(), offer.getGiveSet(), offer.getGetSet());
                        player.setCurrentOffer(remadeOffer);
                        messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, (String) c.data + " made an offer to trade."));

                        SOCMakeOffer makeOfferMessage = new SOCMakeOffer(ga.getName(), remadeOffer);
                        messageToGame(ga.getName(), makeOfferMessage);

                        recordGameEvent(mes, mes.getGame(), mes.toCmd());

                        /**
                         * clear all the trade messages because a new offer has been made
                         */
                        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                        {
                            messageToGame(ga.getName(), new SOCClearTradeMsg(ga.getName(), i));
                        }
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "clear offer" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleCLEAROFFER(Connection c, SOCClearOffer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    ga.getPlayer((String) c.data).setCurrentOffer(null);
                    messageToGame(ga.getName(), new SOCClearOffer(ga.getName(), ga.getPlayer((String) c.data).getPlayerNumber()));
                    recordGameEvent(mes, mes.getGame(), mes.toCmd());

                    /**
                     * clear all the trade messages
                     */
                    for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                    {
                        messageToGame(ga.getName(), new SOCClearTradeMsg(ga.getName(), i));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "reject offer" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleREJECTOFFER(Connection c, SOCRejectOffer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                SOCPlayer player = ga.getPlayer((String) c.data);

                if (player != null)
                {
                    SOCRejectOffer rejectMessage = new SOCRejectOffer(ga.getName(), player.getPlayerNumber());
                    messageToGame(ga.getName(), rejectMessage);

                    recordGameEvent(mes, mes.getGame(), mes.toCmd());
                }
            }
        }
    }

    /**
     * handle "accept offer" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleACCEPTOFFER(Connection c, SOCAcceptOffer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCPlayer player = ga.getPlayer((String) c.data);

                    if (player != null)
                    {
                        int acceptingNumber = player.getPlayerNumber();

                        if (ga.canMakeTrade(mes.getOfferingNumber(), acceptingNumber))
                        {
                            ga.makeTrade(mes.getOfferingNumber(), acceptingNumber);
                            reportTrade(ga, mes.getOfferingNumber(), acceptingNumber);

                            recordGameEvent(mes, mes.getGame(), mes.toCmd());

                            /**
                             * clear all offers
                             */
                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                ga.getPlayer(i).setCurrentOffer(null);
                                messageToGame(ga.getName(), new SOCClearOffer(ga.getName(), i));
                            }

                            /**
                             * send a message to the bots that the offer was accepted
                             */
                            messageToGame(ga.getName(), mes);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't make that trade."));
                        }
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "bank trade" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleBANKTRADE(Connection c, SOCBankTrade mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.canMakeBankTrade(mes.getGiveSet(), mes.getGetSet()))
                        {
                            ga.makeBankTrade(mes.getGiveSet(), mes.getGetSet());
                            reportBankTrade(ga, mes.getGiveSet(), mes.getGetSet());

                            recordGameEvent(mes, mes.getGame(), mes.toCmd());
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't make that trade."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "build request" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleBUILDREQUEST(Connection c, SOCBuildRequest mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.getGameState() == SOCGame.PLAY1)
                        {
                            SOCPlayer player = ga.getPlayer((String) c.data);

                            switch (mes.getPieceType())
                            {
                            case SOCPlayingPiece.ROAD:

                                if (ga.couldBuildRoad(player.getPlayerNumber()))
                                {
                                    ga.buyRoad(player.getPlayerNumber());
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, 1));
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, 1));
                                    //recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                    sendGameState(ga);
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a road."));
                                }

                                break;

                            case SOCPlayingPiece.SETTLEMENT:

                                if (ga.couldBuildSettlement(player.getPlayerNumber()))
                                {
                                    ga.buySettlement(player.getPlayerNumber());
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, 1));
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, 1));
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, 1));
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, 1));
                                    sendGameState(ga);
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a settlement."));
                                }

                                break;

                            case SOCPlayingPiece.CITY:

                                if (ga.couldBuildCity(player.getPlayerNumber()))
                                {
                                    ga.buyCity(player.getPlayerNumber());
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, 3));
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, 2));
                                    sendGameState(ga);
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build a city."));
                                }

                                break;
                            }
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't build now."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "cancel build request" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleCANCELBUILDREQUEST(Connection c, SOCCancelBuildRequest mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        SOCPlayer player = ga.getPlayer((String) c.data);

                        switch (mes.getPieceType())
                        {
                        case SOCPlayingPiece.ROAD:

                            if (ga.getGameState() == SOCGame.PLACING_ROAD)
                            {
                                ga.cancelBuildRoad(player.getPlayerNumber());
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, 1));
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, 1));
                                //recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You didn't buy a road."));
                            }

                            break;

                        case SOCPlayingPiece.SETTLEMENT:

                            if (ga.getGameState() == SOCGame.PLACING_SETTLEMENT)
                            {
                                ga.cancelBuildSettlement(player.getPlayerNumber());
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, 1));
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, 1));
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, 1));
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, 1));
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You didn't buy a settlement."));
                            }

                            break;

                        case SOCPlayingPiece.CITY:

                            if (ga.getGameState() == SOCGame.PLACING_CITY)
                            {
                                ga.cancelBuildCity(player.getPlayerNumber());
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.ORE, 3));
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, 2));
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You didn't buy a city."));
                            }

                            break;
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "buy card request" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleBUYCARDREQUEST(Connection c, SOCBuyCardRequest mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        SOCPlayer player = ga.getPlayer((String) c.data);

                        if ((ga.getGameState() == SOCGame.PLAY1) && (ga.couldBuyDevCard(player.getPlayerNumber())))
                        {
                            int card = ga.buyDevCard();
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, 1));
                            messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, 1));
                            messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, 1));
                            messageToGame(ga.getName(), new SOCDevCardCount(ga.getName(), ga.getNumDevCards()));
                            messageToPlayer(c, new SOCDevCard(ga.getName(), player.getPlayerNumber(), SOCDevCard.DRAW, card));

                            Vector ex = new Vector(1);
                            ex.addElement(c);
                            messageToGameExcept(ga.getName(), ex, new SOCDevCard(ga.getName(), player.getPlayerNumber(), SOCDevCard.DRAW, SOCDevCardConstants.UNKNOWN));
                            messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, (String) c.data + " bought a development card."));

                            if (ga.getNumDevCards() > 1)
                            {
                                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, "There are " + ga.getNumDevCards() + " cards left."));
                            }
                            else if (ga.getNumDevCards() == 1)
                            {
                                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, "There is 1 card left."));
                            }
                            else
                            {
                                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, "There are no more Development cards."));
                            }

                            //recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            sendGameState(ga);
                        }
                        else
                        {
                            if (ga.getNumDevCards() == 0)
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "There are no more Development cards."));
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't buy a development card now."));
                            }
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "play development card request" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handlePLAYDEVCARDREQUEST(Connection c, SOCPlayDevCardRequest mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        SOCPlayer player = ga.getPlayer((String) c.data);

                        switch (mes.getDevCard())
                        {
                        case SOCDevCardConstants.KNIGHT:

                            if (ga.canPlayKnight(player.getPlayerNumber()))
                            {
                                ga.playKnight();
                                recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                messageToGame(ga.getName(), new SOCDevCard(ga.getName(), player.getPlayerNumber(), SOCDevCard.PLAY, SOCDevCardConstants.KNIGHT));
                                messageToGame(ga.getName(), new SOCSetPlayedDevCard(ga.getName(), player.getPlayerNumber(), true));
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.NUMKNIGHTS, 1));
                                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, player.getName() + " played a Knight card."));
                                broadcastGameStats(ga);
                                //recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't play a Knight card now."));
                            }

                            break;

                        case SOCDevCardConstants.ROADS:

                            if (ga.canPlayRoadBuilding(player.getPlayerNumber()))
                            {
                                ga.playRoadBuilding();
                                recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                messageToGame(ga.getName(), new SOCDevCard(ga.getName(), player.getPlayerNumber(), SOCDevCard.PLAY, SOCDevCardConstants.ROADS));
                                messageToGame(ga.getName(), new SOCSetPlayedDevCard(ga.getName(), player.getPlayerNumber(), true));
                                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, player.getName() + " played a Road Building card."));
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't play a Road Building card now."));
                            }

                            break;

                        case SOCDevCardConstants.DISC:

                            if (ga.canPlayDiscovery(player.getPlayerNumber()))
                            {
                                ga.playDiscovery();
                                recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                messageToGame(ga.getName(), new SOCDevCard(ga.getName(), player.getPlayerNumber(), SOCDevCard.PLAY, SOCDevCardConstants.DISC));
                                messageToGame(ga.getName(), new SOCSetPlayedDevCard(ga.getName(), player.getPlayerNumber(), true));
                                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, player.getName() + " played a Discovery card."));
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't play a Discovery card now."));
                            }

                            break;

                        case SOCDevCardConstants.MONO:

                            if (ga.canPlayMonopoly(player.getPlayerNumber()))
                            {
                                ga.playMonopoly();
                                recordGameEvent(mes, mes.getGame(), mes.toCmd());
                                messageToGame(ga.getName(), new SOCDevCard(ga.getName(), player.getPlayerNumber(), SOCDevCard.PLAY, SOCDevCardConstants.MONO));
                                messageToGame(ga.getName(), new SOCSetPlayedDevCard(ga.getName(), player.getPlayerNumber(), true));
                                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, player.getName() + " played a Monopoly card."));
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't play a Monopoly card now."));
                            }

                            break;
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "discovery pick" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleDISCOVERYPICK(Connection c, SOCDiscoveryPick mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        SOCPlayer player = ga.getPlayer((String) c.data);

                        if (ga.canDoDiscoveryAction(mes.getResources()))
                        {
                            ga.doDiscoveryAction(mes.getResources());
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());

                            String message = (String) c.data + " received ";
                            int cl;
                            int or;
                            int sh;
                            int wh;
                            int wo;
                            cl = mes.getResources().getAmount(SOCResourceConstants.CLAY);
                            or = mes.getResources().getAmount(SOCResourceConstants.ORE);
                            sh = mes.getResources().getAmount(SOCResourceConstants.SHEEP);
                            wh = mes.getResources().getAmount(SOCResourceConstants.WHEAT);
                            wo = mes.getResources().getAmount(SOCResourceConstants.WOOD);

                            if (cl > 0)
                            {
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, cl));
                                message += (cl + " clay");

                                if ((or + sh + wh + wo) > 0)
                                {
                                    message += " and ";
                                }
                            }

                            if (or > 0)
                            {
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.ORE, or));
                                message += (or + " ore");

                                if ((sh + wh + wo) > 0)
                                {
                                    message += " and ";
                                }
                            }

                            if (sh > 0)
                            {
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, sh));
                                message += (sh + " sheep");

                                if ((wh + wo) > 0)
                                {
                                    message += " and ";
                                }
                            }

                            if (wh > 0)
                            {
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, wh));
                                message += (wh + " wheat");

                                if (wo > 0)
                                {
                                    message += " and ";
                                }
                            }

                            if (wo > 0)
                            {
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, wo));
                                message += (wo + " wood");
                            }

                            message += " from the bank.";
                            messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, message));
                            //recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            sendGameState(ga);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "That is not a legal Discovery pick."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "monopoly pick" message
     *
     * @param c     the connection that sent the message
     * @param mes   the messsage
     */
    private void handleMONOPOLYPICK(Connection c, SOCMonopolyPick mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.canDoMonopolyAction())
                        {
                            ga.doMonopolyAction(mes.getResource());
                            recordGameEvent(mes, mes.getGame(), mes.toCmd());

                            String message = (String) c.data + " monopolized ";

                            switch (mes.getResource())
                            {
                            case SOCResourceConstants.CLAY:
                                message += "clay.";

                                break;

                            case SOCResourceConstants.ORE:
                                message += "ore.";

                                break;

                            case SOCResourceConstants.SHEEP:
                                message += "sheep.";

                                break;

                            case SOCResourceConstants.WHEAT:
                                message += "wheat.";

                                break;

                            case SOCResourceConstants.WOOD:
                                message += "wood.";

                                break;
                            }

                            messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, message));

                            /**
                             * just send all the player's resource counts for the
                             * monopolized resource
                             */
                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                /**
                                 * Note: This only works if SOCPlayerElement.CLAY == SOCResourceConstants.CLAY
                                 */
                                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, mes.getResource(), ga.getPlayer(i).getResources().getAmount(mes.getResource())));
                            }

                            //recordGameEvent(mes, mes.getGame(), mes.toCmd());
                            sendGameState(ga);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't do a Monopoly pick now."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "change face" message
     *
     * @param c  the connection
     * @param mes  the message
     */
    private void handleCHANGEFACE(Connection c, SOCChangeFace mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                SOCPlayer player = ga.getPlayer((String) c.data);

                if (player != null)
                {
                    player.setFaceId(mes.getFaceId());
                    messageToGame(mes.getGame(), new SOCChangeFace(mes.getGame(), player.getPlayerNumber(), mes.getFaceId()));
                }
            }
        }
    }

    /**
     * handle "set seat lock" message
     *
     * @param c  the connection
     * @param mes  the message
     */
    private void handleSETSEATLOCK(Connection c, SOCSetSeatLock mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                SOCPlayer player = ga.getPlayer((String) c.data);

                if (player != null)
                {
                    if (mes.getLockState() == true)
                    {
                        ga.lockSeat(mes.getPlayerNumber());
                    }
                    else
                    {
                        ga.unlockSeat(mes.getPlayerNumber());
                    }

                    messageToGame(mes.getGame(), mes);
                }
            }
        }
    }

    /**
     * handle "create account" message
     *
     * @param c  the connection
     * @param mes  the message
     */
    private void handleCREATEACCOUNT(Connection c, SOCCreateAccount mes)
    {
        //
        // check to see if there is an account with
        // the requested nickname
        //
        String userPassword = null;

        try
        {
            userPassword = SOCDBHelper.getUserPassword(mes.getNickname());
        }
        catch (SQLException sqle)
        {
            // Indicates a db problem: don't continue
            c.put(SOCStatusMessage.toCmd("Problem connecting to database, please try again later."));
            return;
        }

        if (userPassword != null)
        {
            c.put(SOCStatusMessage.toCmd("The nickname '" + mes.getNickname() + "' is already in use."));

            return;
        }

        //
        // create the account
        //
        Date currentTime = new Date();

        boolean success = false;

        try
        {
            success = SOCDBHelper.createAccount(mes.getNickname(), c.host(), mes.getPassword(), mes.getEmail(), currentTime.getTime());
        }
        catch (SQLException sqle)
        {
            System.err.println("Error creating account in db.");
        }

        if (success)
        {
            c.put(SOCStatusMessage.toCmd("Account created for '" + mes.getNickname() + "'."));
        }
        else
        {
            c.put(SOCStatusMessage.toCmd("Account not created due to error."));
        }
    }

    /**
     * This player is sitting down at the game
     *
     * @param ga     the game
     * @param c      the connection for the player
     * @param pn     which seat the player is taking
     * @param robot  true if this player is a robot
     */
    private void sitDown(SOCGame ga, Connection c, int pn, boolean robot)
    {
        if ((c != null) && (ga != null))
        {
            ga.takeMonitor();

            try
            {
                ga.addPlayer((String) c.data, pn);
                ga.getPlayer(pn).setRobotFlag(robot);

                /**
                 * if the player can sit, then tell the other clients in the game
                 */
                SOCSitDown sitMessage = new SOCSitDown(ga.getName(), (String) c.data, pn, robot);
                messageToGame(ga.getName(), sitMessage);

                D.ebugPrintln("*** sent SOCSitDown message to game ***");

                //recordGameEvent(sitMessage, ga.getName(), sitMessage.toCmd());

                Vector requests = (Vector) robotJoinRequests.get(ga.getName());

                if (requests != null)
                {
                    /**
                     * if the request list is empty and the game hasn't started yet,
                     * then start the game
                     */
                    if (requests.isEmpty() && (ga.getGameState() < SOCGame.START1A))
                    {
                        startGame(ga);
                    }

                    /**
                     * if the request list is empty, remove the empty list
                     */
                    if (requests.isEmpty())
                    {
                        robotJoinRequests.remove(ga.getName());
                    }
                }

                broadcastGameStats(ga);

                /**
                 * send all the private information
                 */
                SOCResourceSet resources = ga.getPlayer(pn).getResources();
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.CLAY, resources.getAmount(SOCPlayerElement.CLAY)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.ORE, resources.getAmount(SOCPlayerElement.ORE)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.SHEEP, resources.getAmount(SOCPlayerElement.SHEEP)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.WHEAT, resources.getAmount(SOCPlayerElement.WHEAT)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.WOOD, resources.getAmount(SOCPlayerElement.WOOD)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.UNKNOWN, resources.getAmount(SOCPlayerElement.UNKNOWN)));

                SOCDevCardSet devCards = ga.getPlayer(pn).getDevCards();

                /**
                 * remove the unknown cards
                 */
                int i;

                for (i = 0; i < devCards.getTotal(); i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.PLAY, SOCDevCardConstants.UNKNOWN));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.KNIGHT);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.KNIGHT));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.ROADS);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.ROADS));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.DISC);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.DISC));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.MONO);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.MONO));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.CAP);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.CAP));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.LIB);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.LIB));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.UNIV);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.UNIV));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.TEMP);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.TEMP));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.TOW);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.TOW));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.KNIGHT);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.KNIGHT));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.ROADS);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.ROADS));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.DISC);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.DISC));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.MONO);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.MONO));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.CAP);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.CAP));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.LIB);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.LIB));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.UNIV);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.UNIV));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.TEMP);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.TEMP));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.TOW);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.TOW));
                }

                /**
                 * send game state info like requests for discards
                 */
                sendGameState(ga);

                if ((ga.getCurrentDice() == 7) && (ga.getPlayer(pn).getResources().getTotal() > 7))
                {
                    messageToPlayer(c, new SOCDiscardRequest(ga.getName(), ga.getPlayer(pn).getResources().getTotal() / 2));
                }

                /**
                 * send what face this player is using
                 */
                messageToGame(ga.getName(), new SOCChangeFace(ga.getName(), pn, ga.getPlayer(pn).getFaceId()));
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception caught - " + e);
                e.printStackTrace();
            }

            ga.releaseMonitor();
        }
    }

    /**
     * The current player is stealing from another player.
     * Send messages saying what was stolen.
     *
     * @param ga  the game
     * @param pe  the perpetrator
     * @param vi  the the victim
     * @param rsrc  what was stolen
     */
    protected void reportRobbery(SOCGame ga, SOCPlayer pe, SOCPlayer vi, int rsrc)
    {
        if (ga != null)
        {
            String mes1 = "You stole ";
            String mes2 = pe.getName() + " stole ";
            SOCPlayerElement gainRsrc = null;
            SOCPlayerElement loseRsrc = null;
            SOCPlayerElement gainUnknown;
            SOCPlayerElement loseUnknown;

            switch (rsrc)
            {
            case SOCResourceConstants.CLAY:
                mes1 += "a clay ";
                mes2 += "a clay ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, 1);

                break;

            case SOCResourceConstants.ORE:
                mes1 += "an ore ";
                mes2 += "an ore ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.ORE, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, 1);

                break;

            case SOCResourceConstants.SHEEP:
                mes1 += "a sheep ";
                mes2 += "a sheep ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, 1);

                break;

            case SOCResourceConstants.WHEAT:
                mes1 += "a wheat ";
                mes2 += "a wheat ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, 1);

                break;

            case SOCResourceConstants.WOOD:
                mes1 += "a wood ";
                mes2 += "a wood ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, 1);

                break;
            }

            mes1 += ("resource from " + vi.getName() + ".");
            mes2 += "resource from you.";

            Connection peCon = null;
            Connection viCon = null;
            Enumeration conEnum = conns.elements();

            while (conEnum.hasMoreElements())
            {
                Connection con = (Connection) conEnum.nextElement();

                if (pe.getName().equals((String) con.data))
                {
                    peCon = con;
                }
                else if (vi.getName().equals((String) con.data))
                {
                    viCon = con;
                }
            }

            Vector exceptions = new Vector(2);
            exceptions.addElement(peCon);
            exceptions.addElement(viCon);

            /**
             * send the game messages
             */
            messageToPlayer(peCon, gainRsrc);
            messageToPlayer(peCon, loseRsrc);
            messageToPlayer(viCon, gainRsrc);
            messageToPlayer(viCon, loseRsrc);
            gainUnknown = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.UNKNOWN, 1);
            loseUnknown = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.UNKNOWN, 1);
            messageToGameExcept(ga.getName(), exceptions, gainUnknown);
            messageToGameExcept(ga.getName(), exceptions, loseUnknown);

            /**
             * send the text messages
             */
            messageToPlayer(peCon, new SOCGameTextMsg(ga.getName(), SERVERNAME, mes1));
            messageToPlayer(viCon, new SOCGameTextMsg(ga.getName(), SERVERNAME, mes2));
            messageToGameExcept(ga.getName(), exceptions, new SOCGameTextMsg(ga.getName(), SERVERNAME, pe.getName() + " stole a resource from " + vi.getName()));
        }
    }

    /**
     * send the current state of the game with a message
     *
     * @param ga  the game
     */
    protected void sendGameState(SOCGame ga) throws InterruptedException
    {
        if (ga != null)
        {
            messageToGame(ga.getName(), new SOCGameState(ga.getName(), ga.getGameState()));

            SOCPlayer player = null;

            if (ga.getCurrentPlayerNumber() != -1)
            {
                player = ga.getPlayer(ga.getCurrentPlayerNumber());
            }

            switch (ga.getGameState())
            {
            case SOCGame.START1A:
            case SOCGame.START2A:
                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, "It's " + player.getName() + "'s turn to build a settlement."));

                break;

            case SOCGame.START1B:
            case SOCGame.START2B:
                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, "It's " + player.getName() + "'s turn to build a road."));

                break;

            case SOCGame.PLAY:
                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, "It's " + player.getName() + "'s turn to roll the dice."));

                break;

            case SOCGame.WAITING_FOR_DISCARDS:

                int count = 0;
                String message = "error at sendGameState()";
                String[] names = new String[SOCGame.MAXPLAYERS];

                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    if (ga.getPlayer(i).getNeedToDiscard())
                    {
                        names[count] = ga.getPlayer(i).getName();
                        count++;
                    }
                }

                if (count == 1)
                {
                    message = names[0] + " needs to discard.";
                }
                else if (count == 2)
                {
                    message = names[0] + " and " + names[1] + " need to discard.";
                }
                else if (count > 2)
                {
                    message = names[0];

                    for (int i = 1; i < (count - 1); i++)
                    {
                        message += (", " + names[i]);
                    }

                    message += (" and " + names[count - 1] + " need to discard.");
                }

                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, message));

                break;

            case SOCGame.PLACING_ROBBER:
                messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, player.getName() + " will move the robber."));

                break;

            case SOCGame.WAITING_FOR_CHOICE:

                /**
                 * get the choices from the game
                 */
                boolean[] choices = new boolean[SOCGame.MAXPLAYERS];

                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    choices[i] = false;
                }

                Enumeration plEnum = ga.getPossibleVictims().elements();

                while (plEnum.hasMoreElements())
                {
                    SOCPlayer pl = (SOCPlayer) plEnum.nextElement();
                    choices[pl.getPlayerNumber()] = true;
                }

                /**
                 * ask the current player to choose a player to steal from
                 */
                String n = ga.getPlayer(ga.getCurrentPlayerNumber()).getName();
                Enumeration connsEnum = getConnections();

                while (connsEnum.hasMoreElements())
                {
                    Connection con = (Connection) connsEnum.nextElement();

                    if (n.equals((String) con.data))
                    {
                        con.put(SOCChoosePlayerRequest.toCmd(ga.getName(), choices));

                        break;
                    }
                }

                break;

            case SOCGame.OVER:

                SOCPlayer pl;

                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    pl = ga.getPlayer(i);

                    if (pl.getTotalVP() >= 10)
                    {
                        String msg;
                        msg = pl.getName() + " has won the game with " + pl.getTotalVP() + " points.";
                        messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, msg));

                        ///
                        /// send a message saying what VP cards the player has
                        ///
                        SOCDevCardSet devCards = pl.getDevCards();

                        if (devCards.getNumVPCards() > 0)
                        {
                            msg = pl.getName() + " has";

                            int vpCardCount = 0;

                            for (int devCardType = SOCDevCardConstants.CAP;
                                    devCardType < SOCDevCardConstants.UNKNOWN;
                                    devCardType++)
                            {
                                if ((devCards.getAmount(SOCDevCardSet.OLD, devCardType) > 0) || (devCards.getAmount(SOCDevCardSet.NEW, devCardType) > 0))
                                {
                                    if (vpCardCount > 0)
                                    {
                                        if ((devCards.getNumVPCards() - vpCardCount) == 1)
                                        {
                                            msg += " and";
                                        }
                                        else if ((devCards.getNumVPCards() - vpCardCount) > 0)
                                        {
                                            msg += ",";
                                        }
                                    }

                                    vpCardCount++;

                                    switch (devCardType)
                                    {
                                    case SOCDevCardConstants.CAP:
                                        msg += " a Capitol (+1VP)";

                                        break;

                                    case SOCDevCardConstants.LIB:
                                        msg += " a Library (+1VP)";

                                        break;

                                    case SOCDevCardConstants.UNIV:
                                        msg += " a University (+1VP)";

                                        break;

                                    case SOCDevCardConstants.TEMP:
                                        msg += " a Temple (+1VP)";

                                        break;

                                    case SOCDevCardConstants.TOW:
                                        msg += " a Tower (+1VP)";

                                        break;
                                    }
                                }
                            }

                            messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, msg));
                        }

                        break;
                    }
                }
                sleep(2000);
                messageToGame(ga.getName(), new SOCDeleteGame(ga.getName() ));
                sleep(8000);
                System.out.println("Clean exit.");
                System.exit(0);
//                stopServer();
                
                break;
            }
        }
//                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
//                {
//                    SOCPlayer pl = ga.getPlayer(i).;
//                    
//                    if ()
//                }
    }

    /**
     * report a trade that has taken place
     *
     * @param ga        the game
     * @param offering  the number of the player making the offer
     * @param accepting the number of the player accepting the offer
     */
    protected void reportTrade(SOCGame ga, int offering, int accepting)
    {
        if (ga != null)
        {
            SOCTradeOffer offer = ga.getPlayer(offering).getCurrentOffer();
            String message = ga.getPlayer(offering).getName() + " traded ";
            SOCResourceSet rsrcs;
            int cl;
            int or;
            int sh;
            int wh;
            int wo;

            rsrcs = offer.getGiveSet();
            cl = rsrcs.getAmount(SOCResourceConstants.CLAY);
            or = rsrcs.getAmount(SOCResourceConstants.ORE);
            sh = rsrcs.getAmount(SOCResourceConstants.SHEEP);
            wh = rsrcs.getAmount(SOCResourceConstants.WHEAT);
            wo = rsrcs.getAmount(SOCResourceConstants.WOOD);

            if (cl > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, cl));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, cl));
                message += (cl + " clay");

                if ((or + sh + wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (or > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.GAIN, SOCPlayerElement.ORE, or));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.LOSE, SOCPlayerElement.ORE, or));
                message += (or + " ore");

                if ((sh + wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (sh > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, sh));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, sh));
                message += (sh + " sheep");

                if ((wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (wh > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, wh));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, wh));
                message += (wh + " wheat");

                if (wo > 0)
                {
                    message += ",";
                }
            }

            if (wo > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, wo));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, wo));
                message += (wo + " wood");
            }

            message += " for ";
            rsrcs = offer.getGetSet();
            cl = rsrcs.getAmount(SOCResourceConstants.CLAY);
            or = rsrcs.getAmount(SOCResourceConstants.ORE);
            sh = rsrcs.getAmount(SOCResourceConstants.SHEEP);
            wh = rsrcs.getAmount(SOCResourceConstants.WHEAT);
            wo = rsrcs.getAmount(SOCResourceConstants.WOOD);

            if (cl > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, cl));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, cl));
                message += (cl + " clay");

                if ((or + sh + wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (or > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.LOSE, SOCPlayerElement.ORE, or));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.GAIN, SOCPlayerElement.ORE, or));
                message += (or + " ore");

                if ((sh + wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (sh > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, sh));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, sh));
                message += (sh + " sheep");

                if ((wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (wh > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, wh));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, wh));
                message += (wh + " wheat");

                if (wo > 0)
                {
                    message += ",";
                }
            }

            if (wo > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), accepting, SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, wo));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), offering, SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, wo));
                message += (wo + " wood");
            }

            message += (" from " + ga.getPlayer(accepting).getName() + ".");
            messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, message));
        }
    }

    /**
     * report that the current player traded with the bank
     *
     * @param ga        the game
     * @param give      the number of the player making the offer
     * @param get       the number of the player accepting the offer
     */
    protected void reportBankTrade(SOCGame ga, SOCResourceSet give, SOCResourceSet get)
    {
        if (ga != null)
        {
            String message = ga.getPlayer(ga.getCurrentPlayerNumber()).getName() + " traded ";
            int cl;
            int or;
            int sh;
            int wh;
            int wo;

            cl = give.getAmount(SOCResourceConstants.CLAY);
            or = give.getAmount(SOCResourceConstants.ORE);
            sh = give.getAmount(SOCResourceConstants.SHEEP);
            wh = give.getAmount(SOCResourceConstants.WHEAT);
            wo = give.getAmount(SOCResourceConstants.WOOD);

            if (cl > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, cl));
                message += (cl + " clay");

                if ((or + sh + wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (or > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, or));
                message += (or + " ore");

                if ((sh + wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (sh > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, sh));
                message += (sh + " sheep");

                if ((wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (wh > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, wh));
                message += (wh + " wheat");

                if (wo > 0)
                {
                    message += ",";
                }
            }

            if (wo > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, wo));
                message += (wo + " wood");
            }

            message += " for ";

            cl = get.getAmount(SOCResourceConstants.CLAY);
            or = get.getAmount(SOCResourceConstants.ORE);
            sh = get.getAmount(SOCResourceConstants.SHEEP);
            wh = get.getAmount(SOCResourceConstants.WHEAT);
            wo = get.getAmount(SOCResourceConstants.WOOD);

            if (cl > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, cl));
                message += (cl + " clay");

                if ((or + sh + wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (or > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.ORE, or));
                message += (or + " ore");

                if ((sh + wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (sh > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, sh));
                message += (sh + " sheep");

                if ((wh + wo) > 0)
                {
                    message += ",";
                }
            }

            if (wh > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, wh));
                message += (wh + " wheat");

                if (wo > 0)
                {
                    message += ",";
                }
            }

            if (wo > 0)
            {
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), ga.getCurrentPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, wo));
                message += (wo + " wood");
            }

            if ((give.getTotal() / get.getTotal()) == 4)
            {
                message += " from the bank.";
            }
            else
            {
                message += " from a port.";
            }

            messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, message));
        }
    }

    /**
     * make sure it's the player's turn
     *
     * @param c  the connection
     * @param ga the game
     *
     * @return true if it is the player's turn
     */
    protected boolean checkTurn(Connection c, SOCGame ga)
    {
        if ((c != null) && (ga != null))
        {
            try
            {
                if (ga.getCurrentPlayerNumber() != ga.getPlayer((String) c.data).getPlayerNumber())
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            catch (Exception e)
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * do the stuff you need to do to start a game
     *
     * @param ga  the game
     */
    protected void startGame(SOCGame ga) throws InterruptedException
    {
        if (ga != null)
        {
            numberOfGamesStarted++;
            ga.startGame();

            /**
             * send the board layout
             */
            SOCBoardLayout bl = getBoardLayoutMessage(ga);
            messageToGame(ga.getName(), bl);

            /**
             * send the player info
             */
            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                SOCPlayer pl = ga.getPlayer(i);
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.ROADS, pl.getNumPieces(SOCPlayingPiece.ROAD)));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.SETTLEMENTS, pl.getNumPieces(SOCPlayingPiece.SETTLEMENT)));
                messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.CITIES, pl.getNumPieces(SOCPlayingPiece.CITY)));
                messageToGame(ga.getName(), new SOCSetPlayedDevCard(ga.getName(), i, false));
            }

            /**
             * send the number of dev cards
             */
            messageToGame(ga.getName(), new SOCDevCardCount(ga.getName(), ga.getNumDevCards()));

            /**
             * ga.startGame() picks who goes first, but feedback is nice
             */
            messageToGame(ga.getName(), new SOCGameTextMsg(ga.getName(), SERVERNAME, "Randomly picking a starting player..."));

            /**
             * send the game state
             */
            sendGameState(ga);

            /**
             * start the game
             */
            messageToGame(ga.getName(), new SOCStartGame(ga.getName()));

            /**
             * send who's turn it is
             */
            sendTurn(ga);
        }
    }

    /**
     * send who's turn it is
     *
     * @param ga  the game
     */
    private void sendTurn(SOCGame ga)
    {
        if (ga != null)
        {
            messageToGame(ga.getName(), new SOCSetPlayedDevCard(ga.getName(), ga.getCurrentPlayerNumber(), false));

            SOCTurn turnMessage = new SOCTurn(ga.getName(), ga.getCurrentPlayerNumber());
            messageToGame(ga.getName(), turnMessage);
            //recordGameEvent(turnMessage, ga.getName(), turnMessage.toCmd());
        }
    }

    /**
     * put together the SOCBoardLayout message
     *
     * @param  ga   the game
     * @return      a board layout message
     */
    private SOCBoardLayout getBoardLayoutMessage(SOCGame ga)
    {
        SOCBoard board;
        int[] hexes;
        int[] numbers;
        int robber;

        board = ga.getBoard();
        hexes = board.getHexLayout();
        numbers = board.getNumberLayout();
        robber = board.getRobberHex();

        return (new SOCBoardLayout(ga.getName(), hexes, numbers, robber));
    }

    /**
     * create a new game event record
     */
    private void createNewGameEventRecord()
    {
        /*
           currentGameEventRecord = new SOCGameEventRecord();
           currentGameEventRecord.setTimestamp(new Date());
         */
    }

    /**
     * save the current game event record in the game record
     *
     * @param gn  the name of the game
     */
    private void saveCurrentGameEventRecord(String gn)
    {
        /*
           SOCGameRecord gr = (SOCGameRecord)gameRecords.get(gn);
           SOCGameEventRecord ger = currentGameEventRecord.myClone();
           gr.addEvent(ger);
         */
    }

    /**
     * write a gameRecord out to disk
     *
     * @param na  the name of the record
     * @param gr  the game record
     */

    /*
       private void writeGameRecord(String na, SOCGameRecord gr) {
       FileOutputStream os = null;
       ObjectOutput output = null;
    
       try {
       Date theTime = new Date();
       os = new FileOutputStream("dataFiles/"+na+"."+theTime.getTime());
       output = new ObjectOutputStream(os);
       } catch (Exception e) {
       D.ebugPrintln(e.toString());
       D.ebugPrintln("Unable to open output stream.");
       }
       try{
       output.writeObject(gr);
       // D.ebugPrintln("*** Wrote "+na+" out to disk. ***");
       output.close();
       } catch (Exception e) {
       D.ebugPrintln(e.toString());
       D.ebugPrintln("Unable to write game record to disk.");
       }
       }
     */

    /**
     * if all the players stayed for the whole game,
     * record the scores in the database
     *
     * @param ga  the game
     */
    protected void storeGameScores(SOCGame ga)
    {
        if (ga != null)
        {
            //D.ebugPrintln("allOriginalPlayers for "+ga.getName()+" : "+ga.allOriginalPlayers());
            if ((ga.getGameState() == SOCGame.OVER) && (ga.allOriginalPlayers()))
            {
                //if (ga.allOriginalPlayers()) {				
                try
                {
                    SOCDBHelper.saveGameScores(ga.getName(), ga.getPlayer(0).getName(), ga.getPlayer(1).getName(), ga.getPlayer(2).getName(), ga.getPlayer(3).getName(), (short) ga.getPlayer(0).getTotalVP(), (short) ga.getPlayer(1).getTotalVP(), (short) ga.getPlayer(2).getTotalVP(), (short) ga.getPlayer(3).getTotalVP(), ga.getStartTime());
                }
                catch (SQLException sqle)
                {
                    System.err.println("Error saving game scores in db.");
                }
            }
        }
    }
    
//    @Override
//    public void run()
//    {
//        FileWriter fw = null;
//        try {
//            fw = new FileWriter("gamelog.txt");
//            fw.write("test\n");
//            gameDataFiles.put("g0", fw);
//            super.run();
//        } catch (IOException ex) {
//            Logger.getLogger(SOCServer.class.getName()).log(Level.SEVERE, null, ex);
//        } finally {
//            try {
//                fw.close();
//            } catch (IOException ex) {
//                Logger.getLogger(SOCServer.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }

    /**
     * record events that happen during the game
     *
     * @param gameName   the name of the game
     * @param event      the event
     */
    protected void recordGameEvent(SOCMessage mes, String gameName, String event)
    {
           
//        SOCRobotBrain brain = (SOCRobotBrain) loggerClient.robotBrains.get(mes.getGame());
//        brain.recordGameEvent(mes,gameName);
           
        System.out.println("log:  " + event);
        if (loggerClient != null)
            loggerClient.recordGameEvent(mes,gameName,event);
           
//           FileWriter fw = (FileWriter)gameDataFiles.get(gameName);
//           if (fw == null)
//           {
//                try {
//                    fw = new FileWriter(gameName+".txt");
//                    fw.write("test\n");
//                    gameDataFiles.put(gameName, fw);
//                } catch (IOException ex) {
//                    Logger.getLogger(SOCServer.class.getName()).log(Level.SEVERE, null, ex);
//                } finally {
//                    try {
//                        fw.close();
//                    } catch (IOException ex) {
//                        Logger.getLogger(SOCServer.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//               
//           }                      
//           
//           if (fw != null) {
//           try {
//           fw.write(event+"\n");
//           fw.flush();
//           //D.ebugPrintln("WROTE |"+event+"|");
//           } catch (Exception e) {
//           D.ebugPrintln(e.toString());
//           D.ebugPrintln("Unable to write to disk.");
//           }
//           }
         
    }

    /**
     * this is a debugging command that gives resources to a player
     */
    protected void giveResources(String mes, SOCGame game)
    {
        StringTokenizer st = new StringTokenizer(mes.substring(6));
        int[] resources = new int[SOCResourceConstants.WOOD + 1];
        int resourceType = SOCResourceConstants.CLAY;
        String name = "";

        while (st.hasMoreTokens())
        {
            String token = st.nextToken();

            if (resourceType <= SOCResourceConstants.WOOD)
            {
                resources[resourceType] = Integer.parseInt(token);
                resourceType++;
            }
            else
            {
                name = token;

                break;
            }
        }

        SOCResourceSet rset = game.getPlayer(name).getResources();
        int pnum = game.getPlayer(name).getPlayerNumber();
        String outMes = "### " + name + " got";

        for (resourceType = SOCResourceConstants.CLAY;
                resourceType <= SOCResourceConstants.WOOD; resourceType++)
        {
            rset.add(resources[resourceType], resourceType);
            outMes += (" " + resources[resourceType]);

            switch (resourceType)
            {
            case SOCResourceConstants.CLAY:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, resources[resourceType]));

                break;

            case SOCResourceConstants.ORE:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.ORE, resources[resourceType]));

                break;

            case SOCResourceConstants.SHEEP:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, resources[resourceType]));

                break;

            case SOCResourceConstants.WHEAT:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, resources[resourceType]));

                break;

            case SOCResourceConstants.WOOD:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, resources[resourceType]));

                break;
            }
        }

        messageToGame(game.getName(), new SOCGameTextMsg(game.getName(), SERVERNAME, outMes));
    }

    /**
     * this broadcasts game information to all people connected
     * used to display the scores on the player client
     */
    protected void broadcastGameStats(SOCGame ga)
    {
        /*
           if (ga != null) {
           int scores[] = new int[SOCGame.MAXPLAYERS];
           boolean robots[] = new boolean[SOCGame.MAXPLAYERS];
           for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
           SOCPlayer player = ga.getPlayer(i);
           if (player != null) {
           if (ga.isSeatVacant(i)) {
           scores[i] = -1;
           robots[i] = false;
           } else {
           scores[i] = player.getPublicVP();
           robots[i] = player.isRobot();
           }
           } else {
           scores[i] = -1;
           }
           }
        
           broadcast(SOCGameStats.toCmd(ga.getName(), scores, robots));
           }
         */
    }

    /**
     * check for games that have expired and destroy them
     * if games are about to expire, send a warning
     */
    public void checkForExpiredGames()
    {
        Vector expired = new Vector();

        gameList.takeMonitor();

        try
        {
            for (Enumeration k = gameList.getGames(); k.hasMoreElements();)
            {
                String gameName = (String) k.nextElement();
                SOCGame gameData = gameList.getGameData(gameName);

                if (gameData.getExpiration() <= System.currentTimeMillis())
                {
                    expired.addElement(gameName);
                    messageToGame(gameName, new SOCGameTextMsg(gameName, SERVERNAME, ">>> The time limit on this game has expired and will now be destroyed."));
                }
                else
                //
                //  Give people a 5 minute warning
                //
                if ((gameData.getExpiration() - 300000) <= System.currentTimeMillis())
                {
                    gameData.setExpiration(System.currentTimeMillis() + 300000);
                    messageToGame(gameName, new SOCGameTextMsg(gameName, SERVERNAME, ">>> Less than 5 minutes remaining.  Type *ADDTIME* to extend this game another 30 minutes."));
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in checkForExpiredGames - " + e);
        }

        gameList.releaseMonitor();

        //
        // destroy the expired games
        //
        for (Enumeration ex = expired.elements(); ex.hasMoreElements();)
        {
            String ga = (String) ex.nextElement();
            gameList.takeMonitor();

            try
            {
                destroyGame(ga);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in checkForExpired - " + e);
            }

            gameList.releaseMonitor();
            broadcast(SOCDeleteGame.toCmd(ga));
        }
    }

    /** this is a debugging command that gives a dev card to a player
     */
    protected void giveDevCard(String mes, SOCGame game)
    {
        StringTokenizer st = new StringTokenizer(mes.substring(5));
        String name = "";
        int cardType = -1;

        while (st.hasMoreTokens())
        {
            String token = st.nextToken();

            if (cardType < 0)
            {
                cardType = Integer.parseInt(token);
            }
            else
            {
                name = token;

                break;
            }
        }

        SOCDevCardSet dcSet = game.getPlayer(name).getDevCards();
        dcSet.add(1, SOCDevCardSet.NEW, cardType);

        int pnum = game.getPlayer(name).getPlayerNumber();
        String outMes = "### " + name + " got a " + cardType + " card.";
        messageToGame(game.getName(), new SOCDevCard(game.getName(), pnum, SOCDevCard.DRAW, cardType));
        messageToGame(game.getName(), new SOCGameTextMsg(game.getName(), SERVERNAME, outMes));
    }

    public void setLoggerClient(SSRobotClient cl)
    {
        loggerClient = cl;
    }
    
    /**
     * Starting the server from the command line
     *
     * @param args  arguments: port number
     */
    static public void main(String[] args)
    {
        int port;
        int mc;

        if (args.length < 4)
        {
            System.err.println("usage: java soc.server.SOCServer port_number max_connections dbUser dbPass");

            return;
        }

        try
        {
            port = Integer.parseInt(args[0]);
            mc = Integer.parseInt(args[1]);
        }
        catch (Exception e)
        {
            System.err.println("usage: java soc.server.SOCServer port_number max_connections dbUser dbPass");

            return;
        }

        SOCServer server = new SOCServer(port, mc, args[2], args[3]);
        server.setPriority(5);
        server.start();
    }
}
