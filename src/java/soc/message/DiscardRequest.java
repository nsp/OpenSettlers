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

import java.util.StringTokenizer;


/**
 * This message requests that the player discard a particular number of cards
 *
 * @author Robert S. Thomas
 */
public class DiscardRequest extends Message
{
    private static final long serialVersionUID = -8323473015877654160L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The number of discards
     */
    private int numDiscards;

    /**
     * Create a DiscardRequest message.
     *
     * @param ga  the name of the game
     * @param nd  the number of discards
     */
    public DiscardRequest(String ga, int nd)
    {
        messageType = DISCARDREQUEST;
        game = ga;
        numDiscards = nd;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the number of discards
     */
    public int getNumberOfDiscards()
    {
        return numDiscards;
    }

    /**
     * DISCARDREQUEST sep game sep2 numDiscards
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, numDiscards);
    }

    /**
     * DISCARDREQUEST sep game sep2 numDiscards
     *
     * @param ga  the name of the game
     * @param nd  the number of discards
     * @return the command string
     */
    public static String toCmd(String ga, int nd)
    {
        return DISCARDREQUEST + sep + ga + sep2 + nd;
    }

    /**
     * Parse the command String into a DiscardRequest message
     *
     * @param s   the String to parse
     * @return    a DiscardRequest message, or null of the data is garbled
     */
    public static DiscardRequest parseDataStr(String s)
    {
        String ga; // the game name
        int nd; // the number of discards

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            nd = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new DiscardRequest(ga, nd);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "DiscardRequest:game=" + game + "|numDiscards=" + numDiscards;
    }
}
