package soc.common.game.gamePhase;

import soc.common.actions.gameAction.GameAction;
import soc.common.actions.gameAction.GamePhaseHasEnded;
import soc.common.actions.gameAction.GameAction.*;
import soc.common.actions.gameAction.turnActions.BuildCity;
import soc.common.actions.gameAction.turnActions.BuildRoad;
import soc.common.actions.gameAction.turnActions.BuildShip;
import soc.common.actions.gameAction.turnActions.BuildTown;
import soc.common.game.Game;

public class InitialPlacementGamePhase extends GamePhase
{
    
    @Override
    public void start(Game game)
    {
        // Expect each player to place town/road - town/road
        int i = 0;
        boolean back = false;
    
        // A loop going backward. Each index should be hit twice.
        // Example with 4 players: p1 - p2 - p3 - p4 - p4 - p3 - p2 - p1
        while (i > -1)
        {
            // If tournament starting rules are set, second building should be a city
            if (back && game.getGameSettings().isTournamentStart())
            {
                // Tournamanet starting rules, add a city
                game.getActionsQueue().enqueue
                (
                    new BuildCity()
                    .setPlayer(game.getPlayers().get(i))
                );

            }
            else
            {
                // Normal starting rules, add two towns
                game.getActionsQueue().enqueue
                (
                    new BuildTown()
                        .setPlayer(game.getPlayers().get(i))
                );
            }
    
            // This action actually might be a BuildShipAction too.
            // TODO: implement this somewhere
            game.getActionsQueue().enqueue
            (
                new BuildRoad()
                    .setPlayer(game.getPlayers().get(i))
            );
    
            // if the "back" flag is set, we should decrease the counter
            if (back)
            {
                i--;
            }
            else
            {
                i++;
            }
            
            // flip the flag when counter reaches maximum value 
            // (maximum value equals amount of players)
            if (i == game.getPlayers().size())
            {
                // next loop is walked with same maximum value
                i--;
    
                // switch flag
                back = true;
            }
        }
    

        // When in tournament phase, very player may build a third road
        if (game.getGameSettings().isTournamentStart())
        {
            for (int j = 0; j < game.getPlayers().size(); j++)
            {
                game.getActionsQueue().enqueue
                (
                    new BuildRoad()
                        .setPlayer(game.getPlayers().get(i))
                );
            }
        }
    }
     
    @Override
    public void performAction(GameAction gameAction, Game game)
    {
        gameAction.perform(game);

        // If the last road or ship has been built, add new gamephase action on the queue
        if (gameAction.getClass() == new BuildRoad().getClass() ||
            gameAction.getClass() == new BuildShip().getClass())
        {
            if (game.getActionsQueue().size() == 0)
            {
                game.getActionsQueue().enqueue
                (
                    new GamePhaseHasEnded()
                        .setEndedGamePhase(this)
                        .setSender(0)
                );
            }
            else
            {
                // Next player is the player of the first action on the queue
                game.setPlayerOnTurn
                (
                    game.getActionsQueue().peek().getPlayer()
                );
            }
        }
       
    }

    @Override
    public GamePhase next(Game game)
    {
        return new PlayTurnsGamePhase();
    }

}
