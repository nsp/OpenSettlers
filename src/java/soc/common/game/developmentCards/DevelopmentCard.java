package soc.common.game.developmentCards;

import soc.common.game.Game;
import soc.common.game.Player;
import soc.common.game.gamePhase.GamePhase;
import soc.common.game.gamePhase.turnPhase.TurnPhase;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class DevelopmentCard
{
    protected String invalidMessage;
    protected String message;
    protected int _turnBought = 0;
    private int id = 0;
    private boolean isPlayable = false;
    
    public void play(Game game, Player player)
    {
        isPlayable = false;
    }
    
    public boolean isValid(Game game)
    {
        return true;     
    }
    
    /* 
     * Returns true if player is allowed to play this card in given TurnPhase
     */
    public boolean isAllowed(TurnPhase turnPhase)
    {
        throw new NotImplementedException();
    }
    
    /* 
     * Returns true if player is allowed to play this card in given GamePhase
     */
    public boolean isAllowed(GamePhase turnPhase)
    {
        throw new NotImplementedException();
    }
    
    public String getInvalidMessage()
    {
        return invalidMessage;
    }
    public void setInvalidMessage(String invalidMessage)
    {
        this.invalidMessage = invalidMessage;
    }
    public String getMessage()
    {
        return message;
    }
    public void setMessage(String message)
    {
        this.message = message;
    }
    public int get_turnBought()
    {
        return _turnBought;
    }
    public void set_turnBought(int turnBought)
    {
        _turnBought = turnBought;
    }
    public int getId()
    {
        return id;
    }
    public void setId(int id)
    {
        this.id = id;
    }
    public boolean isPlayable()
    {
        return isPlayable;
    }
    public void setPlayable(boolean isPlayable)
    {
        this.isPlayable = isPlayable;
    }
}
