package soc.common.board;

import java.util.ArrayList;
import java.util.List;

/*
 * Represents a side, determined by two HexLocations. 
 */
public class HexSide
{
    // A HexSide may be constructed using a set of two HexLocations or
    // a set of two HexPoints
    
    // The two HexLocations the HexSide is represented by
    private HexLocation hex1;
    private HexLocation hex2;

    // The two HexPoints a hexside is represented by
    private HexPoint hexPoint1;
    private HexPoint hexPoint2;
    
    // Direction the side points to
    private SideDirection sideDirection;
    
    public HexLocation getHex1()
    {
        return hex1;
    }
    public HexLocation getHex2()
    {
        return hex2;
    }
    
    public HexPoint getHexPoint1()
    {
        return hexPoint1;
    }
    
    public HexPoint getHexPoint2()
    {
        return hexPoint2;
    }
    
    HexSide(HexLocation hex1, HexLocation hex2) throws Exception
    {
        this.hex1=hex1;
        this.hex2=hex2;
        calculatePoints();
    }
    
    HexSide(HexPoint point1, HexPoint point2)
    {
        this.hexPoint1=point1;
        this.hexPoint2=point2;
        calculateHexes();
    }
    
    public HexLocation getHighestOrLeftestHex()
    {
        if (hex1.getH() == hex2.getH())
            //both on same row, return leftest
            return hex1.getW() < hex2.getW() ? hex1 : hex2;
        else
            //different rows, return highest
            return hex1.getH() > hex2.getH() ? hex2 : hex1;    
    }
    
    /*
     * Returns a list of (maximum) three points neighbouring
     * this 
     */
    public List<HexPoint> getNeighbourPoints() throws Exception
    {
        List<HexPoint> result = new ArrayList<HexPoint>();

        HexLocation top = getHighestOrLeftestHex();
        
        // TODO: headache code
        // either visualize using pics or rewrite
        switch (getDirection())
        {
            case UPDOWN:
                int offset = top.getH() % 2 == 0 ? 1 : 0;
                result.add(new HexPoint(hex1, hex2,
                    new HexLocation(top.getW() + offset, top.getH() - 1)));
                result.add(new HexPoint(hex1, hex2,
                    new HexLocation(top.getW() + offset, top.getH() + 1)));
                break;
            case SLOPEDOWN:
                int offset2 = top.getH() % 2 == 0 ? 1 : 0;
                result.add(new HexPoint(hex1, hex2,
                    new HexLocation(top.getW() - 1, top.getH())));
                // generates bad hex
                result.add(new HexPoint(hex1, hex2,
                    new HexLocation(top.getW() + offset2, top.getH() + 1)));
                    break;
            case SLOPEUP:
                int offset3 = top.getH() % 2 == 0 ? 0 : 1;
                result.add(new HexPoint(hex1, hex2,
                    new HexLocation(top.getW() - offset3, top.getH() + 1)));
                result.add(new HexPoint(hex1, hex2,
                    new HexLocation(top.getW() + 1, top.getH())));
                break;
        }
        return result;
    }
    
    /*
     *  Creates the two HexLocations this HexSide is primarily represented by 
     */
    private void calculateHexes()
    {
        List<HexLocation> locations = new ArrayList<HexLocation>();
        
        locations.add(hexPoint1.getHex1());
        locations.add(hexPoint1.getHex2());
        locations.add(hexPoint1.getHex3());
        locations.add(hexPoint2.getHex1());
        locations.add(hexPoint2.getHex2());
        locations.add(hexPoint2.getHex3());

        /*
         * TODO: port to java
        
        
        var x = from l in locations
                group l by l into lunique
                where lunique.Count() == 2
                select lunique.Key;
         */

        // first of resultset
        hex1 = locations.get(0);
        
        // last of resultset 
        hex2 = locations.get(5);
    }
    
    /*
     * Returns the direction this side is pointing to
     */
    public SideDirection getDirection()
    {
        // lazy init of direction variable
        if (sideDirection == null)
        {
            //   |
            // both hexes are on the same row, so the side is updown
            if (hex1.getH() == hex2.getH()) return SideDirection.UPDOWN;
        
            if (getHighestOrLeftestHex().getH() % 2 == 0)
            //even rows
            {
                if (hex1.getW() == hex2.getW())
                    sideDirection = SideDirection.SLOPEDOWN;
                else
                    sideDirection = SideDirection.SLOPEUP;
            }
            else
            //uneven rows
            {
                if (hex1.getW() == hex2.getW())
                    sideDirection = SideDirection.SLOPEUP;
                else
                    sideDirection = SideDirection.SLOPEDOWN;
            }
        }
        return sideDirection;
    }
    
    /*
     *  Creates two HexPoints, each consisting of three HexLocations
     *  TODO: copy+paste image reference from paper
     */
    private void calculatePoints() throws Exception
    {
        HexLocation loc1 = null;
        HexLocation loc2 = null;
        
        HexLocation lefttop = getHighestOrLeftestHex();
        int offset = lefttop.getH() % 2 == 0 ? 1 : 0;
        switch (getDirection())
        {
            case UPDOWN:
                loc1 = new HexLocation(offset + lefttop.getW(), lefttop.getH() - 1);
                loc2 = new HexLocation(offset + lefttop.getW(), lefttop.getH() + 1);
                break;
            case SLOPEDOWN:
                loc1 = new HexLocation(offset + lefttop.getW(), lefttop.getH() + 1);
                loc2 = new HexLocation(lefttop.getW() - 1, lefttop.getH());
                break;
            case SLOPEUP:
                loc1 = new HexLocation(lefttop.getW() + 1, lefttop.getH());
                loc2 = new HexLocation(lefttop.getW() -1 + offset, lefttop.getH() + 1);
                break;
        }
        hexPoint1 = new HexPoint(hex1, hex2, loc1);
        hexPoint2 = new HexPoint(hex1, hex2, loc2);
    }
    
    public int getHashCode()
    {
        return hex1.hashCode() ^ hex2.hashCode();    
    }
    
    private boolean isEqual(HexSide other)
    {
        return (hex1.equals(other.getHex1()) && hex2.equals(other.getHex2())) ||
               (hex1.equals(other.getHex2()) && hex2.equals(other.getHex1()));
    }
    
    public HexPoint getOtherPoint(HexPoint first)
    {
        if (first.equals(hexPoint1)) 
            return hexPoint2;
        else
            return hexPoint1;
    }
    
    /*
     * Returns true when given location is contained by this HexSide
     */
    public boolean HasLocation(HexLocation check)
    {
        return hex1.equals(check) || hex2.equals(check);
    }
    
    public boolean equals(Object other)
    {
        if (other instanceof HexSide)
        {
            return isEqual((HexSide)other);
        }
        else
        {
            return false;
        }
    }
}
