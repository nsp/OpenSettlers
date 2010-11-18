package soc.common.board;

import soc.common.annotations.SeaFarers;
import soc.common.board.ports.PortList;

/*
 * Represents a group of LandHexes. A territory is useful for:
 * - Trade routes
 * - Chit swapping
 * - Bonus island VPs
 * 
 */
@SeaFarers
public class Territory
{
    private String name;
    private int ID;
    private boolean isMainland;
    private boolean isIsland;
    private PortList ports;
    
    public String getName()
    {
        return name;
    }
    /**
     * @return the ports
     */
    public PortList getPorts()
    {
        return ports;
    }
    /**
     * @param ports the ports to set
     */
    public Territory setPorts(PortList ports)
    {
        this.ports = ports;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public int getID()
    {
        return ID;
    }
    public void setID(int iD)
    {
        ID = iD;
    }
    public boolean isMainland()
    {
        return isMainland;
    }
    public void setMainland(boolean isMainland)
    {
        this.isMainland = isMainland;
    }
    public boolean isIsland()
    {
        return isIsland;
    }
    public void setIsland(boolean isIsland)
    {
        this.isIsland = isIsland;
    }
}
