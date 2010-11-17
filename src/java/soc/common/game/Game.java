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
