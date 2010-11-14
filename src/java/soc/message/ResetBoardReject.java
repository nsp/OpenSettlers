/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * This file Copyright (C) 2008 Jeremy D. Monin <jeremy@nand.net>
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

/**
 * This message from server informs all clients that voting has ended,
 * and the board reset request has been rejected.
 *<P>
 * Follows {@link ResetBoardRequest}, and then usually {@link ResetBoardVote}, messages.
 *
 * @see ResetBoardRequest
 * @author Jeremy D. Monin <jeremy@nand.net>
 *
 */
public class ResetBoardReject extends MessageTemplate0
{
    private static final long serialVersionUID = -7937303264050050122L;

    /**
     * Create a ResetBoardReject message.
     *
     * @param ga  the name of the game
     */
    public ResetBoardReject(String ga)
    {
        super(RESETBOARDREJECT, ga);
    }

    /**
     * Parse the command String into a ResetBoardReject message
     *
     * @param s   the String to parse
     * @return    a ResetBoardAuth message, or null if the data is garbled
     */
    public static ResetBoardReject parseDataStr(String s)
    {
        // s is just the game name
        return new ResetBoardReject(s);
    }

    /**
     * Minimum version where this message type is used.
     * RESETBOARDREJECT introduced in 1.1.00 for reset-board feature.
     * @return Version number, 1100 for JSettlers 1.1.00.
     */
    public int getMinimumVersion()
    {
        return 1100;
    }

}
