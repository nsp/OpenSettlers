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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.message;

import soc.game.ResourceConstants;
import soc.game.ResourceSet;

import java.util.StringTokenizer;


/**
 * This message means that a player wants to trade with the bank
 *
 * @author Robert S. Thomas
 */
public class BankTrade extends Message
{
    private static final long serialVersionUID = -8425078056720461761L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The set of resources being given to the bank
     */
    private ResourceSet give;

    /**
     * The set of resources being taken from the bank
     */
    private ResourceSet get;

    /**
     * Create a BankTrade message.
     *
     * @param ga   the name of the game
     * @param give the set of resources being given to the bank
     * @param get  the set of resources being taken from the bank
     */
    public BankTrade(String ga, ResourceSet give, ResourceSet get)
    {
        messageType = BANKTRADE;
        game = ga;
        this.give = give;
        this.get = get;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the set of resources being given to the bank
     */
    public ResourceSet getGiveSet()
    {
        return give;
    }

    /**
     * @return the set of resources being taken from the bank
     */
    public ResourceSet getGetSet()
    {
        return get;
    }

    /**
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, give, get);
    }

    /**
     * @return the command string
     *
     * @param ga  the name of the game
     * @param give the set of resources being given to the bank
     * @param get  the set of resources being taken from the bank
     */
    public static String toCmd(String ga, ResourceSet give, ResourceSet get)
    {
        String cmd = BANKTRADE + sep + ga;

        for (int i = ResourceConstants.CLAY; i <= ResourceConstants.WOOD;
                i++)
        {
            cmd += (sep2 + give.getAmount(i));
        }

        for (int i = ResourceConstants.CLAY; i <= ResourceConstants.WOOD;
                i++)
        {
            cmd += (sep2 + get.getAmount(i));
        }

        return cmd;
    }

    /**
     * Parse the command String into a BankTrade message
     *
     * @param s   the String to parse
     * @return    a BankTrade message, or null of the data is garbled
     */
    public static BankTrade parseDataStr(String s)
    {
        String ga; // the game name
        ResourceSet give; // the set of resources being given to the bank
        ResourceSet get; // the set of resources being taken from the bank

        give = new ResourceSet();
        get = new ResourceSet();

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();

            /**
             * Note: this only works if ResourceConstants.CLAY == 1
             */
            for (int i = 1; i <= ResourceConstants.WOOD; i++)
            {
                give.setAmount(Integer.parseInt(st.nextToken()), i);
            }

            for (int i = 1; i <= ResourceConstants.WOOD; i++)
            {
                get.setAmount(Integer.parseInt(st.nextToken()), i);
            }
        }
        catch (Exception e)
        {
            return null;
        }

        return new BankTrade(ga, give, get);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "BankTrade:game=" + game + "|give=" + give + "|get=" + get;
    }
}
