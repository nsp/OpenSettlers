/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2009-2010 Jeremy D Monin <jeremy@nand.net>
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

import java.io.Serializable;
import java.util.Vector;


/**
 * Playing pieces for Settlers of Catan.
 * For the resources needed to build a piece type, see {@link #getResourcesToBuild(int)}.
 */
public abstract class PlayingPiece implements Serializable, Cloneable
{
    private static final long serialVersionUID = -7462074962321284062L;

    /**
     * Types of playing pieces: Road.
     * @see #getResourcesToBuild(int)
     */
    public static final int ROAD = 0;

    /**
     * Types of playing pieces: Settlement.
     * @see #getResourcesToBuild(int)
     */
    public static final int SETTLEMENT = 1;

    /**
     * Types of playing pieces: City.
     * @see #getResourcesToBuild(int)
     */
    public static final int CITY = 2;

    /**
     * Minimum type number of playing piece (currently Road).
     */
    public static final int MIN = 0;

    /**
     * One past the maximum type number of playing piece.
     */
    public static final int MAXPLUSONE = 3;

    /**
     * The type of this playing piece, within range {@link #MIN} to ({@link #MAXPLUSONE} - 1)
     */
    protected int pieceType;

    /**
     * The player who owns this piece
     */
    protected Player player;

    /**
     * Where this piece is on the board
     */
    protected int coord;

    /**
     * Board, for coordinate-related operations. Should be from same game as {@link #player}.
     * @since 1.1.08
     */
    protected Board board;

    /**
     * Make a new piece.
     *
     * @param ptype  the type of piece, such as {@link #SETTLEMENT}
     * @param pl  player who owns the piece
     * @param co  coordinates
     * @param pboard  board if known; otherwise will extract from <tt>pl</tt>.
     *               Board should be from same game as <tt>pl</tt>.
     * @throws IllegalArgumentException  if <tt>pl</tt> null, or board null and <tt>pl.board</tt> also null
     * @since 1.1.08
     */
    protected PlayingPiece(final int ptype, Player pl, final int co, Board pboard)
        throws IllegalArgumentException
    {
        if (pl == null)
            throw new IllegalArgumentException("player null");
        pieceType = ptype;
        player = pl;
        coord = co;
        if (pboard == null)
        {
            pboard = pl.getGame().getBoard();
            if (pboard == null)
                throw new IllegalArgumentException("player has null board");
        }
        board = pboard;       
    }

    /**
     * Which edges touch this piece on the board?
     * @return edges touching this piece, same format as {@link Board#getAdjacentEdgesToNode(int)}
     */
    public Vector getAdjacentEdges()
    {
        return board.getAdjacentEdgesToNode(coord);
    }

    /**
     * @return  the type of piece
     */
    public int getType()
    {
        return pieceType;
    }

    /**
     * @return the owner of the piece
     */
    public Player getPlayer()
    {
        return player;
    }

    /**
     * @return the coordinates for this piece
     */
    public int getCoordinates()
    {
        return coord;
    }

    /**
     * @return a human readable form of this object
     */
    public String toString()
    {
        String s = "PlayingPiece:type=" + pieceType + "|player=" + player + "|coord=" + Integer.toHexString(coord);

        return s;
    }
    
    /**
     * Compare this PlayingPiece to another PlayingPiece, or another object.
     * Comparison method:
     * <UL>
     * <LI> If other is null, false.
     * <LI> If other is not a PlayingPiece, use our super.equals to compare.
     * <LI> SOCPlayingPieces are equal if same piece type, coordinate, and player.
     * </UL>
     * 
     * @param other The object to compare with, or null.
     */
    public boolean equals(Object other)
    {
        if (other == null)
            return false;
        if (! (other instanceof PlayingPiece))
            return super.equals(other);
        return ((this.pieceType == ((PlayingPiece) other).pieceType)
            &&  (this.coord == ((PlayingPiece) other).coord)
            &&  (this.player == ((PlayingPiece) other).player));

        // board is based on player; no need to check board too.
    }

    /**
     * the set of resources a player needs to build a playing piece.
     * @param pieceType The type of this playing piece, in range {@link #MIN} to ({@link #MAXPLUSONE} - 1).
     *           Can also pass -2 or {@link #MAXPLUSONE} for {@link Game#CARD_SET}.
     * @return the set, such as {@link Game#SETTLEMENT_SET}
     * @throws IllegalArgumentException if <tt>pieceType</tt> is out of range
     * @since 1.1.08
     */
    public static ResourceSet getResourcesToBuild(final int pieceType)
        throws IllegalArgumentException 
    {
        switch (pieceType)
        {
        case ROAD:
            return Game.ROAD_SET;
        case SETTLEMENT:
            return Game.SETTLEMENT_SET;
        case CITY:
            return Game.CITY_SET;
        case -2:
            // fall through
        case 4:    // == PossiblePiece.CARD (robots)
            // fall through
        case PlayingPiece.MAXPLUSONE:
            return Game.CARD_SET;
        default:
            throw new IllegalArgumentException("pieceType: " + pieceType);
        }
    }
}
