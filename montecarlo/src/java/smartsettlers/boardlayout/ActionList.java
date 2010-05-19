/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.boardlayout;

import java.util.Random;

/**
 *
 * @author szityu
 */
public class ActionList implements GameStateConstants {
    public static final int MAX_ACTIONLISTSIZE = 200;
    
    public static final int [] NOOP = {A_NOTHING, 0, 0, 0, 0};
            
    public int n;
    public int[][] action;
//    public int[] par1, par2;
    public double[] score;
    public double[] weight;
    Random rnd = new Random();
    double totalweight = 0.0;
    
    
    public ActionList()
    {
        n = 0;
        totalweight = 0.0;
        action = new int[MAX_ACTIONLISTSIZE][ACTIONSIZE];
//        par1 = new int[MAX_ACTIONLISTSIZE];
//        par2 = new int[MAX_ACTIONLISTSIZE];
        score = new double[MAX_ACTIONLISTSIZE];
        weight = new double[MAX_ACTIONLISTSIZE];
    }
    
    public void Clear()
    {
        n = 0;
        totalweight = 0.0;
    }
    
    public int uniformRandomInd()
    {
        if (n==0)
            return -1;
        else
            return rnd.nextInt(n);
    }
    
    public int[] uniformRandomAction()
    {
        if (n==0)
            return NOOP;
        else
            return action[rnd.nextInt(n)];
    }
    
    public int randomInd()
    {
        if (n==0)
            return -1;
        double wlim = rnd.nextFloat()*totalweight;
        double w = 0;
        int i;
        for (i=0; i<n; i++)
        {
            w += weight[i];
            if (w>= wlim)
            {
                break;
            }
        }
        if (i==n) i--;
        return i;
    }
    
    public int[] randomAction()
    {
        if (n==0)
            return NOOP;
        else
            return action[randomInd()];
    }

    public void addAction(int a)
    {
        weight[n] = 1.0;
        totalweight += 1.0;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        n++;
    }

    public void addAction(int a, int par1)
    {
        weight[n] = 1.0;
        totalweight += 1.0;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        action[n][1] = par1;
        n++;
    }
    
    public void addAction(int a, int par1, int par2)
    {
        weight[n] = 1.0;
        totalweight += 1.0;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        action[n][1] = par1;
        action[n][2] = par2;
        n++;
    }
    
    public void addAction(int a, int par1, int par2, int par3)
    {
        weight[n] = 1.0;
        totalweight += 1.0;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        action[n][1] = par1;
        action[n][2] = par2;
        action[n][3] = par3;
        n++;
    }    

    public void addAction(int a, int par1, int par2, int par3, int par4)
    {
        weight[n] = 1.0;
        totalweight += 1.0;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        action[n][1] = par1;
        action[n][2] = par2;
        action[n][3] = par3;
        action[n][4] = par4;
        n++;
    }    
    public void addAction(double w, int a)
    {
        weight[n] = w;
        totalweight += w;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        n++;
    }

    public void addAction(double w, int a, int par1)
    {
        weight[n] = w;
        totalweight += w;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        action[n][1] = par1;
        n++;
    }
    
    public void addAction(double w, int a, int par1, int par2)
    {
        weight[n] = w;
        totalweight += w;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        action[n][1] = par1;
        action[n][2] = par2;
        n++;
    }
    
    public void addAction(double w, int a, int par1, int par2, int par3)
    {
        weight[n] = w;
        totalweight += w;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        action[n][1] = par1;
        action[n][2] = par2;
        action[n][3] = par3;
        n++;
    }    

    public void addAction(double w, int a, int par1, int par2, int par3, int par4)
    {
        weight[n] = w;
        totalweight += w;
        //!!! a can be shorter than actionsize
        action[n][0] = a;
        action[n][1] = par1;
        action[n][2] = par2;
        action[n][3] = par3;
        action[n][4] = par4;
        n++;
    }    
}
