/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.client;

import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This class is a panel that shows how much it costs
 * to build things, and it allows the player to build.
 */
public class SOCBuildingPanel extends Panel implements ActionListener
{
    static final String ROAD = "road";
    static final String STLMT = "stlmt";
    static final String CITY = "city";
    static final String CARD = "card";
    Label title;
    Button roadBut;
    Button settlementBut;
    Button cityBut;
    Button cardBut;
    Label roadT;
    Label roadV;
    Label roadC;
    ColorSquare roadWood;
    ColorSquare roadClay;
    Label settlementT;
    Label settlementV;
    Label settlementC;
    ColorSquare settlementWood;
    ColorSquare settlementClay;
    ColorSquare settlementWheat;
    ColorSquare settlementSheep;
    Label cityT;
    Label cityV;
    Label cityC;
    ColorSquare cityWheat;
    ColorSquare cityOre;
    Label cardT;
    Label cardV;
    Label cardC;
    ColorSquare cardWheat;
    ColorSquare cardSheep;
    ColorSquare cardOre;
    SOCPlayerInterface pi;

    /**
     * make a new building panel
     *
     * @param pi  the player interface that this panel is in
     */
    public SOCBuildingPanel(SOCPlayerInterface pi)
    {
        super();
        setLayout(null);

        this.pi = pi;

        setBackground(new Color(156, 179, 94));
        setForeground(Color.black);
        setFont(new Font("Helvetica", Font.PLAIN, 10));

        /*
           title = new Label("Building Costs:");
           title.setAlignment(Label.CENTER);
           add(title);
         */
        roadT = new Label("Road: ");
        add(roadT);
        roadV = new Label("0 VP  (longest road = 2 VP) ");
        roadV.setAlignment(Label.LEFT);
        add(roadV);
        roadC = new Label("Cost: ");
        add(roadC);
        roadWood = new ColorSquare(ColorSquare.WOOD, 1);
        add(roadWood);
        roadClay = new ColorSquare(ColorSquare.CLAY, 1);
        add(roadClay);
        roadBut = new Button("---");
        add(roadBut);
        roadBut.setActionCommand(ROAD);
        roadBut.addActionListener(this);

        settlementT = new Label("Settlement: ");
        add(settlementT);
        settlementV = new Label("1 VP ");
        settlementV.setAlignment(Label.LEFT);
        add(settlementV);
        settlementC = new Label("Cost: ");
        add(settlementC);
        settlementWood = new ColorSquare(ColorSquare.WOOD, 1);
        add(settlementWood);
        settlementClay = new ColorSquare(ColorSquare.CLAY, 1);
        add(settlementClay);
        settlementWheat = new ColorSquare(ColorSquare.WHEAT, 1);
        add(settlementWheat);
        settlementSheep = new ColorSquare(ColorSquare.SHEEP, 1);
        add(settlementSheep);
        settlementBut = new Button("---");
        add(settlementBut);
        settlementBut.setActionCommand(STLMT);
        settlementBut.addActionListener(this);

        cityT = new Label("City Upgrade: ");
        add(cityT);
        cityV = new Label("2 VP  (receives 2x rsrc.) ");
        cityV.setAlignment(Label.LEFT);
        add(cityV);
        cityC = new Label("Cost: ");
        add(cityC);
        cityWheat = new ColorSquare(ColorSquare.WHEAT, 2);
        add(cityWheat);
        cityOre = new ColorSquare(ColorSquare.ORE, 3);
        add(cityOre);
        cityBut = new Button("---");
        add(cityBut);
        cityBut.setActionCommand(CITY);
        cityBut.addActionListener(this);

        cardT = new Label("Card: ");
        add(cardT);
        cardV = new Label("? VP  (largest army = 2 VP) ");
        cardV.setAlignment(Label.LEFT);
        add(cardV);
        cardC = new Label("Cost: ");
        add(cardC);
        cardWheat = new ColorSquare(ColorSquare.WHEAT, 1);
        add(cardWheat);
        cardSheep = new ColorSquare(ColorSquare.SHEEP, 1);
        add(cardSheep);
        cardOre = new ColorSquare(ColorSquare.ORE, 1);
        add(cardOre);
        cardBut = new Button("---");
        add(cardBut);
        cardBut.setActionCommand(CARD);
        cardBut.addActionListener(this);
    }

    /**
     * DOCUMENT ME!
     */
    public void doLayout()
    {
        Dimension dim = getSize();
        int curY = 0;
        int curX;
        FontMetrics fm = this.getFontMetrics(this.getFont());
        int lineH = ColorSquare.HEIGHT;
        int rowSpaceH = (dim.height - (8 * lineH)) / 3;
        int halfLineH = lineH / 2;
        int costW = fm.stringWidth(new String("Cost: "));
        int butW = 50;
        int margin = 2;

        /*
           title.setSize(dim.width, lineH);
           title.setLocation(0, 0);
           curY += lineH;
         */
        roadT.setSize(fm.stringWidth(roadT.getText()), lineH);
        roadT.setLocation(margin, curY);

        int roadVW = fm.stringWidth(roadV.getText());
        roadV.setSize(roadVW, lineH);
        roadV.setLocation(dim.width - (roadVW + margin), curY);
        curY += lineH;
        roadC.setSize(costW, lineH);
        roadC.setLocation(margin, curY);
        curX = 1 + costW + 3;
        roadWood.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        roadWood.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        roadClay.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        roadClay.setLocation(curX, curY);
        roadBut.setSize(butW, lineH);
        roadBut.setLocation(dim.width - (butW + margin), curY);
        curY += (rowSpaceH + lineH);

        settlementT.setSize(fm.stringWidth(settlementT.getText()), lineH);
        settlementT.setLocation(margin, curY);

        int settlementVW = fm.stringWidth(settlementV.getText());
        settlementV.setSize(settlementVW, lineH);
        settlementV.setLocation(dim.width - (settlementVW + margin), curY);
        curY += lineH;
        settlementC.setSize(costW, lineH);
        settlementC.setLocation(margin, curY);
        curX = 1 + costW + 3;
        settlementWood.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        settlementWood.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        settlementClay.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        settlementClay.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        settlementWheat.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        settlementWheat.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        settlementSheep.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        settlementSheep.setLocation(curX, curY);
        settlementBut.setSize(butW, lineH);
        settlementBut.setLocation(dim.width - (butW + margin), curY);
        curY += (rowSpaceH + lineH);

        cityT.setSize(fm.stringWidth(cityT.getText()), lineH);
        cityT.setLocation(margin, curY);

        int cityVW = fm.stringWidth(cityV.getText());
        cityV.setSize(cityVW, lineH);
        cityV.setLocation(dim.width - (cityVW + margin), curY);
        curY += lineH;
        cityC.setSize(costW, lineH);
        cityC.setLocation(margin, curY);
        curX = 1 + costW + 3;
        cityWheat.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cityWheat.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        cityOre.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cityOre.setLocation(curX, curY);
        cityBut.setSize(butW, lineH);
        cityBut.setLocation(dim.width - (butW + margin), curY);
        curY += (rowSpaceH + lineH);

        cardT.setSize(fm.stringWidth(cardT.getText()), lineH);
        cardT.setLocation(margin, curY);

        int cardVW = fm.stringWidth(cardV.getText());
        cardV.setSize(cardVW, lineH);
        cardV.setLocation(dim.width - (cardVW + margin), curY);
        curY += lineH;
        cardC.setSize(costW, lineH);
        cardC.setLocation(margin, curY);
        curX = 1 + costW + 3;
        cardWheat.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cardWheat.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        cardSheep.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cardSheep.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        cardOre.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cardOre.setLocation(curX, curY);
        cardBut.setSize(butW, lineH);
        cardBut.setLocation(dim.width - (butW + margin), curY);
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void actionPerformed(ActionEvent e)
    {
        String target = e.getActionCommand();
        SOCGame game = pi.getGame();
        SOCPlayerClient client = pi.getClient();
        SOCPlayer ourPlayerData = game.getPlayer(client.getNickname());

        if (ourPlayerData != null)
        {
            if (game.getCurrentPlayerNumber() == ourPlayerData.getPlayerNumber())
            {
                if (target == ROAD)
                {
                    if ((game.getGameState() == SOCGame.PLAY1) && (roadBut.getLabel().equals("Buy")))
                    {
                        client.buildRequest(game, SOCPlayingPiece.ROAD);
                    }
                    else if (roadBut.getLabel().equals("Cancel"))
                    {
                        client.cancelBuildRequest(game, SOCPlayingPiece.ROAD);
                    }
                }
                else if (target == STLMT)
                {
                    if ((game.getGameState() == SOCGame.PLAY1) && (settlementBut.getLabel().equals("Buy")))
                    {
                        client.buildRequest(game, SOCPlayingPiece.SETTLEMENT);
                    }
                    else if (settlementBut.getLabel().equals("Cancel"))
                    {
                        client.cancelBuildRequest(game, SOCPlayingPiece.SETTLEMENT);
                    }
                }
                else if (target == CITY)
                {
                    if ((game.getGameState() == SOCGame.PLAY1) && (cityBut.getLabel().equals("Buy")))
                    {
                        client.buildRequest(game, SOCPlayingPiece.CITY);
                    }
                    else if (cityBut.getLabel().equals("Cancel"))
                    {
                        client.cancelBuildRequest(game, SOCPlayingPiece.CITY);
                    }
                }
                else if (target == CARD)
                {
                    if ((game.getGameState() == SOCGame.PLAY1) && (cardBut.getLabel().equals("Buy")))
                    {
                        client.buyDevCard(game);
                    }
                }
            }
        }
    }

    /**
     * update the status of the buttons
     */
    public void updateButtonStatus()
    {
        SOCGame game = pi.getGame();
        SOCPlayer player = game.getPlayer(pi.getClient().getNickname());

        if (player != null)
        {
            if ((game.getCurrentPlayerNumber() == player.getPlayerNumber()) && (game.getGameState() == SOCGame.PLACING_ROAD))
            {
                roadBut.setLabel("Cancel");
            }
            else if (game.couldBuildRoad(player.getPlayerNumber()))
            {
                roadBut.setLabel("Buy");
            }
            else
            {
                roadBut.setLabel("---");
            }

            if ((game.getCurrentPlayerNumber() == player.getPlayerNumber()) && (game.getGameState() == SOCGame.PLACING_SETTLEMENT))
            {
                settlementBut.setLabel("Cancel");
            }
            else if (game.couldBuildSettlement(player.getPlayerNumber()))
            {
                settlementBut.setLabel("Buy");
            }
            else
            {
                settlementBut.setLabel("---");
            }

            if ((game.getCurrentPlayerNumber() == player.getPlayerNumber()) && (game.getGameState() == SOCGame.PLACING_CITY))
            {
                cityBut.setLabel("Cancel");
            }
            else if (game.couldBuildCity(player.getPlayerNumber()))
            {
                cityBut.setLabel("Buy");
            }
            else
            {
                cityBut.setLabel("---");
            }

            if (game.couldBuyDevCard(player.getPlayerNumber()))
            {
                cardBut.setLabel("Buy");
            }
            else
            {
                cardBut.setLabel("---");
            }
        }
    }
}
