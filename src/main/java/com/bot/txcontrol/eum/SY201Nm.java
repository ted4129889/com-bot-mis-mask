/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum SY201Nm {
    // RGNO 區域代號
    RGNO(),
    // AREA 地區別
    AREA(),
    // BRNO 分行別
    BRNO(),
    // FLAG TRACE用
    FLAG(),
    // BOFF 營業記號
    BOFF(),
    // MICR MICR行記號
    MICR(),
    // FREX 外匯行記號
    FREX(),
    // RTOLBR 國內匯兌行記號
    RTOLBR(),
    // RCLTM 實際軋帳時間
    RCLTM(),
    // RU1TM 實際聯行票交時間
    RU1TM(),
    // DCLTM Default軋帳時間
    DCLTM(),
    // DU1TM Default聯行票交時間
    DU1TM(),
    // BRNAME 分行英文名稱
    BRNAME(),
    // AFCBVD 存款科目軋帳記號
    AFCBVD(),
    // AFCBVL 放款科目軋帳記號
    AFCBVL(),
    // AFCBVF 外匯科目軋帳記號
    AFCBVF(),
    // AFCBVR1 匯兌科目軋帳記號1
    AFCBVR1(),
    // AFCBVR2 匯兌科目軋帳記號2
    AFCBVR2(),
    // AFCBVR3 匯兌科目軋帳記號3
    AFCBVR3(),
    // PREFIX 央行字軌 OCCURS 2 TIMES
    PREFIX(),
    // MONBR 記帳母行
    MONBR(),
    // MAINCD 通匯行MAIN CODE
    MAINCD(),
    // XRATE 收到匯率時間
    XRATE(),
    // LASTDT 上次交易日 收到通匯行、清?
    LASTDT(),
    // CLASS 分行等級
    CLASS(),
    // SEQ 行別的排列順序
    SEQ(),
    // CHNAM 中文名稱
    CHNAM(),
    // MNGNAM 負責人
    MNGNAM(),
    // ACTENAM 主辦會計
    ACTENAM(),
    // ARCD縣市別
    ARCD(),
    // ZIPCD 郵遞區號 (檔案欄位此欄位靠?
    ZIPCD(),
    // ADR 住址
    ADR(),
    // LMNGNO
    LMNGNO(),
    // LMNGNAM
    LMNGNAM(),
    // LEMPNO
    LEMPNO(),
    // LEMPNAM
    LEMPNAM(),
    // CHCD 票交所代號
    CHCD(),
    // BQCLS 退票結束記號
    BQCLS(),
    // CKBR 票據交換主辦行
    CKBR(),
    // BINBR
    BINBR(),
    // SEDCKCLS 應提出同業票據控制記號
    SEDCKCLS(),
    // EKPFIX 電子票據字軌
    EKPFIX(),
    // EMAIL E-Mail地址
    EMAIL(),
    // TELNO 電話
    TELNO(),
    // NSEQNO MMA新申請序號
    NSEQNO(),
    // CSEQNO MMA轉換序號
    CSEQNO(),
    // SNAME 發文代字
    SNAME(),
    // IPADR 分行IP位置
    IPADR(),
    // OLINTCD 連線計息記號
    OLINTCD(),
    // GOLDCD 黃金業務鎖定記號
    GOLDCD(),
    // ENTPNO 單位統編
    ENTPNO(),
    // TAXNO 營利事業稅籍編號
    TAXNO(),
    // ENTPNAME 營利事業分公司名稱
    ENTPNAME(),
    // GOLD 黃金業務記號
    GOLD(),
    // CITY 稅籍所在地(印花稅)
    CITY(),
    // AFCBVL1 放款總軋記號
    AFCBVL1(),
    // SBRNO 所屬帳務行
    SBRNO(),
    // ACFLG 會計系統啟用記號
    ACFLG(),
    // ACDATE 會計系統啟用日期
    ACDATE(),
    // BRLVL 分行種類
    BRLVL(),
    // BRCLS 分行總結記號
    BRCLS(),
    // BRCLSEQ 分行總結編號
    BRCLSEQ(),
    // BRCLSTM 分行總結時間
    BRCLSTM(),
    // DEPTCLS 部門結帳記號OCCURS 10 TIM
    DEPTCLS(),
    // DEPTCLSEQ 部門結帳序號OCCURS 10 T
    DEPTCLSEQ(),
    // DEPTCLSTM 部門結帳時間OCCURS 10 T
    DEPTCLSTM(),
    // TUNE 前一營業日調帳記號
    TUNE(),
    // SUMTYPE 匯總傳票總類
    SUMTYPE(),
    // ACDFG1 利息提列記號
    ACDFG1(),
    // ACDFG2 營業稅提列記號
    ACDFG2(),
    // ACDFG3 印花稅提列記號
    ACDFG3(),
    // BUGFG 福利金提列記號
    BUGFG(),
    // GOLDFG 黃金業務營業稅提列記號
    GOLDFG(),
    // BRFG 聯行限制時間交易設定記號
    BRFG(),
    // TLRCASH 櫃員日終箱存限額
    TLRCASH(),
    // RECLMT 收款限額
    RECLMT(),
    // PAYLMT 付款限額
    PAYLMT(),
    // FRECLMT 值台幣
    FRECLMT(),
    // FPAYLMT 值台幣
    FPAYLMT(),
    // UFLAG1 經收稅費軋帳記號
    UFLAG1(),
    // UFLAG2 經收國庫軋帳記號
    UFLAG2(),
    // CARDFLG
    CARDFLG(),
    // TREGION 所屬稅捐管轄機關代號
    TREGION(),
    // LOCATE 省屬縣市名稱(中文)
    LOCATE(),
    // LOCATECD 所屬分處分局代號
    LOCATECD(),
    // HSTAXNO 房屋稅籍編號
    HSTAXNO(),
    // FBRNAME 營利事業名稱(全名)
    FBRNAME(),
    // SBRNAME 營利事業名稱(縮寫)
    SBRNAME()
}
