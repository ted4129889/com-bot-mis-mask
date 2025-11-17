/* (C) 2023 */
package com.bot.txcontrol.buffer.mg;

import lombok.Data;

@Data
public class Bctl {
    // BRNO 分行別
    private String brno;
    // SBRNO 帳務行
    private String sbrno;
    // ACFLG 會計系統啟用記號
    private short acflg;
    // BRCLS A99:分行總結記號
    private short brcls;
    // DEPTCLS1 A98:部門結帳記號1
    private short deptcls1;
    // DEPTCLS2 A98:部門結帳記號2
    private short deptcls2;
    // DEPTCLS3 A98:部門結帳記號3
    private short deptcls3;
    // DEPTCLS4 A98:部門結帳記號4
    private short deptcls4;
    // DEPTCLS5 A98:部門結帳記號5
    private short deptcls5;
    // DEPTCLS6 A98:部門結帳記號6
    private short deptcls6;
    // DEPTCLS7 A98:部門結帳記號7
    private short deptcls7;
    // DEPTCLS8 A98:部門結帳記號8
    private short deptcls8;
    // DEPTCLS9 A98:部門結帳記號9
    private short deptcls9;
    // DEPTCLS10 A98:部門結帳記號10
    private short deptcls10;
    // RU1TM 分行營業時間
    private int ru1tm;
    // TCCLSN 分行開關機記號
    private short tcclsn;
    // 會計系統啟用日
    private int acdate;
    // 分行種類
    // % １：總行  ２：非銀行部門
    // % ３：專屬業務分行
    // % ４：海外分行、財務部、 OBU
    // % ５：消金部、國際部、保險經濟人組
    private int brlvl;

    /*------------------------------------------------*/
    // 實際軋帳時間
    private String rcltm;
    // 存款科目軋帳記號
    private String afcbvd;
    // 中文名稱
    private String chnam;
}
