
package smartsettlers.util;

/**
 *
 * @author szityu
 */
public class Vector2d {

    public double x, y;
    
    public Vector2d(double x, double y)
    {
        this.x = x;
        this.y = y;
    }
    
    public Vector2d(Vector2d v)
    {
        this.x = v.x;
        this.y = v.y;
    }

    public Vector2d()
    {
        this(0,0);
    }
    
    public Vector2d Add(Vector2d v)
    {
        Vector2d v2 = new Vector2d(this.x + v.x, this.y + v.y);
        return v2;
    }

    public Vector2d Subtract(Vector2d v)
    {
        Vector2d v2 = new Vector2d(this.x - v.x, this.y - v.y);
        return v2;
    }

    public Vector2d Mult(double lambda)
    {
        Vector2d v2 = new Vector2d(this.x * lambda, this.y * lambda);
        return v2;
    }
    
    public Vector2d Neg()
    {
        return new Vector2d(-x,-y);
    }
    
}
