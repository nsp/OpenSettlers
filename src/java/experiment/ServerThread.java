/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package experiment;

import soc.server.SOCServer;

/**
 *
 * @author szityu
 */
public class ServerThread extends Thread
{
    public SOCServer server;
    
    @Override
    public void run()
    {
        server = new SOCServer(8880, 10, "root", "");
        server.setPriority(5);
        server.start();

//        String[] serverArgs = {"8880", "10", "root", ""};
//        soc.server.SOCServer.main(serverArgs);

    }
}
