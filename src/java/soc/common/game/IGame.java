package soc.common.game;

import java.util.List;

import soc.common.board.Board;
import soc.common.board.HexLocation;
import soc.common.board.resources.ResourceList;
import soc.common.game.gamePhase.IGamePhase;

public interface IGame
{
    // List of actions during the game
    public IGameLog getGameLog();
    
    // List of actions expected to be performed
    public IActionsQueue getActionsQueue();
    
    // List of players in the game
    public List<Player> getPlayers();
    
    // The pirate is no more then a location on a hex
    public HexLocation getPirate();
    
    // Bank, list of available resources
    public ResourceList getBank();
    
    // List of users watching the game
    public List<User> getSpectators();
    
    // Current player on turn
    public Player getPlayerOnTurn();
    
    // Player which turn it is next turn
    public Player getNextPlayerOnTurn();
    
    // Get the player object instance from an id
    public Player getPlayer(int playerID);
    
    // Phase where the game is in
    public IGamePhase getGamePhase();
    
    // Board with hextiles on it
    public Board getBoard();
}
