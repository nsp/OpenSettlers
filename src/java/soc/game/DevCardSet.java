/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2007,2009 Jeremy D. Monin <jeremy@nand.net>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.game;

import java.io.Serializable;


/**
 * This represents a collection of development cards
 */
public class DevCardSet implements Serializable, Cloneable
{
    private static final long serialVersionUID = 7178006197603834224L;
    /**
     * age constants. OLD == 0, NEW == 1 (guaranteed for use in loops).
     */
    public static final int OLD = 0;
    public static final int NEW = 1;

    /**
     * the number of development cards of each type.
     * [{@link #OLD}] are the old cards.
     * [{@link #NEW}] are recently bought cards.
     * Card types as in {@link DevCardConstants}.
     */
    private int[][] devCards;

    /**
     * Make an empty development card set
     */
    public DevCardSet()
    {
        devCards = new int[2][DevCardConstants.MAXPLUSONE];
        clear();
    }

    /**
     * Make a copy of a dev card set
     *
     * @param set  the dev card set to copy
     */
    public DevCardSet(DevCardSet set)
    {
        devCards = new int[2][DevCardConstants.MAXPLUSONE];

        for (int i = DevCardConstants.MIN;
                i < DevCardConstants.MAXPLUSONE; i++)
        {
            devCards[OLD][i] = set.devCards[OLD][i];
            devCards[NEW][i] = set.devCards[NEW][i];
        }
    }

    /**
     * set the number of old and new dev cards to zero
     */
    public void clear()
    {
        for (int i = DevCardConstants.MIN;
                i < DevCardConstants.MAXPLUSONE; i++)
        {
            devCards[OLD][i] = 0;
            devCards[NEW][i] = 0;
        }
    }

    /**
     * @return the number of a kind of development card
     *
     * @param age  either {@link #OLD} or {@link #NEW}
     * @param ctype
     *        the type of development card as described
     *        in {@link DevCardConstants};
     *        at least {@link DevCardConstants#MIN}
     *        and less than {@link DevCardConstants#MAXPLUSONE}
     */
    public int getAmount(int age, int ctype)
    {
        return devCards[age][ctype];
    }

    /**
     * @return the total number of development cards
     */
    public int getTotal()
    {
        int sum = 0;

        for (int i = DevCardConstants.MIN;
                i < DevCardConstants.MAXPLUSONE; i++)
        {
            sum += (devCards[OLD][i] + devCards[NEW][i]);
        }

        return sum;
    }

    /**
     * set the amount of a type of card
     *
     * @param age   either {@link #OLD} or {@link #NEW}
     * @param ctype the type of development card, at least
     *              {@link DevCardConstants#MIN} and less than {@link DevCardConstants#MAXPLUSONE}
     * @param amt   the amount
     */
    public void setAmount(int amt, int age, int ctype)
    {
        devCards[age][ctype] = amt;
    }

    /**
     * add an amount to a type of card
     *
     * @param age   either {@link #OLD} or {@link #NEW}
     * @param ctype the type of development card, at least
     *              {@link DevCardConstants#MIN} and less than {@link DevCardConstants#MAXPLUSONE}
     * @param amt   the amount
     */
    public void add(int amt, int age, int ctype)
    {
        devCards[age][ctype] += amt;
    }

    /**
     * subtract an amount from a type of card
     *
     * @param age   either {@link #OLD} or {@link #NEW}
     * @param ctype the type of development card, at least
     *              {@link DevCardConstants#MIN} and less than {@link DevCardConstants#MAXPLUSONE}
     * @param amt   the amount
     */
    public void subtract(int amt, int age, int ctype)
    {
        if (amt <= devCards[age][ctype])
        {
            devCards[age][ctype] -= amt;
        }
        else
        {
            devCards[age][ctype] = 0;
            devCards[age][DevCardConstants.UNKNOWN] -= amt;
        }
    }

    /**
     * @return the number of victory point cards in
     *         this set
     */
    public int getNumVPCards()
    {
        int sum = 0;

        sum += devCards[OLD][DevCardConstants.CAP];
        sum += devCards[OLD][DevCardConstants.LIB];
        sum += devCards[OLD][DevCardConstants.UNIV];
        sum += devCards[OLD][DevCardConstants.TEMP];
        sum += devCards[OLD][DevCardConstants.TOW];
        sum += devCards[NEW][DevCardConstants.CAP];
        sum += devCards[NEW][DevCardConstants.LIB];
        sum += devCards[NEW][DevCardConstants.UNIV];
        sum += devCards[NEW][DevCardConstants.TEMP];
        sum += devCards[NEW][DevCardConstants.TOW];

        return sum;
    }
    
    /**
     * Some card types stay in your hand after being played.
     * Count only the unplayed ones (old or new). 
     * 
     * @return the number of unplayed cards in this set
     */
    public int getNumUnplayed()
    {
        int sum = 0;

        sum += devCards[OLD][DevCardConstants.KNIGHT];
        sum += devCards[OLD][DevCardConstants.ROADS];
        sum += devCards[OLD][DevCardConstants.DISC];
        sum += devCards[OLD][DevCardConstants.MONO];
        sum += devCards[OLD][DevCardConstants.UNKNOWN];
        sum += devCards[NEW][DevCardConstants.KNIGHT];
        sum += devCards[NEW][DevCardConstants.ROADS];
        sum += devCards[NEW][DevCardConstants.DISC];
        sum += devCards[NEW][DevCardConstants.MONO];
        sum += devCards[NEW][DevCardConstants.UNKNOWN];
        
        return sum;
    }

    /**
     * change all the new cards to old ones
     */
    public void newToOld()
    {
        for (int i = DevCardConstants.MIN;
                i < DevCardConstants.MAXPLUSONE; i++)
        {
            devCards[OLD][i] += devCards[NEW][i];
            devCards[NEW][i] = 0;
        }
    }
}
