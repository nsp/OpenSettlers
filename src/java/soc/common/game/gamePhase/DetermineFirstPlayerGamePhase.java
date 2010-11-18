package soc.common.game.gamePhase;

import java.util.ArrayList;
import java.util.List;

import soc.common.actions.gameAction.GameAction;
import soc.common.actions.gameAction.RolledSame;
import soc.common.actions.gameAction.StartingPlayerDetermined;
import soc.common.actions.gameAction.turnActions.RollDice;
import soc.common.board.Territory;
import soc.common.board.ports.Port;
import soc.common.game.Game;
import soc.common.game.Player;

public class DetermineFirstPlayerGamePhase extends GamePhase
{    
    @Override
    public void start(Game game)
    {
        // expect each player to roll at least once (first phase: everyone rolls once)
        for (Player p : game.getPlayers())
        {
            game.getActionsQueue().enqueue
            (
                new RollDice()
                    .setPlayer(p)
            );
        }
    }
    
    private int getHighRoll(List<RollDice> rolledDices)
    {
        int result = 2;
        
        for (RollDice rollDice : rolledDices)
        {
            if (rollDice.getDice() > result)
                result = rollDice.getDice();
        }
        
        return result;
    }
    
    @Override
    public void performAction(GameAction action, Game game)
    {
        action.perform(game);
        
        if (action instanceof RollDice)
        {
            RollDice rollDice = (RollDice)action;
            // Check if a phase has ended. If the queue is empty, every player has rolled the dice.
            if (game.getActionsQueue().size() == 0)
            {
                // Make a list of rolls in this round
                List<RollDice> rolledDices = game.getGameLog().getCurrentRoundRolls(game);

                // highroll dice number
                int highRoll = getHighRoll(rolledDices);

                // When starting player is not determined yet, repeat dice roll between winners until 
                // winner is determined
                Player gameStarter = game.getGameLog().firstPlayerIsDetermined(game, highRoll);
                if (gameStarter !=null)
                {
                    // We have a starting player
                    game.getActionsQueue().enqueue
                    (
                        new StartingPlayerDetermined()
                            // winning dice
                            .setDiceRoll(highRoll)
                            // The starter of the placement/portplacement/turnactionsgamephase
                            .setPlayer(gameStarter)
                            // Server will send this message
                            .setSender(0)
                    );
                    return;
                }
                else
                {
                    // Starting player is not determined. Notify players and update Game object
                    game.getActionsQueue().enqueue
                    (
                        new RolledSame()
                            // Pass on the highest diceroll
                            .setHighRoll(highRoll)
                            // Server says dice rolled the same
                            .setSender(0)
                    );

                    // Enqueue each highroller 
                    for (RollDice sameRoll : rolledDices)
                    {
                        if (sameRoll.getDice() == highRoll) 
                        {
                            game.getActionsQueue().enqueue
                            (
                                new RollDice() 
                                    .setPlayer(sameRoll.getPlayer())
                            );
                        }
                    }

                    // First player is on turn
                    game.setPlayerOnTurn(game.getActionsQueue().peek().getPlayer());
                    return;
                }
            }

            // Next player should be the player next on the queue
            /* TODO: port to java
            game.setPlayerOnTurn = game.GetPlayer(game.ActionsQueue
                .OfType<RollDiceAction>()
                .First()
                .Sender);
            */
            
        }
    }

    /* (non-Javadoc)
     * @see soc.common.game.gamePhase.GamePhase#next(soc.common.game.Game)
     */
    @Override
    public GamePhase next(Game game)
    {
        // Determine if we should skip placing ports
        // randomports are assigned at start using the port lists on each territory.
        // The remaining ports are placed in the placement phase
        List<Port> allPorts = new ArrayList<Port>();
        for (Territory t : game.getBoard().getTerritories())
        {
            for (Port p : t.getPorts())
            {
                allPorts.add(p);
            }
        }
        if (allPorts.size() == 0)
        {
            // We do not have any ports to set, skip to placement phase
            return new InitialPlacementGamePhase();
        }
        else
        {
            // players should place ports
            return new InitialPlacementGamePhase();
        }
    }

}
