/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package experiment;

import soc.client.SOCDisplaylessPlayerClient;

/**
 *
 * @author szityu
 */
public abstract class ClientHelperThread extends Thread
{
    int num;
    SOCDisplaylessPlayerClient client;
    
    ClientHelperThread(String name, int num)
    {
        super(name+num);
        this.num = num;
    }
    
}
