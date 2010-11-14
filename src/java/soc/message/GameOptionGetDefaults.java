/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file Copyright (C) 2009 Jeremy D Monin <jeremy@nand.net>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Jeremy D. Monin <jeremy@nand.net>
 * @since 1.1.07
 **/
package soc.message;

/**
 * Information on current defaults for new games' {@link soc.game.GameOption game options}.
 * Based on server's current values ({@link soc.game.GameOption#getIntValue() .getIntValue()},
 * not {@link soc.game.GameOption#defaultIntValue .defaultIntValue} field).
 *<P>
 * Server responds to client's GAMEOPTIONGETDEFAULTS by sending its own GAMEOPTIONGETDEFAULTS.
 * All of server's known options are sent, except empty string-valued options. 
 * Depending on client version, server's response may include option names that
 * the client is too old to use; the client is able to ignore them.
 * If the client asks about such an option (by sending {@link GameOptionInfo GAMEOPTIONINFO}),
 * the server will respond with {@link soc.game.GameOption#OTYPE_UNKNOWN GAMEOPTIONINFO(OTYPE_UNKNOWN)}.
 *<P>
 * Introduced in 1.1.07; check server version against {@link NewGameWithOptions#VERSION_FOR_NEWGAMEWITHOPTIONS}
 * before sending this message.
 *<P>
 * Robot clients don't need to know about or handle this message type,
 * because they don't create games.
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 * @since 1.1.07
 */
public class GameOptionGetDefaults extends Message
{
    private static final long serialVersionUID = 145679725378575755L;
    /**
     * String of the options (name-value pairs) as sent over network
     */
    private String opts;

    /**
     * Create a GameOptionGetDefaults message.
     *
     * @param opts  the options string, or null if none (client to server).
     *              To create the string, call
     *              {@link soc.game.GameOption#packOptionsToString(Hashtable, boolean) GameOption.packOptionsToString(opts, true)}.
     */
    public GameOptionGetDefaults(String opts)
    {
        messageType = GAMEOPTIONGETDEFAULTS;
        this.opts = opts;
    }

    /**
     * Get the string of option name-value pairs sent over the network.
     * To turn this into a hashtable of {@link soc.game.GameOption SOCGameOptions},
     * call {@link soc.game.GameOption#parseOptionsToHash(String) GameOption.parseOptionsToHash()}.
     * @return the string of options, or null if none (client to server)
     */
    public String getOpts()
    {
        return opts;
    }

    /**
     * GAMEOPTIONGETDEFAULTS [sep opts]
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(opts);
    }

    /**
     * GAMEOPTIONGETDEFAULTS [sep opts]
     *
     * @param opts  the options string, or null if none (cli->serv)
     * @return    the command string
     */
    public static String toCmd(String opts)
    {
	if (opts != null)
	    return GAMEOPTIONGETDEFAULTS + sep + opts;
	else
	    return Integer.toString(GAMEOPTIONGETDEFAULTS);
    }

    /**
     * Parse the command String into a GameOptionGetDefaults message
     *
     * @param s   the String to parse
     * @return    a GameOptionGetDefaults message
     */
    public static GameOptionGetDefaults parseDataStr(String s)
    {
	if (s.length() == 0)
	    s = null;
        return new GameOptionGetDefaults(s);
    }

    /**
     * Minimum version where this message type is used.
     * GAMEOPTIONGETDEFAULTS introduced in 1.1.07 for game-options feature.
     * @return Version number, 1107 for JSettlers 1.1.07.
     */
    public int getMinimumVersion() { return 1107; }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "GameOptionGetDefaults:opts=" + opts;
    }
}
