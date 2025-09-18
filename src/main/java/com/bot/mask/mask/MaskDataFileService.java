package com.bot.mask.mask;


import com.bot.mask.filter.CheakSafePathUtil;
import com.bot.mask.log.LogProcess;
import com.bot.mask.util.files.FileNameUtil;
import com.bot.mask.util.files.PathUtil;
import com.bot.mask.util.files.TextFileUtil;
import com.bot.mask.util.xml.XmlToFile;
import com.bot.mask.util.xml.mask.DataMasker;
import com.bot.mask.util.xml.mask.XmlParser;
import com.bot.mask.util.xml.vo.XmlData;
import com.bot.mask.util.xml.vo.XmlField;
import com.bot.mask.util.xml.vo.XmlFile;
import com.bot.txcontrol.util.parse.Parse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class MaskDataFileService {
    @Value("${localFile.mis.batch.input}")
    private String inputPath;

    @Value("${localFile.mis.batch.output}")
    private String outputPath;

    @Value("${localFile.mis.xml.output.def}")
    private String botMaskXmlFilePath;
    @Autowired
    private XmlParser xmlParser;
    @Autowired
    private Parse parse;
    @Autowired
    private TextFileUtil textFileUtil;

    @Autowired
    private FileNameUtil fileNameUtil;

    @Autowired
    private PathUtil pathUtil;
    @Autowired
    private DataMasker dataMasker;
    @Autowired
    private XmlToFile xmlToFile;
    private final String TXT_EXTENSION = ".txt";
    private static final String CHARSET_BIG5 = "Big5";
    private static final String CHARSET_UTF8 = "UTF-8";

    public boolean exec() {
        LogProcess.info(log,"執行資料檔案遮蔽處理...");
        //允許的路徑
        String tbotOutputPath = FilenameUtils.normalize(inputPath);
        String tbotMaskXmlFilePath = FilenameUtils.normalize(botMaskXmlFilePath);

        XmlFile xmlFile;
        List<XmlData> xmlDataList = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        try {
            LogProcess.info(log,"output def file = " + tbotMaskXmlFilePath);
            xmlFile = xmlParser.parseXmlFile2(tbotMaskXmlFilePath);

            xmlDataList = xmlFile.getDataList();
//            LogProcess.info(log,"xmlDataList = " + xmlDataList);

            fileNames = xmlDataList.stream()
                    .map(XmlData::getFileName)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            LogProcess.info(log,"xml 讀取問題");

        }
        LogProcess.info(log,"讀取 external-config/xml/bot_output 資料夾下的 DailyBatchFileDefinition.xml 定義檔內有" + xmlDataList.size() + "組 <data> 格式，檔名清單如下...");
        LogProcess.info(log,fileNames.toString());
        int calcuTotal = 0;
        //台銀原檔案路徑
        List<String> folderList = getFilePaths(tbotOutputPath);
        LogProcess.info(log,"在batch-file/input資料夾內的檔案有" + folderList.size() + "個，清單如下...");
        LogProcess.info(log,folderList.toString());
        for (String inputFilePath : folderList) {
            //允許路徑，若有txt去除
            inputFilePath = FilenameUtils.normalize(inputFilePath.replace(TXT_EXTENSION, ""));

            Path inputFilePathTmp = Paths.get(inputFilePath);
            String inputFileName = textFileUtil.replaceDateWithPlaceholder(inputFilePathTmp.getFileName().toString());

            try {
                for (XmlData xmlData : xmlDataList) {
                    String xmlFileName = xmlData.getFileName();

                    if (fileNameUtil.isFileNameMatch(inputFileName, xmlFileName)) {

                        LogProcess.info(log,"inputFilePath = " + inputFilePath);
                        //設好要輸出的路徑
                        String outputFilePath = inputFilePath.replace("input", "output_mask_datafile");

                        //Fas 檔案轉檔處理，只有中間檔案 會有 conv
                        if (fileNameUtil.isFasFile(inputFilePath)) {


                            //XML 有Conv 開頭的表示資料轉檔(一次性用) 需特殊處理
                            if (xmlFileName.contains("Conv")) {
                                //Conv 輸入及輸出的檔名就不用動
                                outputFilePath = inputFilePath.replace("input", "output_mask_datafile");
                            } else {
                                xmlToFile.readCobolFileConvToTextFile(inputFilePath, xmlData);
                                //這是給轉檔後要讀檔及寫檔的路徑
                                inputFilePath = inputFilePath + ".Conv";
                                outputFilePath = outputFilePath + ".Conv";
                            }

                        } else if (inputFileName.toLowerCase().contains("faslnclfl")) {
                            //這是給轉檔後要讀檔及寫檔的路徑
                            inputFilePath = inputFilePath;
                            outputFilePath = outputFilePath + ".Conv";
                        }
                        List<String> outputData = new ArrayList<>();

                        //特殊處理的中間檔
                        if (inputFileName.toLowerCase().contains("faslnclfl")) {
                            LogProcess.info(log,"performMasking2 inputFilePath = " + inputFilePath);
                            outputData = performMasking2(inputFilePath, xmlData);

                            //處理中間檔
                        } else if (inputFileName.toLowerCase().startsWith("fas") || inputFileName.toLowerCase().startsWith("misbh_fas")) {
                            //XML 有Conv 開頭的表示資料轉檔(一次性用) 需特殊處理
                            if (xmlFileName.contains("Conv")) {
                                LogProcess.info(log,"performMasking4 inputFilePath = " + inputFilePath);
                                outputData = performMasking4(inputFilePath, xmlData);
                            } else {

                                LogProcess.info(log,"performMasking3 inputFilePath = " + inputFilePath);
                                outputData = performMasking3(inputFilePath, xmlData);
                            }
                            //處理外送檔
                        } else {
                            LogProcess.info(log,"performMasking inputFilePath = " + inputFilePath);

                            outputData = performMasking(inputFilePath, xmlData);

                            outputFilePath = outputFilePath.replace("Misbh_", "");
                        }

                        //確認允許路徑
                        outputFilePath = FilenameUtils.normalize(outputFilePath);

                        //刪除原檔案
                        textFileUtil.deleteFile(outputFilePath);

                        //輸出檔案
                        textFileUtil.writeFileContent(outputFilePath, outputData, CHARSET_BIG5);

                        LogProcess.info(log,"output file path = " + outputFilePath);

                        calcuTotal++;
                    }
                }

            } catch (Exception e) {
                LogProcess.info(log,"XmlToInsertGenerator.sqlConvInsertTxt error");
                LogProcess.info(log,"error=" + e.getMessage());
            }
        }

        LogProcess.info(log,"產出遮蔽後的檔案在 batch-file/output_mask_datafile 資料夾,有" + calcuTotal + "個檔案");

        return true;
    }

    /**
     * 取得指定資料夾內的所有檔案名稱
     *
     * @param folderPath 資料夾路徑
     * @return List<String> 回傳資料夾清單
     */
    private List<String> getFilePaths(String folderPath) {
        List<String> sqlFilePaths = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(folderPath), 1)) { // 只讀取第一層檔案
            sqlFilePaths = paths
                    .filter(Files::isRegularFile)
                    .map(path -> FilenameUtils.normalize(path.toString()))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            LogProcess.info(log,"Error reading files");
            LogProcess.info(log,"error=" + e.getMessage());
        }
        return sqlFilePaths;
    }

    /**
     * 執行遮蔽資料處理(蔽用於遮蔽程式使用) 分區處理
     *
     * @param fileName 讀取txt檔案(含路徑)
     * @param xmlData  定義檔內容
     * @return List<String> 輸出內容
     */

    private int headerCnt = 0;
    private int footerCnt = 0;

    public List<String> performMasking3(String fileName, XmlData xmlData) {

        //輸出資料
        List<String> outputData = new ArrayList<>();
        try {

            String allowedPath = FilenameUtils.normalize(inputPath);
            List<XmlField> xmlFieldListH = new ArrayList<>();
            List<XmlField> xmlFieldListB = new ArrayList<>();
            List<XmlField> xmlFieldListF = new ArrayList<>();
            xmlFieldListH = xmlData.getHeader().getFieldList();
            xmlFieldListB = xmlData.getBody().getFieldList();
            xmlFieldListF = xmlData.getFooter().getFieldList();
//            LogProcess.info(log,"lines xmlFieldListH  = " + xmlFieldListH.size());
//            LogProcess.info(log,"lines xmlFieldListB  = " + xmlFieldListB.size());
//            LogProcess.info(log,"lines xmlFieldListF  = " + xmlFieldListF.size());
//            LogProcess.info(log,"fileName fileName = " + fileName);


            // 確認檔案路徑 是否與 允許的路徑匹配
            if (CheakSafePathUtil.isSafeFilePath(allowedPath, fileName)) {
                // 讀取檔案內容
                List<String> lines = textFileUtil.readFileContent(fileName, CHARSET_BIG5);
                int index = 0;
//                LogProcess.info(log,"lines size  = " + lines.size());


                for (String s : lines) {
                    index++;


                    // header處理...
                    if (!xmlFieldListH.isEmpty() && index == 1) {
                        outputData.add(processFieldCobol(xmlFieldListH, s));
                        continue;
                    }

                    // body處理...
                    if (!xmlFieldListB.isEmpty() && lines.size() - footerCnt >= index) {
                        outputData.add(processFieldCobol(xmlFieldListB, s));
                    }
                    // footer處理...

                    if (!xmlFieldListF.isEmpty() && lines.size() == index) {
                        outputData.add(processFieldCobol(xmlFieldListF, s));
                    }

                }
                LogProcess.info(log,"result after masking count = " + index);
            } else {
                LogProcess.info(log,"not allowed to read  = " + fileName);
            }

        } catch (Exception e) {
            LogProcess.info(log,"XmlToReadFile.exec error");
            LogProcess.info(log,"error=" + e.getMessage());
        }

        return outputData;
    }


    public List<String> performMasking(String fileName, XmlData xmlData) {

        List<String> result = new ArrayList<>();
//        LogProcess.info(log,"fileName = " + fileName);
//        LogProcess.info(log,"xmlData = " + xmlData);
        int header = 0;
        int footer = 0;
        try {
            // 解析XML檔案格式

            // header處理...
            List<XmlField> xmlFieldListH = xmlData.getHeader().getFieldList();
            if (!xmlFieldListH.isEmpty()) {
                header = 1;
            }
            // footer處理...
            List<XmlField> xmlFieldListF = xmlData.getFooter().getFieldList();
            if (!xmlFieldListF.isEmpty()) {
                footer = 1;
            }

            // body處理...
            List<XmlField> xmlFieldListB = xmlData.getBody().getFieldList();
            if (!xmlFieldListB.isEmpty()) {
                result.addAll(processFileData(fileName, xmlFieldListH, xmlFieldListB, xmlFieldListF, header, footer));
            }


            LogProcess.info(log,"result after masking count = " + result.size());

        } catch (Exception e) {
            LogProcess.info(log,"XmlToReadFile.exec error");
            LogProcess.info(log,"error=" + e.getMessage());
        }

        return result;
    }

    /**
     * 個案處理
     * 執行遮蔽資料處理(蔽用於遮蔽程式使用) 根據資料串處理
     *
     * @param fileName 讀取txt檔案(含路徑)
     * @param xmlData  定義檔內容
     * @return List<String> 輸出內容
     */

    public List<String> performMasking2(String fileName, XmlData xmlData) {

        List<XmlField> xmlFieldHeaderList = xmlData.getHeader().getFieldList();
        List<XmlField> xmlFieldBodyList = xmlData.getBody().getFieldList();
        List<XmlField> xmlFieldFooterList = xmlData.getFooter().getFieldList();

        List<String> result = new ArrayList<>();

        String allowedPath = FilenameUtils.normalize(inputPath);

        // 確認檔案路徑 是否與 允許的路徑匹配
        if (CheakSafePathUtil.isSafeFilePath(allowedPath, fileName)) {
            // 讀取檔案內容
            List<String> lines = textFileUtil.readFileContent(fileName, CHARSET_BIG5);

            String firstChar = "";
            int index = 0;
            for (String s : lines) {
                index++;
                firstChar = s.substring(0, 1);
                try {
                    LogProcess.debug(log,"fileName:{},line data = {}", fileName, s);
                    switch (firstChar) {
                        case "0":
                            result.add(processField2(xmlFieldHeaderList, s));
                            break;
                        case "1":
                            result.add(processField2(xmlFieldBodyList, s));
                            break;
                        case "2":
                            result.add(processField2(xmlFieldFooterList, s));
                            break;
                    }

                } catch (Exception e) {
                    LogProcess.info(log,"XmlToReadFile.exec error");
                    LogProcess.error(log,"error = {}" + e);
                }

            }
            LogProcess.info(log,"result after masking count = " + index);
        } else {
            LogProcess.info(log,"not allowed to read  = " + fileName);
        }


        return result;
    }


    public List<String> performMasking4(String fileName, XmlData xmlData) {

        List<String> result = new ArrayList<>();
        int header = 0;
        int footer = 0;
        try {
            // 解析XML檔案格式

            // header處理...
            List<XmlField> xmlFieldListH = xmlData.getHeader().getFieldList();
            if (!xmlFieldListH.isEmpty()) {
                header = 1;
            }
            // footer處理...
            List<XmlField> xmlFieldListF = xmlData.getFooter().getFieldList();
            if (!xmlFieldListF.isEmpty()) {
                footer = 1;
            }

            // body處理...
            List<XmlField> xmlFieldListB = xmlData.getBody().getFieldList();
            if (!xmlFieldListB.isEmpty()) {
                result.addAll(processFileData2(fileName, xmlFieldListH, xmlFieldListB, xmlFieldListF, header, footer));
            }


            LogProcess.info(log,"result after masking count = " + result.size());

        } catch (Exception e) {
            LogProcess.info(log,"XmlToReadFile.exec error");
            LogProcess.info(log,"error=" + e.getMessage());
        }

        return result;
    }


    /**
     * 匹配單筆資料定義檔欄位，並將資料做遮蔽處理
     *
     * @param xmlFieldList 定義檔欄位
     * @param line         單筆資料串
     * @return 回傳遮罩後的資料
     */
    private String processField(List<XmlField> xmlFieldList, String line) {
        Charset charset = Charset.forName("Big5");
        Charset charset2 = Charset.forName("UTF-8");

        int xmlLength = 0;
        for (XmlField xmlField : xmlFieldList) {
//            LogProcess.info(log,"xmlLength = " + xmlField.getLength() +" , fieldName = " + xmlField.getFieldName());
            xmlLength = xmlLength + parse.string2Integer(xmlField.getLength());
        }
        byte[] bytes = line.getBytes(charset);
        int dataLength = bytes.length;


        //先比對檔案資料長度是否與定義檔加總一致
//        if (xmlLength != dataLength) {
//            LogProcess.info(log,"xml length = " + xmlLength + " VS data length = " + dataLength);
//            return line;
//        }
        //起始位置
        int sPos = 0;
        StringBuilder s = new StringBuilder();

        //XML定義檔的格式
        for (XmlField xmlField : xmlFieldList) {


            //取得定義黨內的 欄位名稱、長度、遮蔽代號
            String fieledName = xmlField.getFieldName();
            int length = parse.string2Integer(xmlField.getLength());
            String maskType = xmlField.getMaskType();


            // 取得可使用的 substring 長度結尾
            String remaining = line.substring(sPos);
            int safeCut = getSafeSubstringLength(remaining, length, charset);

            // 切出這個欄位字串
            String value = remaining.substring(0, safeCut);

            // 更新 char index 位置（要考慮已用掉的實際字元數）
            sPos += safeCut;

            //略過分隔符號
            if ("separator".equals(fieledName)) {
                s.append(value);
                continue;
            }

            //判斷有無遮蔽欄位
            if (!Objects.isNull(maskType)) {
                //進行遮蔽處理
                String valueMask = cleanAndRestore(value, v -> {
                    try {
                        return dataMasker.applyMask(v, maskType);
                    } catch (IOException e) {
                        LogProcess.error(log,"error = {}" + e);
                    }
                    return v;
                });
                s.append(valueMask);
            } else {
                s.append(value);
            }
        }
        return s.toString();
    }


    /**
     * 匹配單筆資料定義檔欄位，並將資料做遮蔽處理
     * 針對個案 FasLnClfl 檔案處理
     *
     * @param xmlFieldList 定義檔欄位
     * @param line         單筆資料串
     * @return 回傳遮罩後的資料
     */
    private String processField2(List<XmlField> xmlFieldList, String line) {
        Charset charset = Charset.forName("Big5");
        Charset charset2 = Charset.forName("UTF-8");

        int xmlLength = 0;
        for (XmlField xmlField : xmlFieldList) {
//            LogProcess.info(log,"xmlLength = " + xmlField.getLength() +" , fieldName = " + xmlField.getFieldName());
            xmlLength = xmlLength + parse.string2Integer(xmlField.getLength());
        }
        byte[] bytes = line.getBytes(charset);
        int dataLength = bytes.length;


        //先比對檔案資料長度是否與定義檔加總一致
//        if (xmlLength != dataLength) {
//            LogProcess.info(log,"xml length = " + xmlLength + " VS data length = " + dataLength);
//            return line;
//        }
        //起始位置
        int sPos = 0;
        StringBuilder s = new StringBuilder();

        int idx = 0;

        //XML定義檔的格式
        for (XmlField xmlField : xmlFieldList) {
            idx++;

            //取得定義黨內的 欄位名稱、長度、遮蔽代號
            String fieledName = xmlField.getFieldName();
            int length = parse.string2Integer(xmlField.getLength());
            String maskType = xmlField.getMaskType();


            // 取得可使用的 substring 長度結尾
            String remaining = line.substring(sPos);
            int safeCut = getSafeSubstringLength(remaining, length, charset);

            // 切出這個欄位字串
            String value = remaining.substring(0, safeCut);


            // 更新 char index 位置（要考慮已用掉的實際字元數）
            sPos += safeCut;

            //略過分隔符號
            if ("separator".equals(fieledName)) {
                s.append(value);
                continue;
            }

            //判斷有無遮蔽欄位
            if (!Objects.isNull(maskType)) {
                //進行遮蔽處理
                String valueMask = cleanAndRestore(value, v -> {
                    try {
                        return dataMasker.applyMask(v, maskType);
                    } catch (IOException e) {
                        LogProcess.error(log,"error = {}", e);
                    }
                    return v;
                });
                s.append(valueMask);
            } else {
                s.append(value);
            }

            if (idx < xmlFieldList.size()) {
                s.append(",");
            }

        }
        return s.toString();
    }


    /**
     * 轉檔檔案處理(用逗號間隔)
     *
     * @param xmlFieldList 定義檔欄位
     * @param line         單筆資料串
     * @return 回傳遮罩後的資料
     */
    private String processField3(List<XmlField> xmlFieldList, String line) {
        Charset charset = Charset.forName("Big5");
        Charset charset2 = Charset.forName("UTF-8");

        int xmlColumnCnt = 0;
        for (XmlField xmlField : xmlFieldList) {
            xmlColumnCnt = xmlColumnCnt + 1;
        }

        String[] sLine = line.split(",");
        int dataLength = sLine.length;
        //先比對檔案資料長度是否與定義檔加總一致
        if (xmlColumnCnt != dataLength) {
            LogProcess.debug(log,"xml length = " + xmlColumnCnt + " VS data length = " + dataLength);
            return line;
        }
        //起始位置
        StringBuilder s = new StringBuilder();

        int i = 0;
        //XML定義檔的格式
        for (int idx = 0; idx < xmlFieldList.size(); idx++) {
            XmlField xmlField = xmlFieldList.get(idx);

            String maskType = xmlField.getMaskType();
            String value = sLine[i];
            i++;

            if (maskType != null) {
                String valueMask = cleanAndRestore(value, v -> {
                    try {
                        return dataMasker.applyMask(v, maskType);
                    } catch (IOException e) {
                        LogProcess.error(log,"error = {}", e);
                    }
                    return v;
                });
                s.append(valueMask);
            } else {
                s.append(value);
            }

            if (idx < xmlFieldList.size() - 1) {
                s.append(",");
            }
        }


        return s.toString();
    }

    /**
     * 匹配單筆資料定義檔欄位，並將資料做遮蔽處理
     *
     * @param xmlFieldList 定義檔欄位
     * @param line         單筆資料串
     * @return 回傳遮罩後的資料
     */
    private String processFieldCobol(List<XmlField> xmlFieldList, String line) {
        Charset charset = Charset.forName("Big5");
        Charset charset2 = Charset.forName("UTF-8");

        int xmlColumnCnt = 0;
        for (XmlField xmlField : xmlFieldList) {
            xmlColumnCnt = xmlColumnCnt + 1;
        }
//        byte[] bytes = line.getBytes(charset);

        String[] sLine = line.split(",");
        int dataLength = sLine.length;
        //先比對檔案資料長度是否與定義檔加總一致
        if (xmlColumnCnt != dataLength) {
            LogProcess.debug(log,"xml length = " + xmlColumnCnt + " VS data length = " + dataLength);
            return line;
        }
        //起始位置
        StringBuilder s = new StringBuilder();

        int i = 0;
        //XML定義檔的格式
        for (int idx = 0; idx < xmlFieldList.size(); idx++) {
            XmlField xmlField = xmlFieldList.get(idx);

            String maskType = xmlField.getMaskType();
            String value = sLine[i];
            i++;

            if (maskType != null) {
                String valueMask = cleanAndRestore(value, v -> {
                    try {
                        return dataMasker.applyMask(v, maskType);
                    } catch (IOException e) {
                        LogProcess.error(log,"error = {}", e);
                    }
                    return v;
                });
                s.append(valueMask);
            } else {
                s.append(value);
            }


            if (idx < xmlFieldList.size() - 1) {
                s.append(",");
            }
        }


        return s.toString();
    }


    /**
     * 匹配單筆資料定義檔欄位，並將資料做遮蔽處理
     *
     * @param fileName      檔案名稱(用於確認路徑)
     * @param xmlFieldListH 定義檔欄位(表頭)
     * @param xmlFieldListB 定義檔欄位(內容)
     * @param xmlFieldListF 定義檔欄位(表尾)
     * @return 回傳遮罩後的資料
     */
    private List<String> processFileData(String fileName, List<XmlField> xmlFieldListH, List<XmlField> xmlFieldListB, List<XmlField> xmlFieldListF, int headerCnt, int footerCnt) {

        //輸出資料
        List<String> outputData = new ArrayList<>();

        String allowedPath = FilenameUtils.normalize(inputPath);

        // 確認檔案路徑 是否與 允許的路徑匹配
        if (CheakSafePathUtil.isSafeFilePath(allowedPath, fileName)) {
            // 讀取檔案內容
            List<String> lines = textFileUtil.readFileContent(fileName, CHARSET_BIG5);
            int index = 0;
            for (String s : lines) {
                index++;
                if (headerCnt == 1 && headerCnt == index) {
                    outputData.add(processField(xmlFieldListH, s));
                    continue;
                }
                //總筆數減去表尾 要大於等於 計算中筆數
                if (lines.size() - footerCnt >= index) {
                    outputData.add(processField(xmlFieldListB, s));
                }

                if (footerCnt == 1 && lines.size() == index) {
                    outputData.add(processField(xmlFieldListF, s));
                }


            }
        } else {
            LogProcess.info(log,"not allowed to read  = {}", fileName);
        }

        return outputData;

    }



    /**
     * 特殊處理 只處理資料轉檔的檔案(一次性)
     *
     * @param fileName      檔案名稱(用於確認路徑)
     * @param xmlFieldListH 定義檔欄位(表頭)
     * @param xmlFieldListB 定義檔欄位(內容)
     * @param xmlFieldListF 定義檔欄位(表尾)
     * @return 回傳遮罩後的資料
     */
    private List<String> processFileData2(String fileName, List<XmlField> xmlFieldListH, List<XmlField> xmlFieldListB, List<XmlField> xmlFieldListF, int headerCnt, int footerCnt) {
        LogProcess.info(log,"processFileData2 ... (Conv)");
        //輸出資料
        List<String> outputData = new ArrayList<>();

        String allowedPath = FilenameUtils.normalize(inputPath);

        // 確認檔案路徑 是否與 允許的路徑匹配
        if (CheakSafePathUtil.isSafeFilePath(allowedPath, fileName)) {
            // 讀取檔案內容
            List<String> lines = textFileUtil.readFileContent(fileName, CHARSET_BIG5);

            int index = 0;
            for (String s : lines) {
                index++;
                if (headerCnt == 1 && headerCnt == index) {
                    outputData.add(processField3(xmlFieldListH, s));
                    continue;
                }
                //總筆數減去表尾 要大於等於 計算中筆數
                if (lines.size() - footerCnt >= index) {
                    outputData.add(processField3(xmlFieldListB, s));
                }

                if (footerCnt == 1 && lines.size() == index) {
                    outputData.add(processField3(xmlFieldListF, s));
                }


            }
        } else {
            LogProcess.info(log,"not allowed to read  = {}", fileName);
        }

        return outputData;

    }

    /**
     * 根據指定的 byte 長度與編碼，回傳可以安全 substring 的字元位置。
     *
     * @param str      要處理的字串
     * @param maxBytes 最多 byte 數
     * @param charset  使用的字元編碼（例如 "Big5"、"UTF-8"）
     * @return 回傳正確的位置截斷位置可用於 substring(0, result) 的字元位置
     */
    public int getSafeSubstringLength(String str, int maxBytes, Charset charset) {

        int currentBytes = 0;

        for (int i = 0; i < str.length(); i++) {
            String ch = str.substring(i, i + 1);
            int byteLen = ch.getBytes(charset).length;

            if (currentBytes + byteLen > maxBytes) {
                return i; //
            }
            currentBytes += byteLen;
        }
        return str.length();
    }


    public String cleanAndRestore(String input, Function<String, String> processor) {
        if (input == null || input.length() < 2) return input;

        char first = input.charAt(0);
        char last = input.charAt(input.length() - 1);

        if (first == last && isWrapSymbol(first)) {
            String core = input.substring(1, input.length() - 1);
            String processed = processor.apply(core);
            return first + processed + last;
        }

        return processor.apply(input);
    }

    private static boolean isWrapSymbol(char ch) {
        return ch == '"' || ch == '\'' || ch == '*' || ch == '$' || ch == '(' || ch == ')' || ch == '!';
    }

}
