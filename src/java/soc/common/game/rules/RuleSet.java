package soc.common.game.rules;

import soc.common.board.resources.Clay;
import soc.common.board.resources.Ore;
import soc.common.board.resources.ResourceList;
import soc.common.board.resources.Sheep;
import soc.common.board.resources.Timber;
import soc.common.board.resources.Wheat;
import soc.common.game.Game;

/*
 * Basic standard settlers ruleset
 */
public class RuleSet
{
    protected RuleSet nextRuleSet;
    private int bankAmount = 19;
    protected Game game;
    
    public RuleSet(Game game)
    {
        this.game=game;        
    }
    
    public void createBank(int amount)
    {
        ResourceList result = game.getBank();
        
        // Standard Settlers has 19 cards in the bank for each of 5 resources
        for (int i=0; i< amount; i++)
            result.add(new Timber());
        for (int i=0; i< amount; i++)
            result.add(new Wheat());
        for (int i=0; i< amount; i++)
            result.add(new Ore());
        for (int i=0; i< amount; i++)
            result.add(new Clay());
        for (int i=0; i< amount; i++)
            result.add(new Sheep());
        
        // Call next ruleset to add additional stock resources to the bank
        if (nextRuleSet != null)
            nextRuleSet.createBank(amount);
    }

    public int getBankAmount()
    {
        return bankAmount;
    }

    public void setBankAmount(int bankAmount)
    {
        this.bankAmount = bankAmount;
    }

    public Game getGame()
    {
        return game;
    }

    public void setGame(Game game)
    {
        this.game = game;
    }

    public void setNextRuleSet(RuleSet nextRuleSet)
    {
        this.nextRuleSet = nextRuleSet;
    }

    public void initialize()
    {
        // TODO Auto-generated method stub
        
    }
}
