/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smartsettlers.util;

/**
 *
 * @author szityu
 */
public interface VectorConstants {

    public static final Vector2d E1 = new Vector2d(1,0);
    public static final Vector2d E2 = new Vector2d(0,1);
    public static final Vector2d E3 = E2.Subtract(E1);
    Vector2d E4 = E1.Neg();
    Vector2d E5 = E2.Neg();
    Vector2d E6 = E3.Neg();
    
    Vector2d[] HEX_EDGES = {
        E1.Add(E2).Mult(1.0/3),
        E2.Add(E3).Mult(1.0/3),
        E3.Add(E4).Mult(1.0/3),
        E4.Add(E5).Mult(1.0/3),
        E5.Add(E6).Mult(1.0/3),
        E6.Add(E1).Mult(1.0/3)
    };

}
