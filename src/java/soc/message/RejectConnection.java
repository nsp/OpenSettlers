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
 * This message means that a client isn't allowed to connect
 *
 * @author Robert S Thomas
 */
public class RejectConnection extends Message
{
    private static final long serialVersionUID = -3461376041211350539L;
    /**
     * Text message
     */
    private String text;

    /**
     * Create a RejectConnection message.
     *
     * @param message   the text message
     */
    public RejectConnection(String message)
    {
        messageType = REJECTCONNECTION;
        text = message;
    }

    /**
     * @return the text message
     */
    public String getText()
    {
        return text;
    }

    /**
     * <REJECTCONNECTION>
     *
     * @return the command String
     */
    public String toCmd()
    {
        return toCmd(text);
    }

    /**
     * TEXTMSG sep text
     *
     * @param tm  the text message
     * @return    the command string
     */
    public static String toCmd(String tm)
    {
        return REJECTCONNECTION + sep + tm;
    }

    /**
     * Parse the command String into a RejectConnection message
     *
     * @param s   the String to parse
     * @return    a RejectConnection message
     */
    public static RejectConnection parseDataStr(String s)
    {
        return new RejectConnection(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "RejectConnection:" + text;
    }
}
