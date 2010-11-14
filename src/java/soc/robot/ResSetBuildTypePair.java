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
package soc.robot;

import soc.game.ResourceSet;


/**
 * this class holds a ResourceSet and a building type
 */
public class ResSetBuildTypePair
{
    /**
     * the resource set
     */
    ResourceSet resources;

    /**
     * the building type
     */
    int building;

    /**
     * the constructor
     *
     * @param rs  the resource set
     * @param bt  the building type
     */
    public ResSetBuildTypePair(ResourceSet rs, int bt)
    {
        resources = rs;
        building = bt;
    }

    /**
     * @return the resource set
     */
    public ResourceSet getResources()
    {
        return resources;
    }

    /**
     * @return the building type
     */
    public int getBuildingType()
    {
        return building;
    }

    /**
     * @return a hashcode for this pair
     */
    public int hashCode()
    {
        String tmp = resources.toString() + building;

        return tmp.hashCode();
    }

    /**
     * @return true if the argument contains the same data
     *
     * @param anObject  the object in question
     */
    public boolean equals(Object anObject)
    {
        if ((anObject instanceof ResSetBuildTypePair) && (((ResSetBuildTypePair) anObject).getBuildingType() == building) && (((ResSetBuildTypePair) anObject).getResources().equals(resources)))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * @return a human readable form of this object
     */
    public String toString()
    {
        String str = "ResType:res=" + resources + "|type=" + building;

        return str;
    }
}
