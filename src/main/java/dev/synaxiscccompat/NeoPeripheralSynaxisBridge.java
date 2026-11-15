package dev.synaxiscccompat;

import com.verr1.synaxis.foundation.blockentity.NetworkBlockEntityAccess;
import com.verr1.synaxis.foundation.cimulink.game.body.PlantEndpointProvider;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface NeoPeripheralSynaxisBridge extends IPeripheral {
    NetworkBlockEntityAccess synaxisCCCompat$getDevice();

    @Override
    default Set<String> getAdditionalTypes() {
        return Set.of("synaxis", "synaxis_device");
    }

    @LuaFunction(mainThread = true)
    default Map<String, Object> getSynaxisInfo() throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.getSynaxisInfo(synaxisCCCompat$getDevice());
    }

    @LuaFunction(mainThread = true)
    default List<Map<String, Object>> listSynaxisStates() throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.listSynaxisStates(synaxisCCCompat$getDevice());
    }

    @LuaFunction(mainThread = true)
    default Map<String, Object> getAllSynaxisStates() throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.getAllSynaxisStates(synaxisCCCompat$getDevice());
    }

    @LuaFunction(mainThread = true)
    default Object getSynaxisState(String id) throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.getSynaxisState(synaxisCCCompat$getDevice(), id);
    }

    @LuaFunction(mainThread = true)
    default Object setSynaxisState(String id, Object value) throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.setSynaxisState(synaxisCCCompat$getDevice(), id, value);
    }

    @LuaFunction(mainThread = true)
    default Map<String, Object> getSynaxisPlantInfo() throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.getSynaxisPlantInfo(plant());
    }

    @LuaFunction(mainThread = true)
    default Map<String, Object> listSynaxisPlantPorts() throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.listSynaxisPlantPorts(plant());
    }

    @LuaFunction(mainThread = true)
    default Object readSynaxisPlantOutput(String portName) throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.readSynaxisPlantOutput(plant(), portName);
    }

    @LuaFunction(mainThread = true)
    default Object writeSynaxisPlantInput(String portName, Object value) throws LuaException {
        return SynaxisCCCompat.PERIPHERAL.writeSynaxisPlantInput(plant(), portName, value);
    }

    private PlantEndpointProvider plant() throws LuaException {
        if (synaxisCCCompat$getDevice() instanceof PlantEndpointProvider provider) return provider;
        throw new LuaException("This Synaxis block has no Cimulink plant endpoint");
    }
}
