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

import soc.game.SOCDevCardConstants;
import soc.game.SOCDevCardSet;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCTradeOffer;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This panel displays a player's information.
 * If the player is us, then more information is
 * displayed than in another player's hand panel.
 */
public class SOCHandPanel extends Panel implements ActionListener
{
    public static final int ROADS = 0;
    public static final int SETTLEMENTS = 1;
    public static final int CITIES = 2;
    public static final int NUMRESOURCES = 3;
    public static final int NUMDEVCARDS = 4;
    public static final int NUMKNIGHTS = 5;
    public static final int VICTORYPOINTS = 6;
    public static final int LONGESTROAD = 7;
    public static final int LARGESTARMY = 8;
    public static final int CLAY = 9;
    public static final int ORE = 10;
    public static final int SHEEP = 11;
    public static final int WHEAT = 12;
    public static final int WOOD = 13;
    protected static final int[] zero = { 0, 0, 0, 0, 0 };
    protected static final String SIT = "Sit Here";
    protected static final String START = "Start Game";
    protected static final String ROBOT = "Robot";
    protected static final String TAKEOVER = "Take Over";
    protected static final String LOCKSEAT = "Lock";
    protected static final String UNLOCKSEAT = "Unlock";
    protected static final String ROLL = "Roll";
    protected static final String QUIT = "Quit";
    protected static final String DONE = "Done";
    protected static final String CLEAR = "Clear";
    protected static final String SEND = "Send";
    protected static final String BANK = "Bank/Port";
    protected static final String CARD = "  Play Card  ";
    protected static final String GIVE = "I Give: ";
    protected static final String GET = "I Get: ";
    protected Button sitBut;
    protected Button robotBut;
    protected Button startBut;
    protected Button takeOverBut;
    protected Button seatLockBut;
    protected SOCFaceButton faceImg;
    protected Label pname;
    protected Label vpLab;
    protected ColorSquare vpSq;
    protected Label larmyLab;
    protected Label lroadLab;
    protected ColorSquare claySq;
    protected ColorSquare oreSq;
    protected ColorSquare sheepSq;
    protected ColorSquare wheatSq;
    protected ColorSquare woodSq;
    protected Label clayLab;
    protected Label oreLab;
    protected Label sheepLab;
    protected Label wheatLab;
    protected Label woodLab;
    protected ColorSquare settlementSq;
    protected ColorSquare citySq;
    protected ColorSquare roadSq;
    protected Label settlementLab;
    protected Label cityLab;
    protected Label roadLab;
    protected ColorSquare resourceSq;
    protected Label resourceLab;
    protected ColorSquare developmentSq;
    protected Label developmentLab;
    protected ColorSquare knightsSq;
    protected Label knightsLab;
    //protected Label cardLab; // no longer used?
    protected List cardList;
    protected Button playCardBut;
    protected SquaresPanel sqPanel;
    protected Label giveLab;
    protected Label getLab;
    protected Button sendBut;
    protected Button clearBut;
    protected Button bankBut;
    protected ColorSquare[] playerSend;
    protected Button rollBut;
    protected Button doneBut;
    protected Button quitBut;
    protected SOCPlayerInterface playerInterface;
    protected SOCPlayerClient client;
    protected SOCGame game;
    protected SOCPlayer player;
    protected boolean inPlay;
    protected int[] playerSendMap;
    protected TradeOfferPanel offer;

    /**
     * When this flag is true, the panel is interactive.
     */
    protected boolean interactive;

    /**
     * make a new hand panel
     *
     * @param pi  the interface that this panel is a part of
     * @param pl  the player associated with this panel
     * @param in  the interactive flag setting
     */
    public SOCHandPanel(SOCPlayerInterface pi, SOCPlayer pl, boolean in)
    {
        super(null);
        creation(pi, pl, in);
    }

    /**
     * make a new hand panel
     *
     * @param pi  the interface that this panel is a part of
     * @param pl  the player associated with this panel
     */
    public SOCHandPanel(SOCPlayerInterface pi, SOCPlayer pl)
    {
        this(pi, pl, true);
    }

    /**
     * Stuff to do when a SOCHandPanel is created
     *
     * @param pi   player interface
     * @param pl   the player data
     * @param in   the interactive flag setting
     */
    protected void creation(SOCPlayerInterface pi, SOCPlayer pl, boolean in)
    {
        playerInterface = pi;
        client = pi.getClient();
        game = pi.getGame();
        player = pl;
        interactive = in;

        setBackground(playerInterface.getPlayerColor(player.getPlayerNumber()));
        setForeground(Color.black);
        setFont(new Font("Helvetica", Font.PLAIN, 10));

        faceImg = new SOCFaceButton(playerInterface, player.getPlayerNumber());
        add(faceImg);

        pname = new Label();
        pname.setFont(new Font("Serif", Font.PLAIN, 14));
        add(pname);

        startBut = new Button(START);
        startBut.addActionListener(this);
        // this button always enabled
        add(startBut);

        vpLab = new Label("Points: ");
        add(vpLab);
        vpSq = new ColorSquare(ColorSquare.GREY, 0);
        add(vpSq);
        
        larmyLab = new Label("", Label.CENTER);
        larmyLab.setForeground(new Color(142, 45, 10));
        add(larmyLab);
        
        lroadLab = new Label("", Label.CENTER);
        lroadLab.setForeground(new Color(142, 45, 10));
        add(lroadLab);
        
        clayLab = new Label("Clay:");
        add(clayLab);
        claySq = new ColorSquare(ColorSquare.CLAY, 0);
        add(claySq);

        oreLab = new Label("Ore:");
        add(oreLab);
        oreSq = new ColorSquare(ColorSquare.ORE, 0);
        add(oreSq);

        sheepLab = new Label("Sheep:");
        add(sheepLab);
        sheepSq = new ColorSquare(ColorSquare.SHEEP, 0);
        add(sheepSq);
        
        wheatLab = new Label("Wheat:");
        add(wheatLab);
        wheatSq = new ColorSquare(ColorSquare.WHEAT, 0);
        add(wheatSq);

        woodLab = new Label("Wood:");
        add(woodLab);
        woodSq = new ColorSquare(ColorSquare.WOOD, 0);
        add(woodSq);

        //cardLab = new Label("Cards:");
        //add(cardLab);
        cardList = new List(0, false);
        add(cardList);

        roadSq = new ColorSquare(ColorSquare.GREY, 0);
        add(roadSq);
        roadLab = new Label("Roads:");
        add(roadLab);

        settlementSq = new ColorSquare(ColorSquare.GREY, 0);
        add(settlementSq);
        settlementLab = new Label("Stlmts:");
        add(settlementLab);
  
        citySq = new ColorSquare(ColorSquare.GREY, 0);
        add(citySq);
        cityLab = new Label("Cities:");
        add(cityLab);

        knightsLab = new Label("Knights: ");
        add(knightsLab);
        knightsSq = new ColorSquare(ColorSquare.GREY, 0);
        add(knightsSq);

        resourceLab = new Label("Resources: ");
        add(resourceLab);
        resourceSq = new ColorSquare(ColorSquare.GREY, 0);
        add(resourceSq);

        developmentLab = new Label("Dev. Cards: ");
        add(developmentLab);
        developmentSq = new ColorSquare(ColorSquare.GREY, 0);
        add(developmentSq);
        
        seatLockBut = new Button(UNLOCKSEAT);
        seatLockBut.addActionListener(this);
        seatLockBut.setEnabled(interactive);
        add(seatLockBut);

        takeOverBut = new Button(TAKEOVER);
        takeOverBut.addActionListener(this);
        takeOverBut.setEnabled(interactive);
        add(takeOverBut);

        sitBut = new Button(SIT);
        sitBut.addActionListener(this);
        sitBut.setEnabled(interactive);
        add(sitBut);

        robotBut = new Button(ROBOT);
        robotBut.addActionListener(this);
        robotBut.setEnabled(interactive);
        add(robotBut);

        playCardBut = new Button(CARD);
        playCardBut.addActionListener(this);
        playCardBut.setEnabled(interactive);
        add(playCardBut);
        
        giveLab = new Label(GIVE);
        add(giveLab);
        
        getLab = new Label(GET);
        add(getLab);

        sqPanel = new SquaresPanel(interactive);
        add(sqPanel);
        sqPanel.setVisible(false); // else it's visible in all (dunno why?)
        
        sendBut = new Button(SEND);
        sendBut.addActionListener(this);
        sendBut.setEnabled(interactive);
        add(sendBut);
        
        clearBut = new Button(CLEAR);
        clearBut.addActionListener(this);
        clearBut.setEnabled(interactive);
        add(clearBut);
        
        bankBut = new Button(BANK);
        bankBut.addActionListener(this);
        bankBut.setEnabled(interactive);
        add(bankBut);

        playerSend = new ColorSquare[SOCGame.MAXPLAYERS-1];
        playerSendMap = new int[SOCGame.MAXPLAYERS-1];

        // set the trade buttons correctly
        int cnt = 0;
        for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++)
        {
            if (pn != player.getPlayerNumber())
            {
                Color color = playerInterface.getPlayerColor(pn);
                playerSendMap[cnt] = pn;
                playerSend[cnt] = new ColorSquare(ColorSquare.CHECKBOX, true, color);
                playerSend[cnt].setColor(playerInterface.getPlayerColor(pn));
                playerSend[cnt].setBoolValue(true);
                add(playerSend[cnt]);
                cnt++;
            }
        }

        rollBut = new Button(ROLL);
        rollBut.addActionListener(this);
        rollBut.setEnabled(interactive);
        add(rollBut);
        
        doneBut = new Button(DONE);
        doneBut.addActionListener(this);
        doneBut.setEnabled(interactive);
        add(doneBut);

        quitBut = new Button(QUIT);
        quitBut.addActionListener(this);
        quitBut.setEnabled(interactive);
        add(quitBut);

        offer = new TradeOfferPanel(this, player.getPlayerNumber());
        offer.setVisible(false);
        add(offer);
        // set the starting state of the panel
        removePlayer();
    }

    /**
     * @return the player interface
     */
    public SOCPlayerInterface getPlayerInterface()
    {
        return playerInterface;
    }

    /**
     * @return the player
     */
    public SOCPlayer getPlayer()
    {
        return player;
    }

    /**
     * @return the client
     */
    public SOCPlayerClient getClient()
    {
        return client;
    }

    /**
     * @return the game
     */
    public SOCGame getGame()
    {
        return game;
    }

    /**
     * handle interaction
     */
    public void actionPerformed(ActionEvent e)
    {
        String target = e.getActionCommand();

        SOCPlayerClient client = playerInterface.getClient();
        SOCGame game = playerInterface.getGame();

        if (target == LOCKSEAT)
        {
            client.lockSeat(game, player.getPlayerNumber());
        }
        else if (target == UNLOCKSEAT)
        {
            client.unlockSeat(game, player.getPlayerNumber());
        }
        else if (target == TAKEOVER)
        {
            client.sitDown(game, player.getPlayerNumber());
        }
        else if (target == SIT)
        {
            client.sitDown(game, player.getPlayerNumber());
        }
        else if (target == START)
        {
            client.startGame(game);
        }
        else if (target == ROBOT)
        {
            // cf.cc.addRobot(cf.cname, playerNum);
        }
        else if (target == ROLL)
        {
            client.rollDice(game);
        }
        else if (target == QUIT)
        {
            playerInterface.leaveGame();
        }
        else if (target == DONE)
        {
            // sqPanel.setValues(zero, zero);
            client.endTurn(game);
        }
        else if (target == CLEAR)
        {
            sqPanel.setValues(zero, zero);

            if (game.getGameState() == SOCGame.PLAY1)
            {
                client.clearOffer(game);
            }
        }
        else if (target == BANK)
        {
            if (game.getGameState() == SOCGame.PLAY1)
            {
                int[] give = new int[5];
                int[] get = new int[5];
                sqPanel.getValues(give, get);
                client.clearOffer(game);

                SOCResourceSet giveSet = new SOCResourceSet(give[0], give[1], give[2], give[3], give[4], 0);
                SOCResourceSet getSet = new SOCResourceSet(get[0], get[1], get[2], get[3], get[4], 0);
                client.bankTrade(game, giveSet, getSet);
            }
        }
        else if (target == SEND)
        {
            if (game.getGameState() == SOCGame.PLAY1)
            {
                int[] give = new int[5];
                int[] get = new int[5];
                int giveSum = 0;
                int getSum = 0;
                sqPanel.getValues(give, get);

                for (int i = 0; i < 5; i++)
                {
                    giveSum += give[i];
                    getSum += get[i];
                }

                SOCResourceSet giveSet = new SOCResourceSet(give[0], give[1], give[2], give[3], give[4], 0);
                SOCResourceSet getSet = new SOCResourceSet(get[0], get[1], get[2], get[3], get[4], 0);

                if (!player.getResources().contains(giveSet))
                {
                    playerInterface.print("*** You can't offer what you don't have.");
                }
                else if ((giveSum == 0) || (getSum == 0))
                {
                    playerInterface.print("*** A trade must contain at least one resource card from each player.");
                }
                else
                {
                    // bool array elements begin as false
                    boolean[] to = new boolean[SOCGame.MAXPLAYERS];

                    if (game.getCurrentPlayerNumber() == player.getPlayerNumber())
                    {
                        for (int i = 0; i < (SOCGame.MAXPLAYERS - 1); i++)
                        {
                            if (playerSend[i].getBoolValue())
                            {
                                to[playerSendMap[i]] = true;
                            }
                        }
                    }
                    else
                    {
                        // can only offer to current player 
                        to[game.getCurrentPlayerNumber()] = true;
                    }

                    SOCTradeOffer tradeOffer =
                        new SOCTradeOffer(game.getName(),
                                          player.getPlayerNumber(),
                                          to, giveSet, getSet);
                    client.offerTrade(game, tradeOffer);
                }
            }
        }
        else if ((e.getSource() == cardList) || (target == CARD))
        {
            String item;
            int itemNum;

            item = cardList.getSelectedItem();
            itemNum = cardList.getSelectedIndex();

            if (item == null || item.length() == 0)
            {
                return;
            }

            if (game.getCurrentPlayerNumber() == player.getPlayerNumber())
            {
                if (item.equals("Knight"))
                {
                    if (game.canPlayKnight(player.getPlayerNumber()))
                    {
                        client.playDevCard(game, SOCDevCardConstants.KNIGHT);
                    }
                }
                else if (item.equals("Road Building"))
                {
                    if (game.canPlayRoadBuilding(player.getPlayerNumber()))
                    {
                        client.playDevCard(game, SOCDevCardConstants.ROADS);
                    }
                }
                else if (item.equals("Discovery"))
                {
                    if (game.canPlayDiscovery(player.getPlayerNumber()))
                    {
                        client.playDevCard(game, SOCDevCardConstants.DISC);
                    }
                }
                else if (item.equals("Monopoly"))
                {
                    if (game.canPlayMonopoly(player.getPlayerNumber()))
                    {
                        client.playDevCard(game, SOCDevCardConstants.MONO);
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void addSeatLockBut()
    {
        D.ebugPrintln("*** addSeatLockBut() ***");
        D.ebugPrintln("seatLockBut = " + seatLockBut);

            if (game.isSeatLocked(player.getPlayerNumber()))
            {
                seatLockBut.setLabel(UNLOCKSEAT);
            }
            else
            {
                seatLockBut.setLabel(UNLOCKSEAT);
            }

            seatLockBut.setVisible(true);

            //seatLockBut.repaint();
    }

    /**
     * DOCUMENT ME!
     */
    public void addTakeOverBut()
    {
        takeOverBut.setVisible(true);        
    }

    /**
     * DOCUMENT ME!
     */
    public void addSitButton()
    {
        if (player.getName() == null)
        {
            sitBut.setVisible(true);
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void addRobotButton()
    {
        robotBut.setVisible(true);
    }

    /**
     * Change the face image
     *
     * @param id  the id of the image
     */
    public void changeFace(int id)
    {
        faceImg.setFace(id);
    }


    /**
     * remove this player
     */
    public void removePlayer()
    {
        //D.ebugPrintln("REMOVE PLAYER");
        //D.ebugPrintln("NAME = "+player.getName());
        vpLab.setVisible(false);
        vpSq.setVisible(false);
        faceImg.setVisible(false);
        pname.setVisible(false);
        roadSq.setVisible(false);
        roadLab.setVisible(false);
        settlementLab.setVisible(false);
        settlementSq.setVisible(false);
        cityLab.setVisible(false);
        citySq.setVisible(false);
        knightsSq.setVisible(false);
        knightsLab.setVisible(false);

        offer.setVisible(false);

        larmyLab.setVisible(false);
        lroadLab.setVisible(false);

        if (game.getPlayer(client.getNickname()) == null &&
            game.getGameState() == game.NEW)
       {
           sitBut.setVisible(true);
       }

        /* This is our hand */
        claySq.setVisible(false);
        clayLab.setVisible(false);
        oreSq.setVisible(false);
        oreLab.setVisible(false);
        sheepSq.setVisible(false);
        sheepLab.setVisible(false);
        wheatSq.setVisible(false);
        wheatLab.setVisible(false);
        woodSq.setVisible(false);
        woodLab.setVisible(false);
            
        //cardLab.setVisible(false);
        cardList.setVisible(false);
        playCardBut.setVisible(false);

        giveLab.setVisible(false);
        getLab.setVisible(false);
        sqPanel.setVisible(false);
        sendBut.setVisible(false);
        clearBut.setVisible(false);
        bankBut.setVisible(false);

        for (int i = 0; i < (SOCGame.MAXPLAYERS - 1); i++)
        {
            playerSend[i].setVisible(false);
        }

        rollBut.setVisible(false);
        doneBut.setVisible(false);
        quitBut.setVisible(false);

        /* other player's hand */
        resourceLab.setVisible(false);
        resourceSq.setVisible(false);
        developmentLab.setVisible(false);
        developmentSq.setVisible(false);

        removeTakeOverBut();
        removeSeatLockBut();

        inPlay = false;

        validate();
        repaint();
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     */
    public void addPlayer(String name)
    {
        /* This is visible for both our hand and opponent hands */
        faceImg.setDefaultFace();
        faceImg.setVisible(true);

        pname.setText(name);
        pname.setVisible(true);

        larmyLab.setVisible(true);
        lroadLab.setVisible(true);

        roadSq.setVisible(true);
        roadLab.setVisible(true);
        settlementSq.setVisible(true);
        settlementLab.setVisible(true);
        citySq.setVisible(true);
        cityLab.setVisible(true);
        knightsLab.setVisible(true);
        knightsSq.setVisible(true);

        if (player.getName().equals(client.getNickname()))
        {
            D.ebugPrintln("SOCHandPanel.addPlayer: This is our hand");

            // show 'Victory Points' and hide "Start Button" if game in progress
            if (game.getGameState() == game.NEW)
            {
                startBut.setVisible(true);
            }
            else
            {
                vpLab.setVisible(true);
                vpSq.setVisible(true);
            }
            
            claySq.setVisible(true);
            clayLab.setVisible(true);
            oreSq.setVisible(true);
            oreLab.setVisible(true);
            sheepSq.setVisible(true);
            sheepLab.setVisible(true);
            wheatSq.setVisible(true);
            wheatLab.setVisible(true);
            woodSq.setVisible(true);
            woodLab.setVisible(true);

            //cardLab.setVisible(true);
            cardList.setVisible(true);
            playCardBut.setVisible(true);

            giveLab.setVisible(true);
            getLab.setVisible(true);
            sqPanel.setVisible(true);

            sendBut.setVisible(true);
            clearBut.setVisible(true);
            bankBut.setVisible(true);

            for (int i = 0; i < (SOCGame.MAXPLAYERS - 1); i++)
            {
                playerSend[i].setBoolValue(true);
                playerSend[i].setVisible(true);
            }
            rollBut.setVisible(true);
            doneBut.setVisible(true);
            quitBut.setVisible(true);

            // Remove all of the sit and take over buttons. 
            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                playerInterface.getPlayerHandPanel(i).removeSitBut();
                playerInterface.getPlayerHandPanel(i).removeTakeOverBut();
            }
        }
        else
        {
            /* This is another player's hand */

            D.ebugPrintln("**** SOCHandPanel.addPlayer(name) ****");
            D.ebugPrintln("player.getPlayerNumber() = " + player.getPlayerNumber());
            D.ebugPrintln("player.isRobot() = " + player.isRobot());
            D.ebugPrintln("game.isSeatLocked(" + player.getPlayerNumber() + ") = " + game.isSeatLocked(player.getPlayerNumber()));
            D.ebugPrintln("game.getPlayer(client.getNickname()) = " + game.getPlayer(client.getNickname()));

            if (player.isRobot() && (game.getPlayer(client.getNickname()) == null) && (!game.isSeatLocked(player.getPlayerNumber())))
            {
                addTakeOverBut();
            }

            if (player.isRobot() && (game.getPlayer(client.getNickname()) != null))
            {
                addSeatLockBut();
            }
            else
            {
                removeSeatLockBut();
            }

            vpLab.setVisible(true);
            vpSq.setVisible(true);

            resourceLab.setVisible(true);
            resourceSq.setVisible(true);
            developmentLab.setVisible(true);
            developmentSq.setVisible(true);

            removeSitBut();
            removeRobotBut();
        }

        inPlay = true;

        validate();
        repaint();
    }

    /**
     * DOCUMENT ME!
     */
    public void updateDevCards()
    {
        SOCDevCardSet cards = player.getDevCards();

        int[] cardTypes = { SOCDevCardConstants.DISC,
                            SOCDevCardConstants.KNIGHT,
                            SOCDevCardConstants.MONO,
                            SOCDevCardConstants.ROADS,
                            SOCDevCardConstants.CAP,
                            SOCDevCardConstants.LIB,
                            SOCDevCardConstants.TEMP,
                            SOCDevCardConstants.TOW,
                            SOCDevCardConstants.UNIV };
        String[] cardNames = {"Discovery",
                              "Knight",
                              "Monopoly",
                              "Road Building",
                              "Capitol (1VP)",
                              "Library (1VP)",
                              "Temple (1VP)",
                              "Tower (1VP)",
                              "University (1VP)"};

        synchronized (cardList.getTreeLock())
        {
            cardList.removeAll();
            
            // add items to the list for each new and old card, of each type
            for (int i = 0; i < cardTypes.length; i++)
            {
                int numOld = cards.getAmount(SOCDevCardSet.OLD, cardTypes[i]);
                int numNew = cards.getAmount(SOCDevCardSet.NEW, cardTypes[i]);

                for (int j = 0; j < numOld; j++)
                {
                    cardList.add(cardNames[i]);
                }
                for (int j = 0; j < numNew; j++)
                {
                    // VP cards (starting at 4) are valid immidiately
                    String prefix = (i < 4) ? "*NEW* " : "";
                    cardList.add(prefix + cardNames[i]);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void removeSeatLockBut()
    {
        seatLockBut.setVisible(false);
    }

    /**
     * DOCUMENT ME!
     */
    public void removeTakeOverBut()
    {
        takeOverBut.setVisible(false);
    }

    /**
     * DOCUMENT ME!
     */
    public void removeSitBut()
    {
        sitBut.setVisible(false);
    }

    /**
     * DOCUMENT ME!
     */
    public void removeRobotBut()
    {
        robotBut.setVisible(false);
    }

    /**
     * Internal mechanism to remove start button (if visible) and add VP label.
     */
    public void removeStartBut()
    {
        vpLab.setVisible(true);
        vpSq.setVisible(true);

        startBut.setVisible(false);
    }
    
    /**
     * DOCUMENT ME!
     */
    public void updateCurrentOffer()
    {
        if (inPlay)
        {
            SOCTradeOffer currentOffer = player.getCurrentOffer();

            if (currentOffer != null)
            {
                offer.setOffer(currentOffer);
                offer.setVisible(true);
                offer.repaint();
            }
            else
            {
                clearOffer();
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void rejectOffer()
    {
        offer.setMessage("No thanks.");
        offer.setVisible(true);
        //validate();
        repaint();
    }

    /**
     * DOCUMENT ME!
     */
    public void clearTradeMsg()
    {
        if (offer.getMode() == TradeOfferPanel.MESSAGE_MODE)
        {
            offer.setVisible(false);
            repaint();
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void clearOffer()
    {
        offer.setVisible(false);

        if (player.getName().equals(client.getNickname()))
        {
            // clear the squares panel
            sqPanel.setValues(zero, zero);

            // reset the send squares
            for (int i = 0; i < 3; i++)
            {
                playerSend[i].setBoolValue(true);
            }
        }
        validate();
        repaint();
    }

    /**
     * update the takeover button so that it only
     * allows takover when it's not the robot's turn
     */
    public void updateTakeOverButton()
    {
        if ((!game.isSeatLocked(player.getPlayerNumber())) &&
            (game.getCurrentPlayerNumber() != player.getPlayerNumber()))
        {
            takeOverBut.setLabel(TAKEOVER);
        }
        else
        {
            takeOverBut.setLabel("* Seat Locked *");
        }
    }

    /**
     * update the seat lock button so that it
     * allows a player to lock an unlocked seat
     * and vice versa
     */
    public void updateSeatLockButton()
    {
        if (game.isSeatLocked(player.getPlayerNumber()))
        {
            seatLockBut.setLabel(UNLOCKSEAT);
        }
        else
        {
            seatLockBut.setLabel(LOCKSEAT);
        }
    }

    /**
     * turn the "largest army" label on or off
     *
     * @param haveIt  true if this player has the largest army
     */
    protected void setLArmy(boolean haveIt)
    {
        larmyLab.setText(haveIt ? "L. Army" : "");
    }

    /**
     * turn the "longest road" label on or off
     *
     * @param haveIt  true if this player has the longest road
     */
    protected void setLRoad(boolean haveIt)
    {
        lroadLab.setText(haveIt ? "L. Road" : "");
    }

    /**
     * update the value of a player element
     *
     * @param vt  the type of value
     */
    public void updateValue(int vt)
    {
        /**
         * We say that we're getting the total vp, but
         * for other players this will automatically get
         * the public vp because we will assume their
         * dev card vp total is zero.
         */
        switch (vt)
        {
        case VICTORYPOINTS:

            vpSq.setIntValue(player.getTotalVP());

            break;

        case LONGESTROAD:

            setLRoad(player.hasLongestRoad());

            break;

        case LARGESTARMY:

            setLArmy(player.hasLargestArmy());

            break;

        case CLAY:

            claySq.setIntValue(player.getResources().getAmount(SOCResourceConstants.CLAY));

            break;

        case ORE:

            oreSq.setIntValue(player.getResources().getAmount(SOCResourceConstants.ORE));

            break;

        case SHEEP:

            sheepSq.setIntValue(player.getResources().getAmount(SOCResourceConstants.SHEEP));

            break;

        case WHEAT:

            wheatSq.setIntValue(player.getResources().getAmount(SOCResourceConstants.WHEAT));

            break;

        case WOOD:

            woodSq.setIntValue(player.getResources().getAmount(SOCResourceConstants.WOOD));

            break;

        case NUMRESOURCES:

            resourceSq.setIntValue(player.getResources().getTotal());

            break;

        case ROADS:

            roadSq.setIntValue(player.getNumPieces(SOCPlayingPiece.ROAD));

            break;

        case SETTLEMENTS:

            settlementSq.setIntValue(player.getNumPieces(SOCPlayingPiece.SETTLEMENT));

            break;

        case CITIES:

            citySq.setIntValue(player.getNumPieces(SOCPlayingPiece.CITY));

            break;

        case NUMDEVCARDS:

            developmentSq.setIntValue(player.getDevCards().getTotal());

            break;

        case NUMKNIGHTS:

            knightsSq.setIntValue(player.getNumKnights());

            break;
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void doLayout()
    {
        Dimension dim = getSize();
        int inset = 8;
        int space = 2;

        if (!inPlay)
        {
            /* just show the 'sit' button */
            /* and the 'robot' button     */
            sitBut.setBounds((dim.width - 60) / 2, (dim.height - 82) / 2, 60, 40);
        }
        else
        {
            FontMetrics fm = this.getFontMetrics(this.getFont());
            int lineH = ColorSquare.HEIGHT;
            int stlmtsW = fm.stringWidth("Stlmts: ");
            int knightsW = fm.stringWidth("Knights: ");
            int faceW = 40;
            int pnameW = dim.width - (inset + faceW + inset + inset);

            faceImg.setBounds(inset, inset, faceW, faceW);
            pname.setBounds(inset + faceW + inset, inset, pnameW, lineH);

            //if (true) {
            if (player.getName().equals(client.getNickname()))
            {
                /* This is our hand */
                //sqPanel.doLayout();

                Dimension sqpDim = sqPanel.getSize();
                int sheepW = fm.stringWidth("Sheep: ");
                int pcW = fm.stringWidth(CARD);
                int giveW = fm.stringWidth(GIVE);
                int clearW = fm.stringWidth(CLEAR);
                int bankW = fm.stringWidth(BANK);
                int cardsH = 5 * (lineH + space);
                int tradeH = sqpDim.height + space + (2 * (lineH + space));
                int sectionSpace = (dim.height - (inset + faceW + cardsH + tradeH + lineH + inset)) / 3;
                int tradeY = inset + faceW + sectionSpace;
                int cardsY = tradeY + tradeH + sectionSpace;

                // Always reposition everything
                startBut.setBounds(inset + faceW + inset, inset + lineH + space, dim.width - (inset + faceW + inset + inset), lineH);

                    int vpW = fm.stringWidth(vpLab.getText());
                    vpLab.setBounds(inset + faceW + inset, (inset + faceW) - lineH, vpW, lineH);
                    vpSq.setBounds(inset + faceW + inset + vpW + space, (inset + faceW) - lineH, ColorSquare.WIDTH, ColorSquare.WIDTH);

                    int topStuffW = inset + faceW + inset + vpW + space + ColorSquare.WIDTH + space;

                    // always position these: though they may not be visible
                    larmyLab.setBounds(topStuffW, (inset + faceW) - lineH, (dim.width - (topStuffW + inset + space)) / 2, lineH);
                    lroadLab.setBounds(topStuffW + ((dim.width - (topStuffW + inset + space)) / 2) + space, (inset + faceW) - lineH, (dim.width - (topStuffW + inset + space)) / 2, lineH);

                giveLab.setBounds(inset, tradeY, giveW, lineH);
                getLab.setBounds(inset, tradeY + lineH, giveW, lineH);
                sqPanel.setLocation(inset + giveW + space, tradeY);

                int tbW = ((giveW + sqpDim.width) / 2);
                int tbX = inset;
                int tbY = tradeY + sqpDim.height + space;
                sendBut.setBounds(tbX, tbY, tbW, lineH);
                clearBut.setBounds(tbX, tbY + lineH + space, tbW, lineH);
                bankBut.setBounds(tbX + tbW + space, tbY + lineH + space, tbW, lineH);

                    playerSend[0].setBounds(tbX + tbW + space, tbY, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                    playerSend[1].setBounds(tbX + tbW + space + ((tbW - ColorSquare.WIDTH) / 2), tbY, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                    playerSend[2].setBounds((tbX + tbW + space + tbW) - ColorSquare.WIDTH, tbY, ColorSquare.WIDTH, ColorSquare.HEIGHT);

                knightsLab.setBounds(dim.width - inset - knightsW - ColorSquare.WIDTH - space, tradeY, knightsW, lineH);
                knightsSq.setBounds(dim.width - inset - ColorSquare.WIDTH, tradeY, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                roadLab.setBounds(dim.width - inset - knightsW - ColorSquare.WIDTH - space, tradeY + lineH + space, knightsW, lineH);
                roadSq.setBounds(dim.width - inset - ColorSquare.WIDTH, tradeY + lineH + space, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                settlementLab.setBounds(dim.width - inset - knightsW - ColorSquare.WIDTH - space, tradeY + (2 * (lineH + space)), knightsW, lineH);
                settlementSq.setBounds(dim.width - inset - ColorSquare.WIDTH, tradeY + (2 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                cityLab.setBounds(dim.width - inset - knightsW - ColorSquare.WIDTH - space, tradeY + (3 * (lineH + space)), knightsW, lineH);
                citySq.setBounds(dim.width - inset - ColorSquare.WIDTH, tradeY + (3 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);

                clayLab.setBounds(inset, cardsY, sheepW, lineH);
                claySq.setBounds(inset + sheepW + space, cardsY, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                oreLab.setBounds(inset, cardsY + (lineH + space), sheepW, lineH);
                oreSq.setBounds(inset + sheepW + space, cardsY + (lineH + space), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                sheepLab.setBounds(inset, cardsY + (2 * (lineH + space)), sheepW, lineH);
                sheepSq.setBounds(inset + sheepW + space, cardsY + (2 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                wheatLab.setBounds(inset, cardsY + (3 * (lineH + space)), sheepW, lineH);
                wheatSq.setBounds(inset + sheepW + space, cardsY + (3 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                woodLab.setBounds(inset, cardsY + (4 * (lineH + space)), sheepW, lineH);
                woodSq.setBounds(inset + sheepW + space, cardsY + (4 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);

                int clW = dim.width - (inset + sheepW + space + ColorSquare.WIDTH + (4 * space) + inset);
                int clX = inset + sheepW + space + ColorSquare.WIDTH + (4 * space);
                cardList.setBounds(clX, cardsY, clW, (4 * (lineH + space)) - 2);
                playCardBut.setBounds(((clW - pcW) / 2) + clX, cardsY + (4 * (lineH + space)), pcW, lineH);

                int bbW = 50;
                quitBut.setBounds(inset, dim.height - lineH - inset, bbW, lineH);
                rollBut.setBounds(dim.width - (bbW + space + bbW + inset), dim.height - lineH - inset, bbW, lineH);
                doneBut.setBounds(dim.width - inset - bbW, dim.height - lineH - inset, bbW, lineH);
            }
            else
            {
                /* This is another player's hand */
                int balloonH = dim.height - (inset + (4 * (lineH + space)) + inset);
                int dcardsW = fm.stringWidth("Dev. Cards: ");
                int vpW = fm.stringWidth(vpLab.getText());

                if (player.isRobot())
                {
                    if (game.getPlayer(client.getNickname()) == null)
                    {
                        takeOverBut.setBounds(10, (inset + balloonH) - 10, dim.width - 20, 20);
                    }
                    else if (seatLockBut.isVisible())
                    {
                        //seatLockBut.setBounds(10, inset+balloonH-10, dim.width-20, 20);
                        seatLockBut.setBounds(inset + dcardsW + space + ColorSquare.WIDTH + space, inset + balloonH + (lineH + space) + (lineH / 2), (dim.width - (2 * (inset + ColorSquare.WIDTH + (2 * space))) - stlmtsW - dcardsW), 2 * (lineH + space));
                    }
                }

                    offer.setBounds(inset, inset + faceW + space, dim.width - (2 * inset), balloonH);
                    offer.doLayout();

                vpLab.setBounds(inset + faceW + inset, (inset + faceW) - lineH, vpW, lineH);
                vpSq.setBounds(inset + faceW + inset + vpW + space, (inset + faceW) - lineH, ColorSquare.WIDTH, ColorSquare.HEIGHT);

                int topStuffW = inset + faceW + inset + vpW + space + ColorSquare.WIDTH + space;

                // always position these: though they may not be visible
                larmyLab.setBounds(topStuffW, (inset + faceW) - lineH, (dim.width - (topStuffW + inset + space)) / 2, lineH);
                lroadLab.setBounds(topStuffW + ((dim.width - (topStuffW + inset + space)) / 2) + space, (inset + faceW) - lineH, (dim.width - (topStuffW + inset + space)) / 2, lineH);

                resourceLab.setBounds(inset, inset + balloonH + (2 * (lineH + space)), dcardsW, lineH);
                resourceSq.setBounds(inset + dcardsW + space, inset + balloonH + (2 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                developmentLab.setBounds(inset, inset + balloonH + (3 * (lineH + space)), dcardsW, lineH);
                developmentSq.setBounds(inset + dcardsW + space, inset + balloonH + (3 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                knightsLab.setBounds(inset, inset + balloonH + (lineH + space), dcardsW, lineH);
                knightsSq.setBounds(inset + dcardsW + space, inset + balloonH + (lineH + space), ColorSquare.WIDTH, ColorSquare.HEIGHT);

                roadLab.setBounds(dim.width - inset - stlmtsW - ColorSquare.WIDTH - space, inset + balloonH + (lineH + space), stlmtsW, lineH);
                roadSq.setBounds(dim.width - inset - ColorSquare.WIDTH, inset + balloonH + (lineH + space), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                settlementLab.setBounds(dim.width - inset - stlmtsW - ColorSquare.WIDTH - space, inset + balloonH + (2 * (lineH + space)), stlmtsW, lineH);
                settlementSq.setBounds(dim.width - inset - ColorSquare.WIDTH, inset + balloonH + (2 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                cityLab.setBounds(dim.width - inset - stlmtsW - ColorSquare.WIDTH - space, inset + balloonH + (3 * (lineH + space)), stlmtsW, lineH);
                citySq.setBounds(dim.width - inset - ColorSquare.WIDTH, inset + balloonH + (3 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
            }
        }
    }
}
