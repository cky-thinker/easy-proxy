package com.cky.proxy.benchmark;

/**
 * 具体测试任务接口
 */
public interface BenchmarkTask {
    /**
     * 执行单次测试动作
     * @return 传输的字节数（如果不涉及数据传输则返回0）
     * @throws Exception 执行失败时抛出异常
     */
    long execute() throws Exception;
}
