import * as fs from 'fs';
import * as http from 'http';
import * as path from 'path';
import { CertDownloader } from '../src/CertDownloader';

jest.mock('fs');
jest.mock('http');

describe('CertDownloader', () => {
    const configDir = '/mock/config';
    const serverIp = '127.0.0.1';
    const webPort = 8080;

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should return cert path if exists', async () => {
        (fs.existsSync as jest.Mock).mockImplementation((pathStr) => {
            return pathStr === configDir || pathStr === path.join(configDir, 'cert.pem');
        });

        const certPath = await CertDownloader.downloadIfNotExists(serverIp, webPort, configDir);
        expect(certPath).toBe(path.join(configDir, 'cert.pem'));
        expect(http.get).not.toHaveBeenCalled();
    });

    it('should create directory if it does not exist', async () => {
        (fs.existsSync as jest.Mock).mockReturnValue(false);
        const mockWriteStream = {
            on: jest.fn(),
            close: jest.fn()
        };
        (fs.createWriteStream as jest.Mock).mockReturnValue(mockWriteStream);

        const mockResponse = {
            statusCode: 200,
            pipe: jest.fn(),
            on: jest.fn()
        };

        const mockRequest = {
            on: jest.fn().mockReturnThis()
        };

        (http.get as jest.Mock).mockImplementation((url, callback) => {
            callback(mockResponse);
            return mockRequest;
        });

        // We simulate the file finish event asynchronously
        setTimeout(() => {
            const finishCallback = mockWriteStream.on.mock.calls.find(call => call[0] === 'finish')[1];
            finishCallback();
        }, 10);

        const certPath = await CertDownloader.downloadIfNotExists(serverIp, webPort, configDir);
        
        expect(fs.mkdirSync).toHaveBeenCalledWith(configDir, { recursive: true });
        expect(certPath).toBe(path.join(configDir, 'cert.pem'));
        expect(http.get).toHaveBeenCalled();
    });
});
