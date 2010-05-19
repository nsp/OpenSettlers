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
public class Vertex implements HexTypeConstants 
{

    public Vector2d centerpoint;
    public Point screenCoord;
    public int debugLRstatus;
    
    public Vertex(Vector2d p)
    {
        centerpoint = p;
        
//        screenCoord = new Point[2];
        // screenCoord values are set externally by BoardLayout!
    }
}
