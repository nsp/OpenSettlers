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
 * This message says that a player is changing the
 * face icon.
 *
 * @author Robert S. Thomas
 */
public class ChangeFace extends Message
{
    private static final long serialVersionUID = -489094385596864972L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The number player that is changing
     */
    private int playerNumber;

    /**
     * The id of the face image
     */
    private int faceId;

    /**
     * Create a ChangeFace message.
     *
     * @param ga  the name of the game
     * @param pn  the number of the changing player
     * @param id  the id of the face image
     */
    public ChangeFace(String ga, int pn, int id)
    {
        messageType = CHANGEFACE;
        game = ga;
        playerNumber = pn;
        faceId = id;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the number of changing player
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * @return the id of the face image
     */
    public int getFaceId()
    {
        return faceId;
    }

    /**
     * CHANGEFACE sep game sep2 playerNumber sep2 faceId
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, playerNumber, faceId);
    }

    /**
     * CHANGEFACE sep game sep2 playerNumber sep2 faceId
     *
     * @param ga  the name of the game
     * @param pn  the number of the changing player
     * @param id  the id of the face image
     * @return the command string
     */
    public static String toCmd(String ga, int pn, int id)
    {
        return CHANGEFACE + sep + ga + sep2 + pn + sep2 + id;
    }

    /**
     * Parse the command String into a ChangeFace message
     *
     * @param s   the String to parse
     * @return    a ChangeFace message, or null of the data is garbled
     */
    public static ChangeFace parseDataStr(String s)
    {
        String ga; // the game name
        int pn; // the number of the changing player
        int id; // the id of the face image

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
            id = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new ChangeFace(ga, pn, id);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "ChangeFace:game=" + game + "|playerNumber=" + playerNumber + "|faceId=" + faceId;
    }
}
