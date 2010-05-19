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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Panel;


/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.2 $
 */
public class SquaresPanel extends Panel
{
    private ColorSquare[] give;
    private ColorSquare[] get;
    boolean interactive;

    /**
     * Creates a new SquaresPanel object.
     *
     * @param in DOCUMENT ME!
     */
    public SquaresPanel(boolean in)
    {
        super(null);

        interactive = in;

        setFont(new Font("Helvetica", Font.PLAIN, 10));

        give = new ColorSquare[5];
        give[0] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.CLAY);
        give[1] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.ORE);
        give[2] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.SHEEP);
        give[3] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.WHEAT);
        give[4] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.WOOD);

        get = new ColorSquare[5];
        get[0] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.CLAY);
        get[1] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.ORE);
        get[2] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.SHEEP);
        get[3] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.WHEAT);
        get[4] = new ColorSquare(ColorSquare.NUMBER, in, ColorSquare.WOOD);

        for (int i = 0; i < 5; i++)
        {
            add(get[i]);
            add(give[i]);
        }

        int lineH = ColorSquare.HEIGHT - 1;
        int sqW = ColorSquare.WIDTH - 1;
        setSize((5 * sqW) + 1, (2 * lineH) + 1);
    }

    /**
     * DOCUMENT ME!
     */
    public void doLayout()
    {
        int curX = 0;
        FontMetrics fm = this.getFontMetrics(this.getFont());
        int lineH = ColorSquare.HEIGHT - 1;
        int sqW = ColorSquare.WIDTH - 1;
        int i;

        for (i = 0; i < 5; i++)
        {
            give[i].setSize(sqW + 1, lineH + 1);
            give[i].setLocation(i * sqW, 0);
            //give[i].draw();
            get[i].setSize(sqW + 1, lineH + 1);
            get[i].setLocation(i * sqW, lineH);
            //get[i].draw();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param give DOCUMENT ME!
     * @param get DOCUMENT ME!
     */
    public void setValues(int[] give, int[] get)
    {
        for (int i = 0; i < 5; i++)
        {
            this.give[i].setIntValue(give[i]);
            this.get[i].setIntValue(get[i]);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param give DOCUMENT ME!
     * @param get DOCUMENT ME!
     */
    public void getValues(int[] give, int[] get)
    {
        for (int i = 0; i < 5; i++)
        {
            give[i] = this.give[i].getIntValue();
            get[i] = this.get[i].getIntValue();
        }
    }
}
