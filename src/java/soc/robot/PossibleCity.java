/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Some documentation javadocs here are Copyright (C) 2009 Jeremy D Monin <jeremy@nand.net>
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

import soc.game.City;
import soc.game.Player;
import soc.game.PlayerNumbers;

import java.util.Vector;


/**
 * This is a possible city that we can build
 *
 * @author Robert S Thomas
 *
 */
public class PossibleCity extends PossiblePiece
{
    /**
     * Speedup per building type.  Indexed from {@link BuildingSpeedEstimate#MIN}
     * to {@link BuildingSpeedEstimate#MAXPLUSONE}.
     */
    protected int[] speedup = { 0, 0, 0, 0 };

    /**
     * constructor
     *
     * @param pl  the owner
     * @param co  coordinates;
     */
    public PossibleCity(Player pl, int co)
    {
        pieceType = PossiblePiece.CITY;
        player = pl;
        coord = co;
        eta = 0;
        threats = new Vector();
        biggestThreats = new Vector();
        threatUpdatedFlag = false;
        hasBeenExpanded = false;
        updateSpeedup();
    }

    /**
     * copy constructor
     *
     * Note: This will not copy vectors, only make empty ones
     *
     * @param pc  the possible city to copy
     */
    public PossibleCity(PossibleCity pc)
    {
        //D.ebugPrintln(">>>> Copying possible city: "+pc);
        pieceType = PossiblePiece.CITY;
        player = pc.getPlayer();
        coord = pc.getCoordinates();
        eta = pc.getETA();
        threats = new Vector();
        biggestThreats = new Vector();
        threatUpdatedFlag = false;
        hasBeenExpanded = false;

        int[] pcSpeedup = pc.getSpeedup();

        for (int buildingType = BuildingSpeedEstimate.MIN;
                buildingType < BuildingSpeedEstimate.MAXPLUSONE;
                buildingType++)
        {
            speedup[buildingType] = pcSpeedup[buildingType];
        }
    }

    /**
     * calculate the speedup that this city gives
     * @see #getSpeedup()
     */
    public void updateSpeedup()
    {
        //D.ebugPrintln("****************************** (CITY) updateSpeedup at "+Integer.toHexString(coord));
        BuildingSpeedEstimate bse1 = new BuildingSpeedEstimate(player.getNumbers());
        int[] ourBuildingSpeed = bse1.getEstimatesFromNothingFast(player.getPortFlags());
        PlayerNumbers newNumbers = new PlayerNumbers(player.getNumbers());
        newNumbers.updateNumbers(new City(player, coord, null), player.getGame().getBoard());

        BuildingSpeedEstimate bse2 = new BuildingSpeedEstimate(newNumbers);
        int[] speed = bse2.getEstimatesFromNothingFast(player.getPortFlags());

        for (int buildingType = BuildingSpeedEstimate.MIN;
                buildingType < BuildingSpeedEstimate.MAXPLUSONE;
                buildingType++)
        {
            //D.ebugPrintln("!@#$% ourBuildingSpeed[buildingType]="+ourBuildingSpeed[buildingType]+" speed[buildingType]="+speed[buildingType]);
            speedup[buildingType] = ourBuildingSpeed[buildingType] - speed[buildingType];
        }
    }

    /**
     * @return the speedup for this city
     */
    public int[] getSpeedup()
    {
        return speedup;
    }

    /**
     * @return the sum of all of the speedup numbers
     */
    public int getSpeedupTotal()
    {
        int sum = 0;

        for (int buildingType = BuildingSpeedEstimate.MIN;
                buildingType < BuildingSpeedEstimate.MAXPLUSONE;
                buildingType++)
        {
            sum += speedup[buildingType];
        }

        return sum;
    }
}
