package soc.common.board.hexes;

import soc.common.board.Chit;
import soc.common.board.resources.Resource;

public class ResourceHex extends LandHex
{
    private Resource resource;
    private Chit chit;
    
    /**
     * @return the chit
     */
    public Chit getChit()
    {
        return chit;
    }

    /**
     * @param chit the chit to set
     */
    public void setChit(Chit chit)
    {
        this.chit = chit;
    }

    /**
     * @return the production
     */
    public Resource getResource()
    {
        return null;
    }
    
    /*
     *  At init time, we want a resource
     */
    public ResourceHex(Resource resource)
    {
        super();
        this.resource = resource;
    }
    
    public ResourceHex()
    {
    
    }

}
