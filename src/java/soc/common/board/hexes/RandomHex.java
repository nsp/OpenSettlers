package soc.common.board.hexes;

public class RandomHex extends Hex implements ITerritoryHex
{
    private int territoryID;
    
    @Override
    public int getTerritoryID()
    {
        // TODO Auto-generated method stub
        return territoryID;
    }

    @Override
    public void setTerritoryID(int id)
    {
        // TODO Auto-generated method stub
        territoryID = id;
    }
}
