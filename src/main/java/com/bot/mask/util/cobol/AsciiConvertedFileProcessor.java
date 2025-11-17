/* (C) 2024 */
package com.bot.mask.util.cobol;

import com.bot.mask.log.LogProcess;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
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
//        parseCobolWithOptionalHeaderFooter(
//                data, null, layout, null, false, h -> {
//                }, consumer, t -> {
//                }, 1, 1);
    }

    @Override
    public void parseAndConsume2(
            byte[] data, List<CobolField> layout, Consumer<Map<String, String>> consumer) {
//        parseCobolWithOptionalHeaderFooter(
//                data, null, layout, null, true, h -> {
//                }, consumer, t -> {
//                }, 1, 1);
    }


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
        final int bodyLen   = (layoutBody   == null || layoutBody.isEmpty())
                ? 0 : decoder.calculateRecordLength(layoutBody);
        final int footerLen = (layoutFooter == null || layoutFooter.isEmpty())
                ? 0 : decoder.calculateRecordLength(layoutFooter);

        LogProcess.info(log, "useMs950Handle = {}", useMs950Handle);
        LogProcess.info(log, "HeaderLen = {}, BodyLen = {}, FooterLen = {}", headerLen, bodyLen, footerLen);
        LogProcess.info(log, "headerCnt = {}, footerCnt = {}", headerCnt, footerCnt);

//        if (headerCnt < 0 || footerCnt < 0) {
//            throw new IllegalArgumentException("headerCnt/footerCnt 不可為負數");
//        }

        if (!useMs950Handle) {
            // ===================== 變長：以換行分隔 (CRLF/LF) =====================
            // 串流讀檔，逐行切割，不轉十六進位、不整檔 split
            try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path))) {

                // 先處理表頭（前 headerCnt 行）
                for (int i = 0; i < headerCnt; i++) {
                    byte[] line = readOneRecord(in,headerLen,false);
                    if (line == null) {
                        // 檔案長度不夠
                        LogProcess.warn(log, "實際行數不足以讀取 headerCnt={}，已提早結束。", headerCnt);
                        return;
                    }
                    Map<String, String> row = decoder.decodeAsciiMs950(line, layoutHeader);
                    headerConsumer.accept(row);
                }

                // 表身 + 表尾：用 deque 緩存尾段
                ArrayDeque<byte[]> tailDeque = new ArrayDeque<>(Math.max(footerCnt, 1));
                while (true) {
//                    byte[] line = readOneLine(in);
                    byte[] line = readOneRecord(in,bodyLen,false);

                    if (line == null) {
                        break; // EOF
                    }

                    // 空行可視需求略過
                    if (line.length == 0) continue;

                    // 推入 tail；若超過 footerCnt，就彈出最舊者視為「表身」
                    tailDeque.addLast(line);
                    if (tailDeque.size() > footerCnt) {
                        byte[] bodyLine = tailDeque.removeFirst();
                        Map<String, String> row = decoder.decodeAsciiMs950(bodyLine, layoutBody);
                        bodyConsumer.accept(row);
                    }
                }

                // 檔案讀完後，deque 中剩下的就是表尾（footerCnt 行，不足就剩幾行就幾行）
                while (!tailDeque.isEmpty()) {
                    byte[] line = readOneRecord(in,footerLen,false);
//                    byte[] tailLine = tailDeque.removeFirst();
                    Map<String, String> footerRow = decoder.decodeAsciiMs950(line, layoutFooter);
                    footerConsumer.accept(footerRow);
                }

            } catch (IOException e) {
                LogProcess.error(log, "變長串流讀取失敗: {}", inputFile, e);
                throw new UncheckedIOException(e);
            }

        } else {
            // ===================== 定長：逐筆讀 recordLen =====================
//            if (bodyLen <= 0) {
//                throw new IllegalArgumentException("定長模式需要有效的 layoutBody/recordLen");
//            }
//            // 有指定 header/footer 但 layout 長度為 0 的防呆
//            if (headerCnt > 0 && headerLen == 0) {
//                throw new IllegalArgumentException("有指定 headerCnt，但 layoutHeader 或 headerLen 為 0");
//            }
//            if (footerCnt > 0 && footerLen == 0) {
//                throw new IllegalArgumentException("有指定 footerCnt，但 layoutFooter 或 footerLen 為 0");
//            }

            try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
                final long totalLen = ch.size();
                LogProcess.info(log, "File totalLen = {}", totalLen);

                // 表頭：直接讀 headerCnt 筆
                for (int i = 0; i < headerCnt; i++) {
                    byte[] headerBytes = readFully(ch, headerLen > 0 ? headerLen : bodyLen); // 若 headerLen = 0，視為與 bodyLen 同步長
                    Map<String, String> headerRow = (headerLen > 0)
                            ? decoder.decodeAscii(headerBytes, layoutHeader)
                            : decoder.decodeAscii(headerBytes, layoutBody); // 有些檔案表頭與表身同長度但不同 layout；若你一定有 layoutHeader，改回上面那行
                    headerConsumer.accept(headerRow);
                }

                // 表身 + 表尾：用 deque 暫存尾段
                ArrayDeque<byte[]> tailDeque = new ArrayDeque<>(Math.max(footerCnt, 1));

                while (true) {
                    byte[] rec;
                    try {
                        rec = readFully(ch, bodyLen);
                    } catch (EOFException eof) {
                        break;
                    }

                    tailDeque.addLast(rec);
                    if (tailDeque.size() > footerCnt) {
                        byte[] bodyRec = tailDeque.removeFirst();
                        Map<String, String> row = decoder.decodeAscii(bodyRec, layoutBody);
                        bodyConsumer.accept(row);
                    }
                }

                // deque 剩下的是表尾
                while (!tailDeque.isEmpty()) {
                    byte[] tailRec = tailDeque.removeFirst();
                    Map<String, String> footerRow = decoder.decodeAscii(tailRec, layoutFooter);
                    footerConsumer.accept(footerRow);
                }

                // 非必要但可做對齊檢查：totalLen 是否為
                // headerCnt*headerLen + n*bodyLen + footerCnt*footerLen（若 headerLen=0/異長，則略過）
                // 依需求加上警告或校驗

            } catch (IOException e) {
                LogProcess.error(log, "定長串流讀取失敗: {}", inputFile, e);
                throw new UncheckedIOException(e);
            }
        }
    }

    /* ===================== 輔助方法 ===================== */

    /** 逐行讀取：支援 CRLF(0D0A) 與 LF(0A)。回傳不含換行符號的 line bytes；EOF 則回傳 null。 */
    private byte[] readOneLine(BufferedInputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
        int b;
        boolean isFirstByte = true;

        while ((b = in.read()) != -1) {

            //  第一次就遇到 LF → 丟掉這一行，重新讀下一行 
            if (isFirstByte && b == 0x0A) {
                isFirstByte = true;   // 重置（下一輪會再當第一次）
                continue;             // 直接跳過，不算一筆資料
            }

            isFirstByte = false;  // 之後就不是第一個 byte 了

            //  遇到 CR/LF，一樣寫進內容並結束這一筆 
            if (b == 0x0D || b == 0x0A) {
                bos.write(b);     // 保留 CR or LF
                return bos.toByteArray();
            }

            bos.write(b);
        }

        // EOF：如果有資料就回傳
        return (bos.size() > 0) ? bos.toByteArray() : null;
    }


    /**
     * 讀一筆固定長度的資料：
     * @param in             輸入串流
     * @param bodyLenChars   一筆資料「原始字串」長度（例如 180）
     * @param isBinaryString true 表示讀的是「二進制字串（HEX）」→ 長度會用 bodyLen * 2
     * @return 一筆資料的 bytes；若 EOF 且沒有資料則回傳 null
     */
    private byte[] readOneRecord(BufferedInputStream in, int bodyLenChars, boolean isBinaryString)
            throws IOException {

        // 如果是「二進制字串（HEX 表示）」就 *2，否則就用原來長度
        int expectedLen = isBinaryString ? bodyLenChars * 2 : bodyLenChars;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(expectedLen);
        int b;
        boolean isFirstByte = true;

        while ((b = in.read()) != -1) {

            // 第一次就遇到 0x0A (LF) → 視為前置空白行，直接跳過
            if (isFirstByte && (b == 0x0A|| b == 0x0D)) {
                // 保持 isFirstByte = true，下一個 byte 還是當作第一個
                continue;
            }

            isFirstByte = false;

            // 如果遇到 CR (0x0D) 或 LF (0x0A) 都替換成空白
            if (b == 0x0A || b == 0x0D) {
                b = 0x20;  // ' '
            }

            // 把讀到的 byte 寫進內容
            bos.write(b);

            // 如果已經達到預期長度 → 為完整一筆，直接回傳
            if (bos.size() >= expectedLen) {
                return bos.toByteArray();
            }

        }

        // EOF：若 buffer 有內容，回傳最後一筆；否則回傳 null
        return (bos.size() > 0) ? bos.toByteArray() : null;
    }


    /** 從 FileChannel 讀滿指定長度；遇 EOF 丟 EOFException。 */
    private  byte[] readFully(FileChannel ch, int len) throws IOException {
        if (len <= 0) throw new IllegalArgumentException("len must be > 0");
        ByteBuffer buf = ByteBuffer.allocate(len);
        while (buf.hasRemaining()) {
            int n = ch.read(buf);
            if (n < 0) throw new EOFException("Unexpected EOF when reading " + len + " bytes");
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
//        LogProcess.info(log, "useMs950Handle = {}", useMs950Handle);
//        LogProcess.info(log, "總長度 = {}", data.length);
//        LogProcess.info(log, "HeaderLen = {},BodyLen = {},FooterLen = {}", layoutHeader.size(),layoutBody.size(),layoutFooter.size());
//
//
//
//
//
//        if (!useMs950Handle) {
//            // ========== HEX分割 0D0A ==========
//            String hex = decoder.bytesToHex(data).toUpperCase();
//            int total = 0;
//
//            String[] hexRecords = hex.split("0D0A");
//            if(hexRecords.length == 1){
//                hexRecords = hex.split("0A");
//            }
//            total = hexRecords.length;
//
//            LogProcess.info(log, "split newline character total = {}", total);
//
//            int start = 0;
//            int end = total;
//
//            // 處理表頭
//            if (layoutHeader != null
//                    && !layoutHeader.isEmpty()
//                    && headerCnt > 0
//                    && total >= headerCnt) {
//                for (int i = 0; i < headerCnt; i++) {
//                    byte[] headBytes = decoder.hexToBytes(hexRecords[i]);
//
//                    headerConsumer.accept(decoder.decodeAsciiMs950(headBytes, layoutHeader));
//                }
//                start = headerCnt;
//            } else {
//                start = 0;
//            }
//
//            // 處理表尾
//            if (layoutFooter != null
//                    && !layoutFooter.isEmpty()
//                    && footerCnt > 0
//                    && total >= headerCnt + footerCnt) {
//                end = total - footerCnt;
//            } else {
//                end = total;
//            }
//
//            // 處理表身（明細）
//            for (int i = start; i < end; i++) {
//                String hexLine = hexRecords[i].trim();
//
//                if (hexLine.isEmpty()) continue;
//
//                byte[] lineBytes = decoder.hexToBytes(hexLine);
//                bodyConsumer.accept(decoder.decodeAsciiMs950(lineBytes, layoutBody));
//            }
//
//            // 再處理表尾資料（用 footerCnt 計算）
//            if (layoutFooter != null
//                    && !layoutFooter.isEmpty()
//                    && footerCnt > 0
//                    && total >= headerCnt + footerCnt) {
//                for (int i = total - footerCnt; i < total; i++) {
//                    byte[] tailBytes = decoder.hexToBytes(hexRecords[i]);
//                    footerConsumer.accept(decoder.decodeAsciiMs950(tailBytes, layoutFooter));
//                }
//            }
//
//
//        } else {
//            // ========= 定長切割 ==========
//            int recordLen = layoutBody.stream().mapToInt(f -> (int) f.getDigits()).sum();
//            int totalRecords = data.length / recordLen;
//
//            LogProcess.info(log, "totalRecords total = {}", totalRecords);
//
//            int startIdx = 0;
//            int endIdx = totalRecords;
//
//            // 處理表頭
//            if (layoutHeader != null
//                    && !layoutHeader.isEmpty()
//                    && headerCnt > 0
//                    && totalRecords >= headerCnt) {
//                for (int i = 0; i < headerCnt; i++) {
//                    int s = i * recordLen;
//                    int e = s + recordLen;
//                    byte[] headBytes = Arrays.copyOfRange(data, s, e);
//                    headerConsumer.accept(decoder.decodeAscii(headBytes, layoutHeader));
//                }
//                startIdx = headerCnt;
//            }
//
//            // 處理表尾
//            if (layoutFooter != null
//                    && !layoutFooter.isEmpty()
//                    && footerCnt > 0
//                    && totalRecords >= headerCnt + footerCnt) {
//                endIdx = totalRecords - footerCnt;
//            } else {
//                endIdx = totalRecords;
//            }
//
//            // 處理表身（明細）
//            for (int i = startIdx; i < endIdx; i++) {
//                int s = i * recordLen;
//                int e = s + recordLen;
//                byte[] detailBytes = Arrays.copyOfRange(data, s, e);
//                bodyConsumer.accept(decoder.decodeAscii(detailBytes, layoutBody));
//            }
//
//            // 處理表尾
//            if (layoutFooter != null
//                    && !layoutFooter.isEmpty()
//                    && footerCnt > 0
//                    && totalRecords >= headerCnt + footerCnt) {
//                for (int i = totalRecords - footerCnt; i < totalRecords; i++) {
//                    int s = i * recordLen;
//                    int e = s + recordLen;
//                    byte[] tailBytes = Arrays.copyOfRange(data, s, e);
//                    footerConsumer.accept(decoder.decodeAscii(tailBytes, layoutFooter));
//                }
//            }
//        }
//    }
}
