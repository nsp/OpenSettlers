/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.player;

import java.util.Random;
import smartsettlers.boardlayout.*;
import smartsettlers.util.*;

/**
 *
 * @author szityu
 */
public abstract class Player implements GameStateConstants
{
    int position;
    int type;
    
    //int lastvertex; // last vertex where a settlement has been placed. 
                    // needed for initial road placement
    
    
    BoardLayout bl;
    Random rnd;
    
    public Player(BoardLayout bl, int position)
    {
        this.bl = bl;
        this.position = position;
        rnd = new Random();
    }
    
    public void listMonopolyPossibilities(int []s)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int i;
        if (s[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD] != 0)
            return;
        if (s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_MONOPOLY] >= 1)
        {
            for (i=0; i<NRESOURCES; i++)
                bl.possibilities.addAction(1.0,A_PLAYCARD_MONOPOLY, i);                    
//                bl.possibilities.addAction(1000.0,A_PLAYCARD_MONOPOLY, i);                    
        }
        
    }
    
    public void listInitSettlementPossibilities(int[] s) 
    {
       int i;

       for (i=0; i<N_VERTICES; i++)
        {
            if (s[OFS_VERTICES+i]==0)
            {
                bl.possibilities.addAction(1.0,A_BUILDSETTLEMENT, i);
            }
        }

    }
    
    public void listInitRoadPossibilities(int[] s) 
    {
        int i, ind;
        int lastvertex = s[OFS_LASTVERTEX];
        for (i=0; i<6; i++)
        {
            ind = bl.neighborVertexEdge[lastvertex][i];
            if ((ind != -1) && (s[OFS_EDGES+ind]==0))
            {
                bl.possibilities.addAction(1.0,A_BUILDROAD, ind);
            }
        }        
    }
    
    public void listNormalPossibilities(int[] s) 
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int fsmstate    = s[OFS_FSMSTATE+fsmlevel];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int i, j, ind, pl2, val;
        boolean hasneighbor;
        
        bl.possibilities.addAction(1.0,A_ENDTURN);
//        bl.possibilities.addAction(10.0,A_ENDTURN);

        // buy road
        // TODO: only 15 roads can be built
        if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WOOD] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_CLAY] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_NROADS] < 15) )
        {
            listRoadPossibilities(s);
            
//            for (i=0; i<N_EDGES; i++)
//            {
//                if (s[OFS_EDGES+i]==EDGE_EMPTY)
//                {
//                    hasneighbor = false;
//                    for (j=0; j<6; j++)
//                    {
//                        ind = bl.neighborEdgeEdge[i][j];
//                        if ((ind!=-1) && (s[OFS_EDGES+ind]==EDGE_OCCUPIED+pl))
//                        {
//                            hasneighbor = true;
//                        }
//                    }
//                    if (hasneighbor)
//                        bl.possibilities.addAction(A_BUILDROAD,i);                                
//                }
//            }
        }

        // buy settlement
        // TODO: only 5 settlements can be built
        if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WOOD] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_CLAY] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WHEAT] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_SHEEP] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_NSETTLEMENTS] <= 5) )
        {
            for (i=0; i<N_VERTICES; i++)
            {
                if (s[OFS_VERTICES+i]==VERTEX_EMPTY) 
                {
                    hasneighbor = false;
                    for (j=0; j<6; j++)
                    {
                        ind = bl.neighborVertexEdge[i][j];
                        if ((ind!=-1) && (s[OFS_EDGES+ind]==EDGE_OCCUPIED+pl))
                            hasneighbor = true;
                    }
                    if (hasneighbor)
                        bl.possibilities.addAction(1.0,A_BUILDSETTLEMENT, i);
//                        bl.possibilities.addAction(10000.0,A_BUILDSETTLEMENT, i);
                }
            }
        }

        // buy city
        if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_STONE] >= 3) &&
               (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WHEAT] >= 2) &&
               (s[OFS_PLAYERDATA[pl]+OFS_NCITIES] <= 4) )
        {
            for (i=0; i<N_VERTICES; i++)
            {
                if (s[OFS_VERTICES+i]==VERTEX_HASSETTLEMENT + pl) 
                {
                    bl.possibilities.addAction(1.0,A_BUILDCITY, i);
//                    bl.possibilities.addAction(10000.0,A_BUILDCITY, i);
                }
            }
        }

        // buy devcard
        if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_STONE] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WHEAT] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_SHEEP] >= 1) &&
               s[OFS_NCARDSGONE] < NCARDS  )
        {
            bl.possibilities.addAction(1.0,A_BUYCARD, bl.cardSequence[s[OFS_NCARDSGONE]]);
            // the type of next card is added. It is added only for logging,
            // may not be peeked by the player
        }


        listDevCardPossibilities(s);

        double w = 10.0;
        
        // trade with ports or bank
        for (i=0; i<NRESOURCES; i++)
        {
            for (j = 0; j<NRESOURCES; j++)
            {
                if (i==j) continue;
//                if (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+j]==0)
//                    w = 100.0;
//                else
//                    w = 10.0;
                w = 1.0;
                //double weight = Math.pow(10, nresources-4);
                // specific port
                if (    (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i] >= 2) &&
                        (s[OFS_PLAYERDATA[pl]+OFS_ACCESSTOPORT+i] == 1) )
                    bl.possibilities.addAction(w,A_PORTTRADE, 2, i, 1, j);
                // misc port
                else if (    (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i] >= 3) &&
                        (s[OFS_PLAYERDATA[pl]+OFS_ACCESSTOPORT+NRESOURCES] == 1) )
                    bl.possibilities.addAction(w,A_PORTTRADE, 3, i, 1, j);                        
                // bank
                else if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i] >= 4) )
                    bl.possibilities.addAction(w,A_PORTTRADE, 4, i, 1, j);
            }
        }
    }
    
    public void listDevCardPossibilities(int []s)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int i, j, ind, pl2, val;

        // play devcards
        if (s[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD] != 0)
            return;
        if (s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREERESOURCE] >= 1)
        {
            for (i=0; i<NRESOURCES; i++)
                for (j=i; j<NRESOURCES; j++)
//                    bl.possibilities.addAction(100.0,A_PLAYCARD_FREERESOURCE, i, j);
                    bl.possibilities.addAction(1.0,A_PLAYCARD_FREERESOURCE, i, j);
        }

        listMonopolyPossibilities(s);
        
        if ((s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREEROAD] >= 1) &&
               (s[OFS_PLAYERDATA[pl]+OFS_NROADS] < 15-1) )                
        {
//            bl.possibilities.addAction(100.0,A_PLAYCARD_FREEROAD);                    
            bl.possibilities.addAction(1.0,A_PLAYCARD_FREEROAD);                    
        }

        if (s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_KNIGHT] >= 1)
        {
            listRobberPossibilities(s, A_PLAYCARD_KNIGHT);
        }
    }

    public void listRoadPossibilities(int[] s)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int fsmstate    = s[OFS_FSMSTATE+fsmlevel];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int i, j, ind;
        boolean hasneighbor;
        double ratio = ((double)s[OFS_PLAYERDATA[pl]+OFS_NROADS])/(s[OFS_PLAYERDATA[pl]+OFS_NSETTLEMENTS]+s[OFS_PLAYERDATA[pl]+OFS_NCITIES]);
        double weight = Math.pow(10, -ratio+1);
        weight = 1.0;

            for (i=0; i<N_VERTICES; i++)
            {
                
                if ((s[OFS_VERTICES+i] == VERTEX_EMPTY) || 
                        (s[OFS_VERTICES+i] == VERTEX_TOOCLOSE) ||
                        (s[OFS_VERTICES+i] == VERTEX_HASSETTLEMENT+pl) ||
                        (s[OFS_VERTICES+i] == VERTEX_HASCITY+pl))
                {
                    hasneighbor = false;
                    for (j=0; j<6; j++)
                    {
                        ind = bl.neighborVertexEdge[i][j];
                        if ((ind!=-1) && (s[OFS_EDGES+ind]==EDGE_OCCUPIED+pl))
                        {
                            hasneighbor = true;
                        }
                    }
                    if (hasneighbor)
                    {
                        for (j=0; j<6; j++)
                        {
                            ind = bl.neighborVertexEdge[i][j];
                            if ((ind!=-1) && (s[OFS_EDGES+ind]==EDGE_EMPTY))
                                bl.possibilities.addAction(weight,A_BUILDROAD,ind);                                
                        }
                    }
                }
            }
//        for (i=0; i<N_EDGES; i++)
//        {
//            if (s[OFS_EDGES+i]==EDGE_EMPTY)
//            {
//                hasneighbor = false;
//                for (j=0; j<6; j++)
//                {
//                    ind = bl.neighborEdgeEdge[i][j];
//                    if ((ind!=-1) && (s[OFS_EDGES+ind]==EDGE_OCCUPIED+pl))
//                        hasneighbor = true;
//                }
//                if (hasneighbor)
//                    bl.possibilities.addAction(A_BUILDROAD,i);                                
//            }
//        }
        
    }
    
    public void listRobberPossibilities(int[] s, int action)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int fsmstate    = s[OFS_FSMSTATE+fsmlevel];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int i, j, ind, pl2, val;
        boolean hasneighbor;

        for (i=0; i<N_HEXES; i++)
        {
            if (bl.hextiles[i].type != TYPE_LAND)
                continue;
            if (i == s[OFS_ROBBERPLACE])
                continue;
            //!!!!! bl.possibilities.addAction(A_PLACEROBBER, i, -1);
            // TODO: if opponent player has multiple buildings, multipe actions are added
            for (j=0; j<6; j++)
            {
                ind = bl.neighborHexVertex[i][j];
                if (ind==-1)
                    continue;
                val = s[OFS_VERTICES + ind];
                if ((val >= VERTEX_HASSETTLEMENT) && (val< VERTEX_HASSETTLEMENT + NPLAYERS))
                    pl2 = val - VERTEX_HASSETTLEMENT;
                else if ((val >= VERTEX_HASCITY) && (val< VERTEX_HASCITY + NPLAYERS))
                    pl2 = val - VERTEX_HASCITY;
                else
                    pl2 = -1;
                if ((pl2!=-1) && (pl2!=pl))
                    bl.possibilities.addAction(1.0,action, i, pl2, selectRandomResourceInHand(pl2, s));                                
            }
        }
        
    }
    
    public void listPossibilities(int[] s)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int fsmstate    = s[OFS_FSMSTATE+fsmlevel];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int i, j, ind, pl2, val;
        boolean hasneighbor;

        bl.possibilities.Clear();
        switch (fsmstate)
        {
            case S_SETTLEMENT1:
            case S_SETTLEMENT2:
                listInitSettlementPossibilities(s);
                break;
            case S_ROAD1:
            case S_ROAD2:
                listInitRoadPossibilities(s);
                break;
            case S_BEFOREDICE:
                bl.possibilities.addAction(1.0,A_THROWDICE);                
                //listDevCardPossibilities(s);
                if ((s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_KNIGHT] >= 1)
                        && (s[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD]==0))
                {
                    listRobberPossibilities(s, A_PLAYCARD_KNIGHT);
                }                
                break;
            case S_FREEROAD1:
            case S_FREEROAD2:
                listRoadPossibilities(s);
                break;
            case S_PAYTAX:
                val = 0;
                for (i=0; i<NRESOURCES; i++)
                    val += s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + i];
                if (val>7)
                    val = val/2;
                else
                    val = 0;
                bl.possibilities.addAction(1.0,A_PAYTAX,val);
                break;
            case S_ROBBERAT7:
                listRobberPossibilities(s, A_PLACEROBBER);
                break;
            case S_NORMAL:
                listNormalPossibilities(s);
                break;
        }
        if (bl.possibilities.n==0)
        {
            bl.possibilities.addAction(1.0,A_NOTHING);
        }
       
    }

    public abstract void selectAction(int[] s, int [] a);

    
//    public void PlaceSettlement(int[] s)
//    {
//        int i; 
//        
//        // possibilities are already listed...
//        i = bl.possibilities.RandomInd();
//        performAction(s, bl.possibilities.action[i], bl.possibilities.par1[i], bl.possibilities.par2[i]);
//    }
//
//    public void PlaceRoad(int[] s)
//    {
//        // todo: place road
//        // s has information whether first, second or general
//    }
    
    public int selectRandomResourceInHand(int pl, int[] s)
    {
        int i, ind, j;
        int ncards = 0;
        for (i=0; i<NRESOURCES; i++)
            ncards += s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + i];
        if (ncards == 0)
            return -1;
        if (ncards<= 0)
        {
            String str = "";
            System.out.printf("Player %d has %d cards\n", pl, ncards);
            for (i=0; i<s.length; i++)
                str = str + " " + s[i];
            System.out.flush();
        }
        ind = rnd.nextInt(ncards)+1;
        j = 0;
        for (i=0; i<NRESOURCES; i++)
        {
            j += s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + i];
            if (j>=ind)
                break;
        }
        return Math.min(i, NRESOURCES-1);
    }
    
    public abstract int selectMostUselessResourceInHand(int pl, int []s);
    
    public void performAction(int[] s, int [] a)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int fsmstate    = s[OFS_FSMSTATE+fsmlevel];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int i, j, ind, val, ind2, k, ncards;
        
        switch (a[0])
        {
            case A_BUILDSETTLEMENT: 
                s[OFS_VERTICES+a[1]] =  VERTEX_HASSETTLEMENT+ pl;
                s[OFS_PLAYERDATA[pl]+OFS_NSETTLEMENTS]++;
                s[OFS_LASTVERTEX] = a[1];
                boolean[] hasOpponentRoad = new boolean[NPLAYERS];
                for (j=0; j<6; j++)
                {
                    ind = bl.neighborVertexVertex[a[1]][j];
                    if (ind != -1)
                    {
                        s[OFS_VERTICES+ind] = VERTEX_TOOCLOSE;
                    }
                    ind = bl.neighborVertexEdge[a[1]][j];
                    if ((ind != -1) && (s[OFS_EDGES+ind] != EDGE_EMPTY))
                    {
                        hasOpponentRoad[s[OFS_EDGES+ind]-EDGE_OCCUPIED] = true;
                    }
                }
                hasOpponentRoad[pl] = false;
                for (j=0; j<6; j++)
                {
                    ind = bl.neighborVertexHex[a[1]][j];
                    if ((ind != -1) && (bl.hextiles[ind].type == TYPE_PORT))
                    {
                        val = bl.hextiles[ind].subtype - PORT_SHEEP;
                        k = j-2; if (k<0) k+=6;
                        if (k==bl.hextiles[ind].orientation)
                            s[OFS_PLAYERDATA[pl] + OFS_ACCESSTOPORT + val] = 1;
                        k = j-3; if (k<0) k+=6;
                        if (k==bl.hextiles[ind].orientation)
                            s[OFS_PLAYERDATA[pl] + OFS_ACCESSTOPORT + val] = 1;
                    }
                }
                for (int pl2=0; pl2<NPLAYERS; pl2++)
                {
                    if (hasOpponentRoad[pl2])
                        bl.recalcLongestRoad(s,pl2);
                }
                if (fsmstate == S_SETTLEMENT2)
                {
                    int resource;
                    for (j=0; j<6; j++)
                    {
                        ind = bl.neighborVertexHex[a[1]][j];
                        if (ind !=-1)
                        {
                            resource = bl.hextiles[ind].yields();
                            if (resource != -1)
                            {
                                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + resource]++;
                            }
                        }
                    }
                }
                else if (fsmstate == S_NORMAL)
                {
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_WOOD]--;
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_CLAY]--;                    
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_WHEAT]--;
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_SHEEP]--;                    
                }

                break;
            case A_BUILDCITY: 
                s[OFS_VERTICES+a[1]] =  VERTEX_HASCITY+ pl;
                s[OFS_PLAYERDATA[pl]+OFS_NSETTLEMENTS]--;
                s[OFS_PLAYERDATA[pl]+OFS_NCITIES]++;

                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_STONE]-= 3;
                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_WHEAT]-= 2;
                break;
            case A_BUILDROAD:
                s[OFS_EDGES + a[1]] = EDGE_OCCUPIED + pl;
                s[OFS_PLAYERDATA[pl]+OFS_NROADS]++;
                if (fsmstate == S_NORMAL)
                {
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_WOOD]--;
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_CLAY]--;                    
                }
                bl.recalcLongestRoad(s,pl);
                break;
            case A_THROWDICE:
                s[OFS_DIE1] = rnd.nextInt(6)+1;
                s[OFS_DIE2] = rnd.nextInt(6)+1;
                val = s[OFS_DIE1] + s[OFS_DIE2];
                for (ind=0; ind<N_HEXES; ind++)
                {
                    if ( (val == bl.hextiles[ind].productionNumber)
                            && (s[OFS_ROBBERPLACE]!=ind) )
                    {
                        for (j = 0; j<6; j++)
                        {
                            ind2 = bl.neighborHexVertex[ind][j];
                            if (ind2 != -1)
                            {
                                k = s[OFS_VERTICES + ind2];
                                // production for settlement
                                if ((k>=VERTEX_HASSETTLEMENT) && (k<VERTEX_HASSETTLEMENT+NPLAYERS))
                                {
                                    pl = k-VERTEX_HASSETTLEMENT;
                                    s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+bl.hextiles[ind].yields()] ++;
                                }
                                // production for city
                                if ((k>=VERTEX_HASCITY) && (k<VERTEX_HASCITY+NPLAYERS))
                                {
                                    pl = k-VERTEX_HASCITY;
                                    s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+bl.hextiles[ind].yields()] += 2;
                                }
                          }
                        }
                    }
                }
                break;
            case A_PORTTRADE:
                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + a[2]] -= a[1];
                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + a[4]] += a[3];
                break;
            case A_BUYCARD:
                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_WHEAT]--;
                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_SHEEP]--;                    
                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_STONE]--;
                val = bl.cardSequence[s[OFS_NCARDSGONE]];
                if (val==CARD_ONEPOINT)
                    s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + val]++;
                else
                    s[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + val]++;
                s[OFS_NCARDSGONE] ++;
                break;
            case A_PLAYCARD_FREERESOURCE:
                s[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD] = 1;
                s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREERESOURCE]--;
                s[OFS_PLAYERDATA[pl] + OFS_USEDCARDS + CARD_FREERESOURCE]++;
                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + a[1]] ++;
                s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + a[2]] ++;
                break;
            case A_PLAYCARD_MONOPOLY:
                s[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD] = 1;
                s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_MONOPOLY]--;
                s[OFS_PLAYERDATA[pl] + OFS_USEDCARDS + CARD_MONOPOLY]++;
                for (ind = 0; ind<NPLAYERS; ind++)
                {
                    if (ind==pl)
                        continue;
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + a[1]] += s[OFS_PLAYERDATA[ind] + OFS_RESOURCES + a[1]];                    
                    s[OFS_PLAYERDATA[ind] + OFS_RESOURCES + a[1]] = 0;
                }
                break;
            case A_PLAYCARD_FREEROAD:
                s[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD] = 1;
                s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREEROAD]--;
                s[OFS_PLAYERDATA[pl] + OFS_USEDCARDS + CARD_FREEROAD]++;
                break;
            case A_PLAYCARD_KNIGHT:
                s[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD] = 1;
                s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_KNIGHT]--;
                s[OFS_PLAYERDATA[pl] + OFS_USEDCARDS + CARD_KNIGHT]++;
                bl.recalcLargestArmy(s);
            // flow to next case! 
            case A_PLACEROBBER:
                s[OFS_ROBBERPLACE] = a[1];
                if ((a[2]!=-1) && a[3]!=-1)
                {
                    s[OFS_PLAYERDATA[a[2]] + OFS_RESOURCES + a[3]]--;
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + a[3]]++;
                }
                break;
            case A_PAYTAX:
                for (i=0; i<a[1]; i++)
                {
                    ind = selectMostUselessResourceInHand(pl, s);
                    s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + ind]--;
                }
                break;
            case A_ENDTURN:
                // new cards become old cards
                for (ind=0; ind<NCARDTYPES; ind++)
                {
                    s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + ind] += s[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + ind];
                    s[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + ind] = 0;
                }
                s[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD] = 0;
                break;
                
        }
    }

//    private void listDevCardPossibilities(int []s)
//    {
//        int fsmlevel    = s[OFS_FSMLEVEL];
//        int pl          = s[OFS_FSMPLAYER+fsmlevel];
//        int i, j, ind, pl2, val;
//
//        // play devcards
//        if (s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREERESOURCE] >= 1)
//        {
//            i = rnd.nextInt(NRESOURCES);
//            j = rnd.nextInt(NRESOURCES);
//            bl.possibilities.addAction(10.0,A_PLAYCARD_FREERESOURCE, i, j);
//        }
//
//        if (s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_MONOPOLY] >= 1)
//        {
//            i = rnd.nextInt(NRESOURCES);
//            bl.possibilities.addAction(10.0,A_PLAYCARD_MONOPOLY, i);                    
//        }
//
//        if (s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREEROAD] >= 1)
//        {
//            bl.possibilities.addAction(10.0,A_PLAYCARD_FREEROAD);                    
//        }
//
//        if (s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_KNIGHT] >= 1)
//        {
//            while (true)
//            {
//                i = rnd.nextInt(N_HEXES);
//                if (bl.hextiles[i].type != TYPE_LAND)
//                    continue;
//                if (i == s[OFS_ROBBERPLACE])
//                    continue;
//                j = rnd.nextInt(6);
//                bl.possibilities.addAction(1.0,A_PLAYCARD_KNIGHT, i, -1);
//                ind = bl.neighborHexVertex[i][j];
//                if (ind!=-1)
//                {
//                    val = s[OFS_VERTICES + ind];
//                    if ((val >= VERTEX_HASSETTLEMENT) && (val< VERTEX_HASSETTLEMENT + NPLAYERS))
//                        pl2 = val - VERTEX_HASSETTLEMENT;
//                    else if ((val >= VERTEX_HASCITY) && (val< VERTEX_HASCITY + NPLAYERS))
//                        pl2 = val - VERTEX_HASCITY;
//                    else
//                        pl2 = -1;
//                    if ((pl2!=-1) && (pl2!=pl))
//                        bl.possibilities.addAction(10.0,A_PLAYCARD_KNIGHT, i, pl2, selectRandomResourceInHand(pl2, s));                                
//                }
//                break;
//            }
//        }
//        
//    }
//
//    public static final int MAXITER = 10;
//
//    public void listPossibilities(int[] s)
//    {
//        int fsmlevel    = s[OFS_FSMLEVEL];
//        int fsmstate    = s[OFS_FSMSTATE+fsmlevel];
//        int pl          = s[OFS_FSMPLAYER+fsmlevel];
//        int i, j, ind, pl2, val;
//        boolean hasneighbor;
//        int iter;
//
//        bl.possibilities.Clear();
//        switch (fsmstate)
//        {
//            case S_SETTLEMENT1:
//            case S_SETTLEMENT2:
//                while (true)
//                {
//                    i = rnd.nextInt(N_VERTICES);
//                    if (s[OFS_VERTICES+i]==0)
//                    {
//                        bl.possibilities.addAction(1.0,A_BUILDSETTLEMENT, i);
//                        break;
//                    }
//                }
//                break;
//            case S_ROAD1:
//            case S_ROAD2:
//                while (true)
//                {
//                    i = rnd.nextInt(6);
//                    ind = bl.neighborVertexEdge[lastvertex][i];
//                    if ((ind != -1) && (s[OFS_EDGES+ind]==0))
//                    {
//                        bl.possibilities.addAction(1.0,A_BUILDROAD, ind);
//                        break;
//                    }
//                }
//                break;
//            case S_BEFOREDICE:
//                bl.possibilities.addAction(10.0,A_THROWDICE);                
//                //TODO: play cards
//                //!!! listDevCardPossibilities(s);
//                break;
//            case S_FREEROAD1:
//            case S_FREEROAD2:
//                for (iter=0; iter<MAXITER; iter++)
//                {
//                    i = rnd.nextInt(N_EDGES);
//                    if (s[OFS_EDGES+i]==EDGE_EMPTY)
//                    {
//                        hasneighbor = false;
//                        for (j=0; j<6; j++)
//                        {
//                            ind = bl.neighborEdgeEdge[i][j];
//                            if ((ind!=-1) && (s[OFS_EDGES+ind]==EDGE_OCCUPIED+pl))
//                                hasneighbor = true;
//                        }
//                        if (hasneighbor)
//                        {
//                            bl.possibilities.addAction(1.0,A_BUILDROAD,i);                                
//                            break;
//                        }
//                    }
//                }
//                break;
//            case S_PAYTAX:
//                val = 0;
//                for (i=0; i<NRESOURCES; i++)
//                    val += s[OFS_PLAYERDATA[pl] + OFS_RESOURCES + i];
//                if (val>7)
//                    val = val/2;
//                else
//                    val = 0;
//                bl.possibilities.addAction(1.0,A_PAYTAX,val);
//                break;
//            case S_ROBBERAT7:
//                // !!! the same as for playing a knight card, with a different action name
//                while (true)
//                {
//                    i = rnd.nextInt(N_HEXES);
//                    if (bl.hextiles[i].type != TYPE_LAND)
//                        continue;
//                    if (i == s[OFS_ROBBERPLACE])
//                        continue;
//                    bl.possibilities.addAction(1.0,A_PLACEROBBER, i, -1);
//                    j = rnd.nextInt(6);
//                    ind = bl.neighborHexVertex[i][j];
//                    if (ind!=-1)
//                    {
//                        val = s[OFS_VERTICES + ind];
//                        if ((val >= VERTEX_HASSETTLEMENT) && (val< VERTEX_HASSETTLEMENT + NPLAYERS))
//                            pl2 = val - VERTEX_HASSETTLEMENT;
//                        else if ((val >= VERTEX_HASCITY) && (val< VERTEX_HASCITY + NPLAYERS))
//                            pl2 = val - VERTEX_HASCITY;
//                        else
//                            pl2 = -1;
//                        if ((pl2!=-1) && (pl2!=pl))
//                            bl.possibilities.addAction(10.0,A_PLACEROBBER, i, pl2, selectRandomResourceInHand(pl2, s));                                
//                    }
//                    break;
//                }
//                break;
//            case S_NORMAL:
//                bl.possibilities.addAction(1.0,A_ENDTURN);
//                
//                // buy road
//                // TODO: only 13 roads can be built
//                if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WOOD] >= 1) &&
//                       (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_CLAY] >= 1))
//                {
//                    
//                    for (iter=0; iter<MAXITER; iter++)
//                    {
//                        i = rnd.nextInt(N_EDGES);
//                        if (s[OFS_EDGES+i]==EDGE_EMPTY)
//                        {
//                            hasneighbor = false;
//                            for (j=0; j<6; j++)
//                            {
//                                ind = bl.neighborEdgeEdge[i][j];
//                                if ((ind!=-1) && (s[OFS_EDGES+ind]==EDGE_OCCUPIED+pl))
//                                {
//                                    hasneighbor = true;
//                                }
//                            }
//                            if (hasneighbor)
//                            {
//                                bl.possibilities.addAction(1.0,A_BUILDROAD,i);                                
//                                break;
//                            }
//                        }
//                    }
//                }
//                
//                // buy settlement
//                // TODO: only 5 settlements can be built
//                if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WOOD] >= 1) &&
//                       (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_CLAY] >= 1) &&
//                       (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WHEAT] >= 1) &&
//                       (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_SHEEP] >= 1))
//                {
//                    for (iter=0; iter<MAXITER; iter++)
//                    {
//                        i = rnd.nextInt(N_VERTICES);
//                        if (s[OFS_VERTICES+i]==VERTEX_EMPTY) 
//                        {
//                            hasneighbor = false;
//                            for (j=0; j<6; j++)
//                            {
//                                ind = bl.neighborVertexEdge[i][j];
//                                if ((ind!=-1) && (s[OFS_EDGES+ind]==EDGE_OCCUPIED+pl))
//                                    hasneighbor = true;
//                            }
//                            if (hasneighbor)
//                            {
//                                bl.possibilities.addAction(10.0,A_BUILDSETTLEMENT, i);
//                                break;
//                            }
//                        }
//                    }
//                }
//                
//                // buy city
//                if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_STONE] >= 3) &&
//                       (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WHEAT] >= 2) )
//                {
//                    for (iter=0; iter<MAXITER; iter++)
//                    {
//                        i = rnd.nextInt(N_VERTICES);
//                        if (s[OFS_VERTICES+i]==VERTEX_HASSETTLEMENT + pl) 
//                        {
//                            bl.possibilities.addAction(10.0,A_BUILDCITY, i);
//                            break;
//                        }
//                    }
//                }
//
//                // buy devcard
//                if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_STONE] >= 1) &&
//                       (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_WHEAT] >= 1) &&
//                       (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+RES_SHEEP] >= 1) &&
//                       s[OFS_NCARDSGONE] < NCARDS  )
//                {
//                    bl.possibilities.addAction(5.0,A_BUYCARD, bl.cardSequence[s[OFS_NCARDSGONE]]);
//                    // the type of next card is added. It is added only for logging,
//                    // may not be peeked by the player
//                }
//                 
//                
//                listDevCardPossibilities(s);
//                
//                // trade with ports or bank
//                for (iter=0; iter<MAXITER; iter++)
//                {
//                    i = rnd.nextInt(NRESOURCES);
//                    j = rnd.nextInt(NRESOURCES);
//                    if (i==j) continue;
//                    // specific port
//                    if (    (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i] >= 2) &&
//                            (s[OFS_PLAYERDATA[pl]+OFS_ACCESSTOPORT+i] == 1) )
//                    {
//                        bl.possibilities.addAction(1.0,A_PORTTRADE, 2, i, 1, j);
//                        break;
//                    }
//                    // misc port
//                    else if (    (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i] >= 3) &&
//                            (s[OFS_PLAYERDATA[pl]+OFS_ACCESSTOPORT+NRESOURCES] == 1) )
//                    {
//                        bl.possibilities.addAction(1.0,A_PORTTRADE, 3, i, 1, j);                        
//                        break;
//                    }
//                    // bank
//                    else if (   (s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i] >= 4) )
//                    {
//                        bl.possibilities.addAction(1.0,A_PORTTRADE, 4, i, 1, j);
//                        break;
//                    }
//                }
//                break;
//        }
//                        
//       
//    }
    
    
}
