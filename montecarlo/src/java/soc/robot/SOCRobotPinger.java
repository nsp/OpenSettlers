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
package soc.robot;

import soc.message.SOCGameTextMsg;

import soc.util.CappedQueue;
import soc.util.CutoffExceededException;


/**
 * Pings the robots so that they can have a sense of time
 *
 * @author Robert S Thomas
 */
public class SOCRobotPinger extends Thread
{
    CappedQueue messageQueue;
    SOCGameTextMsg ping;
    boolean alive;

    /**
     * Create a robot pinger
     *
     * @param q  the robot brain's message queue
     */
    public SOCRobotPinger(CappedQueue q)
    {
        messageQueue = q;
        ping = new SOCGameTextMsg("*PING*", "*PING*", "*PING*");
        alive = true;
    }

    /**
     * DOCUMENT ME!
     */
    public void run()
    {
        while (alive)
        {
            try
            {
                messageQueue.put(ping);
            }
            catch (CutoffExceededException exc)
            {
                alive = false;
            }

            yield();

            try
            {
                sleep(1000);
            }
            catch (InterruptedException exc) {}
        }

        messageQueue = null;
        ping = null;
    }

    /**
     * DOCUMENT ME!
     */
    public void stopPinger()
    {
        alive = false;
    }
}
