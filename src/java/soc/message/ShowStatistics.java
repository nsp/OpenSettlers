/*
 * ShowStatistics.java
 *
 * Created on February 16, 2005, 1:36 PM
 */

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

import soc.util.PlayerInfo;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;


/**
 * This message returns statistics for humans or robots
 *
 * @author Jim Browan
 **/
public class ShowStatistics extends Message
{
    private static final long serialVersionUID = 5356459606391187792L;

    /**
     * Type of statistics
     **/
    private String stype;
    
    /**
     * The statistics
     **/
    private Vector statistics;

    /**
     * Create a ShowStats message.
     *
     * @param st     type of statistics(human|robot)
     * @param stats  statistics array
     **/
    public ShowStatistics(String st, Vector stats)
    {
        messageType = SHOWSTATS;
        stype = st;
        statistics = stats;
    }

    /**
     * @return the type of statistics
     **/
    public String getStype()
    {
        return stype;
    }
    
    /**
     * @return the statistics
     **/
    public Vector getStatistics()
    {
        return statistics;
    }

    /**
     * SHOWSTATS sep stype sep2 statistics
     *
     * @return the command String
     **/
    public String toCmd()
    {
        return toCmd(stype, statistics);
    }

    /**
     * SHOWSTATS sep stype sep name sep2 rank sep2 wins sep2 losses sep2
     * totalPoints sep2 averagePoints sep2 winRatio sep name ...
     *
     * @param st     the type of statistics
     * @param stats  Vector of soc.util.PlayerInfo objects
     * @return       the type and statistics as a string
     **/
    public static String toCmd(String st, Vector stats)
    {
        StringBuffer data = new StringBuffer();
        data.append(SHOWSTATS).append(sep).append(st);

        Enumeration statEnum = stats.elements();
        while (statEnum.hasMoreElements())
        {
            PlayerInfo info = (PlayerInfo) statEnum.nextElement();
            data.append(sep);
            
            data.append(info.getName());
            data.append(sep2);
            data.append(info.getRank());
            data.append(sep2);
            data.append(info.getWins());
            data.append(sep2);
            data.append(info.getLosses());
            data.append(sep2);
            data.append(info.getTotalPoints());
            data.append(sep2);
            data.append(info.getAveragePoints());
            data.append(sep2);
            data.append(info.getWinRatio());
        }
        return data.toString();
    }

    /**
     * Parse the data String into a Show Stats message
     *
     * @param s   the String to parse
     * @return    a Show Stats message, or null of the data is garbled
     */
    public static ShowStatistics parseDataStr(String s)
    {
        Vector statistics = new Vector();
        String st = null;

        try
        {
            StringTokenizer stk = new StringTokenizer(s, sep);

            st = stk.nextToken();
            
            while (stk.hasMoreTokens())
            {
                String s2 = stk.nextToken();
                StringTokenizer i = new StringTokenizer(s2, sep2);
                
                PlayerInfo info = new PlayerInfo();
                info.setName(i.nextToken());
                info.setRank(Integer.parseInt(i.nextToken()));
                info.setWins(Integer.parseInt(i.nextToken()));
                info.setLosses(Integer.parseInt(i.nextToken()));
                info.setTotalPoints(Integer.parseInt(i.nextToken()));
                info.setAveragePoints(Float.parseFloat(i.nextToken()));
                info.setWinRatio(Float.parseFloat(i.nextToken()));
                
                statistics.add(info);
            }
        }
        catch (Exception e)
        {
            System.err.println("ShowStatistics.parseDataStr error: " + e);
            return null;
        }

        return new ShowStatistics(st, statistics);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer("ShowStatistics:stype=");
        buf.append(stype).append("|statistics=");
        
        Enumeration statEnum = statistics.elements();
        while (statEnum.hasMoreElements())
        {
            PlayerInfo info = (PlayerInfo) statEnum.nextElement();
            buf.append("|").append(info);
        }
        
        return buf.toString();
    }
}
