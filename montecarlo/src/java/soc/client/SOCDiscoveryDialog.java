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

import soc.game.SOCResourceSet;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class SOCDiscoveryDialog extends Dialog implements ActionListener
{
    Button doneBut;
    Button clearBut;
    ColorSquare[] rsrc;
    Label msg;
    SOCPlayerInterface pi;

    /**
     * Creates a new SOCDiscoveryDialog object.
     *
     * @param pi DOCUMENT ME!
     */
    public SOCDiscoveryDialog(SOCPlayerInterface pi)
    {
        super(pi, "Discovery", true);

        this.pi = pi;
        setBackground(new Color(255, 230, 162));
        setForeground(Color.black);
        setFont(new Font("Geneva", Font.PLAIN, 12));

        doneBut = new Button("Done");
        clearBut = new Button("Clear");

        setLayout(null);
        addNotify();
        setSize(280, 190);

        msg = new Label("Please pick two resources.", Label.CENTER);
        add(msg);

        add(doneBut);
        doneBut.addActionListener(this);

        add(clearBut);
        clearBut.addActionListener(this);

        rsrc = new ColorSquare[5];
        rsrc[0] = new ColorSquare(ColorSquare.BOUNDED_INC, true, ColorSquare.CLAY, 2, 0);
        rsrc[1] = new ColorSquare(ColorSquare.BOUNDED_INC, true, ColorSquare.ORE, 2, 0);
        rsrc[2] = new ColorSquare(ColorSquare.BOUNDED_INC, true, ColorSquare.SHEEP, 2, 0);
        rsrc[3] = new ColorSquare(ColorSquare.BOUNDED_INC, true, ColorSquare.WHEAT, 2, 0);
        rsrc[4] = new ColorSquare(ColorSquare.BOUNDED_INC, true, ColorSquare.WOOD, 2, 0);

        for (int i = 0; i < 5; i++)
        {
            add(rsrc[i]);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param b DOCUMENT ME!
     */
    public void setVisible(boolean b)
    {
        super.setVisible(b);

        if (b)
        {
            doneBut.requestFocus();
        }
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

        int pix = pi.getInsets().left;
        int piy = pi.getInsets().top;
        int piwidth = pi.getSize().width - pi.getInsets().left - pi.getInsets().right;
        int piheight = pi.getSize().height - pi.getInsets().top - pi.getInsets().bottom;

        int sqwidth = ColorSquare.WIDTH;
        int sqspace = (width - (5 * sqwidth)) / 5;

        int buttonW = 80;
        int buttonX = (width - ((2 * buttonW) + space)) / 2;
        int rsrcY;

        /* put the dialog in the center of the game window */
        setLocation(pix + ((piwidth - width) / 2), piy + ((piheight - height) / 2));

        if (msg != null)
        {
            msg.setBounds((width - 188) / 2, getInsets().top, 180, 20);
        }

        if (clearBut != null)
        {
            clearBut.setBounds(buttonX, (getInsets().bottom + height) - 25, buttonW, 25);
        }

        if (doneBut != null)
        {
            doneBut.setBounds(buttonX + buttonW + space, (getInsets().bottom + height) - 25, buttonW, 25);
        }

        try
        {
            rsrcY = y + 20 + space + 20 + space;

            for (int i = 0; i < 5; i++)
            {
                rsrc[i].setSize(sqwidth, sqwidth);
                rsrc[i].setLocation((i * sqspace) + ((width - ((3 * sqspace) + (4 * sqwidth))) / 2), rsrcY);
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

        if (target == doneBut)
        {
            int[] rsrcCnt = new int[5];
            int i;
            int sum = 0;

            for (i = 0; i < 5; i++)
            {
                rsrcCnt[i] = rsrc[i].getIntValue();
                sum += rsrcCnt[i];
            }

            if (sum == 2)
            {
                SOCResourceSet resources = new SOCResourceSet(rsrcCnt[0], rsrcCnt[1], rsrcCnt[2], rsrcCnt[3], rsrcCnt[4], 0);
                pi.getClient().discoveryPick(pi.getGame(), resources);
                dispose();
            }
        }
        else if (target == clearBut)
        {
            for (int i = 0; i < 5; i++)
            {
                rsrc[i].setIntValue(0);
            }
        }
    }
}
