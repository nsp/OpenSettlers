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
 * This message means that the player is rejecting an offer.
 *<P>
 * Sent from rejecting player's client to server.
 * The server then sends a copy of the message to all players
 * to announce the rejection.
 *<UL>
 * <LI> Message to server is in response to a {@link MakeOffer} sent earlier this turn to client.
 * <LI> Followed by (from server, to all clients) {@link RejectOffer} with the same data.
 *</UL>
 * @author Robert S. Thomas
 * @see AcceptOffer
 */
public class RejectOffer extends Message
{
    private static final long serialVersionUID = -2529083872491376072L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The seat number
     */
    private int playerNumber;

    /**
     * Create a RejectOffer message.
     *
     * @param ga  the name of the game
     * @param pn  the seat number
     */
    public RejectOffer(String ga, int pn)
    {
        messageType = REJECTOFFER;
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
     * @return the seat number
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * REJECTOFFER sep game sep2 playerNumber
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, playerNumber);
    }

    /**
     * REJECTOFFER sep game sep2 playerNumber
     *
     * @param ga  the name of the game
     * @param pn  the seat number
     * @return the command string
     */
    public static String toCmd(String ga, int pn)
    {
        return REJECTOFFER + sep + ga + sep2 + pn;
    }

    /**
     * Parse the command String into a StartGame message
     *
     * @param s   the String to parse
     * @return    a StartGame message, or null of the data is garbled
     */
    public static RejectOffer parseDataStr(String s)
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

        return new RejectOffer(ga, pn);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "RejectOffer:game=" + game + "|playerNumber=" + playerNumber;
    }
}
