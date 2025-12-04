package top.yumbo.ai.rag.ppl;

/**
 * PPL 服务异常
 *
 * @author AI Reviewer Team
 * @since 2025-12-04
 */
public class PPLException extends Exception {

    private final PPLProviderType providerType;
    private final String errorCode;

    public PPLException(String message) {
        super(message);
        this.providerType = null;
        this.errorCode = null;
    }

    public PPLException(String message, Throwable cause) {
        super(message, cause);
        this.providerType = null;
        this.errorCode = null;
    }

    public PPLException(PPLProviderType providerType, String message) {
        super(String.format("[%s] %s", providerType.getDisplayName(), message));
        this.providerType = providerType;
        this.errorCode = null;
    }

    public PPLException(PPLProviderType providerType, String message, Throwable cause) {
        super(String.format("[%s] %s", providerType.getDisplayName(), message), cause);
        this.providerType = providerType;
        this.errorCode = null;
    }

    public PPLException(PPLProviderType providerType, String errorCode, String message, Throwable cause) {
        super(String.format("[%s][%s] %s", providerType.getDisplayName(), errorCode, message), cause);
        this.providerType = providerType;
        this.errorCode = errorCode;
    }

    public PPLProviderType getProviderType() {
        return providerType;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

