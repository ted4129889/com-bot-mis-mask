/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum ErrMsgId {
    E000("E000"),
    E999("E999"),
    E009("E009"),
    E067("E067"),
    E090("E090"),
    E259("E259"),
    E622("E622"),
    E630("E630"),
    E636("E636"),
    E692("E692");

    private final String code;

    ErrMsgId(final String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
