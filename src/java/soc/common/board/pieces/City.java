package soc.common.board.pieces;

import soc.common.board.resources.*;

public class City extends PlayerPiece
{
    @Override
    public ResourceList getCost()
    {
        ResourceList result = new ResourceList();
        
        result.add(new Wheat());
        result.add(new Wheat());
        result.add(new Ore());
        result.add(new Ore());
        result.add(new Ore());
        
        return result;
    }

    @Override
    public String toString()
    {
        return "City";
    }
}
