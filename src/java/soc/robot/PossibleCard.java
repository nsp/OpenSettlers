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
package soc.robot;

import soc.game.Player;

import java.util.Vector;


/**
 * This is a possible card that we can buy
 *
 * @author Robert S Thomas
 *
 */
public class PossibleCard extends PossiblePiece
{
    /**
     * constructor
     *
     * @param pl   the owner
     * @param et  the eta
     */
    public PossibleCard(Player pl, int et)
    {
        pieceType = PossiblePiece.CARD;
        player = pl;
        coord = 0;
        eta = et;
        threats = new Vector();
        biggestThreats = new Vector();
        threatUpdatedFlag = false;
        hasBeenExpanded = false;
    }

    /**
     * copy constructor
     *
     * @param pc  the possible card to copy
     */
    public PossibleCard(PossibleCard pc)
    {
        //D.ebugPrintln(">>>> Copying possible card: "+pc);
        pieceType = PossiblePiece.CARD;
        player = pc.getPlayer();
        coord = 0;
        eta = pc.getETA();
        threats = new Vector();
        biggestThreats = new Vector();
        threatUpdatedFlag = false;
        hasBeenExpanded = false;
    }
}
