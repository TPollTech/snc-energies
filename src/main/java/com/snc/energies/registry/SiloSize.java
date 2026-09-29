package com.snc.energies.registry;

/** Shared silo footprint: 2 wide, 2 deep, 3 tall (twelve cells, one inventory). */
public enum SiloSize {
    ;
    public static final int WIDTH=2, DEPTH=2, LEVELS=3;
    public static final int RING=WIDTH*DEPTH;
    public static final int CELLS=RING*LEVELS;
}
