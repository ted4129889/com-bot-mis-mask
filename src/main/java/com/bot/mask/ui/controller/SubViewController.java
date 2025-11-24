package com.bot.mask.ui.controller;

import com.bot.mask.log.LogProcess;
import com.bot.mask.mask.MaskDataBaseService;
import com.bot.mask.mask.MaskDataFileService;
import com.bot.mask.mask.MaskRunSqlService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class SubViewController {
    @FXML
    private TextArea logArea;
    @FXML
    private ComboBox<String> envComboBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label labelChooseBatch;
    @FXML
    private HBox chooseBatchDateHBox;

    @FXML
    private Label lblStatus;
    @FXML
    private ProgressBar progressBar;
//    @FXML private Button btnStopSql;

    public Button btnMaskData = new Button();
    public Button btnMaskRunSql = new Button();

    public String dateParam = "";
    //    public Button btnMaskDataFile = new Button();

    @Value("${localFile.mis.batch.output}")
    private String allowedPath;

    @Autowired
    private MaskDataBaseService maskDataBaseService;

    @Autowired
    private MaskRunSqlService maskRunSqlService;

    @Autowired
    private MaskDataFileService maskDataFileService;

    @FXML
    public void initialize() {
//        int cores = Runtime.getRuntime().availableProcessors();
//       LogProcess.info(log,"Core Count: " + cores);
        // 只顯示 INFO 以上
        LogProcess.setUiMinLevel(LogProcess.Level.INFO);
        // 開發模式看細節
//        LogProcess.setUiMinLevel(LogProcess.Level.DEBUG);
        // 生產模式看重大錯誤
//        LogProcess.setUiMinLevel(LogProcess.Level.ERROR);
        LogProcess.setUiLogger(msg -> Platform.runLater(() -> logArea.appendText(msg + "\n")));

        envComboBox.getItems().addAll("prod", "dev", "local", "MaskFileData");
        //預設prod
        envComboBox.setValue("prod");

        // 一開始根據預設值設定按鈕狀態
        updateButtonVisibility(envComboBox.getValue());

        // 當 ComboBox 值改變時，更新按鈕顯示
        envComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateButtonVisibility(newVal);
        });
    }

    private void updateButtonVisibility(String env) {
        //切換環境
        LogProcess.info(log,"目前環境:" + env);

        lblStatus.setText("狀態：等待中");
        switch (env) {
            case "prod":
                btnMaskData.setText("執行產生遮蔽SQL檔");
                btnMaskData.setVisible(true);
                btnMaskRunSql.setVisible(false);
                chooseBatchDateHBox.setVisible(true);
                chooseBatchDateHBox.setDisable(false);
                break;
            case "dev":
            case "local":
                List<String> sqlPaths = maskRunSqlService.getSafeSQLFilePaths(allowedPath, env);
                btnMaskData.setText("執行產生遮蔽SQL檔");
                btnMaskData.setVisible(true);
                btnMaskRunSql.setVisible(true);
                chooseBatchDateHBox.setVisible(true);
                chooseBatchDateHBox.setDisable(false);
                if (sqlPaths.isEmpty()) {
                    btnMaskRunSql.setDisable(true);
                } else {
                    btnMaskRunSql.setDisable(false);
                }

                break;
            case "MaskFileData":
                btnMaskData.setText("執行資料檔案遮蔽");
                btnMaskData.setVisible(true);
                btnMaskRunSql.setVisible(false);
                chooseBatchDateHBox.setVisible(false);
                chooseBatchDateHBox.setDisable(true);
                break;
        }

    }

    @FXML
    private void btnMaskData() {
        LocalDate selectedDate = datePicker.getValue();
        String env = envComboBox.getValue();


        // 1. 禁用按鈕，顯示執行狀態
        setButtonsDisabled(true);

        boolean flag = false;
        switch (env) {
            case "MaskFileData":
                lblStatus.setText("遮蔽檔案 執行中...");
                // 2. 建立背景 Task
                Task<Boolean> maskTask = new Task<>() {
                    @Override
                    protected Boolean call() {
                        //  在背景執行 SQL
                        return  maskDataFileService.exec();
                    }
                };

                maskTask.setOnSucceeded(e -> {
                    lblStatus.setText("遮蔽檔案 執行完成");
                    setButtonsDisabled(false);
                    btnMaskData.setDisable(false);
                    runWithAlert(maskTask.getValue(), env);
                });

                maskTask.setOnCancelled(e -> {
                    lblStatus.setText("遮蔽檔案 已被中斷");
                    btnMaskData.setDisable(false);
                });

                maskTask.setOnFailed(e -> {
                    lblStatus.setText("遮蔽檔案 執行失敗");
                    btnMaskData.setDisable(false);
                });

                // 5. 啟動背景執行
                new Thread(maskTask).start();

                break;
            default:
                if (selectedDate != null || ("local".equals(env))) {

                    String date = selectedDate == null? "":selectedDate.toString().replace("-", "");

                    lblStatus.setText("遮蔽資料表 執行中...");
                    // 2. 建立背景 Task
                    Task<Boolean> maskTask2 = new Task<>() {
                        @Override
                        protected Boolean call() {
                            //  在背景執行 SQL
                            return  maskDataBaseService.exec(env, date);
                        }
                    };

                    maskTask2.setOnSucceeded(e -> {
                        lblStatus.setText("遮蔽資料表 執行完成");
                        setButtonsDisabled(false);
                        btnMaskRunSql.setDisable(false);
                        runWithAlert(maskTask2.getValue(), env);
                    });

                    maskTask2.setOnCancelled(e -> {
                        lblStatus.setText("遮蔽資料表 已被中斷");
                        btnMaskRunSql.setDisable(false);
                    });

                    maskTask2.setOnFailed(e -> {
                        lblStatus.setText("遮蔽資料表 執行失敗");
                        btnMaskRunSql.setDisable(false);
                    });

                    // 5. 啟動背景執行
                    new Thread(maskTask2).start();

                    break;

                } else {
                    showAlert("", "請先選擇批次日期(BatchDate)");
                    setButtonsDisabled(false);
                }
                break;
        }


    }

//    @FXML
//    private void btnMaskRunSql() {
//        LocalDate selectedDate = datePicker.getValue();
//        String date ="";
//        if (selectedDate != null) {
//             date = selectedDate.toString().replace("-", "");
//        }
//        String env = envComboBox.getValue();
//        boolean flag = maskRunSqlService.exec(env,date);
//        runWithAlert(flag, env);
//    }

    @FXML
    private void btnMaskRunSql() {
        LocalDate selectedDate = datePicker.getValue();
        String date = (selectedDate != null) ? selectedDate.toString().replace("-", "") : "";
        String env = envComboBox.getValue();

        // 1. 禁用按鈕，顯示執行狀態
        setButtonsDisabled(true);
        progressBar.setVisible(true);

        //  先顯示固定的狀態文字
        lblStatus.setText("SQL 執行中...");

        // 2. 建立背景 Task
        Task<Boolean> sqlTask = new Task<>() {
            @Override
            protected Boolean call() {
                //  在背景執行 SQL
                return maskRunSqlService.exec(env, date);
            }
        };

        //   這裡不再綁定 lblStatus.textProperty()，避免覆蓋掉「SQL 執行中...」
        progressBar.progressProperty().bind(sqlTask.progressProperty());

        sqlTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            lblStatus.setText("SQL 執行完成");
            setButtonsDisabled(false);
            progressBar.setVisible(false);
            btnMaskRunSql.setDisable(false);
            runWithAlert(sqlTask.getValue(), env);
        });

        sqlTask.setOnCancelled(e -> {
            progressBar.progressProperty().unbind();
            lblStatus.setText("SQL 已被中斷");
            progressBar.setVisible(false);
            btnMaskRunSql.setDisable(false);
        });

        sqlTask.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            lblStatus.setText("SQL 執行失敗");
            progressBar.setVisible(false);
            btnMaskRunSql.setDisable(false);
        });

        // 5. 啟動背景執行
        new Thread(sqlTask).start();
    }

    private void runWithAlert(boolean flag, String env) {

        String successMsg = "執行完成";
        String failMsg = "";
        switch (env) {
            case "prod":
                failMsg = "請到測試環境使用";
                break;
            default:
                //dev、local
                failMsg = "請到正式環境使用";
                break;
        }

        showAlert("信息", flag ? successMsg : failMsg);

    }


    /**
     * 顯示彈出提示框
     */
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setPrefSize(100, 40);
        alert.showAndWait();
    }

    private void setButtonsDisabled(boolean disabled) {
        btnMaskData.setDisable(disabled);
        btnMaskRunSql.setDisable(disabled);
//        btnStopSql.setDisable(!disabled); // 停止按鈕反向控制（執行時才可用）
    }

    @FXML
    private void btnStopSql() {
        //通知 Service 停止
//        maskRunSqlService.requestStop();
        showAlert("訊息", "已請求停止，請等待當前批次結束");
    }


//    public void appendLog(String message) {
//        Platform.runLater(() -> {
//            logArea.appendText(message + "\n");
//        });
//    }
}
