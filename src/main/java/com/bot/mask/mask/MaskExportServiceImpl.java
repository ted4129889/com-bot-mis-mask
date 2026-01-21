package com.bot.mask.mask;


import com.bot.mask.log.LogProcess;
import com.bot.mask.util.files.TextFileUtil;
import com.bot.mask.util.path.PathValidator;
import com.bot.mask.util.xml.mask.DataMasker;
import com.bot.mask.util.xml.mask.XmlParser;
import com.bot.mask.util.xml.mask.xmltag.Field;
import com.bot.mask.util.xml.vo.XmlData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MaskExportServiceImpl implements MaskExportService {
    @Value("${spring.profiles.active}")
    private String nowEnv;
    @Value("${localFile.mis.xml.mask.directory}")
    private String maskXmlFilePath;
    @Value("${localFile.mis.xml.mask.config}")
    private String maskXmlConfig;
    @Value("${localFile.mis.batch.output}")
    private String outputFilePath;
    @Value("${localFile.mis.batch.output_original_data}")
    private String outputFileOriginalPath;

    @Autowired
    private PathValidator pathValidator;
    @Autowired
    private DataMasker dataMasker;
    @Autowired
    private XmlParser xmlParser;
    @Autowired
    private TextFileUtil textFileUtil;

    private static final String CHARSET = "BIG5";
    private static final int BUFFER_CAPACITY = 1024;
    private static final String SQL_EXTENSION = ".sql";
    private static final String SQL_DELETE_PREFIX = "DELETE FROM ";
    private static final String SQL_INSERT_TEMPLATE = "INSERT INTO %s (%s) VALUES (%s);";
    private static final String PARAM_VALUE = "value";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_LENGTH = "length";
    // 累積寫入總筆數（所有 writeBatchFiles 呼叫共用）
// 累計已寫入的筆數（控制是否要換檔）
    private long totalWriteCount = 0;

    // 檔案序號（_1, _2, _3...）
    private int fileIndex = 1;

    private int maxLimit = 5000000;
//    private int MAX_FILE_SIZE = 5000000;

    @Override
    public boolean exportMaskedFile(Connection conn, String xmlFileName, String tableName, String env, String date) {
        // 驗證並取得允許的表名
        String allowedTable = validXmlFile(xmlFileName, tableName, env);
        if (allowedTable == null) return false;
        // 刪除舊檔
        textFileUtil.deleteFile(outputFileOriginalPath + allowedTable + SQL_EXTENSION);
        textFileUtil.deleteFile(outputFilePath + allowedTable + SQL_EXTENSION);

        final int batchSize = 1000;
        boolean hasDeleted = false;

        long start = System.nanoTime();
        double duration = 0L;

        XmlData xmlData = null;
        try {

            xmlData = xmlParser.parseXmlFile(
                    FilenameUtils.normalize(maskXmlFilePath + xmlFileName + ".xml"));


        } catch (IOException e) {
            LogProcess.error(log, "讀取TABLE XML 格式錯誤 :" + xmlFileName, e);
        }

        if ("prod".equals(env) || "local".equals(env)) {
            allowedTable = buildTableName(xmlData.getTable().getTableName(), date);
        } else {
            allowedTable = xmlData.getTable().getTableName();
        }

        //組成正確的語句，並加入條件
        String sql = getSql(allowedTable,xmlData.getCondition());

        //根據欄位數量決定每個檔案筆數。
        int countCol = countSelectedFields(sql);
        maxLimit = decideLimit(countCol);

        List<Map<String, Object>> batchList = new ArrayList<>(batchSize);
        try (PreparedStatement pstmt = conn.prepareStatement(
                sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            pstmt.setFetchSize(batchSize);
            try (ResultSet rs = pstmt.executeQuery()) {

                // 解析欄位定義
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                // 無資料則跳過
                if (!rs.isBeforeFirst()) {
                    LogProcess.info(log, "No data found for table: " + allowedTable);
                    return false;
                }
                List<Field> fields = xmlData.getFieldList();
                while (rs.next()) {
                    // 每筆 new 一個 Map，避免重用同一物件
                    Map<String, Object> rowMap = new HashMap<>(colCount);
                    for (int i = 1; i <= colCount; i++) {
                        String columnName = meta.getColumnName(i);
                        Object value = rs.getObject(i);
                        String columnType = meta.getColumnTypeName(i);

                        int columnLength = meta.getColumnDisplaySize(i);

                        Map<String, Object> colInfo = new HashMap<>(3);
                        colInfo.put(PARAM_VALUE, value);
                        colInfo.put(PARAM_TYPE, columnType);
                        colInfo.put(PARAM_LENGTH, columnLength);
                        rowMap.put(columnName, colInfo);


                    }

                    batchList.add(rowMap);

                    if (batchList.size() >= batchSize) {
                        writeBatchFiles(batchList, allowedTable, fields, hasDeleted,xmlData.getCondition());
                        hasDeleted = true;
                        batchList.clear();
                    }
                }
                // 處理剩餘不足 batchSize 的資料
                if (!batchList.isEmpty()) {
                    writeBatchFiles(batchList, allowedTable, fields, hasDeleted,xmlData.getCondition());
                }

                //統計耗時
                duration += (System.nanoTime() - start) / 1_000_000_000.0;

                LogProcess.info(log, "產生SQL檔案 = " + outputFilePath + allowedTable + SQL_EXTENSION + ",耗時: " + duration + "s");


            }
        } catch (SQLException | IOException e) {

            LogProcess.warn(log, "Invalid object name '" + allowedTable + "'");
            return false;
        }
        return true;
    }

    private static String getSql(String tableName, String  condition) {

        String sql = "SELECT * FROM " + tableName;
        //在相應資料表的xml下加上如 <condition>date = 20260101</condition>，就會使用 WHERE條件
        if (condition != null) {
            sql = "SELECT * FROM " + tableName + " WHERE " + condition;
        }
        LogProcess.info(log, " sql =  {}", sql);
        return sql;
    }

    /**
     * 根據給定 rows 列表，先輸出原始，再輸出遮蔽後的 SQL 檔案
     */
    private void writeBatchFiles(
            List<Map<String, Object>> rows,
            String tableName,
            List<Field> fields,
            boolean deleteFlag,
            String condition
    ) throws IOException {

        // 每個檔案都一定有 _1, _2, _3
        String fileSuffix = "_" + String.format("%02d", fileIndex);

        // 原始資料
        List<String> originalSql = generateSqlLines(rows, tableName, deleteFlag,condition);

        textFileUtil.writeFileContent(
                outputFileOriginalPath + tableName + fileSuffix + SQL_EXTENSION,
                originalSql,
                CHARSET
        );

        // 2. 遮蔽後資料
        List<Map<String, Object>> maskedRows = deepCopyRows(rows);
        dataMasker.maskData(maskedRows, fields, true);
        List<String> maskedSql = generateSqlLines(maskedRows, tableName, deleteFlag,condition);

        textFileUtil.writeFileContent(
                outputFilePath + tableName + fileSuffix + SQL_EXTENSION,
                maskedSql,
                CHARSET
        );

        totalWriteCount += rows.size();

        //每 100萬筆， 換下一個檔案
        if (totalWriteCount >= maxLimit) {
            // 下一個檔案序號
            fileIndex++;
            // 重設累計筆數
            totalWriteCount = 0;
        }
    }

    //避免遮蔽時改到原始資料，省掉原來不必要的
    private List<Map<String, Object>> deepCopyRows(List<Map<String, Object>> original) {
        List<Map<String, Object>> copy = new ArrayList<>(original.size());
        for (Map<String, Object> row : original) {
            Map<String, Object> newRow = new HashMap<>(row.size());
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                // 內層 value 也是 Map
                Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
                newRow.put(entry.getKey(), new HashMap<>(valueMap));
            }
            copy.add(newRow);
        }
        return copy;
    }


    /**
     * 根據 rows 和 deleteFlag 產生 SQL 語句列表
     */
    private List<String> generateSqlLines(
            List<Map<String, Object>> rows,
            String tableName,
            boolean deleteFlag,
            String  condition
    ) {
        List<String> lines = new ArrayList<>(rows.size() + 1);
        if (!deleteFlag) {
            if (condition != null) {
                lines.add(SQL_DELETE_PREFIX + tableName + " WHERE " + condition + ";");
            }else{
                lines.add(SQL_DELETE_PREFIX + tableName + ";");
            }
        }

        for (Map<String, Object> row : rows) {
            StringBuilder colSb = new StringBuilder(BUFFER_CAPACITY);
            StringBuilder valSb = new StringBuilder(BUFFER_CAPACITY);

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String column = entry.getKey();
                Object raw = entry.getValue();

                colSb.append(column).append(",");
                if (raw instanceof Map) {
                    Map<String, Object> valuesMap = (Map<String, Object>) raw;
                    Object value = valuesMap.get("value");

                    valSb.append(formatValue(value)).append(",");
                }
            }
            colSb.setLength(colSb.length() - 1);
            valSb.setLength(valSb.length() - 1);
            lines.add(String.format(SQL_INSERT_TEMPLATE, tableName, colSb.toString(), valSb.toString()));
        }
        return lines;
    }

    private String validXmlFile(String xmlTableName, String tableName, String env) {
        String xmlFullPath = FilenameUtils.normalize(maskXmlFilePath + xmlTableName + ".xml");
        if (!pathValidator.isSafe(FilenameUtils.normalize(maskXmlFilePath), xmlFullPath)) {
            LogProcess.info(log, "檔案路徑不安全，無法允許");
            return null;
        }
        try {
            XmlData xmlData = xmlParser.parseXmlFile(xmlFullPath);
            if (xmlData == null) {
                return null;
            }
            String allowed = tableName;
            if ("prod".equals(env)) {
                allowed = xmlData.getTable().getTableName();
            }
            return allowed;
        } catch (Exception e) {
            LogProcess.warn(log, "xml file parsing fail");
            return null;
        }
    }

    private String formatValue(Object val) {
        if (val == null) {
            return "NULL";
        }
        if (val instanceof String) {

            return "N'" + val.toString().replace("'", "''") + "'";
        }
        return val.toString();
    }

    private String readConfigXmlGetDbSuffix() {
        XmlData xmlData = null;

        String dbSuffix = "";
        try {
            xmlData = xmlParser.parseXmlFile(
                    FilenameUtils.normalize(maskXmlConfig));

            dbSuffix = xmlData.getSuffix();

        } catch (IOException e) {
            LogProcess.info(log, "讀取TABLE XML 格式錯誤 :" + maskXmlConfig);
        }

        return dbSuffix;
    }


    private String buildTableName(String tableName, String date) {
        String[] parts = tableName.split("\\.");
        if (parts.length < 3) return tableName;

        String dbName = parts[0] + (!date.isEmpty() ? "_" + date : "");

        return dbName + "." + parts[1] + "." + parts[2];
    }


    // 取得欄位數
    private int countSelectedFields(String sql) {
        String selectPart = sql.toLowerCase().split("from")[0].replace("select", "").trim();
        String[] fields = selectPart.split(",");
        return fields.length;
    }

    // 根據欄位數決定批次大小
    private int decideLimit(int fieldCount) {
        if (fieldCount <= 10) return 1500000;
        if (fieldCount <= 15) return 1000000;
        if (fieldCount <= 20) return 750000;
        if (fieldCount <= 30) return 500000;
        return 100000;
    }

}
