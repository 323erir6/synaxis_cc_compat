package dev.synaxiscccompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.verr1.synaxis.foundation.state.StateKey;
import dan200.computercraft.api.lua.LuaException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LuaValueCodec {
    private LuaValueCodec() {
    }

    static Object encodeState(StateKey<?> key, Object value) throws LuaException {
        @SuppressWarnings("unchecked")
        DataResult<JsonElement> result = ((StateKey<Object>) key).persistentCodec()
            .encodeStart(JsonOps.INSTANCE, value);
        JsonElement json = result.result().orElseThrow(() -> new LuaException(
            "Cannot encode state " + key.id() + ": " + result.error()
                .map(DataResult.Error::message).orElse("unknown codec error")
        ));
        return toLua(json);
    }

    static Object decodeState(StateKey<?> key, Object value) throws LuaException {
        JsonElement json = toJson(value);
        DataResult<?> result = key.persistentCodec().parse(JsonOps.INSTANCE, json);
        return result.result().orElseThrow(() -> new LuaException(
            "Invalid value for state " + key.id() + ": " + result.error()
                .map(DataResult.Error::message).orElse("unknown codec error")
        ));
    }

    static Object toLua(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) return primitive.getAsBoolean();
            if (primitive.isNumber()) return primitive.getAsDouble();
            return primitive.getAsString();
        }
        if (element.isJsonArray()) {
            List<Object> result = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) result.add(toLua(child));
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            result.put(entry.getKey(), toLua(entry.getValue()));
        }
        return result;
    }

    static JsonElement toJson(Object value) throws LuaException {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof Boolean bool) return new JsonPrimitive(bool);
        if (value instanceof Number number) {
            double converted = number.doubleValue();
            if (!Double.isFinite(converted)) throw new LuaException("Numbers must be finite");
            return new JsonPrimitive(number);
        }
        if (value instanceof String string) return new JsonPrimitive(string);
        if (value instanceof Map<?, ?> map) {
            if (isArray(map)) {
                JsonArray array = new JsonArray();
                for (int index = 1; index <= map.size(); index++) {
                    array.add(toJson(indexed(map, index)));
                }
                return array;
            }
            JsonObject object = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new LuaException("Object table keys must be strings");
                }
                object.add(key, toJson(entry.getValue()));
            }
            return object;
        }
        if (value instanceof List<?> list) {
            JsonArray array = new JsonArray();
            for (Object child : list) array.add(toJson(child));
            return array;
        }
        throw new LuaException("Unsupported Lua value type: " + value.getClass().getSimpleName());
    }

    private static boolean isArray(Map<?, ?> map) {
        if (map.isEmpty()) return false;
        for (int index = 1; index <= map.size(); index++) {
            if (indexed(map, index) == null && !containsIndex(map, index)) return false;
        }
        return true;
    }

    private static Object indexed(Map<?, ?> map, int index) {
        Object value = map.get((long) index);
        if (value == null && !map.containsKey((long) index)) value = map.get(index);
        return value;
    }

    private static boolean containsIndex(Map<?, ?> map, int index) {
        return map.containsKey((long) index) || map.containsKey(index);
    }
}
