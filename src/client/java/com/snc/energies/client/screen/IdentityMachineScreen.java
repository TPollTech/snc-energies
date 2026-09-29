package com.snc.energies.client.screen;

import com.snc.energies.menu.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Individual process diagrams share drawing primitives, never a generic machine layout. */
public class IdentityMachineScreen<T extends SncMachineMenu> extends AbstractContainerScreen<T> {
    protected final MachinePanel panel;
    public IdentityMachineScreen(T menu,Inventory inventory,Component title,String id){
        super(menu,inventory,title,MachinePanel.WIDTH,MachinePanel.HEIGHT);panel=MachinePanel.of(id);
    }
    public String panelId(){return panel.id();}
    @Override protected void init(){
        super.init();
        if(panel.button()!=null){
            int[] b=panel.button();
            addRenderableWidget(Button.builder(label(panel.id().equals("seed_press")?"press":panel.id().equals("mineral_synthesizer")?"redstone":"crank"),button->{
                if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
            }).bounds(leftPos+b[0],topPos+b[1],b[2],b[3]).build());
        }
    }
    private static Component label(String key){return Component.translatable("gui.snc_energies.panel."+key);}
    private static int color(String value){return 0xff000000|Integer.parseInt(value.substring(1),16);}    private long amount(String source){return switch(source){
        case "energy" -> menu.getSyncedEnergy();
        case "water" -> menu instanceof IndustrialMenu m?m.water():0;
        case "steam" -> menu instanceof IndustrialMenu m?m.steam():0;
        case "work" -> menu instanceof WorkshopMenu m?m.work():menu instanceof SiloMenu s?s.total():0;
        case "burn" -> menu.getSyncedBurn();default -> menu.getSyncedProgress();
    };}
    private long capacity(String source){return switch(source){
        case "energy" -> menu.getSyncedCapacity();case "water" -> 8000;case "steam" -> 16000;
        case "work" -> menu instanceof SiloMenu?SiloMenu.TOTAL_CAPACITY:480;
        case "burn" -> menu instanceof IndustrialMenu ? Math.max(1,menu.getSyncedBurn()):menu.getSyncedBurnTotal();
        default -> menu.getSyncedBurnTotal();
    };}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){
        for(var r:panel.rects()){
            int x=((Number)r.get(0)).intValue()+leftPos,y=((Number)r.get(1)).intValue()+topPos;
            g.fill(x,y,x+((Number)r.get(2)).intValue(),y+((Number)r.get(3)).intValue(),color((String)r.get(4)));
        }
        for(var b:panel.bars()){
            double fraction=Math.clamp((double)amount(b.source())/Math.max(1,capacity(b.source())),0,1);
            int x=leftPos+b.x(),y=topPos+b.y(),w=b.w(),h=b.h();
            if(b.vertical()) {int fill=(int)(h*fraction);g.fill(x,y+h-fill,x+w,y+h,color(b.color()));}
            else {g.fill(x,y,x+(int)(w*fraction),y+h,color(b.color()));}
            if(mouseX>=x-2&&mouseX<x+w+2&&mouseY>=y-2&&mouseY<y+h+2){
                String unit=switch(b.source()){case "energy"->" E";case "water","steam"->" mB";case "burn","progress"->" ticks";default->"";};
                if(b.source().equals("work")&&menu instanceof SiloMenu)unit=" itens";
                var tip=label(b.source()).copy().append(": "+amount(b.source())+(b.source().equals("burn")&&!menu.isProductionMenu()?"":" / "+capacity(b.source()))+unit);
                g.setTooltipForNextFrame(font,tip,mouseX,mouseY);
            }
        }
    }
    private Component status(){
        if(menu instanceof IndustrialMenu m)return Component.translatable("gui.snc_energies.industry_status."+m.status());
        if(menu instanceof WorkshopMenu m)return Component.translatable("gui.snc_energies.workshop_status."+m.status());
        if(menu instanceof EnergyCubeMenu)return label("stored").copy().append(": "+menu.getSyncedEnergy()+" / "+menu.getSyncedCapacity()+" E");
        if(menu instanceof SiloMenu s)return label("stored").copy().append(": "+s.total()+" / "+SiloMenu.TOTAL_CAPACITY);
        return label(menu.getSyncedBurn()>0||menu.getSyncedProgress()>0?"running":"ready");
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mouseX,int mouseY){
        g.text(font,title,13,10,0xffedf2dc);
        g.text(font,Component.translatable("gui.snc_energies.panel.subtitle."+panel.id()),13,23,color(panel.accent()));
        for(var position:panel.slots()){
            // Bulk rows share one label; the grid itself communicates the rest.
            if(position.role().equals("row")&&position.index()!=com.snc.energies.blockentity.SiloBlockEntity.ROW_BASE)continue;
            g.text(font,label(position.role()),position.x()-2,position.y()-12,color(panel.accent()));
        }
        g.text(font,status(),13,132,0xffd4e2d4);g.text(font,playerInventoryTitle,48,145,0xffabbdb6);
        if(panel.id().equals("mineral_synthesizer")){
            var s=panel.position(1);int mx=mouseX-leftPos,my=mouseY-topPos;
            if(mx>=s.x()&&mx<s.x()+16&&my>=s.y()-13&&my<s.y()-1)g.setTooltipForNextFrame(font,label("sample_hint"),mouseX,mouseY);
        }
    }
}
