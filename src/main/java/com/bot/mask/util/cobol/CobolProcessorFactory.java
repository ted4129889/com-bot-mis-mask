/* (C) 2024 */
package com.bot.mask.util.cobol;

import com.bot.mask.log.LogProcess;
import com.bot.mask.util.xml.vo.XmlField;
import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class CobolProcessorFactory {
    @Autowired
    private AsciiConvertedFileProcessor asciiProcessor;
    @Autowired
    private BinaryCobolFileProcessor binaryProcessor;

    /**
     * 用檔案內容判斷編碼
     */
    public CobolFileProcessor getProcessor(byte[] bytes) {

        if (isConvertedAsciiFile(bytes)) {
            LogProcess.info(log, "use AsciiConvertedFileProcessor");
            return asciiProcessor;
        } else {
            LogProcess.info(log,"use BinaryCobolFileProcessor");
            return binaryProcessor;
        }
    }

    /**
     * 用定義檔型態判斷編碼
     */
    public CobolFileProcessor getProcessor(List<XmlField> cobolTypeList) {
        boolean notExistComp = true;

        for (XmlField f : cobolTypeList) {
            if (CobolField.Type.COMP.equals(CobolField.Type.valueOf(f.getFieldType()))) {
                notExistComp = false;
                break;
            }
        }
        if (notExistComp) {
            LogProcess.info(log,"use AsciiConvertedFileProcessor");
            return asciiProcessor;
        } else {
            LogProcess.info(log,"use BinaryCobolFileProcessor");
            return binaryProcessor;
        }
    }

    /**
     * 判斷是否為 已轉文字的 ASCII 檔
     */
    private boolean isConvertedAsciiFile(byte[] bytes) {
        int nonAsciiCount = 0;
        for (byte b : bytes) {
            int ub = b & 0xFF;
            if (ub < 0x09 || (ub > 0x0D && ub < 0x20) || ub > 0x7E) {
                nonAsciiCount++;
            }
        }
        double ratio = (double) nonAsciiCount / bytes.length;
        return ratio < 0.1;
    }
}
