package com.cky.proxy.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.cky.proxy.server.config.ConfigProperty;
import com.cky.proxy.server.config.ServerProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * 自签名证书生成工具类：首次启动自动生成JKS证书和PEM公钥
 */
@Slf4j
public class CertGenerator {
    // 证书相关配置（可抽成配置文件）
    private static final String CERT_ALIAS = "easyproxy";
    private static final int KEY_SIZE = 2048;

    // 证书文件路径（jar包同级目录）
    public static final String JKS_CERT_PATH = "cert.jks";
    public static final String PEM_CERT_PATH = "cert.pem";

    public static String getJksCertPath() {
        return Paths.get("config").resolve(CertGenerator.JKS_CERT_PATH).toFile().getAbsolutePath();
    }

    public static String getPemCertPath() {
        return Paths.get("config").resolve(CertGenerator.PEM_CERT_PATH).toFile().getAbsolutePath();
    }

    /**
     * 检查并生成证书：不存在则生成JKS证书+导出PEM公钥
     */
    public static void generateIfNotExists() throws Exception {
        Path configDir = Paths.get("config");
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
        Path target = configDir.resolve(JKS_CERT_PATH);
        File jksFile = target.toFile();
        File pemFile = configDir.resolve(PEM_CERT_PATH).toFile();

        boolean needGenerate = false;
        X509Certificate existingCert = null;

        if (jksFile.exists()) {
            try (FileInputStream fis = new FileInputStream(jksFile)) {
                KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(fis, getCertPassword().toCharArray());
                Certificate c = ks.getCertificate(CERT_ALIAS);
                if (c instanceof X509Certificate x509) {
                    existingCert = x509;
                    if (new Date().after(x509.getNotAfter())) {
                        needGenerate = true;
                        log.info("证书已过期，重新生成");
                    }
                } else {
                    needGenerate = true;
                    log.info("证书不存在或无效，重新生成");
                }
            } catch (Exception e) {
                needGenerate = true;
                log.warn("读取现有证书失败，重新生成: " + e.getMessage());
            }
        } else {
            needGenerate = true;
            log.info("证书不存在，自动生成");
        }

        if (!needGenerate && !pemFile.exists() && existingCert != null) {
            exportCertToPem(existingCert, pemFile);
            log.info("PEM公钥证书导出成功：" + pemFile.getAbsolutePath());
            return;
        }

        if (!needGenerate) {
            return;
        }

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X509Certificate cert = generateX509Cert(keyPair);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry(CERT_ALIAS, keyPair.getPrivate(), getCertPassword().toCharArray(), new Certificate[] { cert });
        try (FileOutputStream fos = new FileOutputStream(jksFile)) {
            keyStore.store(fos, getCertPassword().toCharArray());
        }
        log.info("JKS证书生成成功：" + jksFile.getAbsolutePath());

        exportCertToPem(cert, pemFile);
        log.info("PEM公钥证书导出成功：" + pemFile.getAbsolutePath());
    }

    /**
     * 生成自签名X509证书
     */
    private static X509Certificate generateX509Cert(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        ServerProperty serverConfig = ConfigProperty.getInstance().getServer();
        String publicHost = serverConfig.getPublicHost();
        int certDays = serverConfig.getCertValidityDays();
        Date from = new Date(now);
        Date to = new Date(now + certDays * 24L * 60L * 60L * 1000L);
        BigInteger serial = new BigInteger(64, new SecureRandom());
        X500Name subject = new X500Name("CN=" + publicHost);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, serial, from, to, subject, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        
        List<GeneralName> altNamesList = new ArrayList<>();
        altNamesList.add(new GeneralName(GeneralName.dNSName, publicHost));
        altNamesList.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));

        // 自动添加本机所有IP地址
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        altNamesList.add(new GeneralName(GeneralName.iPAddress, addr.getHostAddress()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取本机IP失败，生成的证书可能无法用于局域网访问", e);
        }

        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(altNamesList.toArray(new GeneralName[0])));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(holder);
    }

    /**
     * 将X509证书导出为PEM格式
     */
    private static void exportCertToPem(X509Certificate cert, File pemFile) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(pemFile))) {
            writer.write("-----BEGIN CERTIFICATE-----\n");
            writer.write(Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(cert.getEncoded()));
            writer.write("\n-----END CERTIFICATE-----\n");
        }
    }

    /**
     * 获取证书密码（供服务端配置SSL使用）
     */
    public static String getCertPassword() {
        return ConfigProperty.getInstance().getServer().getCertPassword();
    }
}
