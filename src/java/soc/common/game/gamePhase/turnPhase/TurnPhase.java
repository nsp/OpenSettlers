package soc.common.game.gamePhase.turnPhase;

import java.util.ArrayList;
import java.util.List;

import soc.common.actions.ingame.InGameAction;
import soc.common.game.Game;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class TurnPhase
{
    protected List<String> _AllowedActions = new ArrayList<String>();  
    
    protected void addActions()
    {
                
    }
    
    public TurnPhase next()
    {
        throw new NotImplementedException();
    }
    
    public TurnPhase processAction(InGameAction action, Game game)
    {
        throw new NotImplementedException();
    }
    
    public boolean isAllowed(InGameAction action, Game game)
    {
        throw new NotImplementedException();
    }
}
