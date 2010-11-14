/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2009 Jeremy D Monin <jeremy@nand.net>
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
package soc.game;


/**
 * A road playing piece
 */
public class Road extends PlayingPiece
{
    private static final long serialVersionUID = -4125865620963839043L;

    /**
     * Make a new road
     *
     * @param pl  player who owns the city
     * @param co  coordinates
     * @param board  board if known; otherwise will extract from <tt>pl</tt>
     * @throws IllegalArgumentException  if <tt>pl</tt> null, or board null and <tt>pl.board</tt> also null
     */
    public Road(Player pl, int co, Board board)
        throws IllegalArgumentException
    {
        super(PlayingPiece.ROAD, pl, co, board);
    }

    /**
     * The 2 nodes touching this road.
     * @return the 2 nodes touching this road, same format as {@link Board#getAdjacentNodesToEdge_arr(int)}
     */
    public int[] getAdjacentNodes()
    {
        return Board.getAdjacentNodesToEdge_arr(coord);
    }

}
