package top.yumbo.ai.rag.p2p;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.i18n.I18N;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Base64;

/**
 * P2P 加密处理器 (P2P Encryption Handler)
 *
 * 功能 (Features):
 * 1. AES-256-GCM 加密/解密 (AES-256-GCM encryption/decryption)
 * 2. RSA 密钥交换 (RSA key exchange)
 * 3. 数字签名验证 (Digital signature verification)
 *
 * @author AI Assistant
 * @since 2025-12-12
 */
@Slf4j
@Data
public class P2PEncryptionHandler {

    /**
     * AES 加密算法 (AES encryption algorithm)
     */
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";

    /**
     * AES 密钥长度 (AES key length)
     */
    private static final int AES_KEY_SIZE = 256;

    /**
     * GCM 标签长度 (GCM tag length)
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * GCM IV 长度 (GCM IV length)
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * RSA 算法 (RSA algorithm)
     */
    private static final String RSA_ALGORITHM = "RSA";

    /**
     * RSA 密钥长度 (RSA key length)
     */
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * 会话密钥 (Session key)
     */
    private SecretKey sessionKey;

    /**
     * RSA 密钥对 (RSA key pair)
     */
    private KeyPair rsaKeyPair;

    // ========== 初始化 (Initialization) ==========

    public P2PEncryptionHandler() {
        try {
            // 生成会话密钥 (Generate session key)
            generateSessionKey();

            // 生成 RSA 密钥对 (Generate RSA key pair)
            generateRSAKeyPair();

            log.info(I18N.get("p2p.encryption.initialized"));

        } catch (Exception e) {
            log.error(I18N.get("p2p.encryption.init_failed"), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成会话密钥 (Generate session key)
     */
    private void generateSessionKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(AES_KEY_SIZE);
        sessionKey = keyGenerator.generateKey();

        log.debug(I18N.get("p2p.encryption.session_key_generated"));
    }

    /**
     * 生成 RSA 密钥对 (Generate RSA key pair)
     */
    private void generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_SIZE);
        rsaKeyPair = keyPairGenerator.generateKeyPair();

        log.debug(I18N.get("p2p.encryption.rsa_key_generated"));
    }

    // ========== AES 加密/解密 (AES Encryption/Decryption) ==========

    /**
     * 加密数据 (Encrypt data)
     *
     * @param plaintext 明文 (Plaintext)
     * @return 加密结果（Base64编码） (Encrypted result, Base64 encoded)
     */
    public String encrypt(String plaintext) {
        try {
            // 生成随机 IV (Generate random IV)
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 初始化加密器 (Initialize cipher)
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, sessionKey, parameterSpec);

            // 加密 (Encrypt)
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            // 组合 IV 和密文 (Combine IV and ciphertext)
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

            // Base64 编码 (Base64 encode)
            String result = Base64.getEncoder().encodeToString(encrypted);

            log.debug(I18N.get("p2p.encryption.encrypted"), plaintext.length(), encrypted.length);
            return result;

        } catch (Exception e) {
            log.error(I18N.get("p2p.encryption.encrypt_failed"), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解密数据 (Decrypt data)
     *
     * @param encryptedData 加密数据（Base64编码） (Encrypted data, Base64 encoded)
     * @return 明文 (Plaintext)
     */
    public String decrypt(String encryptedData) {
        try {
            // Base64 解码 (Base64 decode)
            byte[] encrypted = Base64.getDecoder().decode(encryptedData);

            // 提取 IV (Extract IV)
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, iv.length);

            // 提取密文 (Extract ciphertext)
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            // 初始化解密器 (Initialize cipher)
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, sessionKey, parameterSpec);

            // 解密 (Decrypt)
            byte[] plaintext = cipher.doFinal(ciphertext);

            String result = new String(plaintext);
            log.debug(I18N.get("p2p.encryption.decrypted"), encrypted.length, plaintext.length);

            return result;

        } catch (Exception e) {
            log.error(I18N.get("p2p.encryption.decrypt_failed"), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // ========== 密钥交换 (Key Exchange) ==========

    /**
     * 获取公钥（Base64编码） (Get public key, Base64 encoded)
     */
    public String getPublicKey() {
        byte[] publicKeyBytes = rsaKeyPair.getPublic().getEncoded();
        return Base64.getEncoder().encodeToString(publicKeyBytes);
    }

    /**
     * 使用 RSA 加密会话密钥 (Encrypt session key with RSA)
     *
     * @param publicKeyBase64 对方的公钥（Base64编码） (Peer's public key, Base64 encoded)
     * @return 加密的会话密钥（Base64编码） (Encrypted session key, Base64 encoded)
     */
    public String encryptSessionKey(String publicKeyBase64) {
        try {
            // 解码公钥 (Decode public key)
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePublic(new java.security.spec.X509EncodedKeySpec(publicKeyBytes));

            // RSA 加密会话密钥 (RSA encrypt session key)
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedKey = cipher.doFinal(sessionKey.getEncoded());

            log.debug(I18N.get("p2p.encryption.session_key_encrypted"));
            return Base64.getEncoder().encodeToString(encryptedKey);

        } catch (Exception e) {
            log.error(I18N.get("p2p.encryption.key_exchange_failed"), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用 RSA 解密会话密钥 (Decrypt session key with RSA)
     *
     * @param encryptedKeyBase64 加密的会话密钥（Base64编码） (Encrypted session key, Base64 encoded)
     */
    public void decryptSessionKey(String encryptedKeyBase64) {
        try {
            // 解码加密的密钥 (Decode encrypted key)
            byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyBase64);

            // RSA 解密 (RSA decrypt)
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
            byte[] keyBytes = cipher.doFinal(encryptedKey);

            // 设置会话密钥 (Set session key)
            sessionKey = new SecretKeySpec(keyBytes, "AES");

            log.debug(I18N.get("p2p.encryption.session_key_decrypted"));

        } catch (Exception e) {
            log.error(I18N.get("p2p.encryption.key_exchange_failed"), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // ========== 数字签名 (Digital Signature) ==========

    /**
     * 对数据签名 (Sign data)
     *
     * @param data 数据 (Data)
     * @return 签名（Base64编码） (Signature, Base64 encoded)
     */
    public String sign(String data) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(rsaKeyPair.getPrivate());
            signature.update(data.getBytes());

            byte[] signatureBytes = signature.sign();

            log.debug(I18N.get("p2p.encryption.signed"), data.length());
            return Base64.getEncoder().encodeToString(signatureBytes);

        } catch (Exception e) {
            log.error(I18N.get("p2p.encryption.sign_failed"), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 验证签名 (Verify signature)
     *
     * @param data 数据 (Data)
     * @param signatureBase64 签名（Base64编码） (Signature, Base64 encoded)
     * @param publicKeyBase64 公钥（Base64编码） (Public key, Base64 encoded)
     * @return 是否验证通过 (Is verified)
     */
    public boolean verify(String data, String signatureBase64, String publicKeyBase64) {
        try {
            // 解码公钥 (Decode public key)
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance(RSA_ALGORITHM)
                .generatePublic(new java.security.spec.X509EncodedKeySpec(publicKeyBytes));

            // 解码签名 (Decode signature)
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            // 验证签名 (Verify signature)
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes());

            boolean verified = signature.verify(signatureBytes);
            log.debug(I18N.get("p2p.encryption.verified"), verified);

            return verified;

        } catch (Exception e) {
            log.error(I18N.get("p2p.encryption.verify_failed"), e.getMessage(), e);
            return false;
        }
    }
}

