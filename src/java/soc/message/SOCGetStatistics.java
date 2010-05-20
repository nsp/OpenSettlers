/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.message;

import java.util.StringTokenizer;


/**
 * This message is a request to get statistics for humans or robots.
 *
 * @author Jim Browan
 **/
public class SOCGetStatistics extends SOCMessage
{
    
    /**
     * Statistics type
     **/
    private String stype;

    /**
     * Create a GetStatistics message.
     *
     * @param st stats type
     **/
    public SOCGetStatistics(String st)
    {
        messageType = GETSTATISTICS;
        stype = st;
    }

    /**
     * @return the statistics type
     **/
    public String getStype()
    {
        return stype;
    }

    /**
     * GETSTATISTICS sep stype
     *
     * @return the command String
     **/
    public String toCmd()
    {
        return toCmd(stype);
    }

    /**
     * GETSTATISTICS sep stype
     *
     * @param st  the statistics type
     **/
    public static String toCmd(String st)
    {
        return GETSTATISTICS + sep + st;
    }

    /**
     * Parse the command String into a GetStatistics message
     *
     * @param s   the String to parse
     * @return    a CreateAccount message, or null of the data is garbled
     **/
    public static SOCGetStatistics parseDataStr(String s)
    {
        String st;

        StringTokenizer stk = new StringTokenizer(s, sep2);

        try
        {
            st = stk.nextToken();
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCGetStatistics(st);
    }

    /**
     * @return a human readable form of the message
     **/
    public String toString()
    {
        String s = "SOCGetStatistics:stype=" + stype;

        return s;
    }
}

