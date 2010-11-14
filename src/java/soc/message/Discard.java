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
 * This message means that a player is discarding
 *
 * @author Robert S. Thomas
 */
public class Discard extends Message
{
    private static final long serialVersionUID = -5424002864917271133L;

    /**
     * Name of game
     */
    private String game;

    /**
     * The set of resources being discarded
     */
    private ResourceSet resources;

    /**
     * Create a Discard message.
     *
     * @param ga  the name of the game
     * @param cl  the ammount of clay being discarded
     * @param or  the ammount of ore being discarded
     * @param sh  the ammount of sheep being discarded
     * @param wh  the ammount of wheat being discarded
     * @param wo  the ammount of wood being discarded
     * @param uk  the ammount of unknown resources being discarded
     */
    public Discard(String ga, int cl, int or, int sh, int wh, int wo, int uk)
    {
        messageType = DISCARD;
        game = ga;
        resources = new ResourceSet(cl, or, sh, wh, wo, uk);
    }

    /**
     * Create a Discard message.
     *
     * @param ga  the name of the game
     * @param rs  the resources being discarded
     */
    public Discard(String ga, int pn, ResourceSet rs)
    {
        messageType = DISCARD;
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
     * @return the set of resources being discarded
     */
    public ResourceSet getResources()
    {
        return resources;
    }

    /**
     * DISCARD sep game sep2 clay sep2 ore sep2 sheep sep2
     * wheat sep2 wood sep2 unknown
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, resources.getAmount(ResourceConstants.CLAY), resources.getAmount(ResourceConstants.ORE), resources.getAmount(ResourceConstants.SHEEP), resources.getAmount(ResourceConstants.WHEAT), resources.getAmount(ResourceConstants.WOOD), resources.getAmount(ResourceConstants.UNKNOWN));
    }

    /**
     * DISCARD sep game sep2 clay sep2 ore sep2 sheep sep2
     * wheat sep2 wood sep2 unknown
     *
     * @param ga  the name of the game
     * @param rs  the resources being discarded
     * @return the command string
     */
    public static String toCmd(String ga, ResourceSet rs)
    {
        return toCmd(ga, rs.getAmount(ResourceConstants.CLAY), rs.getAmount(ResourceConstants.ORE), rs.getAmount(ResourceConstants.SHEEP), rs.getAmount(ResourceConstants.WHEAT), rs.getAmount(ResourceConstants.WOOD), rs.getAmount(ResourceConstants.UNKNOWN));
    }

    /**
     * DISCARD sep game sep2 clay sep2 ore sep2 sheep sep2
     * wheat sep2 wood sep2 unknown
     *
     * @param ga  the name of the game
     * @param cl  the ammount of clay being discarded
     * @param or  the ammount of ore being discarded
     * @param sh  the ammount of sheep being discarded
     * @param wh  the ammount of wheat being discarded
     * @param wo  the ammount of wood being discarded
     * @param uk  the ammount of unknown resources being discarded
     * @return the command string
     */
    public static String toCmd(String ga, int cl, int or, int sh, int wh, int wo, int uk)
    {
        return DISCARD + sep + ga + sep2 + cl + sep2 + or + sep2 + sh + sep2 + wh + sep2 + wo + sep2 + uk;
    }

    /**
     * Parse the command String into a Discard message
     *
     * @param s   the String to parse
     * @return    a Discard message, or null of the data is garbled
     */
    public static Discard parseDataStr(String s)
    {
        String ga; // the game name
        int cl; // the ammount of clay being discarded  
        int or; // the ammount of ore being discarded  
        int sh; // the ammount of sheep being discarded  
        int wh; // the ammount of wheat being discarded
        int wo; // the ammount of wood being discarded  
        int uk; // the ammount of unknown resources being discarded  

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            cl = Integer.parseInt(st.nextToken());
            or = Integer.parseInt(st.nextToken());
            sh = Integer.parseInt(st.nextToken());
            wh = Integer.parseInt(st.nextToken());
            wo = Integer.parseInt(st.nextToken());
            uk = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new Discard(ga, cl, or, sh, wh, wo, uk);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "Discard:game=" + game + "|resources=" + resources;
    }
}
