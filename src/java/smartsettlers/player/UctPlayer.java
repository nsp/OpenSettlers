/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.player;

import smartsettlers.boardlayout.BoardLayout;

/**
 *
 * @author szityu
 */

public class UctPlayer extends Player {

    public UctPlayer(BoardLayout bl, int position)
    {
        super(bl,position);
//        this.bl = bl;
//        this.position = position;
//        rnd = new Random(2);
    }
    
    public void selectAction(int[] s, int [] a)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int i,j;
        
        int[] s2 = BoardLayout.cloneOfState(s);  
        if (bl.uctTree != null)
        {
            bl.player[pl].listPossibilities(s2);
            for (j=0; j<bl.possibilities.n; j++)
            {
                int[] a2 = bl.possibilities.action[j];
                System.out.printf("%2d: [%d %d %d %d %d]  w:%f\n", j, a2[0], a2[1], a2[2], a2[3], a2[4], bl.possibilities.weight[j]);
            }
            bl.UCTsimulateGame(s2);
            int aind = bl.uctTree.selectAction(s, pl, true);
            bl.player[pl].listPossibilities(s);
            for (i=0; i<a.length; i++)
                a[i] = bl.possibilities.action[aind][i];
        
        }
        else
        {
        // possibilities are already listed...
        // select an action randomly
        
            bl.player[pl].listPossibilities(s);
            int[] a2 = bl.possibilities.randomAction();
            for (i=0; i<a2.length; i++)
                a[i] = a2[i];
        }
        s2=null;
    }
    
    
    public int selectMostUselessResourceInHand(int pl, int []s)
    {
        return selectRandomResourceInHand(pl, s);
    }

}
