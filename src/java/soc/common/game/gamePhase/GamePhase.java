package soc.common.game.gamePhase;

import soc.common.actions.GameAction;
import soc.common.game.Game;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public abstract class GamePhase
{
    public void PerformAction(GameAction action, Game game) {};
    public void isAllowed(GameAction action) {};
    public void start(Game game) {};
    public Class endAction() { throw new NotImplementedException(); };
}
