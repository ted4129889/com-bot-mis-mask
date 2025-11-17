/* (C) 2023 */
package com.bot.txcontrol.util.text.format;

import com.bot.txcontrol.eum.Constant;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FormatUtil {

    public String padX(String s, int n) {
        if (Objects.isNull(s)) s = "";

        // 將tab取代
        s = s.replace("\t", "");

        char[] ch = s.toCharArray();
        int i = n;
        int len = n;
        for (char c : ch) {
            if ((isChinese(c) || !isPrintableAsciiChar(c)) && c != '\n') {
                i--;
                len = len - 2;
            } else len--;

            if (len <= 0) break;
        }
        String re = s.length() >= i ? s.substring(0, i) : s;
        if (len == -1) i++;
        return String.format("%1$-" + i + "s", re);
    }

    public String padLeft(String s, int width) {
        return String.format("%" + width + "s", s);
    }

    public String pad9(String n, int width) {
        String format = String.format("%%0%dd", width);
        String result = String.format(format, 0) + n;
        return right(result, width);
    }

    public String rightPad9(String n, int width) {
        String format = String.format("%%0%dd", width);
        String result = n + String.format(format, 0);
        return left(result, width);
    }

    public String right(String s, int width) {
        if (s.length() <= width) return s;
        return s.substring(s.length() - width);
    }

    public String left(String s, int width) {
        if (s.length() <= width) return s;
        return s.substring(0, width);
    }

    public String pad9(String n, int width, int afterDecimalPoint) {
        if (Objects.isNull(n)) n = "0";
        else n = n.trim();
        String[] ss = n.split("\\.");
        String s1 = ss[0];
        String s2 = "0";
        if (ss.length > 1) s2 = ss[1];

        String result = pad9(s1, width);
        if (afterDecimalPoint > 0) {
            result = result + rightPad9(s2, afterDecimalPoint);
        }

        if (result.length() > width + afterDecimalPoint)
            result = result.substring(width + afterDecimalPoint - result.length() - 1);

        return result;
    }

    @SneakyThrows
    public <T> String vo2JsonString(T sourceVo) {
        if (Objects.isNull(sourceVo)) return Constant.EMPTY.getCode();

        return new ObjectMapper().writeValueAsString(sourceVo);
    }

    @SneakyThrows
    public <T> T jsonString2Vo(String text, Class<T> sourceVo) {
        if (Objects.isNull(text) || text.trim().isEmpty())
            return (T) sourceVo.getDeclaredConstructor().newInstance();

        return (T) new ObjectMapper().readValue(text, sourceVo);
    }

    private boolean isChinese(char c) {
        if ("「」「」".indexOf(c) != -1) return true;
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    private boolean isPrintableAsciiChar(char ch) {
        if ("「」「」".indexOf(ch) != -1) return true;
        return 32 <= ch && ch <= 126;
    }
}
