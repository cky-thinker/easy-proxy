import { Message, MessageDecoder } from '../src/Message';

describe('Message', () => {
    it('should encode and decode AUTH message correctly', () => {
        const token = 'test-token-123';
        const msg = Message.createAuthMsg(token);
        
        const encoded = msg.encode();
        expect(encoded).toBeInstanceOf(Buffer);

        const decoder = new MessageDecoder();
        decoder.append(encoded);
        
        const decoded = decoder.decode();
        expect(decoded).not.toBeNull();
        expect(decoded?.type).toBe(Message.AUTH);
        expect(decoded?.token).toBe(token);
        expect(decoded?.data.length).toBe(0);
    });

    it('should encode and decode DATA message correctly', () => {
        const token = 'test-token-456';
        const data = Buffer.from('hello world', 'utf-8');
        const msg = Message.createDataMsg(token, data);
        
        const encoded = msg.encode();
        
        const decoder = new MessageDecoder();
        decoder.append(encoded);
        
        const decoded = decoder.decode();
        expect(decoded).not.toBeNull();
        expect(decoded?.type).toBe(Message.DATA);
        expect(decoded?.token).toBe(token);
        expect(decoded?.data.toString('utf-8')).toBe('hello world');
    });

    it('should handle partial data in decoder', () => {
        const token = 'test-token-789';
        const msg = Message.createAuthMsg(token);
        const encoded = msg.encode();
        
        const decoder = new MessageDecoder();
        
        // Append first half
        const half = Math.floor(encoded.length / 2);
        decoder.append(encoded.subarray(0, half));
        expect(decoder.decode()).toBeNull(); // Should not decode yet
        
        // Append second half
        decoder.append(encoded.subarray(half));
        const decoded = decoder.decode();
        expect(decoded).not.toBeNull();
        expect(decoded?.type).toBe(Message.AUTH);
        expect(decoded?.token).toBe(token);
    });

    it('should recover from invalid check value', () => {
        const token = 'valid-token';
        const msg = Message.createAuthMsg(token);
        const encoded = msg.encode();

        const decoder = new MessageDecoder();
        // Append some garbage data first
        decoder.append(Buffer.from([0x00, 0x01, 0x02, 0x03]));
        decoder.append(encoded);

        // First decode attempt might throw or we just check the logic
        // Based on the implementation, it tries to find the next check value
        const decoded = decoder.decode();
        expect(decoded).not.toBeNull();
        expect(decoded?.type).toBe(Message.AUTH);
        expect(decoded?.token).toBe(token);
    });
});
