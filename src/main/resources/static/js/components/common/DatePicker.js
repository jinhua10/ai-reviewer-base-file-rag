/**
 * 日期选择器组件 / DatePicker Component
 * 支持中英文、日历选择
 */

function DatePicker({ value, onChange, placeholder, language }) {
    // 获取React hooks（避免重复声明）
    const { useState, useEffect, useRef } = React;

    const [showCalendar, setShowCalendar] = useState(false);
    const [currentYear, setCurrentYear] = useState(new Date().getFullYear());
    const [currentMonth, setCurrentMonth] = useState(new Date().getMonth());
    const calendarRef = useRef(null);

    // 如果有值，初始化到该日期
    useEffect(() => {
        if (value) {
            const date = new Date(value);
            setCurrentYear(date.getFullYear());
            setCurrentMonth(date.getMonth());
        }
    }, [value]);

    // 点击外部关闭日历
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (calendarRef.current && !calendarRef.current.contains(event.target)) {
                setShowCalendar(false);
            }
        };
        if (showCalendar) {
            document.addEventListener('mousedown', handleClickOutside);
        }
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [showCalendar]);

    const monthNames = language === 'zh'
        ? ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
        : ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

    const weekDays = language === 'zh'
        ? ['日', '一', '二', '三', '四', '五', '六']
        : ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

    const getDaysInMonth = (year, month) => new Date(year, month + 1, 0).getDate();
    const getFirstDayOfMonth = (year, month) => new Date(year, month, 1).getDay();

    const formatDate = (date) => {
        if (!date) return '';
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const displayDate = (date) => {
        if (!date) return '';
        const d = new Date(date);
        if (language === 'zh') {
            return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
        } else {
            return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
        }
    };

    const handleDateClick = (day) => {
        const newDate = new Date(currentYear, currentMonth, day);
        onChange(formatDate(newDate));
        setShowCalendar(false);
    };

    const handlePrevMonth = () => {
        if (currentMonth === 0) {
            setCurrentMonth(11);
            setCurrentYear(currentYear - 1);
        } else {
            setCurrentMonth(currentMonth - 1);
        }
    };

    const handleNextMonth = () => {
        if (currentMonth === 11) {
            setCurrentMonth(0);
            setCurrentYear(currentYear + 1);
        } else {
            setCurrentMonth(currentMonth + 1);
        }
    };

    const renderCalendar = () => {
        const daysInMonth = getDaysInMonth(currentYear, currentMonth);
        const firstDay = getFirstDayOfMonth(currentYear, currentMonth);
        const days = [];

        // 空白填充
        for (let i = 0; i < firstDay; i++) {
            days.push(React.createElement('div', { key: `empty-${i}`, className: 'calendar-day empty' }));
        }

        // 日期
        const selectedDate = value ? new Date(value) : null;
        for (let day = 1; day <= daysInMonth; day++) {
            const isSelected = selectedDate &&
                selectedDate.getFullYear() === currentYear &&
                selectedDate.getMonth() === currentMonth &&
                selectedDate.getDate() === day;

            days.push(
                React.createElement(
                    'div',
                    {
                        key: day,
                        className: `calendar-day${isSelected ? ' selected' : ''}`,
                        onClick: () => handleDateClick(day)
                    },
                    day
                )
            );
        }

        return days;
    };

    return React.createElement(
        'div',
        { className: 'date-picker', ref: calendarRef },
        React.createElement('input', {
            type: 'text',
            className: 'input-field',
            placeholder: placeholder,
            value: displayDate(value),
            readOnly: true,
            onClick: () => setShowCalendar(!showCalendar)
        }),
        showCalendar && React.createElement(
            'div',
            { className: 'calendar-dropdown' },
            React.createElement(
                'div',
                { className: 'calendar-header' },
                React.createElement('button', { onClick: handlePrevMonth }, '◀'),
                React.createElement('span', null, `${monthNames[currentMonth]} ${currentYear}`),
                React.createElement('button', { onClick: handleNextMonth }, '▶')
            ),
            React.createElement(
                'div',
                { className: 'calendar-weekdays' },
                ...weekDays.map(day => React.createElement('div', { key: day }, day))
            ),
            React.createElement('div', { className: 'calendar-days' }, ...renderCalendar()),
            React.createElement(
                'div',
                { className: 'calendar-footer' },
                React.createElement(
                    'button',
                    {
                        className: 'btn-text',
                        onClick: () => {
                            onChange('');
                            setShowCalendar(false);
                        }
                    },
                    language === 'zh' ? '清除' : 'Clear'
                )
            )
        )
    );
}

// 导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DatePicker;
} else {
    window.DatePicker = DatePicker;
}

