package top.yumbo.ai.rag.ppl;

/**
 * PPL 服务异常 (PPL Service Exception)
 * 
 * 用于封装 PPL 服务处理过程中可能出现的各种异常情况
 * (Used to encapsulate various exception situations that may occur during PPL service processing)
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
public class PPLException extends Exception {

    /**
     * 提供商类型 (Provider type)
     * 标识异常发生的 PPL 服务提供商
     * (Identifies the PPL service provider where the exception occurred)
     */
    private final PPLProviderType providerType;
    
    /**
     * 错误代码 (Error code)
     * 用于精确定位错误类型
     * (Used to precisely locate the error type)
     */
    private final String errorCode;

    /**
     * 构造函数 (Constructor)
     * 
     * @param message 异常信息 (Exception message)
     */
    public PPLException(String message) {
        super(message);
        this.providerType = null;
        this.errorCode = null;
    }

    /**
     * 构造函数 (Constructor)
     * 
     * @param message 异常信息 (Exception message)
     * @param cause 原因异常 (Cause exception)
     */
    public PPLException(String message, Throwable cause) {
        super(message, cause);
        this.providerType = null;
        this.errorCode = null;
    }

    /**
     * 构造函数 (Constructor)
     * 
     * @param providerType 提供商类型 (Provider type)
     * @param message 异常信息 (Exception message)
     */
    public PPLException(PPLProviderType providerType, String message) {
        super(String.format("[%s] %s", providerType.getDisplayName(), message));
        this.providerType = providerType;
        this.errorCode = null;
    }

    /**
     * 构造函数 (Constructor)
     * 
     * @param providerType 提供商类型 (Provider type)
     * @param message 异常信息 (Exception message)
     * @param cause 原因异常 (Cause exception)
     */
    public PPLException(PPLProviderType providerType, String message, Throwable cause) {
        super(String.format("[%s] %s", providerType.getDisplayName(), message), cause);
        this.providerType = providerType;
        this.errorCode = null;
    }

    /**
     * 构造函数 (Constructor)
     * 
     * @param providerType 提供商类型 (Provider type)
     * @param errorCode 错误代码 (Error code)
     * @param message 异常信息 (Exception message)
     * @param cause 原因异常 (Cause exception)
     */
    public PPLException(PPLProviderType providerType, String errorCode, String message, Throwable cause) {
        super(String.format("[%s][%s] %s", providerType.getDisplayName(), errorCode, message), cause);
        this.providerType = providerType;
        this.errorCode = errorCode;
    }

    /**
     * 获取提供商类型 (Get provider type)
     * 
     * @return 提供商类型 (Provider type)
     */
    public PPLProviderType getProviderType() {
        return providerType;
    }

    /**
     * 获取错误代码 (Get error code)
     * 
     * @return 错误代码 (Error code)
     */
    public String getErrorCode() {
        return errorCode;
    }
}

