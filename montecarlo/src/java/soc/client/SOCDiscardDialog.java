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

import soc.game.SOCPlayer;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


/**
 * This is the dialog to ask players what resources they want
 * to discard.
 *
 * @author  Robert S. Thomas
 */
class SOCDiscardDialog extends Dialog implements ActionListener, MouseListener
{
    Button discardBut;
    ColorSquare[] keep;
    ColorSquare[] disc;
    Label msg;
    Label youHave;
    Label discThese;
    SOCPlayerInterface playerInterface;
    int numDiscards;

    /**
     * Creates a new SOCDiscardDialog object.
     *
     * @param pi DOCUMENT ME!
     * @param rnum DOCUMENT ME!
     */
    public SOCDiscardDialog(SOCPlayerInterface pi, int rnum)
    {
        super(pi, "Discard", true);

        playerInterface = pi;
        numDiscards = rnum;
        setBackground(new Color(255, 230, 162));
        setForeground(Color.black);
        setFont(new Font("Geneva", Font.PLAIN, 12));

        discardBut = new Button("Discard");

        setLayout(null);
        
        setSize(280, 190);

        msg = new Label("Please discard " + Integer.toString(numDiscards) + " resources.", Label.CENTER);
        add(msg);
        youHave = new Label("You have:", Label.LEFT);
        add(youHave);
        discThese = new Label("Discard these:", Label.LEFT);
        add(discThese);

        add(discardBut);
        discardBut.addActionListener(this);

        keep = new ColorSquare[5];
        keep[0] = new ColorSquare(ColorSquare.BOUNDED_DEC, false, ColorSquare.CLAY);
        keep[1] = new ColorSquare(ColorSquare.BOUNDED_DEC, false, ColorSquare.ORE);
        keep[2] = new ColorSquare(ColorSquare.BOUNDED_DEC, false, ColorSquare.SHEEP);
        keep[3] = new ColorSquare(ColorSquare.BOUNDED_DEC, false, ColorSquare.WHEAT);
        keep[4] = new ColorSquare(ColorSquare.BOUNDED_DEC, false, ColorSquare.WOOD);

        disc = new ColorSquare[5];
        disc[0] = new ColorSquare(ColorSquare.BOUNDED_INC, false, ColorSquare.CLAY);
        disc[1] = new ColorSquare(ColorSquare.BOUNDED_INC, false, ColorSquare.ORE);
        disc[2] = new ColorSquare(ColorSquare.BOUNDED_INC, false, ColorSquare.SHEEP);
        disc[3] = new ColorSquare(ColorSquare.BOUNDED_INC, false, ColorSquare.WHEAT);
        disc[4] = new ColorSquare(ColorSquare.BOUNDED_INC, false, ColorSquare.WOOD);

        for (int i = 0; i < 5; i++)
        {
            add(keep[i]);
            add(disc[i]);
            keep[i].addMouseListener(this);
            disc[i].addMouseListener(this);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param b DOCUMENT ME!
     */
    public void setVisible(boolean b)
    {
        if (b)
        {
            /**
             * set initial values
             */
            SOCPlayer player = playerInterface.getGame().getPlayer(playerInterface.getClient().getNickname());
            SOCResourceSet resources = player.getResources();
            keep[0].setIntValue(resources.getAmount(SOCResourceConstants.CLAY));
            keep[1].setIntValue(resources.getAmount(SOCResourceConstants.ORE));
            keep[2].setIntValue(resources.getAmount(SOCResourceConstants.SHEEP));
            keep[3].setIntValue(resources.getAmount(SOCResourceConstants.WHEAT));
            keep[4].setIntValue(resources.getAmount(SOCResourceConstants.WOOD));

            discardBut.requestFocus();
        }

        super.setVisible(b);
    }

    /**
     * DOCUMENT ME!
     */
    public void doLayout()
    {
        int x = getInsets().left;
        int y = getInsets().top;
        int width = getSize().width - getInsets().left - getInsets().right;
        int height = getSize().height - getInsets().top - getInsets().bottom;
        int space = 5;

        int cfx = playerInterface.getInsets().left;
        int cfy = playerInterface.getInsets().top;
        int cfwidth = playerInterface.getSize().width - playerInterface.getInsets().left - playerInterface.getInsets().right;
        int cfheight = playerInterface.getSize().height - playerInterface.getInsets().top - playerInterface.getInsets().bottom;

        int sqwidth = ColorSquare.WIDTH;
        int sqspace = (width - (5 * sqwidth)) / 5;

        int keepY;
        int discY;

        /* put the dialog in the center of the game window */
        setLocation(cfx + ((cfwidth - width) / 2), cfy + ((cfheight - height) / 2));

        try
        {
            msg.setBounds((width - 188) / 2, getInsets().top, 180, 20);
            discardBut.setBounds((width - 88) / 2, (getInsets().bottom + height) - 25, 80, 25);
            youHave.setBounds(getInsets().left, getInsets().top + 20 + space, 70, 20);
            discThese.setBounds(getInsets().left, getInsets().top + 20 + space + 20 + space + sqwidth + space, 100, 20);
        }
        catch (NullPointerException e) {}

        keepY = getInsets().top + 20 + space + 20 + space;
        discY = keepY + sqwidth + space + 20 + space;

        try
        {
            for (int i = 0; i < 5; i++)
            {
                keep[i].setSize(sqwidth, sqwidth);
                keep[i].setLocation((i * sqspace) + ((width - ((3 * sqspace) + (4 * sqwidth))) / 2), keepY);
                disc[i].setSize(sqwidth, sqwidth);
                disc[i].setLocation((i * sqspace) + ((width - ((3 * sqspace) + (4 * sqwidth))) / 2), discY);
            }
        }
        catch (NullPointerException e) {}
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void actionPerformed(ActionEvent e)
    {
        Object target = e.getSource();

        if (target == discardBut)
        {
            SOCResourceSet rsrcs = new SOCResourceSet(disc[0].getIntValue(), disc[1].getIntValue(), disc[2].getIntValue(), disc[3].getIntValue(), disc[4].getIntValue(), 0);

            if (rsrcs.getTotal() == numDiscards)
            {
                playerInterface.getClient().discard(playerInterface.getGame(), rsrcs);
                dispose();
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseEntered(MouseEvent e)
    {
        ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseExited(MouseEvent e)
    {
        ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseClicked(MouseEvent e)
    {
        ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseReleased(MouseEvent e)
    {
        ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mousePressed(MouseEvent e)
    {
        Object target = e.getSource();

        for (int i = 0; i < 5; i++)
        {
            if ((target == keep[i]) && (disc[i].getIntValue() > 0))
            {
                keep[i].addValue(1);
                disc[i].subtractValue(1);
            }
            else if ((target == disc[i]) && (keep[i].getIntValue() > 0))
            {
                keep[i].subtractValue(1);
                disc[i].addValue(1);
            }
        }
    }
}
