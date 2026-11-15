package dev.synaxiscccompat.mixin;

import com.verr1.synaxis.content.blocks.anchor.AnchorBlockEntity;
import com.verr1.synaxis.foundation.blockentity.NetworkBlockEntityAccess;
import dev.synaxiscccompat.NeoPeripheralSynaxisBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "io.p2vman.neoperipheral.integration.synaxis.peripheral.AnchorPeripheral", remap = false)
abstract class AnchorPeripheralMixin implements NeoPeripheralSynaxisBridge {
    @Shadow @Final private AnchorBlockEntity entity;

    @Override
    public NetworkBlockEntityAccess synaxisCCCompat$getDevice() {
        return entity;
    }
}
