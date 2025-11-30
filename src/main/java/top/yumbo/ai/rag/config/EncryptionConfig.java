package top.yumbo.ai.rag.config;
import lombok.Data;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密配置类 (Encryption configuration class)
 * 负责生成和管理加密密钥 (Responsible for generating and managing encryption keys)
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Data
public class EncryptionConfig {
    private boolean enabled = false;
    private String algorithm = "AES";
    private String transformation = "AES/CBC/PKCS5Padding";
    private int keySize = 256;
    private String keyStorePath = "./keys/keystore.jks";
    private String keyStorePassword;

    /**
     * 生成密钥 (Generate key)
     *
     * @return 密钥对象 (key object)
     * @throws Exception 生成异常 (generation exception)
     */
    public SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
        keyGen.init(keySize);
        return keyGen.generateKey();
    }

    /**
     * 从字符串加载密钥 (Load key from string)
     *
     * @param keyStr 密钥字符串 (key string)
     * @return 密钥对象 (key object)
     */
    public SecretKey loadKey(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, algorithm);
    }

    /**
     * 生成初始化向量 (Generate initialization vector)
     *
     * @return 初始化向量 (initialization vector)
     */
    public IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
}
