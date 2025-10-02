/* (C) 2024 */
package com.bot.mask.util.xml;

import com.bot.mask.log.LogProcess;
import com.bot.mask.util.cobol.CobolField;
import com.bot.mask.util.cobol.CobolFileProcessor;
import com.bot.mask.util.cobol.CobolProcessorFactory;
import com.bot.mask.util.cobol.CobolRecordDecoder;
import com.bot.mask.util.files.TextFileUtil;
import com.bot.mask.util.xml.vo.XmlData;
import com.bot.mask.util.xml.vo.XmlField;
import com.bot.mask.util.xml.vo.XmlHeaderBodyFooter;
import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@Scope("prototype")
public class XmlToFile {
    @Value("${localFile.mis.batch.output}")
    private String fileDir;

    @Value("${localFile.mis.xml.output.directory}")
    private String xmlOutputFileDir;

    @Value("${localFile.mis.xml.input.directory}")
    private String xmlInputFileDir;

    @Value("${localFile.mis.batch.input}")
    private String inputFileDir;


    @Autowired
    CobolRecordDecoder cobolRecordDecoder;
    @Autowired
    CobolProcessorFactory cobolProcessorFactory;

    @Autowired
    TextFileUtil textFileUtil;
    private static final String CHARSET_BIG5 = "BIG5";
    private final String CHARSET_UTF8 = "UTF-8";
    private final String STR_SEPARATOR = "separator";
    private final String INPUT_FILE_DETAIL = "D";
    private final String INPUT_FILE_HEADER = "H";
    private final String BOTTON_LINE = "_";
    private final String COMMA = ",";
    private final String SLASH = "/";

    private final int BATCH_LIMIT = 10000;
    private final String INPUT_XML_PATH = "external-config/xml/input/";
    private final String TXT_EXTENSION = ".txt";
    private final String XML_EXTENSION = ".xml";
    private final String NO_DATA = "no data";
    private final String OUTPUT_XML_PATH = "external-config/xml/output/";
    private String outputFileName = "";

    private boolean isFirstWrite = true;


    public void readCobolFileConvToTextFile(String fileName, XmlData xmlData) {
        try {
            isFirstWrite = true;

            // 取得XML定義好的欄位規格
            List<XmlField> xmlFieldHeaderList = safeGetFields(xmlData.getHeader());
            List<XmlField> xmlFieldBodyList = safeGetFields(xmlData.getBody());
            List<XmlField> xmlFieldFooterList = safeGetFields(xmlData.getFooter());

//            LogProcess.info(log,
//                    "read xmlFieldHeader = {}",
//                    xmlFieldHeaderList);
//            LogProcess.info(log,"read xmlFieldBody = {}", xmlFieldBodyList);
//            LogProcess.info(log,
//                    "read xmlFieldFooter = {}",
//                    xmlFieldFooterList);

            // TXT檔案路徑 讀取

            Path inputPath = Path.of(fileName);
            // 先建立父目錄
//            Files.createDirectories(inputPath.getParent());
            // 定義檔欄位組裝
            List<XmlField> allFieldList = new ArrayList<>();
            // 表頭
            allFieldList.addAll(xmlFieldHeaderList);
            // 內容
            allFieldList.addAll(xmlFieldBodyList);
            // 表尾
            allFieldList.addAll(xmlFieldFooterList);


            // CobolFileProcessor processor =cobolProcessorFactory.getProcessor(fileBytes);
            CobolFileProcessor processor = cobolProcessorFactory.getProcessor(allFieldList);

            String outputConvert = fileName.replace(TXT_EXTENSION, "") + ".Conv";

            ApLogHelper.info(
                    log, false, LogType.NORMAL.getCode(), "read input file = {}", inputPath);

            // 判斷檔案型態使用
            byte[] fileBytes = new byte[0];
            // 格式讀取錯誤或是沒
            try {
                fileBytes = Files.readAllBytes(inputPath);

            } catch (IOException e) {
                textFileUtil.createEmptyFileIfNotExist(outputConvert);
            }

            // 整批資料處理
            //            List<Map<String, String>> parsed = processor.parse(fileBytes, layout);
            // 分批資料處理
            handleDataToFile(
                    processor,
                    xmlFieldHeaderList,
                    xmlFieldBodyList,
                    xmlFieldFooterList,
                    fileBytes,
                    outputConvert);

        } catch (Exception e) {
            LogProcess.error(log,"readCobolFile error = {}",e);
        }

    }


//    /**
//     * 讀取Cobol來源檔案轉碼並輸出 <br>
//     * COBOL預設的編碼：EBCDIC + Binary (Packed Decimal)
//     *
//     * @param fileName  檔案名稱(XML與TXT同名)
//     * @param batchDate 批次日期
//     * @return List<Map < String, String>> 查询结果的列表
//     */
//    public void readCobolFileConvToTextFile(String fileName, int batchDate) {
//
//        LogProcess.info(log, "XmlToFile readCobolFileConvertToTextFile...");
//
//        List<Map<String, String>> tmpList = new ArrayList<>();
//        try {
//            // XML檔案路徑(讀取)
//            String xml = xmlInputFileDir + fileName;
//
//            batchDate = batchDate < 19110000 ? batchDate + 19110000 : batchDate;
//
//            // 解析XML檔案格式
//            XmlMapper xmlMapper = SecureXmlMapper.createXmlMapper();
//
//            // 路徑驗證
//            File validXmlPath = new File(INPUT_XML_PATH);
//
//            /* FORTIFY: The file path is securely controlled and validated */
//            File file = new File(xml);
//
//            if (!file.exists()) {
//                throw new FileNotFoundException("File not found: " + xml);
//            }
//
//            if (!file.getCanonicalPath().startsWith(validXmlPath.getCanonicalPath())) {
//                throw new SecurityException("Unauthorized path");
//            }
//
//            File confirmFile = new File(INPUT_XML_PATH + fileName);
//
//            XmlData xmlData = xmlMapper.readValue(confirmFile, XmlData.class);
//
//            // 取得XML定義好的欄位規格
//            List<XmlField> xmlFieldHeader = xmlData.getHeader().getFieldList();
//
//            // 取得XML定義好的欄位規格
//            List<XmlField> xmlFieldBody = xmlData.getBody().getFieldList();
//
//            // 取得XML定義好的欄位規格
//            List<XmlField> xmlFieldFooter = xmlData.getFooter().getFieldList();
//
//            LogProcess.info(log,
//                    "read xmlFieldHeader = {}",
//                    xmlFieldHeader);
//            LogProcess.info(log, "read xmlFieldBody = {}", xmlFieldBody);
//            LogProcess.info(log,
//                    "read xmlFieldFooter = {}",
//                    xmlFieldFooter);
//
//            // TXT檔案路徑 讀取
//            fileName = inputFileDir + batchDate + SLASH + fileName;
//
//            Path targetPath = Path.of(fileName);
//            // 先建立父目錄
//            Files.createDirectories(targetPath.getParent());
//            // 確認檔案路徑正確
//            fileName = FilenameUtils.normalize(fileName);
//            // 定義檔欄位組裝
//            List<XmlField> allFieldList = new ArrayList<>();
//            // 表頭
//            allFieldList.addAll(xmlFieldHeader);
//            // 內容
//            allFieldList.addAll(xmlFieldBody);
//            // 表尾
//            allFieldList.addAll(xmlFieldFooter);
//
//            // CobolFileProcessor processor =cobolProcessorFactory.getProcessor(fileBytes);
//            CobolFileProcessor processor = cobolProcessorFactory.getProcessor(allFieldList);
//
//            String outputConvert =
//                    inputFileDir + batchDate + SLASH + fileName + ".Conv";
//
//            LogProcess.info(log, "read input file = {}", fileName);
//            LogProcess.info(log, "read output file = {}", outputConvert);
//
//            byte[] fileBytes = new byte[0];
//            // 格式讀取錯誤或是沒
//            try {
//                fileBytes = Files.readAllBytes(targetPath);
//            } catch (IOException e) {
//
//            }
//
//            // 整批資料處理
//            //            List<Map<String, String>> parsed = processor.parse(fileBytes, layout);
//            // 分批資料處理
//            handleDataToFile(
//                    processor, xmlFieldHeader, xmlFieldBody, xmlFieldFooter, fileBytes, outputConvert);
//
//        } catch (Exception e) {
//            LogProcess.error(log,"readreadCobolFile error");
//        }
////        return tmpList;
//    }

    /**
     * 處理資料 寫入檔案
     */
    private void handleDataToFile(
            CobolFileProcessor processor,
            List<XmlField> xmlFieldHeader,
            List<XmlField> xmlFieldBody,
            List<XmlField> xmlFieldFooter,
            byte[] fileBytes,
            String outputConvertFile) {


        // 先根據Cobol欄位型態組裝
        List<CobolField> layoutHeader = convertXmlToCobolFields(xmlFieldHeader);
        List<CobolField> layoutBody = convertXmlToCobolFields(xmlFieldBody);
        List<CobolField> layoutFooter = convertXmlToCobolFields(xmlFieldFooter);

        // 分批資料處理
        List<Map<String, String>> parsedBatch = new ArrayList<>();

        EncodingType type = detectEncodingType(fileBytes);
        LogProcess.info(log,"type = {}", type);

        boolean useMs950Handle = false;
        if (type == EncodingType.MS950) {
            useMs950Handle = true;
        } else if (type == EncodingType.UTF8) {
            useMs950Handle = false;
        } else if (type == EncodingType.EBCDIC) {
            useMs950Handle = true;
        } else {
            useMs950Handle = false;
        }


        processor.parseCobolWithOptionalHeaderFooter(
                fileBytes,
                layoutHeader,
                layoutBody,
                layoutFooter,
                useMs950Handle,
                headRow -> {
//                    ApLogHelper.info(log,log, false, LogType.NORMAL.getCode(), "表頭: {}", headRow);
                    if (!headRow.isEmpty()) {
                        parsedBatch.add(headRow);
                        writeFileCobolToText(parsedBatch, outputConvertFile, true);
                        parsedBatch.clear();
                    }

                },
                detailRow -> {
//                    ApLogHelper.info(log,log, false, LogType.NORMAL.getCode(), "表身: {}", detailRow);

                    parsedBatch.add(detailRow);
                    if (parsedBatch.size() >= BATCH_LIMIT) {
                        writeFileCobolToText(parsedBatch, outputConvertFile, true);
                        parsedBatch.clear();
                    }

                    if (!parsedBatch.isEmpty()) {
                        writeFileCobolToText(parsedBatch, outputConvertFile, true);
                        parsedBatch.clear();
                    }
                },
                tailRow -> {
//                    ApLogHelper.info(log,log, false, LogType.NORMAL.getCode(), "表尾: {}", tailRow);
                    if (tailRow.size() > 0) {
                        parsedBatch.add(tailRow);
                        writeFileCobolToText(parsedBatch, outputConvertFile, true);
                        parsedBatch.clear();
                    }

                }
                ,
                1,
                1);

        // 檢查剩下的資料還沒寫
        if (!parsedBatch.isEmpty()) {
            writeFileCobolToText(parsedBatch, outputConvertFile, true);
        }
    }

    private List<XmlField> safeGetFields(XmlHeaderBodyFooter section) {
        return section != null && section.getFieldList() != null
                ? section.getFieldList()
                : Collections.emptyList();
    }

    /**
     * 組裝定義檔欄位
     */
    private List<CobolField> convertXmlToCobolFields(List<XmlField> xmlFields) {

        if (xmlFields.isEmpty()) {
            return new ArrayList<>(); // 防呆：沒有任何欄位時直接回傳 null
        }

        return xmlFields.stream()
                .map(f -> new CobolField(
                        f.getFieldName(),
                        CobolField.Type.valueOf(f.getFieldType()),
                        Double.parseDouble(f.getLength()),
                        Integer.parseInt(f.getDecimal())))
                .collect(Collectors.toList());
    }


    /**
     * COBOL轉換後寫入文字檔，整批寫檔。
     *
     * @param parsed     資料清單
     * @param outputPath 輸出檔案路徑
     */
    public void writeFileCobolToText(List<Map<String, String>> parsed, String outputPath) {
        writeFileCobolToText(parsed, outputPath, false);
    }


    /**
     * COBOL 轉換後寫入文字檔，整批或分批寫檔。
     *
     * @param parsed     資料清單
     * @param outputPath 輸出檔案路徑
     * @param append     （true = 分批寫入；false = 覆蓋整批）
     */
    public void writeFileCobolToText(
            List<Map<String, String>> parsed, String outputPath, boolean append) {


        Path path = Paths.get(outputPath);

        // 建立父資料夾（如果不存在）
        if (!Files.exists(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                LogProcess.info(log,"資料夾建立失敗");
            }
        }

        // 第一次寫入就刪檔（不管有沒有）
        if (isFirstWrite) {
            File file = new File(outputPath);
            if (file.exists()) {
                file.delete();
            }
            isFirstWrite = false;
        }

        //        LogProcess.info(log, "parsed：" + parsed);
        try (BufferedWriter writer =
                     new BufferedWriter(
                             new OutputStreamWriter(
                                     new FileOutputStream(outputPath, append),
                                     Charset.forName(CHARSET_BIG5)))) {

            for (Map<String, String> row : parsed) {

                String line = String.join(",", row.values());
                writer.write(line);
                writer.newLine();
            }

        } catch (IOException e) {
            LogProcess.info(log,"寫入失敗");
        }
    }


    /**
     * 判斷是否為全行字或中文字
     */
    public boolean isFullWidth(char ch) {
        return (ch >= 0xFF00 && ch <= 0xFFEF)
                || // Fullwidth and Halfwidth Forms
                (ch >= 0x4E00 && ch <= 0x9FFF); // CJK Unified Ideographs
    }

    /**
     * 將有 yyyymmdd 或 [yyyymmdd] 格式的檔案名稱 換成日期<br>
     * ex: myfile-[yyyymmdd].txt => myfile-20250101.txt<br>
     * ex: myfile2-yyyymmdd.txt => myfile2-20250101.txt<br>
     *
     * @param batchDate 批次日期
     */
    public String replaceDatePatternIfExists(String pattern, String batchDate) {
        // 找 [yyyymmdd] 或 yyyymmdd
        Pattern datePattern = Pattern.compile("\\[?yyyymmdd\\]?");
        Matcher matcher = datePattern.matcher(pattern);

        if (matcher.find()) {
            // 有找到就替換
            return matcher.replaceAll(batchDate);
        } else {
            // 沒找到就返回原本的名稱
            return pattern;
        }
    }

    /**
     * 拷貝檔案
     *
     * @param source    已存在的檔案
     * @param target    輸出檔案名稱 <br>
     *                  StandardCopyOption.REPLACE_EXISTING 檔案已存在就覆蓋 <br>
     *                  StandardCopyOption.COPY_ATTRIBUTES 連同檔案屬性（例如最後修改時間）一起複製
     * @param batchDate 批次日期
     */
    public boolean copyFile(String source, String target, int batchDate) {

        batchDate = batchDate < 19110000 ? batchDate + 19110000 : batchDate;

        String sourceFile = fileDir + batchDate + SLASH + source;
        String targetFile =
                replaceDatePatternIfExists(
                        fileDir + batchDate + SLASH + target,
                        String.valueOf(batchDate));

        try {
            Path targetPath = Path.of(targetFile);
            Files.createDirectories(targetPath.getParent()); // 先建立父目錄
            Files.copy(Path.of(sourceFile), targetPath, StandardCopyOption.REPLACE_EXISTING);

            LogProcess.info(log,
                    "copy file : sourceFile = {},targetFile = {}",
                    sourceFile,
                    targetFile);
            return true;
        } catch (IOException e) {
            LogProcess.info(log,"copy file fail = {}", targetFile);
            return false;
        }
    }

    // 這段以下可以考慮調整成工具----------------------------------------------------------

    public enum EncodingType {
        MS950,
        UTF8,
        EBCDIC,
        UNKNOWN
    }

    public EncodingType detectEncodingType(byte[] data) {
        if (data == null || data.length == 0) return EncodingType.UNKNOWN;

        int utf8Score = 0;
        int ms950Score = 0;
        int ebcdicScore = 0;

        for (int i = 0; i < data.length; i++) {
            int b = data[i] & 0xFF;

            // ========== EBCDIC 判斷 ==========
            if ((b >= 0xF0 && b <= 0xF9)
                    || // 數字
                    (b >= 0xC1 && b <= 0xE9)
                    || // A~Z
                    b == 0x40
                    || b == 0x15
                    || b == 0x25) { // 空白或控制字元
                ebcdicScore++;
            }

            // ========== MS950 判斷 ==========
            if (b >= 0x81 && b <= 0xFE && i + 1 < data.length) {
                int b2 = data[i + 1] & 0xFF;
                if (b2 >= 0x40 && b2 <= 0xFE && b2 != 0x7F) {
                    ms950Score++;
                    i++; // Skip next byte (雙位元組)
                    continue;
                }
            }

            // ========== UTF-8 判斷 ==========
            if ((b & 0b10000000) == 0) {
                utf8Score++; // ASCII
            } else if ((b & 0b11100000) == 0b11000000
                    && i + 1 < data.length
                    && (data[i + 1] & 0b11000000) == 0b10000000) {
                utf8Score++;
                i++;
            } else if ((b & 0b11110000) == 0b11100000
                    && i + 2 < data.length
                    && (data[i + 1] & 0b11000000) == 0b10000000
                    && (data[i + 2] & 0b11000000) == 0b10000000) {
                utf8Score++;
                i += 2;
            }
        }

        // ========== 根據分數回傳 ==========
        if (ebcdicScore > utf8Score && ebcdicScore > ms950Score) return EncodingType.EBCDIC;
        if (ms950Score > utf8Score && ms950Score > ebcdicScore) return EncodingType.MS950;
        if (utf8Score > ms950Score && utf8Score > ebcdicScore) return EncodingType.UTF8;

        return EncodingType.UNKNOWN;
    }

}
