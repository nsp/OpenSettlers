/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.player;

import java.util.Random;
import smartsettlers.boardlayout.BoardLayout;

/**
 *
 * @author szityu
 */

public class RandomPlayer extends Player {

    public RandomPlayer(BoardLayout bl, int position)
    {
        super(bl,position);
//        this.bl = bl;
//        this.position = position;
//        rnd = new Random(2);
    }
    
    public void selectAction(int[] s, int [] a)
    {
        
        // possibilities are already listed...
        // select an action randomly

        int j;
        for (j=0; j<bl.possibilities.n; j++)
        {
            int[] a2 = bl.possibilities.action[j];
            System.out.printf("%2d: [%d %d %d %d %d]  w:%f\n", j, a2[0], a2[1], a2[2], a2[3], a2[4], bl.possibilities.weight[j]);
        }
        System.out.print("\n==========\n");
        int[] a2 = bl.possibilities.randomAction();
        int i;
        for (i=0; i<a2.length; i++)
            a[i] = a2[i];
    }
    
    public int selectMostUselessResourceInHand(int pl, int []s)
    {
        return selectRandomResourceInHand(pl, s);
    }

}
