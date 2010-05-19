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
package soc.message;

import java.io.Serializable;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
 * Messages used for chatting on a channel
 *
 * @author Robert S Thomas
 */
public abstract class SOCMessage implements Serializable, Cloneable
{
    /**
     * message type IDs
     */
    public static final int NULLMESSAGE = 1000;
    public static final int NEWCHANNEL = 1001;
    public static final int MEMBERS = 1002;
    public static final int CHANNELS = 1003;
    public static final int JOIN = 1004;
    public static final int TEXTMSG = 1005;
    public static final int LEAVE = 1006;
    public static final int DELETECHANNEL = 1007;
    public static final int LEAVEALL = 1008;
    public static final int PUTPIECE = 1009;
    public static final int GAMETEXTMSG = 1010;
    public static final int LEAVEGAME = 1011;
    public static final int SITDOWN = 1012;
    public static final int JOINGAME = 1013;
    public static final int BOARDLAYOUT = 1014;
    public static final int DELETEGAME = 1015;
    public static final int NEWGAME = 1016;
    public static final int GAMEMEMBERS = 1017;
    public static final int STARTGAME = 1018;
    public static final int GAMES = 1019;
    public static final int JOINAUTH = 1020;
    public static final int JOINGAMEAUTH = 1021;
    public static final int IMAROBOT = 1022;
    public static final int JOINGAMEREQUEST = 1023;
    public static final int PLAYERELEMENT = 1024;
    public static final int GAMESTATE = 1025;
    public static final int TURN = 1026;
    public static final int SETUPDONE = 1027;
    public static final int DICERESULT = 1028;
    public static final int DISCARDREQUEST = 1029;
    public static final int ROLLDICEREQUEST = 1030;
    public static final int ROLLDICE = 1031;
    public static final int ENDTURN = 1032;
    public static final int DISCARD = 1033;
    public static final int MOVEROBBER = 1034;
    public static final int CHOOSEPLAYER = 1035;
    public static final int CHOOSEPLAYERREQUEST = 1036;
    public static final int REJECTOFFER = 1037;
    public static final int CLEAROFFER = 1038;
    public static final int ACCEPTOFFER = 1039;
    public static final int BANKTRADE = 1040;
    public static final int MAKEOFFER = 1041;
    public static final int CLEARTRADEMSG = 1042;
    public static final int BUILDREQUEST = 1043;
    public static final int CANCELBUILDREQUEST = 1044;
    public static final int BUYCARDREQUEST = 1045;
    public static final int DEVCARD = 1046;
    public static final int DEVCARDCOUNT = 1047;
    public static final int SETPLAYEDDEVCARD = 1048;
    public static final int PLAYDEVCARDREQUEST = 1049;
    public static final int DISCOVERYPICK = 1052;
    public static final int MONOPOLYPICK = 1053;
    public static final int FIRSTPLAYER = 1054;
    public static final int SETTURN = 1055;
    public static final int ROBOTDISMISS = 1056;
    public static final int POTENTIALSETTLEMENTS = 1057;
    public static final int CHANGEFACE = 1058;
    public static final int REJECTCONNECTION = 1059;
    public static final int LASTSETTLEMENT = 1060;
    public static final int GAMESTATS = 1061;
    public static final int BCASTTEXTMSG = 1062;
    public static final int RESOURCECOUNT = 1063;
    public static final int ADMINPING = 1064;
    public static final int ADMINRESET = 1065;
    public static final int LONGESTROAD = 1066;
    public static final int LARGESTARMY = 1067;
    public static final int SETSEATLOCK = 1068;
    public static final int STATUSMESSAGE = 1069;
    public static final int CREATEACCOUNT = 1070;
    public static final int UPDATEROBOTPARAMS = 1071;
    public static final int SERVERPING = 9999;

    public static final int IMSMARTSETTLERS = 1072;
    
    /**
     * token seperators
     */
    protected static final String sep = "|";
    protected static String sep2 = ",";

    /**
     * An ID identifying the type of message
     */
    protected int messageType;

    /**
     * @return  the message type
     */
    public int getType()
    {
        return messageType;
    }

    /**
     * Converts the contents of this message into
     * a String that can be transferred by a client
     * or server.
     */
    public abstract String toCmd();

    /**
     * Convert a string into a SOCMessage
     * The string is in the form of "<ID> sep <message name> sep <message data>"
     *
     * @param s  String to convert
     * @return   converted String to a SOCMessage, or null if the string is garbled
     */
    public static SOCMessage toMsg(String s)
    {
        try
        {
            StringTokenizer st = new StringTokenizer(s, sep);

            /**
             * get the id that identifies the type of message
             */
            int msgId = Integer.parseInt(st.nextToken());

            /**
             * get the rest of the data
             */
            String data;

            try
            {
                data = st.nextToken();
            }
            catch (NoSuchElementException e)
            {
                data = "";
            }

            /**
             * convert the data part and create the message
             */
            switch (msgId)
            {
            case NULLMESSAGE:
                return null;

            case NEWCHANNEL:
                return SOCNewChannel.parseDataStr(data);

            case MEMBERS:
                return SOCMembers.parseDataStr(data);

            case CHANNELS:
                return SOCChannels.parseDataStr(data);

            case JOIN:
                return SOCJoin.parseDataStr(data);

            case TEXTMSG:
                return SOCTextMsg.parseDataStr(data);

            case LEAVE:
                return SOCLeave.parseDataStr(data);

            case DELETECHANNEL:
                return SOCDeleteChannel.parseDataStr(data);

            case LEAVEALL:
                return SOCLeaveAll.parseDataStr(data);

            case PUTPIECE:
                return SOCPutPiece.parseDataStr(data);

            case GAMETEXTMSG:
                return SOCGameTextMsg.parseDataStr(data);

            case LEAVEGAME:
                return SOCLeaveGame.parseDataStr(data);

            case SITDOWN:
                return SOCSitDown.parseDataStr(data);

            case JOINGAME:
                return SOCJoinGame.parseDataStr(data);

            case BOARDLAYOUT:
                return SOCBoardLayout.parseDataStr(data);

            case GAMES:
                return SOCGames.parseDataStr(data);

            case DELETEGAME:
                return SOCDeleteGame.parseDataStr(data);

            case NEWGAME:
                return SOCNewGame.parseDataStr(data);

            case GAMEMEMBERS:
                return SOCGameMembers.parseDataStr(data);

            case STARTGAME:
                return SOCStartGame.parseDataStr(data);

            case JOINAUTH:
                return SOCJoinAuth.parseDataStr(data);

            case JOINGAMEAUTH:
                return SOCJoinGameAuth.parseDataStr(data);

            case IMAROBOT:
                return SOCImARobot.parseDataStr(data);

            case JOINGAMEREQUEST:
                return SOCJoinGameRequest.parseDataStr(data);

            case PLAYERELEMENT:
                return SOCPlayerElement.parseDataStr(data);

            case GAMESTATE:
                return SOCGameState.parseDataStr(data);

            case TURN:
                return SOCTurn.parseDataStr(data);

            case SETUPDONE:
                return SOCSetupDone.parseDataStr(data);

            case DICERESULT:
                return SOCDiceResult.parseDataStr(data);

            case DISCARDREQUEST:
                return SOCDiscardRequest.parseDataStr(data);

            case ROLLDICEREQUEST:
                return SOCRollDiceRequest.parseDataStr(data);

            case ROLLDICE:
                return SOCRollDice.parseDataStr(data);

            case ENDTURN:
                return SOCEndTurn.parseDataStr(data);

            case DISCARD:
                return SOCDiscard.parseDataStr(data);

            case MOVEROBBER:
                return SOCMoveRobber.parseDataStr(data);

            case CHOOSEPLAYER:
                return SOCChoosePlayer.parseDataStr(data);

            case CHOOSEPLAYERREQUEST:
                return SOCChoosePlayerRequest.parseDataStr(data);

            case REJECTOFFER:
                return SOCRejectOffer.parseDataStr(data);

            case CLEAROFFER:
                return SOCClearOffer.parseDataStr(data);

            case ACCEPTOFFER:
                return SOCAcceptOffer.parseDataStr(data);

            case BANKTRADE:
                return SOCBankTrade.parseDataStr(data);

            case MAKEOFFER:
                return SOCMakeOffer.parseDataStr(data);

            case CLEARTRADEMSG:
                return SOCClearTradeMsg.parseDataStr(data);

            case BUILDREQUEST:
                return SOCBuildRequest.parseDataStr(data);

            case CANCELBUILDREQUEST:
                return SOCCancelBuildRequest.parseDataStr(data);

            case BUYCARDREQUEST:
                return SOCBuyCardRequest.parseDataStr(data);

            case DEVCARD:
                return SOCDevCard.parseDataStr(data);

            case DEVCARDCOUNT:
                return SOCDevCardCount.parseDataStr(data);

            case SETPLAYEDDEVCARD:
                return SOCSetPlayedDevCard.parseDataStr(data);

            case PLAYDEVCARDREQUEST:
                return SOCPlayDevCardRequest.parseDataStr(data);

            case DISCOVERYPICK:
                return SOCDiscoveryPick.parseDataStr(data);

            case MONOPOLYPICK:
                return SOCMonopolyPick.parseDataStr(data);

            case FIRSTPLAYER:
                return SOCFirstPlayer.parseDataStr(data);

            case SETTURN:
                return SOCSetTurn.parseDataStr(data);

            case ROBOTDISMISS:
                return SOCRobotDismiss.parseDataStr(data);

            case POTENTIALSETTLEMENTS:
                return SOCPotentialSettlements.parseDataStr(data);

            case CHANGEFACE:
                return SOCChangeFace.parseDataStr(data);

            case REJECTCONNECTION:
                return SOCRejectConnection.parseDataStr(data);

            case LASTSETTLEMENT:
                return SOCLastSettlement.parseDataStr(data);

            case GAMESTATS:
                return SOCGameStats.parseDataStr(data);

            case BCASTTEXTMSG:
                return SOCBCastTextMsg.parseDataStr(data);

            case RESOURCECOUNT:
                return SOCResourceCount.parseDataStr(data);

            case ADMINPING:
                return SOCAdminPing.parseDataStr(data);

            case ADMINRESET:
                return SOCAdminReset.parseDataStr(data);

            case LONGESTROAD:
                return SOCLongestRoad.parseDataStr(data);

            case LARGESTARMY:
                return SOCLargestArmy.parseDataStr(data);

            case SETSEATLOCK:
                return SOCSetSeatLock.parseDataStr(data);

            case STATUSMESSAGE:
                return SOCStatusMessage.parseDataStr(data);

            case CREATEACCOUNT:
                return SOCCreateAccount.parseDataStr(data);

            case UPDATEROBOTPARAMS:
                return SOCUpdateRobotParams.parseDataStr(data);

            case SERVERPING:
                return SOCServerPing.parseDataStr(data);

            default:
                return null;
            }
        }
        catch (Exception e)
        {
            System.err.println("toMsg ERROR - " + e);
            e.printStackTrace();

            return null;
        }
    }
    
     /**
     * @return the name of the game
     */
    public String getGame()
    {
        return null;
    }

}
