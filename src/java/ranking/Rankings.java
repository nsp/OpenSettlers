package ranking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jskills.GameInfo;
import jskills.IPlayer;
import jskills.ITeam;
import jskills.Player;
import jskills.Rating;
import jskills.TrueSkillCalculator;
import jskills.elo.DuellingEloCalculator;
import jskills.elo.FideEloCalculator;
import jskills.elo.GaussianEloCalculator;
import jskills.elo.TwoPlayerEloCalculator;
import soc.server.database.SOCDBHelper;

public class Rankings
{

    private static String GAME_INFO_QUERY =      "SELECT * FROM games ORDER BY endtime ASC;";
    
    public static void calculate() throws SQLException, FileNotFoundException {
//        for (String s : new File("data").list())
//        {
//            new File(s).delete();
//        }
        Properties props = new Properties();
        props.setProperty(SOCDBHelper.PROP_JSETTLERS_DB_USER, "root");
        props.setProperty(SOCDBHelper.PROP_JSETTLERS_DB_PASS, "root");
        SOCDBHelper.initialize(props);
        PreparedStatement gameInfoQuery = SOCDBHelper.connection.prepareStatement(GAME_INFO_QUERY);
        ResultSet results = gameInfoQuery.executeQuery();

        GameInfo gameInfo = GameInfo.getDefaultGameInfo();
        Map<IPlayer, Rating> ratings = new HashMap<IPlayer, Rating>();
        while(results.next())
        {
            GameResult game = new GameResult(results, gameInfo, ratings);
            Collection<ITeam> teams = new ArrayList<ITeam>();
            int[] teamRanks = game.getTeamsAndRatings(teams);
            Map<IPlayer, Rating> newRatings = new DuellingEloCalculator(new FideEloCalculator()).calculateNewRatings(gameInfo, teams, teamRanks);//TrueSkillCalculator.calculateNewRatings(gameInfo, teams, teamRanks);
            for (Map.Entry<IPlayer, Rating> e : newRatings.entrySet())
            {
                FileOutputStream fos = new FileOutputStream("elodata/"+e.getKey().toString(), ratings.containsKey(e.getKey()));
                Writer w = new OutputStreamWriter(fos, Charset.forName("US-ASCII"));
                try
                {
                    if(!ratings.containsKey(e.getKey()))
                        w.write(String.format("%g,%g,%g\n", gameInfo.getDefaultRating().getMean(), gameInfo.getDefaultRating().getStandardDeviation(), gameInfo.getDefaultRating().getConservativeRating()));
                    w.write(String.format("%g,%g,%g\n", e.getValue().getMean(), e.getValue().getStandardDeviation(), e.getValue().getConservativeRating()));
                    w.flush();
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
                finally
                {
                    try { fos.close(); } catch (IOException e2) { }
                }
            }
            ratings.putAll(newRatings);
            
        }
    }
    
    public static void main(String[] args) throws Exception
    {
            calculate();
    }
}
