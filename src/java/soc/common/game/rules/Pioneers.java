package soc.common.game.rules;

import soc.common.game.Game;

/*
 * A Pioneers ruleset adds two pieces: a wall and a bridge.
 * - A wall can be purchased for 2 clay to increase the maximum amount of cards
 *   for a player by two
 * - A bridge behaves exactly like a road, except that it can be built on water.
 * 
 * Those two pieces can be built in the usual BuildingTurnPhase
 */
public class Pioneers extends RuleSet
{
    public Pioneers(Game game)
    {
        super(game);
        // TODO Auto-generated constructor stub
    }
    
    public void CreateBank(int amount)
    {
        if (nextRuleSet !=null)
        {
            nextRuleSet.createBank(amount);
        }
    }

    public void Initialize(Game game)
    {
        // get the BuildingTurnPhase, and add BuildWall and BuildBridge as allowed actions
    }
}
