package soc.common.board;

import java.util.Random;

public class Chit
{
    private int number = 2;

    public Chit(int number)
    {
        this.number = number;
    }

    public Chit()
    {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the number
     */
    public int getNumber()
    {
        return number;
    }

    /**
     * @param number the number to set
     */
    public void setNumber(int number)
    {
        this.number = number;
    }
    
    public static Chit pickRandomChit(Random random)
    {
        Chit result = new Chit();
        int chitno = (int)(random.nextDouble() * 10);
        switch (chitno)
        {
            case 0: result.setNumber(2);
            case 1: result.setNumber(3);
            case 2: result.setNumber(4);
            case 3: result.setNumber(5);
            case 4: result.setNumber(6);
            case 5: result.setNumber(8);
            case 6: result.setNumber(9);
            case 7: result.setNumber(10);
            case 8: result.setNumber(11);
            case 9: result.setNumber(12);
        }
        return result;
    }
}
