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
public class Edge implements HexTypeConstants 
{

    public Vector2d[] endpoints;
    public Point[] screenCoord;
    public boolean isPartOfLongestRoad;
    
    public Edge(Vector2d p1, Vector2d p2)
    {
        endpoints  = new Vector2d[2];
        endpoints[0] = p1;
        endpoints[1] = p2;
        
        screenCoord = new Point[2];
        // screenCoord values are set externally by BoardLayout!
    }
}
