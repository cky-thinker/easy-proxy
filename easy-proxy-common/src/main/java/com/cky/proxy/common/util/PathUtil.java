package com.cky.proxy.common.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

@Slf4j
public class PathUtil {
    public static String getJarFilePath(Class<?> clazz) {
        String url = "";
        try {
            url = getJarPathBySourceCode(clazz);
        } catch (Exception e) {
            log.error("Cannot get jar file path using getLocationOf", e);
            url = getJarPathClzResource(clazz);
        }
        if (url.endsWith(".jar")) {
            url = url.replaceAll("(.*)[/\\\\].*\\.jar", "$1");
        }
        return url;
    }

    private static String getJarPathBySourceCode(Class<?> clazz) throws URISyntaxException {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        return Paths.get(url.toURI()).toString();
    }

    private static String getJarPathClzResource(Class<?> clazz) {
        URL classResource = clazz.getResource(clazz.getSimpleName() + ".class");
        if (classResource == null) {
            throw new RuntimeException("class resource is null");
        }
        String url = classResource.toString();
        if (url.startsWith("jar:file:")) {
            String path = url.replaceAll("^jar:(file:.*[.]jar)!/.*", "$1");
            try {
                return Paths.get(new URL(path).toURI()).toString();
            } catch (Exception e) {
                throw new RuntimeException("Invalid Jar File URL String");
            }
        }
        throw new RuntimeException("Invalid Jar File URL String");
    }
}
