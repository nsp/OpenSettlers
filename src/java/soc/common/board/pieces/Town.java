package soc.common.board.pieces;

import soc.common.board.resources.*;

public class Town extends PlayerPiece
{
    @Override
    public String toString()
    {
        return "Town";
    }

    @Override
    public ResourceList getCost()
    {
        ResourceList result = new ResourceList();
        
        result.add(new Timber());
        result.add(new Wheat());
        result.add(new Clay());
        result.add(new Sheep());
        
        return result;
    }
}
