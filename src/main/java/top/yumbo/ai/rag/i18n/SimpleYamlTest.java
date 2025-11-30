package top.yumbo.ai.rag.i18n;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 简单测试 YAML 加载
 */
public class SimpleYamlTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("测试 YAML 文件加载");
        System.out.println("========================================");
        System.out.println();

        // 测试加载中文 YAML
        Map<String, String> messagesZh = new HashMap<>();
        try (InputStream is = SimpleYamlTest.class.getClassLoader()
                .getResourceAsStream("messages_zh.yml")) {
            if (is != null) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(is);
                flattenYaml("", data, messagesZh);
                System.out.println("✅ 成功加载中文消息: " + messagesZh.size() + " 个keys");
                System.out.println();

                // 显示前10个keys
                System.out.println("前10个中文keys:");
                messagesZh.entrySet().stream()
                    .limit(10)
                    .forEach(e -> System.out.println("  " + e.getKey() + " = " + e.getValue()));
            } else {
                System.out.println("❌ 未找到 messages_zh.yml");
            }
        } catch (Exception e) {
            System.out.println("❌ 加载失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();

        // 测试加载英文 YAML
        Map<String, String> messagesEn = new HashMap<>();
        try (InputStream is = SimpleYamlTest.class.getClassLoader()
                .getResourceAsStream("messages_en.yml")) {
            if (is != null) {
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(is);
                flattenYaml("", data, messagesEn);
                System.out.println("✅ 成功加载英文消息: " + messagesEn.size() + " 个keys");
                System.out.println();

                // 显示前10个keys
                System.out.println("前10个英文keys:");
                messagesEn.entrySet().stream()
                    .limit(10)
                    .forEach(e -> System.out.println("  " + e.getKey() + " = " + e.getValue()));
            } else {
                System.out.println("❌ 未找到 messages_en.yml");
            }
        } catch (Exception e) {
            System.out.println("❌ 加载失败: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("========================================");

        // 测试获取消息
        if (!messagesZh.isEmpty()) {
            System.out.println();
            System.out.println("测试获取消息:");
            testGetMessage("banner.title", messagesZh);
            testGetMessage("log.kqa.init_start", messagesZh);
            testGetMessage("log.kb.found_files", messagesZh);
            testGetMessage("error.file.not_exists", messagesZh);
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("测试完成！");
        System.out.println("========================================");
    }

    @SuppressWarnings("unchecked")
    private static void flattenYaml(String prefix, Map<String, Object> map, Map<String, String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenYaml(key, (Map<String, Object>) value, result);
            } else if (value != null) {
                result.put(key, value.toString());
            }
        }
    }

    private static void testGetMessage(String key, Map<String, String> messages) {
        String value = messages.get(key);
        if (value != null) {
            System.out.println("  ✓ " + key + " = " + value);
        } else {
            System.out.println("  ✗ " + key + " (未找到)");
        }
    }
}

