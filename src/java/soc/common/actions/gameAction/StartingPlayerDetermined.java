package soc.common.actions.gameAction;

public class StartingPlayerDetermined extends GameAction
{
    private int diceRoll;

    /**
     * @return the diceRoll
     */
    public int getDiceRoll()
    {
        return diceRoll;
    }

    /**
     * @param diceRoll the diceRoll to set
     */
    public StartingPlayerDetermined setDiceRoll(int diceRoll)
    {
        this.diceRoll = diceRoll;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }


}
