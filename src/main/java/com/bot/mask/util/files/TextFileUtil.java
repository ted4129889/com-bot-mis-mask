/* (C) 2023 */
package com.bot.mask.util.files;

import com.bot.mask.log.LogProcess;
import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Scope("prototype")
public class TextFileUtil {
    @Value("${app.file-processing.max-lines}")
    private int maxLines;

    @Value("${app.file-processing.timeout}")
    private long timeout;

    public TextFileUtil() {
        // YOU SHOULD USE @Autowired ,NOT new TextFileUtil()
    }

    /**
     * Reads the contents of the specified file and returns it as a list of strings, with each
     * string representing a line from the file. The file is read using the specified charset.
     *
     * @param filePath    The path to the file whose contents are to be read.
     * @param charsetName The name of the charset to use for decoding the file content. Supported
     *                    charsets are "UTF-8" and "BIG5".
     * @return List of strings where each string is a line read from the file specified by filePath.
     */
    public List<String> readFileContent(String filePath, String charsetName) {

        String normalizedPath = FilenameUtils.normalize(filePath);
        Path path = Paths.get(normalizedPath);

        List<String> fileContents = new ArrayList<>();

        Charset charset= null;
            if ("UTF-8".equalsIgnoreCase(charsetName)) {
                charset = StandardCharsets.UTF_8;
            } else if ("BIG5".equalsIgnoreCase(charsetName)) {
                charset = Charset.forName("Big5");
            } else {
                charset = Charset.forName(charsetName);
            }
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)      // Big5 編碼的中文都是兩個byte一組不是的話就會有問題，會以 � 代替。
                .onUnmappableCharacter(CodingErrorAction.REPLACE);// 找不到Unicode 字元的 byte 組合時，也用 � 代替。

        int lineCount = 0;
        try (InputStream rawIn = Files.newInputStream(path);

             Reader in = new InputStreamReader(rawIn, decoder);
             BufferedReader reader = new BufferedReader(in, 8192)) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                // 簡單偵測：若這行包含替代字元，打警告，之後再決定是否回頭抓原始 HEX
                if (line.indexOf('\uFFFD') >= 0) {
                    LogProcess.warn(log, "Line {} has invalid bytes (shown as �) => {}", lineCount,line);
                }
                fileContents.add(line);
            }
            LogProcess.info(log, "1 source data count = {}", lineCount);

        } catch (IOException e) {
            LogProcess.info(log, "2 source data count = {}", lineCount);
            LogProcess.error(log, "Error Message: file is problem = {}", e.getMessage(), e);
        }
        return fileContents;
    }

    //        public List<String> readFileContent(String filePath, String charsetName) {
//
//            String normalizedPath = FilenameUtils.normalize(filePath);
//            Path path = Paths.get(normalizedPath);
//            Charset charset = null;
//            List<String> fileContents = new ArrayList<>();
//
//            if ("UTF-8".equalsIgnoreCase(charsetName)) {
//                charset = StandardCharsets.UTF_8;
//            } else if ("BIG5".equalsIgnoreCase(charsetName)) {
//                charset = Charset.forName("Big5");
//            } else {
//            }
//            BufferedReader reader = null;
//            int lineCount = 0;
//
//            try {
//                // 驗證檔案是否存在以及是否可讀性
//                if (!Files.exists(path) || !Files.isReadable(path)) {
//                    throw new IllegalArgumentException(
//                            "File does not exist or does not differ：" + path);
//                } else {
//                    reader = Files.newBufferedReader(path, charset);
//
//                    String line;
//
//                    while ((line = reader.readLine()) != null) {
//                        lineCount++;
//
//                        fileContents.add(line);
//                    }
//
//                    LogProcess.info(log,"1 source data count = {}",lineCount);
//                }
//            } catch (IOException e) {
//                LogProcess.info(log,"2 source data count = {}",lineCount);
//                LogProcess.error(log,"Error Message: file is problem = {}",e);
//
//            } finally {
//                if (reader != null) {
//                    try {
//                        reader.close(); // 很重要！
//                    } catch (IOException e) {
//                        LogProcess.error(log,"Error Message: fail: close file = {}",e);
//
//                    }
//                }
//            }
//
//            return fileContents;
//        }
    public List<String> readFileContent2(String filePath, String charsetName) {
        String normalizedPath = FilenameUtils.normalize(filePath);
        Path path = Paths.get(normalizedPath);
        List<String> fileContents = new ArrayList<>();

        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("File does not exist or is not readable: " + path);
        }
        LogProcess.info(log, "Start readFileContent, filePath = {}, charsetName = {}", filePath, charsetName);

        try {
            if ("UTF-8".equalsIgnoreCase(charsetName)) {
                LogProcess.info(log, "Using UTF-8 decoder with REPLACE policy");

                CharsetDecoder dec = StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE)
                        .replaceWith(" ");

                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(Files.newInputStream(path), dec))) {
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null) {
                        lineCount++;
                        fileContents.add(line);

                        // 細粒度 log：顯示行號、長度與前 20 字元
                        String preview = line.length() > 20 ? line.substring(0, 20) + "..." : line;
                        LogProcess.debug(log, "[UTF-8] line {} length={} preview=\"{}\"", lineCount, line.length(), preview);
                    }
                    LogProcess.info(log, "Completed UTF-8 read, total lines = {}", lineCount);
                }
            } else if ("BIG5".equalsIgnoreCase(charsetName)) {
                LogProcess.info(log, "Using Big5 (MS950) per-line reader with cleaning");


                Big5Result r = readBig5PerLineWithFix(path);
                int lineCount = 0;
                for (String line : r.lines) {

                    lineCount++;
                    fileContents.add(line);

//                    String preview = line.length() > 20 ? line.substring(0, 20) + "..." : line;
//                    LogProcess.debug(log,"[BIG5] line {} length={} preview=\"{}\"", lineCount, line.length(), preview);
                }
                LogProcess.info(log, "Completed Big5 read, total lines = {}", lineCount);

                if (!r.anomalies.isEmpty()) {
                    for (Anomaly a : r.anomalies) {
                        LogProcess.warn(log, "Big5 anomaly at line {}, offset {}: {} | ctx={}",
                                a.lineNo, a.byteOffset, a.reason, a.hexContext);
                    }
                }
            } else {
                LogProcess.info(log, "Using generic charset={} decoder with REPLACE policy", charsetName);

                CharsetDecoder dec = Charset.forName(charsetName)
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE)
                        .replaceWith(" ");

                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(Files.newInputStream(path), dec))) {
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null) {
                        lineCount++;
                        fileContents.add(line);

                        String preview = line.length() > 20 ? line.substring(0, 20) + "..." : line;
                        LogProcess.debug(log, "[{}] line {} length={} preview=\"{}\"",
                                charsetName, lineCount, line.length(), preview);
                    }
                    LogProcess.info(log, "Completed {} read, total lines = {}", charsetName, lineCount);
                }
            }
        } catch (IOException e) {
            LogProcess.error(log, "Error reading file [{}]: {}", filePath, e.toString());
        }

        LogProcess.info(log, "End readFileContent, total lines collected = {}", fileContents.size());
        return fileContents;
    }


    /**
     * Writes the provided list of lines to the file at the specified path using the given charset.
     * This method will create a new file if it does not exist, or it will append to the file if it
     * already exists.
     *
     * @param filePath    The path to the file where the lines will be written.
     * @param lines       The content to write to the file, with each string in the list representing a
     *                    separate line.
     * @param charsetName The name of the charset to use for encoding the file content. Supported
     *                    charsets are "UTF-8" and "BIG5".
     */
    public void writeFileContent(String filePath, List<String> lines, String charsetName) {
        maxLines = 1000;
        timeout = 5000;
        String allowedPath = FilenameUtils.normalize(filePath);

        Path path = Paths.get(allowedPath);
        Charset charset = null;
        CharsetEncoder encoder;

        if ("UTF-8".equalsIgnoreCase(charsetName)) {
            charset = StandardCharsets.UTF_8;
        } else if ("BIG5".equalsIgnoreCase(charsetName)) {
            charset = Charset.forName("Big5");
        }
        encoder = charset.newEncoder();
        encoder.onMalformedInput(CodingErrorAction.REPLACE);
        encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            //如果有沒有資料夾，則建立一個資料夾
            if (!Files.exists(path.getParent())) Files.createDirectories(path.getParent());
//
            if (!Files.exists(path)) Files.createFile(path);

            try (FileOutputStream fos = new FileOutputStream(allowedPath, true);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, encoder);
                 BufferedWriter writer = new BufferedWriter(osw)) {

                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            } catch (Exception e) {
                LogProcess.error(log, "Error Message: not found file = {}", e);

            }
        } catch (Exception e) {
            LogProcess.error(log, "Error Message: There is a problem with the file = {}", e);

        }
    }

    public void deleteDir(String dirPath) {
        Path path = Paths.get(dirPath);
        if (Files.exists(path) && Files.isDirectory(path)) {
            try {
                Files.walkFileTree(
                        path,
                        new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                                    throws IOException {
                                Files.delete(file);
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                                    throws IOException {
                                if (exc != null) throw exc;
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }
                        });
            } catch (IOException e) {
                LogProcess.error(log, "Error Message: delete file fail = {}", e);

            }
        }
    }

    public void deleteFile(String filePath) {
        Path path = Paths.get(filePath);
        if (Files.exists(path) && !Files.isDirectory(path)) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                LogProcess.error(log, "Error Message: delete file fail = {}", e);
            }
        }
    }

    public boolean exists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }


    private boolean isValidInput(String input) {
        if (input == null || input.length() == 0) {
            LogProcess.info(log, "isValidInput is null");
            return false;
        }
        return true;
        // 允許：中文、英數字、底線(_)、@、.、-、空格
//        if (input.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_@$.,?+\\-\\s\\u3000]+$")) {
//            return true;
//        } else {
//            LogProcess.info("input =" + input);
//            LogProcess.info("Not Match");
//            return false;
//        }
    }

    public String replaceDateWithPlaceholder(String fileName) {
        // 偵測可能的日期格式：西元 (yyyyMMdd) 或民國 (yyyMMdd)
        // 這裡假設月日是合法的（01-12, 01-31），不做嚴格校驗
        Pattern pattern = Pattern.compile("\\b(\\d{7,8})\\b");
        Matcher matcher = pattern.matcher(fileName);

        while (matcher.find()) {
            String dateStr = matcher.group(1);

            // 嘗試判斷是否為合法日期
            if (isValidDate(dateStr)) {
                return matcher.replaceFirst("[yyyymmdd]");
            }
        }

        return fileName;
    }

    private boolean isValidDate(String dateStr) {
        String fullDateStr;

        // 民國年 (7碼): 前3碼年份轉為西元
        if (dateStr.length() == 7) {
            try {
                int rocYear = Integer.parseInt(dateStr.substring(0, 3));
                fullDateStr = (rocYear + 1911) + dateStr.substring(3);
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (dateStr.length() == 8) {
            fullDateStr = dateStr;
        } else {
            return false;
        }

        // 驗證是否為有效日期
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate.parse(fullDateStr, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }


    /**
     * 若檔案不存在，先建立一個空檔
     */
    public void createEmptyFileIfNotExist(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                Files.createDirectories(Paths.get(file.getParent())); // 確保目錄存在
                Files.createFile(Paths.get(path)); // 建立空檔
            }
        } catch (IOException e) {
            ApLogHelper.error(
                    log, false, LogType.NORMAL.getCode(), "createEmptyFileIfNotExist failed , {}", e);
        }
    }


    private final Charset MS950 = Charset.forName("MS950");

    private class Anomaly {
        final int lineNo;
        final int byteOffset;
        final String hexContext;
        final String reason;

        Anomaly(int lineNo, int byteOffset, String hexContext, String reason) {
            this.lineNo = lineNo;
            this.byteOffset = byteOffset;
            this.hexContext = hexContext;
            this.reason = reason;
        }
    }

    private class Big5Result {
        final List<String> lines = new ArrayList<>();
        final List<Anomaly> anomalies = new ArrayList<>();
    }

    private Big5Result readBig5PerLineWithFix(Path path) throws IOException {
        Big5Result result = new Big5Result();
//        LogProcess.info("Start readBig5PerLineWithFix, path={}", path);

        try (PushbackInputStream in = new PushbackInputStream(Files.newInputStream(path), 3)) {

            byte[] bom = new byte[3];
            int n = in.read(bom, 0, 3);
            boolean hasBom = (n == 3 && (bom[0] & 0xFF) == 0xEF && (bom[1] & 0xFF) == 0xBB && (bom[2] & 0xFF) == 0xBF);

            if (hasBom) {
                LogProcess.info(log, "Detected UTF-8 BOM, skip first 3 bytes");
            } else {
                if (n > 0) {
                    in.unread(bom, 0, n);
                    LogProcess.info(log, "No BOM detected, push back {} byte(s)", n);
                } else {
                    LogProcess.info(log, "No BOM detected, nothing to push back (n={})", n);
                }
            }

            ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
            int b, lineNo = 0;


            while ((b = in.read()) != -1) {
                if (b == '\n') {
                    lineNo++;
                    byte[] raw = buf.toByteArray();
                    buf.reset();
                    byte[] cleaned = cleanBig5Line(raw, lineNo, result.anomalies);
                    String lineStr = decodeMs950(cleaned);

                    result.lines.add(lineStr);

                } else if (b != '\r') {
                    buf.write(b);
                }


            }

            if (buf.size() > 0) {
                lineNo++;
                byte[] raw = buf.toByteArray();
                byte[] cleaned = cleanBig5Line(raw, lineNo, result.anomalies);
                String lineStr = decodeMs950(cleaned);
                result.lines.add(lineStr);

                String preview = lineStr.length() > 30 ? lineStr.substring(0, 30) + "..." : lineStr;
//                LogProcess.debug("[BIG5] line {} (last) length={} preview=\"{}\"", lineNo, lineStr.length(), preview);
            }

//            LogProcess.info("Completed readBig5PerLineWithFix, total lines={}", lineNo);
        }

        return result;
    }

    public byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] =
                    (byte)
                            ((Character.digit(hex.charAt(i), 16) << 4)
                                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }


    private String decodeMs950(byte[] bytes) throws CharacterCodingException {
        CharsetDecoder dec = MS950.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith(" ");
        return dec.decode(ByteBuffer.wrap(bytes)).toString();
    }

    /**
     * 處理整行 84 C9、行尾落單前導位元組、非法尾位元、控制字元等
     */
    private byte[] cleanBig5Line(byte[] bs, int lineNo, List<Anomaly> anomalies) {
        if (bs.length == 0) return bs;

        // 整行等同「84 C9」重複（你觀察到的“全行空白”誤碼）→ 改為 0x20 空白
        if (isAllPattern(bs, (byte) 0x84, (byte) 0xC9)) {
            Arrays.fill(bs, (byte) 0x20);
            return bs;
        }

        //掃描第一個 Big5 非法點（落單前導位元、非法尾位元、非 Big5 範圍）
        int bad = findFirstInvalidBig5(bs);
        if (bad >= 0) {
            anomalies.add(new Anomaly(lineNo, bad, hexContext(bs, bad, 16),
                    "Invalid Big5 sequence (dangling lead or bad trail)"));
            // 2a) 若是「行尾落單前導位元組」→ 直接改空白（最常見會噴 Input length = 1）
            if (isDanglingLeadAtEnd(bs, bad)) {
                bs[bad] = 0x20;
            } else {
                // 2b) 其他異常：把該位元組改空白（保命、不中斷）
                bs[bad] = 0x20;
            }
            return bs;
        }

        // 3) 清除不可見控制字元（保留 TAB 0x09）
        for (int i = 0; i < bs.length; i++) {
            int v = bs[i] & 0xFF;
            if ((v < 0x09 && v != 0x09) || (v >= 0x0B && v <= 0x1F) || v == 0x7F) {
                bs[i] = 0x20;
            }
        }

        return bs;
    }

    private boolean isAllPattern(byte[] bs, byte a, byte b) {
        if ((bs.length % 2) != 0) return false;
        for (int i = 0; i < bs.length; i += 2) {
            if (bs[i] != a || bs[i + 1] != b) return false;
        }
        return bs.length > 0;
    }

    /**
     * 回傳第一個非法偏移；全部合法回 -1
     */
    private int findFirstInvalidBig5(byte[] bs) {
        int i = 0;
        while (i < bs.length) {
            int b = bs[i] & 0xFF;
            if (b <= 0x7F) {
                i++; // ASCII
            } else if (b >= 0x81 && b <= 0xFE) {
                if (i + 1 >= bs.length) return i; // 落單前導位元組
                int t = bs[i + 1] & 0xFF;
                boolean validTrail = (t >= 0x40 && t <= 0x7E) || (t >= 0xA1 && t <= 0xFE);
                if (!validTrail) return i; // 非法尾位元
                i += 2;
            } else {
                return i; // 非 Big5 範圍
            }
        }
        return -1;
    }

    private boolean isDanglingLeadAtEnd(byte[] bs, int pos) {
        return pos == bs.length - 1 && (bs[pos] & 0xFF) >= 0x81 && (bs[pos] & 0xFF) <= 0xFE;
    }

    private static String hexContext(byte[] bs, int at, int around) {
        int from = Math.max(0, at - around);
        int to = Math.min(bs.length, at + around + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i == at) sb.append("<<");
            sb.append(String.format("%02X", bs[i] & 0xFF));
            if (i == at) sb.append(">>");
            sb.append(' ');
        }
        return sb.toString().trim();
    }


    private final char[] HEX = "0123456789ABCDEF".toCharArray();

    // 將整個檔轉成 HEX，每筆被 0D0A / 0D / 0A 分隔；每筆 HEX 在輸出檔佔一行
    public long toHexLines(Path input, Path hexOutput) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(input), 1 << 16);
             BufferedWriter out = Files.newBufferedWriter(hexOutput)) {

            long records = 0;
            int prev = -1, b;
            boolean lineHasData = false;

            // 小工具：把一個 byte 以「兩位 HEX + 空白」寫出
            final var sb = new StringBuilder(3 * 4096); // 小幅緩衝，避免頻繁 out.write
            Runnable flushChunk = () -> {
                try {
                    if (sb.length() > 0) {
                        out.write(sb.toString());
                        sb.setLength(0);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            java.util.function.IntConsumer writeHex = (val) -> {
                sb.append(HEX[(val >>> 4) & 0xF]).append(HEX[val & 0xF]).append(' ');
            };

            while ((b = in.read()) != -1) {
                // 先處理前一個 CR 的情形
                if (prev == 0x0D) {
                    if (b == 0x0A) {             // CRLF
                        flushChunk.run();
                        out.newLine();           // 結束一筆
                        records++;
                        lineHasData = false;
                        prev = -1;
                        continue;                // 0x0A 不再當資料
                    } else {
                        // 單獨 CR 當分隔
                        flushChunk.run();
                        out.newLine();
                        records++;
                        lineHasData = false;
                        // 接著把當前 b 當新資料處理
                    }
                }

                if (b == 0x0D) {                 // 可能形成 CRLF，先暫存
                    prev = 0x0D;
                    continue;
                }
                if (b == 0x0A) {                 // 單獨 LF 當分隔
                    flushChunk.run();
                    out.newLine();
                    records++;
                    lineHasData = false;
                    prev = -1;
                    continue;
                }

                // 一般資料 → 直接寫 HEX（不累積整筆）
                writeHex.accept(b & 0xFF);
                lineHasData = true;
                prev = -1;

                // 偶爾把 chunk 刷出去，避免 StringBuilder 太大
                if (sb.length() >= 3 * 4096) flushChunk.run();
            }

            // 檔尾如果停在 CR（沒有 LF）→ 補一筆
            if (prev == 0x0D || lineHasData) {
                flushChunk.run();
                out.newLine();
                if (lineHasData || prev == 0x0D) records++;
            }
            return records;
        }
    }

}
