import { EasyProxyClient } from '../src/EasyProxyClient';
import { CertDownloader } from '../src/CertDownloader';
import * as tls from 'tls';
import * as net from 'net';
import * as fs from 'fs';
import { EventEmitter } from 'events';

jest.mock('tls');
jest.mock('net');
jest.mock('fs');
jest.mock('../src/CertDownloader');

describe('EasyProxyClient', () => {
    let client: EasyProxyClient;

    beforeEach(() => {
        jest.clearAllMocks();

        (CertDownloader.downloadIfNotExists as jest.Mock).mockResolvedValue('/mock/cert.pem');
        (fs.readFileSync as jest.Mock).mockReturnValue(Buffer.from('mock-cert'));

        const config = {
            serverIp: '127.0.0.1',
            serverPort: 8888,
            webPort: 8080,
            token: 'test-token',
            configDir: '/mock/config'
        };

        client = new EasyProxyClient(config);
    });

    it('should start and connect to mng server', async () => {
        const mockTlsSocket = new EventEmitter() as any;
        mockTlsSocket.write = jest.fn();
        mockTlsSocket.destroy = jest.fn();

        (tls.connect as jest.Mock).mockReturnValue(mockTlsSocket);

        await client.start();

        // Ensure cert was downloaded and read
        expect(CertDownloader.downloadIfNotExists).toHaveBeenCalled();
        expect(fs.readFileSync).toHaveBeenCalledWith('/mock/cert.pem');

        // Ensure tls connect was called
        expect(tls.connect).toHaveBeenCalled();

        // Simulate secure connect
        const connectedSpy = jest.fn();
        client.on('connected', connectedSpy);
        
        mockTlsSocket.emit('secureConnect');
        
        expect(connectedSpy).toHaveBeenCalled();
        expect(mockTlsSocket.write).toHaveBeenCalled(); // Auth message should be sent
    });

    it('should handle disconnect and retry', async () => {
        jest.useFakeTimers();

        const mockTlsSocket = new EventEmitter() as any;
        mockTlsSocket.write = jest.fn();
        mockTlsSocket.destroy = jest.fn();

        (tls.connect as jest.Mock).mockReturnValue(mockTlsSocket);

        await client.start();

        // Simulate close
        mockTlsSocket.emit('close');

        // Expect tls.connect to be called again after retry wait time
        expect(tls.connect).toHaveBeenCalledTimes(1);
        
        jest.advanceTimersByTime(2500); // Wait > 2000ms
        
        expect(tls.connect).toHaveBeenCalledTimes(2);

        jest.useRealTimers();
    });

    it('should stop and clear sockets', async () => {
        const mockTlsSocket = new EventEmitter() as any;
        mockTlsSocket.write = jest.fn();
        mockTlsSocket.destroy = jest.fn();

        (tls.connect as jest.Mock).mockReturnValue(mockTlsSocket);

        await client.start();

        client.stop();
        expect(mockTlsSocket.destroy).toHaveBeenCalled();
    });
});
