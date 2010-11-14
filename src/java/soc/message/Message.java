/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Documentation paragraphs and other portions of this file Copyright (C) 2007-2010 Jeremy D Monin <jeremy@nand.net>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.message;

import java.io.Serializable;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/**
 * Messages used for game data, events, and chatting on a channel.
 *<P>
 * No objects, only strings and integers, are to be sent over the network
 * between servers and clients!  Your game's code must guarantee that no string
 * sent contains a separator character ({@link #sep_char} or {@link #sep2_char}).
 * To help with this, use {@link #isSingleLineAndSafe(String)}.
 *<P>
 * Text announcements ({@link GameTextMsg}) are often sent along with
 * data messages.
 *<P>
 * The message data is sent over the network as type ID + data strings
 * built by each Message subclass's toCmd() method.  
 *<P>
 * On the remote end, it's reconstructed to a new instance of the
 * appropriate Message subclass, by the subclass' required method
 * static SOCMessageSubclass parseDataStr(String).
 * parseDataStr is called from {@link #toMsg(String)} in this class.
 *<P>
 * The client receives messages in {@link soc.client.PlayerClient#treat(Message, boolean)}.
 * The server receives messages in {@link soc.server.SOCServer#processCommand(String, StringConnection)}.
 *<P>
 * To create and add a new message type:
 *<UL>
 * <LI> Decide the message type name.  Add to the end of the constant list in this
 *      class.  Add a comment to note the OpenSettlers version in which it was introduced, and the date.
 * <LI> If the new message is for something that any kind of game can use,
 *      give it the next available type ID number in the list (10xx).
 *      If the message is specific to the OpenSettlers game and its interface,
 *      use a message number above 10000.  The intention is that other kinds of games
 *      can be played eventually within this server framework.
 * <LI> Add it to the switch in {@link #toMsg(String)}.  Again, note the version.
 *      Do not add if (TODO what instead??) extends MessageTemplateMs or MessageTemplateMi
 * <LI> Extend the Message class, including the required parseDataStr method.
 *      ({@link DiceResult} and {@link SetTurn} are good example subclasses.)
 *      Template parent-classes can help; the example subclasses extend them.
 *      Be sure to override the minimum version reported in {@link #getMinimumVersion()}.
 * <LI> Add to the switch in PlayerClient.treat and/or SOCServer.processCommand.
 *      Note the OpenSettlers version with a comment.
 *      <P>
 *      <em>Note:</em> Most things added to PlayerClient.treat should also be added to
 *      {@link soc.client.DisplaylessPlayerClient#treat(Message)},
 *      to {@link soc.robot.RobotClient#treat(Message)},
 *      and possibly to {@link soc.robot.RobotBrain#run()}. 
 *</UL>
 *<P>
 * Backwards compatability: Unknown message types are ignored by client and by server.
 * Technically they are returned as null from {@link #toMsg(String)} if the local copy
 * of Message doesn't know that message type.
 *<P>
 * Format:
 * For most messages, at most one {@link #sep} token per message, which separates the messagetype number
 * from the message data; multiple SEP2 are allowed after SEP.
 * For multi-messages, multiple SEP are allowed; see {@link MessageMulti}.
 *
 * @author Robert S Thomas
 */
public abstract class Message implements Serializable, Cloneable
{
    private static final long serialVersionUID = 8292403866682412441L;
    /**
     * message type IDs.
     * This list of constants does not provide javadocs, instead please see
     * the Message subclass for the message type.
     * Example: For {@link #DELETEGAME}, see javadocs for {@link DeleteGame}.
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
    public static final int ROLLDICEPROMPT = 1072;     // autoroll, 20071003, sf patch #1812254
    public static final int RESETBOARDREQUEST = 1073;  // resetboard, 20080217, sf patch#tbd
    public static final int RESETBOARDAUTH = 1074;     // resetboard, 20080217, sf patch#tbd
    public static final int RESETBOARDVOTEREQUEST = 1075; // resetboard, 20080223, sf patch#tbd
    public static final int RESETBOARDVOTE = 1076;     // resetboard, 20080223, sf patch#tbd
    public static final int RESETBOARDREJECT = 1077;   // resetboard, 20080223, sf patch#tbd

    /** @since 1.1.07 */
    public static final int NEWGAMEWITHOPTIONSREQUEST = 1078;  // gameoptions, 20090601

    /** @since 1.1.07 */
    public static final int NEWGAMEWITHOPTIONS = 1079;  // gameoptions, 20090601

    /** @since 1.1.07 */
    public static final int GAMEOPTIONGETDEFAULTS = 1080;  // gameoptions, 20090601

    /** @since 1.1.07 */
    public static final int GAMEOPTIONGETINFOS = 1081;  // gameoptions, 20090601

    /** @since 1.1.07 */
    public static final int GAMEOPTIONINFO = 1082;  // gameoptions, 20090601

    /** @since 1.1.07 */
    public static final int GAMESWITHOPTIONS = 1083;  // gameoptions, 20090601

    /** @since 1.1.08 */
    public static final int BOARDLAYOUT2 = 1084;  // 6-player board, 20091104

    /** @since 1.1.09 */
    public static final int PLAYERSTATS = 1085;  // per-player statistics, 20100312, v1.1.09

    /** @since 1.1.09 */
    public static final int PLAYERELEMENTS = 1086;  // multiple PLAYERELEMENT, 20100313, v1.1.09


    /////////////////////////////////////////
    // REQUEST FOR FUTURE MESSAGE NUMBERS: //
    /////////////////////////////////////////
    // Gametype-specific messages (opensettlers) above 10000;
    // messages applicable to any game (game options, etc) in current low-1000s range.
    // Please see class javadoc.
    /////////////////////////////////////////


    /** @since 1.1.00 */
    public static final int VERSION = 9998;   // cli-serv versioning, 20080807, v1.1.00
    public static final int GETSTATISTICS = 10001;
    public static final int SHOWSTATS = 10002;
    public static final int RESETSTATS = 10003;
    public static final int SERVERPING = 9999;  // available in all versions

    public static final int IMSMARTSETTLERS = 10000;
    
    /**
     * Token separators. At most one SEP per message; multiple SEP2 are allowed after SEP.
     * For multi-messages, multiple SEP are allowed; see {@link MessageMulti}.
     * SEP is "|".
     */
    public static final String sep = "|";
    /** secondary separator token SEP2, as string. SEP2 is ",". */
    public static final String sep2 = ",";
    /** main separator token {@link #sep}, as character. SEP is '|'. */
    public static final char sep_char = '|';
    /** secondary separator token {@link #sep2}, as character. SEP2 is ','. */
    public static final char sep2_char = ',';

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
     * To identify new message types, give the minimum version where this
     * type is used.  Default of 1000 (version 1.0.00) unless overridden.
     * @return Version number, as in 1006 for OpenSettlers 1.0.06.
     */
    public int getMinimumVersion()
    {
        return 1000;
    }

    /**
     * To identify obsolete message types, give the maximum version where this
     * type is used.  Default (for active messages) returns {@link Integer#MAX_VALUE}.
     * @return Version number, as in 1006 for OpenSettlers 1.0.06, or {@link Integer#MAX_VALUE}.
     */
    public int getMaximumVersion()
    {
        return Integer.MAX_VALUE;
    }

    /**
     * Converts the contents of this message into
     * a String that can be transferred by a client
     * or server.
     * Your class' required method
     * static SOCMessageSubclass parseDataStr(String)
     * must be able to turn this String
     * back into an instance of the message class.
     *<P>
     * For most message types, at most one {@link #sep} token is allowed,
     * separating the type ID from the rest of the parameters.
     * For multi-messages (@link MessageMulti}, multiple {@link #sep} tokens
     * are allowed.  Multi-messages are parsed with:
     * static SOCMessageSubclass parseDataStr(String[])
     */
    public abstract String toCmd();

    /** Simple human-readable representation, used for debug purposes. */
    public abstract String toString();

    /**
     * For use in toString: Append int array contents to stringbuffer,
     * formatted as "{ 1 2 3 4 5 }".
     * @param ia  int array to append. 0 length is allowed, null is not.
     * @param sb  StringBuffer to which <tt>ia</tt> will be appended, as "{ 1 2 3 4 5 }"
     * @throws NullPointerException if <tt>ia</tt> or <tt>sb</tt> is null
     * @since 1.1.09
     */
    protected static void arrayIntoStringBuf(final int[] ia, StringBuffer sb)
        throws NullPointerException
    {
        sb.append("{");
        for (int i = 0; i < ia.length; ++i)
        {
            sb.append(' ');
            sb.append(ia[i]);
        }
        sb.append(" }");
    }

    /**
     * For use in toString: Append string enum contents to stringbuffer,
     * formatted as "a,b,c,d,e".
     * @param sv  Enum of String to append. 0 length is allowed, null is not allowed.
     * @param sb  StringBuffer to which <tt>se</tt> will be appended, as "a,b,c,d,e"
     * @throws ClassCastException if <tt>se.nextElement()</tt> returns non-String
     * @throws NullPointerException if <tt>se</tt> or <tt>sb</tt> is null
     * @since 1.1.09
     */
    protected static void enumIntoStringBuf(final Enumeration se, StringBuffer sb)
        throws ClassCastException, NullPointerException
    {
        if (! se.hasMoreElements())
            return;
        try
        {
            sb.append ((String) se.nextElement());

            while (se.hasMoreElements())
            {
                sb.append(',');
                sb.append((String) se.nextElement());
            }
        }
        catch (ClassCastException cce) { throw cce; }
        catch (Exception e) {}
    }

    /**
     * Utility, get the short simple name of the class: ResetBoardVote, not soc.message.ResetBoardVote 
     * @return Short name of class, without package name
     * @since 1.1.01
     */
    public String getClassNameShort()
    {
        String clName = getClass().getName();
        int dot = clName.lastIndexOf(".");
        if (dot > 0)
            clName = clName.substring(dot + 1);
        return clName;
    }

    /**
     * Test whether a string is non-empty and its characters are
     * all 'safe' as a single-line string:
     * No newlines or {@link Character#isISOControl(char) control characters},
     * no {@link Character#isSpaceChar(char) line separators or paragraph separators}.
     * Whitespace character type {@link Character#SPACE_SEPARATOR} is OK.
     * Must not contain {@link #sep_char} or {@link #sep2_char}.
     * @param s   string to test; if null, returns false.
     * @return true if all characters are OK, false otherwise.
     *            Null string or 0-length string returns false.
     * @since 1.1.07
     */
    public static final boolean isSingleLineAndSafe(String s)
    {
        if (s == null)
            return false;
        if ((-1 != s.indexOf(sep_char))
            || (-1 != s.indexOf(sep2_char)))
            return false;
        int i = s.length();
        if (i == 0)
            return false;
        --i;
        for (; i>=0; --i)
        {
            final char c = s.charAt(i);
            if (Character.isISOControl(c) || 
                (Character.isSpaceChar(c) && (Character.getType(c) != Character.SPACE_SEPARATOR)))
                return false;
        }
        return true;
    }

    /**
     * Utility, place one string into a new single-element array.
     * To assist with {@link MessageMulti} parsing.
     *
     * @param s  String to place into array, or null
     * @return New single-element array containing s, or null if s null.
     */
    public static String[] toSingleElemArray(String s)
    {
        if (s == null)
            return null;
        String[] sarr = new String[1];
        sarr[0] = s;
        return sarr;
    }

    /**
     * Convert a string into a Message.
     * The string is in the form of "id SEP messagename {SEP2 messagedata}*".
     * If the message type id is unknown, this is printed to System.err.
     *
     * @param s  String to convert
     * @return   converted String to a Message, or null if the string is garbled,
     *           or is an unknown command id
     */
    public static Message toMsg(String s)
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

            /**
             * to handle {@link MessageMulti} subclasses -
             * multiple parameters with sub-fields.
             * If only one param is seen, this will be null;
             * use {@link #toSingleElemArray(String)} to build it.
             *<P>
             * Note that if you passed a non-null gamename to the
             * {@link MessageTemplateMs} or {@link MessageTemplateMi} constructor,
             * then multiData[0] here will be gamename,
             * and multiData[1] == param[0] as passed to that constructor.
             *<code>
             *     case POTENTIALSETTLEMENTS:
             *         if (multiData == null)
             *             multiData = toSingleElemArray(data);
             *         return PotentialSettlements.parseDataStr(multiData);
             *</code>
             */
            String[] multiData = null; 

            try
            {
                data = st.nextToken();
                if (st.hasMoreTokens())
                {
                        // MessageMulti

                        int n = st.countTokens();  // remaining (== number of parameters after "data")
                        multiData = new String[n+1];
                        multiData[0] = data;
                        for (int i = 1; st.hasMoreTokens(); ++i)
                        {
                                try {
                                        multiData[i] = st.nextToken();
                                } catch (NoSuchElementException e)
                                {
                                        multiData[i] = null;
                                }
                        }
                }
            }
            catch (NoSuchElementException e)
            {
                data = "";
            }

            // convert the data part and create the message
            switch (msgId)
            {
            case NULLMESSAGE:
                return null;

            case NEWCHANNEL:
                return NewChannel.parseDataStr(data);

            case MEMBERS:
                return Members.parseDataStr(data);

            case CHANNELS:
                return Channels.parseDataStr(data);

            case JOIN:
                return Join.parseDataStr(data);

            case TEXTMSG:
                return TextMsg.parseDataStr(data);

            case LEAVE:
                return Leave.parseDataStr(data);

            case DELETECHANNEL:
                return DeleteChannel.parseDataStr(data);

            case LEAVEALL:
                return LeaveAll.parseDataStr(data);

            case PUTPIECE:
                return PutPiece.parseDataStr(data);

            case GAMETEXTMSG:
                return GameTextMsg.parseDataStr(data);

            case LEAVEGAME:
                return LeaveGame.parseDataStr(data);

            case SITDOWN:
                return SitDown.parseDataStr(data);

            case JOINGAME:
                return JoinGame.parseDataStr(data);

            case BOARDLAYOUT:
                return BoardLayout.parseDataStr(data);

            case GAMES:
                return Games.parseDataStr(data);

            case DELETEGAME:
                return DeleteGame.parseDataStr(data);

            case NEWGAME:
                return NewGame.parseDataStr(data);

            case GAMEMEMBERS:
                return GameMembers.parseDataStr(data);

            case STARTGAME:
                return StartGame.parseDataStr(data);

            case JOINAUTH:
                return JoinAuth.parseDataStr(data);

            case JOINGAMEAUTH:
                return JoinGameAuth.parseDataStr(data);

            case IMAROBOT:
                return ImARobot.parseDataStr(data);

            case JOINGAMEREQUEST:
                return JoinGameRequest.parseDataStr(data);

            case PLAYERELEMENT:
                return PlayerElement.parseDataStr(data);

            case GAMESTATE:
                return GameState.parseDataStr(data);

            case TURN:
                return Turn.parseDataStr(data);

            case SETUPDONE:
                return SetupDone.parseDataStr(data);

            case DICERESULT:
                return DiceResult.parseDataStr(data);

            case DISCARDREQUEST:
                return DiscardRequest.parseDataStr(data);

            case ROLLDICEREQUEST:
                return RollDiceRequest.parseDataStr(data);

            case ROLLDICE:
                return RollDice.parseDataStr(data);

            case ENDTURN:
                return EndTurn.parseDataStr(data);

            case DISCARD:
                return Discard.parseDataStr(data);

            case MOVEROBBER:
                return MoveRobber.parseDataStr(data);

            case CHOOSEPLAYER:
                return ChoosePlayer.parseDataStr(data);

            case CHOOSEPLAYERREQUEST:
                return ChoosePlayerRequest.parseDataStr(data);

            case REJECTOFFER:
                return RejectOffer.parseDataStr(data);

            case CLEAROFFER:
                return ClearOffer.parseDataStr(data);

            case ACCEPTOFFER:
                return AcceptOffer.parseDataStr(data);

            case BANKTRADE:
                return BankTrade.parseDataStr(data);

            case MAKEOFFER:
                return MakeOffer.parseDataStr(data);

            case CLEARTRADEMSG:
                return ClearTradeMsg.parseDataStr(data);

            case BUILDREQUEST:
                return BuildRequest.parseDataStr(data);

            case CANCELBUILDREQUEST:
                return CancelBuildRequest.parseDataStr(data);

            case BUYCARDREQUEST:
                return BuyCardRequest.parseDataStr(data);

            case DEVCARD:
                return DevCard.parseDataStr(data);

            case DEVCARDCOUNT:
                return DevCardCount.parseDataStr(data);

            case SETPLAYEDDEVCARD:
                return SetPlayedDevCard.parseDataStr(data);

            case PLAYDEVCARDREQUEST:
                return PlayDevCardRequest.parseDataStr(data);

            case DISCOVERYPICK:
                return DiscoveryPick.parseDataStr(data);

            case MONOPOLYPICK:
                return MonopolyPick.parseDataStr(data);

            case FIRSTPLAYER:
                return FirstPlayer.parseDataStr(data);

            case SETTURN:
                return SetTurn.parseDataStr(data);

            case ROBOTDISMISS:
                return RobotDismiss.parseDataStr(data);

            case POTENTIALSETTLEMENTS:
                return PotentialSettlements.parseDataStr(data);

            case CHANGEFACE:
                return ChangeFace.parseDataStr(data);

            case REJECTCONNECTION:
                return RejectConnection.parseDataStr(data);

            case LASTSETTLEMENT:
                return LastSettlement.parseDataStr(data);

            case GAMESTATS:
                return GameStats.parseDataStr(data);

            case BCASTTEXTMSG:
                return BCastTextMsg.parseDataStr(data);

            case RESOURCECOUNT:
                return ResourceCount.parseDataStr(data);

            case ADMINPING:
                return AdminPing.parseDataStr(data);

            case ADMINRESET:
                return AdminReset.parseDataStr(data);

            case LONGESTROAD:
                return LongestRoad.parseDataStr(data);

            case LARGESTARMY:
                return LargestArmy.parseDataStr(data);

            case SETSEATLOCK:
                return SetSeatLock.parseDataStr(data);

            case STATUSMESSAGE:
                return StatusMessage.parseDataStr(data);

            case CREATEACCOUNT:
                return CreateAccount.parseDataStr(data);
                
            case GETSTATISTICS:
                return GetStatistics.parseDataStr(data);
                
            case RESETSTATS:
                return ResetStatistics.parseDataStr(data);
                
            case SHOWSTATS:
                return ShowStatistics.parseDataStr(data);

            case UPDATEROBOTPARAMS:
                return UpdateRobotParams.parseDataStr(data);

            case SERVERPING:
                return ServerPing.parseDataStr(data);

            case ROLLDICEPROMPT:     // autoroll, 20071003, sf patch #1812254
                return RollDicePrompt.parseDataStr(data);

            case RESETBOARDREQUEST:  // resetboard, 20080217, v1.1.00
                return ResetBoardRequest.parseDataStr(data);

            case RESETBOARDAUTH:     // resetboard, 20080217, v1.1.00
                return ResetBoardAuth.parseDataStr(data);

            case RESETBOARDVOTEREQUEST:  // resetboard, 20080223, v1.1.00
                return ResetBoardVoteRequest.parseDataStr(data);

            case RESETBOARDVOTE:     // resetboard, 20080223, v1.1.00
                return ResetBoardVote.parseDataStr(data);

            case RESETBOARDREJECT:   // resetboard, 20080223, v1.1.00
                return ResetBoardReject.parseDataStr(data);

            case VERSION:            // cli-serv versioning, 20080807, v1.1.00
                return SOCVersion.parseDataStr(data);

	    case NEWGAMEWITHOPTIONS:     // per-game options, 20090601, v1.1.07
		return NewGameWithOptions.parseDataStr(data);

            case NEWGAMEWITHOPTIONSREQUEST:  // per-game options, 20090601, v1.1.07
                return NewGameWithOptionsRequest.parseDataStr(data);

	    case GAMEOPTIONGETDEFAULTS:  // per-game options, 20090601, v1.1.07
		return GameOptionGetDefaults.parseDataStr(data);

	    case GAMEOPTIONGETINFOS:     // per-game options, 20090601, v1.1.07
		return GameOptionGetInfos.parseDataStr(data);

	    case GAMEOPTIONINFO:         // per-game options, 20090601, v1.1.07
	        return GameOptionInfo.parseDataStr(multiData);

	    case GAMESWITHOPTIONS:       // per-game options, 20090601, v1.1.07
	        return GamesWithOptions.parseDataStr(multiData);

            case BOARDLAYOUT2:      // 6-player board, 20091104, v1.1.08
                return BoardLayout2.parseDataStr(data);

            case PLAYERSTATS:       // per-player statistics, 20100312, v1.1.09
                return PlayerStats.parseDataStr(multiData);

            case PLAYERELEMENTS:    // multiple PLAYERELEMENT, 20100313, v1.1.09
                return PlayerElements.parseDataStr(multiData);

            default:
                System.err.println("Unhandled message type in Message.toMsg: " + msgId);
                return null;
            }
        }
        catch (Exception e)
        {
            System.err.println("toMsg ERROR - " + e);
            System.err.println("Msg: " + s);
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