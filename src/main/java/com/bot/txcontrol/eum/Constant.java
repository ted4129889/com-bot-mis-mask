/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum Constant {
    NCB("NCB"),
    NCL("NCL"),
    FAC("FAC"),
    NCL_BATCH("NCL-BATCH"),
    FAC_BATCH("FAC-BATCH"),
    NFSTP("NFSTP"),

    FSAP_RUNTIME("FSAP-RUNTIME"),
    FASP_BATCH_RUNTIME("FSAP-BATCH-RUNTIME"),
    FSAP_GETEWAY("FSAP-GETEWAY"),
    FASP_BATCH_GATEWAY("FSAP-BATCH-GATEWAY"),
    FASP_BATCH("FSAP-BATCH"),
    FSAP_COMMON_SERVICE("FSAP-COMMON-SERVICE"),

    DOUBLE_LINE_ON("2"),
    DOUBLE_LINE_OFF("1"),

    MAPPER("mapper"),
    LSNR("Lsnr"),
    LSNR_EXT("Lsnr.java"),

    OFF("0"),
    ON("1"),

    BLANK(""),
    BLANK1(" "),
    BLANK2("  "),
    BLANK3("   "),
    BLANK4("    "),
    BLANK5("     "),
    BLANK6("      "),
    BLANK7("       "),
    BLANK8("        "),
    EMPTY(""),

    USER_ID("USERID"),
    PAGE_NO("PAGENO"),
    PAGE_SIZE("PAGESIZE"),
    TOTAL_COUNT("TOTALCOUNT"),

    PLUS("+"),
    MINUS("-"),

    REQ_LABEL_FAS("REQ_LABEL_FAS"),
    OBFG("OBFG"),

    LAZY_QUERY_TOKENID("LAZYQUERYTOKENID"),
    ORGCLIENTID("ORGCLIENTID"),
    APTYPE("APTYPE"),
    MTYPE("MTYPE"),
    MSGNO("MSGNO"),

    BATCH("B"),
    ONLINE("O"),
    ASYNC("S"),
    TRMSEQ6000("6000"),

    TXTYPE_INQ("0"),
    TXTYPE_UPD("1"),
    TXTYPE_OTH("2"),
    TXTYPE_REVERS("3"),

    HCODE_ON("1"),
    HCODE_OFF("0"),

    UPDATE("U"),
    QUERRY("I"),
    RSTINQ("1"),

    TXRESULT_E("E"),
    TXRESULT_X("X"),
    TXRESULT_S("S"),

    X_BOT_STATUS_OK("0000"),
    X_BOT_STATUS_CHECKFAIL("9991"),
    X_BOT_STATUS_TIMEOUT("9998"),
    X_BOT_STATUS_ERROR("9999"),

    G2660("G2660"),

    DEF_KINBR("000"),
    DEF_TRMSEQ("00"),

    MSGLEN_SPOS("30"),
    MSGEND_SPOS("20"),

    GRPC_CLIENT_MODE_RES("0"),
    GRPC_CLIENT_MODE_STR("1"),
    GRPC_CLIENT_MODE_BYT("2"),

    NFSTP_QRY("QRY"),
    NFSTP_SYSTEM_CDOE_CL("CL"),
    NSFTP_ERROR_BEAN_NAME(""),

    LOG_VO("LOG_VO");

    private final String constant;

    Constant(final String constant) {
        this.constant = constant;
    }

    public String getCode() {
        return this.constant;
    }
}
