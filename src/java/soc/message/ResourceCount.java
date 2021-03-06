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
 * This message has the total resource count for a player
 *
 * @author Robert S. Thomas
 */
public class ResourceCount extends Message
{
    private static final long serialVersionUID = 384689414947089103L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The seat number
     */
    private int playerNumber;

    /**
     * The resource count
     */
    private int count;

    /**
     * Create a ResourceCount message.
     *
     * @param ga  the name of the game
     * @param pn  the seat number
     * @param rc  the resource count
     */
    public ResourceCount(String ga, int pn, int rc)
    {
        messageType = RESOURCECOUNT;
        game = ga;
        playerNumber = pn;
        count = rc;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the seat number
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * @return the recource count
     */
    public int getCount()
    {
        return count;
    }

    /**
     * RESOURCECOUNT sep game sep2 playerNumber sep2 count
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, playerNumber, count);
    }

    /**
     * RESOURCECOUNT sep game sep2 playerNumber sep2 count
     *
     * @param ga  the name of the game
     * @param pn  the seat number
     * @param rc  the resource count
     * @return the command string
     */
    public static String toCmd(String ga, int pn, int rc)
    {
        return RESOURCECOUNT + sep + ga + sep2 + pn + sep2 + rc;
    }

    /**
     * Parse the command String into a ResourceCount message
     *
     * @param s   the String to parse
     * @return    a ResourceCount message, or null of the data is garbled
     */
    public static ResourceCount parseDataStr(String s)
    {
        String ga; // the game name
        int pn; // the seat number
        int rc; // the resource count

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
            rc = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new ResourceCount(ga, pn, rc);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "ResourceCount:game=" + game + "|playerNumber=" + playerNumber + "|count=" + count;
    }
}
