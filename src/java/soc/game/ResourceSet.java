/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2008-2009 Jeremy D Monin <jeremy@nand.net>
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

import java.io.Serializable;


/**
 * This represents a collection of
 * clay, ore, sheep, wheat, and wood resources.
 * Unknown resources are also tracked here.
 * Although it's possible to store negative amounts of resources, it's discouraged.
 *
 * @see ResourceConstants
 * @see PlayingPiece#getResourcesToBuild(int)
 */
public class ResourceSet implements Serializable, Cloneable
{
    private static final long serialVersionUID = 5706718618157436864L;

    /** Resource set with zero of each resource type */
    public static final ResourceSet EMPTY_SET = new ResourceSet();

    /**
     * the number of resources
     */
    private int[] resources;

    /**
     * Make an empty resource set
     */
    public ResourceSet()
    {
        resources = new int[ResourceConstants.MAXPLUSONE];
        clear();
    }

    /**
     * Make a resource set with stuff in it
     *
     * @param cl  number of clay resources
     * @param or  number of ore resources
     * @param sh  number of sheep resources
     * @param wh  number of wheat resources
     * @param wo  number of wood resources
     * @param uk  number of unknown resources
     */
    public ResourceSet(int cl, int or, int sh, int wh, int wo, int uk)
    {
        resources = new int[ResourceConstants.MAXPLUSONE];

        resources[ResourceConstants.CLAY]    = cl;
        resources[ResourceConstants.ORE]     = or;
        resources[ResourceConstants.SHEEP]   = sh;
        resources[ResourceConstants.WHEAT]   = wh;
        resources[ResourceConstants.WOOD]    = wo;
        resources[ResourceConstants.UNKNOWN] = uk;
    }
    
    public int pickResource()
    {
        if (resources[ResourceConstants.CLAY]>0)
            return ResourceConstants.CLAY;
        if (resources[ResourceConstants.ORE]>0)
            return ResourceConstants.ORE;
        if (resources[ResourceConstants.SHEEP]>0)
            return ResourceConstants.SHEEP;
        if (resources[ResourceConstants.WHEAT]>0)
            return ResourceConstants.WHEAT;
        if (resources[ResourceConstants.WOOD]>0)
            return ResourceConstants.WOOD;
        return -1;
    }

    /**
     * Make a resource set from an array
     *
     * @param rset resource set, of length 5 or 6 (clay, ore, sheep, wheat, wood, unknown).
     *     If length is 5, unknown == 0.
     * @since 1.1.08
     */
    public ResourceSet(int[] rset)
    {
        this(rset[0], rset[1], rset[2], rset[3], rset[4], (rset.length >= 6) ? rset[5] : 0);
    }

    /**
     * set the number of resources to zero
     */
    public void clear()
    {
        for (int i = ResourceConstants.MIN;
                i < ResourceConstants.MAXPLUSONE; i++)
        {
            resources[i] = 0;
        }
    }

    /**
     * @return the number of a kind of resource
     *
     * @param rtype  the type of resource, like {@link ResourceConstants#CLAY}
     */
    public int getAmount(int rtype)
    {
        return resources[rtype];
    }

    /**
     * @return the total number of resources
     */
    public int getTotal()
    {
        int sum = 0;

        for (int i = ResourceConstants.MIN;
                i < ResourceConstants.MAXPLUSONE; i++)
        {
            sum += resources[i];
        }

        return sum;
    }

    /**
     * set the amount of a resource
     *
     * @param rtype the type of resource, like {@link ResourceConstants#CLAY}
     * @param amt   the amount
     */
    public void setAmount(int amt, int rtype)
    {
        resources[rtype] = amt;
    }

    /**
     * add an amount to a resource
     *
     * @param rtype the type of resource, like {@link ResourceConstants#CLAY}
     * @param amt   the amount; if below 0 (thus subtracting resources),
     *              the subtraction occurs and no special action is taken.
     *              {@link #subtract(int, int)} takes special action in some cases.
     */
    public void add(int amt, int rtype)
    {
        resources[rtype] += amt;
    }

    /**
     * subtract an amount from a resource.
     * If we're subtracting more from a resource than there are of that resource,
     * set that resource to zero, and then take the difference away from the
     * {@link ResourceConstants#UNKNOWN} resources.
     * As a result, UNKNOWN may be less than zero afterwards.
     *
     * @param rtype the type of resource, like {@link ResourceConstants#CLAY}
     * @param amt   the amount; unlike in {@link #add(int, int)}, any amount that
     *              takes the resource below 0 is treated specially.
     */
    public void subtract(int amt, int rtype)
    {
        /**
         * if we're subtracting more from a resource than
         * there are of that resource, set that resource
         * to zero, and then take the difference away
         * from the UNKNOWN resources
         */
        if (amt > resources[rtype])
        {
            resources[ResourceConstants.UNKNOWN] -= (amt - resources[rtype]);
            resources[rtype] = 0;
        }
        else
        {
            resources[rtype] -= amt;
        }

        if (resources[ResourceConstants.UNKNOWN] < 0)
        {
            System.err.println("RESOURCE < 0 : RESOURCE TYPE=" + rtype);
        }
    }

    /**
     * add an entire resource set's amounts into this set.
     *
     * @param rs  the resource set
     */
    public void add(ResourceSet rs)
    {
        resources[ResourceConstants.CLAY]    += rs.getAmount(ResourceConstants.CLAY);
        resources[ResourceConstants.ORE]     += rs.getAmount(ResourceConstants.ORE);
        resources[ResourceConstants.SHEEP]   += rs.getAmount(ResourceConstants.SHEEP);
        resources[ResourceConstants.WHEAT]   += rs.getAmount(ResourceConstants.WHEAT);
        resources[ResourceConstants.WOOD]    += rs.getAmount(ResourceConstants.WOOD);
        resources[ResourceConstants.UNKNOWN] += rs.getAmount(ResourceConstants.UNKNOWN);
    }

    /**
     * subtract an entire resource set. If any type's amount would go below 0, set it to 0.
     *
     * @param rs  the resource set
     */
    public void subtract(ResourceSet rs)
    {
        resources[ResourceConstants.CLAY] -= rs.getAmount(ResourceConstants.CLAY);

        if (resources[ResourceConstants.CLAY] < 0)
        {
            resources[ResourceConstants.CLAY] = 0;
        }

        resources[ResourceConstants.ORE] -= rs.getAmount(ResourceConstants.ORE);

        if (resources[ResourceConstants.ORE] < 0)
        {
            resources[ResourceConstants.ORE] = 0;
        }

        resources[ResourceConstants.SHEEP] -= rs.getAmount(ResourceConstants.SHEEP);

        if (resources[ResourceConstants.SHEEP] < 0)
        {
            resources[ResourceConstants.SHEEP] = 0;
        }

        resources[ResourceConstants.WHEAT] -= rs.getAmount(ResourceConstants.WHEAT);

        if (resources[ResourceConstants.WHEAT] < 0)
        {
            resources[ResourceConstants.WHEAT] = 0;
        }

        resources[ResourceConstants.WOOD] -= rs.getAmount(ResourceConstants.WOOD);

        if (resources[ResourceConstants.WOOD] < 0)
        {
            resources[ResourceConstants.WOOD] = 0;
        }

        resources[ResourceConstants.UNKNOWN] -= rs.getAmount(ResourceConstants.UNKNOWN);

        if (resources[ResourceConstants.UNKNOWN] < 0)
        {
            resources[ResourceConstants.UNKNOWN] = 0;
        }
    }

    /**
     * Convert all these resources to type {@link ResourceConstants#UNKNOWN}.
     * Information on amount of wood, wheat, etc is no longer available.
     * Equivalent to:
     * <code>
     *    int numTotal = resSet.getTotal();
     *    resSet.clear();
     *    resSet.setAmount (ResourceConstants.UNKNOWN, numTotal);
     * </code>
     */
    public void convertToUnknown()
    {
        int numTotal = getTotal();
        clear();
        resources[ResourceConstants.UNKNOWN] = numTotal;
    }

    /**
     * @return true if each resource type in set A is >= each resource type in set B
     *
     * @param a   set A
     * @param b   set B
     */
    static public boolean gte(ResourceSet a, ResourceSet b)
    {
        return (   (a.getAmount(ResourceConstants.CLAY)    >= b.getAmount(ResourceConstants.CLAY))
                && (a.getAmount(ResourceConstants.ORE)     >= b.getAmount(ResourceConstants.ORE))
                && (a.getAmount(ResourceConstants.SHEEP)   >= b.getAmount(ResourceConstants.SHEEP))
                && (a.getAmount(ResourceConstants.WHEAT)   >= b.getAmount(ResourceConstants.WHEAT))
                && (a.getAmount(ResourceConstants.WOOD)    >= b.getAmount(ResourceConstants.WOOD))
                && (a.getAmount(ResourceConstants.UNKNOWN) >= b.getAmount(ResourceConstants.UNKNOWN)));
    }

    /**
     * @return true if each resource type in set A is <= each resource type in set B
     *
     * @param a   set A
     * @param b   set B
     */
    static public boolean lte(ResourceSet a, ResourceSet b)
    {
        return (   (a.getAmount(ResourceConstants.CLAY)    <= b.getAmount(ResourceConstants.CLAY))
                && (a.getAmount(ResourceConstants.ORE)     <= b.getAmount(ResourceConstants.ORE))
                && (a.getAmount(ResourceConstants.SHEEP)   <= b.getAmount(ResourceConstants.SHEEP))
                && (a.getAmount(ResourceConstants.WHEAT)   <= b.getAmount(ResourceConstants.WHEAT))
                && (a.getAmount(ResourceConstants.WOOD)    <= b.getAmount(ResourceConstants.WOOD))
                && (a.getAmount(ResourceConstants.UNKNOWN) <= b.getAmount(ResourceConstants.UNKNOWN)));
    }

    /**
     * Human-readable form of the set, with format "clay=5|ore=1|sheep=0|wheat=0|wood=3|unknown=0"
     * @return a human readable longer form of the set
     * @see #toShortString()
     * @see #toFriendlyString()
     */
    public String toString()
    {
        String s = "clay=" + resources[ResourceConstants.CLAY]
            + "|ore=" + resources[ResourceConstants.ORE]
            + "|sheep=" + resources[ResourceConstants.SHEEP]
            + "|wheat=" + resources[ResourceConstants.WHEAT]
            + "|wood=" + resources[ResourceConstants.WOOD]
            + "|unknown=" + resources[ResourceConstants.UNKNOWN];

        return s;
    }

    /**
     * Human-readable form of the set, with format "Resources: 5 1 0 0 3 0".
     * Order of types is Clay, ore, sheep, wheat, wood, unknown.
     * @return a human readable short form of the set
     * @see #toFriendlyString()
     */
    public String toShortString()
    {
        String s = "Resources: " + resources[ResourceConstants.CLAY] + " "
            + resources[ResourceConstants.ORE] + " "
            + resources[ResourceConstants.SHEEP] + " "
            + resources[ResourceConstants.WHEAT] + " "
            + resources[ResourceConstants.WOOD] + " "
            + resources[ResourceConstants.UNKNOWN];

        return s;
    }

    /**
     * Human-readable form of the set, with format "5 clay,1 ore,3 wood"
     * @return a human readable longer form of the set;
     *         if the set is empty, return the string "nothing".
     * @see #toShortString()
     */
    public String toFriendlyString()
    {
        StringBuffer sb = new StringBuffer();
        if (toFriendlyString(sb))
            return sb.toString();
        else
            return "nothing";
    }

    /**
     * Human-readable form of the set, with format "5 clay, 1 ore, 3 wood".
     * Unknown resources aren't mentioned.
     * @param sb Append into this buffer.
     * @return true if anything was appended, false if sb unchanged (this resource set is empty).
     * @see #toFriendlyString()
     */
    public boolean toFriendlyString(StringBuffer sb)
    {
        boolean needComma = false;  // Has a resource already been appended to sb?
        int amt;

        for (int res = ResourceConstants.CLAY; res <= ResourceConstants.WOOD; ++res)
        {
            amt = resources[res];
            if (amt == 0)
                continue;

            if (needComma)
                sb.append(", ");
            sb.append(amt);                
            sb.append(" ");
            sb.append(ResourceConstants.resName(res));
            needComma = true;
        }

        return needComma;  // Did we append anything?
    }

    /**
     * @return true if sub is in this set
     *
     * @param sub  the sub set
     */
    public boolean contains(ResourceSet sub)
    {
        return gte(this, sub);
    }

    /**
     * @return true if the argument is a ResourceSet containing the same amounts of each resource, including UNKNOWN
     *
     * @param anObject  the object in question
     */
    public boolean equals(Object anObject)
    {
        if ((anObject instanceof ResourceSet)
                && (((ResourceSet) anObject).getAmount(ResourceConstants.CLAY)    == resources[ResourceConstants.CLAY])
                && (((ResourceSet) anObject).getAmount(ResourceConstants.ORE)     == resources[ResourceConstants.ORE])
                && (((ResourceSet) anObject).getAmount(ResourceConstants.SHEEP)   == resources[ResourceConstants.SHEEP])
                && (((ResourceSet) anObject).getAmount(ResourceConstants.WHEAT)   == resources[ResourceConstants.WHEAT])
                && (((ResourceSet) anObject).getAmount(ResourceConstants.WOOD)    == resources[ResourceConstants.WOOD])
                && (((ResourceSet) anObject).getAmount(ResourceConstants.UNKNOWN) == resources[ResourceConstants.UNKNOWN]))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * @return a hashcode for this data
     */
    public int hashCode()
    {
        return this.toString().hashCode();
    }

    /**
     * @return a copy of this resource set
     */
    public ResourceSet copy()
    {
        ResourceSet copy = new ResourceSet();
        copy.add(this);

        return copy;
    }

    /**
     * copy a resource set into this one. This one's current data is lost and overwritten.
     *
     * @param set  the set to copy from
     */
    public void setAmounts(ResourceSet set)
    {
        resources[ResourceConstants.CLAY]    = set.getAmount(ResourceConstants.CLAY);
        resources[ResourceConstants.ORE]     = set.getAmount(ResourceConstants.ORE);
        resources[ResourceConstants.SHEEP]   = set.getAmount(ResourceConstants.SHEEP);
        resources[ResourceConstants.WHEAT]   = set.getAmount(ResourceConstants.WHEAT);
        resources[ResourceConstants.WOOD]    = set.getAmount(ResourceConstants.WOOD);
        resources[ResourceConstants.UNKNOWN] = set.getAmount(ResourceConstants.UNKNOWN);
    }

}
