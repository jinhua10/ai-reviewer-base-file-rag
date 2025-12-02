package top.yumbo.ai.rag.spring.boot.llm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class OpenAILLMClientTest {

    /**
     * 读取图片文件并转换为 base64 字符串（带 data URI 前缀）
     * Read image file and convert to base64 string (with data URI prefix)
     *
     * @param imagePath 图片文件路径 / Image file path
     * @return base64 格式的图片字符串 / Base64 encoded image string
     * @throws IOException 读取文件失败 / File reading failed
     */
    private String imageToBase64(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);

        // 根据文件扩展名确定 MIME 类型 / Determine MIME type by file extension
        String mimeType = getMimeType(imagePath);

        // 返回 data URI 格式 / Return data URI format
        return "data:" + mimeType + ";base64," + base64;
    }

    /**
     * 根据文件扩展名获取 MIME 类型 / Get MIME type by file extension
     */
    private String getMimeType(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "image/jpeg"; // 默认 / default
    }

    @Test
    void testGenerate() throws Exception {
        // 检查环境变量是否存在 / Check if environment variable exists
        String apiKey = System.getenv("QW_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("跳过测试：未设置 QW_API_KEY 环境变量 / Test skipped: QW_API_KEY not set");
            return;
        }

        String imgPath = "E:\\excel1\\1.jpg";
        Path path = Paths.get(imgPath);

        // 检查图片文件是否存在 / Check if image file exists
        if (!Files.exists(path)) {
            System.out.println("跳过测试：图片文件不存在: " + imgPath);
            return;
        }

        LLMClient qw = new OpenAILLMClient(apiKey,
                "qwen-vl-plus",
                "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");

        // 读取图片并转换为 base64 / Read image and convert to base64
        String base64Image = imageToBase64(imgPath);

        // 调用 API 生成描述 / Call API to generate description
        String result = qw.generateWithImage("请描述图片内容",
                base64Image,
                null);

        // 验证结果不为空 / Verify result is not empty
        assertNotNull(result, "结果不应为空 / Result should not be null");
        assertFalse(result.isEmpty(), "结果不应为空字符串 / Result should not be empty");

        System.out.println("图片描述结果 / Image description result:");
        System.out.println(result);
    }
}
