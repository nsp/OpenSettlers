/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.boardlayout;

import java.awt.*;
import smartsettlers.util.*;

/**
 *
 * @author szityu
 */
public class HexTile implements HexTypeConstants
{
    
    public Vector2d pos;
    public int type, subtype, orientation;
    public Polygon screenCoord;
    public Point centerScreenCord;
    public int productionNumber = 0;
    
    public HexTile(int posx, int posy, int subtype, int orientation)
    {
        this(new Vector2d(posx,posy), subtype, orientation);
    }
    
    /**
     * 
     * @param v             integer coordinates
     * @param subtype       (sub)type of tile
     * @param orientation   orientation of tile
     */
    public HexTile(Vector2d v, int subtype, int orientation)
    {
        pos = new Vector2d(v);
        this.subtype = subtype;
        this.orientation = orientation;
        if (subtype==SEA)
        {
            type = TYPE_SEA;
        }
        else if ((subtype>=PORT_SHEEP) && (subtype<=PORT_MISC))
        {
            type = TYPE_PORT;
        }
        else
        {
            type = TYPE_LAND;
        }
        
    }
    
    public int yields()
    {
        if ((subtype>= LAND_SHEEP) && (subtype <= LAND_STONE))
                return subtype-LAND_SHEEP + RES_SHEEP;
        else
                return -1;
    }
}
