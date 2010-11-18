package soc.common.game.gamePhase;

import soc.common.actions.gameAction.GameAction;
import soc.common.game.Game;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/*
 * Represent a phase in the overall game phases
 * A GamePhase ends itself by adding an EndedGamePhase action onto the actionsQueue.
 */
public abstract class GamePhase
{
    public void performAction(GameAction action, Game game) {};
    public void start(Game game) {};
    public GamePhase next(Game game) { throw new NotImplementedException(); }
}
