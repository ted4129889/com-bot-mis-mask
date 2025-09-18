package com.bot.mask.mask;


import com.bot.mask.log.LogProcess;
import com.bot.mask.util.xml.mask.allowedTable.AllowedDevTableName;
import com.bot.mask.util.xml.mask.allowedTable.AllowedLocalTableName;
import com.bot.mask.util.xml.mask.allowedTable.AllowedProdTableName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

@Slf4j
@Service
public class MaskDataBaseService {
    @Value("${spring.profiles.active}")
    private String nowEnv;
    @Value("${localFile.mis.xml.mask.directory}")
    private String maskXmlFilePath;


    @Value("${localFile.mis.batch.output}")
    private String outputFilePath;

    @Value("${localFile.mis.batch.output_original_data}")
    private String outputFileOriginalPath;

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    private int dbMaxPoolSize;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private MaskDataWorkerService maskDataWorkerService;
    private static final String CHARSET = "BIG5";
    private static final int BUFFER_CAPACITY = 5000;

    // fix SQL Injection
    private int index = 0;
    public int totalCnt = 0;
    public int tableCnt = 0;
    String param = "";

    public boolean exec(String env, String date) {
        LogProcess.info(log,"執行資料庫資料遮蔽處理...");

        totalCnt = 0;
        tableCnt = 0;
        param = date;

        try (Connection connection = dataSource.getConnection()) {
            if (connection == null) {
                LogProcess.warn(log,"資料庫連線失敗");
                return false;
            }

            switch (env) {
                case "local" ->
                        handleEnvTables(AllowedLocalTableName.values(), AllowedLocalTableName::getTableName, t -> t, env);
                case "dev" ->
                        handleEnvTables(AllowedDevTableName.values(), AllowedDevTableName::getTableName, this::buildXmlName, env);
                case "prod" ->
                        handleEnvTables(AllowedProdTableName.values(), AllowedProdTableName::getTableName, t -> t, env);
                default -> {
                    LogProcess.warn(log,"不支援的環境參數: " + env);
                    return false;
                }
            }

        } catch (SQLException e) {
            LogProcess.error(log,"連線錯誤");
            return false;
        }

        return true;
    }

    private <T extends Enum<T>> void handleEnvTables(
            T[] tables,
            Function<T, String> getTableNameFunc,
            Function<String, String> xmlNameBuilder,
            String env
    ) {
        if (tables == null || tables.length == 0) {
            LogProcess.warn(log,"無任何可處理的 Table！");
            return;
        }

        CountDownLatch latch = new CountDownLatch(tables.length);  // 加上 Latch 控制

        for (T tableEnum : tables) {
            String tableName = getTableNameFunc.apply(tableEnum);
            String xmlFileName = xmlNameBuilder.apply(tableName);

            maskDataWorkerService.maskOneTable(
                    tableName,
                    xmlFileName,
                    env,
                    param,
                    success -> { // 每個任務完成回報
                        synchronized (this) {
                            tableCnt++;
                            if (success) {
                                totalCnt++;
                            }
                        }
                        latch.countDown();
                    }
            );


        }

        try {
//            boolean completed = latch.await(1, TimeUnit.HOURS); // 最多等一小時
            latch.await();
            LogProcess.info(log,"所有遮蔽任務完成！");
        } catch (InterruptedException e) {
            LogProcess.error(log,"等待遮蔽任務時被中斷！");
            Thread.currentThread().interrupt();
        }

        LogProcess.info(log,"總計應有 " + tableCnt + " 個允許 SQL 資料表");
        LogProcess.info(log,"有 " + totalCnt + " 個 SQL 檔案成功產生。");
    }

    private String buildXmlName(String tableName) {
        String[] parts = tableName.split("\\.");
        if (parts.length < 2) return tableName;
        return parts[parts.length - 2] + "." + "dbo" + "." + parts[parts.length - 1];
    }
}
