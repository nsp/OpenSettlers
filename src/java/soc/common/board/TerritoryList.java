package soc.common.board;

import java.awt.List;
import java.util.ArrayList;

import soc.common.annotations.SeaFarers;

@SeaFarers
public class TerritoryList extends ArrayList<Territory>
{

    public Territory findByID(int id)
    {
        for (Territory t : this)
        {
            if (t.getID() == id)
                return t;
        }
        
        throw new RuntimeException();
    }
}
