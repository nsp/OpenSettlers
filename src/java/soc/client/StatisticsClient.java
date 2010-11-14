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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package soc.client;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import soc.disableDebug.D;
import soc.message.Channels;
import soc.message.GetStatistics;
import soc.message.Message;
import soc.message.RejectConnection;
import soc.message.ResetStatistics;
import soc.message.ShowStatistics;
import soc.message.StatusMessage;
import soc.util.PlayerInfo;


/**
 * Applet/Standalone client for connecting to the SOCServer, viewing human and
 * robot statistics, and reseting statics for a human. If you want another
 * connection port, you have to specify it as the "port" argument in the html
 * source. If you run this as a stand-alone, you have to specify the port.
 *
 * Author:  Jim Browan
 **/
public class StatisticsClient extends Applet implements Runnable, ActionListener
{
    private static final long serialVersionUID = 3335969676221506087L;
    private static final String MAIN_PANEL = "main";
    private static final String MESSAGE_PANEL = "message";

    protected TextField nick;
    protected TextField pass;
    protected TextField status;
    protected Button reset;
    protected Label messageLabel;
    protected boolean submitLock;

    protected CardLayout cardLayout;
    
    protected String host;
    protected int port;
    protected Socket s;
    protected DataInputStream in;
    protected DataOutputStream out;
    protected Thread reader = null;
    protected Exception ex = null;
    protected boolean connected = false;
    
    /**
     * Declare table model for statistics data
     **/
    
    public static class StatisticsTableModel extends AbstractTableModel
    {
        /**
         * 
         */
        private static final long serialVersionUID = -2774438552967545151L;
        public static final String[] columnNames = {"Name",
                                                    "Rank",
                                                    "Wins",
                                                    "Losses",
                                                    "Ttl. Points",
                                                    "Avg. Points",
                                                    "Pct. Wins"};
        public static final Object[] longValues = { "nnnnnnnnnnnnnnn",
                                                    new Integer(999),
                                                    new Integer(999),
                                                    new Integer(999),
                                                    new Integer(9999),
                                                    new Float(10.00),
                                                    new Float(100.00) };
                                             
        private List data = new LinkedList();
        
        public int getColumnCount()
        {
            return columnNames.length;
        }
        public int getRowCount()
        {
            return data.size();
        }
        public boolean isCellEditable(int row, int col)
        {
            return false;
        }
        public String getColumnName(int col)
        {
            return columnNames[col];
        }
        public Object getValueAt(int row, int col)
        {
            PlayerInfo info = (PlayerInfo) data.get(row);
            
            switch (col)
            {
                case 0:
                    return info.getName();
                case 1:
                    return new Integer(info.getRank());
                case 2:
                    return new Integer(info.getWins());
                case 3:
                    return new Integer(info.getLosses());
                case 4:
                    return new Integer(info.getTotalPoints());
                case 5:
                    return new Float(info.getAveragePoints());
                case 6:
                    return new Float(info.getWinRatio());
                default:
                    return "<invalid: " + col + ">";
            }
        }
        
        /**
         * JTable uses this method to determine the default renderer/
         * editor for each cell.
         */
        public Class getColumnClass(int c) 
        {
            switch (c)
            {
                case 0 :
                    return String.class;
                default :
                    return Number.class;
            }
        }
        /**
         * Clears data and set's it anew, in one fell swoop
         */
        public void setData(Vector info)
        {
            data.clear();
            data.addAll(info);
            fireTableDataChanged();
        }
        public void addRow(PlayerInfo info)
        {
            data.add(info);
            int row = data.size()-1;
            fireTableRowsInserted(row, row);
        }
        public void setValueAt(Object value, int row, int col)
        {
            // not editable... ignore
        }
    };
    
    protected StatisticsTableModel humanModel = null;
    protected StatisticsTableModel robotModel = null;
    
    /**
     * the nickname
     **/
    protected String nickname = null;

    /**
     * the password
     **/
    protected String password = null;

    /**
     * Create a StatisticsClient connecting to localhost port 8880
     **/
    public StatisticsClient()
    {
        this(null, 8880);
    }

    /**
     * Constructor for connecting to the specified host, on the specified port.
     * Must call 'init' to start up and do layout.
     *
     * @param h  host
     * @param p  port
     **/
    public StatisticsClient(String h, int p)
    {
        host = h;
        port = p;
    }

    /**
     * Determine the amount of space between the edges and components
     **/
    public Insets getInsets()
    {
        return new Insets(0, 10, 10, 10);
    }
    
    /**
     * Helper method to set constraints
     **/
    protected void buildConstraints(GridBagConstraints gbc, int gx, int gy, int gw, int gh, int wx, int wy) 
    {
        gbc.gridx = gx;
        gbc.gridy = gy;
        gbc.gridwidth = gw;
        gbc.gridheight = gh;
        gbc.weightx = wx;
        gbc.weighty = wy;                
    }
    
    /**
     * This method picks good column sizes.
     *
     * @param table  The table for which the columns will be sized
     **/
    private void initColumnSizes(JTable table)
    {
        Object[] longValues = StatisticsTableModel.longValues;
        TableColumnModel columnModel = table.getColumnModel();
      
        for (int i = 0; i < longValues.length; i++)
        {
            TableColumn column = columnModel.getColumn(i);
            TableCellRenderer renderer = table.getTableHeader().getDefaultRenderer();
            Component comp = renderer.getTableCellRendererComponent(null, longValues[i], false, false, -1, 0);
            column.setPreferredWidth(comp.getWidth());
        }
    }
    
    /**
     * init the visual elements
     **/
    protected void initVisualElements()
    {
        setFont(new Font("Monaco", Font.PLAIN, 12));
        Font lf = new Font("Monaco", Font.BOLD, 18);
        
        nick = new TextField(20);
        pass = new TextField(10);
        pass.setEchoChar('*');
        status = new TextField(50);
        status.setEditable(false);
        reset = new Button("Reset Statistics");
        submitLock = false;

        reset.addActionListener(this);

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        Panel mainPane = new Panel(gbl);

        Label l;
        
        buildConstraints(gbc, 0, 0, 7, 1, 0, 5);
        l = new Label("Human Statistics");
        l.setFont(lf);
        gbl.setConstraints(l, gbc);
        mainPane.add(l);

        buildConstraints(gbc, 0, 1, 7, 10, 100, 50);
        gbc.fill = GridBagConstraints.BOTH;
        humanModel = new StatisticsTableModel();
        JTable humanTable = new JTable(humanModel);
        initColumnSizes(humanTable);
        JScrollPane humanScroll = new JScrollPane(humanTable);
        gbl.setConstraints(humanScroll, gbc);
        mainPane.add(humanScroll);
        gbc.fill = GridBagConstraints.NONE;
                
        buildConstraints(gbc, 0, 11, 1, 1, 0, 2);
        l = new Label("");
        gbl.setConstraints(l, gbc);
        mainPane.add(l);
        
        buildConstraints(gbc, 1, 11, 1, 1, 0, 2);
        l = new Label("Nickname:");
        gbc.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(l, gbc);
        mainPane.add(l);
        gbc.anchor = GridBagConstraints.CENTER;

        buildConstraints(gbc, 2, 11, 2, 1, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbl.setConstraints(nick, gbc);
        mainPane.add(nick);
        gbc.fill = GridBagConstraints.NONE;

        buildConstraints(gbc, 4, 11, 1, 1, 0, 0);
        gbc.anchor = GridBagConstraints.EAST;
        l = new Label("Password:");
        gbl.setConstraints(l, gbc);
        mainPane.add(l);
        gbc.anchor = GridBagConstraints.CENTER;

        buildConstraints(gbc, 5, 11, 2, 1, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbl.setConstraints(pass, gbc);
        mainPane.add(pass);
        gbc.fill = GridBagConstraints.NONE;
        
        buildConstraints(gbc, 0, 12, 2, 1, 0, 2);
        gbl.setConstraints(reset, gbc);
        mainPane.add(reset);

        buildConstraints(gbc, 2, 12, 5, 1, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbl.setConstraints(status, gbc);
        mainPane.add(status);
        gbc.fill = GridBagConstraints.NONE;

        buildConstraints(gbc, 0, 14, 7, 1, 0, 5);
        l = new Label("Robot Statistics");
        l.setFont(lf);
        gbl.setConstraints(l, gbc);
        mainPane.add(l);
        
        buildConstraints(gbc, 0, 15, 7, 10, 0, 50);
        gbc.fill = GridBagConstraints.BOTH;
        robotModel = new StatisticsTableModel();
        JTable robotTable = new JTable(robotModel);
        initColumnSizes(robotTable);
        JScrollPane robotScroll = new JScrollPane(robotTable);
        gbl.setConstraints(robotScroll, gbc);
        mainPane.add(robotScroll);
        gbc.fill = GridBagConstraints.NONE;

        // message label that takes up the whole pane
        messageLabel = new Label("", Label.CENTER);

        Panel messagePane = new Panel(new BorderLayout());
        messagePane.add(messageLabel, BorderLayout.CENTER);
        
        // all together now...
        cardLayout = new CardLayout();
        setLayout(cardLayout);

        add(messagePane, MESSAGE_PANEL); // shown first
        add(mainPane, MAIN_PANEL);
    }

    /**
     * Retrieve a parameter and translate to a hex value.
     *
     * @param name a parameter name. null is ignored
     * @return the parameter parsed as a hex value or -1 on error
     **/
    public int getHexParameter(String name)
    {
        String value = null;
        int iValue = -1;
        try
        {
            value = getParameter(name);
            if (value != null)
            {
                iValue = Integer.parseInt(value, 16);
            }
        }
        catch (Exception e)
        {
            System.err.println("Invalid " + name + ": " + value);
        }
        return iValue;
    }

    /**
     * Initialize the applet
     **/
    public synchronized void init()
    {
        System.out.println("Java Settlers Statistics Client, Version 1.0, by Jim Browan");
        System.out.println("Network layer based on code by Cristian Bogdan.");

        String param = null;
        int intValue;
            
        intValue = getHexParameter("background"); 
        if (intValue != -1)
            setBackground(new Color(intValue));

        intValue = getHexParameter("foreground");
        if (intValue != -1)
            setForeground(new Color(intValue));
        
        initVisualElements(); // after the background is set
        
        System.out.println("Getting host...");
        host = getCodeBase().getHost();
        if (host.equals(""))
            host = null;  // localhost

        try {
            param = getParameter("PORT");
            if (param != null)
                port = Integer.parseInt(param);
        }
        catch (Exception e) {
            System.err.println("Invalid port: " + param);
        }

        connect();
    }

    /**
     * Attempts to connect to the server. See {@link #connected} for success or
     * failure.
     * @throws IllegalStateException if already connected 
     **/
    public synchronized void connect()
    {
        String hostString = (host != null ? host : "localhost") + ":" + port;
        if (connected)
        {
            throw new IllegalStateException("Already connected to " +
                                            hostString);
        }
        System.out.println("Connecting to " + hostString);
        messageLabel.setText("Connecting to server...");

        try
        {
            s = new Socket(host, port);
            in = new DataInputStream(s.getInputStream());
            out = new DataOutputStream(s.getOutputStream());
            connected = true;
            (reader = new Thread(this)).start();
        }
        catch (Exception e)
        {
            ex = e;
            String msg = "Could not connect to the server: " + ex;
            System.err.println(msg);
            messageLabel.setText(msg);
        }
        
        // Request statistics data
        put(GetStatistics.toCmd("human"));
        put(GetStatistics.toCmd("robot"));
    }

    /**
     * Show statistics is seperate tables
     * @param type PlayerInfo.HUMAN or PlayerInfo.ROBOT
     * @param statistics Vector of soc.util.PlayerInfo objects
     */
    public void showStats(String type, Vector statistics)
    {
        if (type.equals(PlayerInfo.ROBOT))
        {
            robotModel.setData(statistics);
        }
        else  // type == PlayerInfo.HUMAN
        {
            humanModel.setData(statistics);
        }
    }
    
    /**
     * Handle mouse clicks and keyboard
     **/
    public void actionPerformed(ActionEvent e)
    {
        Object target = e.getSource();

        if (target == reset)
        {
            String n = nick.getText().trim();

            if (n.length() > 20)
            {
                nickname = n.substring(1, 20);
            }
            else
            {
                nickname = n;
            }

            String p1 = pass.getText().trim();

            if (p1.length() > 10)
            {
                password = p1.substring(1, 10);
            }
            else
            {
                password = p1;
            }

            //
            // make sure all the info is ok
            //
            if (nickname.length() == 0)
            {
                status.setText("You must enter a nickname.");
                nick.requestFocus();
            }
            else if (password.length() == 0)
            {
                status.setText("You must enter a password.");
                pass.requestFocus();
            }
            else if (!submitLock)
            {
                submitLock = true;
                status.setText("Resetting statistics for" + nickname + "...");
                put(ResetStatistics.toCmd(nickname, password));
            }
        }
    }

    /**
     * continuously read from the net in a separate thread
     **/
    public void run()
    {
        try
        {
            while (connected)
            {
                String s = in.readUTF();
                treat((Message) Message.toMsg(s));
            }
        }
        catch (IOException e)
        {
            // purposefully closing the socket brings us here too
            if (connected)
            {
                ex = e;
                System.out.println("Could not read from the net: " + ex);
                destroy();
            }
        }
    }

    /**
     * write a message to the net
     *
     * @param s  the message
     * @return true if the message was sent, false if not
     **/
    public synchronized boolean put(String s)
    {
        D.ebugPrintln("OUT - " + s);

        if ((ex != null) || !connected)
        {
            return false;
        }

        try
        {
            out.writeUTF(s);
        }
        catch (IOException e)
        {
            ex = e;
            System.err.println("Could not write to the net: " + ex);
            destroy();

            return false;
        }

        return true;
    }

    /**
     * Treat the incoming messages
     *
     * @param mes    the message
     **/
    public void treat(Message mes)
    {
        D.ebugPrintln(mes.toString());

        try
        {
            switch (mes.getType())
            {
            /**
             * list of channels on the server (first message from server)
             **/
            case Message.CHANNELS:
                handleCHANNELS((Channels) mes);
                break;

            /**
             * status message
             **/
            case Message.STATUSMESSAGE:
                handleSTATUSMESSAGE((StatusMessage) mes);
                break;

            /**
             * handle the reject connection message
             **/
            case Message.REJECTCONNECTION:
                handleREJECTCONNECTION((RejectConnection) mes);
                break;

            /**
             * Handle the show statistics message
             **/
            case Message.SHOWSTATS:
                handleSHOWSTATS((ShowStatistics) mes);

            break;
            }
        }
        catch (Exception e)
        {
            System.out.println("StatisticsClient treat ERROR - " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * handle the "list of channels" message
     * @param mes  the message
     **/
    protected void handleCHANNELS(Channels mes)
    {
        //
        // this message indicates that we're connected to the server
        //
        cardLayout.show(this, MAIN_PANEL);
        validate();
    }

    /**
     * handle the "reject connection" message
     * @param mes  the message
     **/
    protected void handleREJECTCONNECTION(RejectConnection mes)
    {
        disconnect();

        messageLabel.setText(mes.getText());
        cardLayout.show(this, MESSAGE_PANEL);
        validate();
    }

    /**
     * handle the "status message" message
     * @param mes  the message
     */
    protected void handleSTATUSMESSAGE(StatusMessage mes)
    {
        status.setText(mes.getStatus());
        submitLock = false;
    }
    
    /**
     * Handle the "show statistics" message
     *
     * @param mes the message
     **/
    protected void handleSHOWSTATS(ShowStatistics mes)
    {
        showStats(mes.getStype(), mes.getStatistics());
    }

    /**
     * disconnect from the net
     **/
    protected synchronized void disconnect()
    {
        connected = false;

        // reader will die once 'connected' is false, and socket is closed

        try
        {
            s.close();
        }
        catch (Exception e)
        {
            ex = e;
        }
    }

    /**
     * applet info
     **/
    public String getAppletInfo()
    {
        return "StatisticsClient 1.0 by Jim Browan.";
    }

    /** destroy the applet */
    public void destroy()
    {
        String err = "Sorry, the applet has been destroyed. " + ((ex == null) ? "Load the page again." : ex.toString());

        disconnect();
        
        messageLabel.setText(err);
        cardLayout.show(this, MESSAGE_PANEL);
        validate();
    }

    /**
     * for stand-alones
     **/
    public static void usage()
    {
        System.err.println("usage: java soc.client.StatisticsClient <host> <port>");
    }

    /**
     * for stand-alones
     **/
    public static void main(String[] args)
    {
        StatisticsClient client = new StatisticsClient();
        
        if (args.length != 2)
        {
            usage();
            System.exit(1);
        }

        try 
        {
            client.host = args[0];
            client.port = Integer.parseInt(args[1]);
        } 
        catch (NumberFormatException x) 
        {
            usage();
            System.err.println("Invalid port: " + args[1]);
            System.exit(1);
        }

        Frame frame = new Frame("StatisticsClient");
        frame.setBackground(new Color(Integer.parseInt("61AF71",16)));
        frame.setForeground(Color.black);
        // Add a listener for the close event
        frame.addWindowListener(client.createWindowAdapter());

        client.initVisualElements(); // after the background is set
        
        frame.add(client, BorderLayout.NORTH);
        frame.setSize(600, 600);
        frame.setVisible(true);

        client.connect();
    }

    private WindowAdapter createWindowAdapter()
    {
        return new MyWindowAdapter();
    }
    
    private class MyWindowAdapter extends WindowAdapter
    {
        public void windowClosing(WindowEvent evt)
        {
            System.exit(0);
        }

        public void windowOpened(WindowEvent evt)
        {
            nick.requestFocus();
        }
    }
}
