package com.cky.proxy.common.util;

import lombok.extern.slf4j.Slf4j;

import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.io.resource.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class ConfigBootstrap {
    public static void initConfigs() {
        Path configDir = Paths.get("config");
        copyIfAbsent(configDir, "config.yml");
    }

    private static void copyIfAbsent(Path configDir, String resourceName) {
        Path target = configDir.resolve(resourceName);
        if (Files.exists(target)) {
            return;
        }
        try {
            log.info("Copy default external config: {} {}", target.toFile().getAbsolutePath());
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            
            Resource resource = ResourceUtil.getResourceObj(resourceName);
            if (resource == null) {
                return;
            }
            try (InputStream in = resource.getStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            log.error("Failed to copy default external config: {}", resourceName, e);
        }
    }
}
