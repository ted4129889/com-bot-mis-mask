/* (C) 2024 */
package com.bot.mask.util.xml.mask;

import com.bot.mask.log.LogProcess;
import com.bot.mask.util.text.FormatData;
import com.bot.mask.util.xml.mask.xmltag.Field;
import com.bot.txcontrol.util.text.astart.AstarUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Scope("prototype")
public class DataMasker {

    // maskType
    private static final String ID_NUMBER = "1"; // 身分證字號或統一(居留)證號
    private static final String BANK_ACCOUNT_NUMBER = "2"; // 銀行帳戶之號碼 (不遮)
    private static final String CREDIT_CARD_NUMBER = "3"; // 信用卡或簽帳卡之號碼
    private static final String NAME = "4"; // 姓名(遮蔽補到底)
    private static final String ADDRESS = "5"; // 地址
    private static final String EMAIL_ADDRESS = "6"; // 電子郵遞地址
    private static final String PHONE_NUMBER = "7"; // 電話號碼
    private static final String BIRTHDAY = "8"; // 生日
    private static final String JOB_TITLE = "9"; // 職稱
    private static final String FINGERPRINT_PHOTO = "10"; // 指紋、相片
    private static final String PLACE_OF_BIRTH = "11"; // 出生地
    private static final String EDUCATION = "12"; // 學歷
    private static final String WORK_EXPERIENCE = "13"; // 經歷
    private static final String OCCUPATION = "14"; // 職業
    private static final String NICKNAME = "15"; // 暱稱
    private static final String MARITAL_STATUS = "16"; // 婚姻狀態
    private static final String SERVICE_UNIT = "17"; // 服務單位
    private static final String PASSPORT_NUMBER = "18"; // 護照號碼

    private static final String OBU = "OBU";
    private static final String ID = "ID";
    private static final String UNIFIED_NUMBER = "UNIFIED_NUMBER";

    private static final String STR_STAR = "*"; // 星號
    private static final String STR_NINE = "9"; // 9

    @Autowired
    private FormatData formatData;

    @Autowired
    private IdMapping idMapping;

    @Autowired
    AstarUtils astarUtils;

    //    @Autowired private IdConverterByYaml idConverterByYaml;

    /**
     * Masks sensitive data in the provided list of SQL data rows based on the specified field list.
     *
     * @param sqlData   the list of SQL data rows to be masked
     * @param fieldList the list of fields that define the masking rules
     */
    public void maskData(List<Map<String, Object>> sqlData, List<Field> fieldList, boolean isNotMask) throws IOException {
        Map<String, String> maskingFields = new HashMap<>();
        for (Field field : fieldList) {
            maskingFields.put(field.getFieldName(), field.getMaskType());
        }
//        long start = System.nanoTime();
        for (Map<String, Object> row : sqlData) {
            for (Map.Entry<String, String> entry : maskingFields.entrySet()) {
                String fieldName = entry.getKey();
                String maskType = entry.getValue();

                Map<String, Object> columnInfo = (Map<String, Object>) row.get(fieldName);
                if (columnInfo != null) {
                    Object value = columnInfo.get("value");
                    if (isNotMask) {
                        columnInfo.put("value", applyMask(value != null ? value.toString() : null, maskType, 0));
                    } else {
                        columnInfo.put("value", value != null ? value.toString() : null);
                    }
                }
            }
        }
//        long duration = 0L;

//        duration  = duration + (System.nanoTime() - start);

//        LogProcess.info("耗時: " + duration + "ns");
    }

    /**
     * Applies the specified mask type to the given value.
     *
     * @param value    the value to be masked
     * @param maskType the type of masking to apply
     * @param length   value length
     * @return the masked value
     */
    public String applyMask(String value, String maskType, int length) throws IOException {
        // 如果value為空則直接return
        if (value == null || value.isEmpty()) return value;
//        long start = System.nanoTime();
        String result = switch (maskType) {
            case ID_NUMBER -> maskId(value);
            case CREDIT_CARD_NUMBER -> maskCreditCardNumber(value, length);
            case NAME,
                    PLACE_OF_BIRTH,
                    EDUCATION,
                    WORK_EXPERIENCE,
                    OCCUPATION,
                    NICKNAME,
                    SERVICE_UNIT -> maskAllButFirst(value, length);
            case ADDRESS -> maskAddress(value, length);
            case EMAIL_ADDRESS -> maskEmail(value, length);
            case PHONE_NUMBER -> maskPhoneNumber(value, length);
            case BIRTHDAY -> maskBirthDay(value, length);
            case JOB_TITLE -> maskJobTitle(value, length);
            case MARITAL_STATUS -> maskMaritalStatus(value, length);
            case PASSPORT_NUMBER -> maskPassportNumber(value, length);
            default -> value;
        };
//        long duration = System.nanoTime() - start;
//        LogProcess.info("遮蔽類型: " + maskType + ", 耗時: " + duration + "ns");
        return result;

    }

    private static String determineIdType(String value) {
        String result = "";
        switch (value.trim().length()) {
            case 10:
                result = value.startsWith(OBU) ? OBU : ID;
                break;
            case 8:
                result = UNIFIED_NUMBER;
                break;
            default:
                result = value;
                break;
        }
        return result;
    }

    /**
     * Masks the given ID based on its type.
     *
     * @param value the ID to be masked
     * @return the masked ID
     */
    private String maskId(String value) throws IOException {
        return switch (determineIdType(value)) {
            // OBU前三碼不置換，後7碼置換
            case OBU -> OBU + generateRandomString(value.substring(3), OBU);
            // 本國ID後8碼置換
            case ID -> value.substring(0, 2) + generateRandomString(value.substring(2), ID);
            // 統編全部8碼置換
            case UNIFIED_NUMBER -> generateRandomString(value, UNIFIED_NUMBER);
            //其餘就是照ID方式全轉，但英文就不動
            default -> generateRandomString(value, UNIFIED_NUMBER);
        };
    }

    // 可以在 class 層做 cache
    private final Map<String, HashMap<Integer, String>> cacheMapping = new HashMap<>();

    private HashMap<Integer, String> getCachedMapping(String idType) throws IOException {
        return cacheMapping.computeIfAbsent(idType, key -> idMapping.getMapping(key));
    }

    private String generateRandomString(String value, String idType) throws IOException {
        StringBuilder maskedString = new StringBuilder(value.length());
        HashMap<Integer, String> mapping = getCachedMapping(idType);

        boolean isConvert = !UNIFIED_NUMBER.equals(idType) && value.chars().anyMatch(c -> !Character.isDigit(c));

//        for (char c : value.toCharArray()) {
//            if (isConvert) {
//                maskedString.append(c);
//            } else {
//                int digit = Character.getNumericValue(c);
//                maskedString.append(mapping.get(digit));
//            }
//        }
        for (char c : value.toCharArray()) {
            // 處理空白或非數字
            if (Character.isWhitespace(c)) {
                maskedString.append(c); // 保留空白
                continue;
            }

            if (isConvert) {
                // 原樣保留
                maskedString.append(c);
            } else {
                int digit = Character.getNumericValue(c);
                String mapped = mapping.get(digit);

                if (mapped != null) {
                    maskedString.append(mapped);
                } else {
                    // 保留原字元
                    maskedString.append(c);
                }
            }
        }
        return maskedString.toString();
    }
//    private String generateRandomString(String value, String idType) throws IOException {
//
//
//        StringBuilder maskedString = new StringBuilder(value.length());
//        HashMap<Integer, String> mapping = idMapping.getMapping(idType);
//
//        boolean isConvert = false;
//        //非統編 判斷有無轉過遮蔽
//        if (!UNIFIED_NUMBER.equals(idType)) {
//            for (char c : value.toCharArray()) {
//                if (!Character.isDigit(c)) {
//                    isConvert = true;
//                }
//            }
//        }
//
//        for (char c : value.toCharArray()) {
//            if (isConvert) {
//                maskedString.append(c);
//            } else {
//                int digit = Character.getNumericValue(c);
//                maskedString.append(mapping.get(digit));
//            }
//        }
//        return maskedString.toString();
//    }

    /**
     * Masks the given credit card number.
     *
     * @param value the credit card number to be masked
     * @return the masked credit card number
     */
    private String maskCreditCardNumber(String value, int length) {
        // 前6 後4，中間以*代替
        return value.length() > 10
                ? value.substring(0, 6)
                + "*".repeat(value.length() - 10)
                + value.substring(value.length() - 4)
                : value;
    }

    /**
     * Masks all characters except the first in the given value.
     *
     * @param value the value to be masked
     * @return the masked value
     */
//    private String maskAllButFirst(String value) {
//        // 第一個字不遮蔽，其餘以*代替
//        //如果字串中 全部皆為數字，則使用9，否則使用* 代替
//        String replaceObj = isAllDigitsOrDecimal(value) ? STR_NINE : STR_STAR;
//
//        return value.length() > 1 ? value.charAt(0) + formatData.getMaskedValue(value.substring(1), replaceObj) : value;
//    }
    private String maskAllButFirst(String value, int length) {
        if (value == null) return null;

        String replaceObj = isAllDigitsOrDecimal(value) ? STR_NINE : STR_STAR;
//        LogProcess.info(log,"b value = {} ",value);
//        LogProcess.info(log,"byte value = {} ",bytesToHex(astarUtils.utf8ToBIG5(value)));

        value = formatData.getReplaceSpace(value," ");

        //實際字串長度
        int actualLen = formatData.getDisplayWidth(value);
        //差異長度: 實際字串長度 與 定義好的長度 比較
        int diffLen = actualLen <= length?length - actualLen : 0;

//        LogProcess.info(log,"a value = {} actualLen = {} diffLen = {} ",value,actualLen,diffLen);
        //若有差異，剩餘補空白
        value = value + " ".repeat(diffLen);
        //前一位不處理
        String prefix = value.substring(0, 1);
        //第一位後處理遮蔽
        String suffix = value.substring(1);
        //第一位後的字串總長度
        int suffixLen = formatData.getDisplayWidth(suffix);

        //需要遮蔽的部分
        String maskedPart = suffix.trim();
        String maskedTail = replaceObj.repeat(formatData.getDisplayWidth(maskedPart));

        // 補回原本長度 (右側空白)
        return prefix + padRight(maskedTail, suffixLen);
    }

    /**
     * Masks the given address.
     *
     * @param value the address to be masked
     * @return the masked address
     */
//    private String maskAddress(String value) {
//
//        // 前六個字不遮蔽，其餘以*代替
//        return value.length() > 6 ? value.substring(0, 6) + formatData.getMaskedValue(value.substring(6), "*") : value;
//    }
    private String maskAddress(String value, int length) {
        if (value == null) return null;
        value = formatData.getReplaceSpace(value," ");
        //實際字串長度
        int actualLen = formatData.getDisplayWidth(value);
        //差異長度: 實際字串長度 與 定義好的長度 比較
        int diffLen = actualLen <= length?length - actualLen : 0;
        //若有差異，剩餘補空白
        value = value + " ".repeat(diffLen);
        //前六位不處理
        String prefix = value.substring(0, 6);
        //第六位後處理遮蔽
        String suffix = value.substring(6);
        //第六位後的字串總長度
        int suffixLen = formatData.getDisplayWidth(suffix);

        //需要遮蔽的部分
        String maskedPart = suffix.trim();
        String maskedTail = "*".repeat(formatData.getDisplayWidth(maskedPart));

        // 補回原本長度 (右側空白)

        return prefix + padRight(maskedTail, suffixLen);
    }


    private String getFirstNCharsConsideringFullWidth(String text, int n) {
        int count = 0;
        int index = 0;

        for (char c : text.toCharArray()) {
            // 每遇到一個字元就算 1「字」
            count++;
            index++;

            if (count == n) {
                break;
            }
        }
        return text.substring(0, index);
    }

    /**
     * Masks the given email address.
     *
     * @param value the email address to be masked
     * @return the masked email address
     */
    private String maskEmail(String value, int length) {
        // 前二個字不遮蔽、其餘以@COM 代替
        return value.length() > 2 ? value.substring(0, 2) + "@COM" : value;
    }

    /**
     * Masks the given phone number.
     *
     * @param value the phone number to be masked
     * @return the masked phone number
     */
    private String maskPhoneNumber(String value, int length) {
        if (value == null) return null;
        // 先記原始長度
        int originalLen = value.length();
        // TRIM 後的內容
        String t = value.trim();

        if (t.length() <= 4) {
            // TRIM 後長度 <= 4 → 不需要遮罩
            // 但仍需補回原本長度
            return padRight(t, originalLen);
        }

        // 前 4 位不遮蔽
        String prefix = t.substring(0, 4);

        // 後面要補 9（依照 TRIM 後的長度計算）
        String maskedTrim = prefix + "9".repeat(t.length() - 4);

        // 最後補空白 → 總長度保持和原 value 一樣
        return padRight(maskedTrim, originalLen);
    }

    // 右側補空白
    private String padRight(String s, int totalLen) {
        if (s.length() >= totalLen) return s;
        return s + " ".repeat(totalLen - s.length());
    }

    /**
     * Masks the given birthday.
     *
     * @param value the birthday to be masked
     * @return the masked birthday
     */
    private String maskBirthDay(String value, int length) {
        // 前六位固定設為199001，後二位不遮
        return "199001" + (value.length() == 8 ? value.substring(6) : "01");
    }

    /**
     * Masks the given job title.
     *
     * @param value the job title to be masked
     * @return the masked job title
     */
    private String maskJobTitle(String value, int length) {
        // 以統一職稱代替IdConverterByYaml
        return "統一職稱";
    }

    /**
     * Masks the given marital status.
     *
     * @param value the marital status to be masked
     * @return the masked marital status
     */
    private String maskMaritalStatus(String value, int length) {
        // 以一個*代替
        return "*";
    }

    /**
     * Masks the given passport number.
     *
     * @param value the passport number to be masked
     * @return the masked passport number
     */
    private String maskPassportNumber(String value, int length) {
        // 前五個字不遮蔽，其餘以9 代替
        return value.length() > 5 ? value.substring(0, 5) + "9".repeat(value.length() - 5) : value;
    }

    private boolean isAllDigitsOrDecimal(String str) {
        boolean hasDot = false;
        for (char c : str.toCharArray()) {
            if (c == '.') {
                if (hasDot) return false;  // 只允許一個小數點
                hasDot = true;
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}

