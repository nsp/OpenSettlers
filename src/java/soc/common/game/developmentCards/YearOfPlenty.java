package soc.common.game.developmentCards;

import soc.common.board.resources.ResourceList;
import soc.common.game.Game;
import soc.common.game.Player;

public class YearOfPlenty extends DevelopmentCard 
{
    //actual picked resources by player
    private ResourceList goldPick = new ResourceList();
    
    @Override
    public void play(Game game, Player player)
    {
        message = String.format("%s gained %s by playing a Year of Plenty card",
                player.getName(), goldPick.toString());
            
        // give player the resources
        player.getResources().swapResourcesFrom(goldPick, game.getBank());

        super.play(game, player);
    }
    
    @Override
    public boolean isValid(Game game)
    {
        if (!super.isValid(game))
            return false;
        
        if (goldPick == null)
            return false;
        
        if (goldPick.size() != 2)
            return false;
        
        return true;
    }
}
