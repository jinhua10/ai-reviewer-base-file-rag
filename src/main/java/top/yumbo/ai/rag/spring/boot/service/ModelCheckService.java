package top.yumbo.ai.rag.spring.boot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.yumbo.ai.rag.spring.boot.config.KnowledgeQAProperties;
import top.yumbo.ai.rag.i18n.I18N;

import java.io.File;
import java.io.InputStream;

/**
 * 模型检查服务
 * 在应用启动时检查向量嵌入模型是否存在
 *
 * @author AI Reviewer Team
 * @since 2025-11-22
 */
@Slf4j
@Component
@Order(1)  // 最先执行
public class ModelCheckService {

    private final KnowledgeQAProperties properties;

    public ModelCheckService(KnowledgeQAProperties properties) {
        this.properties = properties;
    }

    /**
     * 应用启动时检查模型
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkModelOnStartup() {
        // 如果未启用向量检索，跳过检查
        if (!properties.getVectorSearch().isEnabled()) {
            log.info(I18N.get("log.model.vector_disabled"));
            return;
        }

        log.info(I18N.get("log.model.sep"));
        log.info(I18N.get("log.model.checking"));
        log.info(I18N.get("log.model.sep"));

        boolean modelFound = checkModel();

        if (!modelFound) {
            printModelDownloadInstructions();

            // 退出应用
            System.exit(1);
        }

        log.info(I18N.get("log.model.sep"));
        log.info(I18N.get("log.model.passed"));
        log.info(I18N.get("log.model.sep"));
    }

    /**
     * 检查模型是否存在
     */
    private boolean checkModel() {
        var modelConfig = properties.getVectorSearch().getModel();

        // 检查所有可能的模型目录
        for (String modelDir : modelConfig.getSearchPaths()) {
            for (String fileName : modelConfig.getFileNames()) {

                // 1. 检查 resources 中是否存在
                String resourcePath = "/models/" + modelDir + "/" + fileName;
                InputStream resourceStream = getClass().getResourceAsStream(resourcePath);

                if (resourceStream != null) {
                    try {
                        resourceStream.close();
                        log.info(I18N.get("log.model.found", resourcePath));
                        log.info(I18N.get("log.model.dir_and_file", "models/" + modelDir, fileName));
                        return true;
                    } catch (Exception e) {
                        // 忽略
                    }
                }

                // 2. 检查文件系统中是否存在
                String fileSystemPath = "./models/" + modelDir + "/" + fileName;
                File file = new File(fileSystemPath);
                if (file.exists()) {
                    log.info(I18N.get("log.model.found", file.getAbsolutePath()));
                    log.info(I18N.get("log.model.dir_and_file", "models/" + modelDir, fileName));
                    return true;
                }

                // 3. 检查 src/main/resources 中是否存在
                String srcResourcePath = "src/main/resources/models/" + modelDir + "/" + fileName;
                File srcFile = new File(srcResourcePath);
                if (srcFile.exists()) {
                    log.info(I18N.get("log.model.found", srcFile.getAbsolutePath()));
                    log.info(I18N.get("log.model.dir_and_file", "models/" + modelDir, fileName));
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 打印模型下载说明
     */
    private void printModelDownloadInstructions() {
        log.error(I18N.get("log.model.not_found_title"));
        log.error(I18N.get("log.model.list_header"));
        log.error(I18N.get("log.model.recommendation_1"));
        log.error(I18N.get("log.model.recommendation_1_link"));
        log.error(I18N.get("log.model.recommendation_1_path"));
        log.error(I18N.get("log.model.recommendation_2"));
        log.error(I18N.get("log.model.recommendation_2_link"));
        log.error(I18N.get("log.model.recommendation_2_path"));
        log.error(I18N.get("log.model.recommendation_3"));
        log.error(I18N.get("log.model.recommendation_3_link"));
        log.error(I18N.get("log.model.recommendation_3_path"));
        log.error(I18N.get("log.model.recommendation_4"));
        log.error(I18N.get("log.model.recommendation_4_link"));
        log.error(I18N.get("log.model.recommendation_4_path"));
        log.error(I18N.get("log.model.sep"));
        log.error(I18N.get("log.model.download_quick"));
        log.error(I18N.get("log.model.sep"));
        log.error(I18N.get("log.model.quick_method1"));
        log.error(I18N.get("log.model.quick_method1_code1"));
        log.error(I18N.get("log.model.quick_method1_code2"));
        log.error(I18N.get("log.model.quick_method1_code3"));
        log.error(I18N.get("log.model.quick_method1_code4"));
        log.error(I18N.get("log.model.sep"));
        log.error(I18N.get("log.model.quick_method2"));
        log.error(I18N.get("log.model.quick_method2_steps"));
        log.error(I18N.get("log.model.sep"));
        log.error(I18N.get("log.model.searched_locations"));
        var modelConfig = properties.getVectorSearch().getModel();
        for (String modelDir : modelConfig.getSearchPaths()) {
            log.error(I18N.get("log.model.searched_path", modelDir));
        }
        log.error(I18N.get("log.model.docs"));
    }
}
