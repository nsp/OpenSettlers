package soc.common.board.hexes;

import soc.common.board.HexLocation;

/// Represents the base type for each hex.
/// @seealso cref="http://www.codeproject.com/KB/cs/hexagonal_part1.aspx"/>
/// @seealso cref="http://gmc.yoyogames.com/index.php?showtopic=336183"/>
public class Hex
{
    private HexLocation hexLocation;
    private static double s = 10;
    private static double h;
    private static double r;
    private static double b;
    private static double a;

    /// <summary>
    /// The width of the hex measured from outer left to the middle
    /// </summary>
    public static double getHalfWidth()
    {
        return r; 
    }

    /// <summary>
    /// Total width of the hex
    /// </summary>
    public static double getWidth()
    {  
        return a; 
    }

    /// <summary>
    /// Total height of the hex
    /// </summary>
    public static double getHeight()
    { 
        return b; 
    }
    /// <summary>
    /// Height measured from top to the first line
    ///      __         _
    ///     /  \        _ } PartialHeight
    ///    |    |
    ///     \  /
    ///      --
    /// </summary>
    public static double getPartialHeight()
    { 
        return s + h; 
    }

    /// <summary>
    /// Height measured from the top to the second line
    ///      __         _
    ///     /  \          } BottomHeight
    ///    |    |       _
    ///     \  /
    ///      --
    /// </summary>
    public static double getBottomHeight()
    { 
        return h; 
    }

    /// <summary>
    /// Size of the hex, measured one line
    ///     |  | --> size
    ///      __         
    ///     /  \    _    
    ///    |    |   _ } --> size    
    ///     \  /
    ///      --
    /// </summary>
    public static int getSize()
    {
        return (int)s; 
    }
    
    static 
    {
        h = Math.sin(DegreesToRadians(30)) * s;
        r = Math.cos(DegreesToRadians(30)) * s;
        b = s + 2 * h;
        a = 2 * r;
    }
    
    /// <summary>
    /// Helper function for size calculation
    /// </summary>
    /// @param degrees
    static private double DegreesToRadians(double degrees)
    {
        return degrees * Math.PI / 180;
    }

    public void setLocation(HexLocation hexLocation)
    {
        this.hexLocation = hexLocation;   
    }
    
    public HexLocation getLocation()
    {
        return hexLocation;
    }

    public Hex Copy()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return this.getClass().toString()+ " [hexLocation=" + hexLocation + "]";
    }
    
    
    
}
