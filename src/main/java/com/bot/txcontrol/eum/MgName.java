/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum MgName {

    // TBSDY系統營業日
    TBSDY("TBSDY"),
    // NBSDY下營業日
    NBSDY("NBSDY"),
    // BATCHDY 批次日期
    BATCHDY("BBSDY"),
    // TODAYCTL本日控制欄
    TODAYCTL("TODAYCTL"),
    // NEXTDYCTL次日控制欄
    NEXTDYCTL("NEXTDYCTL"),
    // DAYTONIGHT計劃性切換
    DAYTONIGHT("DAYTONIGHT"),
    AHCODE("AHCODE");

    private final String name;

    MgName(String name) {
        this.name = name;
    }

    public String getCode() {
        return this.name;
    }
}
