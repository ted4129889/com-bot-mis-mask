/* (C) 2024 */
package com.bot.mask.util.cobol;

import com.bot.mask.log.LogProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class AsciiConvertedFileProcessor implements CobolFileProcessor {

    @Autowired
    CobolRecordDecoder decoder;

    @Override
    public List<Map<String, String>> parse(byte[] data, List<CobolField> layout) {

        List<Map<String, String>> result = new ArrayList<>();
        int dataLength = data.length;
        int recordLen = layout.stream().mapToInt(f -> (int) f.getDigits()).sum();
        int totalRecords = dataLength / recordLen;
        LogProcess.info(log, "dataLength = {}", dataLength);
        LogProcess.info(log, "recordLen = {}", recordLen);
        LogProcess.info(log, "totalRecords = {}", totalRecords);

        for (int i = 0; i < totalRecords; i++) {

            int s = i * recordLen;
            int e = (i + 1) * recordLen;
            byte[] singleData = Arrays.copyOfRange(data, s, e);
            //            1.這段用好，然後開始測這三個案例 OK
            //                    2.分批寫的問題 OK
            //                3.如果有表投表委的問題 OK
            //                4.寫在遮蔽檔案 可以把日期格式去除掉再來比對 以及 要把定義檔加上遮蔽欄位
            //                5.確認包版的問題
            //                6.禮拜一要先確定FTP問題，然後可以跑之後確認檔案內容，然後再給YOKO時間，給啟俊專案包版
            //                7.RESTFUL 測試是否可以直接抓API?

            Map<String, String> row = decoder.decodeAscii(singleData, layout);
            result.add(row);
        }
        return result;
    }

    @Override
    public void parseAndConsume(
            byte[] data, List<CobolField> layout, Consumer<Map<String, String>> consumer) {
        parseCobolWithOptionalHeaderFooter(
                data, null, layout, null, false, h -> {
                }, consumer, t -> {
                }, 1, 1);
    }

    @Override
    public void parseAndConsume2(
            byte[] data, List<CobolField> layout, Consumer<Map<String, String>> consumer) {
        parseCobolWithOptionalHeaderFooter(
                data, null, layout, null, true, h -> {
                }, consumer, t -> {
                }, 1, 1);
    }

    @Override
    public void parseCobolWithOptionalHeaderFooter(
            byte[] data,
            List<CobolField> layoutHeader,
            List<CobolField> layoutBody,
            List<CobolField> layoutFooter,
            boolean useMs950Handle,
            Consumer<Map<String, String>> headerConsumer,
            Consumer<Map<String, String>> bodyConsumer,
            Consumer<Map<String, String>> footerConsumer,
            int headerCnt,
            int footerCnt) {

        //        Function<byte[], Map<String, String>> decodeFunction =
        //                charset.name().equalsIgnoreCase("MS950")
        //                        ? bytes -> decoder.decodeAsciiMs950(bytes, layoutBody)
        //                        : bytes -> decoder.decodeAscii(bytes, layoutBody);
//        LogProcess.info(log, "layoutHeader = {}", layoutHeader);
//        LogProcess.info(log, "layoutBody = {}", layoutBody);
//        LogProcess.info(log, "layoutFooter = {}", layoutFooter);
////        LogProcess.info(log, "data = {}", data);
        LogProcess.info(log, "useMs950Handle = {}", useMs950Handle);
        LogProcess.info(log, "總長度 = {}", data.length);
        LogProcess.info(log, "HeaderLen = {},BodyLen = {},FooterLen = {}", layoutHeader.size(),layoutBody.size(),layoutFooter.size());





        if (!useMs950Handle) {
            // ========== HEX分割 0D0A ==========
            String hex = decoder.bytesToHex(data).toUpperCase();
            int total = 0;

            String[] hexRecords = hex.split("0D0A");
            if(hexRecords.length == 1){
                hexRecords = hex.split("0A");
            }
            total = hexRecords.length;

            LogProcess.info(log, "split newline character total = {}", total);

            int start = 0;
            int end = total;

            // 處理表頭
            if (layoutHeader != null
                    && !layoutHeader.isEmpty()
                    && headerCnt > 0
                    && total >= headerCnt) {
                for (int i = 0; i < headerCnt; i++) {
                    byte[] headBytes = decoder.hexToBytes(hexRecords[i]);

                    headerConsumer.accept(decoder.decodeAsciiMs950(headBytes, layoutHeader));
                }
                start = headerCnt;
            } else {
                start = 0;
            }

            // 處理表尾
            if (layoutFooter != null
                    && !layoutFooter.isEmpty()
                    && footerCnt > 0
                    && total >= headerCnt + footerCnt) {
                end = total - footerCnt;
            } else {
                end = total;
            }

            // 處理表身（明細）
            for (int i = start; i < end; i++) {
                String hexLine = hexRecords[i].trim();

                if (hexLine.isEmpty()) continue;

                byte[] lineBytes = decoder.hexToBytes(hexLine);
                bodyConsumer.accept(decoder.decodeAsciiMs950(lineBytes, layoutBody));
            }

            // 再處理表尾資料（用 footerCnt 計算）
            if (layoutFooter != null
                    && !layoutFooter.isEmpty()
                    && footerCnt > 0
                    && total >= headerCnt + footerCnt) {
                for (int i = total - footerCnt; i < total; i++) {
                    byte[] tailBytes = decoder.hexToBytes(hexRecords[i]);
                    footerConsumer.accept(decoder.decodeAsciiMs950(tailBytes, layoutFooter));
                }
            }
            //            // 處理表頭
            //            if (layoutHeader != null && !layoutHeader.isEmpty() && total >= 1) {
            //                byte[] headBytes = decoder.hexToBytes(hexRecords[0]);
            //                headerConsumer.accept(decoder.decodeAsciiMs950(headBytes,
            // layoutHeader));
            //                start = 1;
            //            }
            //
            //            // 處理表尾
            //            if (layoutFooter != null && !layoutFooter.isEmpty() && total >= 2) {
            //                end = total - 1;
            //            }
            //
            //            for (int i = start; i < end; i++) {
            //                String hexLine = hexRecords[i].trim();
            //                if (hexLine.isEmpty()) continue;
            //
            //                byte[] lineBytes = decoder.hexToBytes(hexLine);
            //                bodyConsumer.accept(decoder.decodeAsciiMs950(lineBytes, layoutBody));
            //                //
            // bodyConsumer.accept(decodeFunction.apply(lineBytes)); // << 用
            //                // decodeFunction
            //            }
            //
            //            if (layoutFooter != null && !layoutFooter.isEmpty() && total >= 2) {
            //                byte[] tailBytes = decoder.hexToBytes(hexRecords[total - 1]);
            //                footerConsumer.accept(decoder.decodeAsciiMs950(tailBytes,
            // layoutFooter));
            //            }

        } else {
            // ========= 定長切割 ==========
            int recordLen = layoutBody.stream().mapToInt(f -> (int) f.getDigits()).sum();
            int totalRecords = data.length / recordLen;

            LogProcess.info(log, "totalRecords total = {}", totalRecords);

            int startIdx = 0;
            int endIdx = totalRecords;

            // 處理表頭
            if (layoutHeader != null
                    && !layoutHeader.isEmpty()
                    && headerCnt > 0
                    && totalRecords >= headerCnt) {
                for (int i = 0; i < headerCnt; i++) {
                    int s = i * recordLen;
                    int e = s + recordLen;
                    byte[] headBytes = Arrays.copyOfRange(data, s, e);
                    headerConsumer.accept(decoder.decodeAscii(headBytes, layoutHeader));
                }
                startIdx = headerCnt;
            }

            // 處理表尾
            if (layoutFooter != null
                    && !layoutFooter.isEmpty()
                    && footerCnt > 0
                    && totalRecords >= headerCnt + footerCnt) {
                endIdx = totalRecords - footerCnt;
            } else {
                endIdx = totalRecords;
            }

            // 處理表身（明細）
            for (int i = startIdx; i < endIdx; i++) {
                int s = i * recordLen;
                int e = s + recordLen;
                byte[] detailBytes = Arrays.copyOfRange(data, s, e);
                bodyConsumer.accept(decoder.decodeAscii(detailBytes, layoutBody));
            }

            // 處理表尾
            if (layoutFooter != null
                    && !layoutFooter.isEmpty()
                    && footerCnt > 0
                    && totalRecords >= headerCnt + footerCnt) {
                for (int i = totalRecords - footerCnt; i < totalRecords; i++) {
                    int s = i * recordLen;
                    int e = s + recordLen;
                    byte[] tailBytes = Arrays.copyOfRange(data, s, e);
                    footerConsumer.accept(decoder.decodeAscii(tailBytes, layoutFooter));
                }
            }
        }
    }
}
