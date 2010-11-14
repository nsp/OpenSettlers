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


/**
 * This message means that a player wants to start the game
 *
 * @author Robert S. Thomas
 */
public class StartGame extends Message
{
    private static final long serialVersionUID = -765289531709655693L;
    /**
     * Name of game
     */
    private String game;

    /**
     * Create a StartGame message.
     *
     * @param ga  the name of the game
     */
    public StartGame(String ga)
    {
        messageType = STARTGAME;
        game = ga;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * STARTGAME sep game
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game);
    }

    /**
     * STARTGAME sep game
     *
     * @param ga  the name of the game
     * @return the command string
     */
    public static String toCmd(String ga)
    {
        return STARTGAME + sep + ga;
    }

    /**
     * Parse the command String into a StartGame message
     *
     * @param s   the String to parse
     * @return    a StartGame message, or null of the data is garbled
     */
    public static StartGame parseDataStr(String s)
    {
        return new StartGame(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "StartGame:game=" + game;
    }
}
