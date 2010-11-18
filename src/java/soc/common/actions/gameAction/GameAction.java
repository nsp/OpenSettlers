package soc.common.actions.gameAction;

import java.util.Date;

import soc.common.game.Game;
import soc.common.game.IGame;
import soc.common.game.Player;
import soc.common.game.User;
import soc.common.game.gamePhase.GamePhase;
import soc.common.game.gamePhase.turnPhase.TurnPhase;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/*
 * A GameAction performed in a game
 */
public class GameAction
{
    private Player player;
    private int sender;
    protected String invalidMessage;
    protected String toDoMessage;
    protected String message;
    
    /*
     * Should be omitted at hashCode calculation, since values differ at server
     * and at client
     */
    protected Date dateTimeExecuted;

    /**
     * @return the toDoMessage
     */
    public String getToDoMessage()
    {
        return toDoMessage;
    }
    
    /**
     * @return the dateTimeExecuted
     */
    public Date getDateTimeExecuted()
    {
        return dateTimeExecuted;
    }

    /**
     * @return the sender
     */
    public int getSender()
    {
        return sender;
    }

    /**
     * @param sender the sender to set
     */
    public GameAction setSender(int sender)
    {
        this.sender = sender;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return  this;
    }

    /**
     * @return the invalidMessage
     */
    public String getInvalidMessage()
    {
        return invalidMessage;
    }


    /**
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * @return the player
     */
    public Player getPlayer()
    {
        if (sender == 0 && player == null)
        {
            User p = new Player()
                .setId(0)
                .setName("Server");
            
            player = (Player)p;
        }
        return player;
    }
    
    
    /**
     * @param player the player to set
     */
    public GameAction setPlayer(Player player)
    {
        this.player = player;
        this.sender = player.getId();        

        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }

    public void perform(Game game)
    {
        dateTimeExecuted = new Date();
    }

    /* 
     * Returns true if player is allowed to play this action in given TurnPhase
     */
    public boolean isAllowed(TurnPhase turnPhase)
    {
        throw new NotImplementedException();
    }
    
    /* 
     * Returns true if player is allowed to play this action in given GamePhase
     */
    public boolean isAllowed(GamePhase gamePhase)
    {
        throw new NotImplementedException();
    }

}
