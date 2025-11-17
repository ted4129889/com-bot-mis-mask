/* (C) 2024 */
package com.bot.txcontrol.util.text.convert;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HexConvert {
    // 1122轉{0x11,0x12}
    public static byte[] hexString2Bytes(String s) {
        byte[] bytes;
        bytes = new byte[s.length() / 2];

        for (int i = 0; i < bytes.length; i++) {
            // 十六轉十進制
            bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }

        return bytes;
    }

    // {0x11,0x12}轉1122
    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    public static byte[] convertHexString(byte[] data) {
        // AFTER CP937
//        ApLogHelper.info(
//                log,
//                false,
//                LogType.UTIL.getCode(),
//                "Before convertHexString: [{}]",
//                HexDump.dumpHexString(data));
        byte[] datachange = new byte[data.length];
        // CCSID 37 has [] at hex BA and BB instead of at hex AD and BD respectively.???
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0x15) {
                datachange[i] = (byte) 0x25; // 0x0E 會被刪除
            } else {
                datachange[i] = data[i];
            }
        }
//        ApLogHelper.info(
//                log,
//                false,
//                LogType.UTIL.getCode(),
//                "After convertHexString: [{}]",
//                HexDump.dumpHexString(datachange));
        return datachange;
    }
}
