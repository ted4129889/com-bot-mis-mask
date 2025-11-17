/* (C) 2023 */
package com.bot.txcontrol.buffer;

import lombok.Data;

@Data
public class TxBizDate {

    // HOLIDAY 非假日或假日
    private short holiday;
    // WEEKDY 星期幾
    private short weekdy;
    // TBSDY 日曆日
    private int tbsdy;
    // NBSDY 下營業日
    private int nbsdy;
    // NNBSDY 下下營業日
    private int nnbsdy;
    // N3BSDY 下三營業日
    private int n3bsdy;
    // N4BSDY 下四營業日
    private int n4bsdy;
    // N5BSDY 下五營業日
    private int n5bsdy;
    // N6BSDY 下六營業日
    private int n6bsdy;
    // LBSDY 上營業日
    private int lbsdy;
    // LLBSDY 上上營業日
    private int llbsdy;
    // L3BSDY 上三營業日
    private int l3bsdy;
    // L4BSDY 上四營業日
    private int l4bsdy;
    // L5BSDY 上五營業日
    private int l5bsdy;
    // LMNDY 上月月底日
    private int lmndy;
    // TMNDY 本月月底日
    private int tmndy;
    // FNBSDY 本月最終營業日
    private int fnbsdy;
    // LFNBSDY 上月最終營業日
    private int lfnbsdy;
    // NDYCNT 下營業日差
    private short ndycnt;
    // NNDYCNT 下下營業日差
    private short nndycnt;
    // N3DYCNT 下三營業日差
    private short n3dycnt;
    // N4DYCNT 下四營業日差
    private short n4dycnt;
    // N5DYCNT 下五營業日差
    private short n5dycnt;
    // N6DYCNT 下六營業日差
    private short n6dycnt;
    // LDYCNT 上營業日差
    private short ldycnt;
    // LLDYCNT 上上營業日差
    private short lldycnt;
    // L3DYCNT 上三營業日差
    private short l3dycnt;
    // L4DYCNT 上四營業日差
    private short l4dycnt;
    // L5DYCNT 上五營業日差
    private short l5dycnt;
    // TIMECD
    private short timecd;

    public boolean isHliday() {
        return this.holiday == 1;
    }
}
