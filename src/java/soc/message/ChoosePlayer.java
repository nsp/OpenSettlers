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
 * This message says which player the current player wants to
 * steal from.
 *
 * @author Robert S. Thomas
 */
public class ChoosePlayer extends Message
{
    private static final long serialVersionUID = -2890995142291739510L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The number of the chosen player
     */
    private int choice;

    /**
     * Create a ChoosePlayer message.
     *
     * @param ga  the name of the game
     * @param ch  the number of the chosen player
     */
    public ChoosePlayer(String ga, int ch)
    {
        messageType = CHOOSEPLAYER;
        game = ga;
        choice = ch;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the number of the chosen player
     */
    public int getChoice()
    {
        return choice;
    }

    /**
     * CHOOSEPLAYER sep game sep2 choice
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, choice);
    }

    /**
     * CHOOSEPLAYER sep game sep2 choice
     *
     * @param ga  the name of the game
     * @param ch  the number of the chosen player
     * @return the command string
     */
    public static String toCmd(String ga, int ch)
    {
        return CHOOSEPLAYER + sep + ga + sep2 + ch;
    }

    /**
     * Parse the command String into a ChoosePlayer message
     *
     * @param s   the String to parse
     * @return    a ChoosePlayer message, or null of the data is garbled
     */
    public static ChoosePlayer parseDataStr(String s)
    {
        String ga; // the game name
        int ch; // the number of the chosen player 

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            ch = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new ChoosePlayer(ga, ch);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "ChoosePlayer:game=" + game + "|choice=" + choice;
    }
}
