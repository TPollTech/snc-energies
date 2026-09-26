package com.snc.energies.world;

import java.util.Iterator;
import java.util.List;

import com.snc.energies.blockentity.MachineBlockEntity;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.Level;

/**
 * The vanilla server tick loop only ticks block entities that registered a ticker
 * for their block. SNC machines instead keep a global list and are driven here,
 * on the last phase of each server tick.
 */
public final class EndServerTicks {
	/** Machines register themselves here on construction (client side ones are skipped below). */
	public static final List<MachineBlockEntity> TICKERS = new java.util.ArrayList<>();

	private EndServerTicks() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (TICKERS.isEmpty()) {
				return;
			}
			Iterator<MachineBlockEntity> it = TICKERS.iterator();
			while (it.hasNext()) {
				MachineBlockEntity be = it.next();
				if (be.isRemoved()) {
					it.remove();
					continue;
				}
				Level level = be.getLevel();
				if (level == null || level.isClientSide()) {
					continue;
				}
				be.serverTick();
			}
		});
	}
}
