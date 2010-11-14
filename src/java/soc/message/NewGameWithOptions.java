/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * This file Copyright (C) 2009 Jeremy D. Monin <jeremy@nand.net>
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

import java.util.Hashtable;
import java.util.StringTokenizer;

import soc.game.Game;
import soc.game.GameOption;

/**
 * This broadcast message from server announces a new game, with a certain
 * version and possibly {@link GameOption game options}, to all clients.
 *<UL>
 * <LI> This message is followed by {@link JoinGameAuth JOINGAMEAUTH}
 *      to the requesting client, so they will join the game they just created.
 *</UL>
 * Introduced in 1.1.07; check server version against {@link #VERSION_FOR_NEWGAMEWITHOPTIONS}
 * before sending this message.
 *<P>
 * Game name may include a prefix marker if the client can't join;
 * see {@link Games#MARKER_THIS_GAME_UNJOINABLE}.
 * This marker will be retained within the game name returned by
 * {@link #getGame()}.
 *<P>
 * Just like {@link NewGame NEWGAME}, robot clients don't need to
 * know about or handle this message type.
 *
 * @author Jeremy D. Monin <jeremy@nand.net>
 * @since 1.1.07
 */
public class NewGameWithOptions extends MessageTemplate2s
{
    private static final long serialVersionUID = 3154889703429801860L;

    /**
     * Minimum version (1.1.07) of client/server which recognize
     * and send NEWGAMEWITHOPTIONS and other messages related
     * to {@link soc.game.GameOption}.
     */
    public static final int VERSION_FOR_NEWGAMEWITHOPTIONS = 1107;

    private int gameMinVers = -1;

    // private Hashtable opts = null;

    /**
     * Create a NewGameWithOptions message.
     *
     * @param ga  the name of the game; may have the
     *            {@link Games#MARKER_THIS_GAME_UNJOINABLE} prefix.
     *            minVers also designates if the game is joinable.
     * @param optstr Requested game options, in the format returned by
     *            {@link soc.game.GameOption#packOptionsToString(Hashtable, boolean) GameOption.packOptionsToString(opts, false)},
     *            or null
     * @param minVers Minimum client version required for this game, or -1.
     */
    public NewGameWithOptions(String ga, String optstr, int minVers)
    {
        super(NEWGAMEWITHOPTIONS, ga, Integer.toString(minVers),
	      ((optstr != null) && (optstr.length() > 0) ? optstr : "-"));
	gameMinVers = minVers;
	// p1 = minVers
	// p2 = optstr
    }

    /**
     * Create a NewGameWithOptions message.
     *
     * @param ga  the name of the game; may have the
     *            {@link Games#MARKER_THIS_GAME_UNJOINABLE} prefix.
     *            minVers also designates if the game is joinable.
     * @param opts Hashtable of {@link GameOption game options}, or null
     * @param minVers Minimum client version for this game, or -1.
     *                Ignored if sent from client to server. Calculated at
     *                server and sent out to all clients.
     */
    public NewGameWithOptions(String ga, Hashtable opts, int minVers)
    {
	this(ga, GameOption.packOptionsToString(opts, false), minVers);
	// p1 = minVers
	// p2 = optstr
    }

    /**
     * @return the options for the new game, in the format returned by
     *         {@link soc.game.GameOption#packOptionsToString(Hashtable, boolean) GameOption.packOptionsToString(opts, false)},
     *         or null if no options
     */
    public String getOptionsString()
    {
        return p2;
    }

    /**
     * @return the game's minimum required client version, or -1.
     *         This is when sending from server to all clients.
     */
    public int getMinVersion()
    {
        return gameMinVers;
    }

    /**
     * NEWGAMEWITHOPTIONS sep game sep2 minVers sep2 optionstring
     *
     * @param ga  the name of the game; the game name may have
     *            the {@link Games#MARKER_THIS_GAME_UNJOINABLE} prefix.
     * @param optstr Requested game options, in the format returned by
     *            {@link soc.game.GameOption#packOptionsToString(Hashtable, boolean)},
     *            or null
     * @param minVers Minimum client version required, or -1
     * @return the command string
     */
    public static String toCmd(String ga, String optstr, int minVers)
    {
        return NEWGAMEWITHOPTIONS + sep + ga + sep2 + Integer.toString(minVers) + sep2
	    + (((optstr != null) && (optstr.length() > 0)) ? optstr : "-");
    }

    /**
     * NEWGAMEWITHOPTIONS sep game sep2 minVers sep2 optionstring
     *
     * @param ga  the name of the game; the game name may have
     *            the {@link Games#MARKER_THIS_GAME_UNJOINABLE} prefix.
     * @param opts Requested game options, as a hashtable of {@link soc.game.GameOption}
     * @param minVers Minimum client version required, or -1     
     * @return the command string
     */
    public static String toCmd(String ga, Hashtable opts, int minVers)
    {
	return toCmd(ga, GameOption.packOptionsToString(opts, false), minVers);
    }

    /**
     * NEWGAMEWITHOPTIONS sep game sep2 minVers sep2 optionstring
     *<P>
     * Game's options and minimum required version will be extracted from game.
     *
     * @param ga  the game
     * @return the command string
     */
    public static String toCmd(Game ga)
    {
        return toCmd(ga.getName(),
                GameOption.packOptionsToString(ga.getGameOptions(), false),
                ga.getClientVersionMinRequired());
    }

    /**
     * Parse the command String into a NewGameWithOptions message.
     *
     * @param s   the String to parse: NEWGAMEWITHOPTIONS sep game sep2 opts
     * @return    a NewGameWithOptions message, or null if the data is garbled
     */
    public static NewGameWithOptions parseDataStr(String s)
    {
        String ga; // the game name
	int minVers;
	String opts;

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
	    minVers = Integer.parseInt(st.nextToken());
	    opts = st.nextToken(sep);  // NOT sep2! options may contain commas.	 
	    // Will begin with "," (sep2) due to the separator change. This is cosmetic only.
        }
        catch (Exception e)
        {
            return null;
        }
	if (opts.equals("-"))
	    opts = null;

        return new NewGameWithOptions(ga, opts, minVers);
    }

    /**
     * Minimum version where this message type is used.
     * NEWGAMEWITHOPTIONS introduced in 1.1.07 for game-options feature.
     * @return Version number, 1107 for JSettlers 1.1.07.
     */
    public int getMinimumVersion()
    {
        return VERSION_FOR_NEWGAMEWITHOPTIONS; // == 1107;
    }

}
