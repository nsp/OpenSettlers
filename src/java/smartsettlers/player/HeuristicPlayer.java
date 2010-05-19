/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.player;

import smartsettlers.boardlayout.BoardLayout;
//import soc.game.ModSOCPlayerNumbers;
import smartsettlers.boardlayout.GameStateConstants;
import smartsettlers.boardlayout.HexTypeConstants;

/**
 *
 * @author szityu
 */
public class HeuristicPlayer extends Player implements GameStateConstants {

    /**
     * the numbers that our settlements are touching
     */
//    private ModSOCPlayerNumbers ourNumbers = new ModSOCPlayerNumbers();
    
    public HeuristicPlayer(BoardLayout bl, int position)
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
    
    Resource[] stock = new Resource[NPLAYERS];
    Resource[] prodPerTurn = new Resource[NPLAYERS];
    int[][] exchRatio = new int[NPLAYERS][NRESOURCES];
    // TODO: calculate these
    int[] totalVP   = new int[NPLAYERS]; // total victory points
    int[] tempVP    = new int[NPLAYERS]; // victory points for longest road/largest army
    int[] regularVP = new int[NPLAYERS]; // VP for settlements and cities
    int[] cardVP    = new int[NPLAYERS]; // VP for +1 point cards
    double[] internalScore    = new double[NPLAYERS];  // score for ranking players
    int strongestOpponentPl;
    double[] totalProdPerTurn = new double[NPLAYERS];  // expected numbr of cards per turn
    // estimated time to build things
    double[] etbRoad;  
    double[] etbStlmt;
    double[] etbCity;
    double[] etbCard;
    double[] etbLongestRoad;
    double[] etbLargestArmy;
    
    // TODO: calc. shortest road to each potential settlement
    // from this: calc. total influence on the map
        
    // calc. Estimated time to build "what"
    private double calcETB(int pl, Resource what)
    {
        int i;
        Resource st0 = new Resource(stock[pl].res);
        double val;
        
//        for (i=0; i<NRESOURCES; i++)
//        {
//            val = Math.min(st0.res[i], what.res[i]);
//            st0.res[i] -= val;
//            what.res[i] -= val;
//        }
        if (Resource.Contains(st0,what))
            return 0;  // "what" can be built immediately
        
        // logarithmic search to find the ETB
        int low = 0;
        int up = 1000;
        int mid;
        int njokers = 0, nmissing = 0;
        while (up-low > 1)
        {
            mid = (up+low)/2;
            Resource st = new Resource(Resource.Add(st0, Resource.Mult(prodPerTurn[pl], mid)).res);
            
            //calc. number of "joker resources" obtainable from bank trades
            njokers = 0;
            nmissing = 0;
            for (i=0; i<NRESOURCES; i++)
            {
                val = Math.max(0, (st.res[i]-what.res[i])/exchRatio[pl][i]);
                njokers += (int)val;
                
                nmissing += (int) Math.max(0, what.res[i]-st.res[i]);
            }
            if (njokers >= nmissing)
                up = mid;
            else
                low = mid;
        }
        return (int) (up + 0.5*njokers);
        //add +0.5 penalty per joker resources
    }
    
    public void analyzeState(int pl0, int[] s, BoardLayout bl)
    {
        int pl;

        
        
        int i, ind, ind2, val, j, k;
        double rate;
        
        for (ind = 0; ind < N_HEXES; ind++) 
        {
            if (s[OFS_ROBBERPLACE] == ind) 
                continue;            
            val = bl.hextiles[ind].productionNumber;
            if (val > 7) 
                val = 14 - val;
            rate = (val - 1) / 36;

            for (j = 0; j < 6; j++) 
            {
                ind2 = bl.neighborHexVertex[ind][j];
                if (ind2 != -1) 
                {
                    k = s[OFS_VERTICES + ind2];
                    // production for settlement
                    if ((k >= VERTEX_HASSETTLEMENT) && (k < VERTEX_HASSETTLEMENT + NPLAYERS)) {
                        pl = k - VERTEX_HASSETTLEMENT;
                        prodPerTurn[pl].res[bl.hextiles[ind].yields()] += rate;
                    }
                    // production for city
                    if ((k >= VERTEX_HASCITY) && (k < VERTEX_HASCITY + NPLAYERS)) {
                        pl = k - VERTEX_HASCITY;
                        prodPerTurn[pl].res[bl.hextiles[ind].yields()] += 2 * rate;
                    }
                }
            }
        }
        for(pl=0; pl<NPLAYERS; pl++)
        {
            stock[pl] = new Resource();
            totalProdPerTurn[pl] = 0;
            
            for (i=0; i<NRESOURCES; i++)
            {
                stock[pl].res[i] = s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i];
                exchRatio[pl][i] = 4;
                if (s[OFS_PLAYERDATA[pl]+OFS_ACCESSTOPORT + PORT_MISC] != 0)
                    exchRatio[pl][i] = 3;
                if (s[OFS_PLAYERDATA[pl]+OFS_ACCESSTOPORT + i] != 0)
                    exchRatio[pl][i] = 2;
                totalProdPerTurn[pl] += prodPerTurn[pl].res[i];
            }
        }
    }

//    public ModSOCPlayerNumbers getNumbers(int[] s)
//    {
//        ourNumbers.recalcNumbers(bl, position, s);
//        return ourNumbers;
//    }
}

/**
 * 
 * Stores a set of resources, and implements operations on them
 * resource quantities are real numbers, not integers, because 
 * this structure also holds production ratios.
 */
class Resource implements HexTypeConstants
{
    double[] res;
    
    public Resource(double sh, double wd, double cl, double wh, double st)
    {
        res = new double[5];
        res[RES_SHEEP] = sh;
        res[RES_WOOD]  = wd;
        res[RES_CLAY]  = cl;
        res[RES_WHEAT] = wh;
        res[RES_STONE] = st;
    }

    public Resource(double[] res)
    {
        this.res = new double[5];
        for (int i=0; i<5; i++)
        {
            this.res[i] = res[i];
        }
    }
    
    public Resource()
    {
        res = new double[5];
        for (int i=0; i<5; i++)
        {
            res[i] = 0;
        }
    }

    static Resource Mult(Resource r, double factor)
    {
        Resource rr = new Resource(r.res);
        for (int i=0; i<5; i++)
        {
            rr.res[i] *= factor;
        }        
        return rr;
    }
    
    static Resource Add(Resource r1, Resource r2)
    {
        Resource rr = new Resource(r1.res);
        for (int i=0; i<5; i++)
        {
            rr.res[i] += r2.res[i];
        }        
        return rr;        
    }
    
    static boolean Contains(Resource r1, Resource r2)
    {
        for (int i=0; i<5; i++)
        {
            if (r1.res[i]<r2.res[i])
                return false;
        }        
        return true;
    }
    
    static boolean IsEmpty(Resource r1)
    {
        for (int i=0; i<5; i++)
        {
            if (r1.res[i]>0)
                return false;
        }        
        return true;        
    }
    
    final static Resource ResRoad =  new Resource(0,1,1,0,0);
    final static Resource ResStlmt = new Resource(1,1,1,1,0);
    final static Resource ResCity =  new Resource(0,0,0,2,3);
    final static Resource ResCard =  new Resource(1,0,0,1,1);
    
    
}

