package com.cky.proxy.server.socket.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class SocketManagerConcurrencyTest {
    private static final int CONCURRENT_CONNECTIONS = 500;

    @Test
    void clientManagementSocketsRemainConsistentUnderConcurrentRegistration() throws Exception {
        List<String> tokens = new ArrayList<>();
        List<Socket> sockets = new ArrayList<>();
        for (int index = 0; index < CONCURRENT_CONNECTIONS; index++) {
            tokens.add("client-" + index);
            sockets.add(new Socket());
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> registrations = new ArrayList<>();
            for (int index = 0; index < CONCURRENT_CONNECTIONS; index++) {
                int current = index;
                registrations.add(executor.submit(
                        () -> ClientSocketManager.online(tokens.get(current), sockets.get(current))));
            }
            for (Future<Boolean> registration : registrations) {
                assertTrue(registration.get(10, TimeUnit.SECONDS));
            }

            for (int index = 0; index < CONCURRENT_CONNECTIONS; index++) {
                assertSame(sockets.get(index), ClientSocketManager.getClientSocket(tokens.get(index)));
            }

            List<Future<String>> removals = new ArrayList<>();
            for (int index = 0; index < CONCURRENT_CONNECTIONS; index++) {
                Socket socket = sockets.get(index);
                removals.add(executor.submit(() -> ClientSocketManager.offline(socket)));
            }
            for (int index = 0; index < CONCURRENT_CONNECTIONS; index++) {
                assertEquals(tokens.get(index), removals.get(index).get(10, TimeUnit.SECONDS));
            }
        }

        for (String token : tokens) {
            assertNull(ClientSocketManager.getClientSocket(token));
        }
    }

    @Test
    void duplicateManagementRegistrationDoesNotReplaceActiveSocket() {
        Socket active = new Socket();
        Socket duplicate = new Socket();

        assertTrue(ClientSocketManager.online("client", active));
        assertFalse(ClientSocketManager.online("client", duplicate));
        assertSame(active, ClientSocketManager.getClientSocket("client"));
        assertEquals("client", ClientSocketManager.offline(active));
        assertNull(ClientSocketManager.getClientSocket("client"));
        assertNull(ClientSocketManager.offline(duplicate));
    }

    @Test
    void clientDataSocketsCompleteWaitersAndRemainConsistent() throws Exception {
        List<String> userIds = new ArrayList<>();
        List<Socket> sockets = new ArrayList<>();
        List<CompletableFuture<Socket>> waiters = new ArrayList<>();
        for (int index = 0; index < CONCURRENT_CONNECTIONS; index++) {
            String userId = "user-" + index;
            userIds.add(userId);
            sockets.add(new Socket());
            waiters.add(ClientDataSocketManager.getWaitFuture(userId));
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Boolean>> registrations = new ArrayList<>();
            for (int index = 0; index < CONCURRENT_CONNECTIONS; index++) {
                int current = index;
                registrations.add(executor.submit(
                        () -> ClientDataSocketManager.online(userIds.get(current), sockets.get(current))));
            }
            for (int index = 0; index < CONCURRENT_CONNECTIONS; index++) {
                assertTrue(registrations.get(index).get(10, TimeUnit.SECONDS));
                assertSame(sockets.get(index), waiters.get(index).get(10, TimeUnit.SECONDS));
                assertSame(sockets.get(index), ClientDataSocketManager.getDataSocket(userIds.get(index)));
            }

            List<Future<?>> removals = new ArrayList<>();
            for (String userId : userIds) {
                removals.add(executor.submit(() -> ClientDataSocketManager.offline(userId)));
            }
            for (Future<?> removal : removals) {
                removal.get(10, TimeUnit.SECONDS);
            }
        }

        for (String userId : userIds) {
            assertNull(ClientDataSocketManager.getDataSocket(userId));
        }
    }

    @Test
    void staleDataSocketCloseDoesNotRemoveNewMapping() {
        Socket first = new Socket();
        Socket second = new Socket();

        assertTrue(ClientDataSocketManager.online("user", first));
        ClientDataSocketManager.offline("user", first);
        assertTrue(ClientDataSocketManager.online("user", second));
        ClientDataSocketManager.offline("user", first);

        assertSame(second, ClientDataSocketManager.getDataSocket("user"));
        assertEquals("user", ClientDataSocketManager.getUserId(second));
    }
}