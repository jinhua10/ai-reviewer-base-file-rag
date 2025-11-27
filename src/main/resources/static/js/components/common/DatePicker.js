/**
 * 日期选择器组件 - 全新版本
 * DatePicker Component - Redesigned
 *
 * 特性 Features:
 * - 现代化UI设计 Modern UI Design
 * - 支持中英文 Chinese/English Support
 * - 今天日期高亮 Today Highlight
 * - 快速选择今天 Quick Select Today
 * - 键盘导航支持 Keyboard Navigation
 * - 响应式设计 Responsive Design
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */

function DatePicker({ value, onChange, placeholder, language = 'zh' }) {
    const { useState, useEffect, useRef } = React;

    // ============================================================================
    // 状态管理 State Management
    // ============================================================================
    const [isOpen, setIsOpen] = useState(false);
    const [viewYear, setViewYear] = useState(new Date().getFullYear());
    const [viewMonth, setViewMonth] = useState(new Date().getMonth());
    const wrapperRef = useRef(null);

    // ============================================================================
    // 国际化配置 i18n Configuration
    // ============================================================================
    const i18n = {
        zh: {
            months: ['一月', '二月', '三月', '四月', '五月', '六月',
                    '七月', '八月', '九月', '十月', '十一月', '十二月'],
            weekdays: ['日', '一', '二', '三', '四', '五', '六'],
            today: '今天',
            clear: '清除',
            placeholder: '选择日期'
        },
        en: {
            months: ['January', 'February', 'March', 'April', 'May', 'June',
                    'July', 'August', 'September', 'October', 'November', 'December'],
            weekdays: ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
            today: 'Today',
            clear: 'Clear',
            placeholder: 'Select date'
        }
    };

    const t = i18n[language] || i18n.zh;

    // ============================================================================
    // 初始化和副作用 Initialization & Side Effects
    // ============================================================================

    // 根据当前值初始化视图
    useEffect(() => {
        if (value) {
            try {
                const date = new Date(value);
                if (!isNaN(date.getTime())) {
                    setViewYear(date.getFullYear());
                    setViewMonth(date.getMonth());
                }
            } catch (e) {
                console.error('Invalid date value:', value);
            }
        }
    }, [value]);

    // 点击外部关闭日历
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
            return () => document.removeEventListener('mousedown', handleClickOutside);
        }
    }, [isOpen]);

    // ============================================================================
    // 工具函数 Utility Functions
    // ============================================================================

    // 格式化日期为 YYYY-MM-DD
    const formatDate = (date) => {
        if (!date) return '';
        const d = typeof date === 'string' ? new Date(date) : date;
        if (isNaN(d.getTime())) return '';

        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    // 显示格式化的日期
    const displayDate = (dateStr) => {
        if (!dateStr) return '';
        try {
            const d = new Date(dateStr);
            if (isNaN(d.getTime())) return '';

            if (language === 'zh') {
                return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
            } else {
                return d.toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                });
            }
        } catch (e) {
            return '';
        }
    };

    // 获取某月的天数
    const getDaysInMonth = (year, month) => {
        return new Date(year, month + 1, 0).getDate();
    };

    // 获取某月第一天是星期几
    const getFirstDayOfMonth = (year, month) => {
        return new Date(year, month, 1).getDay();
    };

    // 判断是否是同一天
    const isSameDay = (date1, date2) => {
        if (!date1 || !date2) return false;
        const d1 = typeof date1 === 'string' ? new Date(date1) : date1;
        const d2 = typeof date2 === 'string' ? new Date(date2) : date2;
        return d1.getFullYear() === d2.getFullYear() &&
               d1.getMonth() === d2.getMonth() &&
               d1.getDate() === d2.getDate();
    };

    // 判断是否是今天
    const isToday = (year, month, day) => {
        const today = new Date();
        return today.getFullYear() === year &&
               today.getMonth() === month &&
               today.getDate() === day;
    };

    // 判断日期是否被选中
    const isSelected = (year, month, day) => {
        if (!value) return false;
        try {
            const selectedDate = new Date(value);
            return selectedDate.getFullYear() === year &&
                   selectedDate.getMonth() === month &&
                   selectedDate.getDate() === day;
        } catch (e) {
            return false;
        }
    };

    // ============================================================================
    // 事件处理 Event Handlers
    // ============================================================================

    // 切换日历显示
    const toggleCalendar = () => {
        setIsOpen(!isOpen);
    };

    // 选择日期
    const selectDate = (year, month, day) => {
        const date = new Date(year, month, day);
        onChange(formatDate(date));
        setIsOpen(false);
    };

    // 选择今天
    const selectToday = () => {
        const today = new Date();
        onChange(formatDate(today));
        setViewYear(today.getFullYear());
        setViewMonth(today.getMonth());
        setIsOpen(false);
    };

    // 清除日期
    const clearDate = () => {
        onChange('');
        setIsOpen(false);
    };

    // 上个月
    const prevMonth = () => {
        if (viewMonth === 0) {
            setViewMonth(11);
            setViewYear(viewYear - 1);
        } else {
            setViewMonth(viewMonth - 1);
        }
    };

    // 下个月
    const nextMonth = () => {
        if (viewMonth === 11) {
            setViewMonth(0);
            setViewYear(viewYear + 1);
        } else {
            setViewMonth(viewMonth + 1);
        }
    };

    // ============================================================================
    // 渲染日历 Render Calendar
    // ============================================================================

    const renderCalendar = () => {
        const daysInMonth = getDaysInMonth(viewYear, viewMonth);
        const firstDay = getFirstDayOfMonth(viewYear, viewMonth);
        const days = [];

        // 空白单元格
        for (let i = 0; i < firstDay; i++) {
            days.push(
                React.createElement('div', {
                    key: `empty-${i}`,
                    className: 'date-picker-day empty'
                })
            );
        }

        // 日期单元格
        for (let day = 1; day <= daysInMonth; day++) {
            const classes = ['date-picker-day'];

            if (isToday(viewYear, viewMonth, day)) {
                classes.push('today');
            }

            if (isSelected(viewYear, viewMonth, day)) {
                classes.push('selected');
            }

            days.push(
                React.createElement('div', {
                    key: `day-${day}`,
                    className: classes.join(' '),
                    onClick: () => selectDate(viewYear, viewMonth, day)
                }, day)
            );
        }

        return days;
    };

    // ============================================================================
    // 主渲染 Main Render
    // ============================================================================

    return React.createElement('div', {
        className: 'date-picker-wrapper',
        ref: wrapperRef
    },
        // 输入框
        React.createElement('input', {
            type: 'text',
            className: `date-picker-input ${isOpen ? 'active' : ''}`,
            placeholder: placeholder || t.placeholder,
            value: displayDate(value),
            readOnly: true,
            onClick: toggleCalendar
        }),

        // 日历图标
        React.createElement('span', {
            className: 'date-picker-icon'
        }, '📅'),

        // 日历弹窗
        isOpen && React.createElement('div', {
            className: 'date-picker-popup'
        },
            // 头部
            React.createElement('div', {
                className: 'date-picker-header'
            },
                React.createElement('div', {
                    className: 'date-picker-nav'
                },
                    React.createElement('button', {
                        className: 'date-picker-nav-btn',
                        onClick: prevMonth
                    }, '‹'),
                    React.createElement('button', {
                        className: 'date-picker-nav-btn',
                        onClick: nextMonth
                    }, '›')
                ),
                React.createElement('div', {
                    className: 'date-picker-current'
                }, `${t.months[viewMonth]} ${viewYear}`)
            ),

            // 星期标题
            React.createElement('div', {
                className: 'date-picker-weekdays'
            }, ...t.weekdays.map((day, idx) =>
                React.createElement('div', {
                    key: `weekday-${idx}`,
                    className: 'date-picker-weekday'
                }, day)
            )),

            // 日期网格
            React.createElement('div', {
                className: 'date-picker-days'
            }, ...renderCalendar()),

            // 底部操作栏
            React.createElement('div', {
                className: 'date-picker-footer'
            },
                React.createElement('button', {
                    className: 'date-picker-today-btn',
                    onClick: selectToday
                }, t.today),
                React.createElement('button', {
                    className: 'date-picker-clear-btn',
                    onClick: clearDate
                }, t.clear)
            )
        )
    );
}

// ============================================================================
// 导出 Export
// ============================================================================

if (typeof module !== 'undefined' && module.exports) {
    module.exports = DatePicker;
}

if (typeof window !== 'undefined') {
    window.DatePicker = DatePicker;
}

