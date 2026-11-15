package dev.synaxiscccompat;

import com.verr1.synaxis.foundation.cimulink.core.signal.SignalType;
import com.verr1.synaxis.foundation.cimulink.core.signal.SignalValue;
import dan200.computercraft.api.lua.LuaException;

import java.util.LinkedHashMap;
import java.util.Map;

final class SignalCodec {
    private SignalCodec() {
    }

    static Object toLua(SignalValue value) {
        if (value instanceof SignalValue.Real real) return real.value();
        if (value instanceof SignalValue.Bool bool) return bool.value();
        if (value instanceof SignalValue.Vec3 vector) return vector(vector);
        if (value instanceof SignalValue.Quaternion rotation) return quaternion(rotation);
        if (value instanceof SignalValue.Pose pose) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("position", vector(pose.position()));
            result.put("orientation", quaternion(pose.orientation()));
            return result;
        }
        if (value instanceof SignalValue.Twist twist) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("velocity", vector(twist.velocity()));
            result.put("omega", vector(twist.omega()));
            return result;
        }
        if (value instanceof SignalValue.Bundle bundle) {
            Map<String, Object> result = new LinkedHashMap<>();
            bundle.fields().forEach((name, child) -> result.put(name, toLua(child)));
            return result;
        }
        return value.toString();
    }

    static SignalValue fromLua(SignalType type, Object value) throws LuaException {
        return switch (type.kind()) {
            case REAL -> new SignalValue.Real(number(value, "real"));
            case BOOLEAN -> new SignalValue.Bool(bool(value, "boolean"));
            case VEC3 -> vector(table(value, "vec3"));
            case QUATERNION -> quaternion(table(value, "quaternion"));
            case POSE -> {
                Map<?, ?> map = table(value, "pose");
                yield new SignalValue.Pose(
                    vector(table(required(map, "position"), "pose.position")),
                    quaternion(table(required(map, "orientation"), "pose.orientation"))
                );
            }
            case TWIST -> {
                Map<?, ?> map = table(value, "twist");
                yield new SignalValue.Twist(
                    vector(table(required(map, "velocity"), "twist.velocity")),
                    vector(table(required(map, "omega"), "twist.omega"))
                );
            }
            case BUNDLE -> {
                Map<?, ?> map = table(value, "bundle");
                Map<String, SignalValue> fields = new LinkedHashMap<>();
                for (Map.Entry<String, SignalType> field : type.fields().entrySet()) {
                    fields.put(field.getKey(), fromLua(field.getValue(), required(map, field.getKey())));
                }
                yield new SignalValue.Bundle(fields);
            }
        };
    }

    static Map<String, Object> describeType(SignalType type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kind", type.kind().name().toLowerCase());
        if (!type.fields().isEmpty()) {
            Map<String, Object> fields = new LinkedHashMap<>();
            type.fields().forEach((name, child) -> fields.put(name, describeType(child)));
            result.put("fields", fields);
        }
        return result;
    }

    private static Map<String, Double> vector(SignalValue.Vec3 value) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("x", value.x());
        result.put("y", value.y());
        result.put("z", value.z());
        return result;
    }

    private static Map<String, Double> quaternion(SignalValue.Quaternion value) {
        Map<String, Double> result = new LinkedHashMap<>();
        result.put("x", value.x());
        result.put("y", value.y());
        result.put("z", value.z());
        result.put("w", value.w());
        return result;
    }

    private static SignalValue.Vec3 vector(Map<?, ?> map) throws LuaException {
        return new SignalValue.Vec3(
            number(required(map, "x"), "x"),
            number(required(map, "y"), "y"),
            number(required(map, "z"), "z")
        );
    }

    private static SignalValue.Quaternion quaternion(Map<?, ?> map) throws LuaException {
        return new SignalValue.Quaternion(
            number(required(map, "x"), "x"),
            number(required(map, "y"), "y"),
            number(required(map, "z"), "z"),
            number(required(map, "w"), "w")
        );
    }

    private static double number(Object value, String name) throws LuaException {
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            throw new LuaException(name + " must be a finite number");
        }
        return number.doubleValue();
    }

    private static boolean bool(Object value, String name) throws LuaException {
        if (!(value instanceof Boolean bool)) throw new LuaException(name + " must be a boolean");
        return bool;
    }

    private static Map<?, ?> table(Object value, String name) throws LuaException {
        if (!(value instanceof Map<?, ?> map)) throw new LuaException(name + " must be a table");
        return map;
    }

    private static Object required(Map<?, ?> map, String key) throws LuaException {
        if (!map.containsKey(key)) throw new LuaException("Missing field '" + key + "'");
        return map.get(key);
    }
}
