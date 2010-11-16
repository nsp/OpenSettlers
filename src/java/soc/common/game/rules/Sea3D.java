package soc.common.game.rules;

import soc.common.board.resources.Diamond;
import soc.common.board.resources.ResourceList;
import soc.common.game.Game;

public class Sea3D extends RuleSet
{
    public Sea3D(Game game)
    {
        super(game);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void createBank(int amount)
    {
        ResourceList result = game.getBank();

        // Sea3D supports Jungle's which produce diamonds
        for (int i = 0; i < amount * 2; i++)
            result.add(new Diamond());
        
        if (nextRuleSet != null)
            nextRuleSet.createBank(amount);
    }
    
    @Override
    public void initialize()
    {
        if (true) // TODO: add logic to determine if a placeport phase is necessary
        {
            // find DetermineFirstPlayerPhase, and add PlacePortPhase after
        }
    }
}
