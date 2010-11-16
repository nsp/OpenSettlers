package soc.common.board;

import java.util.List;
import java.util.Random;

import soc.common.board.hexes.Hex;
import soc.common.board.hexes.ITerritoryHex;
import soc.common.board.hexes.RandomHex;
import soc.common.board.hexes.SeaHex;
import soc.common.game.GameSettings;


/// <summary>
/// Represents the board data structure. 
/// 
/// A board is made up of hexes in a 2D matrix. The even rows of the matrix 
/// have an indentation length on the left side half the width of a hex.
/// For example, a 5x5 sized board will have the layout of:
/// 
/// <code>
/// |H| |H| |H| |H| |H|     0
///   |H| |H| |H| |H| |H|   1
/// |H| |H| |H| |H| |H|     2
///   |H| |H| |H| |H| |H|   3
/// |H| |H| |H| |H| |H|     4
/// </code>
/// 
/// Sea3D has the same layout, only has the last hexes of the even rows 
/// omitted. Thus, a Sea3D 'compatible' board would have the following
/// layout:
/// 
/// <code>
/// |H| |H| |H| |H| |H|     0
///   |H| |H| |H| |H|       1
/// |H| |H| |H| |H| |H|     2
///   |H| |H| |H| |H|       3
/// |H| |H| |H| |H| |H|     4
/// </code>
/// 
/// The last hexes of each even row in a 'Sea3D compatible' configuration
/// should be made invisible, and/or locked.
/// </summary>

public class Board
{
    // list of hexes this board is made of
    public HexList hexes;
    
    //Name of the creator of the board
    private String _Creator = "Unknown player";
    
    // data fields
    private String name = "New Board";
    private boolean useTradeRoutes = false;
    private boolean assignPortsBeforePlacement = false;
    private boolean requiresInitialShips = false;
    private int allowedCards = 7;
    private int bankResources = 19;
    private int stockRoads = 15;
    private int stockShips = 15;
    private int stockTowns = 5;
    private int stockCities = 4;
    private int bonusNewIsland;
    private int maxPlayers = 4;
    private int minPlayers = 3;
    private int vpToWin = 10;
    private int width = 0;
    private int height = 0;
    private TerritoryList territories;

    private int maximumCardsInHandWhenSeven = 7;
    
    //private StandardDevCardStack _DevCards = new StandardDevCardStack(5, 2, 14, 2, 2);


    /// <summary>
    /// Resizes the board to a new size. 
    /// </summary>
    /// <param name="newWidth">New width of the board</param>
    /// <param name="newHeight">New height of the board</param>
    public void Resize(int newWidth, int newHeight, Hex defaultHex)
    {
        // default on seahexes if we have no default
        if (defaultHex == null) defaultHex = new SeaHex();
        
        //return if there is nothing to resize
        if (width == newWidth && height == newHeight)
        {
            return;
        }

        //Instantiate a new board
        HexList newboard = new HexList(newWidth, newHeight);

        //loop through new sized matrix.
        for (int h = 0; h < newHeight; h++)
        {
            for (int w = 0; w < newWidth; w++)
            {
                //when width or height is bigger then original, add hexes
                if (w >= width || h >= height)
                {
                    Hex newHex = null;

                    //if outer bounds, put a SeaHex in place, otherwise a defaulthex
                    if (w == newWidth - 1 || w == 0 || h == newHeight - 1 || h == 0)
                        newHex = new SeaHex();
                    else
                        newHex = defaultHex.Copy();

                    newHex.setLocation(new HexLocation(w,h));
                    newboard.set(w, h, newHex);
                }
                else
                {
                    //if outer bounds, put a seahex in place, 
                    // otherwise the defaulthex
                    if (w == newWidth - 1 || w == 0 || h == newHeight - 1 || h == 0)
                    {
                        newboard.set(w, h, new SeaHex());
                    }
                    else
                    {
                        newboard.set(w, h, defaultHex.Copy());
                    }

                    newboard.set(w, h, hexes.get(w, h).Copy());
                }

            }
        }
        hexes = newboard;
    }
    
    /// <summary>
    /// Prepares a saved board definition into a playable board.
    /// 1. Puts hexes from InitialRandomHexes list on RandomHexes
    /// 2. Replaces random ports from those out of RandomPorts bag
    /// 3. Replaces deserts by volcano/jungles if necessary
    /// </summary>
    public void PrepareForPlay(GameSettings settings)
    {
        // TODO: add code from JSettlers

    }

    public HexList getHexes()
    {
        return hexes;
    }

    public void setHexes(HexList hexes)
    {
        this.hexes = hexes;
    }

    public String get_Creator()
    {
        return _Creator;
    }

    public void set_Creator(String creator)
    {
        _Creator = creator;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isUseTradeRoutes()
    {
        return useTradeRoutes;
    }

    public void setUseTradeRoutes(boolean useTradeRoutes)
    {
        this.useTradeRoutes = useTradeRoutes;
    }

    public boolean isAssignPortsBeforePlacement()
    {
        return assignPortsBeforePlacement;
    }

    public void setAssignPortsBeforePlacement(boolean assignPortsBeforePlacement)
    {
        this.assignPortsBeforePlacement = assignPortsBeforePlacement;
    }

    public boolean isRequiresInitialShips()
    {
        return requiresInitialShips;
    }

    public void setRequiresInitialShips(boolean requiresInitialShips)
    {
        this.requiresInitialShips = requiresInitialShips;
    }

    public int getAllowedCards()
    {
        return allowedCards;
    }

    public void setAllowedCards(int allowedCards)
    {
        this.allowedCards = allowedCards;
    }

    public int getBankResources()
    {
        return bankResources;
    }

    public void setBankResources(int bankResources)
    {
        this.bankResources = bankResources;
    }

    public int getStockRoads()
    {
        return stockRoads;
    }

    public void setStockRoads(int stockRoads)
    {
        this.stockRoads = stockRoads;
    }

    public int getStockShips()
    {
        return stockShips;
    }

    public void setStockShips(int stockShips)
    {
        this.stockShips = stockShips;
    }

    public int getStockTowns()
    {
        return stockTowns;
    }

    public void setStockTowns(int stockTowns)
    {
        this.stockTowns = stockTowns;
    }

    public int getStockCities()
    {
        return stockCities;
    }

    public void setStockCities(int stockCities)
    {
        this.stockCities = stockCities;
    }

    public int getBonusNewIsland()
    {
        return bonusNewIsland;
    }

    public void setBonusNewIsland(int bonusNewIsland)
    {
        this.bonusNewIsland = bonusNewIsland;
    }

    public int getMaxPlayers()
    {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers)
    {
        this.maxPlayers = maxPlayers;
    }

    public int getMinPlayers()
    {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers)
    {
        this.minPlayers = minPlayers;
    }

    public int getVpToWin()
    {
        return vpToWin;
    }

    public void setVpToWin(int vpToWin)
    {
        this.vpToWin = vpToWin;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public int getHeight()
    {
        return height;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    public TerritoryList getTerritories()
    {
        return territories;
    }

    public void setTerritories(TerritoryList territories)
    {
        this.territories = territories;
    }

    public int getMaximumCardsInHandWhenSeven()
    {
        return maximumCardsInHandWhenSeven;
    }

    public void setMaximumCardsInHandWhenSeven(int maximumCardsInHandWhenSeven)
    {
        this.maximumCardsInHandWhenSeven = maximumCardsInHandWhenSeven;
    }
}
