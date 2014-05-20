package soc.common.board;

public enum PointPositionOnHex
{
    //          TopMiddle,
    //              ^
    //    TopLeft  /  \  TopRight
    //            |    |
    //            |    |
    // BottomLeft  \  /  BottomRight
    //               +    
    //         BottomMiddle
    TOPMIDDLE,
    TOPRIGHT,
    BOTTOMRIGHT,
    BOTTOMMIDDLE,
    BOTTOMLEFT,
    TOPLEFT;
}
