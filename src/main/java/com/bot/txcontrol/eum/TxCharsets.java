/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum TxCharsets {
    CHARSETS("CHARSETS"),
    UTF8("UTF8"),
    UTF_16BE("UTF_16BE"),
    UTF_16LE("UTF_16LE"),
    BIG5("BIG5"),
    BUR("BUR"), // 繁體Unisys內碼
    ASCIIEX("ASCIIEX"), // 簡體Unisys特殊內碼一
    EBCDIC("EBCDIC");

    private final String txCharsets;

    TxCharsets(final String txCharsets) {
        this.txCharsets = txCharsets;
    }

    public String getCode() {
        return this.txCharsets;
    }
}
