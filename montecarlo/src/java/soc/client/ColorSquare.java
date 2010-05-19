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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;


/**
 * This is a square box with a background color and
 * possibly a number or X in it.  This box can be
 * interactive, or non-interactive.  The possible
 * colors of the box correspond to resources in SoC.
 *
 * @author Robert S Thomas
 */
public class ColorSquare extends Canvas implements MouseListener
{
    public final static Color CLAY = new Color(204, 102, 102);
    public final static Color ORE = new Color(153, 153, 153);
    public final static Color SHEEP = new Color(51, 204, 51);
    public final static Color WHEAT = new Color(204, 204, 51);
    public final static Color WOOD = new Color(204, 153, 102);
    public final static Color GREY = new Color(204, 204, 204);
    public final static int NUMBER = 0;
    public final static int YES_NO = 1;
    public final static int CHECKBOX = 2;
    public final static int BOUNDED_INC = 3;
    public final static int BOUNDED_DEC = 4;
    public final static int WIDTH = 16;
    public final static int HEIGHT = 16;
    int intValue;
    boolean boolValue;
    boolean valueVis;
    int kind;
    int upperBound;
    int lowerBound;
    boolean interactive;

    /**
     * Creates a new ColorSquare object without a visible value.
     *
     * @see #ColorSquare(int, boolean, Color, int, int)
     */
    public ColorSquare()
    {
        this(NUMBER, false, GREY, 0, 0);
        valueVis = false;
    }

    /**
     * Creates a new ColorSquare object with specified background color. Type
     * <code>NUMBER</code>, non-interactive, upper=99, lower=0.
     *
     * @param c background color
     * @see #ColorSquare(int, boolean, Color, int, int)
     */
    public ColorSquare(Color c)
    {
        this(NUMBER, false, c, 99, 0);
    }

    /**
     * Creates a new ColorSquare object with specified background color and
     * initial value. Type <code>NUMBER</code>, non-interactive, upper=99,
     * lower=0.
     *
     * @param c background color
     * @param v initial int value
     * @see #ColorSquare(int, boolean, Color, int, int)
     */
    public ColorSquare(Color c, int v)
    {
        this(NUMBER, false, c, 99, 0);
        intValue = v;
    }

    /**
     * Creates a new ColorSquare of the specified kind and background
     * color. Possibly interactive. For kind = NUMBER, upper=99, lower=0.
     *
     * @param k Kind: NUMBER, YES_NO, CHECKBOX, BOUNDED_INC, BOUNDED_DECk
     * @param in interactive flag allowing user interaction
     * @param c background color
     * @see #ColorSquare(int, boolean, Color, int, int)
     */
    public ColorSquare(int k, boolean in, Color c)
    {
        this(k, in, c, 99, 0);
    }

    /**
     * Creates a new ColorSquare of the specified kind and background
     * color. Possibly interactive, with upper and lower bounds specified for
     * NUMBER kinds.
     *
     * @param k Kind: NUMBER, YES_NO, CHECKBOX, BOUNDED_INC, BOUNDED_DECk
     * @param in interactive flag allowing user interaction
     * @param c background color
     * @param upper upper bound if k == NUMBER
     * @param lower lower bound if k == NUMBER
     */
    public ColorSquare(int k, boolean in, Color c, int upper, int lower)
    {
        super();

        setFont(new Font("Geneva", Font.PLAIN, 10));

        setBackground(c);
        kind = k;
        interactive = in;

        switch (k)
        {
        case NUMBER:
            valueVis = true;
            intValue = 0;

            break;

        case YES_NO:
            valueVis = true;
            boolValue = false;

            break;

        case CHECKBOX:
            valueVis = true;
            boolValue = false;

            break;

        case BOUNDED_INC:
            valueVis = true;
            boolValue = false;
            upperBound = upper;
            lowerBound = lower;

            break;

        case BOUNDED_DEC:
            valueVis = true;
            boolValue = false;
            upperBound = upper;
            lowerBound = lower;

            break;
        }

        this.addMouseListener(this);
    }

    /**
     * DOCUMENT ME!
     *
     * @param c DOCUMENT ME!
     */
    public void setColor(Color c)
    {
        setBackground(c);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Dimension getPreferedSize()
    {
        return new Dimension(WIDTH, HEIGHT);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Dimension getMinimumSize()
    {
        return new Dimension(WIDTH, HEIGHT);
    }

    /**
     * DOCUMENT ME!
     *
     * @param g DOCUMENT ME!
     */
    public void paint(Graphics g)
    {
            g.setPaintMode();
            g.clearRect(0, 0, WIDTH, HEIGHT);
            g.setColor(Color.black);
            g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

            int x;
            int y;

            if (valueVis)
            {
                FontMetrics fm = this.getFontMetrics(this.getFont());
                int numW;
                //int numH = fm.getHeight();
                //int numA = fm.getAscent();
                switch (kind)
                {
                case NUMBER:
                case BOUNDED_INC:
                case BOUNDED_DEC:

                    numW = fm.stringWidth(Integer.toString(intValue));

                    x = (WIDTH - numW) / 2;
                    
                    // y = numA + (HEIGHT - numH) / 2; // proper way
                    y = 12; // way that works

                    g.drawString(Integer.toString(intValue), x, y);

                    break;

                case YES_NO:
                    String value = (boolValue ? "Y" : "N");

                    numW = fm.stringWidth(value);

                    x = (WIDTH - numW) / 2;

                    // y = numA + (HEIGHT - numH) / 2; // proper way
                    y = 12; // way that works

                    g.drawString(value, x, y);

                    break;

                case CHECKBOX:

                    if (boolValue)
                    {
                        int checkX = WIDTH / 5;
                        int checkY = HEIGHT / 4;
                        g.drawLine(checkX, 2 * checkY, 2 * checkX, 3 * checkY);
                        g.drawLine(2 * checkX, 3 * checkY, 4 * checkX, checkY);
                    }

                    break;
                }
            }
    }

    /**
     * DOCUMENT ME!
     *
     * @param v DOCUMENT ME!
     */
    public void addValue(int v)
    {
        intValue += v;
        repaint();
    }

    /**
     * DOCUMENT ME!
     *
     * @param v DOCUMENT ME!
     */
    public void subtractValue(int v)
    {
        intValue -= v;
        repaint();
    }

    /**
     * DOCUMENT ME!
     *
     * @param v DOCUMENT ME!
     */
    public void setIntValue(int v)
    {
        intValue = v;
        repaint();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public int getIntValue()
    {
        return intValue;
    }

    /**
     * DOCUMENT ME!
     *
     * @param v DOCUMENT ME!
     */
    public void setBoolValue(boolean v)
    {
        boolValue = v;
        repaint();
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public boolean getBoolValue()
    {
        return boolValue;
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
     * @param evt DOCUMENT ME!
     */
    public void mousePressed(MouseEvent evt)
    {
        if (interactive)
        {
            switch (kind)
            {
            case YES_NO:
            case CHECKBOX:
                boolValue = !boolValue;

                break;

            case NUMBER:
                intValue++;

                break;

            case BOUNDED_INC:

                if (intValue < upperBound)
                {
                    intValue++;
                }

                break;

            case BOUNDED_DEC:

                if (intValue > lowerBound)
                {
                    intValue--;
                }

                break;
            }

            repaint();
        }
    }
}
