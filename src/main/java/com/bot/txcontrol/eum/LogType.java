/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum LogType {
    SYSTEM("System"),

    NORMAL("Normal"),

    UTIL("Util"),

    TXCONTROL("TxControl"),
    APLOG("ApLog"),
    EXCEPTION("Exception"),
    BATCH("Normal");

    private final String code;

    LogType(final String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
