package dev.synaxiscccompat;

import dan200.computercraft.api.ComputerCraftAPI;
import net.neoforged.fml.common.Mod;

@Mod(SynaxisCCCompat.MOD_ID)
public final class SynaxisCCCompat {
    public static final String MOD_ID = "synaxis_cc_compat";
    static final SynaxisPeripheral PERIPHERAL = new SynaxisPeripheral();

    public SynaxisCCCompat() {
        ComputerCraftAPI.registerGenericSource(PERIPHERAL);
    }
}
