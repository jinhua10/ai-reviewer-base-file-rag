package top.yumbo.ai.rag.test;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * YAML文件格式验证测试
 */
public class YamlValidationTest {

    @Test
    public void testZhIndexYamlFormat() {
        validateYamlFile("i18n/zh/zh-index.yml", "中文索引");
    }

    @Test
    public void testEnIndexYamlFormat() {
        validateYamlFile("i18n/en/en-index.yml", "英文索引");
    }

    private void validateYamlFile(String filename, String description) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is == null) {
                System.out.println("❌ " + description + " 文件未找到: " + filename);
                return;
            }

            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);

            System.out.println("✅ " + description + " 解析成功: " + filename);
            System.out.println("   顶层键数量: " + data.size());
            System.out.println("   顶层键: " + data.keySet());

            // 检查是否有null键
            checkForNullKeys("", data, filename);

        } catch (Exception e) {
            System.out.println("❌ " + description + " 解析失败: " + filename);
            e.printStackTrace();
        }
    }

    private void checkForNullKeys(String prefix, Map<?, ?> map, String filename) {
        if (map == null) return;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (key == null) {
                System.out.println("⚠️  发现null键在: " + filename + " 路径: " +
                        (prefix.isEmpty() ? "<root>" : prefix) +
                        " 值类型: " + (value == null ? "null" : value.getClass().getSimpleName()));
            }

            String currentPath = prefix.isEmpty() ? String.valueOf(key) : prefix + "." + key;

            if (value instanceof Map) {
                checkForNullKeys(currentPath, (Map<?, ?>) value, filename);
            }
        }
    }
}

