package dev.synaxiscccompat.mixin;

import com.verr1.synaxis.content.blocks.kinetic.resistor.KineticResistorBlockEntity;
import com.verr1.synaxis.foundation.blockentity.NetworkBlockEntityAccess;
import dev.synaxiscccompat.NeoPeripheralSynaxisBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "io.p2vman.neoperipheral.integration.synaxis.peripheral.KineticResistorPeripheral", remap = false)
abstract class KineticResistorPeripheralMixin implements NeoPeripheralSynaxisBridge {
    @Shadow @Final private KineticResistorBlockEntity entity;

    @Override
    public NetworkBlockEntityAccess synaxisCCCompat$getDevice() {
        return entity;
    }
}
