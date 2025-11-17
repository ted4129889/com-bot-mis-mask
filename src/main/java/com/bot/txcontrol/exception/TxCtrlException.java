/* (C) 2023 */
package com.bot.txcontrol.exception;

import com.bot.txcontrol.eum.LogType;
import com.bot.txcontrol.util.dump.ExceptionDump;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TxCtrlException extends BaseException {

    public TxCtrlException(String msgId, String errMsg) {
        this.setMsgId(msgId);
        this.setErrMsg(errMsg);
    }

    public TxCtrlException(String msgId, String errMsg, Exception ex) {
        this.setMsgId(msgId);
        this.setErrMsg(errMsg);

//        ApLogHelper.error(log, false, LogType.TXCONTROL.getCode(), " turn to TxCtrlException");
//        ApLogHelper.error(
//                log, false, LogType.TXCONTROL.getCode(), ExceptionDump.exception2String(ex));
    }
}
