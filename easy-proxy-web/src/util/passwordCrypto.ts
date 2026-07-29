import forge from 'node-forge'

const publicKeyCache = new Map<string, forge.pki.rsa.PublicKey>()

const getPublicKey = (pem: string): forge.pki.rsa.PublicKey => {
    if (!publicKeyCache.has(pem)) {
        publicKeyCache.set(pem, forge.pki.publicKeyFromPem(pem) as forge.pki.rsa.PublicKey)
    }
    return publicKeyCache.get(pem)!
}

/**
 * 使用 RSA-OAEP (SHA-256) 加密密码
 * 该实现使用 node-forge 库，支持在非安全上下文（HTTP）中运行
 */
export const encryptPassword = async (password: string, publicKeyPem: string): Promise<string> => {
    if (!password) {
        throw new Error('密码不能为空')
    }
    if (!publicKeyPem) {
        throw new Error('未获取到密码加密公钥')
    }

    try {
        const publicKey = getPublicKey(publicKeyPem)

        // 使用 RSA-OAEP 加密，配合 SHA-256 哈希函数，与后端 Java 实现保持一致
        // 后端：RSA/ECB/OAEPWithSHA-256AndMGF1Padding
        const encrypted = publicKey.encrypt(password, 'RSA-OAEP', {
            md: forge.md.sha256.create(),
            mgf1: {
                md: forge.md.sha256.create(),
            },
        })

        // 将二进制字符串转换为 Base64
        return forge.util.encode64(encrypted)
    } catch (error) {
        console.error('Password encryption failed:', error)
        throw new Error('密码加密失败')
    }
}
