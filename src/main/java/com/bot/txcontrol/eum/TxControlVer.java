/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum TxControlVer {
    VERSION("2025/10/31 v3.0.0");

    private final String value;

    TxControlVer(final String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
