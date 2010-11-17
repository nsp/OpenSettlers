package soc.common.board.resources;

import java.util.ArrayList;
import java.util.List;

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
    
    public List<Resource> ofType(Resource type)
    {
        List<Resource> result = new ArrayList<Resource>();
        
        for (Resource res : this)
        {
            if (res.getClass() == type.getClass())
            {
                result.add(res);                                                
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
            ofType(new Timber()).size() >= toHave.ofType(new Timber()).size() &&
            ofType(new Wheat()).size() >= toHave.ofType(new Wheat()).size()   &&
            ofType(new Ore()).size() >= toHave.ofType(new Ore()).size()       &&
            ofType(new Clay()).size() >= toHave.ofType(new Clay()).size()     &&
            ofType(new Sheep()).size() >= toHave.ofType(new Sheep()).size();
    }
    
    public void swapResourcesFrom(ResourceList resourcesToAdd, ResourceList from)
    {
        // add the resources to this list...
        this.addAll(resourcesToAdd);
        
        // ...and remove them at the "from source"
        from.removeAll(resourcesToAdd);
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
