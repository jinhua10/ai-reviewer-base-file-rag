/**
 * 样式常量定义 / Style Constants
 * 统一管理所有组件的样式
 */

const StyleConstants = {
    // 按钮样式
    BUTTON: {
        primary: {
            padding: '7px 12px',
            fontSize: '12px',
            fontWeight: '500',
            borderRadius: '6px',
            border: 'none',
            color: 'white',
            cursor: 'pointer',
            whiteSpace: 'nowrap',
            transition: 'all 0.3s ease',
            minWidth: '70px'
        },
        gradientPurple: {
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            boxShadow: '0 2px 4px rgba(102, 126, 234, 0.3)'
        },
        gradientPink: {
            background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
            boxShadow: '0 2px 4px rgba(245, 87, 108, 0.3)'
        },
        gradientBlue: {
            background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
            boxShadow: '0 2px 4px rgba(79, 172, 254, 0.3)'
        },
        gradientGreen: {
            background: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
            boxShadow: '0 2px 4px rgba(67, 233, 123, 0.3)'
        }
    },

    // 输入框样式
    INPUT: {
        base: {
            padding: '8px 12px',
            borderRadius: '6px',
            border: '2px solid #e0e7ff',
            fontSize: '13px',
            outline: 'none',
            transition: 'all 0.3s ease'
        },
        focused: {
            borderColor: '#667eea',
            boxShadow: '0 0 0 3px rgba(102, 126, 234, 0.1)'
        }
    },

    // 下拉框样式
    SELECT: {
        base: {
            width: '160px',
            maxHeight: '180px',
            padding: '8px 12px',
            borderRadius: '6px',
            border: '2px solid #e0e7ff',
            background: 'white',
            fontSize: '13px',
            fontWeight: '500',
            color: '#4b5563',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            outline: 'none'
        }
    },

    // 卡片样式
    CARD: {
        base: {
            background: 'white',
            borderRadius: '8px',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            padding: '20px',
            marginBottom: '15px'
        }
    },

    // 颜色
    COLORS: {
        primary: '#667eea',
        secondary: '#764ba2',
        success: '#43e97b',
        danger: '#f5576c',
        warning: '#feca57',
        info: '#4facfe',
        text: '#4b5563',
        textLight: '#9ca3af',
        border: '#e0e7ff',
        background: '#f9fafb'
    },

    // 间距
    SPACING: {
        xs: '4px',
        sm: '8px',
        md: '12px',
        lg: '16px',
        xl: '20px',
        xxl: '24px'
    },

    // 工具函数：合并样式
    merge: (...styles) => Object.assign({}, ...styles),

    // 工具函数：创建按钮样式
    createButton: (type = 'primary', gradient = 'gradientPurple') => {
        return StyleConstants.merge(
            StyleConstants.BUTTON.primary,
            StyleConstants.BUTTON[gradient] || {}
        );
    },

    // 工具函数：按钮悬停事件
    onButtonHover: (e, color) => {
        e.target.style.transform = 'translateY(-2px)';
        e.target.style.boxShadow = `0 4px 8px ${color}`;
    },

    onButtonLeave: (e, color) => {
        e.target.style.transform = 'translateY(0)';
        e.target.style.boxShadow = `0 2px 4px ${color}`;
    }
};

// 导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = StyleConstants;
} else {
    window.StyleConstants = StyleConstants;
}

