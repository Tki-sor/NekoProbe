package com.tkisor.nekoprobe;

import com.google.common.reflect.ClassPath;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class ClassScanner {

    public static void seedTargetedPackages(Queue<Class<?>> scanQueue) {
        Set<String> classNames = new HashSet<>();
        try {
            for (ModFileScanData sd : ModList.get().getAllScanData())
                for (ModFileScanData.ClassData cd : sd.getClasses()) classNames.add(cd.clazz().getClassName());
            ClassPath cp = ClassPath.from(ProbeGenerator.class.getClassLoader());
            for (String p : new String[]{"net.minecraft", "net.neoforged", "java"})
                for (ClassPath.ClassInfo ci : cp.getTopLevelClassesRecursive(p)) classNames.add(ci.getName());
        } catch (Exception ignored) {}

        ClassLoader cl = ProbeGenerator.class.getClassLoader();

        classNames.parallelStream().forEach(cn -> {
            if (cn.contains("mixin") || cn.contains("shadow") || TSTypeMapper.isBlacklisted(cn)) return;
            try {
                Class<?> clazz = Class.forName(cn, false, cl);
                if (TSTypeMapper.isValidClassForTS(clazz)) {
                    scanQueue.add(clazz);
                }
            } catch (Throwable ignored) {}
        });
    }

    public static String getTopLevelPackage(String pkg) {
        String[] p = pkg.split("\\.");
        return p.length >= 2 ? p[0] + "." + p[1] : p[0];
    }
}