package soc.common.board.hexes;

import soc.common.board.ports.Port;

public class SeaHex extends Hex
{
    private Port port = null;

    /**
     * @return the port
     */
    public Port getPort()
    {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(Port port)
    {
        this.port = port;
    }
    
    
    
}
