/**
 * Open Settlers - an open implementation of the game Settlers of Catan
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.game;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This exception indicates game option(s) too new for a client.
 * @see GameOption#optionsMinimumVersion(Hashtable)
 * @see GameOption#optionsNewerThanVersion(int, boolean, boolean, Hashtable)
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 * @since 1.1.07
 */
public class GameOptionVersionException extends IllegalArgumentException
{
    private static final long serialVersionUID = -6341950670774449066L;

    /** Minimum client version required by game options */
    public final int gameOptsVersion;

    /** Requesting client's version */
    public final int cliVersion;

    /**
     * The {@link GameOption}(s) which are too new,
     *     as returned by {@link GameOption#optionsNewerThanVersion(int, boolean, boolean, Hashtable)}
     */
    public Vector problemOptionsTooNew;

    /**
     * @param optVers Minimum client version required by game options
     * @param cliVers Requesting client's version
     * @param optsValuesTooNew The {@link GameOption}(s) which are too new,
     *     as returned by {@link GameOption#optionsNewerThanVersion(int, boolean, boolean, Hashtable)}
     */
    public GameOptionVersionException(int optVers, int cliVers, Vector optsValuesTooNew)
    {
        super("Client version vs game options");
        gameOptsVersion = optVers;
        cliVersion = cliVers;
        problemOptionsTooNew = optsValuesTooNew;
    }

    /**
     * Build the list of "problem options" as a string, separated by "," (Message.SEP2).
     * @return list of options (and values?) too new, or "" if none
     */
    public String problemOptionsList()
    {
        if (problemOptionsTooNew == null)
            return "";

        StringBuffer sb = new StringBuffer();
        boolean hadAny = false;
        for (Enumeration e = problemOptionsTooNew.elements(); e.hasMoreElements(); )
        {
            Object opt = e.nextElement();
            String item;
            if (opt instanceof GameOption)
                item = ((GameOption) opt).optKey;
            else
                item = opt.toString();
            if (hadAny)
                sb.append(",");  // "," == Message.SEP2
            sb.append(item);
            hadAny = true;
        }
        return sb.toString();
    }

}
