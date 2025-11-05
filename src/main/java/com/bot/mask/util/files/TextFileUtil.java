/* (C) 2023 */
package com.bot.mask.util.files;

import com.bot.mask.log.LogProcess;
import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import com.bot.txcontrol.exception.LogicException;
import com.bot.txcontrol.util.dump.ExceptionDump;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
    /**
     * 以指定的位元組長度分段讀取檔案內容，保持包含 CR (0x0D) 與 LF (0x0A) 字元。
     *
     * <p>將檔案分段讀取，每段長度約為 {@code length} 位元組，並依序存入 {@code List<byte[]>} 中回傳。
     * 若最後一段未滿長度，仍會以指定長度填充（可能含多餘資料）。
     *
     * @param filePath 要讀取之檔案完整路徑字串。
     * @param length   要讀取的位元組長度（包含 CR LF 符號），每次讀取的 buffer 大小。
     * @return List<byte [ ]> 讀取後的檔案內容清單，每筆元素長度固定為 {@code length} 位元組。
     */
    public List<byte[]> readFileByByte(String filePath, int length) {
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "readFileByByte");
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "filePath = {}", filePath);
        byte[] buffer = new byte[length];
        int bytesRead;
        List<byte[]> fileContents = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(Paths.get(filePath).toFile())) {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] completeRecord = new byte[length];
                System.arraycopy(buffer, 0, completeRecord, 0, length);
                fileContents.add(completeRecord);
            }
        } catch (Exception e) {
            ApLogHelper.error(log, false, LogType.NORMAL.getCode(), "readFileByByte error");
            ApLogHelper.error(
                    log, false, LogType.NORMAL.getCode(), ExceptionDump.exception2String(e));
        }
        return fileContents;
    }

    /**
     * 處理檔案結尾可能存在的不完整記錄，確保每一筆讀取到的資料都是獨立且正確的。 *
     */
    public List<byte[]> readFileByByte2(String filePath, int recordLength) {
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "readFileByByte");
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "filePath = {}", filePath);

        List<byte[]> fileContents = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(Paths.get(filePath).toFile())) {
            byte[] buffer = new byte[recordLength]; // 創建一個緩衝區，大小為每筆記錄的長度
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // 在此處處理每一筆記錄
                // 檢查是否讀到了完整的記錄長度
                if (bytesRead == recordLength) {
                    // 如果讀到了完整的記錄，直接加入清單
                    byte[] completeRecord = new byte[recordLength];
                    System.arraycopy(buffer, 0, completeRecord, 0, recordLength);
                    fileContents.add(completeRecord);
                } else {
                    // 如果讀取到的位元組數小於記錄長度，表示已經到達檔案結尾
                    byte[] partialRecord = new byte[bytesRead];
                    System.arraycopy(buffer, 0, partialRecord, 0, bytesRead);
                    fileContents.add(partialRecord);
                }
            }
        } catch (Exception e) {
            ApLogHelper.error(log, false, LogType.NORMAL.getCode(), "readFileByByte error");
            ApLogHelper.error(
                    log, false, LogType.NORMAL.getCode(), ExceptionDump.exception2String(e));
        }
        return fileContents;
    }

    /**
     * Reads the contents of the specified file and returns it as a list of strings, with each
     * string representing a line from the file. The file is read using the specified charset.
     *
     * @param filePath    The path to the file whose contents are to be read.
     * @param charsetName The name of the charset to use for decoding the file content. Supported
     *                    charsets are "UTF-8" and "BIG5".
     * @return List of strings where each string is a line read from the file specified by filePath.
     * @throws LogicException if an I/O error occurs during reading of the file.
     */
//    public List<String> readFileContent(String filePath, String charsetName) throws LogicException {
//        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "readFileContent");
//        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "filePath = {}", filePath);
//        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "charsetName = {}", charsetName);
//        Path path = Paths.get(filePath);
//        Charset charset;
//        List<String> fileContents = new ArrayList<>();
//
//        if ("UTF-8".equalsIgnoreCase(charsetName)) {
//            charset = StandardCharsets.UTF_8;
//        } else if ("BIG5".equalsIgnoreCase(charsetName)) {
//            charset = Charset.forName("CP950");
//        } else if ("CP950".equalsIgnoreCase(charsetName)) {
//            charset = Charset.forName("CP950");
//        } else {
//            throw new LogicException("E999", "Unsupported charset:" + charsetName);
//        }
//
//        try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
//            String line;
//            while ((line = reader.readLine()) != null) fileContents.add(line);
//
//            while (!fileContents.isEmpty()
//                    && fileContents.get(fileContents.size() - 1).trim().isEmpty()) {
//                fileContents.remove(fileContents.size() - 1);
//            }
//        } catch (IOException e) {
//            ApLogHelper.error(
//                    log, false, LogType.NORMAL.getCode(), ExceptionDump.exception2String(e));
//            //            if ("BIG5".equalsIgnoreCase(charsetName)) {
//            //                fileContents.clear();
//            //                fileContents.addAll(this.readFileContent(filePath, "CP950"));
//            //            } else
//            // Appropriately handle the exception
//            throw new LogicException("E999", "Error reading file");
//        }
//        ApLogHelper.info(
//                log,
//                false,
//                LogType.NORMAL.getCode(),
//                "fileContents.size = {}",
//                fileContents.size());
//        return fileContents;
//    }

    //第二版讀檔案20250918
    public List<String> readFileContent(String filePath, String charsetName) {
        String normalizedPath = FilenameUtils.normalize(filePath);
        Path path = Paths.get(normalizedPath);

        //Charset ：BIG5 一律改用 MS950
        Charset cs;
        if (charsetName == null) {
            cs = StandardCharsets.UTF_8;
        } else if ("UTF-8".equalsIgnoreCase(charsetName)) {
            cs = StandardCharsets.UTF_8;
        } else if ("BIG5".equalsIgnoreCase(charsetName) || "MS950".equalsIgnoreCase(charsetName)) {
            cs = Charset.forName("MS950");
        } else {
            cs = Charset.forName(charsetName);
        }

        // 調成 true 時，遇到不識字就丟例外
        final boolean strictMode = false;

        CharsetDecoder decoder = cs.newDecoder();
        //是否用替代字取代
        if (strictMode) {
            decoder.onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
        } else {
            decoder.onMalformedInput(CodingErrorAction.REPLACE) // 錯的位元組 會被置換
                    .onUnmappableCharacter(CodingErrorAction.REPLACE) // 無對應的字元 會被置換
                    .replaceWith("?"); // 可改 "□" 或 " "求
        }


        List<String> lines = new ArrayList<>();
        int lineCount = 0;

        try (InputStream rawIn = Files.newInputStream(path);
             Reader in = new InputStreamReader(rawIn, decoder);
             BufferedReader reader = new BufferedReader(in, 64 * 1024)) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;

                if (lineCount == 1 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }

                // 替代字
                if (!strictMode && (line.indexOf('\uFFFD') >= 0 || line.indexOf('?') >= 0)) {
                    LogProcess.warn(log, "Line {} contains replacement chars. length={}", lineCount, line.length());
                }
                lines.add(line);
            }
            LogProcess.info(log, "source data count = {}", lineCount);

        } catch (CharacterCodingException cce) {
            //可在此回頭做 Bytes 定位
            LogProcess.error(log, "Decoding failed near line {}: {}", lineCount + 1, cce.getMessage(), cce);
        } catch (IOException ioe) {
            LogProcess.info(log, "source data count (before error) = {}", lineCount);
            LogProcess.error(log, "I/O error: {}", ioe.getMessage(), ioe);
        }

        return lines;
    }

    //第一版讀檔案
    public List<String> readFileContent2(String filePath, String charsetName) {

        String normalizedPath = FilenameUtils.normalize(filePath);
        Path path = Paths.get(normalizedPath);
        Charset charset = null;
        List<String> fileContents = new ArrayList<>();

        if ("UTF-8".equalsIgnoreCase(charsetName)) {
            charset = StandardCharsets.UTF_8;
        } else if ("BIG5".equalsIgnoreCase(charsetName)) {
            charset = Charset.forName("Big5");
        } else {
        }
        BufferedReader reader = null;
        int lineCount = 0;

        try {
            // 驗證檔案是否存在以及是否可讀性
            if (!Files.exists(path) || !Files.isReadable(path)) {
                throw new IllegalArgumentException(
                        "File does not exist or does not differ：" + path);
            } else {
                reader = Files.newBufferedReader(path, charset);

                String line;

                while ((line = reader.readLine()) != null) {
                    lineCount++;

                    fileContents.add(line);
                }

                LogProcess.info(log, "1 source data count = {}", lineCount);
            }
        } catch (IOException e) {
            LogProcess.info(log, "2 source data count = {}", lineCount);
            LogProcess.error(log, "Error Message: file is problem = {}", e);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LogProcess.error(log, "Error Message: fail: close file = {}", e);

                }
            }
        }

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


    // byte[] -> HEX
    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    // Big5 ：抓第一個不合法位置（回傳 -1 代表看起來都合法或純 ASCII）
    public int firstIllegalBig5Index(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            int x = b[i] & 0xFF;
            if (x >= 0x81 && x <= 0xFE) {       // lead
                if (i + 1 >= b.length) return i;                // 尾端殘缺
                int y = b[i + 1] & 0xFF;
                boolean ok = (y >= 0x40 && y <= 0x7E) || (y >= 0xA1 && y <= 0xFE);
                if (!ok) return i;
                else i++;  // 合法雙位元組，跳過第二個
            } // 其他 ASCII 直接過
        }
        return -1;
    }

    //判斷： EBCDIC
    public boolean looksLikeEbcdic(byte[] b) {
        int d = 0, l = 0, so = 0;
        for (byte v : b) {
            int x = v & 0xFF;
            if (x >= 0xF0 && x <= 0xF9) d++;                  // '0'..'9'
            if (x == 0x0E || x == 0x0F) so++;                 // SO/SI
            if ((x >= 0xC1 && x <= 0xE9)) l++;                // A..Z/a..z
        }
        return (d + l) >= 4 || so > 0;
    }

    //    // 判斷： Big5/MS950 (lead/trail 合法性)
//    private boolean looksLikeBig5(byte[] b) {
//        int pairs = 0, bad = 0;
//        for (int i = 0; i < b.length; i++) {
//            int x = b[i] & 0xFF;
//            if (x >= 0x81 && x <= 0xFE) { // lead
//                if (i + 1 >= b.length) {
//                    bad++;
//                    break;
//                }
//                int y = b[++i] & 0xFF;
//                boolean trailOk = (y >= 0x40 && y <= 0x7E) || (y >= 0xA1 && y <= 0xFE);
//                if (trailOk) pairs++;
//                else bad++;
//            } else {
//                // ASCII 單位元組，略過
//            }
//        }
//        return pairs > 0 && bad == 0;
//    }
    public List<String> readFileContentWithHex(String filePath, String charsetName) {

        return readFileContentWithHex(filePath, charsetName, false);
    }


    //20251002版 讀檔案
    public List<String> readFileContentWithHex(String filePath, String charsetName, boolean strictMode) {
        String normalizedPath = FilenameUtils.normalize(filePath);
        Path path = Paths.get(normalizedPath);

        // 選字集：BIG5 一律用 MS950；其他照你的參數
        Charset cs;
        if (charsetName == null || "UTF-8".equalsIgnoreCase(charsetName)) {
            cs = StandardCharsets.UTF_8;
        } else if ("BIG5".equalsIgnoreCase(charsetName) || "MS950".equalsIgnoreCase(charsetName) || "CP950".equalsIgnoreCase(charsetName)) {
            cs = Charset.forName("MS950");
        } else if ("CP1047".equalsIgnoreCase(charsetName) || "CP037".equalsIgnoreCase(charsetName) || "CP500".equalsIgnoreCase(charsetName) || "CP937".equalsIgnoreCase(charsetName)) {
            cs = Charset.forName(charsetName); // EBCDIC （依實際來源決定）
        } else {
            cs = Charset.forName(charsetName);
        }

//        CharsetDecoder decoder = cs.newDecoder();
//        if (strictMode) {
//            decoder.onMalformedInput(CodingErrorAction.REPORT)
//                    .onUnmappableCharacter(CodingErrorAction.REPORT);
//        } else {
//            decoder.onMalformedInput(CodingErrorAction.REPLACE)
//                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
//                    .replaceWith("?"); // 需要固定欄寬可改成 "□" 或全形空白 "　"
//        }
        //置換使用
        CharsetDecoder decLenient = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith("?");
        //拋錯使用
        CharsetDecoder decStrict = cs.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        List<String> out = new ArrayList<>();
        int lineNo = 0;

        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path), 64 * 1024)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
            int prev = -1, c;

            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    byte[] bytes = buf.toByteArray();
                    if (prev == '\r' && bytes.length > 0) bytes = Arrays.copyOf(bytes, bytes.length - 1);
                    buf.reset();
                    lineNo++;

                    //放進 List
                    String s;
                    try {
                        s = decLenient.decode(ByteBuffer.wrap(bytes)).toString();
                    } finally {
                        decLenient.reset();
                    }
                    if (lineNo == 1 && !s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
                    out.add(s);

                    //若失敗或出現替代字，記錄錯誤
                    boolean bad = false;
                    try {
                        decStrict.decode(ByteBuffer.wrap(bytes));
                    } catch (CharacterCodingException ex) {
                        bad = true;
                    } finally {
                        decStrict.reset();
                    }

                    // Big5  & EBCDIC
                    int illegalAt = firstIllegalBig5Index(bytes);
                    boolean maybeEbcdic = looksLikeEbcdic(bytes);
                    boolean hasBadStrict = bad;
                    boolean hasReplacement = s.indexOf('\uFFFD') >= 0 || s.indexOf('?') >= 0;
                    boolean hasIllegalBig5 = illegalAt >= 0;
                    boolean hasEbcdicLike = maybeEbcdic;

                    // 只有其中任一條件成立才顯示
                    if (hasBadStrict || hasReplacement || hasIllegalBig5 || hasEbcdicLike) {
                        // 取 HEX 視窗（避免太長）
                        int start = Math.max(0, (illegalAt >= 0 ? illegalAt : 0) - 8);
                        int end = Math.min(bytes.length, start + 32);
                        String window = bytesToHex(Arrays.copyOfRange(bytes, start, end));

                        // 分別顯示原因，讓 log 更明確
                        if (hasBadStrict) {
                            LogProcess.warn(log, "[Line {}] ⚠️ 嚴格解碼失敗 (badStrict=true) HEX[..]={}", lineNo, window);
                        }
                        if (hasReplacement) {
                            LogProcess.warn(log, "[Line {}] ⚠️ 出現替代字 (� 或 ?) HEX[..]={}", lineNo, window);
                        }
                        if (hasIllegalBig5) {
                            LogProcess.warn(log, "[Line {}] ⚠️ 非法 Big5 位元組 (illegalAt={}) HEX[..]={}", lineNo, illegalAt, window);
                        }
                        if (hasEbcdicLike) {
//                            LogProcess.warn(log, "[Line {}] ⚠️ 疑似 EBCDIC 編碼 HEX[..]={}", lineNo, window);
                        }
                    }
                } else {
                    buf.write(c);
                }
                prev = c;
            }

            // 結尾最後一筆（沒有換行符）
            if (buf.size() > 0) {
                byte[] bytes = buf.toByteArray();
                lineNo++;

                String s;
                try {
                    s = decLenient.decode(ByteBuffer.wrap(bytes)).toString();
                } finally {
                    decLenient.reset();
                }
                if (lineNo == 1 && !s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
                out.add(s);

                boolean bad = false;
                try {
                    decStrict.decode(ByteBuffer.wrap(bytes));
                } catch (CharacterCodingException ex) {
                    bad = true;
                } finally {
                    decStrict.reset();
                }

                int illegalAt = firstIllegalBig5Index(bytes);
                boolean maybeEbcdic = looksLikeEbcdic(bytes);
                boolean hasBadStrict = bad;
                boolean hasReplacement = s.indexOf('\uFFFD') >= 0 || s.indexOf('?') >= 0;
                boolean hasIllegalBig5 = illegalAt >= 0;
                boolean hasEbcdicLike = maybeEbcdic;

                // 只有其中任一條件成立才顯示
                if (hasBadStrict || hasReplacement || hasIllegalBig5 || hasEbcdicLike) {
                    // 取 HEX 視窗（避免太長）
                    int start = Math.max(0, (illegalAt >= 0 ? illegalAt : 0) - 8);
                    int end = Math.min(bytes.length, start + 32);
                    String window = bytesToHex(Arrays.copyOfRange(bytes, start, end));

                    // 分別顯示原因，讓 log 更明確
                    if (hasBadStrict) {
                        LogProcess.warn(log, "[Line {}] ⚠️ 嚴格解碼失敗 (badStrict=true) HEX[..]={}", lineNo, window);
                    }
                    if (hasReplacement) {
                        LogProcess.warn(log, "[Line {}] ⚠️ 出現替代字 (� 或 ?) HEX[..]={}", lineNo, window);
                    }
                    if (hasIllegalBig5) {
                        LogProcess.warn(log, "[Line {}] ⚠️ 非法 Big5 位元組 (illegalAt={}) HEX[..]={}", lineNo, illegalAt, window);
                    }
                    if (hasEbcdicLike) {
//                        LogProcess.warn(log, "[Line {}] ⚠️ 疑似 EBCDIC 編碼 HEX[..]={}", lineNo, window);
                    }
                }
            }

//            LogProcess.info(log, "out out out = {}", out);

            LogProcess.info(log, "source data count = {}", lineNo);

        } catch (IOException e) {
            LogProcess.error(log, "I/O error after line {}: {}", lineNo, e.getMessage(), e);
        }

        return out;
    }

    /**
     * 複製或移動檔案(複製或移動)
     *
     * @param srcDir    來源路徑
     * @param dstDir    目的路徑
     * @param fileNames 指定處理的檔案名稱(空為全部檔案)
     * @param move      true:移動 / false:複製 <br>
     *                  移動指定資料夾內的檔案至目的資料夾<br>
     *                  transferFiles("C:/inbox", "C:/outbox", List.of("A.txt", "C.DAT"), true);<br>
     *                  複製資料夾全部的檔案至目的資料夾 <br>
     *                  transferFiles("C:/inbox", "C:/outbox",Collections.emptyList(), false); <br>
     */
    public void transferFiles(String srcDir, String dstDir, List<String> fileNames, boolean move)
            throws IOException {
        Path src = Paths.get(srcDir);
        Path dst = Paths.get(dstDir);
        Files.createDirectories(dst);

        Set<String> includes =
                (fileNames == null || fileNames.isEmpty())
                        ? null
                        : fileNames.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(src)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p)) continue;

                String name = p.getFileName().toString();

                // 若有指定清單：只處理清單內的檔案
                if (includes != null && !includes.contains(name)) continue;

                Path target = dst.resolve(name);

                if (move) {
                    try {
                        Files.move(
                                p,
                                target,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(p, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } else {
                    Files.copy(
                            p,
                            target,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    /**
     * 複製或移動「單一檔案」
     *
     * @param srcFile 完整來源檔案路徑（含檔名）
     * @param dstFile 完整目的檔案路徑（含檔名）
     * @param move    true=移動；false=複製
     * @throws IOException 當來源不存在、非檔案或 I/O 發生錯誤時
     */
    public  void transferFile(String srcFile, String dstFile, boolean move) throws IOException {
        Path src = Paths.get(srcFile);
        Path dst = Paths.get(dstFile);

        LogProcess.info(log, "src path = {} , dst path = {}",src,dst );


        // 來源檢查
        if (!Files.exists(src)) {
            throw new NoSuchFileException("來源檔案不存在: " + src.toAbsolutePath());
        }
        if (!Files.isRegularFile(src)) {
            throw new NotDirectoryException("來源不是一般檔案: " + src.toAbsolutePath());
        }

        // 自動建立目的資料夾（若存在則不動作）
        Path parent = dst.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        // 若來源與目的相同，直接略過
//        if (Files.isSameFile(src, dst)) {
//            return;
//        }

        if (move) {
            try {
                Files.move(src, dst,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            Files.copy(src, dst,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
        }
    }




}
