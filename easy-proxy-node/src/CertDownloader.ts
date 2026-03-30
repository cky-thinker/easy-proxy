import * as http from 'http';
import * as fs from 'fs';
import * as path from 'path';

export class CertDownloader {
    static getPemCertPath(configDir: string): string {
        return path.join(configDir, 'cert.pem');
    }

    static async downloadIfNotExists(serverIp: string, webPort: number, configDir: string): Promise<string> {
        const certPath = this.getPemCertPath(configDir);
        
        if (!fs.existsSync(configDir)) {
            fs.mkdirSync(configDir, { recursive: true });
        }

        if (fs.existsSync(certPath)) {
            return certPath;
        }

        const url = `http://${serverIp}:${webPort}/api/open/cert.pem`;
        
        return new Promise((resolve, reject) => {
            const file = fs.createWriteStream(certPath);
            http.get(url, (response) => {
                if (response.statusCode !== 200) {
                    fs.unlinkSync(certPath);
                    return reject(new Error(`Failed to download cert, status code: ${response.statusCode}`));
                }
                
                response.pipe(file);
                file.on('finish', () => {
                    file.close();
                    resolve(certPath);
                });
            }).on('error', (err) => {
                fs.unlinkSync(certPath);
                reject(err);
            });
        });
    }
}
