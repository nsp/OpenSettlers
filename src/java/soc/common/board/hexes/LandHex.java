package soc.common.board.hexes;

public class LandHex extends Hex implements ITerritoryHex
{
    //ID of territory hex belongs to. Default on mainland (ID=0).
    private int territoryID = 0;

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
