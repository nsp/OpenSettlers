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
 *  @version 1.5
 *  Original author: <A HREF="http://www.nada.kth.se/~cristi">Cristian Bogdan</A>
 *  Lots of mods by Robert S. Thomas and Jay Budzik
 *  This is the real stuff. Server subclasses won't have to care about
 *  reading/writing on the net, data consistency among threads, etc.
 */
public abstract class Server extends Thread implements Serializable, Cloneable
{
    ServerSocket ss;
    boolean up = false;
    protected Exception error = null;
    protected int port;

    /**
     * total number of connections made
     */
    protected int numberOfConnections;

    /** the connections */
    protected Vector conns = new Vector();
    public Vector inQueue = new Vector();

    /** start listening to the given port */
    public Server(int port)
    {
        this.port = port;
        numberOfConnections = 0;

        try
        {
            ss = new ServerSocket(port);
        }
        catch (IOException e)
        {
            System.err.println("Could not listen to port " + port + ": " + e);
            error = e;
        }
    }

    protected Enumeration getConnections()
    {
        return conns.elements();
    }

    protected synchronized int connectionCount()
    {
        return conns.size();
    }

    protected synchronized boolean isUp()
    {
        return up;
    }

    /** run method for Server */
    public void run()
    {
        Treater treater = new Treater(this);
        treater.start();

        if (error != null)
        {
            return;
        }

        up = true;

        while (isUp())
        {
            try
            {
                while (isUp())
                {
                    // we could limit the number of accepted connections here
                    Connection con = new Connection(ss.accept(), this);
                    con.start();

                    //addConnection(new Connection());
                }
            }
            catch (IOException e)
            {
                error = e;
                D.ebugPrintln("Exception " + e + " during accept");

                //System.out.println("STOPPING SERVER");
                //stopServer();
            }

            try
            {
                ss.close();
                ss = new ServerSocket(port);
            }
            catch (IOException e)
            {
                System.err.println("Could not listen to port " + port + ": " + e);
                up = false;
                error = e;
            }
        }
    }

    /** treat a request from the given connection */
    public void treat(String s, Connection c)
    {
        synchronized (inQueue)
        {
            inQueue.addElement(new Command(s, c));
            inQueue.notify();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param str DOCUMENT ME!
     * @param con DOCUMENT ME!
     */
    abstract public void processCommand(String str, Connection con);

    /** placeholder for doing things when server gets down */
    protected void serverDown() {}

    /** placeholder for doing things when a new connection comes */
    protected void newConnection(Connection c) {}

    /** placeholder for doing things when a connection is closed */
    protected void leaveConnection(Connection c) {}

    /** The server is being stopped, disconnect all the connections.
     * Currently nobody calls this.
     */
    public synchronized void stopServer()
    {
        up = false;
        serverDown();

        for (Enumeration e = conns.elements(); e.hasMoreElements();)
        {
            ((Connection) e.nextElement()).disconnect();
        }

        conns.removeAllElements();
    }

    /** remove a connection from the system */
    protected synchronized void removeConnection(Connection c)
    {
        //conns.removeElement(c);
        if (!conns.removeElement(c))
        {
            return;
        }

        c.disconnect();
        leaveConnection(c);
        D.ebugPrintln(c.host() + " left (" + connectionCount() + ")  " + (new Date()).toString() + ((c.error != null) ? (": " + c.error.toString()) : ""));
    }

    /** do cleanup after a remove connection */
    protected void removeConnectionCleanup(Connection c) {}

    /** add a connection to the system */
    protected synchronized void addConnection(Connection c)
    {
        if (c.connect())
        {
            numberOfConnections++;
            newConnection(c);
            conns.addElement(c);
            D.ebugPrintln(c.host() + " came (" + connectionCount() + ")  " + (new Date()).toString());
        }
    }

    /** broadcast a message */
    protected synchronized void broadcast(String m)
    {
        for (Enumeration e = getConnections(); e.hasMoreElements();)
        {
            ((Connection) e.nextElement()).put(m);
        }
    }

    class Command
    {
        public String str;
        public Connection con;

        public Command(String s, Connection c)
        {
            str = s;
            con = c;
        }
    }

    class Treater extends Thread
    {
        Server svr;

        public Treater(Server s)
        {
            svr = s;
        }

        public void run()
        {
            while (svr.isUp())
            {
                //D.ebugPrintln("treater server is up");
                Command c = null;

                synchronized (inQueue)
                {
                    if (inQueue.size() > 0)
                    {
                        //D.ebugPrintln("treater getting command");
                        c = (Command) inQueue.elementAt(0);
                        inQueue.removeElementAt(0);
                    }
                }

                try
                {
                    if (c != null)
                    {
                        svr.processCommand(c.str, c.con);
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Exception in treater (processCommand) - " + e);
                }

                yield();

                synchronized (inQueue)
                {
                    if (inQueue.size() == 0)
                    {
                        try
                        {
                            //D.ebugPrintln("treater waiting");
                            inQueue.wait(1000);
                        }
                        catch (Exception ex)
                        {
                            ;
                        }
                    }
                }
            }

            //D.ebugPrintln("treater returning; server not up");
        }
    }
}
