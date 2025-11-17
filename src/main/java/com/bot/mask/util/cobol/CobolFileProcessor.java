/* (C) 2024 */
package com.bot.mask.util.cobol;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public interface CobolFileProcessor {

    /** 整批處理資料 */
    List<Map<String, String>> parse(byte[] data, List<CobolField> layout);

    /** 分批處理資料 */
    void parseAndConsume(
            byte[] data, List<CobolField> layout, Consumer<Map<String, String>> consumer);

    void parseAndConsume2(
            byte[] data, List<CobolField> layout, Consumer<Map<String, String>> consumer);

    /** 分批處理資料(含表頭表尾) */
    public void parseCobolWithOptionalHeaderFooter(
//            byte[] data,
            String inputFile,
            List<CobolField> layoutHeader,
            List<CobolField> layoutDetail,
            List<CobolField> layoutFooter,
            boolean useMs950Handle,
            Consumer<Map<String, String>> headConsumer,
            Consumer<Map<String, String>> detailConsumer,
            Consumer<Map<String, String>> tailConsumer,
            int headerCnt,
            int footerCnt);
}
