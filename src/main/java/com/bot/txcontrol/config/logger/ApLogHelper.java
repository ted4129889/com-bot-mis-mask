/* (C) 2023 */
package com.bot.txcontrol.config.logger;


import com.bot.txcontrol.buffer.mg.ThreadVariable;
import com.bot.txcontrol.eum.ApLogMode;
import com.bot.txcontrol.eum.Constant;
import com.bot.txcontrol.eum.LogType;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.helpers.MessageFormatter;

public class ApLogHelper {
    public static boolean isJmsSysInfoLoggerOK = true;

//    private static void checkJmsSysInfoVO() {
//        if (Objects.isNull(ThreadVariable.getObject(Constant.LOG_VO.getCode()))) {
//            JmsSysInfoVO jmsSysInfoVO = JmsSysInfoVO.builder().build();
//            jmsSysInfoVO.setTraceId("XXX");
//            jmsSysInfoVO.setX_bot_client_id("0");
//            jmsSysInfoVO.setX_bot_server_id(System.getProperty("spring.application.name"));
//            //
//            // jmsSysInfoVO.setTraceId((String)ThreadVariable.getObject("xBoxRequestId"));
//            //
//            // jmsSysInfoVO.setX_bot_client_id((String)ThreadVariable.getObject("xBotClientId"));
//            //
//            // jmsSysInfoVO.setX_bot_server_id((String)ThreadVariable.getObject("xBotServerId"));
//            ThreadVariable.setObject(Constant.LOG_VO.getCode(), jmsSysInfoVO);
//        }
//    }

    public static void debug(Logger logger, String message, Object... arguments) {
        if (logger.isDebugEnabled()) {
            //            checkJmsSysInfoVO();
            setMDCHelper(false, LogType.NORMAL.getCode(), null);
            logger.debug(message, arguments);

            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.DEBUG);
            //            JmsSysInfoLogger.debug(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()),
            //                    message,
            //                    arguments);
        }
    }

    public static void debug(
            Logger logger,
            boolean isJmsSysInfoLogger,
            String type,
            String message,
            Object... arguments) {
//        checkJmsSysInfoVO();
        if (logger.isDebugEnabled()) {
            Object[] out = null;
            setMDCHelper(isJmsSysInfoLogger, type, null);
//            if (isJmsSysInfoLogger) {
//                ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
//                        .setBiz_log_level(BizLogLevel.DEBUG);
//                ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
//                        .setContentMsg(
//                                MessageFormatter.arrayFormat(message, arguments)
//                                        .getMessage()
//                                        .getBytes(StandardCharsets.UTF_8));
//                if (arguments.length > 1 && Objects.nonNull(arguments[arguments.length - 1])) {
//                    out = trimTail(arguments, 1);
//                    JmsSysInfoLogger.info(
//                            (JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()),
//                            arguments[arguments.length - 1].toString(),
//                            arguments);
//                } else
//                    JmsSysInfoLogger.info(
//                            (JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()),
//                            message,
//                            arguments);
//            }
            if (Objects.nonNull(out)) logger.debug(message, out);
            else logger.debug(message, arguments);
            resetMDCHelper();
        }
    }

    public static void debug(
            Logger logger, String type, String statusCode, String message, Object... arguments) {
        //        checkJmsSysInfoVO();
        if (logger.isDebugEnabled()) {
            setMDCHelper(false, type, statusCode);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.DEBUG);
            //            JmsSysInfoLogger.debug(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()),
            //                    message,
            //                    arguments);
            logger.debug(message, arguments);
            resetMDCHelper();
        }
    }

    public static void debug(
            Logger logger, String type, String statusCode, String message, Throwable t) {
        //        checkJmsSysInfoVO();
        if (logger.isDebugEnabled()) {
            setMDCHelper(false, type, statusCode);
            logger.debug(message, t);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.DEBUG);
            //            JmsSysInfoLogger.debug(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()), message, t);
            resetMDCHelper();
        }
    }

    public static void info(Logger logger, String message, Object... arguments) {
        //        checkJmsSysInfoVO();
        if (logger.isInfoEnabled()) {
            setMDCHelper(false, LogType.NORMAL.getCode(), null);
            logger.info(message, arguments);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.INFO);
            //            JmsSysInfoLogger.info(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()),
            //                    message,
            //                    arguments);
        }
    }

    public static void info(
            Logger logger,
            boolean isJmsSysInfoLogger,
            String type,
            String message,
            Object... arguments) {
//        checkJmsSysInfoVO();
        if (logger.isInfoEnabled()) {
            Object[] out = null;
            setMDCHelper(isJmsSysInfoLogger, type, null);
//            if (isJmsSysInfoLogger) {
//                ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
//                        .setBiz_log_level(BizLogLevel.INFO);
//                ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
//                        .setContentMsg(
//                                MessageFormatter.arrayFormat(message, arguments)
//                                        .getMessage()
//                                        .getBytes(StandardCharsets.UTF_8));
//                if (arguments.length > 1 && Objects.nonNull(arguments[arguments.length - 1])) {
//                    out = trimTail(arguments, 1);
//                    JmsSysInfoLogger.info(
//                            (JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()),
//                            arguments[arguments.length - 1].toString(),
//                            arguments);
//                } else
//                    JmsSysInfoLogger.info(
//                            (JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()),
//                            message,
//                            arguments);
//            }
            if (Objects.nonNull(out)) logger.info(message, out);
            else logger.info(message, arguments);

            resetMDCHelper();
        }
    }

    public static void info(
            Logger logger, String type, String statusCode, String message, Object... arguments) {
        //        checkJmsSysInfoVO();
        if (logger.isDebugEnabled()) {
            setMDCHelper(false, type, statusCode);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.INFO);
            //            JmsSysInfoLogger.info(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()),
            //                    message,
            //                    arguments);
            logger.info(message, arguments);
            resetMDCHelper();
        }
    }

    public static void info(
            Logger logger, String type, String statusCode, String message, Throwable t) {
        //        checkJmsSysInfoVO();
        if (logger.isInfoEnabled()) {
            setMDCHelper(false, type, statusCode);
            logger.info(message, t);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.INFO);
            //            JmsSysInfoLogger.info(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()), message, t);
            resetMDCHelper();
        }
    }

    public static void warn(Logger logger, String message, Object... arguments) {
        //        checkJmsSysInfoVO();
        if (logger.isWarnEnabled()) {
            setMDCHelper(false, LogType.NORMAL.getCode(), null);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.INFO);
            //            JmsSysInfoLogger.warn(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()),
            //                    message,
            //                    arguments);
            logger.warn(message, arguments);
        }
    }

    public static void warn(
            Logger logger,
            boolean isJmsSysInfoLogger,
            String type,
            String message,
            Object... arguments) {
//        checkJmsSysInfoVO();
        if (logger.isWarnEnabled()) {
            Object[] out = null;
            setMDCHelper(isJmsSysInfoLogger, type, null);
//            if (isJmsSysInfoLogger) {
//                ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
//                        .setBiz_log_level(BizLogLevel.INFO);
//                ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
//                        .setContentMsg(
//                                MessageFormatter.arrayFormat(message, arguments)
//                                        .getMessage()
//                                        .getBytes(StandardCharsets.UTF_8));
//                if (arguments.length > 1 && Objects.nonNull(arguments[arguments.length - 1])) {
//                    out = trimTail(arguments, 1);
//                    JmsSysInfoLogger.info(
//                            (JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()),
//                            arguments[arguments.length - 1].toString(),
//                            arguments);
//                } else
//                    JmsSysInfoLogger.info(
//                            (JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()),
//                            message,
//                            arguments);
//            }
            if (Objects.nonNull(out)) logger.warn(message, out);
            else logger.warn(message, arguments);
            resetMDCHelper();
        }
    }

    public static void warn(
            Logger logger, String type, String statusCode, String message, Object... arguments) {
        //        checkJmsSysInfoVO();
        if (logger.isDebugEnabled()) {
            setMDCHelper(false, type, statusCode);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.INFO);
            //            JmsSysInfoLogger.warn(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()),
            //                    message,
            //                    arguments);
            logger.warn(message, arguments);
            resetMDCHelper();
        }
    }

    public static void warn(
            Logger logger, String type, String statusCode, String message, Throwable t) {
        //        checkJmsSysInfoVO();
        if (logger.isWarnEnabled()) {
            setMDCHelper(false, type, statusCode);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.INFO);
            //            JmsSysInfoLogger.warn(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()), message, t);
            logger.warn(message, t);
            resetMDCHelper();
        }
    }

    public static void error(Logger logger, String message, Object... arguments) {
        //        checkJmsSysInfoVO();
        if (logger.isErrorEnabled()) {
            setMDCHelper(false, LogType.NORMAL.getCode(), null);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.ERROR);
            //            JmsSysInfoLogger.error(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()),
            //                    message,
            //                    arguments);
            logger.error(message, arguments);
        }
    }

    public static void error(
            Logger logger,
            boolean isJmsSysInfoLogger,
            String type,
            String message,
            Object... arguments) {
//        checkJmsSysInfoVO();
        if (logger.isErrorEnabled()) {
            Object[] out = null;
            setMDCHelper(isJmsSysInfoLogger, type, null);
//            if (isJmsSysInfoLogger) {
//                ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
//                        .setBiz_log_level(BizLogLevel.ERROR);
//                ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
//                        .setContentMsg(
//                                MessageFormatter.arrayFormat(message, arguments)
//                                        .getMessage()
//                                        .getBytes(StandardCharsets.UTF_8));
//                if (arguments.length > 1 && Objects.nonNull(arguments[arguments.length - 1])) {
//                    out = trimTail(arguments, 1);
//                    JmsSysInfoLogger.info(
//                            (JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()),
//                            arguments[arguments.length - 1].toString(),
//                            arguments);
//                } else
//                    JmsSysInfoLogger.info(
//                            (JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()),
//                            message,
//                            arguments);
//            }
            if (Objects.nonNull(out)) logger.error(message, out);
            else logger.error(message, arguments);
            resetMDCHelper();
        }
    }

    public static void error(
            Logger logger, String type, String statusCode, String message, Object... arguments) {
        //        checkJmsSysInfoVO();
        if (logger.isDebugEnabled()) {
            setMDCHelper(false, type, statusCode);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.ERROR);
            //            JmsSysInfoLogger.error(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()),
            //                    message,
            //                    arguments);
            logger.error(message, arguments);
            resetMDCHelper();
        }
    }

    public static void error(
            Logger logger, String type, String statusCode, String message, Throwable t) {
        //        checkJmsSysInfoVO();
        if (logger.isErrorEnabled()) {
            setMDCHelper(false, type, statusCode);
            //            ((JmsSysInfoVO) ThreadVariable.getObject(Constant.LOG_VO.getCode()))
            //                    .setBiz_log_level(BizLogLevel.ERROR);
            //            JmsSysInfoLogger.error(
            //                    (JmsSysInfoVO)
            // ThreadVariable.getObject(Constant.LOG_VO.getCode()), message, t);
            logger.error(message, t);
            resetMDCHelper();
        }
    }

    private static void setMDCHelper(boolean isJmsSysInfoLogger, String type, String statusCode) {
//        checkJmsSysInfoVO();
        if (isJmsSysInfoLogger) {
            MDC.put("SensitiveFlag", ApLogMode.SENSITIVE.getCode());
        } else {
            MDC.put("SensitiveFlag", ApLogMode.NORMAL.getCode());
        }

        /*
        MDC.put("Type", type == null ? LogType.NORMAL.getCode() : type);
        MDC.put("StatusCode", statusCode == null ? "" : statusCode);
        MDC.put("ErrorType", getErrorType(statusCode));
        MDC.put("BranchNo", ContextHolder.getClientHeader("branchNo"));
        MDC.put("TransactionDateTime", ContextHolder.getClientHeader("transactionDateTime"));
        MDC.put("EmpId", ContextHolder.getClientHeader("empId"));
        MDC.put("TxId", ContextHolder.getClientHeader("txId"));
        MDC.put("Prefix", ContextHolder.getClientHeader("prefix"));
        */
    }

    private static String getErrorType(String statusCode) {
        return null;
    }

    private static void resetMDCHelper() {
        setMDCHelper(false, LogType.NORMAL.getCode(), null);
    }

    private ApLogHelper() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated");
    }

    private static Object[] trimTail(Object[] args, int n) {
        if (args == null || n <= 0 || args.length <= n) return new Object[0];
        Object[] out = new Object[args.length - n];
        System.arraycopy(args, 0, out, 0, out.length);
        return out;
    }
}
