/* (C) 2023 */
package com.bot.txcontrol.util.parse;

import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import com.bot.txcontrol.util.text.format.FormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Scope("singleton")
public class Parse {
    private Pattern pattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");

    @Autowired
    private FormatUtil formatUtil;

    /**
     * Decimal Convert To String
     *
     * @param value     Decimal
     * @param precision precision
     * @param scale     scale
     * @param <T>       xx
     * @return String Type
     */
    public <T> String decimal2String(T value, int precision, int scale) {
        ApLogHelper.debug(
                log,
                false,
                LogType.TXCONTROL.getCode(),
                "decimal2String value : ["
                        + value
                        + "], precision : ["
                        + precision
                        + "], scale : ["
                        + scale
                        + "]");

        //        precision++;
        String text = "";
        try {
            if (value instanceof Integer) {
                text = Integer.toString(((Integer) value).intValue());
            } else if (value instanceof Double) {
                text = Double.toString(((Double) value).doubleValue());
            } else if (value instanceof Float) {
                text = Float.toString(((Float) value).floatValue());
            } else if (value instanceof Long) {
                text = Long.toString(((Long) value).longValue());
            } else if (value instanceof BigDecimal) {
                text = value.toString();
            } else return null;

            // 创建格式化字符串
            String format = String.format("%%0%d.%df", precision, scale);
            // 格式化值
            return String.format(format, new BigDecimal(text));
        } catch (Exception e) {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.UTIL.getCode(),
                    "decimal2String,value={},error={}",
                    value,
                    e.getMessage());
            return null;
        }
    }

    public <T> String decimal2StringPadZero(T value, int precision) {
        ApLogHelper.info(
                log,
                false,
                LogType.TXCONTROL.getCode(),
                "decimal2String value : [" + value + "], precision : [" + precision + "]");

        //        precision++;
        String text = "";
        try {
            if (value instanceof Integer) {
                text = Integer.toString(((Integer) value).intValue());
            } else if (value instanceof Double) {
                text = Double.toString(((Double) value).doubleValue());
            } else if (value instanceof Float) {
                text = Float.toString(((Float) value).floatValue());
            } else if (value instanceof Long) {
                text = Long.toString(((Long) value).longValue());
            } else if (value instanceof BigDecimal) {
                text = value.toString();
            } else return null;

            return formatUtil.pad9(text.trim().replaceAll("\\.", ""), precision);
        } catch (Exception e) {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.UTIL.getCode(),
                    "decimal2StringPadZero,value={},error={}",
                    value,
                    e.getMessage());
            return null;
        }
    }

    @Deprecated
    public BigDecimal stringToBigDecimal(String value) {
        BigDecimal res = null;
        try {
            res = new BigDecimal(value.replaceAll(",", "").trim());
        } catch (Exception e) {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.UTIL.getCode(),
                    "stringToBigDecimal,value={},error={}",
                    value,
                    e.getMessage());
        }
        return res;
    }

    public BigDecimal string2BigDecimal(String value) {
        BigDecimal res = null;
        try {
            res = new BigDecimal(value.replaceAll(",", "").trim());
        } catch (Exception e) {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.UTIL.getCode(),
                    "string2BigDecimal,value={},error={}",
                    value,
                    e.getMessage());
        }
        return res;
    }

    public Long string2Long(String value) {
        Long l;
        try {
            l = Long.parseLong(value.trim());
        } catch (Exception e) {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.UTIL.getCode(),
                    "string2Long,,value={},error={}",
                    value,
                    e.getMessage());
            return null;
        }
        return l;
    }

    public Integer string2Integer(String value) {
        Integer integer;
        try {
            integer = Integer.parseInt(value.trim());
        } catch (Exception e) {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.UTIL.getCode(),
                    "string2Integer,value={},error={}",
                    value,
                    e.getMessage());
            return null;
        }
        return integer;
    }

    public Short string2Short(String value) {
        Short shot = null;

        try {
            shot = Short.parseShort(value.trim());
        } catch (Exception e) {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.UTIL.getCode(),
                    "string2Short,value={},error={}",
                    value,
                    e.getMessage());
        }
        return shot;
    }

    /**
     * is numeric
     *
     * @param str String
     * @return Numeric true else false
     */
    public boolean isNumeric(String str) {
        if (Objects.isNull(str)) return false;

        /*
         * int sz = str.length(); for (int i = 0; i < sz; i++) { if
         * (Character.isDigit(str.charAt(i)) == false) { return false; } } return true;
         */
        Matcher m = pattern.matcher(str.trim());
        return m.matches();
    }

}
