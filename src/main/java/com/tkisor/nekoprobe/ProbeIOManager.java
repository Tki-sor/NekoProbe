package com.tkisor.nekoprobe;

import com.tkisor.nekojs.core.fs.NekoJSPaths;
import com.tkisor.nekojs.script.ScriptType;
import net.neoforged.fml.ModList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Stream;

public class ProbeIOManager {

    private static final Path CACHE_FILE = NekoJSPaths.PROBE_DIR.resolve("probe_cache.md5");
    private static String cachedHash = null;

    public static boolean isCacheValid() {
        cachedHash = calculateModsHash();
        try {
            if (Files.exists(CACHE_FILE)) {
                String savedHash = Files.readString(CACHE_FILE);
                return cachedHash.equals(savedHash);
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void wipeOldProbeData() {
        if (Files.exists(NekoJSPaths.PROBE_DIR)) {
            try (Stream<Path> walk = Files.walk(NekoJSPaths.PROBE_DIR)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (Exception e) {
                NekoProbe.LOGGER.warn("[NekoProbe] 清理旧缓存时遇到部分文件占用，忽略并继续...", e);
            }
        }
        try {
            Files.createDirectories(NekoJSPaths.PROBE_DIR);
        } catch (Exception ignored) {}
    }

    public static void saveCache() {
        try {
            if (cachedHash != null) Files.writeString(CACHE_FILE, cachedHash);
        } catch (Exception ignored) {}
    }

    public static void writeToDiskForEnv(ScriptType env, String eventsDtsContent, String globalJavaTypeContent, String bindingsDtsContent, Map<String, Queue<String>> fileContents) throws Exception {
        Path envDir = NekoJSPaths.PROBE_DIR.resolve(env.name().toLowerCase());
        Path globalsDir = envDir.resolve("probe-types").resolve("globals");
        Path packagesDir = envDir.resolve("probe-types").resolve("packages");

        Files.createDirectories(globalsDir);
        Files.createDirectories(packagesDir);

        // 1. 写入 globals 下的文件
        Files.writeString(globalsDir.resolve("events.d.ts"), eventsDtsContent);
        Files.writeString(globalsDir.resolve("bindings.d.ts"), bindingsDtsContent);
        Files.writeString(globalsDir.resolve("java_type.d.ts"), globalJavaTypeContent);

        // 🌟 2. 动态生成并写入 globals 的 index.d.ts
        String globalsIndex = buildIndexDts("NekoJS Globals", "events.d.ts", "bindings.d.ts", "java_type.d.ts");
        Files.writeString(globalsDir.resolve("index.d.ts"), globalsIndex);

        // 3. 写入 packages 下的文件
        fileContents.entrySet().parallelStream().forEach(entry -> {
            try {
                Files.writeString(packagesDir.resolve(entry.getKey() + ".d.ts"), String.join("", entry.getValue()));
            } catch (Exception ignored) {}
        });

        // 🌟 4. 动态生成并写入 packages 的 index.d.ts
        String[] packageFiles = fileContents.keySet().stream().map(key -> key + ".d.ts").toArray(String[]::new);
        String packagesIndex = buildIndexDts("NekoJS Packages", packageFiles);
        Files.writeString(packagesDir.resolve("index.d.ts"), packagesIndex);
    }

    private static String buildIndexDts(String title, String... fileNames) {
        StringBuilder sb = new StringBuilder("// === ").append(title).append(" ===\n");
        for (String fileName : fileNames) {
            sb.append("/// <reference path=\"./").append(fileName).append("\" />\n");
        }
        return sb.toString();
    }

    private static String calculateModsHash() {
        try {
            StringBuilder modsList = new StringBuilder();
            ModList.get().getMods().forEach(mod ->
                    modsList.append(mod.getModId()).append(":").append(mod.getVersion()).append(";")
            );
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(modsList.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}