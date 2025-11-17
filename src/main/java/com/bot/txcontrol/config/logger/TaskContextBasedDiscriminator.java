/* (C) 2023 */
package com.bot.txcontrol.config.logger;

// import ch.qos.logback.classic.spi.ILoggingEvent;
// import ch.qos.logback.core.sift.AbstractDiscriminator;

// extends AbstractDiscriminator<ILoggingEvent>
public class TaskContextBasedDiscriminator {
    private static final String LOG_FILE_KEY = "userId";
    private static final String DEFAULT_VALUE = "SysTem";

    //    @Override
    //    public String getKey() {
    //        return LOG_FILE_KEY;
    //    }
    //
    //    @Override
    //    public String getDiscriminatingValue(ILoggingEvent iLoggingEvent) {
    //        return DEFAULT_VALUE;
    //    }
}
