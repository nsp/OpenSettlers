/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package experiment;

import java.util.logging.Level;
import java.util.logging.Logger;
import soc.robot.SOCRobotClient;

/**
 *
 * @author szityu
 */
public class ClientThread extends Thread
{
    int num;
    SOCRobotClient client;
    
    ClientThread(int num)
    {
        this.num = num;
    }
    
    @Override
    public void run()
    {
        try {
            sleep(5000);
            
            client = new SOCRobotClient("localhost", 8880, "Computer" + num, "");
            client.init();
//            String[] clientArgs = {"localhost", "8880", "Computer" + num, ""};
//            soc.robot.SOCRobotClient.main(clientArgs);
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
