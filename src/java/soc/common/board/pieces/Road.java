package soc.common.board.pieces;

import soc.common.board.resources.*;

public class Road extends PlayerPiece
{
    @Override
    public String toString()
    {
        return "Road";
    }

    @Override
    public ResourceList getCost()
    {
        ResourceList result = new ResourceList();
        
        result.add(new Timber());
        result.add(new Clay());
        
        return result;
    }
}
