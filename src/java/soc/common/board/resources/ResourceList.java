package soc.common.board.resources;

import java.util.ArrayList;

import soc.common.board.hexes.Hex;
import soc.common.board.pieces.*;

public class ResourceList extends ArrayList<Resource>
{
    public static ResourceList createList(Piece piece)
    {
        ResourceList result = new ResourceList();
        
        if (piece instanceof Town)
        {
            result.add(new Timber());
            result.add(new Wheat());
            result.add(new Clay());
            result.add(new Sheep());
            
            return result;
        }        
        if (piece instanceof City)
        {
            result.add(new Wheat());
            result.add(new Wheat());
            result.add(new Ore());
            result.add(new Ore());
            result.add(new Ore());
            
            return result;
        }
        if (piece instanceof Road)
        {
            result.add(new Timber());
            result.add(new Clay());
        }
        
        return null;
    }
    
    private int countOfType(Resource type)
    {
        int result = 0;
        for (Resource res : this)
        {
            if (res.getClass() == type.getClass())
            {
                result++;                                                
            }
        }
        return result;
    }
    
    /*
     * Returns true if given resources are available in this ResourceList
     */
    public boolean hasAtLeast(ResourceList toHave)
    {
        return
            countOfType(new Timber()) >= toHave.countOfType(new Timber()) &&
            countOfType(new Wheat()) >= toHave.countOfType(new Wheat()) &&
            countOfType(new Ore()) >= toHave.countOfType(new Ore()) &&
            countOfType(new Clay()) >= toHave.countOfType(new Clay()) &&
            countOfType(new Sheep()) >= toHave.countOfType(new Sheep());
    }
    
    /*
     * Returns amount of items halfed and rounded down
     */
    public int halfCount()
    {
        int count = size();
        // Make number even
        if (count % 2 == 1) count--;
        return count / 2;
    }
}
