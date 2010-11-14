/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file Copyright (C) 2008 Jeremy D Monin <jeremy@nand.net>
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

// import java.util.StringTokenizer;


/**
 * Template for per-game message types with 2 integer parameters.
 * You will have to write parseDataStr, because of its return
 * type and because it's static.
 *<P>
 * Sample implementation:
 *<code>
 *   // format of s: LONGESTROAD sep game sep2 seatnumber sep2 coordinates
 *   public static LongestRoad parseDataStr(String s)
 *   {
 *       String ga; // the game name
 *       int pn; // the seat number
 *       int co; // coordinates
 *
 *       StringTokenizer st = new StringTokenizer(s, sep2);
 *
 *       try
 *       {
 *           ga = st.nextToken();
 *           pn = Integer.parseInt(st.nextToken());
 *           co = Integer.parseInt(st.nextToken());
 *       }
 *       catch (Exception e)
 *       {
 *           return null;
 *       }
 *
 *        return new MoveRobber(ga, pn, co);
 *   }
 *</code>
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
public abstract class MessageTemplate2i extends Message
{
    private static final long serialVersionUID = 7210885319216195158L;

    /**
     * Name of the game.
     */
    protected String game;

    /**
     * First integer parameter.
     */
    protected int p1;

    /**
     * Second integer parameter.
     */
    protected int p2;

    /**
     * Create a new message.
     *
     * @param id  Message type ID
     * @param ga  Name of game this message is for
     * @param p1  Parameter 1
     * @param p2  Parameter 2
     */
    protected MessageTemplate2i(int id, String ga, int p1, int p2)
    {
        messageType = id;
        game = ga;
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the first parameter
     */
    public int getParam1()
    {
        return p1;
    }

    /**
     * @return the second parameter
     */
    public int getParam2()
    {
        return p2;
    }

    /**
     * MESSAGETYPE sep game sep2 param1 sep2 param2
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(messageType, game, p1, p2);
    }

    /**
     * MESSAGETYPE sep game sep2 param1 sep2 param2
     *
     * @param messageType The message type id
     * @param ga  the new game name
     * @param param1 The first parameter
     * @param param2 The second parameter
     * @return    the command string
     */
    public static String toCmd(int messageType, String ga, int param1, int param2)
    {
        return Integer.toString(messageType) + sep + ga + sep2 + param1 + sep2 + param2;
    }

    /**
     * Parse the command String into a MessageType message
     *
     * @param s   the String to parse
     * @return    a MoveRobber message, or null if parsing errors
    public static MoveRobber parseDataStr(String s)
    {
        String ga; // the game name
        int pn; // the seat number
        int co; // coordinates

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            na = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
            co = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new MoveRobber(na, pn, co);
    }
     */

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return getClassNameShort() + ":game=" + game
            + "|param1=" + p1 + "|param2=" + p2;
    }
}
