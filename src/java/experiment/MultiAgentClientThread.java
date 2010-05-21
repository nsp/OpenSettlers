/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package experiment;

import java.util.logging.Level;
import java.util.logging.Logger;

import soc.robot.InterfaceAgent;

/**
 *
 * @author szityu
 */
public class MultiAgentClientThread extends ClientHelperThread
{
    
    MultiAgentClientThread(int num)
    {
        super("MultiAgent", num);
    }
    
    @Override
    public void run()
    {
        try {
            sleep(5000);
            
            client = new InterfaceAgent("localhost", 8880, getName(), "");
            ((InterfaceAgent) client).init();
//            String[] clientArgs = {"localhost", "8880", "Computer" + num, ""};
//            soc.robot.SOCRobotClient.main(clientArgs);
        } catch (InterruptedException ex) {
            Logger.getLogger(MultiAgentClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
