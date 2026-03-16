package com.tkisor.nekoprobe;

import java.lang.reflect.*;
import java.util.Queue;

public class TSClassGenerator {

    public static String generate(Class<?> clazz, Queue<Class<?>> scanQueue) {
        String fullTSName = clazz.getName().replace('$', '.');
        int lastDot = fullTSName.lastIndexOf('.');
        String tsNamespace = lastDot == -1 ? "" : fullTSName.substring(0, lastDot);
        String tsClassName = lastDot == -1 ? fullTSName : fullTSName.substring(lastDot + 1);

        StringBuilder sb = new StringBuilder();
        boolean hasNamespace = !tsNamespace.isEmpty();
        String indent = hasNamespace ? "    " : "";

        if (hasNamespace) sb.append("declare namespace ").append(tsNamespace).append(" {\n");

        String classGenerics = "";
        try { classGenerics = TSTypeMapper.buildTypeParams(clazz.getTypeParameters()); } catch (Throwable ignored) {}

        if (hasNamespace) {
            sb.append(indent).append("export class ").append(tsClassName).append(classGenerics).append(" {\n");
        } else {
            sb.append(indent).append("declare class ").append(tsClassName).append(classGenerics).append(" {\n");
        }

        try {
            for (Constructor<?> ctor : clazz.getConstructors()) {
                if (Modifier.isPublic(ctor.getModifiers())) {
                    sb.append(indent).append("    constructor(").append(TSTypeMapper.buildParams(ctor, scanQueue)).append(");\n");
                }
            }
            for (Field field : clazz.getFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    sb.append(indent).append("    static readonly ").append(field.getName())
                            .append(": ").append(TSTypeMapper.mapType(field.getGenericType(), scanQueue)).append(";\n");
                }
            }
            for (Method method : clazz.getMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    sb.append(indent).append("    static ").append(method.getName()).append(TSTypeMapper.buildTypeParams(method.getTypeParameters()))
                            .append("(").append(TSTypeMapper.buildParams(method, scanQueue)).append("): ")
                            .append(TSTypeMapper.mapType(method.getGenericReturnType(), scanQueue)).append(";\n");
                }
            }
        } catch (Throwable ignored) {}

        Method[] methods;
        try { methods = clazz.getMethods(); } catch (Throwable t) { methods = new Method[0]; }
        for (Method method : methods) {
            try {
                if (method.getDeclaringClass() == Object.class || Modifier.isStatic(method.getModifiers())) continue;

                String returnType = TSTypeMapper.mapType(method.getGenericReturnType(), scanQueue);
                String methodName = method.getName();

                sb.append(indent).append("    ").append(methodName).append(TSTypeMapper.buildTypeParams(method.getTypeParameters()))
                        .append("(").append(TSTypeMapper.buildParams(method, scanQueue)).append("): ").append(returnType).append(";\n");

                if (method.getParameterCount() == 0) {
                    String propName = null;
                    if (methodName.startsWith("get") && methodName.length() > 3)
                        propName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
                    else if (methodName.startsWith("is") && methodName.length() > 2)
                        propName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);

                    if (propName != null) {
                        sb.append(indent).append("    readonly ").append(propName).append(": ").append(returnType).append(";\n");
                    }
                }
            } catch (Throwable ignored) {}
        }

        sb.append(indent).append("}\n");
        if (hasNamespace) sb.append("}\n");

        return sb.append("\n").toString();
    }
}