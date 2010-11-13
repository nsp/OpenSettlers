/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package experiment;

import java.util.logging.Level;
import java.util.logging.Logger;
import soc.robot.SOCRobotClient;
import soc.util.SOCRobotParameters;

/**
 *
 * @author szityu
 */
public class ClientThread extends ClientHelperThread
{
    SOCRobotParameters rp;
    
    ClientThread(int num, SOCRobotParameters params)
    {
        super("BuiltIn", num);
        rp = params;
    }
    
    @Override
    public void run()
    {
        try {
            sleep(5000);
            
            client = new SOCRobotClient("localhost", 8880, this.getName(), "");
//            ((SOCRobotClient) client)
            ((SOCRobotClient) client).init();
//            String[] clientArgs = {"localhost", "8880", "Computer" + num, ""};
//            soc.robot.SOCRobotClient.main(clientArgs);
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
