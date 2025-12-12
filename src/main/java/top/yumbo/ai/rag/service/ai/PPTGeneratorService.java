package top.yumbo.ai.rag.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.yumbo.ai.rag.i18n.I18N;
import top.yumbo.ai.rag.model.service.dto.PPTGenerateRequest;
import top.yumbo.ai.rag.model.service.dto.PPTGenerateResult;
import top.yumbo.ai.rag.spring.boot.llm.LLMClient;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PPT 生成服务 (PPT Generator Service)
 *
 * 基于 LLM 和 Apache POI 的实际 PPT 生成实现
 * (Actual PPT generation implementation based on LLM and Apache POI)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */
@Slf4j
@Service
public class PPTGeneratorService {

    @Autowired(required = false)
    private LLMClient llmClient;

    /**
     * 生成 PPT (Generate PPT)
     *
     * @param request 生成请求 (Generate request)
     * @return 生成结果 (Generate result)
     */
    public PPTGenerateResult generatePPT(PPTGenerateRequest request) {
        log.info(I18N.get("ppt.generate.start"), request.getTopic());

        try {
            // 1. 验证参数 (Validate parameters)
            if (request.getTopic() == null || request.getTopic().trim().isEmpty()) {
                throw new IllegalArgumentException(I18N.get("ppt.generate.topic_required"));
            }

            // 2. 调用 LLM 生成 PPT 大纲 (Call LLM to generate PPT outline)
            List<SlideContent> slides = generateOutline(request);

            // 3. 创建 PPT 文件 (Create PPT file)
            File pptFile = createPPTFile(request.getTopic(), slides, request.getTemplate());

            // 4. 构建返回结果 (Build result)
            PPTGenerateResult result = new PPTGenerateResult();
            result.setSuccess(true);
            result.setMessage(I18N.get("ppt.generate.success"));
            result.setFileUrl("/files/ppt/" + pptFile.getName());
            result.setFileName(pptFile.getName());
            result.setFileSize(pptFile.length());

            log.info(I18N.get("ppt.generate.complete"), pptFile.getName(), pptFile.length());
            return result;

        } catch (Exception e) {
            log.error(I18N.get("ppt.generate.failed", e.getMessage()), e);

            PPTGenerateResult result = new PPTGenerateResult();
            result.setSuccess(false);
            result.setMessage(I18N.get("ppt.generate.failed", e.getMessage()));
            return result;
        }
    }

    /**
     * 生成 PPT 大纲 (Generate PPT outline)
     *
     * 调用 LLM 根据主题和内容生成大纲
     * (Call LLM to generate outline based on topic and content)
     */
    private List<SlideContent> generateOutline(PPTGenerateRequest request) {
        log.info(I18N.get("ppt.outline.generating"), request.getTopic());

        List<SlideContent> slides = new ArrayList<>();

        // 如果 LLM 客户端可用，使用 LLM 生成大纲 (If LLM client available, use LLM to generate outline)
        if (llmClient != null) {
            try {
                String prompt = buildOutlinePrompt(request);
                String llmResponse = llmClient.generate(prompt);
                slides = parseOutlineResponse(llmResponse, request.getSlides());
                log.info(I18N.get("ppt.outline.llm_success"), slides.size());
            } catch (Exception e) {
                log.warn(I18N.get("ppt.outline.llm_failed"), e.getMessage());
                // 降级到默认大纲 (Fallback to default outline)
                slides = generateDefaultOutline(request);
            }
        } else {
            // LLM 不可用，使用默认大纲 (LLM not available, use default outline)
            log.info(I18N.get("ppt.outline.default"));
            slides = generateDefaultOutline(request);
        }

        return slides;
    }

    /**
     * 构建生成大纲的提示词 (Build outline generation prompt)
     */
    private String buildOutlinePrompt(PPTGenerateRequest request) {
        return String.format(
            "请根据以下主题和内容生成一个 %d 页的 PPT 大纲。\n\n" +
            "主题：%s\n" +
            "内容：%s\n\n" +
            "请按以下格式返回大纲：\n" +
            "第1页：[标题]\n内容：[要点1、要点2、要点3]\n\n" +
            "第2页：[标题]\n内容：[要点1、要点2、要点3]\n\n" +
            "...",
            request.getSlides(),
            request.getTopic(),
            request.getContent() != null ? request.getContent() : "无额外内容"
        );
    }

    /**
     * 解析 LLM 返回的大纲 (Parse LLM outline response)
     */
    private List<SlideContent> parseOutlineResponse(String response, int maxSlides) {
        List<SlideContent> slides = new ArrayList<>();

        // 简单解析逻辑 (Simple parsing logic)
        String[] lines = response.split("\n");
        SlideContent currentSlide = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.matches("第\\d+页[：:].*") || line.startsWith("Slide ")) {
                if (currentSlide != null) {
                    slides.add(currentSlide);
                }
                String title = line.replaceAll("第\\d+页[：:]", "").replaceAll("Slide \\d+[：:]", "").trim();
                currentSlide = new SlideContent(title);
            } else if (currentSlide != null && (line.startsWith("内容：") || line.startsWith("Content:"))) {
                // 跳过内容标签
                continue;
            } else if (currentSlide != null) {
                currentSlide.addBullet(line.replaceAll("^[•\\-*]\\s*", ""));
            }
        }

        if (currentSlide != null) {
            slides.add(currentSlide);
        }

        // 限制页数 (Limit slides)
        if (slides.size() > maxSlides) {
            slides = slides.subList(0, maxSlides);
        }

        return slides;
    }

    /**
     * 生成默认大纲 (Generate default outline)
     */
    private List<SlideContent> generateDefaultOutline(PPTGenerateRequest request) {
        List<SlideContent> slides = new ArrayList<>();

        // 封面 (Cover slide)
        SlideContent cover = new SlideContent(request.getTopic());
        cover.addBullet("演示文稿");
        cover.addBullet("生成时间：" + java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        slides.add(cover);

        // 内容页 (Content slides)
        for (int i = 2; i <= request.getSlides(); i++) {
            SlideContent slide = new SlideContent("第 " + i + " 页内容");
            slide.addBullet("要点 1");
            slide.addBullet("要点 2");
            slide.addBullet("要点 3");
            slides.add(slide);
        }

        return slides;
    }

    /**
     * 创建 PPT 文件 (Create PPT file)
     */
    private File createPPTFile(String topic, List<SlideContent> slides, String template) throws Exception {
        log.info(I18N.get("ppt.file.creating"), slides.size());

        // 创建 PPT 对象 (Create PPT object)
        XMLSlideShow ppt = new XMLSlideShow();

        // 为每个大纲创建幻灯片 (Create slide for each outline)
        for (SlideContent slideContent : slides) {
            XSLFSlide slide = ppt.createSlide();

            // 添加标题 (Add title)
            XSLFTextShape title = slide.createTextBox();
            title.setAnchor(new Rectangle(50, 40, 600, 80));
            title.setText(slideContent.getTitle());
            XSLFTextParagraph titlePara = title.getTextParagraphs().get(0);
            XSLFTextRun titleRun = titlePara.getTextRuns().get(0);
            titleRun.setFontSize(32.0);
            titleRun.setBold(true);
            titleRun.setFontColor(Color.BLACK);

            // 添加内容要点 (Add bullet points)
            if (!slideContent.getBullets().isEmpty()) {
                XSLFTextShape content = slide.createTextBox();
                content.setAnchor(new Rectangle(50, 150, 600, 400));
                content.clearText();

                for (String bullet : slideContent.getBullets()) {
                    XSLFTextParagraph para = content.addNewTextParagraph();
                    para.setBullet(true);
                    para.setBulletCharacter("•");
                    XSLFTextRun run = para.addNewTextRun();
                    run.setText(bullet);
                    run.setFontSize(18.0);
                    run.setFontColor(Color.DARK_GRAY);
                }
            }
        }

        // 保存文件 (Save file)
        String fileName = topic.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_")
            + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pptx";

        File outputDir = new File("data/ppt");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir, fileName);
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            ppt.write(out);
        }
        ppt.close();

        log.info(I18N.get("ppt.file.created"), outputFile.getAbsolutePath());
        return outputFile;
    }

    /**
     * 幻灯片内容 (Slide content)
     */
    private static class SlideContent {
        private final String title;
        private final List<String> bullets;

        public SlideContent(String title) {
            this.title = title;
            this.bullets = new ArrayList<>();
        }

        public String getTitle() {
            return title;
        }

        public List<String> getBullets() {
            return bullets;
        }

        public void addBullet(String bullet) {
            this.bullets.add(bullet);
        }
    }
}

