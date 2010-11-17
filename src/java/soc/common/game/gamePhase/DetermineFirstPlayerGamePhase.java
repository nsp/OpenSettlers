package soc.common.game.gamePhase;

import soc.common.actions.GameAction;
import soc.common.actions.ingame.InGameAction;
import soc.common.game.Game;
import soc.common.game.IGame;
import soc.common.game.Player;

public class DetermineFirstPlayerGamePhase extends GamePhase
{    
    @Override
    public void start(Game game)
    {
        for (Player player : game.getPlayers())
        {
            game.getActionsQueue().Enqueue(null);
            /*
            new RollDiceAction()
            {
                GamePlayer = player
            });
            */
        }
    }
    
    @Override
    public Class endAction()
    {
        //return instanceof(InGameAction);
        return null;
    }

    @Override
    public void PerformAction(GameAction action, Game game)
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void isAllowed(GameAction action)
    {
        // TODO Auto-generated method stub
        
    }

}
