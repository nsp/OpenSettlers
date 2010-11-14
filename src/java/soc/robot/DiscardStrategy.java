/**
 * Open Settlers - an open implementation of the game Settlers of Catan
 * This file copyright (C) 2008 Christopher McNeil <http://sourceforge.net/users/cmcneil>
 * Portions of this file copyright (C) 2003-2004 Robert S. Thomas
 * Portions of this file copyright (C) 2008 Eli McGowan <http://sourceforge.net/users/emcgowan>
 * Portions of this file copyright (C) 2009 Jeremy D Monin <jeremy@nand.net>
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/
package soc.robot;

import java.util.Random;
import java.util.Stack;

// import org.apache.log4j.Logger;

import soc.disableDebug.D;
import soc.game.Game;
import soc.game.Player;
import soc.game.PlayingPiece;
import soc.game.ResourceConstants;
import soc.game.ResourceSet;
import soc.util.RobotParameters;

public class DiscardStrategy {

	/** debug logging */
    // private transient Logger log = Logger.getLogger(this.getClass().getName());

	public ResourceSet discard(int numDiscards, Stack buildingPlan, Player ourPlayerData, RobotParameters robotParameters, RobotDM decisionMaker, RobotNegotiator negotiator){
		Random rand = new Random();
		//log.debug("DISCARDING...");

        /**
         * if we have a plan, then try to keep the resources
         * needed for that plan, otherwise discard at random
         */
        ResourceSet discards = new ResourceSet();

        /**
         * make a plan if we don't have one
         */
        if (buildingPlan.empty())
        {
            decisionMaker.planStuff(robotParameters.getStrategyType());
        }

        if (!buildingPlan.empty())
        {
            PossiblePiece targetPiece = (PossiblePiece) buildingPlan.peek();
            negotiator.setTargetPiece(ourPlayerData.getPlayerNumber(), targetPiece);

            //log.debug("targetPiece="+targetPiece);
            ResourceSet targetResources = PlayingPiece.getResourcesToBuild(targetPiece.getType());

            /**
             * figure out what resources are NOT the ones we need
             */
            ResourceSet leftOvers = ourPlayerData.getResources().copy();

            for (int rsrc = ResourceConstants.CLAY;
                    rsrc <= ResourceConstants.WOOD; rsrc++)
            {
                if (leftOvers.getAmount(rsrc) > targetResources.getAmount(rsrc))
                {
                    leftOvers.subtract(targetResources.getAmount(rsrc), rsrc);
                }
                else
                {
                    leftOvers.setAmount(0, rsrc);
                }
            }

            ResourceSet neededRsrcs = ourPlayerData.getResources().copy();
            neededRsrcs.subtract(leftOvers);

            /**
             * figure out the order of resources from
             * easiest to get to hardest
             */

            //log.debug("our numbers="+ourPlayerData.getNumbers());
            BuildingSpeedEstimate estimate = new BuildingSpeedEstimate(ourPlayerData.getNumbers());
            int[] rollsPerResource = estimate.getRollsPerResource();
            int[] resourceOrder = 
            {
                ResourceConstants.CLAY, ResourceConstants.ORE,
                ResourceConstants.SHEEP, ResourceConstants.WHEAT,
                ResourceConstants.WOOD
            };

            for (int j = 4; j >= 0; j--)
            {
                for (int i = 0; i < j; i++)
                {
                    if (rollsPerResource[resourceOrder[i]] < rollsPerResource[resourceOrder[i + 1]])
                    {
                        int tmp = resourceOrder[i];
                        resourceOrder[i] = resourceOrder[i + 1];
                        resourceOrder[i + 1] = tmp;
                    }
                }
            }

            /**
             * pick the discards
             */
            int curRsrc = 0;

            while (discards.getTotal() < numDiscards)
            {
                /**
                 * choose from the left overs
                 */
                while ((discards.getTotal() < numDiscards) && (curRsrc < 5))
                {
                    //log.debug("(1) dis.tot="+discards.getTotal()+" curRsrc="+curRsrc);
                    if (leftOvers.getAmount(resourceOrder[curRsrc]) > 0)
                    {
                        discards.add(1, resourceOrder[curRsrc]);
                        leftOvers.subtract(1, resourceOrder[curRsrc]);
                    }
                    else
                    {
                        curRsrc++;
                    }
                }

                curRsrc = 0;

                /**
                 * choose from what we need
                 */
                while ((discards.getTotal() < numDiscards) && (curRsrc < 5))
                {
                    //log.debug("(2) dis.tot="+discards.getTotal()+" curRsrc="+curRsrc);
                    if (neededRsrcs.getAmount(resourceOrder[curRsrc]) > 0)
                    {
                        discards.add(1, resourceOrder[curRsrc]);
                        neededRsrcs.subtract(1, resourceOrder[curRsrc]);
                    }
                    else
                    {
                        curRsrc++;
                    }
                }
            }

            if (curRsrc == 5)
            {
                // log.error("PROBLEM IN DISCARD - curRsrc == 5");
                D.ebugPrintln("discardStrategy: PROBLEM IN DISCARD - curRsrc == 5");
            }
        }
        else
        {
            /**
             *  choose discards at random
             */
            Game.discardPickRandom(ourPlayerData.getResources(), numDiscards, discards, rand);
        }
        
        return discards;
	}
}
