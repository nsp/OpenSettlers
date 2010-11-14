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
 * This message is a request to get statistics for humans or robots.
 *
 * @author Jim Browan
 **/
public class GetStatistics extends Message
{
    
    private static final long serialVersionUID = 5058208009759951809L;
    /**
     * Statistics type
     **/
    private String stype;

    /**
     * Create a GetStatistics message.
     *
     * @param st stats type
     **/
    public GetStatistics(String st)
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
    public static GetStatistics parseDataStr(String s)
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

        return new GetStatistics(st);
    }

    /**
     * @return a human readable form of the message
     **/
    public String toString()
    {
        String s = "GetStatistics:stype=" + stype;

        return s;
    }
}

