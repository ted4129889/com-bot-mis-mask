/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum TxFormat {
    FORMAT("FORMAT"),
    TEXT("0"),
    JSON("1"),
    XML("2");

    private final String txCharsets;

    TxFormat(final String txCharsets) {
        this.txCharsets = txCharsets;
    }

    public String getCode() {
        return this.txCharsets;
    }
}
