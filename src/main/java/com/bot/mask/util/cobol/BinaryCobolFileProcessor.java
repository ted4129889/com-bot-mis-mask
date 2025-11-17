/* (C) 2024 */
package com.bot.mask.util.cobol;

import com.bot.mask.log.LogProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class BinaryCobolFileProcessor implements CobolFileProcessor {

    @Autowired
    CobolRecordDecoder decoder;

    @Override
    public List<Map<String, String>> parse(byte[] data, List<CobolField> layout) {

        int recordLen = decoder.calculateRecordLength(layout);

        int totalRecords = data.length / recordLen;
        LogProcess.info(log, "recordLen = {}", recordLen);
        LogProcess.info(log, "totalRecords = {}", totalRecords);
        List<Map<String, String>> result = new ArrayList<>();

        for (int i = 0; i < totalRecords; i++) {
            int offset = i * recordLen;
            byte[] record = Arrays.copyOfRange(data, offset, offset + recordLen);

            Map<String, String> row = decoder.decodeBinary(record, layout);
            result.add(row);
        }
        return result;
    }

    @Override
    public void parseAndConsume(
            byte[] data, List<CobolField> layout, Consumer<Map<String, String>> consumer) {
//        parseCobolWithOptionalHeaderFooter(
//                data, null, layout, null, true, h -> {}, consumer, f -> {}, 1, 1);
    }

    @Override
    public void parseAndConsume2(
            byte[] data, List<CobolField> layout, Consumer<Map<String, String>> consumer) {}


    @Override
    public void parseCobolWithOptionalHeaderFooter(
            String inputFile,
            List<CobolField> layoutHeader,
            List<CobolField> layoutBody,
            List<CobolField> layoutFooter,
            boolean useMs950Handle,
            Consumer<Map<String, String>> headerConsumer,
            Consumer<Map<String, String>> bodyConsumer,
            Consumer<Map<String, String>> footerConsumer,
            int headerCnt,
            int footerCnt) {

        Path path = Paths.get(inputFile);

        final int headerLen = (layoutHeader == null || layoutHeader.isEmpty())
                ? 0 : decoder.calculateRecordLength(layoutHeader);
        final int bodyLen = (layoutBody == null || layoutBody.isEmpty())
                ? 0 : decoder.calculateRecordLength(layoutBody);
        final int footerLen = (layoutFooter == null || layoutFooter.isEmpty())
                ? 0 : decoder.calculateRecordLength(layoutFooter);

        if (bodyLen <= 0) {
            throw new IllegalArgumentException("layoutBody 未設定或長度為 0，無法解析明細。");
        }
//        if (headerCnt < 0 || footerCnt < 0) {
//            throw new IllegalArgumentException("headerCnt/footerCnt 不可為負數。");
//        }

        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            final long totalLen = ch.size();

            final long headerBytesTotal = (long) headerCnt * headerLen;
            final long footerBytesTotal = (long) footerCnt * footerLen;

//            if (headerLen == 0 && headerCnt > 0) {
//                throw new IllegalArgumentException("有指定 headerCnt，但 layoutHeader 或 headerLen 為 0。");
//            }
//            if (footerLen == 0 && footerCnt > 0) {
//                throw new IllegalArgumentException("有指定 footerCnt，但 layoutFooter 或 footerLen 為 0。");
//            }

            // 邊界檢查
            if (headerBytesTotal + footerBytesTotal > totalLen) {
                throw new IllegalArgumentException(String.format(
                        "檔案長度不足：headerBytesTotal(%d) + footerBytesTotal(%d) > totalLen(%d)",
                        headerBytesTotal, footerBytesTotal, totalLen));
            }

            LogProcess.info(log, "File totalLen = {}", totalLen);
            LogProcess.info(log, "Header: len = {}, cnt = {}, bytes = {}",
                    headerLen, headerCnt, headerBytesTotal);
            LogProcess.info(log, "Body:   len = {}", bodyLen);
            LogProcess.info(log, "Footer: len = {}, cnt = {}, bytes = {}",
                    footerLen, footerCnt, footerBytesTotal);

            long offset = 0L;

            // ---------- 讀表頭 ----------
            if (headerLen > 0 && headerCnt > 0) {
                for (int i = 0; i < headerCnt; i++) {
                    byte[] headerBytes = readFully(ch, headerLen);
                    Map<String, String> headerRow = decoder.decodeBinary(headerBytes, layoutHeader);
                    headerConsumer.accept(headerRow);
                    offset += headerLen;
                }
            }

            // ---------- 讀明細 (直到剩下 footerBytesTotal 為止) ----------
            final long bodyRegionEnd = totalLen - footerBytesTotal; // 不包含 footer 區
            while (offset + bodyLen <= bodyRegionEnd) {
                byte[] recordBytes = readFully(ch, bodyLen);
                LogProcess.info(log, "recordBytes = {}", recordBytes);
                Map<String, String> row = decoder.decodeBinary(recordBytes, layoutBody);
                LogProcess.info(log, "row = {}", row);
                bodyConsumer.accept(row);
                offset += bodyLen;
            }

            // 若有殘留非整筆長度的內容（通常不應該發生），視情況處理或警告
            if (offset < bodyRegionEnd) {
                long remaining = bodyRegionEnd - offset;
                // 依需求可選擇：略過、讀掉、或直接丟錯
                LogProcess.warn(log, "Body 區不是整筆對齊，尚餘 {} bytes，將略過。", remaining);
                ch.position(offset + remaining);
                offset += remaining;
            }

            // ---------- 讀表尾 ----------
            if (footerLen > 0 && footerCnt > 0) {
                for (int i = 0; i < footerCnt; i++) {
                    byte[] footerBytes = readFully(ch, footerLen);
                    Map<String, String> footerRow = decoder.decodeBinary(footerBytes, layoutFooter);
                    footerConsumer.accept(footerRow);
                    offset += footerLen;
                }
            }

            // 最終校驗
            if (offset != totalLen) {
                LogProcess.warn(log, "解析完畢但 offset({}) != totalLen({})，可能有額外 bytes。", offset, totalLen);
            }

        } catch (IOException e) {
            LogProcess.error(log, "讀取檔案失敗: {}", inputFile, e);
            // 如果你有原本的處理：建立空檔
            // textFileUtil.createEmptyFileIfNotExist(outputConvert);
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 從 FileChannel 讀滿指定長度的 bytes；若中途 EOF 則丟出 IOException。
     */
    private static byte[] readFully(FileChannel ch, int len) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(len);
        while (buf.hasRemaining()) {
            int n = ch.read(buf);
            if (n < 0) {
                throw new EOFException("Unexpected EOF when reading " + len + " bytes");
            }
        }
        return buf.array();
    }

//    @Override
//    public void parseCobolWithOptionalHeaderFooter(
//            String inputFile,
//            List<CobolField> layoutHeader,
//            List<CobolField> layoutBody,
//            List<CobolField> layoutFooter,
//            boolean useMs950Handle,
//            Consumer<Map<String, String>> headerConsumer,
//            Consumer<Map<String, String>> bodyConsumer,
//            Consumer<Map<String, String>> footerConsumer,
//            int headerCnt,
//            int footerCnt) {
//
//        CobolRecordDecoder decoder = new CobolRecordDecoder();
//
//        int headerLen = layoutHeader == null ? 0 : decoder.calculateRecordLength(layoutHeader);
//        int bodyLen = layoutBody == null ? 0 : decoder.calculateRecordLength(layoutBody);
//        int footerLen = layoutFooter == null ? 0 : decoder.calculateRecordLength(layoutFooter);
//
//        int totalLen = data.length;
//        int offset = 0;
//
//        LogProcess.info(log, "Binary總長度 = {}", totalLen);
//        LogProcess.info(log, "HeaderLen = {},BodyLen = {},FooterLen = {}", headerLen,bodyLen,footerLen);
//
//        // 表頭（若有）
//        if (headerLen > 0 && offset + headerLen <= totalLen) {
//            byte[] headerBytes = Arrays.copyOfRange(data, offset, offset + headerLen);
//            Map<String, String> headerRow = decoder.decodeBinary(headerBytes, layoutHeader);
//            headerConsumer.accept(headerRow);
//            offset += headerLen;
//        }
//
//        // 表尾（若有）
//        int footerOffset = totalLen;
//        if (footerLen > 0 && (totalLen - footerLen) >= offset) {
//            footerOffset = totalLen - footerLen;
//        } else {
//            footerLen = 0; // 沒有表尾
//        }
//
//        // 明細（多筆）
//        while (offset + bodyLen <= footerOffset) {
//
//            byte[] record = Arrays.copyOfRange(data, offset, offset + bodyLen);
//
//            Map<String, String> row = decoder.decodeBinary(record, layoutBody);
//            bodyConsumer.accept(row);
//            offset += bodyLen;
//        }
//
//        // 表尾（若有）
//        if (footerLen > 0 && footerOffset < totalLen) {
//            byte[] footerBytes = Arrays.copyOfRange(data, footerOffset, totalLen);
//            Map<String, String> footerRow = decoder.decodeBinary(footerBytes, layoutFooter);
//            footerConsumer.accept(footerRow);
//        }
//    }
//
//    public String bytesToHex(byte[] bytes) {
//        StringBuilder sb = new StringBuilder();
//        for (byte b : bytes) {
//            sb.append(String.format("%02X", b));
//        }
//        return sb.toString();
//    }
}
