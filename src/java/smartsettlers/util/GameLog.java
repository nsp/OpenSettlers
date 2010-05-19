/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.util;

import java.util.ArrayList;
import smartsettlers.boardlayout.GameStateConstants;
import smartsettlers.boardlayout.HexTypeConstants;


/**
 *
 * @author szityu
 */
public class GameLog implements GameStateConstants, HexTypeConstants {

        public int size;
        
        public ArrayList states, actions;
        
        public GameLog()
        {
            size = 0;
            states = new ArrayList();
            actions = new ArrayList();
        }
        
        public void clear()
        {
            size = 0;
            states.clear();
            actions.clear();
        }
        
        public void addState(int[] state)
        {
            int[] s = state.clone();
            states.add(s);
        }

        public void addAction(int[] action)
        {
            int [] a = action.clone();
            actions.add(a);
            size++;
        }
        
        //    public final static int PORT_MISC       = 1;
        //    public final static int PORT_SHEEP      = 2;
        //    public final static int PORT_WOOD       = 3;
        //    public final static int PORT_CLAY       = 4;
        //    public final static int PORT_WHEAT      = 5;
        //    public final static int PORT_STONE      = 6;    
        //String[] resourceStrings = {"sheep","wood","clay","wheat","stone"};

        public String toString(int index)
        {
        if ((size == 0) || (index<0))
        {
            return "Empty log";
        } else {
            String str = null;
            String str2 = null;
            int[] s = (int[]) states.get(index);
            int[] s2 = (int[]) states.get(index+1);
            int[] a = (int[]) actions.get(index);
            int fsmlevel = s[OFS_FSMLEVEL];
            int fsmstate = s[OFS_FSMSTATE + fsmlevel];
            int pl = s[OFS_FSMPLAYER + fsmlevel];
            int i, val1, val2, val3;

            str = "" + index+ ". Player " + pl + ": ";
            switch (fsmstate) {
                case S_GAME:
                    str = "Game start";
                    break;
                case S_SETTLEMENT1:
                    str = str + "Place settlement 1 at pos#"+a[1];
                    break;
                case S_SETTLEMENT2:
                    str = str + "Place settlement 2 at pos#"+a[1];
                    break;
                case S_ROAD1:
                    str = str + "Place road 1";
                    break;
                case S_ROAD2:
                    str = str + "Place road 2";
                    break;
                case S_BEFOREDICE:
                    if (a[0]==A_THROWDICE)
                    {
                        str = str + "Throw dice: "+s2[OFS_DIE1]+"+"+s2[OFS_DIE2]+" = "+(s2[OFS_DIE1]+s2[OFS_DIE2]);
                    }
                    else
                    {
                        str = str + "S_BEFOREDICE";
                    }
                    break;
                case S_FREEROAD1:
                    str = str + "Build free road 1 at pos#"+a[1];
                    break;
                case S_FREEROAD2:
                    str = str + "Build free road 2 at pos#"+a[1];
                    break;
                case S_PAYTAX:
                    if (a[1]==0)
                        str = str + "Do not need to pay tax";
                    else
                    {
                        val2 = 1;
                        str = str + "pay " + a[1]+ " tax:";
                        for (i=0; i<NRESOURCES; i++)
                        {
                            val1 = s[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i] -
                                     s2[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i];
                            if (val1 != 0)
                            {
                                if (val2==1)
                                {
                                    val2 = 0;
                                    str = str + " ";
                                }
                                else
                                    str = str + ", ";
                                str = str + val1+ " "+resourceNames[i];
                            }
                        }
                    }
                    break;
                case S_ROBBERAT7:
                    str = str + "Robber to hex #" + a[1] + ", ";
                    if (a[2] == -1)
                        str = str + "do not steal";
                    else if (a[3] == -1)
                        str = str + "steal from Pl."+a[2]+ ", who has no cards";
                    else
                        str = str + "steal a " + resourceNames[a[3]] + " from Pl."+a[2];
                    break;
                case S_NORMAL:
                    //str = str + "S_NORMAL";
                    switch (a[0])
                    {
                        case A_BUILDROAD:
                            str = str + "Build road at pos#"+a[1];
                            break;
                        case A_BUILDSETTLEMENT:
                            str = str + "Build settlement at pos#"+a[1];
                            break;
                        case A_BUILDCITY:
                            str = str + "Build city at pos#"+a[1];
                            break;
                        case A_ENDTURN:
                            str = str + "End turn";
                            break;
                        case A_PORTTRADE:
                            if (a[1]==2) str2 = " at special port";
                            else if (a[1]==3) str2 = " at general port";
                            else if (a[1]==4) str2 = " at bank";
                            str = str + "Trade "+a[1]+" "+resourceNames[a[2]] + " to "
                                    + a[3]+" "+resourceNames[a[4]] + str2;
                            break;
                        case A_BUYCARD:
                            str = str + "Buy development card: " + cardNames[a[1]];
                            break;
                        case A_PLAYCARD_FREERESOURCE:
                            if (a[1]==a[2])
                                str = str + "Take free res.: 2 " + resourceNames[a[1]];
                            else
                                str = str + "Take free res.: 1 " + resourceNames[a[1]]
                                        + " and 1" + resourceNames[a[2]];
                            break;
                        case A_PLAYCARD_MONOPOLY:
                            str = str + "Declare monopoly on " + resourceNames[a[1]];
                            break;
                        case A_PLAYCARD_FREEROAD:
                            str = str + "2 free roads";
                            break;
                        case A_PLAYCARD_KNIGHT:
                            str = str + "Robber to hex #" + a[1] + ", ";
                            if (a[2] == -1)
                                str = str + "do not steal";
                            else if (a[3] == -1)
                                str = str + "steal from Pl."+a[2]+ ", who has no cards";
                            else
                                str = str + "steal a " + resourceNames[a[3]] + " from Pl."+a[2];
                    }
                    break;    
                case S_FINISHED:
                    val1 = -1;
                    for (pl=0; pl<NPLAYERS;pl++)
                    {
                        if (s[OFS_PLAYERDATA[pl] + OFS_SCORE] >= 10)
                            val1 = pl;
                    }
                    
                    str = str + "Finished, Player " + val1 + " wins.";
            }

            return str;
        }
    }
        
    @Override
        public String toString()
        {
            return toString(size-1);
        }
}
