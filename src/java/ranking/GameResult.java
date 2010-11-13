package ranking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jskills.GameInfo;
import jskills.IPlayer;
import jskills.ITeam;
import jskills.Player;
import jskills.Rating;
import jskills.Team;

public class GameResult
{
    List<Player<String>> players = new ArrayList<Player<String>>();
    
    int[] vicPts = new int[4];

    private GameInfo gi;

    private Map<? extends IPlayer, Rating> existRatings;
    
    public GameResult(ResultSet results, GameInfo ginfo, Map<? extends IPlayer, Rating> existingRatings) throws SQLException
    {
        gi = ginfo;
        existRatings = existingRatings;
        players.add(new Player<String>(results.getString("player1")));
        players.add(new Player<String>(results.getString("player2")));
        players.add(new Player<String>(results.getString("player3")));
        players.add(new Player<String>(results.getString("player4")));
        vicPts[0] = -results.getInt("score1");
        vicPts[1] = -results.getInt("score2");
        vicPts[2] = -results.getInt("score3");
        vicPts[3] = -results.getInt("score4");
    }
    
    public int[] getTeamsAndRatings(Collection<ITeam> teams) {
        for (Player<String> p : players)
        {
            if(existRatings.containsKey(p))
                teams.add(new Team(p, existRatings.get(p)));
            else
                teams.add(new Team(p, gi.getDefaultRating()));
        }
        return vicPts;
    }
}
