package soc.common.game.gamePhase;

import soc.common.actions.gameAction.GameAction;
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
            game.getActionsQueue().enqueue(null);
            /*
            new RollDiceAction()
            {
                GamePlayer = player
            });
            */
        }
    }
    
    @Override
    public void performAction(GameAction action, Game game)
    {
        // TODO Auto-generated method stub
        
    }

}
