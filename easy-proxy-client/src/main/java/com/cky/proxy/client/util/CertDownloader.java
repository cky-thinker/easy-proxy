package com.cky.proxy.client.util;

import com.cky.proxy.client.config.ConfigProperty;
import com.cky.proxy.client.config.ServerProperty;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CertDownloader {
    public static final String PEM_CERT_PATH = "cert.pem";

    public static String getPemCertPath() {
        return Paths.get("config").resolve(PEM_CERT_PATH).toFile().getAbsolutePath();
    }

    public static void downloadIfNotExists() throws Exception {
        ServerProperty server = ConfigProperty.getInstance().getServer();
        String serverIp = server.getIp();
        int webPort = ConfigProperty.getInstance().getServer().getWebPort();
        Path certPath = Paths.get(getPemCertPath());
        Path parent = certPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(certPath)) {
            String urlStr = "http://" + serverIp + ":" + webPort + "/cert.pem";
            try (InputStream in = new URL(urlStr).openStream()) {
                Files.copy(in, certPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
