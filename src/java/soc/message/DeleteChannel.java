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


/**
 * This message means that a chat channel has been destroyed.
 *
 * @author Robert S Thomas
 */
public class DeleteChannel extends Message
{
    private static final long serialVersionUID = 396705100596337706L;
    /**
     * Name of the channel.
     */
    private String channel;

    /**
     * Create a DeleteChannel message.
     *
     * @param ch  name of the channel
     */
    public DeleteChannel(String ch)
    {
        messageType = DELETECHANNEL;
        channel = ch;
    }

    /**
     * @return the name of the channel
     */
    public String getChannel()
    {
        return channel;
    }

    /**
     * DELETECHANNEL sep channel
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(channel);
    }

    /**
     * DELETECHANNEL sep channel
     *
     * @param ch  the channel name
     * @return    the command string
     */
    public static String toCmd(String ch)
    {
        return DELETECHANNEL + sep + ch;
    }

    /**
     * Parse the command String into a DeleteChannel message
     *
     * @param s   the String to parse
     * @return    a Delete Channel message
     */
    public static DeleteChannel parseDataStr(String s)
    {
        return new DeleteChannel(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "DeleteChannel:channel=" + channel;
    }
}
