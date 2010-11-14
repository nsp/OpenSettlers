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
 * This message means that the player is accepting an offer.
 *<P>
 * Sent from accepting player's client to server.
 * If the trade is allowed, also sent from server to all players so
 * that robots can learn that news.
 *<UL>
 * <LI> Message to server is in response to a {@link MakeOffer} sent earlier this turn to client.
 * <LI> Followed by (to all from server) {@link PlayerElement}s, {@link GameTextMsg}, {@link ClearOffer}s,
 *      and (for robots' benefit) the received ACCEPTOFFER is re-sent from
 *      server to all clients.
 *</UL>
 * @author Robert S. Thomas
 * @see RejectOffer
 */
public class AcceptOffer extends Message
{
    private static final long serialVersionUID = -7997360736244701725L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The number of the accepting player
     */
    private int accepting;

    /**
     * The number of the offering player
     */
    private int offering;

    /**
     * Create a AcceptOffer message.
     *
     * @param ga  the name of the game
     * @param ac  the number of the accepting player
     * @param of  the number of the offering player
     */
    public AcceptOffer(String ga, int ac, int of)
    {
        messageType = ACCEPTOFFER;
        game = ga;
        accepting = ac;
        offering = of;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the number of the accepting player
     */
    public int getAcceptingNumber()
    {
        return accepting;
    }

    /**
     * @return the number of the offering player
     */
    public int getOfferingNumber()
    {
        return offering;
    }

    /**
     * ACCEPTOFFER sep game sep2 accepting sep2 offering
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, accepting, offering);
    }

    /**
     * ACCEPTOFFER sep game sep2 accepting sep2 offering
     *
     * @param ga  the name of the game
     * @param ac  the number of the accepting player
     * @param of  the number of the offering player
     * @return the command string
     */
    public static String toCmd(String ga, int ac, int of)
    {
        return ACCEPTOFFER + sep + ga + sep2 + ac + sep2 + of;
    }

    /**
     * Parse the command String into a StartGame message
     *
     * @param s   the String to parse
     * @return    a StartGame message, or null of the data is garbled
     */
    public static AcceptOffer parseDataStr(String s)
    {
        String ga; // the game name
        int ac; // the number of the accepting player
        int of; //the number of the offering player

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            ac = Integer.parseInt(st.nextToken());
            of = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new AcceptOffer(ga, ac, of);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "AcceptOffer:game=" + game + "|accepting=" + accepting + "|offering=" + offering;
    }
}
