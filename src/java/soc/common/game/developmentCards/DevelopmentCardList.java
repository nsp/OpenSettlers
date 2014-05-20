package soc.common.game.developmentCards;

import java.util.ArrayList;



public class DevelopmentCardList extends ArrayList<DevelopmentCard>
{
    public static DevelopmentCardList standard()
    {
        DevelopmentCardList result = new DevelopmentCardList();
        
        for (int i=0; i<14; i++)
            result.add(new Soldier());
        
        for (int i=0; i<5; i++)
            result.add(new VictoryPoint());
        
        for (int i=0; i<2; i++)
            result.add(new RoadBuilding());
        
        for (int i=0; i<2; i++)
            result.add(new Monopoly());
        
        for (int i=0; i<2; i++)
            result.add(new YearOfPlenty());
        
        return result;
    }    
    
    public static DevelopmentCardList extended()
    {
        DevelopmentCardList result = new DevelopmentCardList();
        
        for (int i=0; i<19; i++)
            result.add(new Soldier());
        
        for (int i=0; i<5; i++)
            result.add(new VictoryPoint());
        
        for (int i=0; i<3; i++)
            result.add(new RoadBuilding());
        
        for (int i=0; i<3; i++)
            result.add(new Monopoly());
        
        for (int i=0; i<3; i++)
            result.add(new YearOfPlenty());
        
        return result;
    }
}
