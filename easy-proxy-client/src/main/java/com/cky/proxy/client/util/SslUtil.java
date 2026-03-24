package com.cky.proxy.client.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SslUtil {
    private static SSLSocketFactory sslSocketFactory;

    public static synchronized SSLSocketFactory getSslSocketFactory() throws Exception {
        if (sslSocketFactory == null) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert;
            try (InputStream is = new FileInputStream(CertDownloader.getPemCertPath())) {
                cert = (X509Certificate) cf.generateCertificate(is);
            }
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("server", cert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            sslSocketFactory = sslContext.getSocketFactory();
        }
        return sslSocketFactory;
    }
}