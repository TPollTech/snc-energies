package com.snc.energies.menu;

import com.snc.energies.blockentity.BeverageMotorBlockEntity;
import com.snc.energies.registry.SncMenus;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Engine panel: the feed buffer docks machine inputs, the collect buffer
 * drains machine outputs, and the work bar fills toward the next step.
 */
public final class BeverageMotorMenu extends SncMachineMenu {
    public BeverageMotorMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(2), new SimpleContainerData(7), null);
    }

    public BeverageMotorMenu(int id, Inventory inventory, BeverageMotorBlockEntity motor) {
        this(id, inventory, motor, new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> (int) (motor.getEnergy() & 65535);
                    case 1 -> (int) (motor.getEnergy() >>> 16);
                    case 2 -> (int) (motor.getCapacity() & 65535);
                    case 3 -> (int) (motor.getCapacity() >>> 16);
                    case 4 -> motor.getProgress();
                    case 5 -> motor.getMaxProgress();
                    default -> 0;
                };
            }

            @Override public void set(int index, int value) {}

            @Override public int getCount() { return 7; }
        }, motor);
    }

    private BeverageMotorMenu(int id, Inventory inventory, net.minecraft.world.Container container,
                              ContainerData data, BeverageMotorBlockEntity motor) {
        super(SncMenus.BEVERAGE_MOTOR, id, container, data, 2);
        var layout = MachinePanel.of("beverage_motor");
        for (int i = 0; i < 2; i++) {
            final int slot = i;
            var position = layout.position(i);
            addSlot(new Slot(container, i, position.x(), position.y()) {
                @Override public boolean isActive() { return position.x() >= 0; }
                @Override public boolean mayPlace(ItemStack stack) {
                    return slot == BeverageMotorBlockEntity.FEED
                            && (motor == null || motor.acceptsForFeed(stack));
                }
            });
        }
        addPlayerSlots(inventory);
    }

    @Override public boolean isProductionMenu() { return true; }
}
