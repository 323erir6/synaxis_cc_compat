package dev.synaxiscccompat;

import com.verr1.synaxis.content.blocks.circuit.CircuitBlockEntity;
import com.verr1.synaxis.foundation.blockentity.NetworkBlockEntityAccess;
import com.verr1.synaxis.foundation.cimulink.core.component.ComponentSchema;
import com.verr1.synaxis.foundation.cimulink.core.component.ExecutionDomain;
import com.verr1.synaxis.foundation.cimulink.core.signal.PortDef;
import com.verr1.synaxis.foundation.cimulink.core.signal.SignalValue;
import com.verr1.synaxis.foundation.cimulink.game.body.PlantEndpointProvider;
import com.verr1.synaxis.foundation.cimulink.game.body.PlantPort;
import com.verr1.synaxis.foundation.cimulink.game.endpoint.CimulinkEndpoint;
import com.verr1.synaxis.foundation.cimulink.game.endpoint.CimulinkEndpointProvider;
import com.verr1.synaxis.foundation.cimulink.game.endpoint.EndpointAddress;
import com.verr1.synaxis.foundation.cimulink.game.endpoint.EndpointInputDefaultOverrides;
import com.verr1.synaxis.foundation.cimulink.game.runtime.CimulinkWorldRuntime;
import com.verr1.synaxis.foundation.cimulink.game.runtime.CimulinkWorldRuntimes;
import com.verr1.synaxis.foundation.state.StateField;
import com.verr1.synaxis.foundation.state.StateKey;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class SynaxisPeripheral implements GenericPeripheral {
    private static final AtomicLong CONTROL_SEQUENCE = new AtomicLong();
    private static final ResourceLocation CIRCUIT_DEFAULTS = ResourceLocation.fromNamespaceAndPath(
        "synaxis", "circuit/input_default_overrides"
    );

    @Override
    public String id() {
        return SynaxisCCCompat.MOD_ID + ":universal";
    }

    @Override
    public PeripheralType getType() {
        return PeripheralType.ofAdditional("synaxis", "synaxis_device");
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getSynaxisInfo(NetworkBlockEntityAccess device) throws LuaException {
        device.ensureNetworkInfrastructure();
        Map<String, Object> result = baseInfo(device);
        result.put("stateSchemaVersion", device.state().schema().version());
        result.put("stateCount", device.state().schema().fields().size());
        result.put("hasPlant", device instanceof PlantEndpointProvider);
        result.put("hasCimulinkEndpoint", device instanceof CimulinkEndpointProvider);
        return result;
    }

    @LuaFunction(mainThread = true)
    public final List<Map<String, Object>> listSynaxisStates(NetworkBlockEntityAccess device) throws LuaException {
        device.ensureNetworkInfrastructure();
        List<Map<String, Object>> result = new ArrayList<>();
        for (StateField<?> field : device.state().schema().fields()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", field.key().id().toString());
            entry.put("writable", field.writable());
            entry.put("editable", field.editable());
            entry.put("persistent", field.persistent());
            entry.put("runtime", field.runtime());
            entry.put("syncTargets", field.syncTargets().stream().map(Enum::name).toList());
            entry.put("value", readState(device, field));
            result.add(entry);
        }
        return result;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getAllSynaxisStates(NetworkBlockEntityAccess device) throws LuaException {
        device.ensureNetworkInfrastructure();
        Map<String, Object> result = new LinkedHashMap<>();
        for (StateField<?> field : device.state().schema().fields()) {
            result.put(field.key().id().toString(), readState(device, field));
        }
        return result;
    }

    @LuaFunction(mainThread = true)
    public final Object getSynaxisState(NetworkBlockEntityAccess device, String id) throws LuaException {
        device.ensureNetworkInfrastructure();
        return readState(device, findState(device, id));
    }

    @LuaFunction(mainThread = true)
    public final Object setSynaxisState(NetworkBlockEntityAccess device, String id, Object value) throws LuaException {
        device.ensureNetworkInfrastructure();
        StateField<?> field = findState(device, id);
        if (!field.writable()) throw new LuaException("State is read-only: " + field.key().id());
        Object decoded = LuaValueCodec.decodeState(field.key(), value);
        writeState(device, field, decoded);
        return readState(device, field);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getCimulinkInfo(CimulinkEndpointProvider provider) throws LuaException {
        CimulinkEndpoint endpoint = provider.endpoint();
        Map<String, Object> result = endpointInfo(endpoint);
        result.put("ports", ports(endpoint.definition().schema()));
        return result;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> listCimulinkPorts(CimulinkEndpointProvider provider) {
        return ports(provider.endpoint().definition().schema());
    }

    @LuaFunction(mainThread = true)
    public final Object readCimulinkInput(CimulinkEndpointProvider provider, String port) throws LuaException {
        CimulinkEndpoint endpoint = provider.endpoint();
        requirePort(endpoint.definition().schema().inputs(), port, "input");
        return SignalCodec.toLua(runtime(provider).input(endpoint.id(), port)
            .orElseThrow(() -> new LuaException("Cimulink input is not currently available: " + port)));
    }

    @LuaFunction(mainThread = true)
    public final Object readCimulinkOutput(CimulinkEndpointProvider provider, String port) throws LuaException {
        CimulinkEndpoint endpoint = provider.endpoint();
        requirePort(endpoint.definition().schema().outputs(), port, "output");
        return SignalCodec.toLua(runtime(provider).output(endpoint.id(), port)
            .orElseThrow(() -> new LuaException("Cimulink output is not currently available: " + port)));
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> readAllCimulinkSignals(CimulinkEndpointProvider provider) throws LuaException {
        CimulinkEndpoint endpoint = provider.endpoint();
        CimulinkWorldRuntime runtime = runtime(provider);
        Map<String, Object> inputs = new LinkedHashMap<>();
        Map<String, Object> outputs = new LinkedHashMap<>();
        for (PortDef port : endpoint.definition().schema().inputs()) {
            inputs.put(port.name(), runtime.input(endpoint.id(), port.name())
                .map(SignalCodec::toLua).orElse(null));
        }
        for (PortDef port : endpoint.definition().schema().outputs()) {
            outputs.put(port.name(), runtime.output(endpoint.id(), port.name())
                .map(SignalCodec::toLua).orElse(null));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inputs", inputs);
        result.put("outputs", outputs);
        return result;
    }

    @LuaFunction(mainThread = true)
    public final Object setCimulinkInput(CircuitBlockEntity circuit, String portName, Object value)
        throws LuaException {
        ComponentSchema schema = circuit.definition().schema();
        PortDef port = requirePort(schema.inputs(), portName, "input");
        SignalValue signal = SignalCodec.fromLua(port.type(), value);

        circuit.ensureNetworkInfrastructure();
        StateField<?> field = circuit.state().schema().field(CIRCUIT_DEFAULTS)
            .orElseThrow(() -> new LuaException("This Synaxis version does not expose circuit input defaults"));
        Object current = circuit.state().get(field.key());
        if (!(current instanceof CompoundTag defaults)) {
            throw new LuaException("Circuit input defaults have an unexpected type");
        }
        CompoundTag updated = EndpointInputDefaultOverrides.withInputDefault(
            defaults, schema, portName, signal
        );
        writeState(circuit, field, updated);
        circuit.syncEndpointRuntimeNow();
        return SignalCodec.toLua(signal);
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> getSynaxisPlantInfo(PlantEndpointProvider provider) throws LuaException {
        PlantPort plant = plant(provider);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("endpointId", plant.endpointId().toString());
        result.put("name", plant.deviceName());
        result.put("componentType", plant.componentType().toString());
        result.put("address", address(plant.address()));
        result.put("domains", supportedDomains(plant));
        result.put("ports", ports(plant.schema()));
        return result;
    }

    @LuaFunction(mainThread = true)
    public final Map<String, Object> listSynaxisPlantPorts(PlantEndpointProvider provider) throws LuaException {
        return ports(plant(provider).schema());
    }

    @LuaFunction(mainThread = true)
    public final Object readSynaxisPlantOutput(PlantEndpointProvider provider, String portName)
        throws LuaException {
        PlantPort plant = plant(provider);
        requirePort(plant.schema().outputs(), portName, "output");
        ExecutionDomain domain = chooseDomain(plant, false);
        SignalValue value = runtime(provider).plantOutputSource().readOutput(
            domain, plant.endpointId(), portName
        );
        return SignalCodec.toLua(value);
    }

    @LuaFunction(mainThread = true)
    public final Object writeSynaxisPlantInput(
        PlantEndpointProvider provider,
        String portName,
        Object value
    ) throws LuaException {
        PlantPort plant = plant(provider);
        PortDef port = requirePort(plant.schema().inputs(), portName, "input");
        SignalValue signal = SignalCodec.fromLua(port.type(), value);
        ExecutionDomain domain = chooseDomain(plant, true);
        runtime(provider).plantControlSink().writeInput(
            domain,
            plant.endpointId(),
            portName,
            signal,
            CONTROL_SEQUENCE.incrementAndGet()
        );
        return SignalCodec.toLua(signal);
    }

    private static Map<String, Object> baseInfo(NetworkBlockEntityAccess device) throws LuaException {
        if (!(device instanceof BlockEntity blockEntity)) {
            throw new LuaException("Synaxis device is not a block entity");
        }
        ServerLevel level = serverLevel(blockEntity);
        BlockPos pos = device.getBlockPos();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("class", device.getClass().getSimpleName());
        result.put("block", device.getBlockState().getBlock().builtInRegistryHolder().key().location().toString());
        result.put("dimension", level.dimension().location().toString());
        result.put("position", position(pos));
        return result;
    }

    private static Map<String, Object> endpointInfo(CimulinkEndpoint endpoint) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("endpointId", endpoint.id().toString());
        result.put("name", endpoint.displayName());
        result.put("componentType", endpoint.definition().componentType().toString());
        result.put("address", address(endpoint.address()));
        return result;
    }

    private static Map<String, Object> ports(ComponentSchema schema) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inputs", schema.inputs().stream().map(SynaxisPeripheral::portInfo).toList());
        result.put("outputs", schema.outputs().stream().map(SynaxisPeripheral::portInfo).toList());
        return result;
    }

    private static Map<String, Object> portInfo(PortDef port) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", port.name());
        result.put("direction", port.direction().name().toLowerCase(Locale.ROOT));
        result.put("type", SignalCodec.describeType(port.type()));
        result.put("default", SignalCodec.toLua(port.defaultValue()));
        return result;
    }

    private static List<String> supportedDomains(PlantPort plant) {
        List<String> result = new ArrayList<>();
        for (ExecutionDomain domain : ExecutionDomain.values()) {
            if (plant.supportsDomain(domain)) result.add(domain.name().toLowerCase(Locale.ROOT));
        }
        return result;
    }

    private static ExecutionDomain chooseDomain(PlantPort plant, boolean control) throws LuaException {
        ExecutionDomain preferred = control
            ? ExecutionDomain.PHYSICS_SUBSTEP_CONTROL
            : ExecutionDomain.PHYSICS_SUBSTEP_READONLY;
        if (plant.supportsDomain(preferred)) return preferred;
        if (plant.supportsDomain(ExecutionDomain.GAME_TICK)) return ExecutionDomain.GAME_TICK;
        throw new LuaException("Plant does not support a compatible " + (control ? "control" : "read") + " domain");
    }

    private static PlantPort plant(PlantEndpointProvider provider) throws LuaException {
        return runtime(provider).plantPort(provider.plantEndpointId())
            .orElseThrow(() -> new LuaException("Synaxis plant endpoint is not currently registered"));
    }

    private static CimulinkWorldRuntime runtime(Object target) throws LuaException {
        if (!(target instanceof BlockEntity blockEntity)) {
            throw new LuaException("Synaxis endpoint is not a block entity");
        }
        return CimulinkWorldRuntimes.forLevel(serverLevel(blockEntity)).runtime();
    }

    private static ServerLevel serverLevel(BlockEntity blockEntity) throws LuaException {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) {
            throw new LuaException("Synaxis block is not attached to a server level");
        }
        return level;
    }

    private static StateField<?> findState(NetworkBlockEntityAccess device, String rawId) throws LuaException {
        String id = rawId.trim();
        if (id.isEmpty()) throw new LuaException("State id cannot be blank");
        if (id.contains(":")) {
            ResourceLocation location = ResourceLocation.tryParse(id);
            if (location == null) throw new LuaException("Invalid state id: " + id);
            return device.state().schema().field(location)
                .orElseThrow(() -> new LuaException("Unknown state: " + id));
        }

        StateField<?> match = null;
        for (StateField<?> field : device.state().schema().fields()) {
            String path = field.key().id().getPath();
            if (path.equals(id) || path.endsWith("/" + id)) {
                if (match != null) throw new LuaException("Ambiguous state name; use the full namespaced id: " + id);
                match = field;
            }
        }
        if (match == null) throw new LuaException("Unknown state: " + id);
        return match;
    }

    private static Object readState(NetworkBlockEntityAccess device, StateField<?> field) throws LuaException {
        Object value = device.state().get(field.key());
        return LuaValueCodec.encodeState(field.key(), value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void writeState(NetworkBlockEntityAccess device, StateField field, Object value) throws LuaException {
        try {
            field.validate(value);
            device.state().set((StateKey) field.key(), value);
            device.setChanged();
        } catch (IllegalArgumentException exception) {
            throw new LuaException("State rejected the supplied value: " + exception.getMessage());
        }
    }

    private static PortDef requirePort(List<PortDef> ports, String name, String direction) throws LuaException {
        return ports.stream().filter(port -> port.name().equals(name)).findFirst()
            .orElseThrow(() -> new LuaException("Unknown Cimulink " + direction + " port: " + name));
    }

    private static Map<String, Object> address(EndpointAddress address) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dimension", address.dimension().location().toString());
        result.put("position", position(address.pos()));
        address.bodyId().ifPresent(body -> result.put("bodyId", body.toString()));
        address.structureInstanceId().ifPresent(id -> result.put("structureId", id.toString()));
        return result;
    }

    private static Map<String, Integer> position(BlockPos pos) {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("x", pos.getX());
        result.put("y", pos.getY());
        result.put("z", pos.getZ());
        return result;
    }
}
