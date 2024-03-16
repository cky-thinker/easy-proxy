package com.cky.proxy.server.verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;

public class ServerVerticle extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    NetServerOptions options = new NetServerOptions()
      .setSsl(true)
      .setClientAuth(ClientAuth.REQUIRED).
      setPemTrustOptions(
        new PemTrustOptions()
          .addCertPath("/Users/chenkeyu/workspace/test/ca.cer")
          .addCertPath("/Users/chenkeyu/workspace/test/ssl/client/client.crt")
      )
      .setPemKeyCertOptions(
        new PemKeyCertOptions().
          setKeyPath("/Users/chenkeyu/workspace/test/ssl/server/serverkey.pem").
          setCertPath("/Users/chenkeyu/workspace/test/ssl/server/server.cer")
      );
    vertx.createNetServer(options).connectHandler((e) -> {
    }).listen(9000);
  }
}
