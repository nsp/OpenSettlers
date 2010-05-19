/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.boardlayout;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import smartsettlers.player.*;
import smartsettlers.util.*;
import uct.TreeNode;
import uct.UCT;


/**
 *
 * @author szityu
 */

public class BoardLayout implements HexTypeConstants, VectorConstants, GameStateConstants
{
    
    public static final int[][] LAND_COORD = {
        { 3, 1, -1},
        { 4, 1, -1},
        { 5, 1, -1},
        { 2, 2, -1},
        { 3, 2, -1},
        { 4, 2, -1},
        { 5, 2, -1},
        { 1, 3, -1},
        { 2, 3, -1},
        { 3, 3, -1},
        { 4, 3, -1},
        { 5, 3, -1},
        { 1, 4, -1},
        { 2, 4, -1},
        { 3, 4, -1},
        { 4, 4, -1},
        { 1, 5, -1},
        { 2, 5, -1},
        { 3, 5, -1}
    };
    public static final int[][] PORT_COORD = {
        { 3, 0, 1},
        { 5, 0, 2},
        { 6, 1, 2},
        { 6, 3, 3},
        { 4, 5, 4},
        { 2, 6, 4},
        { 0, 6, 5},
        { 0, 4, 0},
        { 1, 2, 0}
    };
    public static final int[][] SEA_COORD = {
        { 4, 0, -1},
        { 6, 0, -1},
        { 6, 2, -1},
        { 5, 4, -1},
        { 3, 6, -1},
        { 1, 6, -1},
        { 0, 5, -1},
        { 0, 3, -1},
        { 2, 1, -1}
    };
    public static final int N_LAND_TILES = LAND_COORD.length;
    public static final int N_SEA_TILES  = SEA_COORD.length;
    public static final int N_PORT_TILES = PORT_COORD.length;
    public static final int N_TILES = N_LAND_TILES + N_SEA_TILES + N_PORT_TILES;
    
    public static final int LAND_START_INDEX = 0;
    public static final int SEA_START_INDEX  = LAND_START_INDEX + N_LAND_TILES;
    public static final int PORT_START_INDEX = SEA_START_INDEX + N_SEA_TILES;
    
    public static final int MAXX = 7;
    public static final int MAXY = 7;
    
    
    public int[] landSequence = {
        LAND_SHEEP, LAND_SHEEP, LAND_SHEEP, LAND_SHEEP, 
        LAND_WHEAT, LAND_WHEAT, LAND_WHEAT, LAND_WHEAT, 
        LAND_CLAY, LAND_CLAY, LAND_CLAY, 
        LAND_WOOD, LAND_WOOD, LAND_WOOD, LAND_WOOD, 
        LAND_STONE, LAND_STONE, LAND_STONE,
        LAND_DESERT };
    
    public int[] portSequence = {
     PORT_MISC, PORT_MISC, PORT_MISC, PORT_MISC, 
     PORT_SHEEP,
     PORT_WOOD,
     PORT_CLAY,
     PORT_WHEAT,
     PORT_STONE
    };

    
    public int[] cardSequence = {       
        //14
        CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, 
        CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, CARD_KNIGHT, 
        // 5
        CARD_ONEPOINT, CARD_ONEPOINT, CARD_ONEPOINT, CARD_ONEPOINT, CARD_ONEPOINT, 
        // 2, 2, 2
        CARD_MONOPOLY, CARD_MONOPOLY,
        CARD_FREERESOURCE, CARD_FREERESOURCE,
        CARD_FREEROAD, CARD_FREEROAD
    };
    
    public int[] hexnumberSequence = {
        2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12
    };
    
    public HexTile[] hextiles;
    public Edge[] edges;
    public Vertex[] vertices;
    public int[][] hexatcoord;
    
    public int[][] neighborHexHex;
    public int[][] neighborVertexVertex;    
    public int[][] neighborHexVertex;
    public int[][] neighborHexEdge;
    public int[][] neighborVertexHex;
    public int[][] neighborVertexEdge;
    public int[][] neighborEdgeEdge;
            
    public Random rnd = new Random();

    public int screenWidth, screenHeight;
    double A[][] = {{1, 0}, {0.5, -0.86602540378443864676372317075294}};
    double offset[] = {-0.5, 6.5};
    public double scale = 20;
    
    public int[] state;
    public int[] action;
    public Player[] player;
    public GameLog gamelog;
    public boolean isLoggingOn;
    public ActionList possibilities = new ActionList();
    
    public static final int MODE_RANDOM = 0;
    public static final int MODE_UCT    = 1;
    public int mode = MODE_UCT;
    
    public UCT uctTree;
    private int uctTime = 0;
    
    public void setState(int[] s)
    {
        int i;
        for (i=0; i<STATESIZE; i++)
            state[i] = s[i];
    }
    
    public static int[] cloneOfState(int[] s)
    {
        int[] s2 = new int[STATESIZE];
        int i;
        for (i=0; i<STATESIZE; i++)
            s2[i] = s[i];
        return s2;
    }
    
    public void setBoardSize(int screenWidth, int screenHeight, double scale)
    {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        //scale = Math.min(screenHeight / 8.0, screenWidth / 8.0);
        this.scale = scale;
    }

    public BoardLayout(int screenWidth, int screenHeight)
    {
        //setBoardSize(screenWidth, screenHeight);
        //InitBoard();
    }
    
    public void ShuffleIntArray(int[] a)
    {
        int firstvalue;
        int pos1, pos2=0;
        int nSteps = 1000;
        int i;
        int N = a.length;
        
        pos1 = rnd.nextInt(N);
        firstvalue = a[pos1];
        for (i=0; i<nSteps; i++)
        {
            pos2 = rnd.nextInt(N);
            a[pos1] = a[pos2];
            pos1 = pos2;
        }
        a[pos2] = firstvalue;
    }
    
    public void InitBoard()
    {
        int i, j, k;
        HexTile t1, t2, t3;
        int ind1, ind2, ind3;

        
        // create Hex tiles, set screen coordinates,
        // place them on the coordinate system
        hextiles = new HexTile[N_TILES];
        edges = new Edge[N_EDGES];
        vertices = new Vertex[N_VERTICES];
        hexatcoord = new int[MAXX][MAXY];
        neighborHexHex = new int[N_TILES][6];
        neighborHexVertex = new int[N_TILES][6];
        neighborHexEdge = new int[N_TILES][6];
        neighborVertexHex = new int[N_VERTICES][6];
        neighborVertexVertex = new int[N_VERTICES][6]; //6 directions, but only 3 active: 0,2,4 or 1,3,5
        neighborVertexEdge = new int[N_VERTICES][6];
        neighborEdgeEdge = new int[N_EDGES][6];
        
        gamelog = new GameLog();
        state = new int[STATESIZE];
        action = new int[ACTIONSIZE];

        // <editor-fold defaultstate="collapsed" desc="init neighborhood matrices to -1">
        for (i=0; i<MAXX; i++)
            for (j=0; j<MAXY; j++)
                hexatcoord[i][j] = -1;
        for (i=0; i<N_TILES; i++)
            for (j=0; j<6; j++)
            {
                neighborHexHex[i][j] = -1;
                neighborHexVertex[i][j] = -1;
                neighborHexEdge[i][j] = -1;
            }
        for (i=0; i<N_VERTICES; i++)
            for (j=0; j<6; j++)
            {
                neighborVertexVertex[i][j] = -1;
                neighborVertexEdge[i][j] = -1;
                neighborVertexHex[i][j] = -1;
            }
        for (i=0; i<N_EDGES; i++)
            for (j=0; j<6; j++)
            {
                neighborEdgeEdge[i][j] = -1;
            }
        // </editor-fold>

        ShuffleIntArray(landSequence);
        ShuffleIntArray(portSequence);
        ShuffleIntArray(cardSequence);
        
        // <editor-fold defaultstate="collapsed" desc="fill hextiles, hexatcoord">
        for(i=0; i<N_LAND_TILES; i++)
        {
            hextiles[LAND_START_INDEX + i] = new HexTile(LAND_COORD[i][0],LAND_COORD[i][1],landSequence[i],-1);
            hexatcoord[LAND_COORD[i][0]][LAND_COORD[i][1]] = LAND_START_INDEX + i;
        }
        for(i=0; i<N_SEA_TILES; i++)
        {
            hextiles[SEA_START_INDEX + i] = new HexTile(SEA_COORD[i][0],SEA_COORD[i][1],SEA,-1);
            hexatcoord[SEA_COORD[i][0]][SEA_COORD[i][1]] = SEA_START_INDEX + i;
        }        
        for(i=0; i<N_PORT_TILES; i++)
        {
            hextiles[PORT_START_INDEX + i] = new HexTile(PORT_COORD[i][0],PORT_COORD[i][1],portSequence[i],PORT_COORD[i][2]);
            hexatcoord[PORT_COORD[i][0]][PORT_COORD[i][1]] = PORT_START_INDEX + i;
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="create neighborHexHex">
        int[][] delta =        {{ 1, 0},
                                { 0, 1},
                                {-1, 1},
                                {-1, 0},
                                { 0,-1},
                                { 1,-1}};
        int x1, y1, x2, y2;
        for (i=0; i<N_TILES; i++)
        {
            x1 = (int) (hextiles[i].pos.x);
            y1 = (int) (hextiles[i].pos.y);
            for (j=0; j<6; j++)
            {
                x2 = x1 + delta[j][0];
                y2 = y1 + delta[j][1];
                
                if ((x2>=0) && (x2<MAXX) && (y2>=0) && (y2<MAXY))
                    ind2 = hexatcoord[x2][y2];
                else
                    ind2 = -1;
                //ind1 = hexatcoord[x1][y1];
                ind1 = i;
                if ((ind1 != -1) && (ind2 != -1))
                {
                    neighborHexHex[i][j] = ind2;
                }
            }
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="set hex tile screen coordinates">
        Point p;
        Polygon hexagon;
        
        for (j = 0; j < N_TILES; j++) 
        {
            hexagon = new Polygon();
            for (i = 0; i < 6; i++) 
            {
                p = VectorToScreenCoord(hextiles[j].pos.Add(HEX_EDGES[i]));
                hexagon.addPoint(p.x, p.y);
            }
            hextiles[j].screenCoord = hexagon;
            hextiles[j].centerScreenCord = VectorToScreenCoord(hextiles[j].pos);
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="create vertices, NeighborHexVertex, neighborVertexHex">
        // create vertices
        int nvertices = 0;
        for (i=0; i<MAXX; i++)
            for (j=0; j<MAXY; j++)
            {
                ind1 = hexatcoord[i][j];
                if (i<MAXX-1 && j<MAXY-1)
                {
                    ind2 = hexatcoord[i+1][j];
                    ind3 = hexatcoord[i][j+1];
                    if ((ind1!=-1) && (ind2!=-1) && (ind3!=-1))
                    {
                        t1 = hextiles[ind1];
                        t2 = hextiles[ind2];
                        t3 = hextiles[ind3];

                        if (t1.type == TYPE_LAND || t2.type == TYPE_LAND || t3.type == TYPE_LAND)
                        {
                            vertices[nvertices] = new Vertex(t1.pos.Add(HEX_EDGES[5]));
                            vertices[nvertices].screenCoord = VectorToScreenCoord(t1.pos.Add(HEX_EDGES[0]));
                            neighborHexVertex[ind1][0] = nvertices;
                            neighborHexVertex[ind2][2] = nvertices;
                            neighborHexVertex[ind3][4] = nvertices;
                            neighborVertexHex[nvertices][3] = ind1;
                            neighborVertexHex[nvertices][5] = ind2;
                            neighborVertexHex[nvertices][1] = ind3;
                            nvertices++;
                        }
                    }
                }
                //System.out.println("nvertices = " + nvertices);
                if (i<MAXX-1 && j>0)
                {
                    ind2 = hexatcoord[i+1][j];
                    ind3 = hexatcoord[i+1][j-1];
                    if ((ind1!=-1) && (ind2!=-1) && (ind3!=-1))
                    {
                        t1 = hextiles[ind1];
                        t2 = hextiles[ind2];
                        t3 = hextiles[ind3];

                        if (t1.type == TYPE_LAND || t2.type == TYPE_LAND || t3.type == TYPE_LAND)
                        {
                            vertices[nvertices] = new Vertex(t1.pos.Add(HEX_EDGES[5]));
                            vertices[nvertices].screenCoord = VectorToScreenCoord(t1.pos.Add(HEX_EDGES[5]));
                            neighborHexVertex[ind1][5] = nvertices;
                            neighborHexVertex[ind2][3] = nvertices;
                            neighborHexVertex[ind3][1] = nvertices;
                            neighborVertexHex[nvertices][2] = ind1;
                            neighborVertexHex[nvertices][0] = ind2;
                            neighborVertexHex[nvertices][4] = ind3;
                            nvertices++;
                        }
                    }
                }
            }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="compute vertex-vertex neighborhoods, neighborVertexVertex">     
        int v1, v2;
        for (ind1 = 0; ind1<N_TILES; ind1++)
        {
            v1 = neighborHexVertex[ind1][0]; v2 = neighborHexVertex[ind1][1];
            if ((v1 != -1) && (v2 != -1))
            {
                neighborVertexVertex[v1][2] = v2;
                neighborVertexVertex[v2][5] = v1;
            }

            v1 = neighborHexVertex[ind1][1]; v2 = neighborHexVertex[ind1][2];
            if ((v1 != -1) && (v2 != -1))
            {
                neighborVertexVertex[v1][3] = v2;
                neighborVertexVertex[v2][0] = v1;
            }
            v1 = neighborHexVertex[ind1][2]; v2 = neighborHexVertex[ind1][3];
            if ((v1 != -1) && (v2 != -1))
            {
                neighborVertexVertex[v1][4] = v2;
                neighborVertexVertex[v2][1] = v1;
            }
            v1 = neighborHexVertex[ind1][3]; v2 = neighborHexVertex[ind1][4];
            if ((v1 != -1) && (v2 != -1))
            {
                neighborVertexVertex[v1][5] = v2;
                neighborVertexVertex[v2][2] = v1;
            }
            v1 = neighborHexVertex[ind1][4]; v2 = neighborHexVertex[ind1][5];
            if ((v1 != -1) && (v2 != -1))
            {
                neighborVertexVertex[v1][0] = v2;
                neighborVertexVertex[v2][3] = v1;
            }
            v1 = neighborHexVertex[ind1][5]; v2 = neighborHexVertex[ind1][0];
            if ((v1 != -1) && (v2 != -1))
            {
                neighborVertexVertex[v1][1] = v2;
                neighborVertexVertex[v2][4] = v1;
            }
            
        }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="create the edges, neighborVertexEdge, neighborHexEdge">
        //create edges
        int nedges = 0;        
        for (i=0; i<MAXX; i++)
            for (j=0; j<MAXY; j++)
            {
                ind1 = hexatcoord[i][j];
                if (i<MAXX-1)
                {
                    ind2 = hexatcoord[i+1][j];
                    if ( (ind1!=-1) && (ind2!=-1) )
                    {
                        t1 = hextiles[ind1];
                        t2 = hextiles[ind2];
                        if (t1.type == TYPE_LAND || t2.type == TYPE_LAND)
                        {
                            edges[nedges] = new Edge(t1.pos.Add(HEX_EDGES[5]),t1.pos.Add(HEX_EDGES[0]));
                            edges[nedges].screenCoord[0] = VectorToScreenCoord(t1.pos.Add(HEX_EDGES[5]));
                            edges[nedges].screenCoord[1] = VectorToScreenCoord(t1.pos.Add(HEX_EDGES[0]));
                            v1 = neighborHexVertex[ind1][5];
                            neighborVertexEdge[v1][1] = nedges;
                            v1 = neighborHexVertex[ind1][0];
                            neighborVertexEdge[v1][4] = nedges;
                            neighborHexEdge[ind1][0] = nedges;
                            neighborHexEdge[ind2][3] = nedges;
                            nedges++;
                        }
                            
                    }
                }

                if (j<MAXY-1)
                {
                    ind2 = hexatcoord[i][j+1];
                    if ( (ind1!=-1) && (ind2!=-1) )
                    {
                        t1 = hextiles[ind1];
                        t2 = hextiles[ind2];
                        if (t1.type == TYPE_LAND || t2.type == TYPE_LAND)
                        {
                            edges[nedges] = new Edge(t1.pos.Add(HEX_EDGES[0]),t1.pos.Add(HEX_EDGES[1]));
                            edges[nedges].screenCoord[0] = VectorToScreenCoord(t1.pos.Add(HEX_EDGES[0]));
                            edges[nedges].screenCoord[1] = VectorToScreenCoord(t1.pos.Add(HEX_EDGES[1]));
                            v1 = neighborHexVertex[ind1][0];
                            neighborVertexEdge[v1][2] = nedges;
                            v1 = neighborHexVertex[ind1][1];
                            neighborVertexEdge[v1][5] = nedges;
                            neighborHexEdge[ind1][1] = nedges;
                            neighborHexEdge[ind2][4] = nedges;
                            nedges++;
                        }
                    }
                }
                if (i>0 && j<MAXY-1)
                {
                    ind2 = hexatcoord[i-1][j+1];
                    if ( (ind1!=-1) && (ind2!=-1) )
                    {
                        t1 = hextiles[ind1];
                        t2 = hextiles[ind2];
                        if (t1.type == TYPE_LAND || t2.type == TYPE_LAND)
                        {
                            edges[nedges] = new Edge(t1.pos.Add(HEX_EDGES[1]),t1.pos.Add(HEX_EDGES[2]));
                            edges[nedges].screenCoord[0] = VectorToScreenCoord(t1.pos.Add(HEX_EDGES[1]));
                            edges[nedges].screenCoord[1] = VectorToScreenCoord(t1.pos.Add(HEX_EDGES[2]));
                            v1 = neighborHexVertex[ind1][1];
                            neighborVertexEdge[v1][3] = nedges;
                            v1 = neighborHexVertex[ind1][2];
                            neighborVertexEdge[v1][0] = nedges;
                            neighborHexEdge[ind1][2] = nedges;
                            neighborHexEdge[ind2][5] = nedges;
                            nedges++;
                        }
                    }
                }
                
            }
        // </editor-fold>
        
        // <editor-fold defaultstate="collapsed" desc="neighborEdgeEdge">
        for (i=0; i<N_VERTICES; i++)
        {
            for (j=0; j<6; j++)
                for (k=0; k<6; k++)
                {
                    ind1 = neighborVertexEdge[i][j];
                    ind2 = neighborVertexEdge[i][k];
                    if ((ind1!=-1) && (ind2 !=-1))
                    {
                        neighborEdgeEdge[ind1][k] = ind2;
                        neighborEdgeEdge[ind2][j] = ind1;
                    }
                }
        }
        // </editor-fold>
        
        
        InitProductionNumbers();
        NewGame(state, true); //todo: remove this.
        
        uctTree = new UCT();
//        int [] a1 = {1, 2, 3};
//        int [] a2 = {1, 2, 3};
//        int [] a3 = {1, 1, 1, 1};
        
//        tree.addState(a1,10);
//        System.out.printf("UCT TEST %d  %d   %d \n", tree.getValue(a1), tree.getValue(a2), tree.getValue(a3));
    }
 
    void InitProductionNumbers()
    {
        boolean goodarrangement = false;
        int x, y, k, ind, ind2;
        int seqind;
        
        while (!goodarrangement)
        {
            
        
            ShuffleIntArray(hexnumberSequence);
            seqind = 0;

            // deal numberts to hexes
            for (x=0; x<MAXX; x++)
                for (y=0; y<MAXY; y++)
                {
                    ind = hexatcoord[x][y];
                    if ((ind != -1) && hextiles[ind].type == TYPE_LAND && hextiles[ind].subtype != LAND_DESERT)
                    {
                        hextiles[ind].productionNumber = hexnumberSequence[seqind];
                        seqind++;
                    }
                }
            
            // check if the arrangement is good
            // it is good if no red numbers (6 or 8) are besides each other
            goodarrangement = true;
            outerloop:
            for (x=0; x<MAXX; x++)
                for (y=0; y<MAXY; y++)
                {
                    ind = hexatcoord[x][y];
                    for (k=0; k<6; k++)
                    {
                        if (ind != -1) 
                        {
                            ind2 = neighborHexHex[ind][k];
                            if ((ind2 != -1) 
                                    && (hextiles[ind].productionNumber==6 || hextiles[ind].productionNumber == 8) 
                                    && (hextiles[ind2].productionNumber==6 || hextiles[ind2].productionNumber == 8) )
                            {
                                goodarrangement = false;
                                break outerloop;
                            }
                        }
                    }
                }
            
        }
    }
    
    public Point VectorToScreenCoord(Vector2d v)
    {
        Point p = new Point();
        
        p.setLocation(
                scale*(A[0][0]*v.x + A[1][0]*v.y + offset[0])   , 
                scale*(A[0][1]*v.x + A[1][1]*v.y + offset[1])   );
        return p;
    }

    public void NewGame(int[] s, boolean isLoggingOn)
    {
        int pl, i;
        
        
        player = new Player[NPLAYERS];
        for (pl=0; pl<NPLAYERS; pl++)
        {
            player[pl] = new UctPlayer(this, pl);
//            player[pl] = new RandomPlayer(this, pl);
        }
        
        s = new int[STATESIZE];
        s[OFS_FSMLEVEL] = 0;
        s[OFS_FSMSTATE+0] = S_GAME;
        s[OFS_FSMPLAYER+0] = 0;
        s[OFS_LARGESTARMY_AT] = -1;
        s[OFS_LONGESTROAD_AT] = -1;
        for (i=0; i<N_HEXES; i++)
        {
            if (hextiles[i].subtype == LAND_DESERT)
            {
                s[OFS_ROBBERPLACE] = i;
                break;
            }
        }
        int[] a = new int[ACTIONSIZE];
        GameTick(s, a);
        setState(s);
        this.isLoggingOn = isLoggingOn;
       
        if (isLoggingOn)
        {
            gamelog.clear();
            gamelog.addState(s);
        }
    }
    
    public void GameTick(int[] s, int [] a)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        
        // doing stuff
        player[pl].listPossibilities(s);
        player[pl].selectAction(s,a);
        if (isLoggingOn)
            gamelog.addAction(a);
        
        player[pl].performAction(s, a);

        stateTransition(s, a);
    }
    
    public static void printArray(int[] s)
    {
        for (int i=0; i<s.length; i++)
            System.out.print(s[i]+" ");
        System.out.println();
    }
    
    public void GameTickUCT(int[] s, int [] a)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        
        if (pl == 0)
        {
            int[] s2 = cloneOfState(s);    
            UCTsimulateGame(s2);
            s2=null;
            player[pl].listPossibilities(s);
            int aind = uctTree.selectAction(s, pl, true);
            int i;
            for (i=0; i<a.length; i++)
                a[i] = possibilities.action[aind][i];
        }
        else
        {
            player[pl].listPossibilities(s);
            player[pl].selectAction(s,a); 
        }
        if (isLoggingOn)
            gamelog.addAction(a);
        
        player[pl].performAction(s, a);

        stateTransition(s, a);
    }

    public void stateTransition(int[] s, int[] a)
    {
        int fsmlevel    = s[OFS_FSMLEVEL];
        int fsmstate    = s[OFS_FSMSTATE+fsmlevel];
        int pl          = s[OFS_FSMPLAYER+fsmlevel];
        int motherstate;
        if (fsmlevel>0) 
            motherstate = s[OFS_FSMSTATE+fsmlevel-1]; 
        else
            motherstate = -1;
        
        switch (fsmstate)
        {
            case S_GAME:
                fsmlevel++;  s[OFS_FSMLEVEL] = fsmlevel;
                s[OFS_FSMSTATE+fsmlevel] = S_SETTLEMENT1;
                break;
            case S_SETTLEMENT1:
                s[OFS_FSMSTATE+fsmlevel] = S_ROAD1;
                break;
            case S_ROAD1:
                if (pl==NPLAYERS-1)
                {
                    pl = NPLAYERS-1;
                    s[OFS_FSMSTATE+fsmlevel] = S_SETTLEMENT2;
                }
                else
                {
                    pl++;
                    s[OFS_FSMPLAYER+fsmlevel] = pl;
                    s[OFS_FSMSTATE+fsmlevel] = S_SETTLEMENT1;
                }
                break;
            case S_SETTLEMENT2:
                s[OFS_FSMSTATE+fsmlevel] = S_ROAD2;
                break;
            case S_ROAD2:
                if (pl==0)
                {
                    pl = 0;
                    s[OFS_FSMSTATE+fsmlevel] = S_BEFOREDICE;
                }
                else
                {
                    pl--;
                    s[OFS_FSMPLAYER+fsmlevel] = pl;
                    s[OFS_FSMSTATE+fsmlevel] = S_SETTLEMENT2;
                }
                break;
            case S_BEFOREDICE:
                if ((a[0]==A_THROWDICE) && (s[OFS_DIE1]+s[OFS_DIE2] != 7) )
                {
                    s[OFS_FSMSTATE+fsmlevel] = S_NORMAL;
                }
                else if ((a[0]==A_THROWDICE) && (s[OFS_DIE1]+s[OFS_DIE2] == 7) )
                {
                    //TODO: place robber, pay tax
                    // s[OFS_FSMSTATE+fsmlevel] = S_NORMAL;
                    fsmlevel++;  s[OFS_FSMLEVEL] = fsmlevel;
                    s[OFS_FSMSTATE + fsmlevel] = S_PAYTAX;
                    s[OFS_FSMPLAYER + fsmlevel] = 0;                    
                }
                break;
            case S_PAYTAX:
                pl++;
                if (pl<NPLAYERS)
                {
                    s[OFS_FSMPLAYER + fsmlevel] = pl;
                    s[OFS_FSMSTATE + fsmlevel] = S_PAYTAX;                    
                }
                else
                {
                    s[OFS_FSMPLAYER + fsmlevel] = s[OFS_FSMPLAYER + fsmlevel-1];
                    s[OFS_FSMSTATE + fsmlevel] = S_ROBBERAT7;
                }
                break;
            case S_ROBBERAT7:
                fsmlevel--;  s[OFS_FSMLEVEL] = fsmlevel;
                s[OFS_FSMSTATE + fsmlevel] = S_NORMAL;
                break;
            case S_NORMAL:
                switch (a[0])
                {
                    case A_ENDTURN:
                        pl++;
                        if (pl>=NPLAYERS) pl=0; 
                        s[OFS_FSMPLAYER+fsmlevel] = pl;
                        s[OFS_FSMSTATE+fsmlevel] = S_BEFOREDICE;                        
                        break;
                    case A_PLAYCARD_FREEROAD:
                        s[OFS_FSMSTATE+fsmlevel] = S_FREEROAD1;                        
                        break;
                }
                break;
            case S_FREEROAD1:
                    s[OFS_FSMSTATE+fsmlevel] = S_FREEROAD2;                 
                break;
            case S_FREEROAD2:
                    s[OFS_FSMSTATE+fsmlevel] = S_NORMAL;                 
                break;
                    
        }
        recalcScores(s);
        if (getWinner(s) != -1)
        {
            s[OFS_FSMSTATE+fsmlevel] = S_FINISHED;
        }
        
        if (isLoggingOn)
            gamelog.addState(s);
        
        
    }
    
    public void UCTsimulateGame(int[] s2)
    {
        int[] s = null;    
        int[] a = new int[ACTIONSIZE];

        TreeNode node;
        boolean isKnownState = true;
        int winner;
        int it;
        
        boolean oldIsLoggingOn = isLoggingOn;
        isLoggingOn = false;
        uctTime ++;
        
        if (uctTree.tree.size()>10000)
            uctTree.tree.clear();
        
        int fsmlevel    = s2[OFS_FSMLEVEL];
        int pl          = s2[OFS_FSMPLAYER+fsmlevel];
//        System.out.printf("!1");
        player[pl].listPossibilities(s2);
//        System.out.printf("!2");
        int N_IT = 1000;
        if (possibilities.n == 1)
            N_IT = 1;
        for(it=0; it<N_IT; it++)
        {
//            if (it%10 == 0)
//                System.out.printf(".");
            isKnownState = true;
            s = cloneOfState(s2);
            uctTree.clearTraces();
            while (true)
            {
                int hc = UCT.getHashCode(s);
                node = uctTree.getNode(hc);
                //System.out.print(node+" ");
                fsmlevel    = s[OFS_FSMLEVEL];
                pl          = s[OFS_FSMPLAYER+fsmlevel];
                player[pl].listPossibilities(s);
                int nactions = possibilities.n;
                int aind;

                if ((isKnownState) && (node!=null))
                {
                    // known states
                    //aind = possibilities.randomInd();                
                    aind = uctTree.selectAction(hc,pl,false);
                    uctTree.addTrace(hc, pl, aind);
//        System.out.printf("!7");

                }
                else if ((isKnownState) && (node==null))
                {
                    // first unknown state
                    isKnownState = false;

                    aind = possibilities.randomInd();                
                    uctTree.addState(s,hc, possibilities);
                    uctTree.addTrace(hc, pl, aind);
//        System.out.printf("!8");
                }
                else
                {
                    // further unknown states
                    aind = possibilities.randomInd();                
//        System.out.printf("!9");
                }

                a = possibilities.action[aind];
//        System.out.printf("!5");
                player[pl].performAction(s, a);
                stateTransition(s, a);

                winner = getWinner(s);
                if (winner !=-1)
                    break;
            }
            uctTree.update(winner, uctTime);
        }
        // !!! printing takes LOTS of time
        //System.out.println(uctTree);
//        s2[3] = 8;
//        printArray(s);
//        printArray(s2);
        isLoggingOn = oldIsLoggingOn;
        s=null;
        a=null;
    }
            
    public int getWinner(int[] s)
    {
        int pl;
        int retval = -1;
        for (pl=0; pl<NPLAYERS;pl++)
        {
            if (s[OFS_PLAYERDATA[pl] + OFS_SCORE] >= 10)
                retval = pl;
        }
        return retval;
    }
          
    int[] auxScoreArray = new int[4];
    
    public void recalcScores(int[] s)
    {
        int pl;
        for (pl=0; pl<NPLAYERS; pl++)
        {
            auxScoreArray[pl] = 0;
            auxScoreArray[pl] += s[OFS_PLAYERDATA[pl] + OFS_NSETTLEMENTS];
            auxScoreArray[pl] += s[OFS_PLAYERDATA[pl] + OFS_NCITIES]*2;
            auxScoreArray[pl] += s[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_ONEPOINT];
        }
        for (pl=0; pl<NPLAYERS; pl++)
        {
            s[OFS_PLAYERDATA[pl] + OFS_SCORE] = auxScoreArray[pl];
        }
        
        pl = s[OFS_LARGESTARMY_AT];
        if (pl!= -1)
            s[OFS_PLAYERDATA[pl] + OFS_SCORE] += 2;
        
        pl = s[OFS_LONGESTROAD_AT];
        if (pl!= -1)
            s[OFS_PLAYERDATA[pl] + OFS_SCORE] += 2;
    }
    
    public void recalcScores()
    {
        recalcScores(state);
    }
    
    
    private static final int LR_EMPTY = 0;
    private static final int LR_UNCHECKED = 1;
    private static final int LR_CHECKED1 = 2;
    private static final int LR_CHECKED2 = 3;
    private static final int LR_CHECKED3 = 4;
    private static final int LR_MAXROUTE = 5;


    boolean isOpponentPresentAtVertex(int []s, int pl, int ind)
    {
        boolean returnval;
        int val = s[OFS_VERTICES + ind];
        if ((val == VERTEX_EMPTY) || (val == VERTEX_TOOCLOSE) || 
                (val == VERTEX_HASSETTLEMENT+pl) || (val == VERTEX_HASCITY+pl))
            returnval = false;
        else returnval = true;
        for (int j = 0; j < 6; j++) 
        {
            val = neighborVertexEdge[ind][j];
            if ((val!=-1) && (s[OFS_EDGES+val] != EDGE_EMPTY)) //opponent has road
                returnval = true;
        }
        return returnval;
    }
    
void lrDepthFirstSearch(int[] s, int pl, int ind, int []lrVertices, boolean[] lrOpponentPresent,  boolean[] lrPlayerPresent,
        int UNCHECKEDVALUE, int CHECKEDVALUE, int[] returnvalues)
{
    int cind, cpos, i, j;
    int[] lrStack = new int[N_VERTICES];
    int[] lrStackPos = new int[N_VERTICES];
    int lrStacklen;
    boolean foundnext, isstartind;
    int maxlen = 0, maxStartInd = 0;
    int nextind, nextedge;

    nextind = 0; // unnecessary, but otherwise "uninitialized" error

    lrStacklen = 0;
    cind = ind;
    cpos = 0;
    isstartind = true;
    //lrStack[0] = ind;
    do
    {
        //System.out.printf("(%d,%d,%d) ",cind,cpos,lrStacklen);
        foundnext = false;
        //System.out.printf("*");
        isstartind = false;
        lrVertices[cind] = CHECKEDVALUE;
        vertices[cind].debugLRstatus = CHECKEDVALUE;
        // TODO: if search starts in a "broken" vertex, the algorithm believes that it is connected
        if ((cind==ind) || (lrPlayerPresent[cind]) || (!lrOpponentPresent[cind]) )
        {
            for (j=cpos; j<6; j++)
            {
                //System.out.printf(".");
                nextind = neighborVertexVertex[cind][j];
                nextedge = neighborVertexEdge[cind][j];
                if (nextind==-1) 
                    continue;
                if (s[OFS_EDGES + nextedge] != EDGE_OCCUPIED+pl)
                    continue;
                if (lrVertices[nextind]!=UNCHECKEDVALUE)
                    continue;
                foundnext = true;
                lrStack[lrStacklen] = cind;
                lrStackPos[lrStacklen] = j+1;
                lrStacklen++;
                if (lrStacklen>maxlen)
                {
                    maxlen = lrStacklen;
                    maxStartInd = nextind;
                }
                if ((CHECKEDVALUE == LR_CHECKED3) && (maxlen==returnvalues[0]))
                {
                    for (i=0; i<lrStacklen; i++)
                    {
                        vertices[lrStack[i]].debugLRstatus = CHECKEDVALUE;
                        // TODO: implement this correctly
//                        edges[neighborVertexEdge[lrStack[i]][lrStackPos[i]-1]].isPartOfLongestRoad 
//                                = true;
                    }
                    vertices[nextind].debugLRstatus = CHECKEDVALUE;
                    break;
                }
                break;
            }
        }
        if (foundnext)
        {
            cind = nextind;
            cpos = 0;
        }
        else
        {
            if (lrStacklen==0)
                break;
            lrStacklen--;
            cind = lrStack[lrStacklen];
            cpos = lrStackPos[lrStacklen];
        }
        //System.out.printf("x");                    
    } while (lrStacklen>=0);
    returnvalues[0] = maxlen;
    returnvalues[1] = maxStartInd;
    
    lrStack=null;
    lrStackPos=null;
}

public void recalcLongestRoad(int[] s, int pl)
    {
        int ind, cind, cpos, j, k;
        int[] lrVertices = new int[N_VERTICES];
        boolean[] lrOpponentPresent = new boolean[N_VERTICES];
        boolean[] lrPlayerPresent = new boolean[N_VERTICES];
        int[] returnvalues = new int[2];
        int maxlen, maxStartInd = 0;
        int val;
        //int pl;
                
        
        for (ind=0; ind<N_VERTICES; ind++)
            vertices[ind].debugLRstatus = 0;
        for (ind=0; ind<N_EDGES; ind++)
            edges[ind].isPartOfLongestRoad = false;
//        for (pl = 0; pl < NPLAYERS; pl++) 
        {
            for (ind=0; ind<N_VERTICES; ind++)
            {
                //System.out.printf("/%d/",ind);       
                
                lrVertices[ind] = LR_EMPTY;
                lrOpponentPresent[ind] = false;
                val = s[OFS_VERTICES + ind];
                if ((val == VERTEX_EMPTY) || (val == VERTEX_TOOCLOSE))
                    ;
                else if ((val == VERTEX_HASSETTLEMENT+pl) || (val == VERTEX_HASCITY+pl))
                    lrPlayerPresent[ind] = true;
                else 
                    lrOpponentPresent[ind] = true;
                for (j = 0; j < 6; j++) 
                {
                    val = neighborVertexEdge[ind][j];
                    if ((val!=-1) && (s[OFS_EDGES+val] == EDGE_OCCUPIED+pl)) //player has road
                        lrVertices[ind] = LR_UNCHECKED;
//                    else if ((val!=-1) && (s[OFS_EDGES+val] != EDGE_EMPTY)) //opponent has road
//                        lrOpponentPresent[ind] = true;     
                }
            }
            
            
            
            // TODO!!! 6-length cycles counts only as a 5 !!!
            maxlen = 0;
            for (ind=0; ind<N_VERTICES; ind++)
            {
                if (lrVertices[ind]!=LR_UNCHECKED)
                    continue;
                lrDepthFirstSearch(s, pl, ind, lrVertices, lrOpponentPresent, lrPlayerPresent, 
                        LR_UNCHECKED, LR_CHECKED1, returnvalues);
                lrDepthFirstSearch(s, pl, returnvalues[1], lrVertices, lrOpponentPresent, lrPlayerPresent, 
                        LR_CHECKED1, LR_CHECKED2, returnvalues);
                if (maxlen<returnvalues[0])
                {
                    maxlen = returnvalues[0];
                    maxStartInd = returnvalues[1];
                }
            }
//            if (maxlen>0)
//                vertices[maxStartInd].isPartOfLongestRoad = LR_MAXROUTE;
//            maxlen = returnvalues[0];
            
            // the purpose of this call to DFS is to mark the longest road.
            lrDepthFirstSearch(s, pl, maxStartInd, lrVertices, lrOpponentPresent, lrPlayerPresent, 
                        LR_CHECKED2, LR_CHECKED3, returnvalues);
            s[OFS_PLAYERDATA[pl] + OFS_PLAYERSLONGESTROAD] = maxlen;
        }
        
        int maxpl = s[OFS_LONGESTROAD_AT]; // current player with longest road;
        if (maxpl!=-1)
            maxlen = s[OFS_PLAYERDATA[maxpl] + OFS_PLAYERSLONGESTROAD];
        else
            maxlen = 0;
        for (pl=0; pl<NPLAYERS; pl++)
        {
            if (s[OFS_PLAYERDATA[pl] + OFS_PLAYERSLONGESTROAD] > maxlen)
            {
                maxlen = s[OFS_PLAYERDATA[pl] + OFS_PLAYERSLONGESTROAD];
                maxpl = pl;
            }
        }
        if (maxlen>=5)
        {
            s[OFS_LONGESTROAD_AT] = maxpl;
        }
        lrVertices=null;
        lrOpponentPresent=null;
        lrPlayerPresent =null;
        returnvalues =null;
    }
    
    public void recalcLargestArmy(int[]s)
    {
        int pl;
        int largestpl = s[OFS_LARGESTARMY_AT];
        int current;
        
        for (pl=0; pl<NPLAYERS; pl++)
        {
            current = s[OFS_PLAYERDATA[pl] + OFS_USEDCARDS + CARD_KNIGHT];
            if ((largestpl==-1) && (current>=3))
                s[OFS_LARGESTARMY_AT] = pl;
            if ((largestpl!=-1) && (current> s[OFS_PLAYERDATA[largestpl] + OFS_USEDCARDS + CARD_KNIGHT]))
                s[OFS_LARGESTARMY_AT] = pl;            
        }
    }
    
    FileWriter logfile = null;
    
    public void writeLog(int[] st,int[] a, int pl, String gameName, String event)
    {
        System.out.println("log:  " + event);
        if (logfile == null) {
            try {
                String s;
                logfile = new FileWriter(gameName + ".txt");
                s = String.format("I %d \n", st.length);
                logfile.write(s);
            } catch (IOException ex) {
                Logger.getLogger(BoardLayout.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    logfile.close();
                } catch (IOException ex) {
                    Logger.getLogger(BoardLayout.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

        if (logfile != null) {
            try {
                String s;
                int i;

                logfile = new FileWriter(gameName + ".txt",true);
                logfile.append("C " + event + "\n");
                System.out.println("C " + event);
                s = "A " + pl;
                for (i=0; i<a.length; i++)
                    s = s + " " + a[i];
                logfile.append(s + "\n");
                System.out.println(s);
                s = "S";
                for (i=0; i<st.length; i++)
                    s = s + " " + st[i];
                logfile.append(s + "\n");
                System.out.println(s);
                logfile.close();              
            //D.ebugPrintln("WROTE |"+event+"|");
            } catch (Exception e) {
                System.out.println("log: ERROR "+e);
            }
        }
        
    }

}
