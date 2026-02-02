/* (C) 2024 */
package com.bot.mask.util.cobol;

import com.bot.mask.log.LogProcess;
import com.bot.mask.util.text.FormatData;
import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import com.bot.txcontrol.util.text.astart.AstarUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Scope("prototype")
public class CobolRecordDecoder {

    @Autowired
    AstarUtils astarUtils;
    @Autowired
    FormatData formatData;
    private final String SPACE = " ";
    private final String CHARSET_MS950 = "MS950";

    /**
     * 使用hex， 處理資料(Binary)
     */
    public Map<String, String> decodeBinary(byte[] data, List<CobolField> layout) {
        Map<String, String> result = new LinkedHashMap<>();
        String hexStr = bytesToHex(data).replace(SPACE, "");

        LogProcess.debug(log, "[DEBUG decodeBinary FasXXXX FIle ] decodeBinary bytesToHex : {}", hexStr);
        int hexCursor = 0;
        double byteCursor = 0.0;
        int tmpType = 0;
        String resultVal = "";

        for (CobolField field : layout) {


            double byteLen = field.digits;

            int hexLen = (int) (byteLen * 2);
            // 處理 COMP
            if (field.type == CobolField.Type.COMP) {

                String hexSlice = hexStr.substring(hexCursor, hexCursor + hexLen);
                LogProcess.debug(log, "[DEBUG decodeBinary FasXXXX FIle ] Field: {}, Type: {}, Length: {} , hexValue: {}", field.name, field.type, field.digits, hexSlice);

                String value;

                if (looksLikeComp3(hexSlice, field.decimal)) {
                    //COMP 3
                    value = decodeComp3(hexSlice, hexLen, field.decimal);

                    tmpType = 1;
                    hexCursor += hexLen;
                    byteCursor = (int) Math.ceil(hexCursor / 2.0);
                } else {
                    //COMP
                    // 沒有特殊符號，正常搬
                    value = hexSlice;
                    tmpType = 2;
                    hexCursor += hexLen;
                    byteCursor += byteLen;
                }
                resultVal = value;
                result.put(field.name, value);

                continue;
            }

            // 處理 DISPLAY / X
            if (field.type == CobolField.Type.DISPLAY || field.type == CobolField.Type.X) {

                byte[] fieldBytes =
                        Arrays.copyOfRange(
                                data, (int) byteCursor, (int) (byteCursor + field.digits));
                LogProcess.debug(log, "[DEBUG decodeBinary FasXXX FIle] Field: {}, Type: {}, Length: {} , fieldBytes: {}", field.name, field.type, field.digits, fieldBytes);

                Object value;
                String resultX = "";

                // 解析 signed zoned decimal
                if (isSignedZonedDecimal(fieldBytes, field.decimal)) {
                    tmpType = 3;

                    resultX = decodeSignedZonedDecimal(fieldBytes, field.decimal);

                } else {
                    tmpType = 4;

                    Charset charset;
                    // EBCDIC 判斷
                    if (looksLikeEBCDIC(fieldBytes)) {
                        tmpType = 42;

                        //V : Cp037=>IBM Mainframe 標準 EBCDIC
                        //X : Cp1047=>IBM AIX / UNIX 使用的 EBCDIC 非標準
                        charset = Charset.forName("CP037");
                        value = new String(fieldBytes, charset).trim();
                    }
                    // ASCII 判斷
                    else if (allAsciiPrintable(fieldBytes)) {
                        tmpType = 41;
                        charset = StandardCharsets.US_ASCII;
                        value = new String(fieldBytes, charset).trim();
                    } else {
                        tmpType = 43;

                        charset = Charset.forName(CHARSET_MS950);
                        value = new String(astarUtils.utf8ToBIG5(astarUtils.burToUTF8(fieldBytes)), charset);
                    }

                    LogProcess.debug(log, "[DEBUG decodeBinary FasXXX FIle] field.digits: {} vs. Length: {} ", field.digits, formatData.getDisplayWidth(value.toString()));

                    int diff = (int) field.digits - formatData.getDisplayWidth(value.toString());
                    //通常結果為字串(空白往後補)
                    resultX = value + SPACE.repeat(diff);

                }

                resultVal = resultX;
                result.put(field.name, resultX);
                byteCursor += byteLen;
                hexCursor = (int) (byteCursor * 2);
            }

            LogProcess.debug(log, "[DEBUG decodeBinary FasXXXX FIle ] type = {},Value = {}", tmpType, resultVal);

        }


        return result;
    }

    /**
     * 使用hex， 處理單筆資料(Ascii 除MS950)
     */
    public Map<String, String> decodeAscii(byte[] data, List<CobolField> layout) {

        Map<String, String> result = new LinkedHashMap<>();
        int cursor = 0;
        LogProcess.debug(log, "[DEBUG decodeAscii FasXXXX FIle ] decodeAscii bytesToHex : {}", bytesToHex(data).replace(SPACE, ""));

        for (CobolField field : layout) {
            int len = (int) field.getDigits();
            byte[] fieldBytes = Arrays.copyOfRange(data, cursor, cursor + len);

            String hexStr = bytesToHex(fieldBytes).replace(SPACE, "");

            LogProcess.debug(log, "[DEBUG decodeAscii FasXXXX FIle ] Field: {}, Type: {}, Length: {} , hexValue: {}", field.name, field.type, field.digits, hexStr);


            String val = astarUtils.burToUTF8(fieldBytes);

//            Charset charset = Charset.forName(CHARSET_MS950);
//            String val = new String(astarUtils.utf8ToBIG5(astarUtils.burToUTF8(fieldBytes)), Charset.forName(CHARSET_MS950));

            // 判斷數值正負號
            String convertVal1 = decodeText(val, field.decimal, field.type);
            // 無法編碼的處理
            String convertVal2 = filterUnencodable(convertVal1);
            // 中文字補空白
            String convertVal3 = checkHexStartEndAndPad(hexStr, convertVal2);
            // 最後長度計算
            String convertVal4 = padToHalfWidthLength(convertVal3, (int) field.digits, field.type);
            LogProcess.debug(log, "[DEBUG decodeAscii FasXXXX FIle ] Convert Value Processing : {} => {} => {} => {}", convertVal1, convertVal2, convertVal3, convertVal4);

            result.put(field.getName(), convertVal4);

            cursor += len;
        }

        LogProcess.debug(log, "[DEBUG decodeAscii FasXXXX FIle ] Hex Conv Result:{}", result);

        return result;
    }

    /**
     * 使用hex， 處理單筆資料(MS950)
     */
    public Map<String, String> decodeAsciiMs950(byte[] data, List<CobolField> layout) {

        Map<String, String> result = new LinkedHashMap<>();

        LogProcess.debug(log, "[DEBUG FasXXXX FIle ] decodeAsciiMs950 bytesToHex : {}", bytesToHex(data).replace(SPACE, ""));

        int cursor = 0;

        for (CobolField field : layout) {
            int len = (int) field.getDigits();
            byte[] fieldBytes = Arrays.copyOfRange(data, cursor, cursor + len);

            String hexStr = bytesToHex(fieldBytes).replace(SPACE, "");
            LogProcess.debug(log, "[DEBUG FasXXXX FIle ] Field: {}, Type: {}, Length: {} , hexValue: {}", field.name, field.type, field.digits, hexStr);


            String val;
            try {
                val = new String(fieldBytes, CHARSET_MS950);
            } catch (Exception e) {
                val = ""; // fallback
                ApLogHelper.error(
                        log,
                        false,
                        LogType.NORMAL.getCode(),
                        "MS950 decode error: " + e.getMessage());
            }

            String convertVal1 = decodeText(val, field.decimal, field.type).replace("　", "  ");

            String convertVal2 = filterUnencodable(convertVal1);

            String convertVal4 = padToHalfWidthLength(convertVal2, (int) field.digits, field.type);

            LogProcess.debug(log, "[DEBUG FasXXXX FIle ] Convert Value Processing : {} => {} => {} ", convertVal1, convertVal2, convertVal4);


            result.put(field.getName(), convertVal4);
            cursor += len;
        }

        LogProcess.debug(log, "[DEBUG FasXXXX FIle ] Hex Conv Result:{}", result);

        return result;
    }

    /**
     * COMP正負符號
     */
    private boolean looksLikeComp3(String hex, int decimal) {
        String sign = hex.substring(0, 1);

        if (sign.equals("D") || sign.equals("C") || decimal > 0) {
            return true;
        }
        return false;
    }

    /**
     * 解開成實際(展開)字串
     */
    private String decodeComp3(String hex, int totalHexLen, int decimal) {
        hex = hex.toUpperCase();
        String signNibble = hex.substring(0, 1);
        String digits = hex.substring(1);
        int digitCount = totalHexLen - 1;
        String sign = "";
        if (signNibble.equals("D")) {
            sign = "-";
        } else if (signNibble.equals("C")) {
            sign = "+";
        } else {
            // 假設都沒有的話表示沒有正負號那就是原值
            digits = hex;
            digitCount = totalHexLen;
        }
        if (sign.length() == 1 && digits.length() == 0) {
            return " ";
        }

        ApLogHelper.debug(log, false, LogType.NORMAL.getCode(), "[DEBUG] sign:{},digits:{}", sign, digits);
        BigDecimal result = new BigDecimal(sign + digits);
        result = result.movePointLeft(decimal);

        // 補0
        String[] parts = result.toPlainString().split("\\.");
        String integerPart = parts[0].replace("-", "").replace("+", "");
        while (integerPart.length() < digitCount - decimal) {
            integerPart = "0" + integerPart;
        }

        String formatted = sign + integerPart;
        if (decimal > 0) {
            String decimalPart = parts.length > 1 ? parts[1] : "";
            while (decimalPart.length() < decimal) {
                decimalPart += "0";
            }
            formatted += "." + decimalPart;
        }

        return formatted;
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * 解碼 COBOL 中的 有符號 Zoned Decimal 格式的數值欄位，可處理含小數位或純整數。
     *
     * @param bytes         Zoned Decimal 編碼的 byte 陣列
     * @param decimalPlaces 小數位數（若為 0 則回傳純整數）
     * @return 解碼後的字串，如 "-123.45" 或 "+789"，格式錯誤則回傳 null
     */
    private String decodeSignedZonedDecimal(byte[] bytes, int decimalPlaces) {
        if (bytes == null || bytes.length == 0) return "";

        StringBuilder rawDigits = new StringBuilder();
        String sign = "";
        int len = bytes.length;

        for (int i = 0; i < len; i++) {
            int b = bytes[i] & 0xFF;
            int high = (b & 0xF0);
            int low = (b & 0x0F);

            if (i == len - 1) {
                // 最後一位：包含符號
                if (high == 0xD0) sign = "-";
                else if (high == 0xC0) sign = "+";
                else if (high == 0xF0) sign = ""; // 預設正號
                else return null; // 非法格式

                rawDigits.append(low);
            } else {
                if (high != 0xF0) return null; // 非 Zoned 格式
                rawDigits.append(low);
            }
        }

        // 補零
        while (rawDigits.length() <= decimalPlaces) {
            rawDigits.insert(0, "0");
        }

        if (decimalPlaces > 0) {
            int splitPos = rawDigits.length() - decimalPlaces;
            String intPart = rawDigits.substring(0, splitPos);
            String decPart = rawDigits.substring(splitPos);
            return sign + intPart + "." + decPart;
        } else {
            return sign + rawDigits.toString();
        }
    }

    /**
     * 自動判斷
     *
     * <p>判斷一段位元組是否為 Signed Zoned Decimal（有符號區段十進位） 的格式
     */
    private boolean isSignedZonedDecimal(byte[] bytes, int decimal) {
        if (bytes == null || bytes.length == 0 || decimal == 0) return false;

        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            int high = b & 0xF0;

            ApLogHelper.debug(log, false, LogType.NORMAL.getCode(), "[DEBUG] b:{},high:{}", b, high);

            if (i < bytes.length - 1) {
                // 中間位元應為 F0 ~ F9
                if (high != 0xF0) return false;
            } else {
                // 最後一位可能是 C0, D0, F0
                if (high != 0xC0 && high != 0xD0 && high != 0xF0) return false;
            }
        }
        return true;
    }

    /**
     * 全部為可列印的 ASCII，就用 ASCII
     */
    private boolean allAsciiPrintable(byte[] bytes) {
        for (byte b : bytes) {
            int val = b & 0xFF;
            if (val < 0x20 || val > 0x7E) return false;
        }
        return true;
    }

    /**
     * 判斷來源檔案屬是否為ASCII(中文)
     */
    private boolean looksLikeEBCDIC(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return false;

        for (byte b : bytes) {
            int value = b & 0xFF;

            // EBCDIC 常見區間：
            // - 0x40 是空格
            // - 0xC1~0xC9, 0xD1~0xD9, 0xE2~0xE9 是 A~Z
            // - 0x81~0x89, 0x91~0x99, 0xA2~0xA9 是 a~z
            // - 0xF0~0xF9 是 0~9
            // - 0x5B~0x5D, 0x4B 是常見標點
            if (value == 0x40
                    || // space
                    (value >= 0xC1 && value <= 0xE9)
                    || // A~Z
                    (value >= 0x81 && value <= 0xA9)
                    || // a~z
                    (value >= 0xF0 && value <= 0xF9)
                    || // 0~9
                    (value >= 0x4B && value <= 0x5D) // 標點
            ) {
                continue;
            } else {
                return false; // 只要有一個不符常見 EBCDIC 區間就不是
            }
        }

        return true;
    }

    /**
     * 計算有效長度
     *
     * @param fieldList Cobol欄位格式清單
     * @return int 回傳有效的編碼長度
     */
    public int calculateRecordLength(List<CobolField> fieldList) {
        double length = 0;
        for (CobolField field : fieldList) {
            length += field.getDigits();
        }
        return (int) length;
    }

    /**
     * 將包含尾碼符號（如正負號）的 COMP-3 類型文字欄位轉成正常字串，並加入小數點處理。
     *
     * @param input   如 "123{", "999A" 等
     * @param decimal 小數位數（例如 2 表示變成 12.34；0 表示不處理）
     * @return 正常表示的字串，例如 "-123.45"
     */
    private String decodeText(String input, int decimal, CobolField.Type type) {
        if (input == null || input.isEmpty() || input.length() == 1) {
            return input;
        }
        // 排除是文字型態
        if (CobolField.Type.X.equals(type)) return input;

        char lastChar = input.charAt(input.length() - 1);
        String body = input.substring(0, input.length() - 1);
        String sign = "";
        String lastDigit = "";

        // 尾碼對應正負數字
        switch (lastChar) {
            case '{':
                sign = "+";
                lastDigit = "0";
                break;
            case 'A':
                sign = "+";
                lastDigit = "1";
                break;
            case 'B':
                sign = "+";
                lastDigit = "2";
                break;
            case 'C':
                sign = "+";
                lastDigit = "3";
                break;
            case 'D':
                sign = "+";
                lastDigit = "4";
                break;
            case 'E':
                sign = "+";
                lastDigit = "5";
                break;
            case 'F':
                sign = "+";
                lastDigit = "6";
                break;
            case 'G':
                sign = "+";
                lastDigit = "7";
                break;
            case 'H':
                sign = "+";
                lastDigit = "8";
                break;
            case 'I':
                sign = "+";
                lastDigit = "9";
                break;

            case '}':
                sign = "-";
                lastDigit = "0";
                break;
            case 'J':
                sign = "-";
                lastDigit = "1";
                break;
            case 'K':
                sign = "-";
                lastDigit = "2";
                break;
            case 'L':
                sign = "-";
                lastDigit = "3";
                break;
            case 'M':
                sign = "-";
                lastDigit = "4";
                break;
            case 'N':
                sign = "-";
                lastDigit = "5";
                break;
            case 'O':
                sign = "-";
                lastDigit = "6";
                break;
            case 'P':
                sign = "-";
                lastDigit = "7";
                break;
            case 'Q':
                sign = "-";
                lastDigit = "8";
                break;
            case 'R':
                sign = "-";
                lastDigit = "9";
                break;

            default:
                // 非尾碼符號，直接返回原始字串
                body = input;
                //                return input;
        }

        String numeric = body + lastDigit;

        if (decimal > 0 && numeric.length() > decimal) {
            int cut = numeric.length() - decimal;
            numeric = numeric.substring(0, cut).replace(".","") + "." + numeric.substring(cut);
        }

        return sign + numeric;
    }

    /**
     * 判斷與剔除無法正常編碼的字元
     */
    private String filterUnencodable(String input) {
        Charset big5 = Charset.forName("BIG5");
        CharsetEncoder encoder = big5.newEncoder();
        StringBuilder sb = new StringBuilder();
        //        ApLogHelper.debug(log, false, LogType.NORMAL.getCode(), "[DEBUG] input {}", input);

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            //            ApLogHelper.debug(log, false, LogType.NORMAL.getCode(), "[DEBUG] charAt
            // :{}", c);

            if (encoder.canEncode(c)) {
                sb.append(c);
            } else {
                sb.append("  ");
            }
        }

        //        ApLogHelper.debug(
        //                log, false, LogType.NORMAL.getCode(), "[DEBUG]sb.toString(): {}",
        // sb.toString());

        return sb.toString();
    }

    /**
     * 判斷 hex 字串是否以 2B 開頭、2C 結尾 若為 true，則在原始字串前後加上半形空白再回傳 否則回傳原字串
     *
     * @param hex      十六進位字串（不分大小寫）
     * @param original 原始文字字串
     * @return 若符合條件則加上半形空白後的字串，否則原樣回傳
     */
    private String checkHexStartEndAndPad(String hex, String original) {
        if (hex == null || hex.length() < 4 || original == null) {
            return original;
        }

        String upperHex = hex.toUpperCase();
        int index2B = upperHex.indexOf("2B");
        int index2C = upperHex.indexOf("2C");

        if (index2B >= 0 && index2C >= 0 && index2B < index2C) {
            // 符合條件，加上半形空白
            // 全形空白替換成兩個半形空白
            return SPACE + original.replace("　", "  ") + SPACE;
        }

        return original;
    }

    /**
     * 根據字串內容計算半形長度（全形:2，半形:1）， 若長度不足預期長度，則補滿半形空白直到相同長度。
     *
     * @param input        原始字串（可能含中日文或全形符號）
     * @param targetLength 預期長度（以半形寬度為單位）
     * @param type         Cobol的欄位型態
     * @return 補滿後的字串
     */
    private String padToHalfWidthLength(String input, int targetLength, CobolField.Type type) {
        if (input == null) return "";

        // 非字串需要
        if (!CobolField.Type.X.equals(type)) return input;

        StringBuilder result = new StringBuilder();
        int currentLength = 0;

        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            int charLen = isFullWidth(ch) ? 2 : 1;

            // 若加上這個字元會超過目標長度，就停止
            if (currentLength + charLen > targetLength) {
                break;
            }

            result.append(ch);
            currentLength += charLen;
        }

        // 不足則補半形空白
        while (currentLength < targetLength) {
            result.append(SPACE);
            currentLength++;
        }

        return result.toString();
    }

    /**
     * 判斷是否為全形字元（全形中文、全形英數、全形標點）
     */
    private boolean isFullWidth(char ch) {
        // 全形範圍：常見在 U+FF01 ~ U+FF60、U+FFE0 ~ U+FFE6
        // 以及中日韓常用字（U+4E00 ~ U+9FFF）
        return (ch >= 0xFF01 && ch <= 0xFF60)
                || (ch >= 0xFFE0 && ch <= 0xFFE6)
                || (ch >= 0x4E00 && ch <= 0x9FFF);
    }
}
