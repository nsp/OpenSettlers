package soc.common.game;

import java.util.UUID;

import soc.common.board.Board;
import soc.common.board.BoardSettings;

public class GameSettings
{
    // Game boardsettings may be overridden by the user
    // This implies not having a ladder game (where settings should equal
    // original boardsettings)
    private BoardSettings boardSettings = BoardSettings.standard();
    
    /**
     * @return the boardSettings
     */
    public BoardSettings getBoardSettings()
    {
        return boardSettings;
    }
    /**
     * @param boardSettings the boardSettings to set
     */
    public void setBoardSettings(BoardSettings boardSettings)
    {
        this.boardSettings = boardSettings;
    }
    // Whether or not deserts will be replaced by jungles
    private boolean replaceDesertWithJungles = false;
    
    // Whether or not deserts will b replaces by volcanos
    private boolean replaceDesertWithVolcanos = false;
    
    // If selected, the build phase's second town will be a city,
    // and a third road is added
    private boolean tournamentStart = false;
    
    // Whether or not first round has sevens
    private int noSevensFirstRound = 0;
    
    // Whether or not robbing from players with 2vp is allowed
    private boolean no2VPPlayersRobbing = false;
    
    // Whether or not players can trade after they entered the build phase
    private boolean tradingAfterBuilding = false;
    
    // Players do not see the chitnumbers before initial placement
    // TODO:implement
    private boolean showChitsAfterPlacing = false;

    private int maximumTradesPerTurn = 2;
    private String mame = null;
    private int host;

    private UUID boardGuid;
    private boolean isLadder = true;
    private Board board;
    
    
    public boolean isReplaceDesertWithJungles()
    {
        return replaceDesertWithJungles;
    }
    public void setReplaceDesertWithJungles(boolean replaceDesertWithJungles)
    {
        this.replaceDesertWithJungles = replaceDesertWithJungles;
    }
    public boolean isReplaceDesertWithVolcanos()
    {
        return replaceDesertWithVolcanos;
    }
    public void setReplaceDesertWithVolcanos(boolean replaceDesertWithVolcanos)
    {
        this.replaceDesertWithVolcanos = replaceDesertWithVolcanos;
    }

    public boolean isTournamentStart()
    {
        return tournamentStart;
    }
    public void setTournamentStart(boolean tournamentStart)
    {
        this.tournamentStart = tournamentStart;
    }
    public int getNoSevensFirstRound()
    {
        return noSevensFirstRound;
    }
    public void setNoSevensFirstRound(int noSevensFirstRound)
    {
        this.noSevensFirstRound = noSevensFirstRound;
    }
    public boolean isNo2VPPlayersRobbing()
    {
        return no2VPPlayersRobbing;
    }
    public void setNo2VPPlayersRobbing(boolean no2vpPlayersRobbing)
    {
        no2VPPlayersRobbing = no2vpPlayersRobbing;
    }
    public boolean isTradingAfterBuilding()
    {
        return tradingAfterBuilding;
    }
    public void setTradingAfterBuilding(boolean tradingAfterBuilding)
    {
        this.tradingAfterBuilding = tradingAfterBuilding;
    }
    public boolean isShowChitsAfterPlacing()
    {
        return showChitsAfterPlacing;
    }
    public void setShowChitsAfterPlacing(boolean showChitsAfterPlacing)
    {
        this.showChitsAfterPlacing = showChitsAfterPlacing;
    }

    public int getMaximumTradesPerTurn()
    {
        return maximumTradesPerTurn;
    }
    public void setMaximumTradesPerTurn(int maximumTradesPerTurn)
    {
        this.maximumTradesPerTurn = maximumTradesPerTurn;
    }
    public String getMame()
    {
        return mame;
    }
    public void setMame(String mame)
    {
        this.mame = mame;
    }
    public int getHost()
    {
        return host;
    }
    
    public void setHost(int host)
    {
        this.host = host;
    }

    public UUID getBoardGuid()
    {
        return boardGuid;
    }
    public void setBoardGuid(UUID boardGuid)
    {
        this.boardGuid = boardGuid;
    }
    public boolean isLadder()
    {
        return isLadder;
    }
    public void setLadder(boolean isLadder)
    {
        this.isLadder = isLadder;
    }
    public Board getBoard()
    {
        return board;
    }
    public void setBoard(Board board)
    {
        this.board = board;
    }

}
