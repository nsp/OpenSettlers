package soc.common.board.ports;

import java.util.ArrayList;
import java.util.List;

import soc.common.board.resources.*;
import soc.common.board.resources.ResourceList;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ThreeToOnePort extends Port
{
    private List<Resource> tradeTypes = new ArrayList<Resource>();
    
    public ThreeToOnePort()
    {
        super();
        tradeTypes.add(new Timber());
        tradeTypes.add(new Wheat());
        tradeTypes.add(new Ore());
        tradeTypes.add(new Clay());
        tradeTypes.add(new Sheep());
    }

    @Override
    public int possibleTradesCount(ResourceList resources)
    {
        int possibleTrades=0;
        for (Resource resource : tradeTypes)
        {
            List<Resource> resourcesOfType = resources.ofType(resource);
            possibleTrades += resourcesOfType.size() / 3;
        }
        return possibleTrades;
    }
}
