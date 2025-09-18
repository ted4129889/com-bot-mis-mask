package com.bot.mask.mask;


import com.bot.mask.log.LogProcess;
import com.bot.mask.util.path.PathValidator;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class MaskRunSqlService {
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${localFile.mis.batch.output}")
    private String allowedPath;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private PathValidator pathValidator;
    @Autowired
    private MaskSqlWorkerService sqlWorker;

    private String env = "";

    public boolean exec(String env, String batchDate) {
        if ("prod".equals(env)) {
            return false;
        } else {
            executeAllSqlFiles(env, batchDate);
            return true;
        }
    }


    private void executeAllSqlFiles(String env, String batchDate) {
        LogProcess.info(log,"執行 SQL 檔案語法...");

        allowedPath = FilenameUtils.normalize(allowedPath);

        List<String> sqlPaths = getSafeSQLFilePaths(allowedPath, env);
        if (sqlPaths.isEmpty()) {
            LogProcess.warn(log,"找不到任何 SQL 檔案可執行。");
            return;
        }

        CountDownLatch latch = new CountDownLatch(sqlPaths.size());
        final int[] successCount = {0};
        List<String> failList = Collections.synchronizedList(new ArrayList<>());

        for (String path : sqlPaths) {
            sqlWorker.runSql(path, env, batchDate, success -> {
                synchronized (this) {
                    if (success) {
                        successCount[0]++;
                    } else {
                        failList.add(path);
                    }
                }
                latch.countDown();
            });
        }

        try {
//            boolean completed = latch.await(1, TimeUnit.HOURS);  // 最多等一小時
//            if (!completed) {
//                LogProcess.warn(log,"[提醒] SQL 本次執行超過 1 小時，仍在處理中。");
//            }
            latch.await();
        } catch (InterruptedException e) {
            LogProcess.error(log,"等待 SQL 任務時被中斷！");
            Thread.currentThread().interrupt();
        }

        LogProcess.info(log,"總共派發 " + sqlPaths.size() + " 個 SQL 檔案任務。");
        LogProcess.info(log,"成功執行 " + successCount[0] + " 個 SQL 檔案。");

        if (!failList.isEmpty()) {
            LogProcess.warn(log,"以下 SQL 檔案執行失敗：");
            for (String failPath : failList) {
                LogProcess.warn(log," - " + failPath);
            }
        } else {

            LogProcess.info(log,"所有 SQL 檔案皆成功執行！");
        }
    }

    public List<String> getSafeSQLFilePaths(String rootPath, String env) {
        this.env = env;
        String normalizedBase = FilenameUtils.normalize(rootPath);
        try (Stream<Path> paths = Files.walk(Paths.get(normalizedBase), 1)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(p -> pathValidator.isSafe(normalizedBase, FilenameUtils.normalize(p)))
                    .filter(p -> p.toLowerCase().endsWith(".sql"))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LogProcess.warn(log,"SQL 檔案路徑尚未產生 (batch-file/output)");
            return List.of();
        }
    }
}