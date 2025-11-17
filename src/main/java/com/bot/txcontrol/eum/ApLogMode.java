/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum ApLogMode {
    SENSITIVE("Y"),
    NORMAL("N"),
    BOTH("A");

    private final String code;

    ApLogMode(final String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
