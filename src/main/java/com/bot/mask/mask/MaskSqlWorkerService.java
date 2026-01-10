package com.bot.mask.mask;


import com.bot.mask.log.LogProcess;
import com.bot.mask.util.xml.mask.XmlParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MaskSqlWorkerService {
    @Autowired
    private DataSource dataSource;

    @Value("${localFile.mis.xml.mask.config}")
    private String maskXmlConfig;

    @Autowired
    private XmlParser xmlParser;

    private static final Charset CHARSET = Charset.forName("MS950");
    private static final int BATCH_SIZE = 5000;
    private static final int LOG_INTERVAL = 10000;

    @Async("executionExecutor")
    public void runSql(String filePath, String env, String batchDate, Consumer<Boolean> onFinish) {
        try {
            LogProcess.info(log, " env = {}", env);
            boolean success;
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                long start = System.nanoTime();
                int inserted = executeSqlFileWithProgress(conn, filePath, env, batchDate);
                double duration = (System.nanoTime() - start) / 1_000_000_000.0;
                LogProcess.info(log, " SQL 檔案: " + filePath + " → 插入 " + inserted + " 筆 , 耗時: " + duration + " s");
                success = true;
            }

            onFinish.accept(success);
        } catch (Exception e) {
            LogProcess.error(log, " SQL 檔案執行失敗: " + filePath + " - " + e.getMessage(), e);
            onFinish.accept(false);
        }
    }

    /**
     * 流式讀取 SQL，第一筆 DELETE 單獨執行，INSERT 批次執行 + 進度顯示
     */
    private int executeSqlFileWithProgress(Connection conn, String filePath, String env, String batchDate) {
        int totalCount = 0;
        int batchCount = 0;
        boolean firstDeleteExecuted = false;

        StringBuilder currentSql = new StringBuilder();


        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), CHARSET)) {
            Connection[] connHolder = new Connection[]{conn};
            Statement stmt = conn.createStatement();
            long totalInserts = countTotalInserts(filePath);
            long processed = 0;
            long nextLogThreshold = LOG_INTERVAL;

            String line;
            while ((line = reader.readLine()) != null) {
                line = processEnv(line, env, batchDate).trim();
                if (line.isEmpty()) continue;

                currentSql.append(line).append(" ");

                if (line.endsWith(";")) {
                    String sql = currentSql.toString().replace(";", "").trim();
                    currentSql.setLength(0);

                    //  第一筆 DELETE → 直接執行並 commit，不納入計數
                    if (!firstDeleteExecuted && sql.matches("(?i)^delete\\s+.*")) {
                        stmt.execute(sql);
                        conn.commit();
                        firstDeleteExecuted = true;
//                        LogProcess.info(log,"️ 已執行第一筆 DELETE: " + filePath);
                        LogProcess.info(log, "️ 讀取檔案: " + filePath);
                        continue;
                    }

                    //  僅允許 INSERT
                    if (!sql.matches("(?i)^insert\\s+.*")) {
                        LogProcess.warn(log, " 跳過不允許的語句：" + sql);
                        continue;
                    }

                    stmt.addBatch(sql);
                    batchCount++;
                    processed++;

                    if (batchCount >= BATCH_SIZE) {
                        totalCount += executeBatchAndCommit(conn, stmt);
                        // 檢查連線是否有效 → 自動重連
                        stmt = ensureConnectionValid(connHolder, stmt);
                        conn = connHolder[0]; //  更新原本 conn
                        stmt.executeBatch();

                        batchCount = 0;
                    }

                    //  每 LOG_INTERVAL 筆 → log 進度
                    if (processed >= nextLogThreshold) {
                        double percent = (totalInserts > 0) ? (processed * 100.0 / totalInserts) : -1;
                        if (percent >= 0) {
                            LogProcess.info(log, String.format(" [%s] 已處理 %d / %d (%.2f%%)",
                                    Paths.get(filePath).getFileName(), processed, totalInserts, percent));
                        } else {
                            LogProcess.info(log, String.format(" [%s] 已處理 %d 筆 (總筆數未知)",
                                    Paths.get(filePath).getFileName(), processed));
                        }
                        nextLogThreshold += LOG_INTERVAL;
                    }
                }
            }

            //  最後一批 INSERT
            if (batchCount > 0) {
                totalCount += executeBatchAndCommit(conn, stmt);
                stmt = ensureConnectionValid(connHolder, stmt);
                conn = connHolder[0];
                stmt.executeBatch();

            }

        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception ignored) {
            }
            LogProcess.error(log, " executeSqlFileWithProgress 失敗: " + filePath + " - " + e.getMessage(), e);
        }

        return totalCount;
    }

    /**
     * 計算檔案內的 INSERT 總數（跳過第一筆 DELETE）
     */
    private long countTotalInserts(String filePath) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), CHARSET)) {
            final boolean[] firstDeleteSkipped = {false};
            return reader.lines()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && l.endsWith(";"))
                    .filter(l -> {
                        if (!firstDeleteSkipped[0] && l.matches("(?i)^delete\\s+.*")) {
                            firstDeleteSkipped[0] = true; // 跳過第一筆 delete
                            return false;
                        }
                        return l.matches("(?i)^insert\\s+.*");
                    })
                    .count();
        } catch (IOException e) {
            LogProcess.warn(log, " 無法計算總 INSERT 筆數，進度將以未知總量顯示。");
            return -1;
        }
    }

    private String processEnv(String line, String env, String batchDate) {


        if ("dev".equals(env)) {
            line = line.replaceAll("\\b(bot\\w*?db)_sync\\.dbo\\.", "misbh_db.$1.");
            line = line.replaceAll("\\b(bot\\w*?db)(?:_\\w+)?\\.dbo\\.", "misbh_db.$1.");
        } else if ("local".equals(env) || "prod".equals(env)) {
//            if (batchDate == null || batchDate.isBlank()) {
//                // 無日期 → 移除日期後綴
//                line = line.replaceAll("\\b(bot\\w*?db)_\\d{8}\\b", "$1");
//            } else {
//                // 有日期 → 替換成指定日期
//                line = line.replaceAll("\\b(bot\\w*?db)_\\d{8}\\b", "$1_" + batchDate);
//            }
            line = handleLine(line, batchDate);
        }
//        LogProcess.info(log, "env = " + env);
//        LogProcess.info(log, "batchDate = " + batchDate);
//        LogProcess.info(log, "line sql = " + line);
        return line;
    }

    private int executeBatchAndCommit(Connection conn, Statement stmt) throws SQLException {
        int[] results = stmt.executeBatch();
        conn.commit();
        return results.length;
    }


    /**
     * 檢查資料庫連線是否有效，若失效則自動重連並重新建立 Statement
     */
    private Statement ensureConnectionValid(Connection[] connHolder, Statement stmt) throws SQLException {
        Connection conn = connHolder[0];

        try {
            if (conn == null || conn.isClosed() || !conn.isValid(2)) {
                LogProcess.warn(log, " 偵測到連線失效，嘗試重新建立資料庫連線...");

                //  安全關閉舊連線
                try {
                    if (conn != null) conn.close();
                } catch (Exception e) {
                    LogProcess.warn(log, " 舊連線關閉時發生例外: " + e.getMessage());
                }

                //  重新建立連線
                conn = dataSource.getConnection();
                conn.setAutoCommit(false);
                connHolder[0] = conn;

                //  重新建立 Statement
                stmt = conn.createStatement();

                LogProcess.info(log, " 資料庫連線已成功重新建立");
            }
        } catch (SQLException ex) {
            LogProcess.error(log, " 重新建立資料庫連線失敗: " + ex.getMessage(), ex);
            throw ex;
        }

        return stmt;
    }

    public String handleLine(String line, String batchDate) {
        final Pattern DB_PATTERN = Pattern.compile("\\b(bot\\w*?db)(?:_(\\d{8}))?\\b");

        Matcher m = DB_PATTERN.matcher(line);
        StringBuffer sb = new StringBuffer();

        while (m.find()) {
            String dbName = m.group(1);
            String existingDate = m.group(2);
            String replacement;

            replacement = (batchDate != null && !batchDate.isBlank())
                    ? dbName + "_" + existingDate
                    : dbName;
//            }
            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);
        return sb.toString();

    }
}