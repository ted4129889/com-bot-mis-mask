package com.bot.mask.mask;


import com.bot.mask.log.LogProcess;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;

@Slf4j
@Service
public class MaskDataWorkerService {
    @Autowired
    private DataSource dataSource;

    @Autowired
    private MaskExportService maskExportService;

    public int successCnt = 0;
    private boolean success = false;

    @Async("executionExecutor")
    public void maskOneTable(
            String tableName,
            String xmlFileName,
            String env,
            String dateParam,
            Consumer<Boolean> onFinish
    ) {
        try (Connection conn = dataSource.getConnection()) {
            boolean success = maskExportService.exportMaskedFile(conn, xmlFileName, tableName, env, dateParam);
            onFinish.accept(success);  // 成功就傳 true
        } catch (SQLException e) {
            LogProcess.error(log,"資料庫連線失敗: " + tableName);
            onFinish.accept(false);    // 失敗就傳 false
        }
    }
}