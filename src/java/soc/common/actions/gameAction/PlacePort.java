package soc.common.actions.gameAction;

public class PlacePort extends GameAction
{
    private int territoryID;

    /**
     * @return the territoryID
     */
    public int getTerritoryID()
    {
        return territoryID;
    }

    /**
     * @param territoryID the territoryID to set
     */
    public PlacePort setTerritoryID(int territoryID)
    {
        this.territoryID = territoryID;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }
}
