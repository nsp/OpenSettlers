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

import soc.game.ResourceConstants;
import soc.game.ResourceSet;

import java.util.StringTokenizer;


/**
 * This message says which resources the player picked
 * for a Discovery card
 *
 * @author Robert S. Thomas
 */
public class DiscoveryPick extends Message
{
    private static final long serialVersionUID = 2149628093687815026L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The chosen resources
     */
    private ResourceSet resources;

    /**
     * Create a DiscoveryPick message.
     *
     * @param ga   the name of the game
     * @param rs   the chosen resources
     */
    public DiscoveryPick(String ga, ResourceSet rs)
    {
        messageType = DISCOVERYPICK;
        game = ga;
        resources = rs;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the chosen resources
     */
    public ResourceSet getResources()
    {
        return resources;
    }

    /**
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, resources);
    }

    /**
     * @return the command string
     *
     * @param ga  the name of the game
     * @param rs   the chosen resources
     */
    public static String toCmd(String ga, ResourceSet rs)
    {
        String cmd = DISCOVERYPICK + sep + ga;

        for (int i = ResourceConstants.CLAY; i <= ResourceConstants.WOOD;
                i++)
        {
            cmd += (sep2 + rs.getAmount(i));
        }

        return cmd;
    }

    /**
     * Parse the command String into a DiscoveryPick message
     *
     * @param s   the String to parse
     * @return    a DiscoveryPick message, or null of the data is garbled
     */
    public static DiscoveryPick parseDataStr(String s)
    {
        String ga; // the game name
        ResourceSet rs; // the chosen resources

        rs = new ResourceSet();

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();

            /**
             * Note: this only works if ResourceConstants.CLAY == 1
             */
            for (int i = 1; i <= ResourceConstants.WOOD; i++)
            {
                rs.setAmount(Integer.parseInt(st.nextToken()), i);
            }
        }
        catch (Exception e)
        {
            return null;
        }

        return new DiscoveryPick(ga, rs);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "DiscoveryPick:game=" + game + "|resources=" + resources;
    }
}
