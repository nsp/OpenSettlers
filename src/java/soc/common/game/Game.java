package soc.common.game;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import soc.common.board.HexLocation;
import soc.common.board.resources.ResourceList;
import soc.common.game.gamePhase.GamePhase;
import soc.common.game.gamePhase.LobbyGamePhase;
import soc.common.game.rules.Pioneers;
import soc.common.game.rules.RuleSet;
import soc.common.game.rules.Sea3D;

public class Game
{
    private RuleSet ruleSet;
    private LinkedList<GamePhase> gamePhases = new LinkedList<GamePhase>();
    private IActionsQueue actionsQueue = new ActionsQueue();
    private ResourceList bank = new ResourceList();
    private List<Player> players = new ArrayList<Player>();
    private GameLog gameLog = new GameLog();
    private HexLocation pirate = new HexLocation(0,0);
    private GamePhase currentPhase = new LobbyGamePhase();
    private GameSettings gameSettings = new GameSettings();
    private Player playerOnTurn;
    
    
    public Player getPlayerByID(int id)
    {
        for (Player p : players)
        {
            if (p.getId() == id)
                return p;
        }
        throw new RuntimeException(
                "Trying to get non-existing player. ID " + id + " is unknown");
    }
    /**
     * @return the playerOnTurn
     */
    public Player getPlayerOnTurn()
    {
        if (playerOnTurn == null)
        {
            playerOnTurn = players.get(0);
        }
        return playerOnTurn;
    }

    /**
     * @param playerOnTurn the playerOnTurn to set
     */
    public Game setPlayerOnTurn(Player playerOnTurn)
    {
        playerOnTurn.setOnTurn(false);
        this.playerOnTurn = playerOnTurn;
        playerOnTurn.setOnTurn(true);
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
    }

    /**
     * @return the gameSettings
     */
    public GameSettings getGameSettings()
    {
        return gameSettings;
    }

    /**
     * @param gameSettings the gameSettings to set
     */
    public void setGameSettings(GameSettings gameSettings)
    {
        this.gameSettings = gameSettings;
    }

    Game()
    {
        ruleSet = new RuleSet(this);
        ruleSet.setNextRuleSet(new Pioneers(this));
        
        ruleSet.createBank(19);
        
        ruleSet.initialize();
    }
    
    public ResourceList getBank()
    {
        return bank;
    }

    public void setBank(ResourceList bank)
    {
        this.bank = bank;
    }

    public LinkedList<GamePhase> getGamePhases()
    {
        return gamePhases;
    }

    public void setGamePhases(LinkedList<GamePhase> gamePhases)
    {
        this.gamePhases = gamePhases;
    }

    public RuleSet getRuleSet()
    {
        return ruleSet;
    }

    public void setRuleSet(RuleSet ruleSet)
    {
        this.ruleSet = ruleSet;
    }

    public IActionsQueue getActionsQueue()
    {
        return actionsQueue;
    }

    public void setActionsQueue(IActionsQueue actionsQueue)
    {
        this.actionsQueue = actionsQueue;
    }

    public List<Player> getPlayers()
    {
        return players;
    }

    public void setPlayers(List<Player> players)
    {
        this.players = players;
    }

    public GameLog getGameLog()
    {
        return gameLog;
    }

    public void setGameLog(GameLog gameLog)
    {
        this.gameLog = gameLog;
    }

    public HexLocation getPirate()
    {
        return pirate;
    }

    public void setPirate(HexLocation pirate)
    {
        this.pirate = pirate;
    }

    public GamePhase getCurrentPhase()
    {
        return currentPhase;
    }

    public void setCurrentPhase(GamePhase currentPhase)
    {
        this.currentPhase = currentPhase;
    }  
}
