package com.tkisor.nekoprobe;

import java.lang.reflect.*;
import java.util.Queue;
import java.util.Set;

public class TSTypeMapper {
    private static final Set<String> BLACKLIST_PREFIXES = Set.of(
            "sun.", "com.sun.", "org.lwjgl.", "io.netty.", "com.mojang.datafixers.", "jdk."
    );

    private static final Set<String> TS_KEYWORDS = Set.of(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else",
            "enum", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof",
            "new", "null", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var", "void",
            "while", "with", "yield", "let", "await", "implements", "interface", "package", "private", "protected", "public"
    );

    public static String mapType(Type type, Queue<Class<?>> scanQueue) {
        if (type == null) return "any";
        if (type instanceof Class<?> clazz) {
            if (clazz == Object.class) return "any";
            if (clazz == String.class || clazz == char.class || clazz == Character.class || CharSequence.class.isAssignableFrom(clazz)) return "string";
            if (clazz == boolean.class || clazz == Boolean.class) return "boolean";
            if (Number.class.isAssignableFrom(clazz) || clazz.isPrimitive() && clazz != void.class) return "number";
            if (clazz == void.class) return "void";
            if (clazz.isArray()) return mapType(clazz.getComponentType(), scanQueue) + "[]";
            if (!isValidClassForTS(clazz) || isBlacklisted(clazz.getName())) return "any";
            scanQueue.add(clazz);
            return clazz.getName().replace('$', '.');
        } else if (type instanceof ParameterizedType pt) {
            String rawTypeStr = mapType(pt.getRawType(), scanQueue);
            Type[] args = pt.getActualTypeArguments();
            if (pt.getRawType() instanceof Class<?> c && c.getName().equals("com.mojang.datafixers.util.Either") && args.length == 2)
                return mapType(args[0], scanQueue) + " | " + mapType(args[1], scanQueue);
            if ("any".equals(rawTypeStr)) return "any";
            StringBuilder sb = new StringBuilder(rawTypeStr).append("<");
            for (int i = 0; i < args.length; i++) {
                sb.append(mapType(args[i], scanQueue));
                if (i < args.length - 1) sb.append(", ");
            }
            return sb.append(">").toString();
        } else if (type instanceof TypeVariable<?> tv) return tv.getName();
        else if (type instanceof WildcardType wt) {
            Type[] bounds = wt.getUpperBounds();
            return (bounds.length > 0 && bounds[0] != Object.class) ? mapType(bounds[0], scanQueue) : "any";
        } else if (type instanceof GenericArrayType gat) return mapType(gat.getGenericComponentType(), scanQueue) + "[]";
        return "any";
    }

    public static String buildTypeParams(TypeVariable<?>[] typeVars) {
        if (typeVars == null || typeVars.length == 0) return "";
        StringBuilder sb = new StringBuilder("<");
        for (int i = 0; i < typeVars.length; i++) {
            sb.append(typeVars[i].getName());
            if (i < typeVars.length - 1) sb.append(", ");
        }
        return sb.append(">").toString();
    }

    public static String buildParams(Executable executable, Queue<Class<?>> scanQueue) {
        StringBuilder sb = new StringBuilder();
        Parameter[] params = executable.getParameters();
        for (int i = 0; i < params.length; i++) {
            sb.append(sanitizeParamName(params[i].getName())).append(": ").append(mapType(params[i].getParameterizedType(), scanQueue));
            if (i < params.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public static boolean isBlacklisted(String name) {
        for (String p : BLACKLIST_PREFIXES) if (name.startsWith(p)) return true;
        return false;
    }

    public static boolean isValidClassForTS(Class<?> c) {
        if (c.isAnonymousClass() || c.isLocalClass() || c.isSynthetic()) return false;
        String n = c.getSimpleName();
        return n != null && !n.isEmpty() && !Character.isDigit(n.charAt(0)) && !c.getName().matches(".*\\$[0-9]+.*");
    }

    private static String sanitizeParamName(String n) {
        return TS_KEYWORDS.contains(n) ? "_" + n : n;
    }
}