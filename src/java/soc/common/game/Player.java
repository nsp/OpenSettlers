package soc.common.game;

import soc.common.board.resources.ResourceList;

public class Player
{
    private int id;
    private String name;
    private ResourceList resources;
    private int maximumCardsInHandWhenSeven;
    private int stockRoads = 15;
    private int stockShips = 15;
    private int stockTowns = 5;
    private int stockCities = 4;
    
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

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
}
