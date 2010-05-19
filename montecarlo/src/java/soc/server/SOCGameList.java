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

import soc.disableDebug.D;

import soc.game.SOCGame;

import soc.server.genericServer.Connection;

import soc.util.MutexFlag;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * A class for tracking the games
 *
 * @author Robert S. Thomas
 */
public class SOCGameList
{
    protected Hashtable gameMutexes;
    protected Hashtable gameMembers;
    protected Hashtable gameData;
    protected boolean inUse;

    /**
     * constructor
     */
    public SOCGameList()
    {
        gameMutexes = new Hashtable();
        gameMembers = new Hashtable();
        gameData = new Hashtable();
        inUse = false;
    }

    /**
     * take the monitor for this game list
     */
    public synchronized void takeMonitor()
    {
        D.ebugPrintln("SOCGameList : TAKE MONITOR");

        while (inUse)
        {
            try
            {
                wait(1000);
            }
            catch (InterruptedException e)
            {
                System.out.println("EXCEPTION IN takeMonitor() -- " + e);
            }
        }

        inUse = true;
    }

    /**
     * release the monitor for this game list
     */
    public synchronized void releaseMonitor()
    {
        D.ebugPrintln("SOCGameList : RELEASE MONITOR");
        inUse = false;
        this.notify();
    }

    /**
     * take the monitor for this game
     *
     * @param game  the name of the game
     * @return false if the game has no mutex
     */
    public boolean takeMonitorForGame(String game)
    {
        D.ebugPrintln("SOCGameList : TAKE MONITOR FOR " + game);

        MutexFlag mutex = (MutexFlag) gameMutexes.get(game);

        if (mutex == null)
        {
            return false;
        }

        boolean done = false;

        while (!done)
        {
            mutex = (MutexFlag) gameMutexes.get(game);

            if (mutex == null)
            {
                return false;
            }

            synchronized (mutex)
            {
                if (mutex.getState() == true)
                {
                    try
                    {
                        mutex.wait(1000);
                    }
                    catch (InterruptedException e)
                    {
                        System.out.println("EXCEPTION IN takeMonitor() -- " + e);
                    }
                }
                else
                {
                    done = true;
                }
            }
        }

        mutex.setState(true);

        return true;
    }

    /**
     * release the monitor for this game
     *
     * @param game  the name of the game
     * @return false if the game has no mutex
     */
    public boolean releaseMonitorForGame(String game)
    {
        D.ebugPrintln("SOCGameList : RELEASE MONITOR FOR " + game);

        MutexFlag mutex = (MutexFlag) gameMutexes.get(game);

        if (mutex == null)
        {
            return false;
        }

        synchronized (mutex)
        {
            mutex.setState(false);
            mutex.notify();
        }

        return true;
    }

    /**
     * @return an enumeration of game names
     */
    public Enumeration getGames()
    {
        return gameMembers.keys();
    }

    /**
     * @param   gaName  the name of the game
     * @return true if the channel exists and has an empty member list
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
     * @param   gaName  game name
     * @return  list of members
     */
    public synchronized Vector getMembers(String gaName)
    {
        return (Vector) gameMembers.get(gaName);
    }

    /**
     * @param   gaName  game name
     * @return the game data
     */
    public SOCGame getGameData(String gaName)
    {
        return (SOCGame) gameData.get(gaName);
    }

    /**
     * @param  gaName   the name of the game
     * @param  conn     the member's connection
     * @return true if memName is a member of the game
     */
    public synchronized boolean isMember(Connection conn, String gaName)
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
     * add a member to the game
     *
     * @param  gaName   the name of the game
     * @param  conn     the member's connection
     */
    public synchronized void addMember(Connection conn, String gaName)
    {
        Vector members = getMembers(gaName);

        if ((members != null) && (!members.contains(conn)))
        {
            members.addElement(conn);
        }
    }

    /**
     * remove member from the game
     *
     * @param  gaName   the name of the game
     * @param  conn     the member's connection
     */
    public synchronized void removeMember(Connection conn, String gaName)
    {
        Vector members = getMembers(gaName);

        if ((members != null))
        {
            members.removeElement(conn);
        }
    }

    /**
     * @param   gaName  the name of the game
     * @return true if the game exists
     */
    public boolean isGame(String gaName)
    {
        return (gameMembers.get(gaName) != null);
    }

    /**
     * create a new game
     *
     * @param gaName  the name of the game
     */
    public synchronized void createGame(String gaName)
    {
        if (!isGame(gaName))
        {
            MutexFlag mutex = new MutexFlag();
            gameMutexes.put(gaName, mutex);

            Vector members = new Vector();
            gameMembers.put(gaName, members);

            SOCGame game = new SOCGame(gaName);

            // set the expiration to 90 min. from now
            game.setExpiration(game.getStartTime().getTime() + 5400000);
            gameData.put(gaName, game);
        }
    }

    /**
     * remove the game from the list
     *
     * @param gaName  the name of the game
     */
    public synchronized void deleteGame(String gaName)
    {
        D.ebugPrintln("SOCGameList : deleteGame(" + gaName + ")");

        SOCGame game = (SOCGame) gameData.get(gaName);

        if (game != null)
        {
            game.destroyGame();
        }

        Vector members = (Vector) gameMembers.get(gaName);

        if (members != null)
        {
            members.removeAllElements();
        }

        MutexFlag mutex = (MutexFlag) gameMutexes.get(gaName);
        gameMutexes.remove(gaName);
        gameMembers.remove(gaName);
        gameData.remove(gaName);

        if (mutex != null)
        {
            synchronized (mutex)
            {
                mutex.notifyAll();
            }
        }
    }
}
