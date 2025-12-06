package top.yumbo.ai.rag.impl.parser.image;

import lombok.AllArgsConstructor;
import lombok.Data;
import top.yumbo.ai.rag.i18n.I18N;

/**
 * 图片位置信息（Image Position Info）
 *
 * 用于保存幻灯片/文档中图片的位置、大小等布局信息
 * 这对于理解架构图、流程图等图片之间的空间关系非常重要
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */
@Data
@AllArgsConstructor
public class ImagePositionInfo {

    /**
     * 图片数据
     */
    private byte[] imageData;

    /**
     * 图片名称
     */
    private String imageName;

    /**
     * X 坐标（相对于幻灯片左上角，单位：像素或点）
     */
    private double x;

    /**
     * Y 坐标（相对于幻灯片左上角，单位：像素或点）
     */
    private double y;

    /**
     * 宽度（单位：像素或点）
     */
    private double width;

    /**
     * 高度（单位：像素或点）
     */
    private double height;

    /**
     * 在幻灯片中的序号（从0开始）
     */
    private int index;

    /**
     * 构造函数 - 不包含位置信息
     */
    public ImagePositionInfo(byte[] imageData, String imageName, int index) {
        this.imageData = imageData;
        this.imageName = imageName;
        this.index = index;
        this.x = 0;
        this.y = 0;
        this.width = 0;
        this.height = 0;
    }

    /**
     * 获取位置描述
     * 用于生成给 Vision LLM 的提示信息
     */
    public String getPositionDescription() {
        if (width == 0 && height == 0) {
            return I18N.get("log.imageproc.position.simple_desc",
                index + 1, imageName);
        }

        // 简化的位置描述
        String verticalPos;
        if (y < 200) {
            verticalPos = I18N.get("log.imageproc.position.top");
        } else if (y > 400) {
            verticalPos = I18N.get("log.imageproc.position.bottom");
        } else {
            verticalPos = I18N.get("log.imageproc.position.middle");
        }

        String horizontalPos;
        if (x < 200) {
            horizontalPos = I18N.get("log.imageproc.position.left");
        } else if (x > 400) {
            horizontalPos = I18N.get("log.imageproc.position.right");
        } else {
            horizontalPos = I18N.get("log.imageproc.position.center");
        }

        String position = verticalPos + horizontalPos;

        return I18N.get("log.imageproc.position.full_desc",
            index + 1, imageName, position,
            String.format("%.0f", x), String.format("%.0f", y),
            String.format("%.0f", width), String.format("%.0f", height));
    }

    /**
     * 判断两张图片的相对位置关系
     */
    public static String getRelativePosition(ImagePositionInfo img1, ImagePositionInfo img2) {
        double dx = img2.x - img1.x;
        double dy = img2.y - img1.y;

        // 主要判断水平或垂直关系
        if (Math.abs(dx) > Math.abs(dy)) {
            // 水平关系更明显
            return dx > 0 ?
                I18N.get("log.imageproc.position.right") :
                I18N.get("log.imageproc.position.left");
        } else {
            // 垂直关系更明显
            return dy > 0 ?
                I18N.get("log.imageproc.position.bottom") :
                I18N.get("log.imageproc.position.top");
        }
    }
}

