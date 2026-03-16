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

    public static void writeToDiskForEnv(ScriptType env, String eventsDtsContent, String globalJavaTypeContent, Map<String, Queue<String>> fileContents) throws Exception {
        Path envDir = NekoJSPaths.PROBE_DIR.resolve(env.name().toLowerCase());
        Path globalsDir = envDir.resolve("probe-types").resolve("globals");
        Path packagesDir = envDir.resolve("probe-types").resolve("packages");

        Files.createDirectories(globalsDir);
        Files.createDirectories(packagesDir);

        Files.writeString(globalsDir.resolve("events.d.ts"), eventsDtsContent);

        StringBuilder packagesIndexContent = new StringBuilder("// === NekoJS Packages ===\n");

        fileContents.entrySet().parallelStream().forEach(entry -> {
            try {
                Files.writeString(packagesDir.resolve(entry.getKey() + ".d.ts"), String.join("", entry.getValue()));
            } catch (Exception ignored) {}
        });

        for (String fileGroupKey : fileContents.keySet()) {
            packagesIndexContent.append("/// <reference path=\"./").append(fileGroupKey).append(".d.ts\" />\n");
        }
        Files.writeString(packagesDir.resolve("index.d.ts"), packagesIndexContent.toString());
        Files.writeString(globalsDir.resolve("java_type.d.ts"), globalJavaTypeContent);
        Files.writeString(globalsDir.resolve("index.d.ts"),
                "// === NekoJS Globals ===\n/// <reference path=\"./events.d.ts\" />\n/// <reference path=\"./java_type.d.ts\" />\n");
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