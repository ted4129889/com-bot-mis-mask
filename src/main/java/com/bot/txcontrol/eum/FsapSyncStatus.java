/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum FsapSyncStatus {

    // 0000 成功
    SUCCESS("0000", "成功"),
    // 9001 查詢條件錯誤
    QUERYERROR("9001", "查詢條件錯誤"),
    // 9002 查無資料
    QUERYNON("9002", "查無資料"),
    // 9901系統異常
    SYSERROR("9901", "系統異常");

    private final String status;

    private final String msg;

    FsapSyncStatus(final String status, final String msg) {
        this.status = status;
        this.msg = msg;
    }

    public String getCode() {
        return this.status;
    }

    public String getMsg(Long status) {
        return this.msg;
    }
}
