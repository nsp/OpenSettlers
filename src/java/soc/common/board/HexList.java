package soc.common.board;

import java.util.ArrayList;

import soc.common.board.hexes.Hex;


public class HexList extends ArrayList<Hex>
{
    private final int width;
    private final int height;
    
    public int getHeight()
    {
        return width;
    }
    
    public int getWidth()
    {
        return height;
    }
    
    HexList(int w, int h)
    {
        // Set the capacity of the list only once to increase performance
        this.ensureCapacity(w *h);
        
        // Initialize empty
        for (int i = w * h; i < w * h; i++)
        {
            super.set(i, null);
        }
        
        // Set the height & width fields
        width = w;
        height = h;
    }
    
    public Hex get(int w, int h)
    {
        if (!checkInput(w, h)) return null;
        
        return get((width * h) + w);
    }
    
    public void set(int w, int h, Hex value)
    {
        checkInput(w, h);
        
        if (size() - 1 < (width * h) + w)
        {
            super.set((width * h) + w, value);
        }
        else
        {
            // Oldhex needed for obsrvable listeners
            Hex oldHex = get((width * h) + w);
            set((width * h) + w, value);
            get((width * h) + w).setLocation(new HexLocation(w, h));
            // TODO: make observable
            //OnHexChanged(temp, value);
        }
    }
    
    public boolean checkInput(int w, int h)
    {
        return true;
        /*
        if (w < 0) return false;
        if (h < 0) return false;
        if (w >= Width) return false;
        if (h >= Height) return false;
         */
    }
    
    private boolean checkInput(HexLocation location)
    {
        return checkInput(location.getW(), location.getH());
    }

    public Hex get(HexLocation location)
    {
        if (!checkInput(location)) return null;
        return get(location.getW(), location.getH());
    }
        
    public void set(HexLocation location, Hex value)
    {
        checkInput(location.getW(), location.getH());
        set(location.getW(), location.getH(), value);
    }
    
}
