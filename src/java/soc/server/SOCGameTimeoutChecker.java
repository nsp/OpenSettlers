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
package soc.server;


/**
 * Wakes up every 5 minutes to check for games that have expired,
 * by calling {@link SOCServer#checkForExpiredGames()}.
 *
 * @author Robert S Thomas
 */
public class SOCGameTimeoutChecker extends Thread
{
    SOCServer server;
    boolean alive;

    /**
     * Create a game timeout checker
     *
     * @param srv  the game server
     */
    public SOCGameTimeoutChecker(SOCServer srv)
    {
        server = srv;
        alive = true;
        setName ("timeoutChecker");  // Thread name for debug
        try { setDaemon(true); } catch (Exception e) {}  // Don't wait on us to exit program
    }

    /**
     * Wakes up every 5 minutes to check for games that have expired
     */
    public void run()
    {
        while (alive)
        {
            server.checkForExpiredGames();
            yield();

            try
            {
                // check every 5 minutes; must be at most half of SOCServer.GAME_EXPIRE_WARN_MINUTES
                sleep(300000);
            }
            catch (InterruptedException exc) {}
        }

        server = null;
    }

    /**
     * DOCUMENT ME!
     */
    public void stopChecking()
    {
        alive = false;
    }
}
