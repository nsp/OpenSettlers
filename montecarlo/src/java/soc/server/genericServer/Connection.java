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
public final class Connection extends Thread implements Runnable, Serializable, Cloneable
{
    static int putters = 0;
    static Object puttersMonitor = new Object();
    protected final static int TIMEOUT_VALUE = 3600000; // approx. 1 hour

    /**
     * the data associated with this connection
     */
    public Object data;
    DataInputStream in = null;
    DataOutputStream out = null;
    Socket s = null;
    Server sv;
    public Thread reader;
    protected String hst;
    protected Exception error = null;
    protected boolean connected = false;
    public Vector outQueue = new Vector();

    /** initialize the connection data */
    Connection(Socket so, Server sve)
    {
        hst = so.getInetAddress().getHostName();

        sv = sve;
        s = so;
        reader = null;
        data = null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public String host()
    {
        return hst;
    }

    /** start reading from the net; called only by the server */
    boolean connect()
    {
        try
        {
            s.setSoTimeout(TIMEOUT_VALUE);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
            connected = true;
            reader = this;

            Putter putter = new Putter(this);
            putter.start();

            //(reader=new Thread(this)).start();
        }
        catch (Exception e)
        {
            D.ebugPrintln("IOException in Connection.connect (" + hst + ") - " + e);

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

    /** continuously read from the net */
    public void run()
    {
        sv.addConnection(this);

        try
        {
            while (connected)
            {
                sv.treat(in.readUTF(), this);
            }
        }
        catch (IOException e)
        {
            D.ebugPrintln("IOException in Connection.run (" + hst + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            if (!connected)
            {
                return;
            }

            error = e;
            sv.removeConnection(this);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param str DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final boolean put(String str)
    {
        synchronized (outQueue)
        {
            D.ebugPrintln("Adding " + str + " to outQueue for " + data);
            outQueue.addElement(str);
            outQueue.notify();
        }

        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param str DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean putForReal(String str)
    {
        boolean rv = putAux(str);

        if (!rv)
        {
            if (!connected)
            {
                return false;
            }
            else
            {
                sv.removeConnection(this);
            }

            return false;
        }
        else
        {
            return true;
        }
    }

    /** put a message on the net
     * @return success, disconnects on failure
     */
    public final boolean putAux(String str)
    {
        if ((error != null) || !connected)
        {
            return false;
        }

        try
        {
            //D.ebugPrintln("trying to put "+str+" to "+data);
            out.writeUTF(str);
        }
        catch (IOException e)
        {
            D.ebugPrintln("IOException in Connection.putAux (" + hst + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            error = e;

            return false;
        }
        catch (Exception ex)
        {
            D.ebugPrintln("generic exception in connection putaux");

            if (D.ebugOn)
            {
                ex.printStackTrace(System.out);
            }

            return false;
        }

        return true;
    }

    /** close the socket, stop the reader */
    void disconnect()
    {
        D.ebugPrintln("DISCONNECTING " + data);
        connected = false;

        /*                if(Thread.currentThread()!=reader && reader!=null && reader.isAlive())
           reader.stop();*/
        try
        {
            s.close();
        }
        catch (IOException e)
        {
            D.ebugPrintln("IOException in Connection.disconnect (" + hst + ") - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            error = e;
        }

        s = null;
        in = null;
        out = null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean isConnected()
    {
        return connected;
    }

    class Putter extends Thread
    {
        Connection con;

        //public boolean putting = true;
        public Putter(Connection c)
        {
            con = c;
            D.ebugPrintln("NEW PUTTER CREATED FOR " + data);
        }

        public void run()
        {
            while (con.connected)
            {
                String c = null;

                D.ebugPrintln("** " + data + " is at the top of the putter loop");

                synchronized (outQueue)
                {
                    if (outQueue.size() > 0)
                    {
                        c = (String) outQueue.elementAt(0);
                        outQueue.removeElementAt(0);
                    }
                }

                if (c != null)
                {
                    boolean rv = con.putForReal(c);

                    // rv ignored because handled by putForReal
                }

                synchronized (outQueue)
                {
                    if (outQueue.size() == 0)
                    {
                        try
                        {
                            //D.ebugPrintln("** "+data+" is WAITING for outQueue");
                            outQueue.wait(1000);
                        }
                        catch (Exception ex)
                        {
                            D.ebugPrintln("Exception while waiting for outQueue in " + data + ". - " + ex);
                        }
                    }
                }
            }

            D.ebugPrintln("putter not putting connected==false : " + data);
        }
    }
}
