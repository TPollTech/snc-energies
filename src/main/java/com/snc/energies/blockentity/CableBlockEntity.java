package com.snc.energies.blockentity;

import com.snc.energies.energy.EnergyStorage;
import com.snc.energies.energy.EnergyTransfer;
import com.snc.energies.energy.SimpleEnergyStorage;
import com.snc.energies.registry.SncBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The cable: absorbs energy pushed by neighbours and relays it onward
 * at up to 160 E/t.
 */
public class CableBlockEntity extends MachineBlockEntity {
	public static final long CAPACITY = 1_600L;
	public static final long MAX_THROUGHPUT = 160L;

	private final SimpleEnergyStorage energy = new SimpleEnergyStorage(CAPACITY);

	public CableBlockEntity(BlockPos pos, BlockState state) {
		super(SncBlockEntities.ENERGY_CABLE, pos, state, 0);
	}

	@Override
	public void serverTick() {
		if (!canTick()) {
			return;
		}
		if (EnergyTransfer.distribute(level, worldPosition, energy, MAX_THROUGHPUT) > 0) {
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
		return "block.snc_energies.energy_cable";
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
