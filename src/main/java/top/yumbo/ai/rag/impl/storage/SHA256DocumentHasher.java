package top.yumbo.ai.rag.impl.storage;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * SHA-256文档哈希器
 * 用于计算文档内容的哈希值，实现去重功能
 *
 * @author AI Reviewer Team
 * @since 2025-11-21
 */
@Slf4j
public class SHA256DocumentHasher {

    private static final String ALGORITHM = "SHA-256";

    /**
     * 计算内容哈希
     *
     * @param content 内容字节数组
     * @return Base64编码的哈希值
     */
    public String computeHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(content);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to compute hash: algorithm not found", e);
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    /**
     * 计算字符串内容哈希
     *
     * @param content 内容字符串
     * @return Base64编码的哈希值
     */
    public String computeHash(String content) {
        if (content == null) {
            return null;
        }
        return computeHash(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证内容是否匹配哈希值
     *
     * @param content 内容
     * @param hash 哈希值
     * @return 是否匹配
     */
    public boolean verify(String content, String hash) {
        if (content == null || hash == null) {
            return false;
        }
        String computed = computeHash(content);
        return computed.equals(hash);
    }
}

