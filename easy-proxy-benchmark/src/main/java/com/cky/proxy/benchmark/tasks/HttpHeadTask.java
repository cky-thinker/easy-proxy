package com.cky.proxy.benchmark.tasks;

import com.cky.proxy.benchmark.BenchmarkTask;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 延迟测试任务：请求 HEAD 头部以最小化数据传输影响
 */
public class HttpHeadTask implements BenchmarkTask {

    private final String url;
    private final HttpClient httpClient;

    public HttpHeadTask(String url) {
        this.url = url;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public long execute() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP request failed with status: " + response.statusCode());
        }
        return 0;
    }
}
