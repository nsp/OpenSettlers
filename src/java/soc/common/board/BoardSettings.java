package soc.common.board;

public class BoardSettings
{
    // Minimum amount of players expected
    private int minPlayers = 3;
    
    // Maximum amount of players for this board
    private int maxPlayers = 4;
    
    // max allowed cards in hand when a 7 rolls
    private int maximumCardsInHandWhenSeven = 7;
    
    // Amount of vp to win on this board
    private int vpToWin = 10;
    
    
    public static BoardSettings standard()
    {
        BoardSettings settings = new BoardSettings();
        
        // default settings are good (for now?) 
        
        return settings;
    }

    /**
     * @return the minPlayers
     */
    public int getMinPlayers()
    {
        return minPlayers;
    }

    /**
     * @param minPlayers the minPlayers to set
     */
    public void setMinPlayers(int minPlayers)
    {
        this.minPlayers = minPlayers;
    }



    /**
     * @return the maxPlayers
     */
    public int getMaxPlayers()
    {
        return maxPlayers;
    }



    /**
     * @param maxPlayers the maxPlayers to set
     */
    public void setMaxPlayers(int maxPlayers)
    {
        this.maxPlayers = maxPlayers;
    }



    /**
     * @return the maximumCardsInHandWhenSeven
     */
    public int getMaximumCardsInHandWhenSeven()
    {
        return maximumCardsInHandWhenSeven;
    }



    /**
     * @param maximumCardsInHandWhenSeven the maximumCardsInHandWhenSeven to set
     */
    public void setMaximumCardsInHandWhenSeven(int maximumCardsInHandWhenSeven)
    {
        this.maximumCardsInHandWhenSeven = maximumCardsInHandWhenSeven;
    }



    /**
     * @return the vpToWin
     */
    public int getVpToWin()
    {
        return vpToWin;
    }



    /**
     * @param vpToWin the vpToWin to set
     */
    public void setVpToWin(int vpToWin)
    {
        this.vpToWin = vpToWin;
    }

}
