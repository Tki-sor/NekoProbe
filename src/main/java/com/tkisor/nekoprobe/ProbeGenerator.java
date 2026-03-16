package com.tkisor.nekoprobe;

import com.tkisor.nekojs.api.EventGroup;
import com.tkisor.nekojs.api.NekoEventGroups;
import com.tkisor.nekojs.script.ScriptType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ProbeGenerator {

    public static void generate() throws Exception {
        long startTime = System.currentTimeMillis();
        if (ProbeIOManager.isCacheValid()) {
            NekoProbe.LOGGER.info("[NekoProbe] 环境未发生变化，命中缓存，极速跳过类型生成 (耗时: 0ms)！");
            return;
        }

        NekoProbe.LOGGER.info("[NekoProbe] 检测到环境变更，正在彻底清理旧的探针缓存...");
        ProbeIOManager.wipeOldProbeData();

        NekoProbe.LOGGER.info("[NekoProbe] 开始执行无锁极速全域扫描...");
        Queue<Class<?>> scanQueue = new ConcurrentLinkedQueue<>();
        Set<Class<?>> processedClasses = ConcurrentHashMap.newKeySet();

        Map<String, Queue<String>> fileContents = new ConcurrentHashMap<>();
        Map<String, String> globalClassesMap = new ConcurrentHashMap<>();
        Map<ScriptType, String> eventsContents = new EnumMap<>(ScriptType.class);

        for (ScriptType env : ScriptType.values()) {
            eventsContents.put(env, buildEventsDts(env, scanQueue));
        }

        ClassScanner.seedTargetedPackages(scanQueue);

        while (!scanQueue.isEmpty()) {
            List<Class<?>> currentBatch = new ArrayList<>();
            Class<?> clazz;
            while ((clazz = scanQueue.poll()) != null) currentBatch.add(clazz);

            currentBatch.parallelStream().forEach(targetClass -> {
                if (targetClass.isArray() || TSTypeMapper.isBlacklisted(targetClass.getName()) || !processedClasses.add(targetClass)) return;

                String fullPackageName = targetClass.getPackageName();
                if (fullPackageName == null || fullPackageName.isEmpty()) fullPackageName = "global_classes";

                String fileGroupKey = ClassScanner.getTopLevelPackage(fullPackageName);
                String classCode = TSClassGenerator.generate(targetClass, scanQueue);

                fileContents.computeIfAbsent(fileGroupKey, k -> new ConcurrentLinkedQueue<>()).add(classCode);
                globalClassesMap.put(targetClass.getName(), targetClass.getName().replace('$', '.'));
            });
        }

        NekoProbe.LOGGER.info("[NekoProbe] 解析完成，正在预编译全局字典并写入磁盘...");
        String globalJavaTypeContent = TSGlobalRegistry.buildJavaTypeDts(globalClassesMap);

        Arrays.stream(ScriptType.values()).parallel().forEach(env -> {
            try {
                ProbeIOManager.writeToDiskForEnv(env, eventsContents.get(env), globalJavaTypeContent, fileContents);
            } catch (Exception e) {
                NekoProbe.LOGGER.error("[NekoProbe] 写入环境 {} 失败", env.name(), e);
            }
        });

        ProbeIOManager.saveCache();

        long endTime = System.currentTimeMillis();
        NekoProbe.LOGGER.info("[NekoProbe] 探针生成完毕！总耗时: {} ms，共解析了 {} 个类！", (endTime - startTime), processedClasses.size());
    }

    private static String buildEventsDts(ScriptType env, Queue<Class<?>> scanQueue) {
        StringBuilder sb = new StringBuilder("// === NekoJS Events ===\n\n");
        for (EventGroup g : NekoEventGroups.all().values()) {
            sb.append("declare const ").append(g.name()).append(": {\n");
            for (String k : g.getHandlerKeys()) {
                if (!g.isHandlerValidFor(k, env)) continue;
                String type = TSTypeMapper.mapType(g.getEventType(k), scanQueue);
                sb.append("    ").append(k).append("(handler: (event: ").append(type).append(") => void): void;\n");
                if (g.isTargeted(k)) {
                    sb.append("    ").append(k).append("(target: string, handler: (event: ").append(type).append(") => void): void;\n");
                }
            }
            sb.append("};\n\n");
        }
        return sb.toString();
    }
}