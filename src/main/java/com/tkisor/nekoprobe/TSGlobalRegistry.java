package com.tkisor.nekoprobe;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class TSGlobalRegistry {
    private static final Map<String, String> GLOBAL_VARIABLES = new LinkedHashMap<>();
    private static final Map<String, String> GLOBAL_FUNCTIONS = new LinkedHashMap<>();

    // 初始化默认的全局对象
    static {
        registerFunction("require", "(id: string): any");
        registerVariable("module", "{ exports: any }");
        registerVariable("exports", "any");
        registerVariable("console", "{ log(...data: any[]): void; info(...data: any[]): void; warn(...data: any[]): void; error(...data: any[]): void; }");
    }

    public static void registerVariable(String name, String type) {
        GLOBAL_VARIABLES.put(name, type);
    }

    public static void registerFunction(String name, String signature) {
        GLOBAL_FUNCTIONS.put(name, signature);
    }

    public static String buildJavaTypeDts(Map<String, String> classes) {
        Map<String, String> sortedClasses = new TreeMap<>(classes);
        StringBuilder sb = new StringBuilder();
        sb.append("// === NekoJS Java Type Mapping & Globals ===\n");
        sb.append("export {};\n\n");
        sb.append("declare global {\n");

        // 注入全局函数
        for (Map.Entry<String, String> entry : GLOBAL_FUNCTIONS.entrySet()) {
            sb.append("    function ").append(entry.getKey()).append(entry.getValue()).append(";\n");
        }
        // 注入全局变量
        for (Map.Entry<String, String> entry : GLOBAL_VARIABLES.entrySet()) {
            sb.append("    var ").append(entry.getKey()).append(": ").append(entry.getValue()).append(";\n");
        }
        sb.append("\n");

        sb.append("    export type GlobalClasses = {\n");
        for (Map.Entry<String, String> entry : sortedClasses.entrySet()) {
            sb.append("        \"").append(entry.getKey()).append("\": typeof ").append(entry.getValue()).append(";\n");
        }
        sb.append("    }\n\n");

        sb.append("    export type JavaClassPath = keyof GlobalClasses;\n");
        sb.append("    export type ResolvedJavaClass<T> = T extends JavaClassPath ? GlobalClasses[T] : any;\n\n");

        sb.append("    const Java: {\n");
        sb.append("        type<T extends JavaClassPath>(clazz: T): ResolvedJavaClass<T>;\n");
        sb.append("        type(clazz: string): any;\n");
        sb.append("        loadClass<T extends JavaClassPath>(clazz: T): ResolvedJavaClass<T>;\n");
        sb.append("        loadClass(clazz: string): any;\n");
        sb.append("        from(object: any): any;\n");
        sb.append("    };\n");
        sb.append("}\n");
        return sb.toString();
    }
}