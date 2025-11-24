package top.yumbo.ai.rag.config;
import lombok.Data;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;
@Data
public class EncryptionConfig {
    private boolean enabled = false;
    private String algorithm = "AES";
    private String transformation = "AES/CBC/PKCS5Padding";
    private int keySize = 256;
    private String keyStorePath = "./keys/keystore.jks";
    private String keyStorePassword;
    public SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
        keyGen.init(keySize);
        return keyGen.generateKey();
    }
    public SecretKey loadKey(String keyStr) {
        byte[] decodedKey = Base64.getDecoder().decode(keyStr);
        return new SecretKeySpec(decodedKey, algorithm);
    }
    public IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }
}
