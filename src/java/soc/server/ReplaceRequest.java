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
package soc.server;

import soc.message.SitDown;

import soc.server.genericServer.StringConnection;


/**
 * This is a pair of connections, one is sitting at the
 * game and the other is leaving.
 */
class ReplaceRequest
{
    private StringConnection arriving;
    private StringConnection leaving;
    private SitDown sdMes;

    /**
     * Make a new request
     * @param a  the arriving connection
     * @param l  the leaving connection
     * @param sm the SITDOWN message
     */
    public ReplaceRequest(StringConnection a, StringConnection l, SitDown sm)
    {
        arriving = a;
        leaving = l;
        sdMes = sm;
    }

    /**
     * @return the arriving connection
     */
    public StringConnection getArriving()
    {
        return arriving;
    }

    /**
     * @return the leaving connection
     */
    public StringConnection getLeaving()
    {
        return leaving;
    }

    /**
     * @return the SITDOWN message
     */
    public SitDown getSitDownMessage()
    {
        return sdMes;
    }
}
