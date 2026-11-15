# Synaxis CC Compatibility

Universal CC:Tweaked integration for Synaxis 1.5.0. Every Synaxis network block entity can be wrapped directly or through a wired modem.

## Universal state API

- `getSynaxisInfo()`
- `listSynaxisStates()`
- `getAllSynaxisStates()`
- `getSynaxisState(id)`
- `setSynaxisState(id, value)`

State values are converted through the codecs provided by Synaxis, so new compatible state fields are exposed without adding per-block hardcoded methods.

## Cimulink API

- `getCimulinkInfo()`
- `listCimulinkPorts()`
- `readCimulinkInput(name)`
- `readCimulinkOutput(name)`
- `readAllCimulinkSignals()`
- `setCimulinkInput(name, value)` on a Circuit block

Supported signals: real, boolean, vec3, quaternion, pose, twist, and nested bundles.

## Plant device API

Available on Synaxis engines, propellers, flaps, motors, actuators, cameras, control chairs, and every other block implementing the Synaxis plant endpoint interface:

- `getSynaxisPlantInfo()`
- `listSynaxisPlantPorts()`
- `readSynaxisPlantOutput(name)`
- `writeSynaxisPlantInput(name, value)`

The plant API selects the physics-substep domain when the device supports it and falls back to the game-tick domain when required.

## Lua example

```lua
local device = peripheral.find("synaxis_device")
assert(device, "No Synaxis peripheral found")

print(textutils.serialize(device.getSynaxisInfo()))
print(textutils.serialize(device.listSynaxisStates()))

if device.listSynaxisPlantPorts then
  print(textutils.serialize(device.listSynaxisPlantPorts()))
  -- The exact port names and value types are returned by the call above.
  -- device.writeSynaxisPlantInput("thrust", 250)
end
```
