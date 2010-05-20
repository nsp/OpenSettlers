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
package soc.util;

import java.io.Serializable;

/**
 * This is a class to store a list of robot parameters.
 *
 * @author Chadwick McHenry
 */
public class SOCPlayerInfo implements Serializable
{
    private static final long serialVersionUID = 7267783219068658395L;
    public static final String HUMAN = "human";
    public static final String ROBOT = "robot";
    
    protected String name;
    protected int rank;
    protected int wins;
    protected int losses;
    protected int totalPoints;
    protected float averagePoints;
    protected float winRatio;

    /**
     */
    public SOCPlayerInfo()
    {
    }

    /**
     * constructor
     *
     * @param params  the robot parameters
     */
    public SOCPlayerInfo(SOCPlayerInfo info)
    {
        name = info.name;
        rank = info.rank;
        wins = info.wins;
        losses = info.losses;
        totalPoints = info.totalPoints;
        averagePoints = info.averagePoints;
        winRatio = info.winRatio;
    }
    
    /**
     * Get the <code>Name</code> value.
     *
     * @return an <code>int</code> value
     */
    public String getName()
    {
        return name;
    }
    
    /**
     * Set the <code>Name</code> value.
     *
     * @param newName The new Name value.
     */
    public void setName(String newName)
    {
        this.name = newName;
    }

    /**
     * Get the <code>Rank</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getRank()
    {
        return rank;
    }

    /**
     * Set the <code>Rank</code> value.
     *
     * @param newRank The new Rank value.
     */
    public final void setRank(final int newRank)
    {
        this.rank = newRank;
    }

    /**
     * Get the <code>Wins</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getWins()
    {
        return wins;
    }

    /**
     * Set the <code>Wins</code> value.
     *
     * @param newWins The new Wins value.
     */
    public final void setWins(final int newWins)
    {
        this.wins = newWins;
    }

    /**
     * Get the <code>Losses</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getLosses()
    {
        return losses;
    }

    /**
     * Set the <code>Losses</code> value.
     *
     * @param newLosses The new Losses value.
     */
    public final void setLosses(final int newLosses)
    {
        this.losses = newLosses;
    }

    /**
     * Get the <code>TotalPoints</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getTotalPoints()
    {
        return totalPoints;
    }

    /**
     * Set the <code>TotalPoints</code> value.
     *
     * @param newTotalPoints The new TotalPoints value.
     */
    public final void setTotalPoints(final int newTotalPoints)
    {
        this.totalPoints = newTotalPoints;
    }

    /**
     * Get the <code>AveragePoints</code> value.
     *
     * @return a <code>float</code> value
     */
    public final float getAveragePoints()
    {
        return averagePoints;
    }

    /**
     * Set the <code>AveragePoints</code> value.
     *
     * @param newAveragePoints The new AveragePoints value.
     */
    public final void setAveragePoints(final float newAveragePoints)
    {
        this.averagePoints = newAveragePoints;
    }

    /**
     * Get the <code>WinRatio</code> value.
     *
     * @return a <code>float</code> value
     */
    public final float getWinRatio()
    {
        return winRatio;
    }

    /**
     * Set the <code>WinRatio</code> value.
     *
     * @param newWinRatio The new WinRatio value.
     */
    public final void setWinRatio(final float newWinRatio)
    {
        this.winRatio = newWinRatio;
    }
    
    /**
     * @return a human readable form of the data
     */
    public String toString()
    {
        return
            "name="+name+
            ",rank=" + rank +
            ",wins=" + wins +
            ",losses=" + losses +
            ",totalPoints=" + totalPoints +
            ",averagePoints=" + averagePoints +
            ",winRatio=" + winRatio;
    }
}
