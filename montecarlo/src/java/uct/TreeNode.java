/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uct;

import smartsettlers.boardlayout.GameStateConstants;

/**
 *
 * @author szityu
 */
public class TreeNode implements GameStateConstants {

    int nvisits;
    int[][] nwins;
    double[][] values;
    int nactions;
    int[] nactionvisits;
    int timeStamp;
    
    public TreeNode(int nactions)
    {
        nvisits = 0;
        nwins = new int[nactions][];
        values = new double[nactions][];
        for (int i=0; i<nactions; i++)
        {
            nwins[i] = new int[NPLAYERS];
            values[i] = new double[NPLAYERS];
        }
        this.nactions = nactions;
        nactionvisits = new int [nactions];
    }
    
    public TreeNode(int nactions, int player, int[] virtualwins)
    {
        this(nactions);
        for (int i=0; i<nactions; i++)
            nwins[i][player] = virtualwins[i];
    }
    // addVisit
    
    // selectAction
}
