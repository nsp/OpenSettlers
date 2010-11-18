package soc.common.actions.gameAction.turnActions;



public class RollDice extends TurnAction
{
    private int dice1;
    private int dice2;
    private int dice;
    /**
     * @return the dice1
     */
    public int getDice1()
    {
        return dice1;
    }
    /**
     * @param dice1 the dice1 to set
     */
    public RollDice setDice1(int dice1)
    {
        this.dice1 = dice1;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }
    /**
     * @return the dice2
     */
    public int getDice2()
    {
        return dice2;
    }
    /**
     * @param dice2 the dice2 to set
     */
    public RollDice setDice2(int dice2)
    {
        this.dice2 = dice2;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }
    /**
     * @return the dice
     */
    public int getDice()
    {
        return dice;
    }

}
