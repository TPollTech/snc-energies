package com.snc.energies.blockentity;

import com.snc.energies.energy.EnergyStorage;
import com.snc.energies.energy.SimpleEnergyStorage;
import com.snc.energies.registry.SncBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The energy cube: a large battery that accepts from and feeds the grid.
 */
public class EnergyCubeBlockEntity extends MachineBlockEntity {
	public static final long CAPACITY = 1_000_000L;

	private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY);

	public EnergyCubeBlockEntity(BlockPos pos, BlockState state) {
		super(SncBlockEntities.ENERGY_CUBE, pos, state, 0);
	}

	@Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.world.entity.player.Player player){
        return new com.snc.energies.menu.EnergyCubeMenu(id,inventory,this);
    }

	@Override
	public void serverTick() {
		if (canTick() && com.snc.energies.energy.EnergyTransfer.distribute(level, worldPosition, energy, 160L) > 0) {
			setChanged();
		}
	}

	@Override
	public EnergyStorage getEnergyStorage(Direction side) {
		return energy;
	}

	@Override
	public long getEnergy() {
		return energy.getEnergy();
	}

	@Override
	public long getCapacity() {
		return CAPACITY;
	}

	@Override
	protected String defaultLangKey() {
		return "block.snc_energies.energy_cube";
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		energy.save(output);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		energy.load(input);
	}
}
