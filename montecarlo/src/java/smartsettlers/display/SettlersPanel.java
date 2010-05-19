/*
 * SettlersPanel.java
 *
 * Created on 2008. március 14., 15:12
 */

package smartsettlers.display;

import java.awt.*;
import java.awt.geom.*;
import java.lang.Math;
import smartsettlers.boardlayout.*;
import smartsettlers.util.*;

/**
 *
 * @author  szityu
 */
public class SettlersPanel extends javax.swing.JPanel 
        implements VectorConstants, HexTypeConstants, GameStateConstants 
{
    
    BoardLayout boardlayout;
    double A[][] = {{1, 0}, {0.5, -0.86602540378443864676372317075294}};
    double offset[] = {-0.5, 6.5};
    double scale = 20;
    
    static int pxPlayerPanelWidth;
    static int pxPlayerPanelHeight;
    static int pxOnePlayerPanelHeight;
    static int pxPlayerPanelXOfs;
    static int pxPlayerPanelYOfs;
    static int pxBoardPanelWidth;
    static int pxBoardPanelHeight;
    
    static final int OBJ_NONE   = -1;
    static final int OBJ_HEX    = 0;
    static final int OBJ_EDGE   = 1;
    static final int OBJ_VERTEX = 2;
    int objUnderMouse = OBJ_NONE;
    int objIndUnderMouse = -1;
    int prevObjUnderMouse = OBJ_NONE;
    int prevObjIndUnderMouse = -1;
    
    /** Creates new form SettlersPanel */
    public SettlersPanel() {
        initComponents();
    }

    public void SetBoardLayout(BoardLayout boardlayout)
    {
        
        this.boardlayout = boardlayout;

        int h = this.getHeight();
        int w = this.getWidth();

        pxPlayerPanelWidth = 300;
        pxOnePlayerPanelHeight = 120;
        pxPlayerPanelHeight = 4*pxOnePlayerPanelHeight;
        pxPlayerPanelXOfs = w-pxPlayerPanelWidth;
        pxPlayerPanelYOfs = 0;
        pxBoardPanelWidth = w-pxPlayerPanelWidth;
        pxBoardPanelHeight = h;
        
        
        scale = Math.min(pxBoardPanelWidth / 8.0, pxBoardPanelHeight / 8.0);
        boardlayout.setBoardSize(w, h, scale);
        //boardlayout.scale = scale;
        
   }
    
    @Override protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (boardlayout == null)
        {
            g.drawString("Board not initialized!", 10, 20);
            // g.setColor(Color.BLACK);
            //g.fillRect(0, 0, 20, 20);
        }
        else
        {
            //g.drawString("Board OK!", 10, 20);
            DrawBoard(g);
        }
    }
    
    
    void HighlightHex(Graphics2D g, boolean on )
    {
        
    }

//    public final static int PORT_MISC       = 1;
//    public final static int PORT_SHEEP      = 2;
//    public final static int PORT_WOOD       = 3;
//    public final static int PORT_CLAY       = 4;
//    public final static int PORT_WHEAT      = 5;
//    public final static int PORT_STONE      = 6;    
    String[] portStrings = {"Sh","Wd","Cl","Wh","St","??"};
    
    void DrawHexTile(Graphics g, HexTile h)
    {
        int i, x, y;
        Point p;
        Polygon hexagon;
        
        hexagon = h.screenCoord;
        if (h.type == TYPE_SEA)
        {
            g.setColor(Color.BLUE);
            g.fillPolygon(hexagon);
        }
        else if (h.type == TYPE_PORT)
        {
            g.setColor(Color.BLUE);
            g.fillPolygon(hexagon);
            g.setColor(Color.CYAN);
            i = h.orientation;
            g.drawLine(h.centerScreenCord.x, h.centerScreenCord.y, hexagon.xpoints[i], hexagon.ypoints[i]);
            i--; if (i<0) i = 5; 
            g.drawLine(h.centerScreenCord.x, h.centerScreenCord.y, hexagon.xpoints[i], hexagon.ypoints[i]);
            
            x = 2*h.centerScreenCord.x;
            y = 2*h.centerScreenCord.y;
            i = h.orientation - 3; 
            if (i<0) i+=6;
            x += hexagon.xpoints[i];
            y += hexagon.ypoints[i];
            i = h.orientation - 4; 
            if (i<0) i+=6;
            x += hexagon.xpoints[i];
            y += hexagon.ypoints[i];
            x /= 4;
            y /= 4;
            g.setColor(Color.WHITE);
            g.drawString(portStrings[h.subtype-PORT_SHEEP], x-5, y+5);
        }
        else if (h.type == TYPE_LAND)
        {
            switch (h.subtype)
            {
                case LAND_SHEEP:
                    g.setColor(Color.GREEN);
                    break;
                case LAND_WHEAT:
                    g.setColor(Color.YELLOW);
                    break;
                case LAND_CLAY:
                    g.setColor(Color.ORANGE);
                    break;
                case LAND_WOOD:
                    g.setColor(new Color(0,150,0));
                    break;
                case LAND_STONE:
                    g.setColor(Color.GRAY);
                    break;
                case LAND_DESERT:
                    g.setColor(Color.LIGHT_GRAY);
                    break;
            }
            //g.setColor(Color.GREEN);
            g.fillPolygon(hexagon);
            
            if (h.productionNumber != 0)
            {
                g.setColor(Color.BLACK);
                g.drawString(""+h.productionNumber, h.centerScreenCord.x-10, h.centerScreenCord.y+5);
            }
        }
    }
    

    void DrawVertex(Graphics2D g2, int i)
    {
        Vertex v = boardlayout.vertices[i];
        Stroke oldstroke = g2.getStroke();
        int vstate;
        int W;

        if (v==null)
        {
            return;
        }
        vstate = boardlayout.state[OFS_VERTICES+i];
        if (vstate==VERTEX_EMPTY)
        {
            g2.setColor(Color.RED);
//            g2.setStroke(new BasicStroke
//                        (4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            W = 5;
            g2.fillOval(v.screenCoord.x-W, v.screenCoord.y-W, 2*W, 2*W);
        }
        else if ((vstate>=VERTEX_HASSETTLEMENT) && (vstate<VERTEX_HASSETTLEMENT+NPLAYERS))
        {
            W = 8;
            g2.setColor(Color.WHITE);
            g2.fillRect(v.screenCoord.x-W-1, v.screenCoord.y-W-1, 2*W+2, 2*W+2);            
            g2.setColor(playerColor[vstate-VERTEX_HASSETTLEMENT]);
            g2.fillRect(v.screenCoord.x-W, v.screenCoord.y-W, 2*W, 2*W);            
        }
        else if ((vstate>=VERTEX_HASCITY) && (vstate<VERTEX_HASCITY+NPLAYERS))
        {
            W = 5;
            g2.setColor(Color.WHITE);
            g2.fillRect(v.screenCoord.x-2*W-1, v.screenCoord.y-1, 4*W+2, 2*W+2);            
            g2.fillRect(v.screenCoord.x-1, v.screenCoord.y-2*W-1, 2*W+2, 4*W+2);            
            g2.setColor(playerColor[vstate-VERTEX_HASCITY]);
            g2.fillRect(v.screenCoord.x-2*W, v.screenCoord.y, 4*W, 2*W);            
            g2.fillRect(v.screenCoord.x, v.screenCoord.y-2*W, 2*W, 4*W);            
        }
        
//        if (v.debugLRstatus!=0)
//        {
//            g2.setColor(Color.BLACK);
//            W = 5;
//            g2.drawString(""+v.debugLRstatus, v.screenCoord.x-W, v.screenCoord.y+W);
//        }

//        W = 5;
//        g2.setColor(Color.BLACK);
//        g2.drawString(""+i, v.screenCoord.x-W, v.screenCoord.y+W);
        
        g2.setStroke(oldstroke);
    }

    void DrawEdge(Graphics2D g2, int i)
    {
        Edge e = boardlayout.edges[i];
        Stroke oldstroke = g2.getStroke();
        int estate;
        int W;

        if (e==null)
        {
            return;
        }
        estate = boardlayout.state[OFS_EDGES+i];
        if ((estate>=EDGE_OCCUPIED) && (estate<EDGE_OCCUPIED+NPLAYERS))
        {
            W = 4;
            if (e.isPartOfLongestRoad)
                g2.setColor(Color.BLACK);
            else
                g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke
                        (W+2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(e.screenCoord[0].x, e.screenCoord[0].y, e.screenCoord[1].x, e.screenCoord[1].y);
            g2.setColor(playerColor[estate-EDGE_OCCUPIED]);
            g2.setStroke(new BasicStroke
                        (W, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(e.screenCoord[0].x, e.screenCoord[0].y, e.screenCoord[1].x, e.screenCoord[1].y);
            //g2.fillRect(v.screenCoord.x-W, v.screenCoord.y-W, 2*W, 2*W);            
        }
        g2.setStroke(oldstroke);
    }

//    void DrawEdge(Graphics g, Edge e)
//    {
//        Graphics2D g2 = (Graphics2D)g;
//        Stroke oldstroke = g2.getStroke();
//
//        if (e==null)
//        {
//            return;
//        }
//        g2.setColor(Color.RED);
//        g2.setStroke(new BasicStroke
//                    (4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//        g2.drawLine(e.screenCoord[0].x, e.screenCoord[0].y, e.screenCoord[1].x, e.screenCoord[1].y);
//        g2.setStroke(oldstroke);
//    }

    void DrawBoard(Graphics g)
    {
       Graphics2D g2 = (Graphics2D)g;
       int i;
        
        for (i=0; i<boardlayout.hextiles.length; i++)
        {
            DrawHexTile(g2,boardlayout.hextiles[i]);
        }
        
        for (i=0; i<boardlayout.edges.length; i++)
        {
            DrawEdge(g2,i);
        }
        
        for (i=0; i<boardlayout.vertices.length; i++)
        {
            DrawVertex(g2,i);
        }
        for (i=0; i<NPLAYERS; i++)
        {
            DrawPlayerInfo(g2,i);
        }
       
        Font font1 = g2.getFont();
        Font font2 = new Font("SansSerif", Font.BOLD, 16);
        int val;
        String s;
        
        g2.setFont(font2);
        val = boardlayout.state[OFS_DIE1];
        g2.drawRect(5, 5, 25, 25);
        if (val==0)
            s = "?";
        else
            s = "" + val;
        g2.drawString(s, 15, 25);
        val = boardlayout.state[OFS_DIE2];
        g2.drawRect(30+5, 5, 25, 25);
        if (val==0)
            s = "?";
        else
            s = "" + val;
        g2.drawString(s, 30+15, 25);
        g2.setFont(font1);
        
        if (boardlayout.state[OFS_ROBBERPLACE] != -1)
        {
            g2.setColor(Color.BLACK);
            HexTile h = boardlayout.hextiles[boardlayout.state[OFS_ROBBERPLACE]];
            g2.fillOval(h.centerScreenCord.x, h.centerScreenCord.y, 15, 15);
        }
    }
    
    void DrawPlayerInfo(Graphics2D g2, int pl)
    {
        Font font1 = g2.getFont();
        Font font2 = new Font("SansSerif", Font.BOLD, 16);
        Font font3 = new Font("SansSerif", Font.PLAIN, 10);
        int val;
        String str;
        
        Stroke oldstroke = g2.getStroke();
        int W;
        int x0 = pxPlayerPanelXOfs;
        int y0 = pxPlayerPanelYOfs+pl*pxOnePlayerPanelHeight;

        W = 2;
        g2.setColor(new Color(236,233,216));
        g2.fillRect(x0+W, y0+W, pxPlayerPanelWidth - 3*W, pxOnePlayerPanelHeight - 2*W);
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawRect(x0+W, y0+W, pxPlayerPanelWidth - 3*W, pxOnePlayerPanelHeight - 2*W);
        
        g2.setColor(Color.BLACK);
        int col1ofs = 10;
        int rowheight = 14;
        g2.drawString("Player "+pl, x0 + col1ofs, y0+ 2*rowheight);
        g2.setColor(playerColor[pl]);
        g2.fillRect(x0 + col1ofs, y0 + 3*rowheight, 50, rowheight);

        g2.setColor(Color.BLACK);
        g2.setFont(font2);
        val = boardlayout.state[OFS_PLAYERDATA[pl] + OFS_SCORE];
        g2.drawString(""+val, x0+col1ofs+10, y0 + 5*rowheight+10);
        
        g2.setColor(Color.BLACK);
        g2.setFont(font3);
        val = boardlayout.state[OFS_LONGESTROAD_AT];
        if (val==pl)
            g2.setColor(Color.BLACK);
        else
            g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("Longest road: " + boardlayout.state[OFS_PLAYERDATA[pl]+OFS_PLAYERSLONGESTROAD], x0+col1ofs, y0 + 7*rowheight);
        val = boardlayout.state[OFS_LARGESTARMY_AT];
        if (val==pl)
        {
            g2.setColor(Color.BLACK);
            g2.drawString("Largest army: " + boardlayout.state[OFS_PLAYERDATA[pl]+OFS_USEDCARDS+CARD_KNIGHT],
                    x0+col1ofs, y0 + 8*rowheight);
        }
        else
        {
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("Army size: " + boardlayout.state[OFS_PLAYERDATA[pl]+OFS_USEDCARDS+CARD_KNIGHT],
                    x0+col1ofs, y0 + 8*rowheight);
        }
        
        g2.setFont(font1);
        g2.setColor(Color.BLACK);
        
        int i;
        int col2ofs = 80;
        for (i=0; i<NRESOURCES; i++)
        {
            val = boardlayout.state[OFS_PLAYERDATA[pl]+OFS_RESOURCES+i];
            g2.drawString(resourceNames[i]+": ", x0+col2ofs, y0 + (i+2)*rowheight);
            g2.drawString(""+val, x0+col2ofs+50, y0 + (i+2)*rowheight);
        }
        for (i=0; i<NRESOURCES+1; i++)
        {
            val = boardlayout.state[OFS_PLAYERDATA[pl]+OFS_ACCESSTOPORT+i];
            g2.drawString(val>0 ? "+": " ", x0+col2ofs+60, y0 + (i+2)*rowheight);
        }
        
        int col3ofs = 170;
        for (i=0; i<NCARDTYPES; i++)
        {
            str = "";
            val = boardlayout.state[OFS_PLAYERDATA[pl]+OFS_OLDCARDS+i];
            str = str + val;
            val = boardlayout.state[OFS_PLAYERDATA[pl]+OFS_NEWCARDS+i];
            str = str + " (+" +val+ ")";
            val = boardlayout.state[OFS_PLAYERDATA[pl]+OFS_USEDCARDS+i];
            str = str + " u:"+val;
            g2.drawString(cardNames[i]+": ", x0+col3ofs, y0 + (i+2)*rowheight);
            g2.drawString(str, x0+col3ofs+60, y0 + (i+2)*rowheight);
        }
        
    }
    
    /**
     * finds what kind of object is under the mouse (hex, edge, vertex or none)
     * type and index of object is saved to objUnderMouse, objIndUnderMouse
     * previous values are saved to prevObjUnderMouse, prevObjIndUnderMouse
     * 
     * @param p     coordinates relative to panel
     * @return      true, if highlighted object has changed
     */
    boolean findObjectAtPoint(Point p)
    {
        int ind;
        
        prevObjUnderMouse = objUnderMouse;
        prevObjIndUnderMouse = objIndUnderMouse;
        
        ind = findHexAtPoint(p);
        if (ind>-1)
        {
            objUnderMouse = OBJ_HEX;
            objIndUnderMouse = ind;
        }
        else
        {
            objUnderMouse = OBJ_NONE;
        }
        if ((prevObjUnderMouse == objUnderMouse) && (prevObjIndUnderMouse == objIndUnderMouse))
            return false;
        if ((prevObjUnderMouse == objUnderMouse) && (prevObjUnderMouse == OBJ_NONE))
            return false;
        return true;
    }
    
    /**
     * 
     * @param p  coordinates relative to panel
     * @return   index of hex under mouse or -1
     */
    int findHexAtPoint(Point p)
    {
        int i;
        for (i=0; i<boardlayout.hextiles.length; i++)
        {
            if (boardlayout.hextiles[i].screenCoord.contains(p))
            {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * de-highlight prevoiusly selected object,
     * highlight newly selected object
     */
    void UpdateMouseHighlight()
    {
        Graphics g = this.getGraphics();
        
        if (prevObjUnderMouse == OBJ_HEX)
        {
            DrawBoard(g); //!!!! overshoot
            DrawHexTile(g,boardlayout.hextiles[prevObjIndUnderMouse]);
        }
        
        if (objUnderMouse == OBJ_HEX)
        {
            Color c = new Color(255, 255, 255, 100);
            
            g.setColor(c);
            g.fillPolygon(boardlayout.hextiles[objIndUnderMouse].screenCoord);
            g.setColor(Color.RED);
            g.drawPolygon(boardlayout.hextiles[objIndUnderMouse].screenCoord);
        }
        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                formMouseMoved(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 394, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 408, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void formMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_formMouseMoved
        // TODO add your handling code here:
        Point p = evt.getPoint();
        
//        if (findObjectAtPoint(p))
//        {
//            jLabel1.setText("x: "+p.x+",  y: "+p.y);
//            UpdateMouseHighlight();
//        }

    }//GEN-LAST:event_formMouseMoved
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
}
