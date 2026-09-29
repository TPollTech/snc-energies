package com.snc.energies.registry;

/** Footprints and operating costs are shared by blocks, menus and processing. */
public enum IndustryKind {
    BOILER("biomass_boiler", 2,2,3,2,0),
    SAWMILL("steam_sawmill", 3,2,2,2,20),
    FOUNDRY("steam_foundry", 2,2,2,2,20),
    TURBINE("steam_turbine", 2,2,2,3,80),
    LAMINATOR("laminator", 2,1,2,3,40),
    DRYER("grain_dryer", 3,3,4,4,80),
    EXTRACTOR("oil_extractor", 3,2,2,4,80),
    REFINERY("voltaic_refinery", 3,3,3,5,240),
    SYNTHESIZER("mineral_synthesizer", 3,3,3,5,400),
    COMPACTOR("compactor", 2,1,2,4,120);
    public final String id;
    public final int width, depth, height, tier, cost;
    IndustryKind(String id,int width,int depth,int height,int tier,int cost) {
        this.id=id;this.width=width;this.depth=depth;this.height=height;this.tier=tier;this.cost=cost;
    }
    public int cells() { return width*depth*height; }
    public boolean steamConsumer() { return this==SAWMILL || this==FOUNDRY || this==TURBINE; }
    public boolean electric() { return tier>=3 && this!=TURBINE; }
}
