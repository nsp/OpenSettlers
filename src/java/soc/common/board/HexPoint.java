package soc.common.board;

import java.util.ArrayList;
import java.util.List;

public class HexPoint
{
    private HexLocation hex1;
    private HexLocation hex2;
    private HexLocation hex3;
    
    public HexLocation getHex1()
    {
        return hex1;
    }
    public HexLocation getHex2()
    {
        return hex2;
    }
    public HexLocation getHex3()
    {
        return hex3;
    }
    
    public HexPointType getPointType()
    {
        List<HexLocation> points = new ArrayList<HexLocation>();
        points.add(hex3);
        points.add(hex2);
        points.add(hex1);

        int h = 220;

        for (HexLocation point : points)
        {
            if (point.getH() < h) h = point.getH();
        } 

        int count=0;
        
        for (HexLocation p : points)
        {
            if (p.getH() == h)
            {
                count++;
            }
        }
        
        if (count==1)
        {
            // There is one Hex at the upper height coordinate
            return HexPointType.UPPERROW1; 
        }
        else
        {
            // There are two Hexes at the upper height coordinate
            return HexPointType.UPPERROW2;
        }
    }
    
    public PointPositionOnHex getHexPositionOnTopLeftMost()
    {
        return getPointType() == HexPointType.UPPERROW1 ?
            PointPositionOnHex.BOTTOMMIDDLE : PointPositionOnHex.BOTTOMRIGHT;
    }
    
    public List<HexSide> getOtherSides(HexSide side) throws Exception
    {
        List<HexSide> result = new ArrayList<HexSide>();

        for (HexSide s : getNeighbourSides())
            if (!side.equals(s))
                result.add(s);

        return result;
    }
    
    
    /*
     * Returns a list of three HexSides adjacent to this point
     */
    public List<HexSide> getNeighbourSides() throws Exception
    {
        List<HexSide> result = new ArrayList<HexSide>();

        // add all three hex sides around point
        result.add(new HexSide(hex1, hex2));
        result.add(new HexSide(hex1, hex3));
        result.add(new HexSide(hex2, hex3));

        return result;
    }
    
    /*
     * Returns the topmost hex of the three hexes
     */
    public HexLocation getTopMost()
    {
        List<HexLocation> points = new ArrayList<HexLocation>();
        points.add(hex3);
        points.add(hex2);
        points.add(hex1);
        int w = 220;
        int h = 220;
        for (HexLocation point : points)
        {
            if (point.getW() < w) w = point.getW();
            if (point.getH() < h) h = point.getH();
        }
        List<HexLocation> res = new ArrayList<HexLocation>();
        if (hex1.getH() == h) res.add(hex1);
        if (hex2.getH() == h) res.add(hex2);
        if (hex3.getH() == h) res.add(hex3);
        if (res.size() == 1)
        {
            return res.get(0);
        }
        else
        {
            if (res.size() == 2)
            {
                HexLocation l = res.get(0);
                if (l.getW() < res.get(1).getW()) return l;
                else return res.get(1);
            }
        }
        return null;
    }
    
    public List<HexPoint> getNeighbours() throws Exception
    {
        List<HexPoint> result = new ArrayList<HexPoint>();
        HexLocation topmost = getTopMost();
        
        if (topmost.getH() % 2 == 0)
        {
            //even rows
            if (getPointType() == HexPointType.UPPERROW1)
            {
                HexPoint p1 = new HexPoint(
                    topmost,
                    new HexLocation(topmost.getW() - 1, topmost.getH()),
                    new HexLocation(topmost.getW(), topmost.getH() + 1));
                result.add(p1);

                HexPoint p2 = new HexPoint(
                    new HexLocation(topmost.getW() + 1, topmost.getH() + 1),
                    new HexLocation(topmost.getW(), topmost.getH() + 1),
                    new HexLocation(topmost.getW(), topmost.getH() + 2));
                result.add(p2);

                HexPoint p3 = new HexPoint(
                    topmost,
                    new HexLocation(topmost.getW() + 1, topmost.getH() + 1),
                    new HexLocation(topmost.getW() + 1, topmost.getH()));
                result.add(p3);
            }
            else
            {
                HexPoint p1 = new HexPoint(
                    topmost,
                    new HexLocation(topmost.getW() + 1, topmost.getH()),
                    new HexLocation(topmost.getW() + 1, topmost.getH() - 1));
                result.add(p1);

                HexPoint p2 = new HexPoint(
                    new HexLocation(topmost.getW() + 2, topmost.getH() + 1),
                    new HexLocation(topmost.getW() + 1, topmost.getH() + 1),
                    new HexLocation(topmost.getW() + 1, topmost.getH()));
                result.add(p2);

                HexPoint p3 = new HexPoint(
                    topmost,
                    new HexLocation(topmost.getW() + 1, topmost.getH() + 1),
                    new HexLocation(topmost.getW(), topmost.getH() + 1));
                result.add(p3);
            }
        }
        else
        {
            //uneven rows
            if (getPointType() == HexPointType.UPPERROW1)
            {
                HexPoint p1 = new HexPoint(
                    topmost,
                    new HexLocation(topmost.getW() - 1, topmost.getH() + 1),
                    new HexLocation(topmost.getW() - 1, topmost.getH()));
                result.add(p1);

                HexPoint p2 = new HexPoint(
                        new HexLocation(topmost.getW(), topmost.getH() + 1),
                        new HexLocation(topmost.getW() - 1, topmost.getH() + 1),
                        new HexLocation(topmost.getW(), topmost.getH() + 2));
                result.add(p2);

                HexPoint p3 = new HexPoint(
                        topmost,
                        new HexLocation(topmost.getW(), topmost.getH() + 1),
                        new HexLocation(topmost.getW() + 1, topmost.getH()));
                result.add(p3);
            }
            else
            {
                // OK
                HexPoint p1 = new HexPoint(
                    topmost,
                    new HexLocation(topmost.getW(), topmost.getH() - 1),
                    new HexLocation(topmost.getW() + 1, topmost.getH()));
                result.add(p1);

                HexPoint p2 = new HexPoint(
                    new HexLocation(topmost.getW() + 1, topmost.getH()),
                    new HexLocation(topmost.getW() + 1, topmost.getH() + 1),
                    new HexLocation(topmost.getW(), topmost.getH() + 1));
                result.add(p2);

                HexPoint p3 = new HexPoint(
                    topmost,
                    new HexLocation(topmost.getW(), topmost.getH() + 1),
                    new HexLocation(topmost.getW() - 1, topmost.getH() + 1));
                result.add(p3);
            }
        }
        return result;
    }
    
    public List<HexPoint> getOtherNeighbours(HexPoint center, HexPoint ignore) throws Exception
    {
        List<HexPoint> result = getNeighbours();
        
        result.remove(ignore);

        return result;
    }
    
    public HexPoint(HexLocation hex1, HexLocation hex2, HexLocation hex3) throws Exception
    {
        this.hex1 = hex1;
        this.hex2 = hex2;
        this.hex3 = hex3;
        if (hex1.equals(hex2) ||
            hex1.equals(hex3) ||
            hex2.equals(hex3))
            throw new IllegalArgumentException("WHOA");
    }
    
    public boolean getHasLocation(HexLocation location)
    {
        return hex1.equals(location) ||
            hex2.equals(location) ||
            hex3.equals(location);
    }

    public HexPoint(HexLocation hex, PointPositionOnHex relativePosition) throws Exception
    {
        // we must assume hex comes from a uneven row, and 
        // relative position on the hex is never the two left positions
        if (hex.getH() % 2 == 0) throw new Exception("WHooa!");
        hex1 = hex;

        switch (relativePosition)
        {
            case TOPMIDDLE:
                hex2 = new HexLocation(hex.getW() - 1, hex.getH() - 1);
                hex3 = new HexLocation(hex.getW(), hex.getH() - 1);
                break;
            case TOPRIGHT:
                hex2 = new HexLocation(hex.getW(), hex.getH() - 1);
                hex3 = new HexLocation(hex.getW() + 1, hex.getH());
                break;
            case BOTTOMRIGHT:
                hex2 = new HexLocation(hex.getW() + 1, hex.getH());
                hex3 = new HexLocation(hex.getW(), hex.getH() + 1);
                break;
            case BOTTOMMIDDLE:
                hex2 = new HexLocation(hex.getW(), hex.getH() + 1);
                hex3 = new HexLocation(hex.getW() - 1, hex.getH() + 1);
                break;
            default: throw new Exception("Whoa!");
        }
    }

    public HexPoint() { }

    // create a point out of two neighbouring sides
    public HexPoint(HexSide side1, HexSide side2)
    {
        List<HexLocation> allLocations = new ArrayList<HexLocation>();
        allLocations.add(side1.getHex1());
        allLocations.add(side1.getHex2());
        allLocations.add(side2.getHex1());
        allLocations.add(side2.getHex2());

        HexLocation equalHex = null;
        if (side1.getHex1().equals(side2.getHex1())) equalHex = side1.getHex1();
        if (side1.getHex1().equals(side2.getHex2())) equalHex = side1.getHex1();

        allLocations.remove(equalHex);

        this.hex1 = allLocations.get(0);
        this.hex2 = allLocations.get(1);
        this.hex3 = allLocations.get(2);
    }
    
    public String toString()
    {
        return String.format("hex1: %s, hex2: %s, hex3: %s",
                hex1.toString(), hex2.toString(), hex3.toString());
    }
    
    public int hashCode()
    {
        return hex1.hashCode() ^ hex2.hashCode() ^ hex3.hashCode();
    }
    
}
