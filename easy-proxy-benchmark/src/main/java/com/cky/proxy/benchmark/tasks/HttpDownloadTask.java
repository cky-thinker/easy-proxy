package com.cky.proxy.benchmark.tasks;

import com.cky.proxy.benchmark.BenchmarkTask;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 吞吐量测试任务：通过下载指定 URL 的内容来衡量
 */
public class HttpDownloadTask implements BenchmarkTask {

    private final String url;
    private final HttpClient httpClient;

    public HttpDownloadTask(String url) {
        this.url = url;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public long execute() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP request failed with status: " + response.statusCode());
        }
        return response.body().length;
    }
}
