package top.yumbo.ai.rag.security;

import lombok.extern.slf4j.Slf4j;
import top.yumbo.ai.rag.config.EncryptionConfig;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;

/**
 * 加密服务（Encryption service）
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
public class EncryptionService {
    
    private final EncryptionConfig config;
    private final SecretKey secretKey;
    
    public EncryptionService(EncryptionConfig config, SecretKey secretKey) {
        this.config = config;
        this.secretKey = secretKey;
    }
    
    /**
     * 加密数据
     */
    public String encrypt(String plainText) throws Exception {
        if (!config.isEnabled()) {
            return plainText;
        }
        
        IvParameterSpec iv = config.generateIv();
        Cipher cipher = Cipher.getInstance(config.getTransformation());
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        
        // 将IV和加密数据一起返回
        byte[] ivBytes = iv.getIV();
        byte[] combined = new byte[ivBytes.length + encrypted.length];
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
        System.arraycopy(encrypted, 0, combined, ivBytes.length, encrypted.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    /**
     * 解密数据
     */
    public String decrypt(String encryptedText) throws Exception {
        if (!config.isEnabled()) {
            return encryptedText;
        }
        
        byte[] combined = Base64.getDecoder().decode(encryptedText);
        
        // 提取IV
        byte[] ivBytes = new byte[16];
        System.arraycopy(combined, 0, ivBytes, 0, 16);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        
        // 提取加密数据
        byte[] encrypted = new byte[combined.length - 16];
        System.arraycopy(combined, 16, encrypted, 0, encrypted.length);
        
        Cipher cipher = Cipher.getInstance(config.getTransformation());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted);
    }
    
    /**
     * 加密文件内容
     */
    public byte[] encryptBytes(byte[] data) throws Exception {
        if (!config.isEnabled()) {
            return data;
        }
        
        IvParameterSpec iv = config.generateIv();
        Cipher cipher = Cipher.getInstance(config.getTransformation());
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        
        byte[] encrypted = cipher.doFinal(data);
        
        // 将IV和加密数据组合
        byte[] ivBytes = iv.getIV();
        byte[] combined = new byte[ivBytes.length + encrypted.length];
        System.arraycopy(ivBytes, 0, combined, 0, ivBytes.length);
        System.arraycopy(encrypted, 0, combined, ivBytes.length, encrypted.length);
        
        return combined;
    }
    
    /**
     * 解密文件内容
     */
    public byte[] decryptBytes(byte[] encryptedData) throws Exception {
        if (!config.isEnabled()) {
            return encryptedData;
        }
        
        // 提取IV
        byte[] ivBytes = new byte[16];
        System.arraycopy(encryptedData, 0, ivBytes, 0, 16);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        
        // 提取加密数据
        byte[] encrypted = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 16, encrypted, 0, encrypted.length);
        
        Cipher cipher = Cipher.getInstance(config.getTransformation());
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        
        return cipher.doFinal(encrypted);
    }
}