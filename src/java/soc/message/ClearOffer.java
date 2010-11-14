/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2010 Jeremy D Monin <jeremy@nand.net>
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

import java.util.StringTokenizer;


/**
 * This message means that the player is retracting an offer.
 *<P>
 * Version 1.1.09: If <tt>playerNumber</tt> is -1, all players are clearing all offers (usually at end of turn).
 * This is allowed only from server to client.
 *
 * @author Robert S. Thomas
 */
public class ClearOffer extends Message
{
    private static final long serialVersionUID = 8889218132911181211L;

    /**
     * Minimum version (1.1.09) which supports playerNumber -1 for clear all.
     * @since 1.1.09
     */
    public static final int VERSION_FOR_CLEAR_ALL = 1109;

    /**
     * Name of game
     */
    private String game;

    /**
     * The seat number, or -1 for all
     */
    private int playerNumber;

    /**
     * Create a ClearOffer message.
     *
     * @param ga  the name of the game
     * @param pn  the seat number, or -1 for all (1.1.09 or newer only)
     */
    public ClearOffer(String ga, int pn)
    {
        messageType = CLEAROFFER;
        game = ga;
        playerNumber = pn;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the seat number, or -1 for all
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * CLEAROFFER sep game sep2 playerNumber
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, playerNumber);
    }

    /**
     * CLEAROFFER sep game sep2 playerNumber
     *
     * @param ga  the name of the game
     * @param pn  the seat number
     * @return the command string
     */
    public static String toCmd(String ga, int pn)
    {
        return CLEAROFFER + sep + ga + sep2 + pn;
    }

    /**
     * Parse the command String into a CLEAROFFER message
     *
     * @param s   the String to parse
     * @return    a CLEAROFFER message, or null of the data is garbled
     */
    public static ClearOffer parseDataStr(String s)
    {
        String ga; // the game name
        int pn; // the seat number

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new ClearOffer(ga, pn);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "ClearOffer:game=" + game + "|playerNumber=" + playerNumber;
    }
}
