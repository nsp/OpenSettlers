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

import soc.disableDebug.D;

import soc.message.ServerPing;

import soc.server.genericServer.StringConnection;

import java.util.Enumeration;
import java.util.Vector;


/**
 * Pings the robots so that they know that they're connected
 * to the server
 *
 * @author Robert S Thomas
 */
public class ServerRobotPinger extends Thread
{
    Vector robotConnections;
    int sleepTime = 150000;
    ServerPing ping;
    boolean alive;

    /**
     * Create a server robot pinger
     *
     * @param robots  the connections to robots
     */
    public ServerRobotPinger(Vector robots)
    {
        robotConnections = robots;
        ping = new ServerPing(sleepTime);
        alive = true;
        setName ("robotPinger-srv");  // Thread name for debug
    }

    /**
     * DOCUMENT ME!
     */
    public void run()
    {
        while (alive)
        {
            if (!robotConnections.isEmpty())
            {
                Enumeration robotConnectionsEnum = robotConnections.elements();

                while (robotConnectionsEnum.hasMoreElements())
                {
                    StringConnection robotConnection = (StringConnection) robotConnectionsEnum.nextElement();
                    D.ebugPrintln("(*)(*)(*)(*) PINGING " + robotConnection.getData());
                    robotConnection.put(ping.toCmd());
                }
            }

            yield();

            try
            {
                sleep(sleepTime - 60000);
            }
            catch (InterruptedException exc) {}
        }

        //
        //  cleanup
        //
        robotConnections = null;
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
