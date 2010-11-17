package soc.common.board.hexes;

import soc.common.annotations.Sea3D;
import soc.common.board.resources.Gold;
import soc.common.board.resources.Resource;

@Sea3D
public class VolcanoHex extends ResourceHex
{
    private Resource resource = new Gold();

    /* (non-Javadoc)
     * @see soc.common.board.hexes.ResourceHex#getResource()
     */
    @Override
    public Resource getResource()
    {
        // TODO Auto-generated method stub
        return resource;
    }

}
