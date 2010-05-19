/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


///!!! causes exceptions...

package smartsettlers.util;


// taken from
// http://qbrundage.com/michaelb/pubs/essays/random_number_generation
public final class FastRandom {

    private int mt_index;
    private int[] mt_buffer = new int[624];

    public FastRandom() {
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < 624; i++)
            mt_buffer[i] = r.nextInt();
        mt_index = 0;
    }

    public int random() {
        if (mt_index == 624)
        {
            mt_index = 0;
            int i = 0;
            int s;
            for (; i < 624 - 397; i++) {
                s = (mt_buffer[i] & 0x80000000) | (mt_buffer[i+1] & 0x7FFFFFFF);
                mt_buffer[i] = mt_buffer[i + 397] ^ (s >> 1) ^ ((s & 1) * 0x9908B0DF);
            }
            for (; i < 623; i++) {
                s = (mt_buffer[i] & 0x80000000) | (mt_buffer[i+1] & 0x7FFFFFFF);
                mt_buffer[i] = mt_buffer[i - (624 - 397)] ^ (s >> 1) ^ ((s & 1) * 0x9908B0DF);
            }
        
            s = (mt_buffer[623] & 0x80000000) | (mt_buffer[0] & 0x7FFFFFFF);
            mt_buffer[623] = mt_buffer[396] ^ (s >> 1) ^ ((s & 1) * 0x9908B0DF);
        }
        return (mt_buffer[mt_index++]);
    }
 
    public int nextInt(int n) {
        if (n==0)
            return 0;
        if (n <= 0)
            throw new IllegalArgumentException("n must be positive");

        if ((n & -n) == n)  // i.e., n is a power of 2
            return (int)((n * (long)random()) >> 31);

        int bits, val;
        do {
            bits = random();
            val = bits % n;
        } while (bits - val + (n-1) < 0);
        
//        System.out.printf("%d ", val);
//        System.out.flush();
        return val;
    }

}