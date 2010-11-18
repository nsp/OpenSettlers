package soc.common.actions.gameAction;

import soc.common.game.gamePhase.GamePhase;


public class GamePhaseHasEnded extends GameAction
{
    private GamePhase endedGamePhase;

    /**
     * @return the endedGamePhase
     */
    public GamePhase getEndedGamePhase()
    {
        return endedGamePhase;
    }

    /**
     * @param endedGamePhase the endedGamePhase to set
     */
    public GamePhaseHasEnded setEndedGamePhase(GamePhase endedGamePhase)
    {
        this.endedGamePhase = endedGamePhase;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }
}
