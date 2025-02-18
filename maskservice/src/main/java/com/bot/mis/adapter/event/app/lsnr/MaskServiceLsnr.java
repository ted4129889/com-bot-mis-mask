/* (C) 2025 */
package com.bot.mis.adapter.event.app.lsnr;

import com.bot.mis.adapter.event.app.evt.MaskService;
import com.bot.mis.util.xml.mask.XmlToInsertGenerator;
import com.bot.mis.util.xml.mask.allowedTable.AllowedDevTableName;
import com.bot.mis.util.xml.mask.allowedTable.AllowedLocalTableName;
import com.bot.mis.util.xml.mask.allowedTable.AllowedProdTableName;
import com.bot.txcontrol.adapter.event.BatchListenerCase;
import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import java.io.File;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component("MaskServiceLsnr")
@Scope("prototype")
public class MaskServiceLsnr extends BatchListenerCase<MaskService> {

    @Value("${localFile.mis.xml.mask.directory}")
    private String maskXmlFilePath;

    @Value("${spring.profiles.active}")
    private String nowEnv;

    private final String STR_DOT = ".";
    private final String XML_EXTENSION = ".xml";
    @Autowired XmlToInsertGenerator xmlToInsertGenerator;

    //    @Async("batchThreadPoolTaskExecutor") // 如需平行處理請將此行註解拿掉
    @Override
    public void onApplicationEvent(MaskService event) {
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "MaskServiceLsnr");
        this.beforRun(event);
    }

    @Override
    @SneakyThrows
    protected void run(MaskService event) {
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "MaskServiceLsnr run()");

        switch (nowEnv) {
            case "local":
                for (AllowedLocalTableName name : AllowedLocalTableName.values()) {
                    validFileAndExportFile(name.getTableName());
                }
                break;
            case "dev":
                for (AllowedDevTableName name : AllowedDevTableName.values()) {
                    validFileAndExportFile(name.getTableName());
                }
                break;

            case "sit":
                for (AllowedProdTableName name : AllowedProdTableName.values()) {
                    validFileAndExportFile(name.getTableName());
                }
                break;
        }
    }

    private void validFileAndExportFile(String tableName) {
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "tableName=" + tableName);
        // 進行拆分(以利於匹配)
        String[] xmlFileSplit = tableName.split("\\.");
        ApLogHelper.info(
                log, false, LogType.NORMAL.getCode(), "xmlFileSplit=" + xmlFileSplit.length);
        String tmpXmlFileName =
                xmlFileSplit[xmlFileSplit.length - 2]
                        + STR_DOT
                        + xmlFileSplit[xmlFileSplit.length - 1];
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "tmpXmlFileName=" + tmpXmlFileName);

        // 組合後的檔案名稱
        String xml = maskXmlFilePath + tmpXmlFileName + XML_EXTENSION;
        ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "xml=" + xml);

        File file = new File(xml);
        // 沒有檔案時 略過
        if (!file.exists()) {
            ApLogHelper.info(log, false, LogType.NORMAL.getCode(), "not find file = " + xml);
        } else {
            xmlToInsertGenerator.sqlConvInsertTxt(xml, tableName);
        }
    }
}
