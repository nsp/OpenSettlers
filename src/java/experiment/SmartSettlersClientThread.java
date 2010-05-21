/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package experiment;

import java.util.logging.Level;
import java.util.logging.Logger;
import soc.robot.SSRobotClient;

/**
 *
 * @author szityu
 */
public class SmartSettlersClientThread extends ClientHelperThread
{
    
    SmartSettlersClientThread(int num)
    {
        super("Monte", num);
    }
    
    @Override
    public void run()
    {
        try {
            sleep(5000);
            client = new SSRobotClient("localhost", 8880, getName(), "");
            ((SSRobotClient) client).init();
//            String[] clientArgs = {"localhost", "8880", "SmartSettlers agent", ""};
//            soc.robot.SSRobotClient.main(clientArgs);
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
