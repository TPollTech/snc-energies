package com.snc.energies.menu;

import com.snc.energies.blockentity.WorkshopBlockEntity;
import com.snc.energies.registry.SncMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class WorkshopMenu extends SncMachineMenu {
    private final boolean press;
    public WorkshopMenu(int id, Inventory inventory) { this(id, inventory, false); }
    public WorkshopMenu(int id, Inventory inventory, boolean press) {this(id, inventory, new SimpleContainer(3), dummyData(), press);}
    public WorkshopMenu(int id, Inventory inventory, WorkshopBlockEntity machine) {
        this(id, inventory, machine, new ContainerData() {
            @Override public int get(int index) { return switch(index) {
                case 0 -> machine.work(); case 1 -> machine.status(); case 2 -> machine.isPress() ? 1 : 0;
                case 4 -> machine.getProgress(); case 6 -> machine.getMaxProgress(); default -> 0;
            }; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 7; }
        }, machine.isPress());
    }
    private WorkshopMenu(int id, Inventory inventory, Container machine, ContainerData data, boolean press) {
        super(press ? SncMenus.SEED_PRESS : SncMenus.WORKSHOP, id, machine, data, 3);
        this.press=press;
        String panel=press?"seed_press":"grain_mill";
        addSlot(new Slot(machine, 0, MachinePanel.x(panel,0), MachinePanel.y(panel,0)) {
            @Override public boolean mayPlace(ItemStack stack) {
                return com.snc.energies.registry.WorkshopRecipes.find(isPress(), stack) != null;
            }
        });
        addSlot(new ElectricFurnaceMenu.OutputSlot(machine, 1, MachinePanel.x(panel,1), MachinePanel.y(panel,1)));
        addSlot(new ElectricFurnaceMenu.OutputSlot(machine, 2, MachinePanel.x(panel,2), MachinePanel.y(panel,2)));
        addPlayerSlots(inventory);
    }
    public int work() { return data.get(0); }
    public int status() { return data.get(1); }
    public boolean isPress() { return press; }
    @Override public boolean clickMenuButton(Player player, int button) {
        return button == 0 && stillValid(player) && machine instanceof WorkshopBlockEntity workshop && workshop.crank();
    }
}
