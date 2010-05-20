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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import java.net.Socket;

import java.util.Vector;


/** A server connection.
 *  @version 1.0
 *  @author <A HREF="http://www.nada.kth.se/~cristi">Cristian Bogdan</A>
 *  Reads from the net, writes atomically to the net and
 *  holds the connection data
 */
public final class Connection implements Serializable, Cloneable
{
    protected final static int TIMEOUT_VALUE = 3600000; // approx. 1 hour

    /** The data associated with this connection. */
    public Object data;

    /** The most recent error. */
    protected Exception error = null;

    private DataInputStream in = null;
    private DataOutputStream out = null;
    private Socket socket = null;
    private Server server;
    private String host;
    private boolean connected = false;
    private Vector outQueue = new Vector();

    /** Initialize the connection data. */
    Connection(Socket so, Server sv)
    {
        host = so.getInetAddress().getHostName();

        server = sv;
        socket = so;
        data = null;
    }

    /** Return the host name of this machine known by this connection. */
    public String host()
    {
        return host;
    }

    /** Returns true if the connection to the client is active. */
    public boolean isConnected()
    {
        return connected;
    }

    /** Use this to send a message to the client this connection is to! */
    public final boolean put(String message)
    {
        synchronized (outQueue)
        {
            D.ebugPrintln("Adding " + message + " to outQueue for " + data);
            outQueue.addElement(message);
            outQueue.notify();
        }

        return true;
    }

    /** Start reading from the net; called only by the server. */
    protected boolean connect()
    {
        try
        {
            socket.setSoTimeout(TIMEOUT_VALUE);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            connected = true;

            // start the reader
            new Reciever().start();

            // start the writer
            new Sender().start();
        }
        catch (Exception e)
        {
            D.ebugPrintln("IOException in Connection.connect (" +  host + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            error = e;
            disconnect();

            return false;
        }

        return true;
    }

    /** Close the socket, stop the reader; called only by server and self. */
    protected void disconnect()
    {
        D.ebugPrintln("DISCONNECTING " + data);
        connected = false;

        try
        {
            // kills the Reciever thread w/ SocketException("Socket closed")
            socket.close();
            // wake the Sender thread
            synchronized (outQueue) {
                outQueue.notify();
            }
        }
        catch (IOException e)
        {
            if (D.ebugOn)
            {
                D.ebugPrintln("IOException in Connection.disconnect (" + host + ") - " + e);
                e.printStackTrace(System.out);
            }

            error = e;
        }

        in = null;
        out = null;
        socket = null;
    }

    /** Put a message on the net, disconnecting on failure. */
    private final boolean send(String message)
    {
        try
        {
            if ((error == null) && connected)
            {
                //D.ebugPrintln("trying to put "+message+" to "+data);
                out.writeUTF(message);
                return true;
            }
        }
        catch (IOException e)
        {
            D.ebugPrintln("IOException in Connection.send(" + host + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            error = e;
        }
        catch (Exception ex)
        {
            D.ebugPrintln("generic exception in connection putaux");

            if (D.ebugOn)
            {
                ex.printStackTrace(System.out);
            }

            error = ex;
        }

        // all failures go through here
        if (connected)
        {
            server.removeConnection(this);
        }

        return false;
    }

    /** Thread to read commands from the net and pass to server to treat. */
    private class Reciever extends Thread
    {
        public Reciever()
        {
            setDaemon(true);
        }

        // continuously read from the net
        public void run()
        {
            try
            {
                while (connected)
                {
                    server.treat(in.readUTF(), Connection.this);
                }
            }
            catch (IOException e)
            {
                D.ebugPrintln("IOException in Connection.run (" +
                              host + ") - " + e);

                if (D.ebugOn)
                {
                    e.printStackTrace(System.out);
                }

                if (connected)
                {
                    error = e;
                    // calls disconnect
                    server.removeConnection(Connection.this);
                }
            }
        }
    }

    /**
     * Thread to dequeue commands (queued by ServerImpl) and send to client.
     */
    private class Sender extends Thread
    {
        public Sender()
        {
            setDaemon(true);
        }

        public void run()
        {
            while (connected)
            {
                String message = null;

                synchronized (outQueue)
                {
                    while (outQueue.size() == 0 && connected)
                    {
                        try
                        {
                            outQueue.wait();
                        }
                        catch (InterruptedException x) //what might cause this?
                        {
                            x.printStackTrace();
                        }
                    }
                    
                    if (connected)
                    {
                        message = (String) outQueue.elementAt(0);
                        outQueue.removeElementAt(0);
                    }
                }

                if (message != null)
                {
                    try
                    {
                        send(message);
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
