package soc.common.game;

import java.util.ArrayList;
import java.util.List;

import soc.common.actions.gameAction.GameAction;
import soc.common.actions.gameAction.RolledSame;
import soc.common.actions.gameAction.turnActions.RollDice;

public class GameLog extends ArrayList<GameAction> implements IGameLog
{    
    @Override
    public void addAction(GameAction inGameAction)
    {
        // TODO Auto-generated method stub
        
    }
    
    public List<RollDice> getCurrentRoundRolls(Game game)
    {
        List<RollDice> result = new ArrayList<RollDice>();

        //We run the like a stack, first to examine is Last in the list
        for (int i = size() -1; i > 0; i--)
        {
            GameAction action = this.get(i);
            // we break the loop when we encountered a rolledsame action
            if (action instanceof RolledSame)
                break;

            // When we encounter a RollDiceAction, add it to the list
            if (action instanceof RollDice)
                result.add((RollDice)action);

            //We always have maximum of PlayerCount RollDiceAction
            if (result.size() == game.getPlayers().size())
                break;
        }

        return result;
    }

    public Player firstPlayerIsDetermined(Game game, int highRoll)
    {
        List<RollDice> rolledDices = getCurrentRoundRolls(game);

        // Get a list of all highrolls
        List<RollDice> highRolls = new ArrayList<RollDice>();
        for (RollDice rollDice : rolledDices)
        {
            if (rollDice.getDice() == highRoll)
                highRolls.add(rollDice);
        }

        // the player with highest dice is determined when we
        // have only one result
        if (highRolls.size()== 1)
        {
            return highRolls.get(0).getPlayer();
        }
        else
        {
            // return false
            return null;
        }
    }
}
