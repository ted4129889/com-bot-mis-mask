/* (C) 2024 */
package com.bot.mask.util.cobol;

import com.bot.mask.log.LogProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class BinaryCobolFileProcessor implements CobolFileProcessor {

    @Override
    public List<Map<String, String>> parse(byte[] data, List<CobolField> layout) {

        CobolRecordDecoder decoder = new CobolRecordDecoder();
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
        parseCobolWithOptionalHeaderFooter(
                data, null, layout, null, true, h -> {}, consumer, f -> {}, 1, 1);
    }

    @Override
    public void parseAndConsume2(
            byte[] data, List<CobolField> layout, Consumer<Map<String, String>> consumer) {}

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

        CobolRecordDecoder decoder = new CobolRecordDecoder();

        int headerLen = layoutHeader == null ? 0 : decoder.calculateRecordLength(layoutHeader);
        int bodyLen = layoutBody == null ? 0 : decoder.calculateRecordLength(layoutBody);
        int footerLen = layoutFooter == null ? 0 : decoder.calculateRecordLength(layoutFooter);

        int totalLen = data.length;
        int offset = 0;

        LogProcess.info(log, "Binary總長度 = {}", totalLen);
        LogProcess.info(log, "HeaderLen = {},BodyLen = {},FooterLen = {}", headerLen,bodyLen,footerLen);

        // 表頭（若有）
        if (headerLen > 0 && offset + headerLen <= totalLen) {
            byte[] headerBytes = Arrays.copyOfRange(data, offset, offset + headerLen);
            Map<String, String> headerRow = decoder.decodeBinary(headerBytes, layoutHeader);
            headerConsumer.accept(headerRow);
            offset += headerLen;
        }

        // 表尾（若有）
        int footerOffset = totalLen;
        if (footerLen > 0 && (totalLen - footerLen) >= offset) {
            footerOffset = totalLen - footerLen;
        } else {
            footerLen = 0; // 沒有表尾
        }

        // 明細（多筆）
        while (offset + bodyLen <= footerOffset) {

            byte[] record = Arrays.copyOfRange(data, offset, offset + bodyLen);

            Map<String, String> row = decoder.decodeBinary(record, layoutBody);
            bodyConsumer.accept(row);
            offset += bodyLen;
        }

        // 表尾（若有）
        if (footerLen > 0 && footerOffset < totalLen) {
            byte[] footerBytes = Arrays.copyOfRange(data, footerOffset, totalLen);
            Map<String, String> footerRow = decoder.decodeBinary(footerBytes, layoutFooter);
            footerConsumer.accept(footerRow);
        }
    }

    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
