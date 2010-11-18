package soc.common.game;

import soc.common.board.resources.ResourceList;

public class Player extends User
{
    private ResourceList resources;
    private int maximumCardsInHandWhenSeven;
    private int stockRoads = 15;
    private int stockShips = 15;
    private int stockTowns = 5;
    private int stockCities = 4;
    private boolean isOnTurn=false;
    
    /**
     * @return the isOnTurn
     */
    public boolean isOnTurn()
    {
        return isOnTurn;
    }

    /**
     * @param isOnTurn the isOnTurn to set
     */
    public Player setOnTurn(boolean isOnTurn)
    {
        this.isOnTurn = isOnTurn;
    
        // Enables fluent interface usage
        // http://en.wikipedia.org/wiki/Fluent_interface
        return this;
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

    public int getMaximumCardsInHandWhenSeven()
    {
        return maximumCardsInHandWhenSeven;
    }

    public void setMaximumCardsInHandWhenSeven(int maximumCardsInHandWhenSeven)
    {
        this.maximumCardsInHandWhenSeven = maximumCardsInHandWhenSeven;
    }

    public ResourceList getResources()
    {
        return resources;
    }

    public void setResources(ResourceList resources)
    {
        this.resources = resources;
    }
    
}
