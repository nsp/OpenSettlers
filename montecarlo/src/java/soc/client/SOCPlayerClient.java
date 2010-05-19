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
package soc.client;

import soc.disableDebug.D;

import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardSet;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.game.SOCTradeOffer;

import soc.message.SOCAcceptOffer;
import soc.message.SOCBCastTextMsg;
import soc.message.SOCBankTrade;
import soc.message.SOCBoardLayout;
import soc.message.SOCBuildRequest;
import soc.message.SOCBuyCardRequest;
import soc.message.SOCCancelBuildRequest;
import soc.message.SOCChangeFace;
import soc.message.SOCChannels;
import soc.message.SOCChoosePlayer;
import soc.message.SOCChoosePlayerRequest;
import soc.message.SOCClearOffer;
import soc.message.SOCClearTradeMsg;
import soc.message.SOCDeleteChannel;
import soc.message.SOCDeleteGame;
import soc.message.SOCDevCard;
import soc.message.SOCDevCardCount;
import soc.message.SOCDiceResult;
import soc.message.SOCDiscard;
import soc.message.SOCDiscardRequest;
import soc.message.SOCDiscoveryPick;
import soc.message.SOCEndTurn;
import soc.message.SOCFirstPlayer;
import soc.message.SOCGameMembers;
import soc.message.SOCGameState;
import soc.message.SOCGameStats;
import soc.message.SOCGameTextMsg;
import soc.message.SOCGames;
import soc.message.SOCJoin;
import soc.message.SOCJoinAuth;
import soc.message.SOCJoinGame;
import soc.message.SOCJoinGameAuth;
import soc.message.SOCLargestArmy;
import soc.message.SOCLeave;
import soc.message.SOCLeaveAll;
import soc.message.SOCLeaveGame;
import soc.message.SOCLongestRoad;
import soc.message.SOCMakeOffer;
import soc.message.SOCMembers;
import soc.message.SOCMessage;
import soc.message.SOCMonopolyPick;
import soc.message.SOCMoveRobber;
import soc.message.SOCNewChannel;
import soc.message.SOCNewGame;
import soc.message.SOCPlayDevCardRequest;
import soc.message.SOCPlayerElement;
import soc.message.SOCPotentialSettlements;
import soc.message.SOCPutPiece;
import soc.message.SOCRejectConnection;
import soc.message.SOCRejectOffer;
import soc.message.SOCResourceCount;
import soc.message.SOCRollDice;
import soc.message.SOCSetPlayedDevCard;
import soc.message.SOCSetSeatLock;
import soc.message.SOCSetTurn;
import soc.message.SOCSitDown;
import soc.message.SOCStartGame;
import soc.message.SOCStatusMessage;
import soc.message.SOCTextMsg;
import soc.message.SOCTurn;

import soc.util.Version;

import java.applet.Applet;
import java.applet.AppletContext;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


/**
 * Applet/Standalone client for connecting to the SOCServer.
 * If you want another connection port, you have to specify it as the "port"
 * argument in the html source. If you run this as a stand-alone, you have to
 * specify the port.
 *
 * @author Robert S Thomas
 */
public class SOCPlayerClient extends Applet implements Runnable, ActionListener
{
    private static final String MAIN_PANEL = "main";
    private static final String MESSAGE_PANEL = "message";

    protected static String STATSPREFEX = "  [";
    protected TextField nick;
    protected TextField pass;
    protected TextField status;
    protected TextField channel;
    protected TextField game;
    protected java.awt.List chlist;
    protected java.awt.List gmlist;
    protected Button jc;
    protected Button jg;
    protected Label messageLabel;
    protected AppletContext ac;
    protected String lastMessage;

    protected CardLayout cardLayout;
    
    public String host;
    public int port;
    public Socket s;
    protected DataInputStream in;
    protected DataOutputStream out;
    protected Thread reader = null;
    protected Exception ex = null;
    protected boolean connected = false;

    /**
     * the nickname
     */
    public String nickname = null;

    /**
     * the password
     */
    protected String password = null;

    /**
     * true if we've stored the password
     */
    protected boolean gotPassword;

    /**
     * the channels
     */
    protected Hashtable channels = new Hashtable();

    /**
     * the games
     */
    public Hashtable games = new Hashtable();

    /**
     * the player interfaces for the games
     */
    public Hashtable playerInterfaces = new Hashtable();

    /**
     * the ignore list
     */
    protected Vector ignoreList = new Vector();

    /**
     * Create a SOCPlayerClient connecting to localhost port 8880
     */
    public SOCPlayerClient()
    {
        this(null, 8880);
    }

    /**
     * Constructor for connecting to the specified host, on the specified
     * port.  Must call 'init' to start up and do layout.
     *
     * @param h  host
     * @param p  port
     */
    public SOCPlayerClient(String h, int p)
    {
        gotPassword = false;
        host = h;
        port = p;
    }

    /**
     * init the visual elements
     */
    public void initVisualElements()
    {
        setFont(new Font("Monaco", Font.PLAIN, 12));
        
        nick = new TextField(20);
        pass = new TextField(20);
        pass.setEchoChar('*');
        status = new TextField(20);
        status.setEditable(false);
        channel = new TextField(20);
        game = new TextField(20);
        chlist = new java.awt.List(10, false);
        chlist.add(" ");
        gmlist = new java.awt.List(10, false);
        gmlist.add(" ");
        jc = new Button("Join Channel");
        jg = new Button("Join Game");

        nick.addActionListener(this);
        pass.addActionListener(this);
        channel.addActionListener(this);
        game.addActionListener(this);
        chlist.addActionListener(this);
        gmlist.addActionListener(this);
        jc.addActionListener(this);
        jg.addActionListener(this);
        
        ac = null;

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        Panel mainPane = new Panel(gbl);

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(status, c);
        mainPane.add(status);

        Label l;

        l = new Label();
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label("Your Nickname:");
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        c.gridwidth = 1;
        gbl.setConstraints(nick, c);
        mainPane.add(nick);

        l = new Label();
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label("Optional Password:");
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        c.gridwidth = 1;
        gbl.setConstraints(pass, c);
        mainPane.add(pass);

        l = new Label();
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label();
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label("New Channel:");
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        c.gridwidth = 1;
        gbl.setConstraints(channel, c);
        mainPane.add(channel);

        l = new Label();
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label("New Game:");
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        c.gridwidth = 1;
        gbl.setConstraints(game, c);
        mainPane.add(game);

        l = new Label();
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label();
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label();
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        c.gridwidth = 1;
        gbl.setConstraints(jc, c);
        mainPane.add(jc);

        l = new Label();
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label();
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        c.gridwidth = 1;
        gbl.setConstraints(jg, c);
        mainPane.add(jg);

        l = new Label();
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label("Channels");
        c.gridwidth = 2;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label();
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        l = new Label("Games");
        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        c.gridwidth = 2;
        c.gridheight = GridBagConstraints.REMAINDER;
        gbl.setConstraints(chlist, c);
        mainPane.add(chlist);

        l = new Label();
        c.gridwidth = 1;
        gbl.setConstraints(l, c);
        mainPane.add(l);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(gmlist, c);
        mainPane.add(gmlist);

        // message label that takes up the whole pane
        messageLabel = new Label("", Label.CENTER);

        Panel messagePane = new Panel(new BorderLayout());
        messagePane.add(messageLabel, BorderLayout.CENTER);
        
        // all together now...
        cardLayout = new CardLayout();
        setLayout(cardLayout);

        add(messagePane, MESSAGE_PANEL); // shown first
        add(mainPane, MAIN_PANEL);

        messageLabel.setText("Waiting to connect.");
        validate();
    }

    /**
     * Retrieve a parameter and translate to a hex value.
     *
     * @param name a parameter name. null is ignored
     * @return the parameter parsed as a hex value or -1 on error
     */
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
     * Called when the applet should start it's work.
     */
    public void start()
    {
        nick.requestFocus();
    }
    
    /**
     * Initialize the applet
     */
    public synchronized void init()
    {
        System.out.println("Java Settlers Client " + Version.version() +
                           ", " + Version.copyright());
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

        param = getParameter("suggestion");
        if (param != null)
            channel.setText(param); // after visuals initialized

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
     */
    public synchronized void connect()
    {
        String hostString = (host != null ? host : "localhost") + ":" + port;
        if (connected)
        {
            throw new IllegalStateException("Already connected to " +
                                            hostString);
        }
                
        System.out.println("Connecting to " + hostString);
//        messageLabel.setText("Connecting to server...");
        
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
//            messageLabel.setText(msg);
        }
    }

    /**
     * @return the nickname of this user
     */
    public String getNickname()
    {
        return nickname;
    }

    /**
     * Handle mouse clicks and keyboard
     */
    public void actionPerformed(ActionEvent e)
    {
        Object target = e.getSource();

        if ((target == jc) || (target == channel) || (target == chlist)) // Join channel stuff
        {
            String ch;

            if (target == jc) // "Join Channel" Button
            {
                ch = channel.getText().trim();

                if (ch.length() == 0)
                {
                    try
                    {
                        ch = chlist.getSelectedItem().trim();
                    }
                    catch (NullPointerException ex)
                    {
                        return;
                    }
                }
            }
            else if (target == channel)
            {
                ch = channel.getText().trim();
            }
            else
            {
                try
                {
                    ch = chlist.getSelectedItem().trim();
                }
                catch (NullPointerException ex)
                {
                    return;
                }
            }

            if (ch.length() == 0)
            {
                return;
            }

            ChannelFrame cf = (ChannelFrame) channels.get(ch);

            if (cf == null)
            {
                if (channels.isEmpty())
                {
                    String n = nick.getText().trim();

                    if (n.length() == 0)
                    {
                        return;
                    }

                    if (n.length() > 20)
                    {
                        nickname = n.substring(1, 20);
                    }
                    else
                    {
                        nickname = n;
                    }

                    if (!gotPassword)
                    {
                        String p = pass.getText().trim();

                        if (p.length() > 20)
                        {
                            password = p.substring(1, 20);
                        }
                        else
                        {
                            password = p;
                        }
                    }
                }

                status.setText("Talking to server...");
                put(SOCJoin.toCmd(nickname, password, host, ch));
            }
            else
            {
                cf.show();
            }

            channel.setText("");

            return;
        }

        if ((target == jg) || (target == game) || (target == gmlist)) // Join game stuff
        {
            String gm;

            if (target == jg) // "Join Game" Button
            {
                gm = game.getText().trim();

                if (gm.length() == 0)
                {
                    try
                    {
                        gm = gmlist.getSelectedItem().trim();
                    }
                    catch (NullPointerException ex)
                    {
                        return;
                    }
                }
            }
            else if (target == game)
            {
                gm = game.getText().trim();
            }
            else
            {
                try
                {
                    gm = gmlist.getSelectedItem().trim();
                }
                catch (NullPointerException ex)
                {
                    return;
                }
            }

            // System.out.println("GM = |"+gm+"|");
            if (gm.length() == 0)
            {
                return;
            }

            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(gm);

            if (pi == null)
            {
                if (games.isEmpty())
                {
                    String n = nick.getText().trim();

                    if (n.length() == 0)
                    {
                        return;
                    }

                    if (n.length() > 20)
                    {
                        nickname = n.substring(1, 20);
                    }
                    else
                    {
                        nickname = n;
                    }

                    if (!gotPassword)
                    {
                        String p = pass.getText().trim();

                        if (p.length() > 20)
                        {
                            password = p.substring(1, 20);
                        }
                        else
                        {
                            password = p;
                        }
                    }
                }

                int endOfName = gm.indexOf(STATSPREFEX);

                if (endOfName > 0)
                {
                    gm = gm.substring(0, endOfName);
                }

                status.setText("Talking to server...");
                put(SOCJoinGame.toCmd(nickname, password, host, gm));
            }
            else
            {
                pi.show();
            }

            game.setText("");

            return;
        }

        if (target == nick)
        { // Nickname TextField
            nick.transferFocus();
        }

        return;
    }

    /**
     * continuously read from the net in a separate thread
     */
    public void run()
    {
        try
        {
            while (connected)
            {
                String s = in.readUTF();
                treat((SOCMessage) SOCMessage.toMsg(s));
            }
        }
        catch (IOException e)
        {
            // purposefully closing the socket brings us here too
            if (connected)
            {
                ex = e;
                System.out.println("could not read from the net: " + ex);
                destroy();
            }
        }
    }

    /**
     * resend the last message
     */
    public void resend()
    {
        put(lastMessage);
    }

    /**
     * write a message to the net
     *
     * @param s  the message
     * @return true if the message was sent, false if not
     */
    public synchronized boolean put(String s)
    {
        lastMessage = s;

        D.ebugPrintln("OUT - " + SOCMessage.toMsg(s));

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
            System.err.println("could not write to the net: " + ex);
            destroy();

            return false;
        }

        return true;
    }

    /**
     * Treat the incoming messages
     *
     * @param mes    the message
     */
    public void treat(SOCMessage mes)
    {
        D.ebugPrintln(mes.toString());

        try
        {
            switch (mes.getType())
            {
            /**
             * status message
             */
            case SOCMessage.STATUSMESSAGE:
                handleSTATUSMESSAGE((SOCStatusMessage) mes);

                break;

            /**
             * join channel authorization
             */
            case SOCMessage.JOINAUTH:
                handleJOINAUTH((SOCJoinAuth) mes);

                break;

            /**
             * someone joined a channel
             */
            case SOCMessage.JOIN:
                handleJOIN((SOCJoin) mes);

                break;

            /**
             * list of members for a channel
             */
            case SOCMessage.MEMBERS:
                handleMEMBERS((SOCMembers) mes);

                break;

            /**
             * a new channel has been created
             */
            case SOCMessage.NEWCHANNEL:
                handleNEWCHANNEL((SOCNewChannel) mes);

                break;

            /**
             * list of channels on the server
             */
            case SOCMessage.CHANNELS:
                handleCHANNELS((SOCChannels) mes);

                break;

            /**
             * text message
             */
            case SOCMessage.TEXTMSG:
                handleTEXTMSG((SOCTextMsg) mes);

                break;

            /**
             * someone left the channel
             */
            case SOCMessage.LEAVE:
                handleLEAVE((SOCLeave) mes);

                break;

            /**
             * delete a channel
             */
            case SOCMessage.DELETECHANNEL:
                handleDELETECHANNEL((SOCDeleteChannel) mes);

                break;

            /**
             * list of games on the server
             */
            case SOCMessage.GAMES:
                handleGAMES((SOCGames) mes);

                break;

            /**
             * join game authorization
             */
            case SOCMessage.JOINGAMEAUTH:
                handleJOINGAMEAUTH((SOCJoinGameAuth) mes);

                break;

            /**
             * someone joined a game
             */
            case SOCMessage.JOINGAME:
                handleJOINGAME((SOCJoinGame) mes);

                break;

            /**
             * someone left a game
             */
            case SOCMessage.LEAVEGAME:
                handleLEAVEGAME((SOCLeaveGame) mes);

                break;

            /**
             * new game has been created
             */
            case SOCMessage.NEWGAME:
                handleNEWGAME((SOCNewGame) mes);

                break;

            /**
             * game has been destroyed
             */
            case SOCMessage.DELETEGAME:
                handleDELETEGAME((SOCDeleteGame) mes);

                break;

            /**
             * list of game members
             */
            case SOCMessage.GAMEMEMBERS:
                handleGAMEMEMBERS((SOCGameMembers) mes);

                break;

            /**
             * game stats
             */
            case SOCMessage.GAMESTATS:
                handleGAMESTATS((SOCGameStats) mes);

                break;

            /**
             * game text message
             */
            case SOCMessage.GAMETEXTMSG:
                handleGAMETEXTMSG((SOCGameTextMsg) mes);

                break;

            /**
             * broadcast text message
             */
            case SOCMessage.BCASTTEXTMSG:
                handleBCASTTEXTMSG((SOCBCastTextMsg) mes);

                break;

            /**
             * someone is sitting down
             */
            case SOCMessage.SITDOWN:
                handleSITDOWN((SOCSitDown) mes);

                break;

            /**
             * receive a board layout
             */
            case SOCMessage.BOARDLAYOUT:
                handleBOARDLAYOUT((SOCBoardLayout) mes);

                break;

            /**
             * message that the game is starting
             */
            case SOCMessage.STARTGAME:
                handleSTARTGAME((SOCStartGame) mes);

                break;

            /**
             * update the state of the game
             */
            case SOCMessage.GAMESTATE:
                handleGAMESTATE((SOCGameState) mes);

                break;

            /**
             * set the current turn
             */
            case SOCMessage.SETTURN:
                handleSETTURN((SOCSetTurn) mes);

                break;

            /**
             * set who the first player is
             */
            case SOCMessage.FIRSTPLAYER:
                handleFIRSTPLAYER((SOCFirstPlayer) mes);

                break;

            /**
             * update who's turn it is
             */
            case SOCMessage.TURN:
                handleTURN((SOCTurn) mes);

                break;

            /**
             * receive player information
             */
            case SOCMessage.PLAYERELEMENT:
                handlePLAYERELEMENT((SOCPlayerElement) mes);

                break;

            /**
             * receive resource count
             */
            case SOCMessage.RESOURCECOUNT:
                handleRESOURCECOUNT((SOCResourceCount) mes);

                break;

            /**
             * the latest dice result
             */
            case SOCMessage.DICERESULT:
                handleDICERESULT((SOCDiceResult) mes);

                break;

            /**
             * a player built something
             */
            case SOCMessage.PUTPIECE:
                handlePUTPIECE((SOCPutPiece) mes);

                break;

            /**
             * the robber moved
             */
            case SOCMessage.MOVEROBBER:
                handleMOVEROBBER((SOCMoveRobber) mes);

                break;

            /**
             * the server wants this player to discard
             */
            case SOCMessage.DISCARDREQUEST:
                handleDISCARDREQUEST((SOCDiscardRequest) mes);

                break;

            /**
             * the server wants this player to choose a player to rob
             */
            case SOCMessage.CHOOSEPLAYERREQUEST:
                handleCHOOSEPLAYERREQUEST((SOCChoosePlayerRequest) mes);

                break;

            /**
             * a player has made an offer
             */
            case SOCMessage.MAKEOFFER:
                handleMAKEOFFER((SOCMakeOffer) mes);

                break;

            /**
             * a player has cleared her offer
             */
            case SOCMessage.CLEAROFFER:
                handleCLEAROFFER((SOCClearOffer) mes);

                break;

            /**
             * a player has rejected an offer
             */
            case SOCMessage.REJECTOFFER:
                handleREJECTOFFER((SOCRejectOffer) mes);

                break;

            /**
             * the trade message needs to be cleared
             */
            case SOCMessage.CLEARTRADEMSG:
                handleCLEARTRADEMSG((SOCClearTradeMsg) mes);

                break;

            /**
             * the current number of development cards
             */
            case SOCMessage.DEVCARDCOUNT:
                handleDEVCARDCOUNT((SOCDevCardCount) mes);

                break;

            /**
             * a dev card action, either draw, play, or add to hand
             */
            case SOCMessage.DEVCARD:
                handleDEVCARD((SOCDevCard) mes);

                break;

            /**
             * set the flag that tells if a player has played a
             * development card this turn
             */
            case SOCMessage.SETPLAYEDDEVCARD:
                handleSETPLAYEDDEVCARD((SOCSetPlayedDevCard) mes);

                break;

            /**
             * get a list of all the potential settlements for a player
             */
            case SOCMessage.POTENTIALSETTLEMENTS:
                handlePOTENTIALSETTLEMENTS((SOCPotentialSettlements) mes);

                break;

            /**
             * handle the change face message
             */
            case SOCMessage.CHANGEFACE:
                handleCHANGEFACE((SOCChangeFace) mes);

                break;

            /**
             * handle the reject connection message
             */
            case SOCMessage.REJECTCONNECTION:
                handleREJECTCONNECTION((SOCRejectConnection) mes);

                break;

            /**
             * handle the longest road message
             */
            case SOCMessage.LONGESTROAD:
                handleLONGESTROAD((SOCLongestRoad) mes);

                break;

            /**
             * handle the largest army message
             */
            case SOCMessage.LARGESTARMY:
                handleLARGESTARMY((SOCLargestArmy) mes);

                break;

            /**
             * handle the seat lock state message
             */
            case SOCMessage.SETSEATLOCK:
                handleSETSEATLOCK((SOCSetSeatLock) mes);

                break;
            }
        }
        catch (Exception e)
        {
            System.out.println("SOCPlayerClient treat ERROR - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * handle the "status message" message
     * @param mes  the message
     */
    protected void handleSTATUSMESSAGE(SOCStatusMessage mes)
    {
        status.setText(mes.getStatus());
    }

    /**
     * handle the "join authorization" message
     * @param mes  the message
     */
    protected void handleJOINAUTH(SOCJoinAuth mes)
    {
        nick.setEditable(false);
        pass.setText("");
        pass.setEditable(false);
        gotPassword = true;

        ChannelFrame cf = new ChannelFrame(mes.getChannel(), this);
        cf.setVisible(true);
        channels.put(mes.getChannel(), cf);
    }

    /**
     * handle the "join channel" message
     * @param mes  the message
     */
    protected void handleJOIN(SOCJoin mes)
    {
        ChannelFrame fr;
        fr = (ChannelFrame) channels.get(mes.getChannel());
        fr.print("*** " + mes.getNickname() + " has joined this channel.\n");
        fr.addMember(mes.getNickname());
    }

    /**
     * handle the "members" message
     * @param mes  the message
     */
    protected void handleMEMBERS(SOCMembers mes)
    {
        ChannelFrame fr;
        fr = (ChannelFrame) channels.get(mes.getChannel());

        Enumeration membersEnum = (mes.getMembers()).elements();

        while (membersEnum.hasMoreElements())
        {
            fr.addMember((String) membersEnum.nextElement());
        }

        fr.began();
    }

    /**
     * handle the "new channel" message
     * @param mes  the message
     */
    protected void handleNEWCHANNEL(SOCNewChannel mes)
    {
        addToList(mes.getChannel(), chlist);
    }

    /**
     * handle the "list of channels" message
     * @param mes  the message
     */
    protected void handleCHANNELS(SOCChannels mes)
    {
        //
        // this message indicates that we're connected to the server
        //
        cardLayout.show(this, MAIN_PANEL);
        validate();
        
        status.setText("Login by entering nickname and then joining a channel or game.");

        Enumeration channelsEnum = (mes.getChannels()).elements();

        while (channelsEnum.hasMoreElements())
        {
            addToList((String) channelsEnum.nextElement(), chlist);
        }
    }

    /**
     * handle a broadcast text message
     * @param mes  the message
     */
    protected void handleBCASTTEXTMSG(SOCBCastTextMsg mes)
    {
        ChannelFrame fr;
        Enumeration channelKeysEnum = channels.keys();

        while (channelKeysEnum.hasMoreElements())
        {
            fr = (ChannelFrame) channels.get(channelKeysEnum.nextElement());
            fr.print("::: " + mes.getText() + " :::");
        }

        SOCPlayerInterface pi;
        Enumeration playerInterfaceKeysEnum = playerInterfaces.keys();

        while (playerInterfaceKeysEnum.hasMoreElements())
        {
            pi = (SOCPlayerInterface) playerInterfaces.get(playerInterfaceKeysEnum.nextElement());
            pi.chatPrint("::: " + mes.getText() + " :::");
        }
    }

    /**
     * handle a text message
     * @param mes  the message
     */
    protected void handleTEXTMSG(SOCTextMsg mes)
    {
        ChannelFrame fr;
        fr = (ChannelFrame) channels.get(mes.getChannel());

        if (fr != null)
        {
            if (!onIgnoreList(mes.getNickname()))
            {
                fr.print(mes.getNickname() + ": " + mes.getText());
            }
        }
    }

    /**
     * handle the "leave channel" message
     * @param mes  the message
     */
    protected void handleLEAVE(SOCLeave mes)
    {
        ChannelFrame fr;
        fr = (ChannelFrame) channels.get(mes.getChannel());
        fr.print("*** " + mes.getNickname() + " left.\n");
        fr.deleteMember(mes.getNickname());
    }

    /**
     * handle the "delete channel" message
     * @param mes  the message
     */
    protected void handleDELETECHANNEL(SOCDeleteChannel mes)
    {
        deleteFromList(mes.getChannel(), chlist);
    }

    /**
     * handle the "list of games" message
     * @param mes  the message
     */
    protected void handleGAMES(SOCGames mes)
    {
        Enumeration gamesEnum = (mes.getGames()).elements();

        while (gamesEnum.hasMoreElements())
        {
            addToGameList((String) gamesEnum.nextElement());
        }
    }

    /**
     * handle the "join game authorization" message
     * @param mes  the message
     */
    protected void handleJOINGAMEAUTH(SOCJoinGameAuth mes)
    {
        nick.setEditable(false);
        pass.setEditable(false);
        pass.setText("");
        gotPassword = true;

        SOCGame ga = new SOCGame(mes.getGame());

        if (ga != null)
        {
            SOCPlayerInterface pi = new SOCPlayerInterface(mes.getGame(), this, ga);
            pi.setVisible(true);
            playerInterfaces.put(mes.getGame(), pi);
            games.put(mes.getGame(), ga);
        }
    }

    /**
     * handle the "join game" message
     * @param mes  the message
     */
    protected void handleJOINGAME(SOCJoinGame mes)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
        pi.print("*** " + mes.getNickname() + " has joined this game.\n");
    }

    /**
     * handle the "leave game" message
     * @param mes  the message
     */
    protected void handleLEAVEGAME(SOCLeaveGame mes)
    {
        String gn = mes.getGame();
        SOCGame ga = (SOCGame) games.get(gn);

        if (ga != null)
        {
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(gn);
            SOCPlayer player = ga.getPlayer(mes.getNickname());

            if (player != null)
            {
                //
                //  This user was not a spectator
                //
                pi.removePlayer(player.getPlayerNumber());
                ga.removePlayer(mes.getNickname());
            }
        }
    }

    /**
     * handle the "new game" message
     * @param mes  the message
     */
    protected void handleNEWGAME(SOCNewGame mes)
    {
        addToGameList(mes.getGame());
    }

    /**
     * handle the "delete game" message
     * @param mes  the message
     */
    protected void handleDELETEGAME(SOCDeleteGame mes)
    {
        deleteFromGameList(mes.getGame());
    }

    /**
     * handle the "game members" message
     * @param mes  the message
     */
    protected void handleGAMEMEMBERS(SOCGameMembers mes)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
        pi.began();
    }

    /**
     * handle the "game stats" message
     */
    protected void handleGAMESTATS(SOCGameStats mes)
    {
        updateGameStats(mes.getGame(), mes.getScores(), mes.getRobotSeats());
    }

    /**
     * handle the "game text message" message
     * @param mes  the message
     */
    protected void handleGAMETEXTMSG(SOCGameTextMsg mes)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

        if (pi != null)
        {
            if (mes.getNickname().equals("Server"))
            {
                pi.print("* " + mes.getText());
            }
            else
            {
                if (!onIgnoreList(mes.getNickname()))
                {
                    pi.chatPrint(mes.getNickname() + ": " + mes.getText());
                }
            }
        }
    }

    /**
     * handle the "player sitting down" message
     * @param mes  the message
     */
    protected void handleSITDOWN(SOCSitDown mes)
    {
        /**
         * tell the game that a player is sitting
         */
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            ga.takeMonitor();

            try
            {
                ga.addPlayer(mes.getNickname(), mes.getPlayerNumber());

                /**
                 * set the robot flag
                 */
                ga.getPlayer(mes.getPlayerNumber()).setRobotFlag(mes.isRobot());
            }
            catch (Exception e)
            {
                ga.releaseMonitor();
                System.out.println("Exception caught - " + e);
                e.printStackTrace();
            }

            ga.releaseMonitor();

            /**
             * tell the GUI that a player is sitting
             */
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            pi.addPlayer(mes.getNickname(), mes.getPlayerNumber());

            /**
             * let the board panel find our player object if we sat down
             */
            if (nickname.equals(mes.getNickname()))
            {
                pi.getBoardPanel().setPlayer();

                /**
                 * chenge the face (this is so that old faces don't 'stick')
                 */
                ga.getPlayer(mes.getPlayerNumber()).setFaceId(1);
                changeFace(ga, 1);
            }

            /**
             * update the hand panel
             */
            SOCHandPanel hp = pi.getPlayerHandPanel(mes.getPlayerNumber());
            hp.updateValue(SOCHandPanel.ROADS);
            hp.updateValue(SOCHandPanel.SETTLEMENTS);
            hp.updateValue(SOCHandPanel.CITIES);
            hp.updateValue(SOCHandPanel.NUMKNIGHTS);
            hp.updateValue(SOCHandPanel.VICTORYPOINTS);
            hp.updateValue(SOCHandPanel.LONGESTROAD);
            hp.updateValue(SOCHandPanel.LARGESTARMY);

            if (nickname.equals(mes.getNickname()))
            {
                hp.updateValue(SOCHandPanel.CLAY);
                hp.updateValue(SOCHandPanel.ORE);
                hp.updateValue(SOCHandPanel.SHEEP);
                hp.updateValue(SOCHandPanel.WHEAT);
                hp.updateValue(SOCHandPanel.WOOD);
                hp.updateDevCards();
            }
            else
            {
                hp.updateValue(SOCHandPanel.NUMRESOURCES);
                hp.updateValue(SOCHandPanel.NUMDEVCARDS);
            }
        }
    }

    /**
     * handle the "board layout" message
     * @param mes  the message
     */
    protected void handleBOARDLAYOUT(SOCBoardLayout mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCBoard bd = ga.getBoard();
            bd.setHexLayout(mes.getHexLayout());
            bd.setNumberLayout(mes.getNumberLayout());
            bd.setRobberHex(mes.getRobberHex());

            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            pi.getBoardPanel().repaint();
        }
    }

    /**
     * handle the "start game" message
     * @param mes  the message
     */
    protected void handleSTARTGAME(SOCStartGame mes)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
        pi.startGame();
    }

    /**
     * handle the "game state" message
     * @param mes  the message
     */
    protected void handleGAMESTATE(SOCGameState mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            if (ga.getGameState() == ga.NEW && mes.getState() != ga.NEW)
            {
                pi.startGame();
            }
            
            ga.setGameState(mes.getState());

            pi.getBoardPanel().updateMode();
            pi.getBuildingPanel().updateButtonStatus();
            pi.getBoardPanel().repaint();

            SOCPlayer ourPlayerData = ga.getPlayer(nickname);

            if (ourPlayerData != null)
            {
                if (ourPlayerData.getPlayerNumber() == ga.getCurrentPlayerNumber())
                {
                    if (mes.getState() == SOCGame.WAITING_FOR_DISCOVERY)
                    {
                        pi.showDiscoveryDialog();
                    }

                    if (mes.getState() == SOCGame.WAITING_FOR_MONOPOLY)
                    {
                        pi.showMonopolyDialog();
                    }
                }
            }
        }
    }

    /**
     * handle the "set turn" message
     * @param mes  the message
     */
    protected void handleSETTURN(SOCSetTurn mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setCurrentPlayerNumber(mes.getPlayerNumber());

            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            pi.getBoardPanel().repaint();

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                pi.getPlayerHandPanel(i).updateTakeOverButton();
            }
        }
    }

    /**
     * handle the "first player" message
     * @param mes  the message
     */
    protected void handleFIRSTPLAYER(SOCFirstPlayer mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setFirstPlayer(mes.getPlayerNumber());
        }
    }

    /**
     * handle the "turn" message
     * @param mes  the message
     */
    protected void handleTURN(SOCTurn mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            /**
             * check if this is the first player
             */
            if (ga.getFirstPlayer() == -1)
            {
                ga.setFirstPlayer(mes.getPlayerNumber());
            }

            ga.setCurrentDice(0);
            ga.setCurrentPlayerNumber(mes.getPlayerNumber());
            ga.getPlayer(mes.getPlayerNumber()).getDevCards().newToOld();

            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            pi.getPlayerHandPanel(mes.getPlayerNumber()).updateDevCards();

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                pi.getPlayerHandPanel(i).updateTakeOverButton();
            }

            pi.getBoardPanel().updateMode();
            pi.getBoardPanel().repaint();
        }
    }

    /**
     * handle the "player information" message
     * @param mes  the message
     */
    protected void handlePLAYERELEMENT(SOCPlayerElement mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayer pl = ga.getPlayer(mes.getPlayerNumber());
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

            switch (mes.getElementType())
            {
            case SOCPlayerElement.ROADS:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.setNumPieces(SOCPlayingPiece.ROAD, mes.getValue());

                    break;

                case SOCPlayerElement.GAIN:
                    pl.setNumPieces(SOCPlayingPiece.ROAD, pl.getNumPieces(SOCPlayingPiece.ROAD) + mes.getValue());

                    break;

                case SOCPlayerElement.LOSE:
                    pl.setNumPieces(SOCPlayingPiece.ROAD, pl.getNumPieces(SOCPlayingPiece.ROAD) - mes.getValue());

                    break;
                }

                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.ROADS);

                break;

            case SOCPlayerElement.SETTLEMENTS:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.setNumPieces(SOCPlayingPiece.SETTLEMENT, mes.getValue());

                    break;

                case SOCPlayerElement.GAIN:
                    pl.setNumPieces(SOCPlayingPiece.SETTLEMENT, pl.getNumPieces(SOCPlayingPiece.SETTLEMENT) + mes.getValue());

                    break;

                case SOCPlayerElement.LOSE:
                    pl.setNumPieces(SOCPlayingPiece.SETTLEMENT, pl.getNumPieces(SOCPlayingPiece.SETTLEMENT) - mes.getValue());

                    break;
                }

                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.SETTLEMENTS);

                break;

            case SOCPlayerElement.CITIES:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.setNumPieces(SOCPlayingPiece.CITY, mes.getValue());

                    break;

                case SOCPlayerElement.GAIN:
                    pl.setNumPieces(SOCPlayingPiece.CITY, pl.getNumPieces(SOCPlayingPiece.CITY) + mes.getValue());

                    break;

                case SOCPlayerElement.LOSE:
                    pl.setNumPieces(SOCPlayingPiece.CITY, pl.getNumPieces(SOCPlayingPiece.CITY) - mes.getValue());

                    break;
                }

                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.CITIES);

                break;

            case SOCPlayerElement.NUMKNIGHTS:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.setNumKnights(mes.getValue());

                    break;

                case SOCPlayerElement.GAIN:
                    pl.setNumKnights(pl.getNumKnights() + mes.getValue());

                    break;

                case SOCPlayerElement.LOSE:
                    pl.setNumKnights(pl.getNumKnights() - mes.getValue());

                    break;
                }

                ga.updateLargestArmy();
                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMKNIGHTS);

                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.LARGESTARMY);
                    pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.VICTORYPOINTS);
                }

                break;

            case SOCPlayerElement.CLAY:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.getResources().setAmount(mes.getValue(), SOCResourceConstants.CLAY);

                    break;

                case SOCPlayerElement.GAIN:
                    pl.getResources().add(mes.getValue(), SOCResourceConstants.CLAY);

                    break;

                case SOCPlayerElement.LOSE:

                    if (pl.getResources().getAmount(SOCResourceConstants.CLAY) >= mes.getValue())
                    {
                        pl.getResources().subtract(mes.getValue(), SOCResourceConstants.CLAY);
                    }
                    else
                    {
                        pl.getResources().subtract(mes.getValue() - pl.getResources().getAmount(SOCResourceConstants.CLAY), SOCResourceConstants.UNKNOWN);
                        pl.getResources().setAmount(0, SOCResourceConstants.CLAY);
                    }

                    break;
                }

                //if (true) {
                if (nickname.equals(pl.getName()))
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.CLAY);
                }
                else
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMRESOURCES);
                }

                break;

            case SOCPlayerElement.ORE:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.getResources().setAmount(mes.getValue(), SOCResourceConstants.ORE);

                    break;

                case SOCPlayerElement.GAIN:
                    pl.getResources().add(mes.getValue(), SOCResourceConstants.ORE);

                    break;

                case SOCPlayerElement.LOSE:

                    if (pl.getResources().getAmount(SOCResourceConstants.ORE) >= mes.getValue())
                    {
                        pl.getResources().subtract(mes.getValue(), SOCResourceConstants.ORE);
                    }
                    else
                    {
                        pl.getResources().subtract(mes.getValue() - pl.getResources().getAmount(SOCResourceConstants.ORE), SOCResourceConstants.UNKNOWN);
                        pl.getResources().setAmount(0, SOCResourceConstants.ORE);
                    }

                    break;
                }

                //if (true) {
                if (nickname.equals(pl.getName()))
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.ORE);
                }
                else
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMRESOURCES);
                }

                break;

            case SOCPlayerElement.SHEEP:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.getResources().setAmount(mes.getValue(), SOCResourceConstants.SHEEP);

                    break;

                case SOCPlayerElement.GAIN:
                    pl.getResources().add(mes.getValue(), SOCResourceConstants.SHEEP);

                    break;

                case SOCPlayerElement.LOSE:

                    if (pl.getResources().getAmount(SOCResourceConstants.SHEEP) >= mes.getValue())
                    {
                        pl.getResources().subtract(mes.getValue(), SOCResourceConstants.SHEEP);
                    }
                    else
                    {
                        pl.getResources().subtract(mes.getValue() - pl.getResources().getAmount(SOCResourceConstants.SHEEP), SOCResourceConstants.UNKNOWN);
                        pl.getResources().setAmount(0, SOCResourceConstants.SHEEP);
                    }

                    break;
                }

                //if (true) {
                if (nickname.equals(pl.getName()))
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.SHEEP);
                }
                else
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMRESOURCES);
                }

                break;

            case SOCPlayerElement.WHEAT:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.getResources().setAmount(mes.getValue(), SOCResourceConstants.WHEAT);

                    break;

                case SOCPlayerElement.GAIN:
                    pl.getResources().add(mes.getValue(), SOCResourceConstants.WHEAT);

                    break;

                case SOCPlayerElement.LOSE:

                    if (pl.getResources().getAmount(SOCResourceConstants.WHEAT) >= mes.getValue())
                    {
                        pl.getResources().subtract(mes.getValue(), SOCResourceConstants.WHEAT);
                    }
                    else
                    {
                        pl.getResources().subtract(mes.getValue() - pl.getResources().getAmount(SOCResourceConstants.WHEAT), SOCResourceConstants.UNKNOWN);
                        pl.getResources().setAmount(0, SOCResourceConstants.WHEAT);
                    }

                    break;
                }

                //if (true) {
                if (nickname.equals(pl.getName()))
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.WHEAT);
                }
                else
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMRESOURCES);
                }

                break;

            case SOCPlayerElement.WOOD:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:
                    pl.getResources().setAmount(mes.getValue(), SOCResourceConstants.WOOD);

                    break;

                case SOCPlayerElement.GAIN:
                    pl.getResources().add(mes.getValue(), SOCResourceConstants.WOOD);

                    break;

                case SOCPlayerElement.LOSE:

                    if (pl.getResources().getAmount(SOCResourceConstants.WOOD) >= mes.getValue())
                    {
                        pl.getResources().subtract(mes.getValue(), SOCResourceConstants.WOOD);
                    }
                    else
                    {
                        pl.getResources().subtract(mes.getValue() - pl.getResources().getAmount(SOCResourceConstants.WOOD), SOCResourceConstants.UNKNOWN);
                        pl.getResources().setAmount(0, SOCResourceConstants.WOOD);
                    }

                    break;
                }

                //if (true) {
                if (nickname.equals(pl.getName()))
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.WOOD);
                }
                else
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMRESOURCES);
                }

                break;

            case SOCPlayerElement.UNKNOWN:

                switch (mes.getAction())
                {
                case SOCPlayerElement.SET:

                    /**
                     * set the ammount of unknown resources
                     */
                    pl.getResources().setAmount(mes.getValue(), SOCResourceConstants.UNKNOWN);

                    break;

                case SOCPlayerElement.GAIN:
                    pl.getResources().add(mes.getValue(), SOCResourceConstants.UNKNOWN);

                    break;

                case SOCPlayerElement.LOSE:

                    SOCResourceSet rs = pl.getResources();

                    /**
                     * first convert known resources to unknown resources
                     */
                    rs.add(rs.getAmount(SOCResourceConstants.CLAY), SOCResourceConstants.UNKNOWN);
                    rs.setAmount(0, SOCResourceConstants.CLAY);
                    rs.add(rs.getAmount(SOCResourceConstants.ORE), SOCResourceConstants.UNKNOWN);
                    rs.setAmount(0, SOCResourceConstants.ORE);
                    rs.add(rs.getAmount(SOCResourceConstants.SHEEP), SOCResourceConstants.UNKNOWN);
                    rs.setAmount(0, SOCResourceConstants.SHEEP);
                    rs.add(rs.getAmount(SOCResourceConstants.WHEAT), SOCResourceConstants.UNKNOWN);
                    rs.setAmount(0, SOCResourceConstants.WHEAT);
                    rs.add(rs.getAmount(SOCResourceConstants.WOOD), SOCResourceConstants.UNKNOWN);
                    rs.setAmount(0, SOCResourceConstants.WOOD);

                    /**
                     * then remove the unknown resources
                     */
                    pl.getResources().subtract(mes.getValue(), SOCResourceConstants.UNKNOWN);

                    break;
                }

                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMRESOURCES);

                break;
            }

            if ((nickname.equals(pl.getName())) && (ga.getGameState() != SOCGame.NEW))
            {
                pi.getBuildingPanel().updateButtonStatus();
            }
        }
    }

    /**
     * handle "resource count" message
     * @param mes  the message
     */
    protected void handleRESOURCECOUNT(SOCResourceCount mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayer pl = ga.getPlayer(mes.getPlayerNumber());
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

            if (mes.getCount() != pl.getResources().getTotal())
            {
                SOCResourceSet rsrcs = pl.getResources();

                if (D.ebugOn)
                {
                    //pi.print(">>> RESOURCE COUNT ERROR: "+mes.getCount()+ " != "+rsrcs.getTotal());
                }

                //
                //  fix it
                //
                if (!pl.getName().equals(nickname))
                {
                    rsrcs.clear();
                    rsrcs.setAmount(mes.getCount(), SOCResourceConstants.UNKNOWN);
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMRESOURCES);
                }
            }
        }
    }

    /**
     * handle the "dice result" message
     * @param mes  the message
     */
    protected void handleDICERESULT(SOCDiceResult mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            ga.setCurrentDice(mes.getResult());
            pi.getBoardPanel().repaint();
        }
    }

    /**
     * handle the "put piece" message
     * @param mes  the message
     */
    protected void handlePUTPIECE(SOCPutPiece mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayer pl = ga.getPlayer(mes.getPlayerNumber());
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

            switch (mes.getPieceType())
            {
            case SOCPlayingPiece.ROAD:

                SOCRoad rd = new SOCRoad(pl, mes.getCoordinates());
                ga.putPiece(rd);
                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.ROADS);

                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.LONGESTROAD);
                    pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.VICTORYPOINTS);
                }

                break;

            case SOCPlayingPiece.SETTLEMENT:

                SOCSettlement se = new SOCSettlement(pl, mes.getCoordinates());
                ga.putPiece(se);
                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.SETTLEMENTS);

                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.LONGESTROAD);
                    pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.VICTORYPOINTS);
                }

                /**
                 * if this is the second initial settlement, then update the resource display
                 */
                if (nickname.equals(pl.getName()))
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.CLAY);
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.ORE);
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.SHEEP);
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.WHEAT);
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.WOOD);
                }
                else
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMRESOURCES);
                }

                break;

            case SOCPlayingPiece.CITY:

                SOCCity ci = new SOCCity(pl, mes.getCoordinates());
                ga.putPiece(ci);
                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.SETTLEMENTS);
                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.CITIES);

                break;
            }

            pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.VICTORYPOINTS);
            pi.getBoardPanel().repaint();
            pi.getBuildingPanel().updateButtonStatus();
        }
    }

    /**
     * handle the "robber moved" message
     * @param mes  the message
     */
    protected void handleMOVEROBBER(SOCMoveRobber mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

            /**
             * Note: Don't call ga.moveRobber() because that will call the
             * functions to do the stealing.  We just want to say where
             * the robber moved without seeing if something was stolen.
             */
            ga.getBoard().setRobberHex(mes.getCoordinates());
            pi.getBoardPanel().repaint();
        }
    }

    /**
     * handle the "discard request" message
     * @param mes  the message
     */
    protected void handleDISCARDREQUEST(SOCDiscardRequest mes)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
        pi.showDiscardDialog(mes.getNumberOfDiscards());
    }

    /**
     * handle the "choose player request" message
     * @param mes  the message
     */
    protected void handleCHOOSEPLAYERREQUEST(SOCChoosePlayerRequest mes)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
        int[] choices = new int[SOCGame.MAXPLAYERS];
        boolean[] ch = mes.getChoices();
        int count = 0;

        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
        {
            if (ch[i])
            {
                choices[count] = i;
                count++;
            }
        }

        pi.choosePlayer(count, choices);
    }

    /**
     * handle the "make offer" message
     * @param mes  the message
     */
    protected void handleMAKEOFFER(SOCMakeOffer mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            SOCTradeOffer offer = mes.getOffer();
            ga.getPlayer(offer.getFrom()).setCurrentOffer(offer);
            pi.getPlayerHandPanel(offer.getFrom()).updateCurrentOffer();
        }
    }

    /**
     * handle the "clear offer" message
     * @param mes  the message
     */
    protected void handleCLEAROFFER(SOCClearOffer mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            ga.getPlayer(mes.getPlayerNumber()).setCurrentOffer(null);
            pi.getPlayerHandPanel(mes.getPlayerNumber()).updateCurrentOffer();
        }
    }

    /**
     * handle the "reject offer" message
     * @param mes  the message
     */
    protected void handleREJECTOFFER(SOCRejectOffer mes)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
        pi.getPlayerHandPanel(mes.getPlayerNumber()).rejectOffer();
    }

    /**
     * handle the "clear trade message" message
     * @param mes  the message
     */
    protected void handleCLEARTRADEMSG(SOCClearTradeMsg mes)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
        pi.getPlayerHandPanel(mes.getPlayerNumber()).clearTradeMsg();
    }

    /**
     * handle the "number of development cards" message
     * @param mes  the message
     */
    protected void handleDEVCARDCOUNT(SOCDevCardCount mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            ga.setNumDevCards(mes.getNumDevCards());
        }
    }

    /**
     * handle the "development card action" message
     * @param mes  the message
     */
    protected void handleDEVCARD(SOCDevCard mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

            switch (mes.getAction())
            {
            case SOCDevCard.DRAW:
                player.getDevCards().add(1, SOCDevCardSet.NEW, mes.getCardType());

                break;

            case SOCDevCard.PLAY:
                player.getDevCards().subtract(1, SOCDevCardSet.OLD, mes.getCardType());

                break;

            case SOCDevCard.ADDOLD:
                player.getDevCards().add(1, SOCDevCardSet.OLD, mes.getCardType());

                break;

            case SOCDevCard.ADDNEW:
                player.getDevCards().add(1, SOCDevCardSet.NEW, mes.getCardType());

                break;
            }

            SOCPlayer ourPlayerData = ga.getPlayer(nickname);

            if (ourPlayerData != null)
            {
                //if (true) {
                if (mes.getPlayerNumber() == ourPlayerData.getPlayerNumber())
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateDevCards();
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.VICTORYPOINTS);
                }
                else
                {
                    pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMDEVCARDS);
                }
            }
            else
            {
                pi.getPlayerHandPanel(mes.getPlayerNumber()).updateValue(SOCHandPanel.NUMDEVCARDS);
            }
        }
    }

    /**
     * handle the "set played dev card flag" message
     * @param mes  the message
     */
    protected void handleSETPLAYEDDEVCARD(SOCSetPlayedDevCard mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
            player.setPlayedDevCard(mes.hasPlayedDevCard());
        }
    }

    /**
     * handle the "list of potential settlements" message
     * @param mes  the message
     */
    protected void handlePOTENTIALSETTLEMENTS(SOCPotentialSettlements mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
            player.setPotentialSettlements(mes.getPotentialSettlements());
        }
    }

    /**
     * handle the "change face" message
     * @param mes  the message
     */
    protected void handleCHANGEFACE(SOCChangeFace mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());
            player.setFaceId(mes.getFaceId());
            pi.changeFace(mes.getPlayerNumber(), mes.getFaceId());
        }
    }

    /**
     * handle the "reject connection" message
     * @param mes  the message
     */
    protected void handleREJECTCONNECTION(SOCRejectConnection mes)
    {
        disconnect();
        
        messageLabel.setText(mes.getText());
        cardLayout.show(this, MESSAGE_PANEL);
        validate();
    }

    /**
     * handle the "longest road" message
     * @param mes  the message
     */
    protected void handleLONGESTROAD(SOCLongestRoad mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            if (mes.getPlayerNumber() == -1)
            {
                ga.setPlayerWithLongestRoad((SOCPlayer) null);
            }
            else
            {
                ga.setPlayerWithLongestRoad(ga.getPlayer(mes.getPlayerNumber()));
            }

            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.LONGESTROAD);
                pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.VICTORYPOINTS);
            }
        }
    }

    /**
     * handle the "largest army" message
     * @param mes  the message
     */
    protected void handleLARGESTARMY(SOCLargestArmy mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            if (mes.getPlayerNumber() == -1)
            {
                ga.setPlayerWithLargestArmy((SOCPlayer) null);
            }
            else
            {
                ga.setPlayerWithLargestArmy(ga.getPlayer(mes.getPlayerNumber()));
            }

            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.LARGESTARMY);
                pi.getPlayerHandPanel(i).updateValue(SOCHandPanel.VICTORYPOINTS);
            }
        }
    }

    /**
     * handle the "set seat lock" message
     * @param mes  the message
     */
    protected void handleSETSEATLOCK(SOCSetSeatLock mes)
    {
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            if (mes.getLockState() == true)
            {
                ga.lockSeat(mes.getPlayerNumber());
            }
            else
            {
                ga.unlockSeat(mes.getPlayerNumber());
            }

            SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(mes.getGame());

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                pi.getPlayerHandPanel(i).updateSeatLockButton();
                pi.getPlayerHandPanel(i).updateTakeOverButton();
            }
        }
    }

    /**
     * add a new game
     *
     * @param thing  the thing to add to the list
     */
    public void addToGameList(String thing)
    {
        // String gameName = thing + STATSPREFEX + "-- -- -- --]";
        String gameName = thing;

        if (gmlist.getItem(0).equals(" "))
        {
            gmlist.replaceItem(gameName, 0);
            gmlist.select(0);
        }
        else
        {
            gmlist.add(gameName, 0);
        }
    }

    /**
     * add a new channel or game, put it in the list in alphabetical order
     *
     * @param thing  the thing to add to the list
     * @param lst    the list
     */
    public void addToList(String thing, java.awt.List lst)
    {
        if (lst.getItem(0).equals(" "))
        {
            lst.replaceItem(thing, 0);
            lst.select(0);
        }
        else
        {
            lst.add(thing, 0);

            /*
               int i;
               for(i=lst.getItemCount()-1;i>=0;i--)
               if(lst.getItem(i).compareTo(thing)<0)
               break;
               lst.add(thing, i+1);
               if(lst.getSelectedIndex()==-1)
               lst.select(0);
             */
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param gameName DOCUMENT ME!
     * @param scores DOCUMENT ME!
     * @param robots DOCUMENT ME!
     */
    public void updateGameStats(String gameName, int[] scores, boolean[] robots)
    {
        //D.ebugPrintln("UPDATE GAME STATS FOR "+gameName);
        String testString = gameName + STATSPREFEX;

        for (int i = 0; i < gmlist.getItemCount(); i++)
        {
            if (gmlist.getItem(i).startsWith(testString))
            {
                String updatedString = gameName + STATSPREFEX;

                for (int pn = 0; pn < (SOCGame.MAXPLAYERS - 1); pn++)
                {
                    if (scores[pn] != -1)
                    {
                        if (robots[pn])
                        {
                            updatedString += "#";
                        }
                        else
                        {
                            updatedString += "o";
                        }

                        updatedString += (scores[pn] + " ");
                    }
                    else
                    {
                        updatedString += "-- ";
                    }
                }

                if (scores[SOCGame.MAXPLAYERS - 1] != -1)
                {
                    if (robots[SOCGame.MAXPLAYERS - 1])
                    {
                        updatedString += "#";
                    }
                    else
                    {
                        updatedString += "o";
                    }

                    updatedString += (scores[SOCGame.MAXPLAYERS - 1] + "]");
                }
                else
                {
                    updatedString += "--]";
                }

                gmlist.replaceItem(updatedString, i);

                break;
            }
        }
    }

    /**
     * delete a game from the list
     *
     * @param gameName   the game to remove
     */
    public void deleteFromGameList(String gameName)
    {
        //String testString = gameName + STATSPREFEX;
        String testString = gameName;

        if (gmlist.getItemCount() == 1)
        {
            if (gmlist.getItem(0).startsWith(testString))
            {
                gmlist.replaceItem(" ", 0);
                gmlist.deselect(0);
            }

            return;
        }

        for (int i = gmlist.getItemCount() - 1; i >= 0; i--)
        {
            if (gmlist.getItem(i).startsWith(testString))
            {
                gmlist.remove(i);
            }
        }

        if (gmlist.getSelectedIndex() == -1)
        {
            gmlist.select(gmlist.getItemCount() - 1);
        }
    }

    /**
     * delete a group
     *
     * @param thing   the thing to remove
     * @param lst     the list
     */
    public void deleteFromList(String thing, java.awt.List lst)
    {
        if (lst.getItemCount() == 1)
        {
            if (lst.getItem(0).equals(thing))
            {
                lst.replaceItem(" ", 0);
                lst.deselect(0);
            }

            return;
        }

        for (int i = lst.getItemCount() - 1; i >= 0; i--)
        {
            if (lst.getItem(i).equals(thing))
            {
                lst.remove(i);
            }
        }

        if (lst.getSelectedIndex() == -1)
        {
            lst.select(lst.getItemCount() - 1);
        }
    }

    /**
     * send a text message to a channel
     *
     * @param ch   the name of the channel
     * @param mes  the message
     */
    public void chSend(String ch, String mes)
    {
        if (!doLocalCommand(ch, mes))
        {
            put(SOCTextMsg.toCmd(ch, nickname, mes));
        }
    }

    /**
     * the user leaves the given channel
     *
     * @param ch  the name of the channel
     */
    public void leaveChannel(String ch)
    {
        channels.remove(ch);
        put(SOCLeave.toCmd(nickname, host, ch));
    }

    /**
     * disconnect from the net
     */
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
     * request to buy a development card
     *
     * @param ga     the game
     */
    public void buyDevCard(SOCGame ga)
    {
        put(SOCBuyCardRequest.toCmd(ga.getName()));
    }

    /**
     * request to build something
     *
     * @param ga     the game
     * @param piece  the type of piece from SOCPlayingPiece
     */
    public void buildRequest(SOCGame ga, int piece)
    {
        put(SOCBuildRequest.toCmd(ga.getName(), piece));
    }

    /**
     * request to cancel building something
     *
     * @param ga     the game
     * @param piece  the type of piece from SOCPlayingPiece
     */
    public void cancelBuildRequest(SOCGame ga, int piece)
    {
        put(SOCCancelBuildRequest.toCmd(ga.getName(), piece));
    }

    /**
     * put a piece on the board
     *
     * @param ga  the game where the action is taking place
     * @param pp  the piece being placed
     */
    public void putPiece(SOCGame ga, SOCPlayingPiece pp)
    {
        /**
         * send the command
         */
        put(SOCPutPiece.toCmd(ga.getName(), pp.getPlayer().getPlayerNumber(), pp.getType(), pp.getCoordinates()));
    }

    /**
     * the player wants to move the robber
     *
     * @param ga  the game
     * @param pl  the player
     * @param coord  where the player wants the robber
     */
    public void moveRobber(SOCGame ga, SOCPlayer pl, int coord)
    {
        put(SOCMoveRobber.toCmd(ga.getName(), pl.getPlayerNumber(), coord));
    }

    /**
     * send a text message to the people in the game
     *
     * @param ga   the game
     * @param me   the message
     */
    public void sendText(SOCGame ga, String me)
    {
        if (!doLocalCommand(ga, me))
        {
            put(SOCGameTextMsg.toCmd(ga.getName(), nickname, me));
        }
    }

    /**
     * the user leaves the given game
     *
     * @param ga   the game
     */
    public void leaveGame(SOCGame ga)
    {
        playerInterfaces.remove(ga.getName());
        games.remove(ga.getName());
        put(SOCLeaveGame.toCmd(nickname, host, ga.getName()));
    }

    /**
     * the user sits down to play
     *
     * @param ga   the game
     * @param pn   the number of the seat where the user wants to sit
     */
    public void sitDown(SOCGame ga, int pn)
    {
        put(SOCSitDown.toCmd(ga.getName(), "dummy", pn, false));
    }

    /**
     * the user is starting the game
     *
     * @param ga  the game
     */
    public void startGame(SOCGame ga)
    {
        put(SOCStartGame.toCmd(ga.getName()));
    }

    /**
     * the user rolls the dice
     *
     * @param ga  the game
     */
    public void rollDice(SOCGame ga)
    {
        put(SOCRollDice.toCmd(ga.getName()));
    }

    /**
     * the user is done with the turn
     *
     * @param ga  the game
     */
    public void endTurn(SOCGame ga)
    {
        put(SOCEndTurn.toCmd(ga.getName()));
    }

    /**
     * the user wants to discard
     *
     * @param ga  the game
     */
    public void discard(SOCGame ga, SOCResourceSet rs)
    {
        put(SOCDiscard.toCmd(ga.getName(), rs));
    }

    /**
     * the user chose a player to steal from
     *
     * @param ga  the game
     * @param pn  the player id
     */
    public void choosePlayer(SOCGame ga, int pn)
    {
        put(SOCChoosePlayer.toCmd(ga.getName(), pn));
    }

    /**
     * the user is rejecting the current offers
     *
     * @param ga  the game
     */
    public void rejectOffer(SOCGame ga)
    {
        put(SOCRejectOffer.toCmd(ga.getName(), ga.getPlayer(nickname).getPlayerNumber()));
    }

    /**
     * the user is accepting an offer
     *
     * @param ga  the game
     * @param from the number of the player that is making the offer
     */
    public void acceptOffer(SOCGame ga, int from)
    {
        put(SOCAcceptOffer.toCmd(ga.getName(), ga.getPlayer(nickname).getPlayerNumber(), from));
    }

    /**
     * the user is clearing an offer
     *
     * @param ga  the game
     */
    public void clearOffer(SOCGame ga)
    {
        put(SOCClearOffer.toCmd(ga.getName(), ga.getPlayer(nickname).getPlayerNumber()));
    }

    /**
     * the user wants to trade with the bank
     *
     * @param ga    the game
     * @param give  what is being offered
     * @param get   what the player wants
     */
    public void bankTrade(SOCGame ga, SOCResourceSet give, SOCResourceSet get)
    {
        put(SOCBankTrade.toCmd(ga.getName(), give, get));
    }

    /**
     * the user is making an offer to trade
     *
     * @param ga    the game
     * @param offer the trade offer
     */
    public void offerTrade(SOCGame ga, SOCTradeOffer offer)
    {
        put(SOCMakeOffer.toCmd(ga.getName(), offer));
    }

    /**
     * the user wants to play a development card
     *
     * @param ga  the game
     * @param dc  the type of development card
     */
    public void playDevCard(SOCGame ga, int dc)
    {
        put(SOCPlayDevCardRequest.toCmd(ga.getName(), dc));
    }

    /**
     * the user picked 2 resources to discover
     *
     * @param ga    the game
     * @param rscs  the resources
     */
    public void discoveryPick(SOCGame ga, SOCResourceSet rscs)
    {
        put(SOCDiscoveryPick.toCmd(ga.getName(), rscs));
    }

    /**
     * the user picked a resource to monopolize
     *
     * @param ga   the game
     * @param res  the resource
     */
    public void monopolyPick(SOCGame ga, int res)
    {
        put(SOCMonopolyPick.toCmd(ga.getName(), res));
    }

    /**
     * the user is changing the face image
     *
     * @param ga  the game
     * @param id  the image id
     */
    public void changeFace(SOCGame ga, int id)
    {
        put(SOCChangeFace.toCmd(ga.getName(), ga.getPlayer(nickname).getPlayerNumber(), id));
    }

    /**
     * the user is locking a seat
     *
     * @param ga  the game
     * @param pn  the seat number
     */
    public void lockSeat(SOCGame ga, int pn)
    {
        put(SOCSetSeatLock.toCmd(ga.getName(), pn, true));
    }

    /**
     * the user is unlocking a seat
     *
     * @param ga  the game
     * @param pn  the seat number
     */
    public void unlockSeat(SOCGame ga, int pn)
    {
        put(SOCSetSeatLock.toCmd(ga.getName(), pn, false));
    }

    /**
     * handle local client commands for channels
     *
     * @return true if a command was handled
     */
    public boolean doLocalCommand(String ch, String cmd)
    {
        ChannelFrame fr = (ChannelFrame) channels.get(ch);

        if (cmd.startsWith("\\ignore "))
        {
            String name = cmd.substring(8);
            addToIgnoreList(name);
            fr.print("* Ignoring " + name);
            fr.print("* Ignore list:");

            Enumeration enum1 = ignoreList.elements();

            while (enum1.hasMoreElements())
            {
                String s = (String) enum1.nextElement();
                fr.print("* " + s);
            }

            return true;
        }
        else if (cmd.startsWith("\\unignore "))
        {
            String name = cmd.substring(10);
            removeFromIgnoreList(name);
            fr.print("* Unignoring " + name);
            fr.print("* Ignore list:");

            Enumeration enum1 = ignoreList.elements();

            while (enum1.hasMoreElements())
            {
                String s = (String) enum1.nextElement();
                fr.print("* " + s);
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * handle local client commands for games
     *
     * @return true if a command was handled
     */
    public boolean doLocalCommand(SOCGame ga, String cmd)
    {
        SOCPlayerInterface pi = (SOCPlayerInterface) playerInterfaces.get(ga.getName());

        if (cmd.startsWith("\\ignore "))
        {
            String name = cmd.substring(8);
            addToIgnoreList(name);
            pi.print("* Ignoring " + name);
            pi.print("* Ignore list:");

            Enumeration enum1 = ignoreList.elements();

            while (enum1.hasMoreElements())
            {
                String s = (String) enum1.nextElement();
                pi.print("* " + s);
            }

            return true;
        }
        else if (cmd.startsWith("\\unignore "))
        {
            String name = cmd.substring(10);
            removeFromIgnoreList(name);
            pi.print("* Unignoring " + name);
            pi.print("* Ignore list:");

            Enumeration enum1 = ignoreList.elements();

            while (enum1.hasMoreElements())
            {
                String s = (String) enum1.nextElement();
                pi.print("* " + s);
            }

            return true;
        }
        else if (cmd.startsWith("\\clm-set "))
        {
            String name = cmd.substring(9).trim();
            pi.getBoardPanel().setOtherPlayer(ga.getPlayer(name));
            pi.getBoardPanel().setMode(SOCBoardPanel.CONSIDER_LM_SETTLEMENT);

            return true;
        }
        else if (cmd.startsWith("\\clm-road "))
        {
            String name = cmd.substring(10).trim();
            pi.getBoardPanel().setOtherPlayer(ga.getPlayer(name));
            pi.getBoardPanel().setMode(SOCBoardPanel.CONSIDER_LM_ROAD);

            return true;
        }
        else if (cmd.startsWith("\\clm-city "))
        {
            String name = cmd.substring(10).trim();
            pi.getBoardPanel().setOtherPlayer(ga.getPlayer(name));
            pi.getBoardPanel().setMode(SOCBoardPanel.CONSIDER_LM_CITY);

            return true;
        }
        else if (cmd.startsWith("\\clt-set "))
        {
            String name = cmd.substring(9).trim();
            pi.getBoardPanel().setOtherPlayer(ga.getPlayer(name));
            pi.getBoardPanel().setMode(SOCBoardPanel.CONSIDER_LT_SETTLEMENT);

            return true;
        }
        else if (cmd.startsWith("\\clt-road "))
        {
            String name = cmd.substring(10).trim();
            pi.getBoardPanel().setOtherPlayer(ga.getPlayer(name));
            pi.getBoardPanel().setMode(SOCBoardPanel.CONSIDER_LT_ROAD);

            return true;
        }
        else if (cmd.startsWith("\\clt-city "))
        {
            String name = cmd.substring(10).trim();
            pi.getBoardPanel().setOtherPlayer(ga.getPlayer(name));
            pi.getBoardPanel().setMode(SOCBoardPanel.CONSIDER_LT_CITY);

            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * @return true if name is on the ignore list
     */
    protected boolean onIgnoreList(String name)
    {
        D.ebugPrintln("onIgnoreList |" + name + "|");

        boolean result = false;
        Enumeration enum1 = ignoreList.elements();

        while (enum1.hasMoreElements())
        {
            String s = (String) enum1.nextElement();
            D.ebugPrintln("comparing |" + s + "| to |" + name + "|");

            if (s.equals(name))
            {
                result = true;
                D.ebugPrintln("match");

                break;
            }
        }

        return result;
    }

    /**
     * add this name to the ignore list
     *
     * @param name the name to add
     */
    protected void addToIgnoreList(String name)
    {
        name = name.trim();

        if (!onIgnoreList(name))
        {
            ignoreList.addElement(name);
        }
    }

    /**
     * remove this name from the ignore list
     *
     * @param name  the name to remove
     */
    protected void removeFromIgnoreList(String name)
    {
        name = name.trim();
        ignoreList.removeElement(name);
    }

    /**
     * send a command to the server with a message
     * asking a robot to show the debug info for
     * a possible move after a move has been made
     *
     * @param ga  the game
     * @param pname  the robot name
     * @param piece  the piece to consider
     */
    public void considerMove(SOCGame ga, String pname, SOCPlayingPiece piece)
    {
        String msg = pname + ":consider-move ";

        switch (piece.getType())
        {
        case SOCPlayingPiece.SETTLEMENT:
            msg += "settlement";

            break;

        case SOCPlayingPiece.ROAD:
            msg += "road";

            break;

        case SOCPlayingPiece.CITY:
            msg += "city";

            break;
        }

        msg += (" " + piece.getCoordinates());
        put(SOCGameTextMsg.toCmd(ga.getName(), nickname, msg));
    }

    /**
     * send a command to the server with a message
     * asking a robot to show the debug info for
     * a possible move before a move has been made
     *
     * @param ga  the game
     * @param pname  the robot name
     * @param piece  the piece to consider
     */
    public void considerTarget(SOCGame ga, String pname, SOCPlayingPiece piece)
    {
        String msg = pname + ":consider-target ";

        switch (piece.getType())
        {
        case SOCPlayingPiece.SETTLEMENT:
            msg += "settlement";

            break;

        case SOCPlayingPiece.ROAD:
            msg += "road";

            break;

        case SOCPlayingPiece.CITY:
            msg += "city";

            break;
        }

        msg += (" " + piece.getCoordinates());
        put(SOCGameTextMsg.toCmd(ga.getName(), nickname, msg));
    }

    /**
     * applet info
     */
    public String getAppletInfo()
    {
        return "SOCPlayerClient 0.9 by Robert S. Thomas.";
    }

    /** destroy the applet */
    public void destroy()
    {
        SOCLeaveAll leaveAllMes = new SOCLeaveAll();
        put(leaveAllMes.toCmd());

        String err = "Sorry, the applet has been destroyed. " + ((ex == null) ? "Load the page again." : ex.toString());

        for (Enumeration e = channels.elements(); e.hasMoreElements();)
        {
            ((ChannelFrame) e.nextElement()).over(err);
        }

        for (Enumeration e = playerInterfaces.elements(); e.hasMoreElements();)
        {
            ((SOCPlayerInterface) e.nextElement()).over(err);
        }
        
        disconnect();

        messageLabel.setText(err);
        cardLayout.show(this, MESSAGE_PANEL);
        validate();
    }

    /**
     * for stand-alones
     */
    public static void usage()
    {
        System.err.println("usage: java soc.client.SOCPlayerClient <host> <port>");
    }

    /**
     * for stand-alones
     */
    public static void main(String[] args)
    {
        SOCPlayerClient client = new SOCPlayerClient();
        
        if (args.length != 2)
        {
            usage();
            System.exit(1);
        }

        try {
            client.host = args[0];
            client.port = Integer.parseInt(args[1]);
        } catch (NumberFormatException x) {
            usage();
            System.err.println("Invalid port: " + args[1]);
            System.exit(1);
        }

        Frame frame = new Frame("SOCPlayerClient");
        frame.setBackground(new Color(Integer.parseInt("61AF71",16)));
        frame.setForeground(Color.black);
        // Add a listener for the close event
        frame.addWindowListener(client.createWindowAdapter());
        
        client.initVisualElements(); // after the background is set
        
        frame.add(client, BorderLayout.CENTER);
        frame.setSize(620, 400);
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
