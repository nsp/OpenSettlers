package soc.common.game.gamePhase;

import soc.common.actions.GameAction;
import soc.common.actions.ingame.InGameAction;
import soc.common.game.Game;

public interface IGamePhase
{
    public void PerformAction(GameAction action, Game game);
    public void isAllowed(GameAction action);
    public void start(Game game);
    public Class endAction();
}
