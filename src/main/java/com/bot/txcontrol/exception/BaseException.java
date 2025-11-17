/* (C) 2023 */
package com.bot.txcontrol.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseException extends RuntimeException {

    private String msgId = "";

    private String errMsg = "";

    private String errName = "";
    private String errLineNum = "";

//    public ResponseCase getErrorVo(RequestCase source) {
//        if (Objects.isNull(source)) return new Error_O();
//
//        this.errName = this.getStackTrace()[0].getFileName();
//        this.errLineNum = String.valueOf(this.getStackTrace()[0].getLineNumber());
//
//        Error_O errorO = new Error_O();
//
//        // seting Label
//        errorO.getLabel().setBrNo(source.getLabel().getKinbr());
//        errorO.getLabel().setTrmSeq(source.getLabel().getTrmseq());
//        errorO.getLabel().setTxtNo(source.getLabel().getTxtno());
//        errorO.getLabel().setTtskId(source.getLabel().getTtskid());
//        errorO.getLabel().setTrmTyp(source.getLabel().getTrmtyp());
//        errorO.getLabel().setTxtsk("0");
//        errorO.getLabel().setMsgEnd("0");
//        errorO.getLabel().setTotaSeq("0000");
//        errorO.getLabel().setApType(source.getLabel().getAptype());
//
//        if (this.msgId.length() >= 5) {
//            errorO.getLabel().setApType(this.msgId.substring(0, 1));
//            errorO.getLabel().setMType(this.msgId.substring(1, 2));
//            errorO.getLabel().setMsgNo(this.msgId.substring(2, 5));
//        } else if (this.msgId.length() >= 4) {
//            errorO.getLabel().setMType(this.msgId.substring(0, 1));
//            errorO.getLabel().setMsgNo(this.msgId.substring(1, 4));
//        }
//
//        errorO.getLabel().setMsgLen("");
//        errorO.getLabel().setWarnCnt("0");
//
//        // setting text
//        //        errorO.setErrorMsg(this.errMsg + " [" + this.errName + ", " + this.errLineNum +
//        // "]");
//        errorO.setErrorMsg(this.errMsg);
//        return errorO;
//    }
//
//    public ResponseCase getErrorVo(RequestSvcCase source) {
//        if (Objects.isNull(source)) return new Error_O();
//
//        this.errName = this.getStackTrace()[0].getFileName();
//        this.errLineNum = String.valueOf(this.getStackTrace()[0].getLineNumber());
//
//        Error_O errorO = new Error_O();
//
//        // seting Label
//        errorO.getLabel().setBrNo(source.getLabel().getKinbr());
//        errorO.getLabel().setTrmSeq(source.getLabel().getTrmseq());
//        errorO.getLabel().setTxtNo(source.getLabel().getTxtno());
//        errorO.getLabel().setTtskId(source.getLabel().getTtskid());
//        errorO.getLabel().setTrmTyp(source.getLabel().getTrmtyp());
//        errorO.getLabel().setTxtsk("0");
//        errorO.getLabel().setMsgEnd("0");
//        errorO.getLabel().setTotaSeq("0000");
//        errorO.getLabel().setApType(source.getLabel().getAptype());
//
//        if (this.msgId.length() >= 5) {
//            errorO.getLabel().setApType(this.msgId.substring(0, 1));
//            errorO.getLabel().setMType(this.msgId.substring(1, 2));
//            errorO.getLabel().setMsgNo(this.msgId.substring(2, 5));
//        } else if (this.msgId.length() >= 4) {
//            errorO.getLabel().setMType(this.msgId.substring(0, 1));
//            errorO.getLabel().setMsgNo(this.msgId.substring(1, 4));
//        }
//        errorO.getLabel().setMsgLen("");
//        errorO.getLabel().setWarnCnt("0");
//
//        // setting text
//        //        errorO.setErrorMsg(this.errMsg + " [" + this.errName + ", " + this.errLineNum +
//        // "]");
//        errorO.setErrorMsg(this.errMsg);
//        return errorO;
//    }
}
