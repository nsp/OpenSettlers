/**
 * Open Settlers - an open implementation of the game Settlers of Catan
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. **/
package soc.message;

import java.util.StringTokenizer;


/**
 * This message means that someone is leaveing a channel
 *
 * @author Robert S Thomas
 */
public class Leave extends Message
{
    private static final long serialVersionUID = -2517303290631098037L;

    /**
     * Nickname of the leaveing member
     */
    private String nickname;

    /**
     * Name of channel
     */
    private String channel;

    /**
     * Host name
     */
    private String host;

    /**
     * Create a Leave message.
     *
     * @param nn  nickname
     * @param hn  host name
     * @param ch  name of chat channel
     */
    public Leave(String nn, String hn, String ch)
    {
        messageType = LEAVE;
        nickname = nn;
        channel = ch;
        host = hn;
    }

    /**
     * @return the nickname
     */
    public String getNickname()
    {
        return nickname;
    }

    /**
     * @return the host name
     */
    public String getHost()
    {
        return host;
    }

    /**
     * @return the channel name
     */
    public String getChannel()
    {
        return channel;
    }

    /**
     * <LEAVE> sep <nickname> sep2 <host> sep2 <channel>
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(nickname, host, channel);
    }

    /**
     * <LEAVE> sep <nickname> sep2 <host> sep2 <channel>
     *
     * @param nn  the neckname
     * @param hn  the host name
     * @param ch  the new channel name
     * @return    the command string
     */
    public static String toCmd(String nn, String hn, String ch)
    {
        return LEAVE + sep + nn + sep2 + hn + sep2 + ch;
    }

    /**
     * Parse the command String into a Leave message
     *
     * @param s   the String to parse
     * @return    a Leave message, or null of the data is garbled
     */
    public static Leave parseDataStr(String s)
    {
        String nn;
        String hn;
        String ch;

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            nn = st.nextToken();
            hn = st.nextToken();
            ch = st.nextToken();
        }
        catch (Exception e)
        {
            return null;
        }

        return new Leave(nn, hn, ch);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        String s = "Leave:nickname=" + nickname + "|host=" + host + "|channel=" + channel;

        return s;
    }
}
