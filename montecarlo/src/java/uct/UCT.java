/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package uct;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import smartsettlers.boardlayout.ActionList;
import smartsettlers.boardlayout.GameStateConstants;

/**
 *
 * @author szityu
 */
public class UCT implements GameStateConstants {
    
    public Hashtable tree;
    
    public static final int MAXNTRACES = 20000;
    int ntraces;
    Trace[] traceList;
    Random rnd = new Random();
    
    public UCT()
    {
        tree = new Hashtable(10000000);
        ntraces = 0;
        traceList = new Trace[MAXNTRACES];
        for (int i=0; i<MAXNTRACES; i++)
            traceList[i] = new Trace();
    }
    
    public static int getHashCode(int[] s)
    {
        int [] s2 = s.clone();
        s2[OFS_TURN] = 0;
        s2[OFS_FSMLEVEL] = 0;
        s2[OFS_DIE1] = 0;
        s2[OFS_DIE2] = 0;

        return(Arrays.hashCode(s2));
        
    }
    
    public void addState(int[] s, ActionList possibilities)
    {
        addState(s, getHashCode(s),possibilities);
    }
    
    public void addState(int[] s, int hc, ActionList possibilities)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int state       = s[OFS_FSMSTATE+fsmlevel];
        int[] vwins     = new int[possibilities.n];
        int totalvwins  = 0;
        int i;
        
        for (i=0; i<possibilities.n; i++)
        {
            switch (possibilities.action[i][0])
            {
                case A_BUILDSETTLEMENT:
                    if ((state!=S_SETTLEMENT1) && (state != S_SETTLEMENT2))
                        vwins[i] = 20;
                    break;
                case A_BUILDCITY:
                    vwins[i] = 10;
                    break;
            }
            totalvwins += vwins[i];
        }
        TreeNode val = new TreeNode(possibilities.n, pl, vwins);
        tree.put(hc, val);
        
////        updateVirtualWins
//        TreeNode node;
//        Trace tr;
//        
//        for (i=0; i<ntraces; i++)
//        {
//            tr = traceList[i];
//            node = getNode(tr.hc);
//            node.nvisits+=totalvwins;
//            node.nwins[tr.aind][pl]+=totalvwins;
//            node.nactionvisits[tr.aind]+=totalvwins;
////            node.timeStamp = timeStamp;
//            
//            tree.put(tr.hc, node);
//        }
    }
    
    public TreeNode getNode(int[] s)
    {
        int hc = getHashCode(s);
        return getNode(hc);
    }

    public TreeNode getNode(int hc)
    {
        TreeNode res = (TreeNode) tree.get(hc);
        return res;
    }
    
    public void clearTraces()
    {
        ntraces = 0;
    }
    
    public void addTrace(int hc, int pl, int aind)
    {
        if (ntraces >= MAXNTRACES)
            return;
        traceList[ntraces].set(hc, pl, aind);
        ntraces++;
    }
    
    public void update(int winner, int timeStamp)
    {
        int i, k, pl;
        TreeNode node;
        Trace tr;
        
        for (i=0; i<ntraces; i++)
        {
            tr = traceList[i];
            node = getNode(tr.hc);
            node.nvisits++;
            node.nwins[tr.aind][winner]++;
            node.nactionvisits[tr.aind]++;
            node.timeStamp = timeStamp;
            
            tree.put(tr.hc, node);
        }

        /// SLOOOW!!! 
//        Enumeration ek = tree.keys();
//        Enumeration ee = tree.elements();
//        Object keyo;
//        //TreeNode node;
//        while( ek.hasMoreElements() )
//        {
//            keyo = ek.nextElement();
//            node = (TreeNode) ee.nextElement();
//            if (node.timeStamp+5<timeStamp)
//            {
//                tree.remove(keyo);
//            }
//        }
    }
        
    public static final int MINVISITS = 10;
    public static final double C0 = 2.0;
    public static final double MAXVAL = 100.0;
    
    public int selectAction(int[] s, int pl, boolean echo)
    {
        return selectAction(getHashCode(s),pl, echo);
    }
    
    public int selectAction(int hc, int pl, boolean echo)
    {
        int k;
        double v, maxv;
        int maxind=0;
        TreeNode node = getNode(hc);
        
        maxv = 0.0;
        if (node==null) 
            return 0;
        if (node.nvisits < MINVISITS)
        {
            return rnd.nextInt(node.nactions);
        }
        for (k=0; k<node.nactions; k++)
        {
            if (node.nactionvisits[k]==0)
            {
                v = MAXVAL;
                if (echo)
                {
                    System.out.printf("%2d (%d): \t %5.2f ", k, node.nactionvisits[k], v);
                    v = 0.0;
                }
            }
            else
            {
                v = ((double)node.nwins[k][pl])/(node.nactionvisits[k]) +
                        C0*Math.sqrt(Math.log(node.nactions)/node.nactionvisits[k]);
                if (echo)
                {
                    System.out.printf("%2d (%d): \t %5.2f = %5.2f + %5.2f\n", k, node.nactionvisits[k], v, 
                            ((double)node.nwins[k][pl])/(node.nactionvisits[k]), C0*Math.sqrt(Math.log(node.nactions)/node.nactionvisits[k]));
                    v = ((double)node.nwins[k][pl])/(node.nactionvisits[k]);
                }
            }
            if (maxv<v)
            {
                maxv = v;
                maxind = k;
            }
        }
        if (echo)
            System.out.printf("sel:%d\n\n", maxind);
        return maxind;
        
    }
    
    @Override
    public String toString()
    {
        String s = getClass().getName() + ": " + tree.size() + "\n";
        int hc;
        TreeNode node;
        
        Enumeration e = tree.keys();
        while( e.hasMoreElements() )
        {
            hc = (Integer) e.nextElement();
            node = (TreeNode) tree.get(hc);
            s += String.format("%X/%d: %3d", hc, node.nactions, node.nvisits);
            for (int i=0; i<node.nactions; i++)
            {
                s+= String.format(" %d[%d,%d,%d,%d]", node.nactionvisits[i],
                        node.nwins[i][0],node.nwins[i][1],node.nwins[i][2],node.nwins[i][3]);
            }
            s+= "\n";
            
        }
        return s;
    }
}

class Trace {
    public int hc;
    public int pl; 
    public int aind;
    
    public void set(int hc, int pl, int aind)
    {
        this.hc = hc;
        this.pl = pl;
        this.aind = aind;
    }
}

