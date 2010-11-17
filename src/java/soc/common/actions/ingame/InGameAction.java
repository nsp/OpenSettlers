package soc.common.actions.ingame;

import soc.common.actions.GameAction;
import soc.common.game.IGame;
import soc.common.game.gamePhase.GamePhase;
import soc.common.game.gamePhase.turnPhase.TurnPhase;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/*
 * A GameAction performed in a game
 */
public class InGameAction extends GameAction
{
    public void Perform(IGame game)
    {
        
    }

    /* 
     * Returns true if player is allowed to play this action in given TurnPhase
     */
    public boolean isAllowed(TurnPhase turnPhase)
    {
        throw new NotImplementedException();
    }
    
    /* 
     * Returns true if player is allowed to play this action in given GamePhase
     */
    public boolean isAllowed(GamePhase gamePhase)
    {
        throw new NotImplementedException();
    }
}
