package experiment;

import java.util.logging.Level;
import java.util.logging.Logger;

import smartsettlers.boardlayout.GameStateConstants;
import soc.client.SOCPlayerClient;
import soc.client.SOCPlayerInterface;
import soc.game.SOCGame;
import soc.message.SOCCreateAccount;
import soc.message.SOCJoinGame;
import soc.message.SOCSitDown;

/**
 *
 * @author szityu
 */
public class Experiment implements GameStateConstants {

    
    Experiment()
    {
        
    }
    
    /**
     * 
     * @param args {run#, 
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException
    {
//        soc.debug.D.ebug_disable();
        if(args.length < 5) {
            System.err.println("Not enough arguments");
        }
        int gameno = Integer.parseInt(args[0]);
        String gamename = "game" + gameno;

        ClientHelperThread[] clients = new ClientHelperThread[4];
        for (int i = 0; i < clients.length; i++)
        {
            if(args[i+1].contains("builtin"))
                clients[i] = new ClientThread(10*gameno+i);
            else if(args[i+1].contains("monte"))
                clients[i] = new SmartSettlersClientThread(10*gameno+i);
            else if(args[i+1].contains("multi"))
                clients[i] = new MultiAgentClientThread(10*gameno+i);
            else {
                System.err.println("Bad argument: " + args[i+1]);
                clients[i] = new ClientThread(10*gameno+i);
            }
        }
        TimerThread ttr = new TimerThread();
        
        for (int i = 0; i < clients.length; i++)
        {
            clients[i].start();
        }
        ttr.start();
        Thread.sleep(6000);
        
        SOCPlayerClient client = new SOCPlayerClient();
        client.host = "localhost";
        client.port = 8880;
        client.nickname = "Experimenter"+args[0];
        String password = "pass";
        client.initVisualElements(); // after the background is set
        client.connect();
        client.put(SOCCreateAccount.toCmd(client.nickname, password, "localhost", ""), false);
        //client.put(SOCJoin.toCmd(client.nickname, "", client.host, "channel0"));
        //client.put(SOCJoinGameAuth.toCmd(gamename));
        SOCGame ga = new SOCGame(gamename);

        if (ga != null)
        {
//            SOCPlayerInterface pi = new SOCPlayerInterface(gamename, client, ga);
//            pi.setVisible(true);
//            client.playerInterfaces.put(gamename, pi);
            client.games.put(gamename, ga);
        }

        client.put(SOCJoinGame.toCmd(client.nickname, password, client.host, gamename), false);
        ga = (SOCGame) client.games.get(gamename);
        
        for (int j = 0; j < clients.length; j++)
        {
            ClientHelperThread bot = clients[j];
            bot.client.put(SOCJoinGame.toCmd(bot.client.getNickname(), "", client.host, gamename));
            bot.client.put(SOCSitDown.toCmd(gamename, bot.client.getNickname(), j, true));
        }
        
        SOCPlayerInterface pi = (SOCPlayerInterface) client.playerInterfaces.get(gamename);
        Thread.sleep(5000);
        client.startGame(ga);


    }
}

class TimerThread extends Thread
{
    TimerThread()
    {
        super("Timer");
    }
    
    @Override
    public void run()
    {
        try {
            sleep(1000*60*60);
        } catch (InterruptedException ex) {
            Logger.getLogger(TimerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("TIMEOUT!!!!");
        System.exit(-1);
    }
}
