/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.robot;

import soc.game.SOCBoard;
import soc.game.SOCGame;
import soc.game.SOCPlayer;

import soc.message.SOCBoardLayout;
import soc.message.SOCDeleteGame;
import soc.message.SOCDiceResult;
import soc.message.SOCGameState;
import soc.message.SOCImARobot;
import soc.message.SOCJoinGameAuth;
import soc.message.SOCMessage;
import soc.message.SOCMoveRobber;
import soc.message.SOCPutPiece;
import soc.message.SOCStartGame;
import soc.util.CappedQueue;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import smartsettlers.DebugFrame;
import smartsettlers.boardlayout.BoardLayout;
import smartsettlers.boardlayout.GameStateConstants;
import smartsettlers.boardlayout.HexTypeConstants;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCDevCardSet;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.message.SOCBankTrade;
import soc.message.SOCChoosePlayer;
import soc.message.SOCDiscard;
import soc.message.SOCDiscoveryPick;
import soc.message.SOCMonopolyPick;
import soc.message.SOCPlayDevCardRequest;



/**
 * This is a client that can play Settlers of Catan.
 *
 * @author Robert S Thomas
 */
public class SSRobotClient extends SOCRobotClient implements GameStateConstants, HexTypeConstants
{
    public BoardLayout bl;
    private DebugFrame debugframe;
    private int lastSettlement;
            
//    /**
//     * constants for debug recording
//     */
//    public static final String CURRENT_PLANS = "CURRENT_PLANS";
//    public static final String CURRENT_RESOURCES = "RESOURCES";

//    /**
//     * the thread the reads incomming messages
//     */
//    private Thread reader;
//
//    /**
//     * the current robot parameters for robot brains
//     */
//    private SOCRobotParameters currentRobotParameters;
//
//    /**
//     * the robot's "brains" for diferent games
//     */
//    private Hashtable robotBrains = new Hashtable();
//
//    /**
//     * the message queues for the different brains
//     */
//    private Hashtable brainQs = new Hashtable();
//
//    /**
//     * a table of requests from the server to sit at games
//     */
//    private Hashtable seatRequests = new Hashtable();

//    /**
//     * number of games this bot has played
//     */
//    protected int gamesPlayed;
//
//    /**
//     * number of games finished
//     */
//    protected int gamesFinished;
//
//    /**
//     * number of games this bot has won
//     */
//    protected int gamesWon;
//
//    /**
//     * number of clean brain kills
//     */
//    protected int cleanBrainKills;
//
//    /**
//     * start time
//     */
//    protected long startTime;
//
//    /**
//     * used to maintain connection
//     */
//    SOCRobotResetThread resetThread;

    /**
     * Constructor for connecting to the specified host, on the specified port
     *
     * @param h  host
     * @param p  port
     * @param nn nickname for robot
     * @param pw password for robot
     */
    public SSRobotClient(String h, int p, String nn, String pw)
    {
        super(h,p,nn,pw);
    }

    /**
     * Initialize the robot player
     */
    @Override
    public void init()
    {
        super.init_noImARobot();
        debugframe = new DebugFrame();
        debugframe.setVisible(true);
        bl = debugframe.boardlayout;
        try
        {
//            s = new Socket(host, port);
//            s.setSoTimeout(300000);
//            in = new DataInputStream(s.getInputStream());
//            out = new DataOutputStream(s.getOutputStream());
//            connected = true;
//            reader = new Thread(this);
//            reader.start();
//
//            //resetThread = new SOCRobotResetThread(this);
//            //resetThread.start();
            put(SOCImARobot.toCmd(nickname, "soc.robot.SSRobotClient"));
        }
        catch (Exception e)
        {
            ex = e;
            System.err.println("Could not connect to the server: " + ex);
        }
        
    }

//    public SSRobotClient(String[] args)
//    {
//		if (args.length < 4)
//		{
//			System.err.println("usage: java soc.robot.SOCRobotClient host port_number userid password");
//
//			return;
//		}
//    	
//        SSRobotClient ex1 = new SSRobotClient(args[0], Integer.parseInt(args[1]), args[2], args[3]);
//        ex1.init();
//    }
//    
    @Override
    protected void handleDELETEGAME(SOCDeleteGame mes)
    {        
        //!!! save game
        SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
        SOCGame ga = (SOCGame) games.get(mes.getGame());
        sendStateToSmartSettlers(ga, S_FINISHED);
        
        PrintStream f;
        String fname = "settlerslog.txt";
        try 
        {            
            //f = new PrintStream(fname);
            String s = "";
            f = new PrintStream(new FileOutputStream(fname, true));
            int i;
            int pn = brain.getOurPlayerData().getPlayerNumber();
            for (i=0; i<NPLAYERS; i++)
            {
                s = String.format("%s%d ", s,(i==pn)?1:0 );
                //f.printf("%d ", (i==pn)?1:0 );
            }
//            for (i=0; i<NPLAYERS; i++)
//            {
//                SOCPlayer p = ga.getPlayer(i);                
//                f.printf("%d ", p.getTotalVP() );                
//            }
//            f.print("\t");
            for (i=0; i<NPLAYERS; i++)
            {
                s = String.format("%s%d ", s,bl.state[OFS_PLAYERDATA[i] + OFS_SCORE] );                
                //f.printf("%d ", bl.state[OFS_PLAYERDATA[i] + OFS_SCORE] );                
            }
            //f.print("\t");
            s = s+"\t";
            for (i=0; i<STATESIZE; i++)
            {
                s = String.format("%s%d ", s,bl.state[i] );                
                //f.printf("%d ", bl.state[i] );                                
            }
            //f.println();
            f.println(s);
            System.out.println(s);
            f.close();
        } 
        catch (FileNotFoundException ex1) 
        {
            Logger.getLogger(SSRobotClient.class.getName()).log(Level.SEVERE, null, ex1);
        }
        
        
        //debugframe.dispose();
        super.handleDELETEGAME(mes);
    }
    
    public void sendGameToSmartSettlers(SOCGame ga)
    {
        int xo, yo;
        int xn, yn;
        int indo, indn;
        int coordo;
        int to, tn;
        
        SOCBoard bo = ga.getBoard();
        for (xn=0; xn<BoardLayout.MAXX; xn++)
            for (yn=0; yn<BoardLayout.MAXX; yn++)
            {
                if ((xn+yn<3) || (xn+yn>9))
                    continue;
                indn = bl.hexatcoord[xn][yn];
                
                xo = 2*xn+1;
                yo = 2*(xn+yn)-5;
                coordo = 16*xo + yo;
                //indo = bo.hexIDtoNum[coordo];
                to = bo.getHexTypeFromCoord(coordo);
                //int hexType = bo.getHexLayout()[to];

                //System.out.println(to);
                if ((to>=0) && (to<=5))
                {
                    switch (to)
                    {
                        case 0:
                            tn = LAND_DESERT;
                            break;                            
                        case 1:
                            tn = LAND_CLAY;
                            break;                            
                        case 2:
                            tn = LAND_STONE;
                            break;                            
                        case 3:
                            tn = LAND_SHEEP;
                            break;                            
                        case 4:
                            tn = LAND_WHEAT;
                            break;                            
                        case 5:
                            tn = LAND_WOOD;
                            break;     
                        default:
                            tn = -1; // should cause error
                    }
                    bl.hextiles[indn].subtype = tn;
                    bl.hextiles[indn].type = TYPE_LAND;
                    if (tn != LAND_DESERT)
                        bl.hextiles[indn].productionNumber = bo.getNumberOnHexFromCoord(coordo);
                    else
                        bl.hextiles[indn].productionNumber = bo.getNumberOnHexFromCoord(coordo);
                    
                }
                else if ((to >= 7) && (to <= 12))
                {
                    switch (to)
                    {
                        case SOCBoard.MISC_PORT_HEX:
                            tn = PORT_MISC;
                            break;
                        case SOCBoard.CLAY_PORT_HEX:
                            tn = PORT_CLAY;
                            break;
                        case SOCBoard.ORE_PORT_HEX:
                            tn = PORT_STONE;
                            break;
                        case SOCBoard.SHEEP_PORT_HEX:
                            tn = PORT_SHEEP;
                            break;
                        case SOCBoard.WHEAT_PORT_HEX:
                            tn = PORT_WHEAT;
                            break;
                        case SOCBoard.WOOD_PORT_HEX:
                            tn = PORT_WOOD;
                            break;
                        default:
                            tn = PORT_MISC;
                    }
                    bl.hextiles[indn].subtype = tn;
                    bl.hextiles[indn].type = TYPE_PORT;
//                    bl.hextiles[indn].orientation = 
                }
                else 
                {
                    bl.hextiles[indn].type = TYPE_SEA;
                    bl.hextiles[indn].subtype = SEA;
                    bl.hextiles[indn].orientation = -1;
                    //System.out.println(".");
                }

                
//                bl.hextiles[indn].subtype = LAND_WOOD;
//                bl.hextiles[indn].type = TYPE_LAND;
            }
        initTranslationTables(ga.getBoard());

//        bl.GameTick(bl.state, bl.action);
        debugframe.repaint();
    }
    
    public int translateHexToSmartSettlers(int indo)
    {
        if (indo==-1)
            return -1;
        int xo = indo/16;
        int yo = indo%16;
        
        int xn = (xo-1)/2;
        int yn = (yo+5)/2-xn;
        
        //System.out.printf("%d  (%d,%d) -> (%d,%d) \n", indo, xo,yo, xn, yn);
        return bl.hexatcoord[xn][yn];
    }
    
    public int translateHexToJSettlers(int indn)
    {
        if (indn==-1)
            return -1;
        
        int xn = (int) bl.hextiles[indn].pos.x;
        int yn = (int) bl.hextiles[indn].pos.y;
        
        int xo = 2*xn+1;
        int yo = 2*(xn+yn)-5;
        
        //System.out.printf("%d  (%d,%d) -> (%d,%d) \n", indo, xo,yo, xn, yn);
        return xo*16+yo;
    }

    public int[] vertexToSS;
    public int[] edgeToSS;
    public int[] vertexToJS;
    public int[] edgeToJS;
    
    public int translateVertexToSmartSettlers(int indo)
    {
        if (vertexToSS == null)
            return 0;
        return vertexToSS[indo];
    }

    public int translateEdgeToSmartSettlers(int indo)
    {
        return edgeToSS[indo];
    }
    
    public int translateVertexToJSettlers(int indo)
    {
        return vertexToJS[indo];
    }

    public int translateEdgeToJSettlers(int indo)
    {
        return edgeToJS[indo];
    }
    
    public int translateResToJSettlers(int ind)
    {
        switch (ind)
        {
            case RES_WOOD:
                return SOCResourceConstants.WOOD;
            case RES_CLAY:
                return SOCResourceConstants.CLAY;
            case RES_SHEEP:
                return SOCResourceConstants.SHEEP;
            case RES_WHEAT:
                return SOCResourceConstants.WHEAT;
            case RES_STONE:
                return SOCResourceConstants.ORE;
            default:
                return -1;
        }
    }
    
    public int translateResToSmartSettlers(int ind)
    {
        switch (ind)
        {
            case SOCResourceConstants.WOOD:
                return RES_WOOD;
            case SOCResourceConstants.CLAY:
                return RES_CLAY;
            case SOCResourceConstants.SHEEP:
                return RES_SHEEP;
            case SOCResourceConstants.WHEAT:
                return RES_WHEAT;
            case SOCResourceConstants.ORE:
                return RES_STONE;
            default:
                return -1;
        }
    }
    
    public void initTranslationTables(SOCBoard bo)
    {
        int vo, vn;
        int eo, en;
        int ho, hn, j;
        vertexToSS = new int[SOCBoard.MAXNODE+1];
        edgeToSS = new int[SOCBoard.MAXEDGE+1];
        vertexToJS = new int[N_VERTICES];        
        edgeToJS = new int[N_EDGES];

        int[] numToHexID = 
        {
            0x17, 0x39, 0x5B, 0x7D,
            0x15, 0x37, 0x59, 0x7B, 0x9D,
            0x13, 0x35, 0x57, 0x79, 0x9B, 0xBD,
            0x11, 0x33, 0x55, 0x77, 0x99, 0xBB, 0xDD,
            0x31, 0x53, 0x75, 0x97, 0xB9, 0xDB,
            0x51, 0x73, 0x95, 0xB7, 0xD9,
            0x71, 0x93, 0xB5, 0xD7
        };
        
        for (j=0; j<numToHexID.length; j++)
        //for (ho = SOCBoard.MINHEX; ho<=SOCBoard.MAXHEX; ho++)
        {
            ho = numToHexID[j];
            if (bo.getHexTypeFromCoord(ho) >= SOCBoard.WATER_HEX)
                continue;
            hn = translateHexToSmartSettlers(ho);
            int i = 0;
            Vector vlist = SOCBoard.getAdjacentNodesToHex(ho);
            Vector elist = SOCBoard.getAdjacentEdgesToHex(ho);
            for (i = 0; i<6; i++)
            {
                vo = (Integer) vlist.get(i);
                vn = bl.neighborHexVertex[hn][i];
                vertexToSS[vo] = vn;
                vertexToJS[vn] = vo;
                eo = (Integer) elist.get(i);
                en = bl.neighborHexEdge[hn][i];
                edgeToSS[eo] = en;
                edgeToJS[en] = eo;
            }
        }
        
    }

    @Override
    public void putPiece(SOCGame ga, SOCPlayingPiece pp)
    {
        super.put(SOCPutPiece.toCmd(ga.getName(), pp.getPlayer().getPlayerNumber(), pp.getType(), pp.getCoordinates()));
        if (pp.getType() == SOCPlayingPiece.SETTLEMENT)
        {
            lastSettlement = pp.getCoordinates();
        }
    }

    public int[] sendStateToSmartSettlers(SOCGame ga, int GAMESTATE)
    {
        int[] st = new int[STATESIZE];
        int val, fsmlevel;
        Vector v;
        Enumeration pEnum;
        int indo, indn;
        
        
        st[OFS_TURN] = 0;
        fsmlevel = 0;
        if (GAMESTATE == S_ROBBERAT7)
            fsmlevel = 1;
        st[OFS_FSMLEVEL] = fsmlevel;
        st[OFS_FSMPLAYER+fsmlevel] = ga.getCurrentPlayerNumber(); 
        st[OFS_FSMSTATE+fsmlevel] = GAMESTATE;
        
        val = ga.getBoard().getRobberHex();
        st[OFS_ROBBERPLACE] = translateHexToSmartSettlers(val);
//        if ((GAMESTATE == S_ROAD1) 
//                
//                || (GAMESTATE == S_SETTLEMENT2)
//        {
        if (lastSettlement !=-1)
        {
            //System.out.printf("last STLMT %d\n" , lastSettlement );
            st[OFS_LASTVERTEX] = translateVertexToSmartSettlers(lastSettlement);
        }
        
        val = ga.getCurrentDice(); ///??? always -1
        //System.out.println(val);
        if (val==-1)
        {
            st[OFS_DIE1] = 0;
            st[OFS_DIE2] = 0;                        
        }
        else if (val<7)
        {
            st[OFS_DIE1] = 1;
            st[OFS_DIE2] = val-1;            
        }
        else
        {
            st[OFS_DIE1] = 6;
            st[OFS_DIE2] = val-6;            
        }
        
        v = ga.getBoard().getSettlements();
        pEnum = v.elements();
        while (pEnum.hasMoreElements())
        {
            SOCSettlement p = (SOCSettlement) pEnum.nextElement();
            
            //System.out.printf("%X ", p.getCoordinates());
            indn = translateVertexToSmartSettlers(p.getCoordinates());
            val = p.getPlayer().getPlayerNumber();
            st[OFS_VERTICES+indn] = VERTEX_HASSETTLEMENT + val;
        }
        //System.out.println();
        v = ga.getBoard().getCities();
        pEnum = v.elements();
        while (pEnum.hasMoreElements())
        {
            SOCCity p = (SOCCity) pEnum.nextElement();
            
            //System.out.printf("%X ", p.getCoordinates());
            indn = translateVertexToSmartSettlers(p.getCoordinates());
            val = p.getPlayer().getPlayerNumber();
            st[OFS_VERTICES+indn] = VERTEX_HASCITY + val;
        }
        int i, j;
        for (i=0; i<N_VERTICES; i++)
        {
            boolean islegal = true;
            if (st[OFS_VERTICES + i] >= VERTEX_HASSETTLEMENT)
                continue;
            for (j=0; j<6; j++)
            {
                indn = bl.neighborVertexVertex[i][j];
                if ((indn!=-1) && (st[OFS_VERTICES + indn] >= VERTEX_HASSETTLEMENT))
                {
                    islegal = false;
                    break;
                }
            }
            if (!islegal)
                st[OFS_VERTICES + i] = VERTEX_TOOCLOSE;
        }

        v = ga.getBoard().getRoads();
        pEnum = v.elements();
        while (pEnum.hasMoreElements())
        {
            SOCRoad p = (SOCRoad) pEnum.nextElement();
            
            //System.out.printf("%X ", p.getCoordinates());
            indn = translateEdgeToSmartSettlers(p.getCoordinates());
            val = p.getPlayer().getPlayerNumber();
            st[OFS_EDGES+indn] = EDGE_OCCUPIED + val;
        }
        
        
        if (ga.getPlayerWithLargestArmy() == null)
            val = -1;
        else
            val = ga.getPlayerWithLargestArmy().getPlayerNumber();
        st[OFS_LARGESTARMY_AT] = val;
        if (ga.getPlayerWithLongestRoad() == null)
            val = -1;
        else
            val = ga.getPlayerWithLongestRoad().getPlayerNumber();
        st[OFS_LONGESTROAD_AT] = val;
        st[OFS_NCARDSGONE] = NCARDS-ga.getNumDevCards();
        int pl;        
        for (pl=0; pl<NPLAYERS; pl++)
        {
            SOCPlayer p = ga.getPlayer(pl);
            st[OFS_PLAYERDATA[pl] + OFS_NSETTLEMENTS] = 5-p.getNumPieces(SOCPlayingPiece.SETTLEMENT);
            st[OFS_PLAYERDATA[pl] + OFS_NCITIES] = 4-p.getNumPieces(SOCPlayingPiece.CITY);
            st[OFS_PLAYERDATA[pl] + OFS_NROADS] = 15-p.getNumPieces(SOCPlayingPiece.ROAD);
            st[OFS_PLAYERDATA[pl] + OFS_PLAYERSLONGESTROAD] = p.getLongestRoadLength();
            st[OFS_PLAYERDATA[pl] + OFS_HASPLAYEDCARD] = p.hasPlayedDevCard() ?1:0;

            boolean hasports[] = p.getPortFlags();
            st[OFS_PLAYERDATA[pl] + OFS_ACCESSTOPORT + PORT_CLAY-1] = hasports[SOCBoard.CLAY_PORT] ?1:0;
            st[OFS_PLAYERDATA[pl] + OFS_ACCESSTOPORT + PORT_WOOD-1] = hasports[SOCBoard.WOOD_PORT] ?1:0;
            st[OFS_PLAYERDATA[pl] + OFS_ACCESSTOPORT + PORT_STONE-1]= hasports[SOCBoard.ORE_PORT] ?1:0;
            st[OFS_PLAYERDATA[pl] + OFS_ACCESSTOPORT + PORT_SHEEP-1]= hasports[SOCBoard.SHEEP_PORT] ?1:0;
            st[OFS_PLAYERDATA[pl] + OFS_ACCESSTOPORT + PORT_WHEAT-1] = hasports[SOCBoard.WHEAT_PORT] ?1:0;
            st[OFS_PLAYERDATA[pl] + OFS_ACCESSTOPORT + PORT_MISC-1] = hasports[SOCBoard.MISC_PORT] ?1:0;
            
            SOCResourceSet rs = p.getResources();
            st[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_CLAY ] = rs.getAmount(SOCResourceConstants.CLAY);
            st[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_WOOD ] = rs.getAmount(SOCResourceConstants.WOOD);
            st[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_STONE] = rs.getAmount(SOCResourceConstants.ORE);
            st[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_SHEEP] = rs.getAmount(SOCResourceConstants.SHEEP);
            st[OFS_PLAYERDATA[pl] + OFS_RESOURCES + RES_WHEAT] = rs.getAmount(SOCResourceConstants.WHEAT);
            
            SOCDevCardSet ds = p.getDevCards();
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_KNIGHT] = 
                    ds.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.KNIGHT);
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_FREEROAD] = 
                    ds.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.ROADS);
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_FREERESOURCE] = 
                    ds.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.DISC);
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_MONOPOLY] = 
                    ds.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.MONO);
            st[OFS_PLAYERDATA[pl] + OFS_NEWCARDS + CARD_ONEPOINT] = 0;

            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_KNIGHT] = 
                    ds.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.KNIGHT);
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREEROAD] = 
                    ds.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.ROADS);
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_FREERESOURCE] = 
                    ds.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.DISC);
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_MONOPOLY] = 
                    ds.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.MONO);
            st[OFS_PLAYERDATA[pl] + OFS_OLDCARDS + CARD_ONEPOINT] = ds.getNumVPCards();
            
            st[OFS_PLAYERDATA[pl] + OFS_USEDCARDS + CARD_KNIGHT] = 
                    p.getNumKnights();
            //!!! other used cards are not stored...
        }
        bl.setState(st);
        bl.recalcScores(); // fills OFS_SCORE fields
        
//    int OFS_TURN                = 0;
//    int OFS_FSMLEVEL            = OFS_TURN          +1 ;
//    int OFS_FSMSTATE            = OFS_FSMLEVEL      +1 ;
// +  int OFS_FSMPLAYER           = OFS_FSMSTATE      +3 ;
// +  int OFS_NCARDSGONE          = OFS_FSMPLAYER     +3 ;
// +  int OFS_DIE1                = OFS_NCARDSGONE    +1 ;
// +  int OFS_DIE2                = OFS_DIE1          +1 ;
// +  int OFS_ROBBERPLACE         = OFS_DIE2          +1 ;
// +  int OFS_LONGESTROAD_AT      = OFS_ROBBERPLACE   +1 ;
// +  int OFS_LARGESTARMY_AT      = OFS_LONGESTROAD_AT   +1 ;
// +  int OFS_EDGES               = OFS_LARGESTARMY_AT   +1 ;
// +  int OFS_VERTICES            = OFS_EDGES         +N_EDGES ;
// x  int OFS_EDGEACCESSIBLE      = OFS_VERTICES      +N_VERTICES;
// x  int OFS_VERTEXACCESSIBLE    = OFS_EDGEACCESSIBLE+N_EDGES;
//            
//    
// +      int OFS_SCORE               = 0;
// +      int OFS_NSETTLEMENTS        = 1;
// +      int OFS_NCITIES             = 2;
// +      int OFS_NROADS              = 3;
// +      int OFS_PLAYERSLONGESTROAD  = 4;
// +      int OFS_RESOURCES           = OFS_PLAYERSLONGESTROAD   +1;
// +      int OFS_ACCESSTOPORT        = OFS_RESOURCES     +NRESOURCES;
// +      int OFS_USEDCARDS           = OFS_ACCESSTOPORT  +(NRESOURCES+1);
// +      int OFS_OLDCARDS            = OFS_USEDCARDS     +N_DEVCARDTYPES;
// +      int OFS_NEWCARDS            = OFS_OLDCARDS      +N_DEVCARDTYPES;
//        int PLAYERSTATESIZE         = OFS_NEWCARDS      +N_DEVCARDTYPES;
//    
//    int[] OFS_PLAYERDATA        = { OFS_VERTEXACCESSIBLE+N_VERTICES,
//                                    OFS_VERTEXACCESSIBLE+N_VERTICES + PLAYERSTATESIZE,
//                                    OFS_VERTEXACCESSIBLE+N_VERTICES + 2*PLAYERSTATESIZE,
//                                    OFS_VERTEXACCESSIBLE+N_VERTICES + 3*PLAYERSTATESIZE};    
//    int STATESIZE = OFS_VERTEXACCESSIBLE+N_VERTICES + 4*PLAYERSTATESIZE;
//    int ACTIONSIZE = 5;
        
        //bl.GameTick(bl.state, bl.action);
        
        debugframe.repaint();
        return st;
    }
    
int logRobberPlace = 0;

public void recordGameEvent(SOCMessage mes, String gameName, String event)
{
    SOCGame ga = (SOCGame) games.get(mes.getGame());
    if (ga==null)
        return;
    int socState = ga.getGameState();
    int pl = ga.getCurrentPlayerNumber();
    int ssState;
    switch (socState)
    {
        case SOCGame.START1A:
            ssState = S_SETTLEMENT1; 
            break;
        case SOCGame.START1B:
            ssState = S_ROAD1; 
            break;
        case SOCGame.START2A:
            ssState = S_SETTLEMENT2; 
            break;
        case SOCGame.START2B:
            ssState = S_ROAD2; 
            break;
        case SOCGame.PLAY:
            ssState = S_BEFOREDICE; 
            break;
        case SOCGame.PLAY1:
            ssState = S_NORMAL; 
            break;
        case SOCGame.PLACING_FREE_ROAD1:
            ssState = S_FREEROAD1; 
            break;
        case SOCGame.PLACING_FREE_ROAD2:
            ssState = S_FREEROAD2; 
            break;
        case SOCGame.PLACING_ROBBER:
            ssState = S_ROBBERAT7; ///??? 
            break;
        case SOCGame.WAITING_FOR_DISCARDS:
            ssState = S_PAYTAX;   ///???
            break;
        case SOCGame.OVER:
            ssState = S_FINISHED; 
            break;
        default:
            ssState = S_NORMAL;
    }

//    if (ssState == -1) 
//        return;
    
    int[] st = sendStateToSmartSettlers(ga, ssState);
    int[] a = new int[ACTIONSIZE];
    for (int i=0; i<a.length; i++)
        a[i]=0;
    
    int pos, r1, r2;
    a[0] = -1;
    switch (mes.getType())
    {
        case SOCMessage.BANKTRADE:
            SOCResourceSet get = ((SOCBankTrade)mes).getGetSet();
            SOCResourceSet give = ((SOCBankTrade)mes).getGiveSet();
            a[0] = A_PORTTRADE;
            System.out.println("get:  " + get.toString());
            System.out.println("give: " + give.toString());
            r1 = give.pickResource();
            a[1] = give.getAmount(r1);
            a[2] = translateResToSmartSettlers(r1);
            r2 = get.pickResource();
            a[3] = get.getAmount(r2);
            a[4] = translateResToSmartSettlers(r2);
            // give what: [2], amt: [1]        
            break;
        case SOCMessage.BUILDREQUEST:
            break;
        case SOCMessage.BUYCARDREQUEST:
            a[0] = A_BUYCARD;
            break;
        case SOCMessage.CHOOSEPLAYERREQUEST:
        case SOCMessage.DEVCARD:
            break;
        case SOCMessage.DISCARD:
            SOCResourceSet ds = ((SOCDiscard)mes).getResources();
            a[0] = A_PAYTAX;
            a[1] = ds.getTotal();
            // !!!! details...
            break;
        case SOCMessage.DISCARDREQUEST:
            break;
        case SOCMessage.DISCOVERYPICK:
            SOCResourceSet rs = ((SOCDiscoveryPick)mes).getResources();
            r1 = rs.pickResource();
            rs.subtract(1, r1);
            r2 = rs.pickResource();
            a[0] = A_PLAYCARD_FREERESOURCE;
            a[1] = translateResToSmartSettlers(r1);
            a[2] = translateResToSmartSettlers(r2);            
            break;
        case SOCMessage.ENDTURN:
            a[0] = A_ENDTURN;
            break;
        case SOCMessage.MONOPOLYPICK:
            a[0] = A_PLAYCARD_MONOPOLY;
            a[1] = translateResToSmartSettlers(((SOCMonopolyPick)mes).getResource());
            break;
        case SOCMessage.PLAYDEVCARDREQUEST:
            int dc = ((SOCPlayDevCardRequest)mes).getDevCard();
            switch (dc)
            {
                case SOCDevCardConstants.KNIGHT:
                    a[0] = A_PLAYCARD_KNIGHT;
                    //!!!! do the rest
                    break;
                case SOCDevCardConstants.MONO:
                    // action is translated at MONOPOLYPICK
                    break;
                case SOCDevCardConstants.DISC:
                    // action is translated at DISCOVERYPICK
                    break;
                case SOCDevCardConstants.ROADS:
                    a[0] = A_PLAYCARD_FREEROAD;
                    break;                    
            }
            break;
        case SOCMessage.PUTPIECE:            
            pos = ((SOCPutPiece)mes).getCoordinates();
            switch (((SOCPutPiece)mes).getPieceType())
            {
                case SOCPlayingPiece.ROAD:
                    a[0] = A_BUILDROAD;
                    a[1] = translateEdgeToSmartSettlers(pos);
                    break;
                case SOCPlayingPiece.SETTLEMENT:
                    a[0] = A_BUILDSETTLEMENT;
                    a[1] = translateVertexToSmartSettlers(pos);
                    break;
                case SOCPlayingPiece.CITY:
                    a[0] = A_BUILDCITY;
                    a[1] = translateVertexToSmartSettlers(pos);
                    break;
                
            }
            break;
        case SOCMessage.MOVEROBBER:
            // store robber position (robber place and victim will be passed as a single command)
            logRobberPlace = ((SOCMoveRobber)mes).getCoordinates();
            break;
        case SOCMessage.CHOOSEPLAYER:
            a[0] = A_PLACEROBBER;
            a[1] = translateEdgeToSmartSettlers(logRobberPlace);
            a[2] = ((SOCChoosePlayer)mes).getChoice();
            break;
        case SOCMessage.ROLLDICE:
            a[0] = A_THROWDICE;
            break;
        case SOCMessage.ROLLDICEREQUEST:
    }
// TURN: do nothing
    if (a[0] != -1)
        bl.writeLog(st,a,ga.getCurrentPlayerNumber(),gameName,event);
}

    
//    /**
//     * disconnect and then try to reconnect
//     */
//    public void disconnectReconnect()
//    {
//        D.ebugPrintln("(*)(*)(*)(*)(*)(*)(*) disconnectReconnect()");
//        ex = null;
//
//        try
//        {
//            connected = false;
//            s.close();
//            s = new Socket(host, port);
//            in = new DataInputStream(s.getInputStream());
//            out = new DataOutputStream(s.getOutputStream());
//            connected = true;
//            reader = new Thread(this);
//            reader.start();
//
//            //resetThread = new SOCRobotResetThread(this);
//            //resetThread.start();
//            put(SOCImARobot.toCmd(nickname));
//        }
//        catch (Exception e)
//        {
//            ex = e;
//            System.err.println("disconnectReconnect error: " + ex);
//        }
//    }

//    /**
//     * Treat the incoming messages
//     *
//     * @param mes    the message
//     */
//    public void treat(SOCMessage mes)
//    {
//        D.ebugPrintln("IN - " + mes);
//
//        try
//        {
//            switch (mes.getType())
//            {
//            /**
//             * server ping
//             */
//            case SOCMessage.SERVERPING:
//                handleSERVERPING((SOCServerPing) mes);
//
//                break;
//
//            /**
//             * admin ping
//             */
//            case SOCMessage.ADMINPING:
//                handleADMINPING((SOCAdminPing) mes);
//
//                break;
//
//            /**
//             * admin reset
//             */
//            case SOCMessage.ADMINRESET:
//                handleADMINRESET((SOCAdminReset) mes);
//
//                break;
//
//            /**
//             * update the current robot parameters
//             */
//            case SOCMessage.UPDATEROBOTPARAMS:
//                handleUPDATEROBOTPARAMS((SOCUpdateRobotParams) mes);
//
//                break;
//
//            /**
//             * join game authorization
//             */
//            case SOCMessage.JOINGAMEAUTH:
//                handleJOINGAMEAUTH((SOCJoinGameAuth) mes);
//
//                break;
//
//            /**
//             * someone joined a game
//             */
//            case SOCMessage.JOINGAME:
//                handleJOINGAME((SOCJoinGame) mes);
//
//                break;
//
//            /**
//             * someone left a game
//             */
//            case SOCMessage.LEAVEGAME:
//                handleLEAVEGAME((SOCLeaveGame) mes);
//
//                break;
//
//            /**
//             * game has been destroyed
//             */
//            case SOCMessage.DELETEGAME:
//                handleDELETEGAME((SOCDeleteGame) mes);
//
//                break;
//
//            /**
//             * list of game members
//             */
//            case SOCMessage.GAMEMEMBERS:
//                handleGAMEMEMBERS((SOCGameMembers) mes);
//
//                break;
//
//            /**
//             * game text message
//             */
//            case SOCMessage.GAMETEXTMSG:
//                handleGAMETEXTMSG((SOCGameTextMsg) mes);
//
//                break;
//
//            /**
//             * someone is sitting down
//             */
//            case SOCMessage.SITDOWN:
//                handleSITDOWN((SOCSitDown) mes);
//
//                break;
//
//            /**
//             * receive a board layout
//             */
//            case SOCMessage.BOARDLAYOUT:
//                handleBOARDLAYOUT((SOCBoardLayout) mes);
//
//                break;
//
//            /**
//             * message that the game is starting
//             */
//            case SOCMessage.STARTGAME:
//                handleSTARTGAME((SOCStartGame) mes);
//
//                break;
//
//            /**
//             * update the state of the game
//             */
//            case SOCMessage.GAMESTATE:
//                handleGAMESTATE((SOCGameState) mes);
//
//                break;
//
//            /**
//             * set the current turn
//             */
//            case SOCMessage.SETTURN:
//                handleSETTURN((SOCSetTurn) mes);
//
//                break;
//
//            /**
//             * set who the first player is
//             */
//            case SOCMessage.FIRSTPLAYER:
//                handleFIRSTPLAYER((SOCFirstPlayer) mes);
//
//                break;
//
//            /**
//             * update who's turn it is
//             */
//            case SOCMessage.TURN:
//                handleTURN((SOCTurn) mes);
//
//                break;
//
//            /**
//             * receive player information
//             */
//            case SOCMessage.PLAYERELEMENT:
//                handlePLAYERELEMENT((SOCPlayerElement) mes);
//
//                break;
//
//            /**
//             * receive resource count
//             */
//            case SOCMessage.RESOURCECOUNT:
//                handleRESOURCECOUNT((SOCResourceCount) mes);
//
//                break;
//
//            /**
//             * the latest dice result
//             */
//            case SOCMessage.DICERESULT:
//                handleDICERESULT((SOCDiceResult) mes);
//
//                break;
//
//            /**
//             * a player built something
//             */
//            case SOCMessage.PUTPIECE:
//                handlePUTPIECE((SOCPutPiece) mes);
//
//                break;
//
//            /**
//             * the robber moved
//             */
//            case SOCMessage.MOVEROBBER:
//                handleMOVEROBBER((SOCMoveRobber) mes);
//
//                break;
//
//            /**
//             * the server wants this player to discard
//             */
//            case SOCMessage.DISCARDREQUEST:
//                handleDISCARDREQUEST((SOCDiscardRequest) mes);
//
//                break;
//
//            /**
//             * the server wants this player to choose a player to rob
//             */
//            case SOCMessage.CHOOSEPLAYERREQUEST:
//                handleCHOOSEPLAYERREQUEST((SOCChoosePlayerRequest) mes);
//
//                break;
//
//            /**
//             * a player has made an offer
//             */
//            case SOCMessage.MAKEOFFER:
//                handleMAKEOFFER((SOCMakeOffer) mes);
//
//                break;
//
//            /**
//             * a player has cleared her offer
//             */
//            case SOCMessage.CLEAROFFER:
//                handleCLEAROFFER((SOCClearOffer) mes);
//
//                break;
//
//            /**
//             * a player has rejected an offer
//             */
//            case SOCMessage.REJECTOFFER:
//                handleREJECTOFFER((SOCRejectOffer) mes);
//
//                break;
//
//            /**
//             * a player has accepted an offer
//             */
//            case SOCMessage.ACCEPTOFFER:
//                handleACCEPTOFFER((SOCAcceptOffer) mes);
//
//                break;
//
//            /**
//             * the trade message needs to be cleared
//             */
//            case SOCMessage.CLEARTRADEMSG:
//                handleCLEARTRADEMSG((SOCClearTradeMsg) mes);
//
//                break;
//
//            /**
//             * the current number of development cards
//             */
//            case SOCMessage.DEVCARDCOUNT:
//                handleDEVCARDCOUNT((SOCDevCardCount) mes);
//
//                break;
//
//            /**
//             * a dev card action, either draw, play, or add to hand
//             */
//            case SOCMessage.DEVCARD:
//                handleDEVCARD((SOCDevCard) mes);
//
//                break;
//
//            /**
//             * set the flag that tells if a player has played a
//             * development card this turn
//             */
//            case SOCMessage.SETPLAYEDDEVCARD:
//                handleSETPLAYEDDEVCARD((SOCSetPlayedDevCard) mes);
//
//                break;
//
//            /**
//             * get a list of all the potential settlements for a player
//             */
//            case SOCMessage.POTENTIALSETTLEMENTS:
//                handlePOTENTIALSETTLEMENTS((SOCPotentialSettlements) mes);
//
//                break;
//
//            /**
//             * the server is requesting that we join a game
//             */
//            case SOCMessage.JOINGAMEREQUEST:
//                handleJOINGAMEREQUEST((SOCJoinGameRequest) mes);
//
//                break;
//
//            /**
//             * message that means the server wants us to leave the game
//             */
//            case SOCMessage.ROBOTDISMISS:
//                handleROBOTDISMISS((SOCRobotDismiss) mes);
//
//                break;
//            }
//            
//            if ((games != null) && (mes.getGame() != null))
//            {
//                SOCGame ga = (SOCGame) games.get(mes.getGame());
//                if (ga != null)
//                {
//                    sendStateToSmartSettlers(ga);
//                }
//            }
//
//        }
//        catch (Exception e)
//        {
//            System.out.println("SOCRobotClient treat ERROR - " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

//    /**
//     * handle the server ping message
//     * @param mes  the message
//     */
//    protected void handleSERVERPING(SOCServerPing mes)
//    {
//        /*
//           D.ebugPrintln("(*)(*) ServerPing message = "+mes);
//           D.ebugPrintln("(*)(*) ServerPing sleepTime = "+mes.getSleepTime());
//           D.ebugPrintln("(*)(*) resetThread = "+resetThread);
//           resetThread.sleepMore();
//         */
//    }

//    /**
//     * handle the admin ping message
//     * @param mes  the message
//     */
//    protected void handleADMINPING(SOCAdminPing mes)
//    {
//        D.ebugPrintln("*** Admin Ping message = " + mes);
//
//        SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//        //
//        //  if the robot hears a PING and is in the game
//        //  where the admin is, then just say "OK".
//        //  otherwise, join the game that the admin is in
//        //
//        //  note: this is a hack because the bot never 
//        //        leaves the game and the game must be 
//        //        killed by the admin
//        //
//        if (ga != null)
//        {
//            sendText(ga, "OK");
//        }
//        else
//        {
//            put(SOCJoinGame.toCmd(nickname, password, host, mes.getGame()));
//        }
//    }
//
//    /**
//     * handle the admin reset message
//     * @param mes  the message
//     */
//    protected void handleADMINRESET(SOCAdminReset mes)
//    {
//        D.ebugPrintln("*** Admin Reset message = " + mes);
//        disconnectReconnect();
//    }

//    /**
//     * handle the update robot params message
//     * @param mes  the message
//     */
//    protected void handleUPDATEROBOTPARAMS(SOCUpdateRobotParams mes)
//    {
//        currentRobotParameters = new SOCRobotParameters(mes.getRobotParameters());
//        D.ebugPrintln("*** current robot parameters = " + currentRobotParameters);
//    }
//
//    /**
//     * handle the "join game request" message
//     * @param mes  the message
//     */
//    protected void handleJOINGAMEREQUEST(SOCJoinGameRequest mes)
//    {
//        D.ebugPrintln("**** handleJOINGAMEREQUEST ****");
//        seatRequests.put(mes.getGame(), new Integer(mes.getPlayerNumber()));
//
//        if (put(SOCJoinGame.toCmd(nickname, password, host, mes.getGame())))
//        {
//            D.ebugPrintln("**** sent SOCJoinGame ****");
//        }
//    }
//
    /**
     * handle the "join game authorization" message
     * @param mes  the message
     */
    @Override
    protected void handleJOINGAMEAUTH(SOCJoinGameAuth mes)
    {
        gamesPlayed++;

        SOCGame ga = new SOCGame(mes.getGame(), true);
        games.put(mes.getGame(), ga);

        CappedQueue brainQ = new CappedQueue();
        brainQs.put(mes.getGame(), brainQ);

        SSRobotBrain rb = new SSRobotBrain(this, currentRobotParameters, ga, brainQ);
        
        robotBrains.put(mes.getGame(), rb);
    }

//    /**
//     * handle the "join game" message
//     * @param mes  the message
//     */
//    protected void handleJOINGAME(SOCJoinGame mes) {}
//
//    /**
//     * handle the "leave game" message
//     * @param mes  the message
//     */
//    protected void handleLEAVEGAME(SOCLeaveGame mes) {}

//    /**
//     * handle the "game members" message
//     * @param mes  the message
//     */
//    protected void handleGAMEMEMBERS(SOCGameMembers mes)
//    {
//        /**
//         * sit down to play
//         */
//        Integer pn = (Integer) seatRequests.get(mes.getGame());
//
//        try
//        {
//            //wait(Math.round(Math.random()*1000));
//        }
//        catch (Exception e)
//        {
//            ;
//        }
//
//        put(SOCSitDown.toCmd(mes.getGame(), nickname, pn.intValue(), true));
//    }

//    /**
//     * handle the "game text message" message
//     * @param mes  the message
//     */
//    protected void handleGAMETEXTMSG(SOCGameTextMsg mes)
//    {
//        //D.ebugPrintln(mes.getNickname()+": "+mes.getText());
//        if (mes.getText().startsWith(nickname + ":debug-off"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if (brain != null)
//            {
//                brain.turnOffDRecorder();
//                sendText(ga, "Debug mode OFF");
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":debug-on"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if (brain != null)
//            {
//                brain.turnOnDRecorder();
//                sendText(ga, "Debug mode ON");
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":current-plans") || mes.getText().startsWith(nickname + ":cp"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                Vector record = brain.getDRecorder().getRecord(CURRENT_PLANS);
//
//                if (record != null)
//                {
//                    Enumeration enum1 = record.elements();
//
//                    while (enum1.hasMoreElements())
//                    {
//                        String str = (String) enum1.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":current-resources") || mes.getText().startsWith(nickname + ":cr"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                Vector record = brain.getDRecorder().getRecord(CURRENT_RESOURCES);
//
//                if (record != null)
//                {
//                    Enumeration enum1 = record.elements();
//
//                    while (enum1.hasMoreElements())
//                    {
//                        String str = (String) enum1.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":last-plans") || mes.getText().startsWith(nickname + ":lp"))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                Vector record = brain.getOldDRecorder().getRecord(CURRENT_PLANS);
//
//                if (record != null)
//                {
//                    SOCGame ga = (SOCGame) games.get(mes.getGame());
//                    Enumeration enum1 = record.elements();
//
//                    while (enum1.hasMoreElements())
//                    {
//                        String str = (String) enum1.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":last-resources") || mes.getText().startsWith(nickname + ":lr"))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                Vector record = brain.getOldDRecorder().getRecord(CURRENT_RESOURCES);
//
//                if (record != null)
//                {
//                    SOCGame ga = (SOCGame) games.get(mes.getGame());
//                    Enumeration enum1 = record.elements();
//
//                    while (enum1.hasMoreElements())
//                    {
//                        String str = (String) enum1.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":last-move") || mes.getText().startsWith(nickname + ":lm"))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getOldDRecorder().isOn()))
//            {
//                SOCPossiblePiece lastMove = brain.getLastMove();
//
//                if (lastMove != null)
//                {
//                    String key = null;
//
//                    switch (lastMove.getType())
//                    {
//                    case SOCPossiblePiece.CARD:
//                        key = "DEVCARD";
//
//                        break;
//
//                    case SOCPossiblePiece.ROAD:
//                        key = "ROAD" + lastMove.getCoordinates();
//
//                        break;
//
//                    case SOCPossiblePiece.SETTLEMENT:
//                        key = "SETTLEMENT" + lastMove.getCoordinates();
//
//                        break;
//
//                    case SOCPossiblePiece.CITY:
//                        key = "CITY" + lastMove.getCoordinates();
//
//                        break;
//                    }
//
//                    Vector record = brain.getOldDRecorder().getRecord(key);
//
//                    if (record != null)
//                    {
//                        SOCGame ga = (SOCGame) games.get(mes.getGame());
//                        Enumeration enum1 = record.elements();
//
//                        while (enum1.hasMoreElements())
//                        {
//                            String str = (String) enum1.nextElement();
//                            sendText(ga, str);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":consider-move ") || mes.getText().startsWith(nickname + ":cm "))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getOldDRecorder().isOn()))
//            {
//                String[] tokens = mes.getText().split(" ");
//                String key = null;
//
//                if (tokens[1].trim().equals("card"))
//                {
//                    key = "DEVCARD";
//                }
//                else if (tokens[1].equals("road"))
//                {
//                    key = "ROAD" + tokens[2].trim();
//                }
//                else if (tokens[1].equals("settlement"))
//                {
//                    key = "SETTLEMENT" + tokens[2].trim();
//                }
//                else if (tokens[1].equals("city"))
//                {
//                    key = "CITY" + tokens[2].trim();
//                }
//
//                Vector record = brain.getOldDRecorder().getRecord(key);
//
//                if (record != null)
//                {
//                    SOCGame ga = (SOCGame) games.get(mes.getGame());
//                    Enumeration enum1 = record.elements();
//
//                    while (enum1.hasMoreElements())
//                    {
//                        String str = (String) enum1.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":last-target") || mes.getText().startsWith(nickname + ":lt"))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                SOCPossiblePiece lastTarget = brain.getLastTarget();
//
//                if (lastTarget != null)
//                {
//                    String key = null;
//
//                    switch (lastTarget.getType())
//                    {
//                    case SOCPossiblePiece.CARD:
//                        key = "DEVCARD";
//
//                        break;
//
//                    case SOCPossiblePiece.ROAD:
//                        key = "ROAD" + lastTarget.getCoordinates();
//
//                        break;
//
//                    case SOCPossiblePiece.SETTLEMENT:
//                        key = "SETTLEMENT" + lastTarget.getCoordinates();
//
//                        break;
//
//                    case SOCPossiblePiece.CITY:
//                        key = "CITY" + lastTarget.getCoordinates();
//
//                        break;
//                    }
//
//                    Vector record = brain.getDRecorder().getRecord(key);
//
//                    if (record != null)
//                    {
//                        SOCGame ga = (SOCGame) games.get(mes.getGame());
//                        Enumeration enum1 = record.elements();
//
//                        while (enum1.hasMoreElements())
//                        {
//                            String str = (String) enum1.nextElement();
//                            sendText(ga, str);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":consider-target ") || mes.getText().startsWith(nickname + ":ct "))
//        {
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain != null) && (brain.getDRecorder().isOn()))
//            {
//                String[] tokens = mes.getText().split(" ");
//                String key = null;
//
//                if (tokens[1].trim().equals("card"))
//                {
//                    key = "DEVCARD";
//                }
//                else if (tokens[1].equals("road"))
//                {
//                    key = "ROAD" + tokens[2].trim();
//                }
//                else if (tokens[1].equals("settlement"))
//                {
//                    key = "SETTLEMENT" + tokens[2].trim();
//                }
//                else if (tokens[1].equals("city"))
//                {
//                    key = "CITY" + tokens[2].trim();
//                }
//
//                Vector record = brain.getDRecorder().getRecord(key);
//
//                if (record != null)
//                {
//                    SOCGame ga = (SOCGame) games.get(mes.getGame());
//                    Enumeration enum1 = record.elements();
//
//                    while (enum1.hasMoreElements())
//                    {
//                        String str = (String) enum1.nextElement();
//                        sendText(ga, str);
//                    }
//                }
//            }
//        }
//
//        if (mes.getText().startsWith(nickname + ":stats"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            sendText(ga, "Games played:" + gamesPlayed);
//            sendText(ga, "Games finished:" + gamesFinished);
//            sendText(ga, "Games won:" + gamesWon);
//            sendText(ga, "Clean brain kills:" + cleanBrainKills);
//            sendText(ga, "Brains running: " + robotBrains.size());
//
//            Runtime rt = Runtime.getRuntime();
//            sendText(ga, "Total Memory:" + rt.totalMemory());
//            sendText(ga, "Free Memory:" + rt.freeMemory());
//        }
//
//        if (mes.getText().startsWith(nickname + ":gc"))
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//            Runtime rt = Runtime.getRuntime();
//            rt.gc();
//            sendText(ga, "Free Memory:" + rt.freeMemory());
//        }
//
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }

//    /**
//     * handle the "someone is sitting down" message
//     * @param mes  the message
//     */
//    protected void handleSITDOWN(SOCSitDown mes)
//    {
//        /**
//         * tell the game that a player is sitting
//         */
//        SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//        if (ga != null)
//        {
//            ga.addPlayer(mes.getNickname(), mes.getPlayerNumber());
//
//            /**
//             * set the robot flag
//             */
//            ga.getPlayer(mes.getPlayerNumber()).setRobotFlag(mes.isRobot());
//
//            /**
//             * let the robot brain find our player object if we sat down
//             */
//            if (nickname.equals(mes.getNickname()))
//            {
//                SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//                brain.setOurPlayerData();
//                brain.start();
//
//                /**
//                 * change our face to the robot face
//                 */
//                put(SOCChangeFace.toCmd(ga.getName(), mes.getPlayerNumber(), 0));
//            }
//        }
//    }

    /**
     * handle the "board layout" message
     * @param mes  the message
     */
    @Override
    protected void handleBOARDLAYOUT(SOCBoardLayout mes)
    {
        super.handleBOARDLAYOUT(mes);
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            SOCBoard bd = ga.getBoard();
            bd.setHexLayout(mes.getHexLayout());
            bd.setNumberLayout(mes.getNumberLayout());
            bd.setRobberHex(mes.getRobberHex());
        }
    }

    /**
     * handle the "start game" message
     * @param mes  the message
     */
    @Override
    protected void handleSTARTGAME(SOCStartGame mes) 
    {
        super.handleSTARTGAME(mes);
        SOCGame ga = (SOCGame) games.get(mes.getGame());
        sendGameToSmartSettlers(ga);
    }

//    /**
//     * handle the "delete game" message
//     * @param mes  the message
//     */
//    protected void handleDELETEGAME(SOCDeleteGame mes)
//    {
//        SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//        if (brain != null)
//        {
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//            if (ga != null)
//            {
//                if (ga.getGameState() == SOCGame.OVER)
//                {
//                    gamesFinished++;
//
//                    if (ga.getPlayer(nickname).getTotalVP() >= 10)
//                    {
//                        gamesWon++;
//                    }
//                }
//
//                brain.kill();
//                robotBrains.remove(mes.getGame());
//                brainQs.remove(mes.getGame());
//                games.remove(mes.getGame());
//            }
//        }
//    }
//
    /**
     * handle the "game state" message
     * @param mes  the message
     */
    @Override
    protected void handleGAMESTATE(SOCGameState mes)
    {
        super.handleGAMESTATE(mes);
        SOCGame ga = (SOCGame) games.get(mes.getGame());

        if (ga != null)
        {
            sendStateToSmartSettlers(ga, S_GAME);
        }
    }
//
//    /**
//     * handle the "set turn" message
//     * @param mes  the message
//     */
//    protected void handleSETTURN(SOCSetTurn mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "set first player" message
//     * @param mes  the message
//     */
//    protected void handleFIRSTPLAYER(SOCFirstPlayer mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "turn" message
//     * @param mes  the message
//     */
//    protected void handleTURN(SOCTurn mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "player element" message
//     * @param mes  the message
//     */
//    protected void handlePLAYERELEMENT(SOCPlayerElement mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle "resource count" message
//     * @param mes  the message
//     */
//    protected void handleRESOURCECOUNT(SOCResourceCount mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }

    /**
     * handle the "dice result" message
     * @param mes  the message
     */
    @Override
    protected void handleDICERESULT(SOCDiceResult mes)
    {
        super.handleDICERESULT(mes);
        SOCGame ga = (SOCGame) games.get(mes.getGame());
        sendStateToSmartSettlers(ga, S_NORMAL);
    }

//    /**
//     * handle the "put piece" message
//     * @param mes  the message
//     */
//    protected void handlePUTPIECE(SOCPutPiece mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//
//            SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//            if (ga != null)
//            {
//                SOCPlayer pl = ga.getPlayer(mes.getPlayerNumber());
//            }
//        }
//    }

//    /**
//     * handle the "move robber" message
//     * @param mes  the message
//     */
//    @Override
//    protected void handleMOVEROBBER(SOCMoveRobber mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }

//    /**
//     * handle the "discard request" message
//     * @param mes  the message
//     */
//    protected void handleDISCARDREQUEST(SOCDiscardRequest mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "choose player request" message
//     * @param mes  the message
//     */
//    protected void handleCHOOSEPLAYERREQUEST(SOCChoosePlayerRequest mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "make offer" message
//     * @param mes  the message
//     */
//    protected void handleMAKEOFFER(SOCMakeOffer mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "clear offer" message
//     * @param mes  the message
//     */
//    protected void handleCLEAROFFER(SOCClearOffer mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "reject offer" message
//     * @param mes  the message
//     */
//    protected void handleREJECTOFFER(SOCRejectOffer mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "accept offer" message
//     * @param mes  the message
//     */
//    protected void handleACCEPTOFFER(SOCAcceptOffer mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "clear trade" message
//     * @param mes  the message
//     */
//    protected void handleCLEARTRADEMSG(SOCClearTradeMsg mes) {}
//
//    /**
//     * handle the "development card count" message
//     * @param mes  the message
//     */
//    protected void handleDEVCARDCOUNT(SOCDevCardCount mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "development card action" message
//     * @param mes  the message
//     */
//    protected void handleDEVCARD(SOCDevCard mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "set played development card" message
//     * @param mes  the message
//     */
//    protected void handleSETPLAYEDDEVCARD(SOCSetPlayedDevCard mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }
//
//    /**
//     * handle the "dismiss robot" message
//     * @param mes  the message
//     */
//    protected void handleROBOTDISMISS(SOCRobotDismiss mes)
//    {
//        SOCGame ga = (SOCGame) games.get(mes.getGame());
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if ((ga != null) && (brainQ != null))
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//
//            /**
//             * if the brain isn't alive, then we need to leave
//             * the game
//             */
//            SOCRobotBrain brain = (SOCRobotBrain) robotBrains.get(mes.getGame());
//
//            if ((brain == null) || (!brain.isAlive()))
//            {
//                leaveGame((SOCGame) games.get(mes.getGame()));
//            }
//        }
//    }
//
//    /**
//     * handle the "potential settlements" message
//     * @param mes  the message
//     */
//    protected void handlePOTENTIALSETTLEMENTS(SOCPotentialSettlements mes)
//    {
//        CappedQueue brainQ = (CappedQueue) brainQs.get(mes.getGame());
//
//        if (brainQ != null)
//        {
//            try
//            {
//                brainQ.put(mes);
//            }
//            catch (CutoffExceededException exc)
//            {
//                D.ebugPrintln("CutoffExceededException" + exc);
//            }
//        }
//    }

//    /**
//     * handle the "change face" message
//     * @param mes  the message
//     */
//    protected void handleCHANGEFACE(SOCChangeFace mes)
//    {
//        SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//        if (ga != null)
//        {
//            SOCPlayer player = ga.getPlayer(mes.getPlayerNumber());
//            player.setFaceId(mes.getFaceId());
//        }
//    }

//    /**
//     * handle the "longest road" message
//     * @param mes  the message
//     */
//    protected void handleLONGESTROAD(SOCLongestRoad mes)
//    {
//        SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//        if (ga != null)
//        {
//            if (mes.getPlayerNumber() == -1)
//            {
//                ga.setPlayerWithLongestRoad((SOCPlayer) null);
//            }
//            else
//            {
//                ga.setPlayerWithLongestRoad(ga.getPlayer(mes.getPlayerNumber()));
//            }
//        }
//    }
//
//    /**
//     * handle the "largest army" message
//     * @param mes  the message
//     */
//    protected void handleLARGESTARMY(SOCLargestArmy mes)
//    {
//        SOCGame ga = (SOCGame) games.get(mes.getGame());
//
//        if (ga != null)
//        {
//            if (mes.getPlayerNumber() == -1)
//            {
//                ga.setPlayerWithLargestArmy((SOCPlayer) null);
//            }
//            else
//            {
//                ga.setPlayerWithLargestArmy(ga.getPlayer(mes.getPlayerNumber()));
//            }
//        }
//    }

//    /**
//     * the user leaves the given game
//     *
//     * @param ga   the game
//     */
//    public void leaveGame(SOCGame ga)
//    {
//        if (ga != null)
//        {
//            robotBrains.remove(ga.getName());
//            brainQs.remove(ga.getName());
//            games.remove(ga.getName());
//            put(SOCLeaveGame.toCmd(nickname, host, ga.getName()));
//        }
//    }

//    /**
//     * add one the the number of clean brain kills
//     */
//    public void addCleanKill()
//    {
//        cleanBrainKills++;
//    }
//
//    /** destroy the applet */
//    public void destroy()
//    {
//        SOCLeaveAll leaveAllMes = new SOCLeaveAll();
//        put(leaveAllMes.toCmd());
//        disconnectReconnect();
//    }

    @Override
    public boolean isSmartSettlersAgent()
    {
        return true;
    }

    /**
     * for stand-alones
     */
    public static void main(String[] args)
    {
		if (args.length < 4)
		{
			System.err.println("usage: java soc.robot.SOCRobotClient host port_number userid password");

			return;
		}
    	
        SSRobotClient ex1 = new SSRobotClient(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        ex1.init();
    }
}
