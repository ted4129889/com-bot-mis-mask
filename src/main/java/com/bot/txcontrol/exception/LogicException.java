/* (C) 2023 */
package com.bot.txcontrol.exception;

import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.LogType;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogicException extends BaseException {
    public LogicException(String msgId, String errMsg) {
        ApLogHelper.error(log, false, LogType.TXCONTROL.getCode(), "{}, {}", msgId, errMsg);
        if (!Objects.isNull(msgId) && msgId.trim().length() == 3) msgId = "E" + msgId;
        this.setMsgId(msgId);
        this.setErrMsg(errMsg);
    }
}
