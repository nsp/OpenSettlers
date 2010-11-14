/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * This file Copyright (C) 2009-2010 Jeremy D Monin <jeremy@nand.net>
 * Portions of this file Copyright (C) 2003 Robert S. Thomas
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
package soc.server;

import soc.debug.D;
import soc.game.Game;
import soc.game.GameOption;
import soc.server.genericServer.StringConnection;
import soc.util.GameBoardReset;
import soc.util.GameList;
import soc.util.Version;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * A class for creating and tracking the games;
 * contains each game's name, {@link GameOption game options},
 * {@link Game} object, and clients ({@link StringConnection}s).
 *<P>
 * In 1.1.07, parent class GameList was refactored, with
 * some methods moved to this new subclass, such as {@link #createGame(String, Hashtable) createGame}.
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 * @since 1.1.07
 */
public class GameListAtServer extends GameList
{
    /**
     * Number of minutes after which a game (created on the list) is expired.
     * Default is 90.
     *
     * @see #createGame(String, Hashtable)
     */
    public static int GAME_EXPIRE_MINUTES = 90;

    /** map of game names to Vector of game members ({@link StringConnection}s) */
    protected Hashtable gameMembers;

    /**
     * constructor
     */
    public GameListAtServer()
    {
        super();
        gameMembers = new Hashtable();
    }

    /**
     * does the game have no members?
     * @param   gaName  the name of the game
     * @return true if the game exists and has an empty member list
     */
    public synchronized boolean isGameEmpty(String gaName)
    {
        boolean result;
        Vector members;

        members = (Vector) gameMembers.get(gaName);

        if ((members != null) && (members.isEmpty()))
        {
            result = true;
        }
        else
        {
            result = false;
        }

        return result;
    }

    /**
     * get a game's members (client connections)
     * @param   gaName  game name
     * @return  list of members: a Vector of {@link StringConnection}s
     */
    public synchronized Vector getMembers(String gaName)
    {
        return (Vector) gameMembers.get(gaName);
    }

    /**
     * is this connection a member of the game?
     * @param  gaName   the name of the game
     * @param  conn     the member's connection
     * @return true if memName is a member of the game
     */
    public synchronized boolean isMember(StringConnection conn, String gaName)
    {
        Vector members = getMembers(gaName);

        if ((members != null) && (members.contains(conn)))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * add a member to the game.
     * Also checks client's version against game's current range of client versions.
     * Please call {@link #takeMonitorForGame(String)} before calling this.
     *
     * @param  gaName   the name of the game
     * @param  conn     the member's connection; version should already be set
     */
    public synchronized void addMember(StringConnection conn, String gaName)
    {
        Vector members = getMembers(gaName);

        if ((members != null) && (!members.contains(conn)))
        {
            System.err.println("L139: game " + gaName + " add " + conn);  // JM TEMP
            final boolean firstMember = members.isEmpty();
            members.addElement(conn);

            // Check version range
            Game ga = getGameData(gaName);
            final int cliVers = conn.getVersion();
            if (firstMember)
            {
                ga.clientVersionLowest = cliVers;
                ga.clientVersionHighest = cliVers;
                ga.hasOldClients = (cliVers < Version.versionNumber());
            }
            else 
            {    
                final int cliLowestAlready  = ga.clientVersionLowest;
                final int cliHighestAlready = ga.clientVersionHighest;
                if (cliVers < cliLowestAlready)
                {
                    ga.clientVersionLowest = cliVers;
                    if (cliVers < Version.versionNumber())
                        ga.hasOldClients = true;
                }
                if (cliVers > cliHighestAlready)
                {
                    ga.clientVersionHighest = cliVers;
                }
            }
        }
    }

    /**
     * remove member from the game.
     * Also updates game's client version range, with remaining connected members.
     * Please call {@link #takeMonitorForGame(String)} before calling this.
     *
     * @param  gaName   the name of the game
     * @param  conn     the member's connection
     */
    public synchronized void removeMember(StringConnection conn, String gaName)
    {
        System.err.println("L139: game " + gaName + " remove " + conn);  // JM TEMP
        Vector members = getMembers(gaName);

        if ((members != null))
        {
            members.removeElement(conn);

            // Check version of remaining members
            if (! members.isEmpty())
            {
                StringConnection c = (StringConnection) members.firstElement();
                int lowVers = c.getVersion();
                int highVers = lowVers;
                for (int i = members.size() - 1; i > 1; --i)
                {
                    c = (StringConnection) members.elementAt(i);
                    int v = c.getVersion();
                    if (v < lowVers)
                        lowVers = v;
                    if (v > highVers)
                        highVers = v;
                }
                Game ga = getGameData(gaName);
                ga.clientVersionLowest  = lowVers;
                ga.clientVersionHighest = highVers;
                ga.hasOldClients = (lowVers < Version.versionNumber());
            }
        }
    }

    /**
     * Replace member from all games, with a new connection with same name (after a network problem).
     *
     * @param  oldConn  the member's old connection
     * @param  oldConn  the member's new connection
     * @throws IllegalArgumentException  if oldConn's keyname (via {@link StringConnection#getData() getData()})
     *            differs from newConn's keyname
     *
     * @see #memberGames(StringConnection, String)
     * @since 1.1.08
     */
    public synchronized void replaceMemberAllGames(StringConnection oldConn, StringConnection newConn)
        throws IllegalArgumentException
    {
        if (! oldConn.getData().equals(newConn.getData()))
            throw new IllegalArgumentException("keyname data");

        System.err.println("L212: replaceMemberAllGames(" + oldConn + ", " + newConn + ")");  // JM TEMP
        final boolean sameVersion = (oldConn.getVersion() == newConn.getVersion()); 
        Enumeration allGa = getGames();
        while (allGa.hasMoreElements())
        {
            final String gaName = (String) allGa.nextElement();
            Vector members = (Vector) gameMembers.get(gaName);
            if ((members != null) && members.contains(oldConn))
            {
                System.err.println("L221: for game " + gaName + ":");  // JM TEMP
                if (sameVersion)
                {
                    if (members.remove(oldConn))
                        System.err.println("   OK");
                    else
                        System.err.println("   ** not found");
                    members.addElement(newConn);
                } else {
                    removeMember(oldConn, gaName);
                    addMember(newConn, gaName);
                }
            }
        }
    }

    /**
     * create a new game, and add to the list; game will expire in {@link #GAME_EXPIRE_MINUTES} minutes.
     * If a game already exists (per {@link #isGame(String)}), do nothing.
     *
     * @param gaName  the name of the game
     * @param gaOpts  if game has options, hashtable of {@link GameOption}; otherwise null.
     *                Should already be validated, by calling
     *                {@link GameOption#adjustOptionsToKnown(Hashtable, Hashtable)}.
     * @return new game object, or null if it already existed
     */
    public synchronized Game createGame(final String gaName, Hashtable gaOpts)
    {
        if (isGame(gaName))
            return null;

        Vector members = new Vector();
        gameMembers.put(gaName, members);

        Game game = new Game(gaName, gaOpts);

        // set the expiration to 90 min. from now
        game.setExpiration(game.getStartTime().getTime() + (60 * 1000 * GAME_EXPIRE_MINUTES));

        gameInfo.put(gaName, new GameInfo(true, game.getGameOptions()));  // also creates MutexFlag
        gameData.put(gaName, game);

        return game;
    }

    /**
     * Reset the board of this game, create a new game of same name,
     * same players, new layout.  The new "reset" board takes the place
     * of the old game in the game list.
     *<P>
     * Robots are not copied and
     * must re-join the game. (They're removed from the list of game members.)
     * If the game had robots, they must leave the old game before any players can
     * join the new game; the new game's {@link Game#boardResetOngoingInfo} field
     * is set to the object returned by this method, and its gameState will be
     * {@link Game#READY_RESET_WAIT_ROBOT_DISMISS} instead of {@link Game#NEW}.
     *<P>
     * <b>Locking:</b>
     * Takes game monitor.
     * Copies old game.
     * Adds reset-copy to gamelist.
     * Destroys old game.
     * Releases game monitor.
     *
     * @param gaName Name of game - If not found, do nothing. No monitor is taken.
     * @return New game if gaName was found and copied; null if no game called gaName,
     *         or if a problem occurs during reset
     * @see soc.game.Game#resetAsCopy()
     */
    public GameBoardReset resetBoard(String gaName)
    {
        Game oldGame = (Game) gameData.get(gaName);
        if (oldGame == null)
            return null;

        takeMonitorForGame(gaName);

        // Create reset-copy of game;
        // also removes robots from game obj and its member list,
        // and sets boardResetOngoingInfo field/gamestate if there are robots.
        GameBoardReset reset = null;
        try
        {
            reset = new GameBoardReset(oldGame, getMembers(gaName));
            Game rgame = reset.newGame;

            // As in createGame, set expiration timer to 90 min. from now
            rgame.setExpiration(new Date().getTime() + (60 * 1000 * GAME_EXPIRE_MINUTES));

            // Adjust game-list
            gameData.remove(gaName);
            gameData.put(gaName, rgame);

            // Done.
            oldGame.destroyGame();
        }
        catch (Throwable e)
        {
            D.ebugPrintStackTrace(e, "ERROR -> gamelist.resetBoard");
        }
        finally
        {
            releaseMonitorForGame(gaName);
        }

        return reset;  // null if error during reset
    }

    /**
     * remove the game from the list
     *
     * @param gaName  the name of the game
     */
    public synchronized void deleteGame(String gaName)
    {
        Vector members = (Vector) gameMembers.get(gaName);
        if (members != null)
        {
            members.removeAllElements();
        }        
        super.deleteGame(gaName);
    }

    /**
     * For the games this player is in, what's the
     * minimum required client version?
     * Checks {@link Game#getClientVersionMinRequired()}.
     *<P>
     * This method helps determine if a client's connection can be
     * "taken over" after a network problem.  It synchronizes on <tt>gameData</tt>.
     *
     * @param  plConn   the previous connection of the player, which might be taken over
     * @return Minimum version, in same format as {@link Game#getClientVersionMinRequired()},
     *         or 0 if player isn't in any games.
     * @since 1.1.08
     */
    public int playerGamesMinVersion(StringConnection plConn)
    {
        int minVers = 0;

        synchronized(gameData)
        {
            Enumeration gdEnum = getGamesData();
            while (gdEnum.hasMoreElements())
            {
                Game ga = (Game) gdEnum.nextElement();
                Vector members = getMembers(ga.getName());
                if ((members == null) || ! members.contains(plConn))
                    continue;

                // plConn is a member of this game.
                int vers = ga.getClientVersionMinRequired();
                if (vers > minVers)
                    minVers = vers;
            }
        }

        return minVers;
    }

    /**
     * List of games containing this member.
     *
     * @param c  Connection 
     * @param firstGameName  Game name that should be first element of list
     *           (if <tt>newConn</tt> is a member of it), or null.
     * @return The games, in no particular order (past firstGameName),
     *           or a 0-length Vector, if member isn't in any game.
     *
     * @see #replaceMemberAllGames(StringConnection, StringConnection)
     * @since 1.1.08
     */
    public Vector memberGames(StringConnection c, final String firstGameName)
    {
        Vector cGames = new Vector();

        synchronized(gameData)
        {
            Game firstGame = null;
            if (firstGameName != null)
            {
                firstGame = getGameData(firstGameName);
                if (firstGame != null)
                {
                    Vector members = getMembers(firstGameName);
                    if ((members != null) && members.contains(c))
                        cGames.addElement(firstGame);
                }
            }

            Enumeration gdEnum = getGamesData();
            while (gdEnum.hasMoreElements())
            {
                Game ga = (Game) gdEnum.nextElement();
                if (ga == firstGame)
                    continue;
                Vector members = getMembers(ga.getName());
                if ((members == null) || ! members.contains(c))
                    continue;

                cGames.addElement(ga);
            }
        }

        return cGames;
    }

}