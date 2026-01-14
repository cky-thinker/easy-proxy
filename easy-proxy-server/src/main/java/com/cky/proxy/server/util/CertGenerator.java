package com.cky.proxy.server.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
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

/**
 * 自签名证书生成工具类：首次启动自动生成JKS证书和PEM公钥
 */
public class CertGenerator {
    // 证书相关配置（可抽成配置文件）
    private static final String CERT_ALIAS = "easyproxy";
    private static final String CERT_PASSWORD = "easyproxy@2008"; // 生产环境建议通过环境变量传入
    private static final int KEY_SIZE = 2048;
    private static final int VALIDITY_DAYS = 3650; // 证书有效期10年
    private static final String CN = "localhost"; // 生产环境改为服务端实际域名/IP

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
        if (Files.exists(target)) {
            return;
        }
        File jksFile = target.toFile();
        File pemFile = configDir.resolve(PEM_CERT_PATH).toFile();

        // 证书已存在则跳过生成
        if (jksFile.exists() && pemFile.exists()) {
            System.out.println("证书文件已存在，跳过自动生成");
            return;
        }

        // 1. 生成密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(KEY_SIZE, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // 2. 创建X509证书（自签名）
        X509Certificate cert = generateX509Cert(keyPair);

        // 3. 保存为JKS密钥库（服务端使用）
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry(CERT_ALIAS, keyPair.getPrivate(), CERT_PASSWORD.toCharArray(), new Certificate[] { cert });
        try (FileOutputStream fos = new FileOutputStream(jksFile)) {
            keyStore.store(fos, CERT_PASSWORD.toCharArray());
        }
        System.out.println("JKS证书生成成功：" + jksFile.getAbsolutePath());

        // 4. 导出PEM格式公钥证书（供客户端下载）
        exportCertToPem(cert, pemFile);
        System.out.println("PEM公钥证书导出成功：" + pemFile.getAbsolutePath());
    }

    /**
     * 生成自签名X509证书
     */
    private static X509Certificate generateX509Cert(KeyPair keyPair) throws Exception {
        long now = System.currentTimeMillis();
        Date from = new Date(now);
        Date to = new Date(now + VALIDITY_DAYS * 24L * 60L * 60L * 1000L);
        BigInteger serial = new BigInteger(64, new SecureRandom());
        X500Name subject = new X500Name("CN=" + CN);

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject, serial, from, to, subject, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        GeneralName[] altNames = new GeneralName[] {};
        builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(altNames));

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
        return CERT_PASSWORD;
    }
}
