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
package soc.server.genericServer;

import soc.disableDebug.D;

import java.io.IOException;
import java.io.Serializable;

import java.net.ServerSocket;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;


/** a general purpose server
 *  @version 1.6
 *  Original author: <A HREF="http://www.nada.kth.se/~cristi">Cristian Bogdan</A>
 *  Lots of mods by Robert S. Thomas and Jay Budzik
 *  This is the real stuff. Server subclasses won't have to care about
 *  reading/writing on the net, data consistency among threads, etc.
 */
public abstract class Server extends Thread implements Serializable, Cloneable
{
    protected Exception error = null;
    
    private int port;
    private ServerSocket ss;
    private boolean connected = false;

    /** total number of connections made */
    private int connectionsMade = 0;

    /** the connections */
    private Vector conns = new Vector();
    private Vector inQueue = new Vector();

    /** Updated when logging time-stamps are needed (in synchronized blocks) */
    private Date date = new Date();
    
    /**
     * Create a server to listen on the specified port.
     */
    public Server(int port)
    {
        this.port = port;
    }

    /** This is dangerous in a threaded environment like this! */
    protected Enumeration getConnections()
    {
        return conns.elements();
    }

    public int connectionCount()
    {
        return conns.size();
    }

    public int connectionsMade()
    {
        return connectionsMade;
    }

    public int getPort()
    {
        return port;
    }

    public void run()
    {
    	connected = true;

        new Treater().start();

        // keep listening on socket, accepting connections until listening
        // fails, or the server is explicitly stopped.  When accepting a
        // connection causes error, close the socket and reopen another.
        while (connected)
        {
            try
            {
                ss = new ServerSocket(port);

                try
                {
                    while (connected)
                    {
                        // could limit the number of accepted connections here
                        addConnection(new Connection(ss.accept(), this));
                    }
                }
                catch (IOException e)
                {
                    error = e;
                    D.ebugPrintln("Exception " + e + " during accept");
                }

                ss.close();
            }
            catch (IOException e)
            {
                System.err.println("Could not listen to port " + port + ": " + e);
                connected = false;
                error = e;
            }
        }
    }

    /**
     * Implemented in subclasses to take action on an incoming command.
     *
     * @param str the command read from the connection
     * @param con the connection
     */
    abstract protected void processCommand(String str, Connection con);

    /** Placeholder for doing things when server shuts down */
    protected void serverDown() {}

    /** Placeholder for doing things when a new connection comes */
    protected void newConnection(Connection c) {}

    /** Placeholder for doing things when a connection is closed */
    protected void leaveConnection(Connection c) {}

    /**
     * Treat a request from the given connection. Called only by {@link
     * Connection}.
     */
    protected void treat(String s, Connection c)
    {
        synchronized (inQueue)
        {
            inQueue.addElement(new Command(s, c));
            inQueue.notify();
        }
    }

    /**
     * Disconnect all the connections, and stop accepting any more.
     */
    public final synchronized void stopServer()
    {
        connected = false;
        serverDown();

        for (Enumeration e = conns.elements(); e.hasMoreElements();)
        {
            ((Connection) e.nextElement()).disconnect();
        }

        conns.removeAllElements();
    }

    /**
     * Broadcast a message to all active clients.
     */
    protected final synchronized void broadcast(String m)
    {
        for (Enumeration e = getConnections(); e.hasMoreElements();)
        {
            ((Connection) e.nextElement()).put(m);
        }
    }

    /**
     * Remove a connection from the system.
     */
    protected final synchronized void removeConnection(Connection c)
    {
        if (conns.removeElement(c))
        {
            c.disconnect();
            leaveConnection(c);
            if (D.ebugOn)
            {
                date.setTime(System.currentTimeMillis());
                D.ebugPrintln(c.host() + " left (" + connectionCount() + ")  " + date
                              + ((c.error != null) ? (": " + c.error) : ""));
            }
        }
    }

    /**
     * Add a connection to the system.
     */
    private synchronized void addConnection(Connection c)
    {
        if (c.connect())
        {
            connectionsMade++;
            conns.addElement(c);
            newConnection(c);
            if (D.ebugOn)
            {
                date.setTime(System.currentTimeMillis());
                D.ebugPrintln(c.host() + " came (" + connectionCount() + ")  " + date);
            }
        }
    }

    /**
     * Wraper for a command and the connection to send it out on.
     */
    private class Command
    {
        public String str;
        public Connection con;

        public Command(String s, Connection c)
        {
            str = s;
            con = c;
        }
    }

    /**
     * Thread to dequeue commands (queued by the Connection) and process them.
     */
    private class Treater extends Thread
    {
        public Treater()
        {
            // must use 'this', or jre1.5 linux throws Thread init error
            this.setDaemon(true);
        }

        public void run()
        {
            while (connected)
            {
                Command command = null;

                synchronized (inQueue)
                {
                    while (inQueue.size() == 0 && connected)
                    {
                        try
                        {
                            inQueue.wait();
                        }
                        catch (InterruptedException x) //what might cause this?
                        {
                            x.printStackTrace();
                        }
                    }
                    
                    if (connected)
                    {
                        command = (Command) inQueue.elementAt(0);
                        inQueue.removeElementAt(0);
                    }
                }

                if (command != null)
                {
                    try
                    {
                        processCommand(command.str, command.con);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
