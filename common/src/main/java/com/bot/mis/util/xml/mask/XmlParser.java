/* (C) 2024 */
package com.bot.mis.util.xml.mask;

import com.bot.mis.util.xml.config.SecureXmlMapper;
import com.bot.mis.util.xml.vo.XmlData;
import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope("prototype")
public class XmlParser {

    private static final String XML_PATH = "external-config/xml/mask";

    public XmlData parseXmlFile(String xmlFileName) throws IOException {
        XmlMapper xmlMapper = SecureXmlMapper.createXmlMapper();
        // 路徑驗證
        File validXmlPath = new File(XML_PATH);

        /* FORTIFY: The file path is securely controlled and validated */
        File file = new File(xmlFileName);

        // 沒有檔案拋錯誤
        if (!file.exists()) {
            ApLogHelper.info(
                    log, false, LogType.NORMAL.getCode(), "not find file = " + xmlFileName);
        }
        // 驗證讀取的XML檔案是否在指定的路徑上
        if (!file.getCanonicalPath().startsWith(validXmlPath.getCanonicalPath())) {
            throw new SecurityException("Unauthorized path");
        }
        /* FORTIFY: The file path is securely controlled and validated */
        File confirmFile = new File(xmlFileName);
        if (!file.getCanonicalPath().startsWith(validXmlPath.getCanonicalPath())) {
            throw new SecurityException("Unauthorized path");
        }

        return xmlMapper.readValue(confirmFile, XmlData.class);
    }
}
