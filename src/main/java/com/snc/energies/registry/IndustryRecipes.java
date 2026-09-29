package com.snc.energies.registry;

import java.util.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import static com.snc.energies.registry.IndustryKind.*;
import static com.snc.energies.registry.SncItems.*;

/** Recipes own the amounts; machine processing and guide read this same catalog. */
public final class IndustryRecipes {
    public record Process(String id,IndustryKind kind,Item input,int count,Item reagent,int reagentCount,
                          boolean keepReagent,Item product,int productCount,Item residue,int residueCount,int ticks) {
        public boolean matches(ItemStack a,ItemStack b) { return a.is(input) && (reagentCount==0 || b.is(reagent)); }
        public boolean supplied(ItemStack a,ItemStack b) { return matches(a,b)&&a.getCount()>=count&&b.getCount()>=reagentCount; }
        public ItemStack output(){return new ItemStack(product,productCount);}
        public ItemStack extra(){return residueCount==0?ItemStack.EMPTY:new ItemStack(residue,residueCount);}
    }
    private IndustryRecipes(){}
    private static final class Catalog { static final List<Process> RECIPES=create(); }
    public static List<Process> all(){return Catalog.RECIPES;}
    private static List<Process> create(){
        List<Process> result=new ArrayList<>();
        Item[][] wood={{Items.OAK_LOG,Items.OAK_PLANKS},{Items.SPRUCE_LOG,Items.SPRUCE_PLANKS},{Items.BIRCH_LOG,Items.BIRCH_PLANKS},
            {Items.JUNGLE_LOG,Items.JUNGLE_PLANKS},{Items.ACACIA_LOG,Items.ACACIA_PLANKS},{Items.DARK_OAK_LOG,Items.DARK_OAK_PLANKS},
            {Items.MANGROVE_LOG,Items.MANGROVE_PLANKS},{Items.CHERRY_LOG,Items.CHERRY_PLANKS}};
        for(int i=0;i<wood.length;i++)result.add(new Process("wood_"+i,SAWMILL,wood[i][0],1,Items.AIR,0,false,wood[i][1],6,SAWDUST,1,100));
        for(Item carbon:new Item[]{Items.COAL,Items.CHARCOAL}) result.add(new Process("steel_"+carbon,FOUNDRY,Items.IRON_INGOT,1,carbon,1,false,STEEL_INGOT,1,Items.AIR,0,200));
        result.add(new Process("steel_plate",LAMINATOR,STEEL_INGOT,1,Items.AIR,0,false,STEEL_PLATE,2,Items.AIR,0,80));
        result.add(new Process("copper_wire",LAMINATOR,Items.COPPER_INGOT,1,Items.AIR,0,false,COPPER_WIRE,2,Items.AIR,0,80));
        result.add(new Process("rice_batch",DRYER,RICE_PADDY,4,Items.AIR,0,false,RICE,4,RICE_HUSK,2,100));
        result.add(new Process("mate_batch",DRYER,MATE_LEAF,4,Items.AIR,0,false,DRIED_MATE,4,Items.AIR,0,100));
        result.add(new Process("insulation",DRYER,STEEL_PLATE,1,SOY_MEAL,2,false,INSULATED_PLATE,1,Items.AIR,0,120));
        result.add(new Process("oil_batch",EXTRACTOR,SOYBEAN,4,Items.AIR,0,false,VEGETABLE_OIL,2,SOY_MEAL,2,120));
        result.add(new Process("refined_voltaite",REFINERY,VOLTAITE_INGOT,2,VEGETABLE_OIL,1,false,REFINED_VOLTAITE,1,Items.AIR,0,240));
        result.add(new Process("mineral_matrix",REFINERY,Items.COBBLESTONE,4,VEGETABLE_OIL,1,false,MINERAL_MATRIX,1,Items.AIR,0,200));
        for(Item sample:new Item[]{Items.RAW_IRON,Items.RAW_COPPER,Items.RAW_GOLD,RAW_TIN,RAW_VOLTAITE})
            result.add(new Process("synthesis_"+sample,SYNTHESIZER,MINERAL_MATRIX,1,sample,1,true,sample,2,Items.AIR,0,400));
        // Compactor (agroindustrial tier): bulk batching of farm and workshop outputs.
        result.add(new Process("hay_block",COMPACTOR,Items.WHEAT,9,Items.AIR,0,false,Items.HAY_BLOCK,1,Items.AIR,0,160));
        result.add(new Process("hay_block_rice",COMPACTOR,RICE,9,Items.AIR,0,false,Items.HAY_BLOCK,1,Items.AIR,0,160));
        result.add(new Process("bone_block",COMPACTOR,Items.BONE_MEAL,9,Items.AIR,0,false,Items.BONE_BLOCK,1,Items.AIR,0,160));
        result.add(new Process("rice_husk_bale",COMPACTOR,RICE_HUSK,6,Items.AIR,0,false,BIOMASS_BRIQUETTE,3,Items.AIR,0,160));
        result.add(new Process("sawdust_briquette_batch",COMPACTOR,SAWDUST,6,Items.AIR,0,false,BIOMASS_BRIQUETTE,3,Items.AIR,0,160));
        result.add(new Process("soy_meal_bale",COMPACTOR,SOY_MEAL,8,Items.AIR,0,false,INSULATED_PLATE,1,Items.AIR,0,200));
        result.add(new Process("sugar_to_cake_base",COMPACTOR,Items.SUGAR,4,Items.EGG,1,false,Items.CAKE,1,Items.AIR,0,200));
        return List.copyOf(result);
    }
    public static Process find(IndustryKind kind,ItemStack a,ItemStack b) {
        for(var recipe:all())if(recipe.kind()==kind&&recipe.matches(a,b))return recipe;
        return null;
    }
    public static boolean accepts(IndustryKind kind,int slot,ItemStack item) {
        for(var recipe:all())if(recipe.kind()==kind&&(slot==0?item.is(recipe.input()):recipe.reagentCount()>0&&item.is(recipe.reagent())))return true;
        return false;
    }
}
