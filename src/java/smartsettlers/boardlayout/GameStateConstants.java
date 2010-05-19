/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.boardlayout;

import java.awt.Color;

/**
 *
 * @author szityu
 */
public interface GameStateConstants extends HexTypeConstants {

    int NPLAYERS = 4;
    
    int N_VERTICES              = 54;
    int N_HEXES                 = 19;
    int N_EDGES                 = 72;
    int N_RESOURCES             = 5;
    int N_DEVCARDTYPES          = 5;

    int OFS_TURN                = 0;
    int OFS_FSMLEVEL            = OFS_TURN          +1 ;
    int OFS_FSMSTATE            = OFS_FSMLEVEL      +1 ;
    int OFS_FSMPLAYER           = OFS_FSMSTATE      +3 ;
    int OFS_NCARDSGONE          = OFS_FSMPLAYER     +3 ;
    int OFS_DIE1                = OFS_NCARDSGONE    +1 ;
    int OFS_DIE2                = OFS_DIE1          +1 ;
    int OFS_ROBBERPLACE         = OFS_DIE2          +1 ;
    int OFS_LONGESTROAD_AT      = OFS_ROBBERPLACE   +1 ;
    int OFS_LARGESTARMY_AT      = OFS_LONGESTROAD_AT   +1 ;
    int OFS_LASTVERTEX          = OFS_LARGESTARMY_AT   +1 ;
    int OFS_EDGES               = OFS_LASTVERTEX    +1 ;
    int OFS_VERTICES            = OFS_EDGES         +N_EDGES ;
    //int OFS_EDGEACCESSIBLE      = OFS_VERTICES      +N_VERTICES;
    //int OFS_VERTEXACCESSIBLE    = OFS_EDGEACCESSIBLE+N_EDGES;
            
    
        int OFS_SCORE               = 0;
        int OFS_NSETTLEMENTS        = 1;
        int OFS_NCITIES             = 2;
        int OFS_NROADS              = 3;
        int OFS_PLAYERSLONGESTROAD  = 4;
        int OFS_HASPLAYEDCARD       = 5;
        int OFS_RESOURCES           = OFS_HASPLAYEDCARD   +1;
        int OFS_ACCESSTOPORT        = OFS_RESOURCES     +NRESOURCES;
        int OFS_USEDCARDS           = OFS_ACCESSTOPORT  +(NRESOURCES+1);
        int OFS_OLDCARDS            = OFS_USEDCARDS     +N_DEVCARDTYPES;
        int OFS_NEWCARDS            = OFS_OLDCARDS      +N_DEVCARDTYPES;
        int PLAYERSTATESIZE         = OFS_NEWCARDS      +N_DEVCARDTYPES;
    
    int[] OFS_PLAYERDATA        = { OFS_VERTICES+N_VERTICES,
                                    OFS_VERTICES+N_VERTICES + PLAYERSTATESIZE,
                                    OFS_VERTICES+N_VERTICES + 2*PLAYERSTATESIZE,
                                    OFS_VERTICES+N_VERTICES + 3*PLAYERSTATESIZE};    
    int STATESIZE = OFS_VERTICES+N_VERTICES + 4*PLAYERSTATESIZE;
    int ACTIONSIZE = 5;
            
    // todo: who has longest road, biggest army, where is the robber
    
    
    int S_GAME                  =  0;
    int S_START                 =  1;
    int S_SETTLEMENT1           =  2;
    int S_ROAD1                 =  3;
    int S_SETTLEMENT2           =  4;
    int S_ROAD2                 =  5;
    //int S_THROWDICE             =  6;
    int S_BEFOREDICE            =  6;
    int S_NORMAL                = 100;
    int S_BUYSETTLEMENT         =  7;
    int S_BUYROAD               =  8;
    int S_BUYCARD               =  9;
    int S_BUYCITY               = 10;
    int S_PAYTAX                = 11;
    int S_KNIGHT                = 12;
    int S_FREEROAD1             = 13;
    int S_FREEROAD2             = 14;
    int S_ROBBERAT7             = 15;
    int S_FINISHED              = 101;
    
            
//    int S_                      = 
    
    int A_NOTHING               = 0;
    int A_BUILDSETTLEMENT       = 1;
    int A_BUILDROAD             = 2;
    int A_BUILDCITY             = 3;
    int A_THROWDICE             = 4;
    int A_ENDTURN               = 5;
    int A_PORTTRADE             = 6;
    int A_BUYCARD               = 7;
    int A_PLAYCARD_KNIGHT       = 8;
    int A_PLAYCARD_FREEROAD     = 9;
    int A_PLAYCARD_FREERESOURCE = 10;
    int A_PLAYCARD_MONOPOLY     = 11;
    int A_PAYTAX                = 12;
    int A_PLACEROBBER           = 13;
    
    
    
    int VERTEX_EMPTY            = 0;
    int VERTEX_TOOCLOSE         = 1;
    int VERTEX_HASSETTLEMENT    = 2; //+player number
    int VERTEX_HASCITY          = 6; //+player number
    
    int EDGE_EMPTY              = 0;
    int EDGE_OCCUPIED           = 1; //+player number
    
    
    int CARD_KNIGHT             = 0;
    int CARD_ONEPOINT           = 1;
    int CARD_FREEROAD           = 2;
    int CARD_FREERESOURCE       = 3;
    int CARD_MONOPOLY           = 4;
    
    int NCARDTYPES              = 5;
    int NCARDS                  = 25;
    
    String[] resourceNames = {"sheep", "wood", "clay", "wheat", "stone"};
    String[] cardNames = {"knight", "+1 point", "+2 road", "+2 res.", "monopoly"};
    
    public final static Color[] playerColor = 
    {
        Color.BLUE,
        Color.RED,
        Color.WHITE,
        Color.ORANGE,
    };

}
