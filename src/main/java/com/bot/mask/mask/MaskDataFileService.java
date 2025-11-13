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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Value("${localFile.mis.xml.file_def}")
    private String botMaskXmlFilePath;

    @Value("${localFile.mis.xml.file_def2}")
    private String botMaskXmlFilePath2;

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
    private static final String CHARSET_CP950 = "CP950";
    private static final String CHARSET_UTF8 = "UTF-8";

    private int headerCnt = 0;
    private int footerCnt = 0;

    public boolean exec() {
        LogProcess.info(log, "執行資料檔案遮蔽處理...");
        //允許的路徑
        String tbotOutputPath = FilenameUtils.normalize(inputPath);
        String tbotMaskXmlFilePath = FilenameUtils.normalize(botMaskXmlFilePath);
        String tbotMaskXmlFilePath2 = FilenameUtils.normalize(botMaskXmlFilePath2);
        XmlFile xmlFile;
        List<XmlData> xmlDataList = new ArrayList<>();
        List<XmlData> xmlDataListD = new ArrayList<>();
        List<XmlData> xmlDataListM = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        try {
            LogProcess.info(log, "output def file = " + tbotMaskXmlFilePath);
            xmlFile = xmlParser.parseXmlFile2(tbotMaskXmlFilePath);

            xmlDataListD = xmlFile.getDataList();
//
            LogProcess.info(log, "xmlDataListD.size = " + xmlDataListD.size());

            xmlDataList.addAll(xmlDataListD);

            LogProcess.info(log, "output def file = " + tbotMaskXmlFilePath2);
            xmlFile = xmlParser.parseXmlFile2(tbotMaskXmlFilePath2);

            xmlDataListM = xmlFile.getDataList();

            LogProcess.info(log, "xmlDataListM.size = " + xmlDataListM.size());
//            LogProcess.info(log, "xmlDataListM = " + xmlDataListM);

            xmlDataList.addAll(xmlDataListM);


            LogProcess.info(log, "xmlDataList.size = " + xmlDataList.size());
//            LogProcess.info(log, "xmlDataList = " + xmlDataList);

//            LogProcess.info(log,"xmlDataList = " + xmlDataList);

            fileNames = xmlDataList.stream()
                    .map(XmlData::getFileName)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            LogProcess.error(log, "xml 讀取問題 = {}", e);

        }
        LogProcess.info(log, "讀取 external-config/xml/bot_output 資料夾下的 日批及月批 定義檔內有" + xmlDataList.size() + "組 <data> 格式，檔名清單如下...");
        LogProcess.info(log, fileNames.toString());
        int calcuTotal = 0;
        //台銀原檔案路徑
        List<String> folderList = getFilePaths(tbotOutputPath);
        LogProcess.info(log, "在batch-file/input資料夾內的檔案有" + folderList.size() + "個，清單如下...");
        LogProcess.info(log, folderList.toString());
        for (String inputFilePath : folderList) {
            //允許路徑，若有txt去除
            inputFilePath = FilenameUtils.normalize(inputFilePath.replace(TXT_EXTENSION, ""));

            Path inputFilePathTmp = Paths.get(inputFilePath);
            String inputFileName = textFileUtil.replaceDateWithPlaceholder(inputFilePathTmp.getFileName().toString());

            try {
                for (XmlData xmlData : xmlDataList) {
                    String xmlFileName = xmlData.getFileName();

                    //先匹配 XML內的fileName檔案名稱 和 讀取檔案的名稱相同
                    if (fileNameUtil.isFileNameMatch(inputFileName, xmlFileName)) {

                        //補:如果不需要遮蔽的話就直接搬檔案即可
                        List<XmlField> xmlFieldListH = xmlData.getHeader().getFieldList();
                        List<XmlField> xmlFieldListB = xmlData.getBody().getFieldList();
                        List<XmlField> xmlFieldListF = xmlData.getFooter().getFieldList();
                        boolean hasMaskTypeH = xmlFieldListH.stream()
                                .anyMatch(f -> f.getMaskType() != null && !f.getMaskType().isBlank());
                        boolean hasMaskTypeB = xmlFieldListB.stream()
                                .anyMatch(f -> f.getMaskType() != null && !f.getMaskType().isBlank());
                        boolean hasMaskTypeF = xmlFieldListF.stream()
                                .anyMatch(f -> f.getMaskType() != null && !f.getMaskType().isBlank());

                        boolean resultHasMaskType = true;

                        if (hasMaskTypeH || hasMaskTypeB || hasMaskTypeF) {
                            LogProcess.info(log, "{} 此檔案有遮蔽欄位，進行遮蔽流程", inputFileName);
                            resultHasMaskType = true;
                        } else {
                            LogProcess.info(log, "{} 此檔案無須遮蔽欄位，進行搬檔流程", inputFileName);
                            resultHasMaskType = false;
                        }

                        LogProcess.info(log, "inputFilePath = " + inputFilePath);
                        //設好要輸出的路徑
                        String outputFilePath = inputFilePath.replace("input", "output_mask_datafile");

                        //Fas 檔案轉檔處理，只有中間檔案 會有 conv
                        if (fileNameUtil.isFasFile(inputFilePath)) {


                            //XML 有Conv 開頭的表示資料轉檔(一次性用) 需特殊處理
                            if (xmlFileName.contains("Conv")) {
                                //Conv 輸入及輸出的檔名就不用動
                                outputFilePath = inputFilePath.replace("input", "output_mask_datafile");
                            } else {
                                //只有中間檔(Fas開頭的檔案)，需要先轉檔
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

                        //確認允許路徑
                        outputFilePath = FilenameUtils.normalize(outputFilePath.replace("Misbh_", ""));

                        //刪除原檔案
                        textFileUtil.deleteFile(outputFilePath);


                        if (resultHasMaskType) {

                            //特殊處理的中間檔
                            if (inputFileName.toLowerCase().contains("faslnclfl")) {
                                LogProcess.info(log, "performMasking2 inputFilePath = " + inputFilePath);
                                performMasking2(inputFilePath, xmlData, outputFilePath);

                                //處理中間檔
                            } else if (inputFileName.toLowerCase().startsWith("fas") || inputFileName.toLowerCase().startsWith("misbh_fas")) {
                                //XML 有Conv 開頭的表示資料轉檔(一次性用) 需特殊處理
                                if (xmlFileName.contains("Conv")) {
                                    LogProcess.info(log, "performMasking4 inputFilePath = " + inputFilePath);
//                                    outputData = performMasking4(inputFilePath, xmlData);
                                    //調整批次處理
                                    performMasking4(inputFilePath, xmlData, outputFilePath);

                                } else {
                                    //調整批次處理
                                    LogProcess.info(log, "performMasking3 inputFilePath = " + inputFilePath);
                                    performMasking3(inputFilePath, xmlData, outputFilePath);
                                }
                                //處理外送檔
                            } else {
                                LogProcess.info(log, "performMasking inputFilePath = " + inputFilePath);
                                //調整批次處理
                                performMasking(inputFilePath, xmlData, outputFilePath);
                            }

                            //輸出檔案
//                            textFileUtil.writeFileContent(outputFilePath, outputData, CHARSET_BIG5);


                            calcuTotal++;
                        } else {

                            //搬檔案
                            textFileUtil.transferFile(inputFilePath, outputFilePath, false);
                            calcuTotal++;
                        }
                    }
                }

            } catch (Exception e) {
                LogProcess.error(log, "XmlToInsertGenerator.sqlConvInsertTxt error = " + e);
            }
        }

        LogProcess.info(log, "產出遮蔽後的檔案在 batch-file/output_mask_datafile 資料夾,有" + calcuTotal + "個檔案");

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
            LogProcess.info(log, "Error reading files");
            LogProcess.info(log, "error=" + e.getMessage());
        }
        return sqlFilePaths;
    }


    /**
     * 執行外送檔遮蔽資料處理
     *
     * @param inputFileName  讀取讀檔(含路徑)
     * @param xmlData        定義檔內容
     * @param outputFileName 輸出檔案(含路徑)
     */
    public void performMasking(String inputFileName, XmlData xmlData, String outputFileName) {
        LogProcess.info(log, "performMasking... ");

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
//            if (!xmlFieldListB.isEmpty()) {
//                result.addAll(processFileData(inputFileName, xmlFieldListH, xmlFieldListB, xmlFieldListF, header, footer, outputFIleName));
//            }

            fileToFileBatch(inputFileName, CHARSET_BIG5, xmlFieldListH, xmlFieldListB, xmlFieldListF, header, footer, outputFileName);

//            LogProcess.info(log, "result after masking count = " + result.size());

        } catch (Exception e) {
            LogProcess.error(log, "performMasking error =" + e);

        }

    }

    /**
     * 個案處理
     * 執行遮蔽資料處理(蔽用於遮蔽程式使用) 根據資料串處理
     *
     * @param inputFileName  讀取讀檔(含路徑)
     * @param xmlData        定義檔內容
     * @param outputFileName 輸出檔案(含路徑)
     */

    public List<String> performMasking2(String inputFileName, XmlData xmlData, String outputFileName) {
        LogProcess.info(log, "performMasking2... ");

        List<XmlField> xmlFieldHeaderList = xmlData.getHeader().getFieldList();
        List<XmlField> xmlFieldBodyList = xmlData.getBody().getFieldList();
        List<XmlField> xmlFieldFooterList = xmlData.getFooter().getFieldList();

        List<String> result = new ArrayList<>();
        try {
            String allowedPath = FilenameUtils.normalize(inputPath);

            // 確認檔案路徑 是否與 允許的路徑匹配
            if (CheakSafePathUtil.isSafeFilePath(allowedPath, inputFileName)) {
                // 讀取檔案內容
                List<String> lines = textFileUtil.readFileContentWithHex(inputFileName, CHARSET_BIG5);

                String firstChar = "";
                int index = 0;
                for (String s : lines) {
                    index++;
                    firstChar = s.substring(0, 1);
                    try {

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
                        LogProcess.error(log, "performMasking2 error = " + e);

                    }

                }
                //暫時不使用批次讀取
                textFileUtil.writeFileContent(outputFileName, result, CHARSET_BIG5);

                LogProcess.debug(log, "data output file = {}", index);

            } else {
                LogProcess.info(log, "not allowed to read  = " + outputFileName);
            }
        } catch (Exception e) {
            LogProcess.error(log, "performMasking3 error =" + e);
        }

        return result;
    }

    /**
     * 執行中間檔遮蔽資料處理
     *
     * @param inputFileName  讀取讀檔(含路徑)
     * @param xmlData        定義檔內容
     * @param outputFileName 輸出檔案(含路徑)
     */

    public List<String> performMasking3(String inputFileName, XmlData xmlData, String outputFileName) {
        LogProcess.info(log, "performMasking3... ");

        int header = 0;
        int footer = 0;
        //輸出資料
        List<String> outputData = new ArrayList<>();
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


            fileToFileBatch3(inputFileName, CHARSET_BIG5, xmlFieldListH, xmlFieldListB, xmlFieldListF, header, footer, outputFileName);

        } catch (Exception e) {
            LogProcess.error(log, "performMasking3 error =" + e);
        }

        return outputData;
    }

    /**
     * 執行資料轉換檔案遮蔽資料處理
     *
     * @param inputFileName  讀取讀檔(含路徑)
     * @param xmlData        定義檔內容
     * @param outputFileName 輸出檔案(含路徑)
     */

    public List<String> performMasking4(String inputFileName, XmlData xmlData, String outputFileName) {
        LogProcess.info(log, "performMasking4... ");

        List<String> result = new ArrayList<>();
        int header = 0;
        int footer = 0;
        try {
            // 解析XML檔案格式

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
//            if (!xmlFieldListB.isEmpty()) {
//                result.addAll(processFileData2(fileName, xmlFieldListH, xmlFieldListB, xmlFieldListF, header, footer));
//            }
            fileToFileBatch4(inputFileName, CHARSET_BIG5, xmlFieldListH, xmlFieldListB, xmlFieldListF, header, footer, outputFileName);

//            LogProcess.info(log, "result after masking count = " + result.size());

        } catch (Exception e) {
            LogProcess.error(log, "performMasking4 error =" + e);


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
        Charset charset = Charset.forName(CHARSET_CP950);
        Charset charset2 = Charset.forName("UTF-8");

        int xmlLength = 0;
//        for (XmlField xmlField : xmlFieldList) {
//            LogProcess.info(log,"xmlLength = " + xmlField.getLength() +" , fieldName = " + xmlField.getFieldName());
//            xmlLength = xmlLength + parse.string2Integer(xmlField.getLength());
//        }
//        byte[] bytes = line.getBytes(charset);
//        int dataLength = bytes.length;


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
                        LogProcess.error(log, "error = {}" + e);
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
        Charset charset = Charset.forName(CHARSET_CP950);
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
                        LogProcess.error(log, "error = {}", e);
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
    private String processField4(List<XmlField> xmlFieldList, String line) {
        Charset charset = Charset.forName(CHARSET_CP950);
        Charset charset2 = Charset.forName("UTF-8");

        int xmlColumnCnt = 0;
        for (XmlField xmlField : xmlFieldList) {
            xmlColumnCnt = xmlColumnCnt + 1;
        }

        String[] sLine = line.split(",");
        int dataLength = sLine.length;
        //先比對檔案資料長度是否與定義檔加總一致
        if (xmlColumnCnt != dataLength) {
            LogProcess.debug(log, "xml length = " + xmlColumnCnt + " VS data length = " + dataLength);

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

//            LogProcess.info(log, "xmlFieldh = " + xmlField + ",value = " + value);

            i++;

            if (maskType != null) {
                String valueMask = cleanAndRestore(value, v -> {
                    try {
                        return dataMasker.applyMask(v, maskType);
                    } catch (IOException e) {
                        LogProcess.error(log, "error = {}", e);
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
        Charset charset = Charset.forName(CHARSET_CP950);
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
            LogProcess.debug(log, "xml length = " + xmlColumnCnt + " VS data length = " + dataLength);
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
                        LogProcess.error(log, "error = {}", e);
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
     * @param inputFileName  讀取檔案名稱(用於確認路徑)
     * @param xmlFieldListH  定義檔欄位(表頭)
     * @param xmlFieldListB  定義檔欄位(內容)
     * @param xmlFieldListF  定義檔欄位(表尾)
     * @param outputFileName 輸出檔案名稱(用於確認路徑)
     * @return 回傳遮罩後的資料
     */
    private List<String> processFileData(String inputFileName, List<XmlField> xmlFieldListH, List<XmlField> xmlFieldListB, List<XmlField> xmlFieldListF, int headerCnt, int footerCnt, String outputFileName) {

        //輸出資料
        List<String> outputData = new ArrayList<>();

        String allowedPath = FilenameUtils.normalize(inputPath);

        // 確認檔案路徑 是否與 允許的路徑匹配
        if (CheakSafePathUtil.isSafeFilePath(allowedPath, inputFileName)) {
            // 讀取檔案內容

            List<String> lines = textFileUtil.readFileContentWithHex(inputFileName, CHARSET_BIG5);
//            List<String> lines = textFileUtil.readFileContent(fileName, CHARSET_BIG5);
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
            LogProcess.info(log, "not allowed to read  = {}", inputFileName);
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
        LogProcess.info(log, "processFileData2 ... (Conv)");
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
                    outputData.add(processField4(xmlFieldListH, s));
                    continue;
                }
                //總筆數減去表尾 要大於等於 計算中筆數
                if (lines.size() - footerCnt >= index) {
                    outputData.add(processField4(xmlFieldListB, s));
                }

                if (footerCnt == 1 && lines.size() == index) {
                    outputData.add(processField4(xmlFieldListF, s));
                }


            }
        } else {
            LogProcess.info(log, "not allowed to read  = {}", fileName);
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

    /**
     * 批次讀寫檔案(外送檔案遮蔽)
     */
    public void fileToFileBatch(String inputFilePath, String charsetName, List<XmlField> xmlFieldListH, List<XmlField> xmlFieldListB, List<XmlField> xmlFieldListF, int headerCnt, int footerCnt, String outputFile) {
        String normalizedPath = FilenameUtils.normalize(inputFilePath);
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

        int headerLen = 0;
        int bodyLen = 0;
        int footerLen = 0;
        for (XmlField xmlField : xmlFieldListH) {
            headerLen = headerLen + parse.string2Integer(xmlField.getLength());
        }
        for (XmlField xmlField : xmlFieldListB) {
            bodyLen = bodyLen + parse.string2Integer(xmlField.getLength());
        }
        for (XmlField xmlField : xmlFieldListF) {
            footerLen = footerLen + parse.string2Integer(xmlField.getLength());
        }

        LogProcess.info(log, "headerLen = {} ,bodyLen = {} , footerLen = {} , headerCnt = {} , footerCnt = {}", headerLen, bodyLen, footerLen, headerCnt, footerCnt);


        List<String> outputData = new ArrayList<>();
        int lineNo = 0;
        int totalCnt = 0;

        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path), 64 * 1024)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
            int prev = -1, c;

            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    byte[] bytes = buf.toByteArray();

                    if (prev == '\r' && bytes.length > 0) bytes = Arrays.copyOf(bytes, bytes.length - 1);
                    buf.reset();

                    int dataLength = bytes.length;
                    //放進 List
                    String s;
                    try {
                        s = decLenient.decode(ByteBuffer.wrap(bytes)).toString();
                    } finally {
                        decLenient.reset();
                    }
                    if (lineNo == 1 && !s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
//                    outputData.add(s);
                    lineNo++;

//                    LogProcess.info(log, "lineNo =  {} ,dataLength = {} ",lineNo, dataLength);


                    if (headerCnt == 1 && headerLen == dataLength && lineNo == 1) {
                        outputData.add(processField(xmlFieldListH, s));
                        continue;
                    }
                    //總筆數減去表尾 要大於等於 計算中筆數
                    if (bodyLen == dataLength && lineNo > 0) {
                        outputData.add(processField(xmlFieldListB, s));
                    }

                    if (footerCnt == 1 && footerLen == dataLength) {
                        outputData.add(processField(xmlFieldListF, s));
                    }

                    if (outputData.size() >= 5000) {
                        textFileUtil.writeFileContent(outputFile, outputData, charsetName);
                        totalCnt = totalCnt + outputData.size();
                        outputData = new ArrayList<>();
                    }

                    if (totalCnt % 100000 == 0 && totalCnt > 0)  {
//                        LogProcess.info(log, "Number of entries already written = {} ", totalCnt);
                    }

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
                    int illegalAt = textFileUtil.firstIllegalBig5Index(bytes);
                    boolean maybeEbcdic = textFileUtil.looksLikeEbcdic(bytes);
                    boolean hasBadStrict = bad;
                    boolean hasReplacement = s.indexOf('\uFFFD') >= 0 || s.indexOf('?') >= 0;
                    boolean hasIllegalBig5 = illegalAt >= 0;
                    boolean hasEbcdicLike = maybeEbcdic;

                    // 只有其中任一條件成立才顯示
                    if (hasBadStrict || hasReplacement || hasIllegalBig5 || hasEbcdicLike) {
                        // 取 HEX 視窗（避免太長）
                        int start = Math.max(0, (illegalAt >= 0 ? illegalAt : 0) - 8);
                        int end = Math.min(bytes.length, start + 32);
                        String window = textFileUtil.bytesToHex(Arrays.copyOfRange(bytes, start, end));

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

                int dataLength = bytes.length;

                String s;
                try {
                    s = decLenient.decode(ByteBuffer.wrap(bytes)).toString();
                } finally {
                    decLenient.reset();
                }
                if (lineNo == 1 && !s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
//                out.add(s);

                if (headerCnt == 1 && headerLen == dataLength && lineNo == 1) {
                    outputData.add(processField(xmlFieldListH, s));
                }
                //總筆數減去表尾 要大於等於 計算中筆數
                if (bodyLen == dataLength && lineNo > 0) {
                    outputData.add(processField(xmlFieldListB, s));
                }

                if (footerCnt == 1 && footerLen == dataLength) {
                    outputData.add(processField(xmlFieldListF, s));
                }

                boolean bad = false;
                try {
                    decStrict.decode(ByteBuffer.wrap(bytes));
                } catch (CharacterCodingException ex) {
                    bad = true;
                } finally {
                    decStrict.reset();
                }

                int illegalAt = textFileUtil.firstIllegalBig5Index(bytes);
                boolean maybeEbcdic = textFileUtil.looksLikeEbcdic(bytes);
                boolean hasBadStrict = bad;
                boolean hasReplacement = s.indexOf('\uFFFD') >= 0 || s.indexOf('?') >= 0;
                boolean hasIllegalBig5 = illegalAt >= 0;
                boolean hasEbcdicLike = maybeEbcdic;

                // 只有其中任一條件成立才顯示
                if (hasBadStrict || hasReplacement || hasIllegalBig5 || hasEbcdicLike) {
                    // 取 HEX 視窗（避免太長）
                    int start = Math.max(0, (illegalAt >= 0 ? illegalAt : 0) - 8);
                    int end = Math.min(bytes.length, start + 32);
                    String window = textFileUtil.bytesToHex(Arrays.copyOfRange(bytes, start, end));

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


            if (!outputData.isEmpty()) {
                totalCnt = totalCnt + outputData.size();

//                LogProcess.info(log, "Number of entries already written = {} ", outputData.size());
                textFileUtil.writeFileContent(outputFile, outputData, charsetName);
                outputData = new ArrayList<>();
            }

            LogProcess.info(log, "Final number of data entries = {}", totalCnt);

            if (totalCnt == 0) {
                LogProcess.info(log, "no data output file = {}", outputFile);
            } else {
                LogProcess.info(log, "output file path = {}", outputFile);
            }

        } catch (IOException e) {
            LogProcess.error(log, "I/O error after line {}: {}", lineNo, e.getMessage(), e);
        }

    }


    /**
     * 批次讀寫檔案(中間檔案遮蔽)
     */
    public void fileToFileBatch3(String inputFilePath, String charsetName, List<XmlField> xmlFieldListH, List<XmlField> xmlFieldListB, List<XmlField> xmlFieldListF, int headerCnt, int footerCnt, String outputFile) {
        String normalizedPath = FilenameUtils.normalize(inputFilePath);
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

        int headerLen = 0;
        int bodyLen = 0;
        int footerLen = 0;
        for (XmlField xmlField : xmlFieldListH) {
            headerLen = headerLen + 1;
        }
        for (XmlField xmlField : xmlFieldListB) {
            bodyLen = bodyLen + 1;
        }
        for (XmlField xmlField : xmlFieldListF) {
            footerLen = footerLen + 1;
        }

        LogProcess.info(log, "headerLen = {} ,bodyLen = {} , footerLen = {} , headerCnt = {} , footerCnt = {}", headerLen, bodyLen, footerLen, headerCnt, footerCnt);


        List<String> outputData = new ArrayList<>();
        int lineNo = 0;
        int totalCnt = 0;

        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path), 64 * 1024)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
            int prev = -1, c;

            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    byte[] bytes = buf.toByteArray();

                    if (prev == '\r' && bytes.length > 0) bytes = Arrays.copyOf(bytes, bytes.length - 1);
                    buf.reset();

                    int dataLength = bytes.length;
                    //放進 List
                    String s;
                    try {
                        s = decLenient.decode(ByteBuffer.wrap(bytes)).toString();
                    } finally {
                        decLenient.reset();
                    }
                    if (lineNo == 1 && !s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
//                    outputData.add(s);
                    lineNo++;

//                    LogProcess.info(log, "lineNo =  {} ,dataLength = {} ",lineNo, dataLength);


                    if (headerCnt == 1 && headerLen > 0 && lineNo == 1) {
                        outputData.add(processFieldCobol(xmlFieldListH, s));
                        continue;
                    }
                    //總筆數減去表尾 要大於等於 計算中筆數
                    if (bodyLen > 0 && lineNo > 0) {
                        outputData.add(processFieldCobol(xmlFieldListB, s));
                    }

                    if (footerCnt == 1 && footerLen > 0) {
                        outputData.add(processFieldCobol(xmlFieldListF, s));
                    }

                    if (outputData.size() >= 5000) {
                        textFileUtil.writeFileContent(outputFile, outputData, charsetName);
                        totalCnt = totalCnt + outputData.size();
                        outputData = new ArrayList<>();
                    }
                    if (totalCnt % 100000 == 0 && totalCnt > 0) {
//                        LogProcess.info(log, "Number of entries already written = {} ", totalCnt);
                    }

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
                    int illegalAt = textFileUtil.firstIllegalBig5Index(bytes);
                    boolean maybeEbcdic = textFileUtil.looksLikeEbcdic(bytes);
                    boolean hasBadStrict = bad;
                    boolean hasReplacement = s.indexOf('\uFFFD') >= 0 || s.indexOf('?') >= 0;
                    boolean hasIllegalBig5 = illegalAt >= 0;
                    boolean hasEbcdicLike = maybeEbcdic;

                    // 只有其中任一條件成立才顯示
                    if (hasBadStrict || hasReplacement || hasIllegalBig5 || hasEbcdicLike) {
                        // 取 HEX 視窗（避免太長）
                        int start = Math.max(0, (illegalAt >= 0 ? illegalAt : 0) - 8);
                        int end = Math.min(bytes.length, start + 32);
                        String window = textFileUtil.bytesToHex(Arrays.copyOfRange(bytes, start, end));

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
//                out.add(s);

                if (headerCnt == 1 && headerLen > 0 && lineNo == 1) {
                    outputData.add(processFieldCobol(xmlFieldListH, s));
                }
                //總筆數減去表尾 要大於等於 計算中筆數
                if (bodyLen > 0 && lineNo > 0) {
                    outputData.add(processFieldCobol(xmlFieldListB, s));
                }

                if (footerCnt == 1 && footerLen > 0) {
                    outputData.add(processFieldCobol(xmlFieldListF, s));
                }

                boolean bad = false;
                try {
                    decStrict.decode(ByteBuffer.wrap(bytes));
                } catch (CharacterCodingException ex) {
                    bad = true;
                } finally {
                    decStrict.reset();
                }

                int illegalAt = textFileUtil.firstIllegalBig5Index(bytes);
                boolean maybeEbcdic = textFileUtil.looksLikeEbcdic(bytes);
                boolean hasBadStrict = bad;
                boolean hasReplacement = s.indexOf('\uFFFD') >= 0 || s.indexOf('?') >= 0;
                boolean hasIllegalBig5 = illegalAt >= 0;
                boolean hasEbcdicLike = maybeEbcdic;

                // 只有其中任一條件成立才顯示
                if (hasBadStrict || hasReplacement || hasIllegalBig5 || hasEbcdicLike) {
                    // 取 HEX 視窗（避免太長）
                    int start = Math.max(0, (illegalAt >= 0 ? illegalAt : 0) - 8);
                    int end = Math.min(bytes.length, start + 32);
                    String window = textFileUtil.bytesToHex(Arrays.copyOfRange(bytes, start, end));

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


            if (!outputData.isEmpty()) {
                totalCnt = totalCnt + outputData.size();

                textFileUtil.writeFileContent(outputFile, outputData, charsetName);
                outputData = new ArrayList<>();
            }

            LogProcess.info(log, "Final number of data entries = {}", totalCnt);


            if (totalCnt == 0) {
                LogProcess.info(log, "no data output file = {}", outputFile);
            } else {
                LogProcess.info(log, "output file path = {}", outputFile);
            }
        } catch (IOException e) {
            LogProcess.error(log, "I/O error after line {}: {}", lineNo, e.getMessage(), e);
        }

    }


    /**
     * 處理轉檔資料檔案
     */
    public void fileToFileBatch4(String inputFilePath, String charsetName, List<XmlField> xmlFieldListH, List<XmlField> xmlFieldListB, List<XmlField> xmlFieldListF, int headerCnt, int footerCnt, String outputFile) {
        String normalizedPath = FilenameUtils.normalize(inputFilePath);
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

        int headerLen = 0;
        int bodyLen = 0;
        int footerLen = 0;
        for (XmlField xmlField : xmlFieldListH) {
            headerLen = headerLen + parse.string2Integer(xmlField.getLength());
        }
        for (XmlField xmlField : xmlFieldListB) {
            bodyLen = bodyLen + parse.string2Integer(xmlField.getLength());
        }
        for (XmlField xmlField : xmlFieldListF) {
            footerLen = footerLen + parse.string2Integer(xmlField.getLength());
        }

        LogProcess.info(log, "headerLen = {} ,bodyLen = {} , footerLen = {} , headerCnt = {} , footerCnt = {}", headerLen, bodyLen, footerLen, headerCnt, footerCnt);


        List<String> outputData = new ArrayList<>();
        int lineNo = 0;
        int totalCnt = 0;

        try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path), 64 * 1024)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(4096);
            int prev = -1, c;

            while ((c = in.read()) != -1) {
                if (c == '\n') {
                    byte[] bytes = buf.toByteArray();

                    if (prev == '\r' && bytes.length > 0) bytes = Arrays.copyOf(bytes, bytes.length - 1);
                    buf.reset();

                    int dataLength = bytes.length;
                    //放進 List
                    String s;
                    try {
                        s = decLenient.decode(ByteBuffer.wrap(bytes)).toString();
                    } finally {
                        decLenient.reset();
                    }
                    if (lineNo == 1 && !s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
//                    outputData.add(s);
                    lineNo++;

//                    LogProcess.info(log, "lineNo = {},s =  {}  ",lineNo,s);


                    if (headerCnt == 1 && headerLen > 0 && lineNo == 1) {
                        outputData.add(processField4(xmlFieldListH, s));
                        continue;
                    }
                    //總筆數減去表尾 要大於等於 計算中筆數
                    if (bodyLen > 0 && lineNo > 0) {
                        outputData.add(processField4(xmlFieldListB, s));
                    }

                    if (footerCnt == 1 && footerLen > 0) {
                        outputData.add(processField4(xmlFieldListF, s));
                    }

                    if (outputData.size() >= 5000) {
                        textFileUtil.writeFileContent(outputFile, outputData, charsetName);
                        totalCnt = totalCnt + outputData.size();
                        outputData = new ArrayList<>();
                    }

                    if (totalCnt % 100000 == 0 && totalCnt > 0) {
                    }
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
                    int illegalAt = textFileUtil.firstIllegalBig5Index(bytes);
                    boolean maybeEbcdic = textFileUtil.looksLikeEbcdic(bytes);
                    boolean hasBadStrict = bad;
                    boolean hasReplacement = s.indexOf('\uFFFD') >= 0 || s.indexOf('?') >= 0;
                    boolean hasIllegalBig5 = illegalAt >= 0;
                    boolean hasEbcdicLike = maybeEbcdic;

                    // 只有其中任一條件成立才顯示
                    if (hasBadStrict || hasReplacement || hasIllegalBig5 || hasEbcdicLike) {
                        // 取 HEX 視窗（避免太長）
                        int start = Math.max(0, (illegalAt >= 0 ? illegalAt : 0) - 8);
                        int end = Math.min(bytes.length, start + 32);
                        String window = textFileUtil.bytesToHex(Arrays.copyOfRange(bytes, start, end));

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
//                out.add(s);

                if (headerCnt == 1 && headerLen > 0 && lineNo == 1) {
                    outputData.add(processField4(xmlFieldListH, s));
                }
                //總筆數減去表尾 要大於等於 計算中筆數
                if (bodyLen > 0 && lineNo > 0) {
                    outputData.add(processField4(xmlFieldListB, s));
                }

                if (footerCnt == 1 && footerLen > 0) {
                    outputData.add(processField4(xmlFieldListF, s));
                }

                boolean bad = false;
                try {
                    decStrict.decode(ByteBuffer.wrap(bytes));
                } catch (CharacterCodingException ex) {
                    bad = true;
                } finally {
                    decStrict.reset();
                }

                int illegalAt = textFileUtil.firstIllegalBig5Index(bytes);
                boolean maybeEbcdic = textFileUtil.looksLikeEbcdic(bytes);
                boolean hasBadStrict = bad;
                boolean hasReplacement = s.indexOf('\uFFFD') >= 0 || s.indexOf('?') >= 0;
                boolean hasIllegalBig5 = illegalAt >= 0;
                boolean hasEbcdicLike = maybeEbcdic;

                // 只有其中任一條件成立才顯示
                if (hasBadStrict || hasReplacement || hasIllegalBig5 || hasEbcdicLike) {
                    // 取 HEX 視窗（避免太長）
                    int start = Math.max(0, (illegalAt >= 0 ? illegalAt : 0) - 8);
                    int end = Math.min(bytes.length, start + 32);
                    String window = textFileUtil.bytesToHex(Arrays.copyOfRange(bytes, start, end));

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


            if (!outputData.isEmpty()) {
                totalCnt = totalCnt + outputData.size();

                textFileUtil.writeFileContent(outputFile, outputData, charsetName);
                outputData = new ArrayList<>();
            }

            LogProcess.info(log, "Final number of data entries = {}", totalCnt);

            if (totalCnt == 0) {
                LogProcess.info(log, "no data output file = {}", outputFile);
            } else {
                LogProcess.info(log, "output file path = {}", outputFile);
            }
        } catch (IOException e) {
            LogProcess.error(log, "I/O error after line {}: {}", lineNo, e.getMessage(), e);
        }

    }


}
