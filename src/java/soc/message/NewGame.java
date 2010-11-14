/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2009 Jeremy D. Monin <jeremy@nand.net>
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
 * This message to all clients means that a new game has been created.
 * If the client is requesting the game, NEWGAME will be followed
 * by JOINGAMEAUTH.
 *<P>
 * Version 1.1.06 and later:
 * Game name may include a marker prefix if the client can't join;
 * see {@link Games#MARKER_THIS_GAME_UNJOINABLE}.
 * This marker will be retained within the game name returned by
 * {@link #getGame()}.
 *
 * @author Robert S Thomas
 */
public class NewGame extends Message
{
    private static final long serialVersionUID = -7687054390443240821L;
    /**
     * Name of the new game.
     */
    private String game;

    /**
     * Create a NewGame message.
     *
     * @param ga  the name of the game; may have
     *            the {@link Games#MARKER_THIS_GAME_UNJOINABLE} prefix.
     */
    public NewGame(String ga)
    {
        messageType = NEWGAME;
        game = ga;
    }

    /**
     * @return the name of the game; may have
     *         the {@link Games#MARKER_THIS_GAME_UNJOINABLE} prefix.
     */
    public String getGame()
    {
        return game;
    }

    /**
     * NEWGAME sep game
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(game);
    }

    /**
     * NEWGAME sep game
     *
     * @param ga  the name of the new game; may have
     *            the {@link Games#MARKER_THIS_GAME_UNJOINABLE} prefix.
     * @return    the command string
     */
    public static String toCmd(String ga)
    {
        return NEWGAME + sep + ga;
    }

    /**
     * Parse the command String into a NewGame message
     *
     * @param s   the String to parse
     * @return    a NewGame message
     */
    public static NewGame parseDataStr(String s)
    {
        return new NewGame(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "NewGame:game=" + game;
    }
}
