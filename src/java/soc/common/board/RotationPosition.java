package soc.common.board;

public enum RotationPosition
{
     DEG60 (60),
     DEG120 (120),
     DEG180 (180),
     DEG240 (240),
     DEG300 (300),
     DEG0 (0);
     
     private final int index;
     
     RotationPosition(int index)
     {
         this.index=index;
     }
     
     public int index()
     {
         return index;
     }
}
