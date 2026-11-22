package com.cky.proxy.server.socket.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class TrafficStatisticManagerTest {
    private static final AtomicInteger NEXT_RULE_ID = new AtomicInteger(1_900_000_000);

    @Test
    void tracksActiveConnectionsAndLifetimeTrafficWithoutDoubleCounting() {
        int ruleId = NEXT_RULE_ID.incrementAndGet();
        String userId = "traffic-test-" + ruleId;
        long uploadBefore = TrafficStatisticManager.getTotalUploadBytes();
        long downloadBefore = TrafficStatisticManager.getTotalDownloadBytes();

        try {
            TrafficStatisticManager.addConnection(userId, 1, ruleId);
            assertEquals(1, TrafficStatisticManager.getActiveConnections(ruleId));
            TrafficStatisticManager.addConnection(userId, 1, ruleId);
            assertEquals(1, TrafficStatisticManager.getActiveConnections(ruleId));

            TrafficStatisticManager.addUpload(userId, 1234);
            TrafficStatisticManager.addDownload(userId, 5678);

            assertTrue(TrafficStatisticManager.getTotalUploadBytes() >= uploadBefore + 1234);
            assertTrue(TrafficStatisticManager.getTotalDownloadBytes() >= downloadBefore + 5678);
        }
        finally {
            TrafficStatisticManager.removeConnection(userId);
        }

        assertEquals(0, TrafficStatisticManager.getActiveConnections(ruleId));
        TrafficStatisticManager.removeConnection(userId);
        assertEquals(0, TrafficStatisticManager.getActiveConnections(ruleId));
    }
}