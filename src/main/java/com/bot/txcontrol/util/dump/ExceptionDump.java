/* (C) 2023 */
package com.bot.txcontrol.util.dump;

import com.bot.txcontrol.eum.Constant;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExceptionDump {

    public static String exception2String(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        return errors.toString();
        //        return findCodeLine(e);
    }

    public static String exception2String(Throwable t) {
        StringWriter errors = new StringWriter();
        t.printStackTrace(new PrintWriter(errors));
        return errors.toString();
        //        return findCodeLine(t);
    }

    private static String findCodeLine(Exception ex) {
        String errName = "";
        String errLineNum = "";
        for (StackTraceElement ste : ex.getStackTrace()) {
            if (!Objects.isNull(ste.getFileName())
                    && ste.getFileName().indexOf(Constant.LSNR_EXT.getCode()) != -1) {
                errName = ste.getFileName();
                errLineNum = String.valueOf(ste.getLineNumber());
                break;
            }
        }
        return errName.isEmpty() || errLineNum.isEmpty()
                ? ex.getMessage()
                : (ex.getMessage() + " [" + errName + ", " + errLineNum + "]");
    }

    private static String findCodeLine(Throwable throwable) {
        String errName = "";
        String errLineNum = "";
        for (StackTraceElement ste : throwable.getStackTrace()) {
            if (!Objects.isNull(ste.getFileName())
                    && ste.getFileName().indexOf(Constant.LSNR_EXT.getCode()) != -1) {
                errName = ste.getFileName();
                errLineNum = String.valueOf(ste.getLineNumber());
                break;
            }
        }
        return errName.isEmpty() || errLineNum.isEmpty()
                ? throwable.getMessage()
                : (throwable.getMessage() + " [" + errName + ", " + errLineNum + "]");
    }
}
