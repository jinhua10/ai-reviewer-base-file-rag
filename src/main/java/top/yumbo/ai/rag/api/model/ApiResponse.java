package top.yumbo.ai.rag.api.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 响应封装 (API response wrapper)
 *
 * 用于统一后端对外接口的返回格式，包含成功标志、消息、数据和错误信息。
 * (Used to standardize responses from backend APIs, includes success flag, message, data and error info.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 是否成功 (whether successful)
     */
    private boolean success;

    /**
     * 消息 (message)
     */
    private String message;

    /**
     * 数据 (data)
     */
    private T data;

    /**
     * 错误信息 (error info)
     */
    private String error;

    /**
     * 创建成功响应 (Create success response)
     *
     * @param data 数据 (data)
     * @return 响应对象 (response object)
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .build();
    }

    /**
     * 创建成功响应 (Create success response)
     *
     * @param message 消息 (message)
     * @param data 数据 (data)
     * @return 响应对象 (response object)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .build();
    }

    /**
     * 创建错误响应 (Create error response)
     *
     * @param error 错误信息 (error info)
     * @return 响应对象 (response object)
     */
    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(error)
            .build();
    }
}
