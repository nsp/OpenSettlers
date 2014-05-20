package soc.common.board.ports;

import soc.common.board.HexLocation;
import soc.common.board.HexSide;
import soc.common.board.RotationPosition;
import soc.common.board.resources.ResourceList;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class Port
{
    protected HexLocation hexLocation;
    protected HexSide hexSide;
    protected RotationPosition rotationPosition;
    
    /**
     * @return the hexLocation
     */
    public HexLocation getHexLocation()
    {
        return hexLocation;
    }


    /**
     * @return the hexSide
     */
    public HexSide getHexSide()
    {
        return hexSide;
    }


    /**
     * @param hexSide the hexSide to set
     */
    public void setHexSide(HexSide hexSide)
    {
        this.hexSide = hexSide;
    }


    /**
     * @return the rotationPosition
     */
    public RotationPosition getRotationPosition()
    {
        return rotationPosition;
    }

    /**
     * @param hexLocation the hexLocation to set
     * @throws Exception 
     */
    public void setHexLocation(HexLocation hexLocation) throws Exception
    {
        hexSide = hexLocation.GetSideLocation(rotationPosition);
    }


    public Port()
    {
        super();
    }
    
    public Port(HexLocation hexLocation)
    {
        super();
        this.hexLocation = hexLocation;
    }


    public int possibleTradesCount(ResourceList resources)
    {
        throw new NotImplementedException();
    }
}
