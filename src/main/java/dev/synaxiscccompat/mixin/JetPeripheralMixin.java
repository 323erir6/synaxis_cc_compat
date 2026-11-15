package dev.synaxiscccompat.mixin;

import com.verr1.synaxis.content.blocks.jet.JetBlockEntity;
import com.verr1.synaxis.foundation.blockentity.NetworkBlockEntityAccess;
import dev.synaxiscccompat.NeoPeripheralSynaxisBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "io.p2vman.neoperipheral.integration.synaxis.peripheral.JetPeripheral", remap = false)
abstract class JetPeripheralMixin implements NeoPeripheralSynaxisBridge {
    @Shadow @Final private JetBlockEntity entity;

    @Override
    public NetworkBlockEntityAccess synaxisCCCompat$getDevice() {
        return entity;
    }
}
