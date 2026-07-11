const encoder = new TextEncoder()
const publicKeyCache = new Map<string, Promise<CryptoKey>>()

const pemToArrayBuffer = (pem: string): ArrayBuffer => {
  const cleaned = pem
    .replace('-----BEGIN PUBLIC KEY-----', '')
    .replace('-----END PUBLIC KEY-----', '')
    .replace(/\s+/g, '')

  const binary = window.atob(cleaned)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

const importPublicKey = async (pem: string): Promise<CryptoKey> => {
  const spki = pemToArrayBuffer(pem)
  return await crypto.subtle.importKey(
    'spki',
    spki,
    {
      name: 'RSA-OAEP',
      hash: 'SHA-256'
    },
    false,
    ['encrypt']
  )
}

const getPublicKey = async (pem: string): Promise<CryptoKey> => {
  if (!publicKeyCache.has(pem)) {
    publicKeyCache.set(pem, importPublicKey(pem))
  }
  return await publicKeyCache.get(pem)!
}

export const encryptPassword = async (password: string, publicKeyPem: string): Promise<string> => {
  if (!password) {
    throw new Error('密码不能为空')
  }
  if (!publicKeyPem) {
    throw new Error('未获取到密码加密公钥')
  }

  const key = await getPublicKey(publicKeyPem)
  const encrypted = await crypto.subtle.encrypt(
    {
      name: 'RSA-OAEP'
    },
    key,
    encoder.encode(password)
  )

  const bytes = new Uint8Array(encrypted)
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }
  return window.btoa(binary)
}
