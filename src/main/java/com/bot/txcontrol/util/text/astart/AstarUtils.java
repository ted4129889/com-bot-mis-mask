/* (C) 2023 */
package com.bot.txcontrol.util.text.astart;

import com.AStar.TBConvert.BUR.ConvertBUR_Big5;
import com.AStar.TBConvert.BUR.ConvertBUR_UTF8;
import com.AStar.TBConvert.BUR.TB_BUR_Big5;
import com.AStar.TBConvert.BUR.TB_BUR_UCS2;
import com.AStar.TBConvert.Big5.ConvertBig5_UCS2;
import com.AStar.TBConvert.Big5.TB_Big5_BUR;
import com.AStar.TBConvert.Big5.TB_Big5_UCS2;
import com.AStar.TBConvert.TBConvertDefine;
import com.AStar.TBConvert.UCS2.ConvertUCS2_Big5;
import com.AStar.TBConvert.UCS2.TB_UCS2_BUR;
import com.AStar.TBConvert.UCS2.TB_UCS2_Big5;
import com.AStar.TBConvert.UTF8.ConvertUTF8_BUR;
import com.bot.txcontrol.buffer.mg.ThreadVariable;
import com.bot.txcontrol.config.logger.ApLogHelper;
import com.bot.txcontrol.eum.*;
import com.bot.txcontrol.exception.TxCtrlException;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.Charsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
//@DependsOn("LoggerHelperFactory")
@Component
// @Scope("prototype")
@Scope("singleton")
public class AstarUtils {

    private static final String TB_BUR_UCS2_BIN = "TB_BUR_UCS2.bin";
    private static final String TB_UCS2_BUR_BIN = "TB_UCS2_BUR.bin";

    private static final String TB_BIG5_UCS2_BIN = "TB_BIG5_UCS2.bin";
    private static final String TB_UCS2_BIG5_BIN = "TB_UCS2_BIG5.bin";

    private static final String TB_BUR_BIG5_BIN = "TB_BUR_BIG5.bin";

    private static final String BIG5 = "BIG5";

    @Value("${spring.profiles.active:}")
    private String env;

    @Value("${astar.binPath:}")
    private String binPath = "/Users/adam/work/BOT/config/astar/";

    private TB_BUR_UCS2 tb_bur_ucs2 = null;
    private TB_UCS2_BUR tb_ucs2_bur = null;

    private TB_Big5_UCS2 tb_big5_ucs2 = null;
    private TB_UCS2_Big5 tb_ucs2_big5 = null;

    private TB_BUR_Big5 tb_bur_big5 = null;
    private TB_Big5_BUR tb_big5_bur = null;

    @PostConstruct
    public void init() {
        ApLogHelper.debug(log, false, LogType.SYSTEM.getCode(), "Astar init...");

        this.tb_bur_ucs2 = new TB_BUR_UCS2();
        this.tb_ucs2_bur = new TB_UCS2_BUR();
        this.tb_big5_ucs2 = new TB_Big5_UCS2();
        this.tb_ucs2_big5 = new TB_UCS2_Big5();

        this.tb_bur_big5 = new TB_BUR_Big5();

        ApLogHelper.info(
                log,
                false,
                LogType.SYSTEM.getCode(),
                "Load TB_BUR_UCS2 [{}]  returnCode : [{}]",
                this.binPath + TB_BUR_UCS2_BIN,
                this.tb_bur_ucs2.load(this.binPath + TB_BUR_UCS2_BIN));
        ApLogHelper.info(
                log,
                false,
                LogType.SYSTEM.getCode(),
                "Load TB_UCS2_BUR [{}]  returnCode : [{}]",
                this.binPath + TB_UCS2_BUR_BIN,
                this.tb_ucs2_bur.load(this.binPath + TB_UCS2_BUR_BIN));
        ApLogHelper.info(
                log,
                false,
                LogType.SYSTEM.getCode(),
                "Load TB_BIG5_UCS2 [{}] returnCode : [{}]",
                this.binPath + TB_BIG5_UCS2_BIN,
                this.tb_big5_ucs2.load(this.binPath + TB_BIG5_UCS2_BIN));
        ApLogHelper.info(
                log,
                false,
                LogType.SYSTEM.getCode(),
                "Load TB_UCS2_BIG5 [{}] returnCode : [{}]",
                this.binPath + TB_UCS2_BIG5_BIN,
                this.tb_ucs2_big5.load(this.binPath + TB_UCS2_BIG5_BIN));
        ApLogHelper.info(
                log,
                false,
                LogType.SYSTEM.getCode(),
                "Load TB_BUR_BIG5 [{}] returnCode : [{}]",
                this.binPath + TB_UCS2_BIG5_BIN,
                this.tb_bur_big5.load(this.binPath + TB_BUR_BIG5_BIN));
    }

    public String byte2String(byte[] source) {
        if (Objects.isNull(source) || source.length == 0) return Constant.EMPTY.getCode();

        if (ThreadVariable.getObject(TxCharsets.CHARSETS.getCode()) == Charsets.toCharset("BUR"))
            return this.burToUTF8(source);
        if (ThreadVariable.getObject(TxCharsets.CHARSETS.getCode()) == Charsets.toCharset("BIG5"))
            return this.big5ToUTF8(source);
        if (ThreadVariable.getObject(TxCharsets.CHARSETS.getCode()) == Charsets.UTF_8)
            return new String(source, StandardCharsets.UTF_8);

        return new String(source, StandardCharsets.UTF_8);
    }

    public byte[] string2Byte(String source) {
        if (ThreadVariable.getObject(TxCharsets.CHARSETS.getCode()) == Charsets.toCharset("BUR"))
            return this.utf8ToBUR(source);
        if (ThreadVariable.getObject(TxCharsets.CHARSETS.getCode()) == Charsets.toCharset("BIG5"))
            return this.utf8ToBIG5(source);
        if (ThreadVariable.getObject(TxCharsets.CHARSETS.getCode()) == Charsets.UTF_8)
            return source.getBytes(StandardCharsets.UTF_8);

        return source.getBytes(StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public String burToUTF8(byte[] data) {
        return new String(this.burToUcs2ForCol(data), StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public byte[] utf8ToBUR(String s) {
        return this.ucs2ToBurForCol(s.getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    public byte[] utf8ToBIG5(String s) {
        //        if (Env.LOCAL.getCode().equals(this.env)) return s.getBytes(BIG5);
        //        else
        return this.ucs2ToBig5(s.toCharArray());
    }

    @SneakyThrows
    public String big5ToUTF8(byte[] bytes) {
        //        if (Env.LOCAL.getCode().equals(this.env)) return new String(bytes,
        // StandardCharsets.UTF_8);
        //        else
        return new String(this.big5ToUcs2(bytes));
    }

    @SneakyThrows
    public byte[] bur2Big5(byte[] bytes) {
        return this.burToBig5(bytes);
    }

    private byte[] ucs2ToBig5(char[] dataByte) {
        ConvertUCS2_Big5 convertUCS2_big5 = new ConvertUCS2_Big5();
        long returnCode = convertUCS2_big5.convert(tb_ucs2_big5, dataByte, dataByte.length, false);
        if (returnCode == TBConvertDefine.CONVT_SUCCESS) return convertUCS2_big5.getResult();
        else
            throw new TxCtrlException(
                    ErrMsgId.E999.getCode(),
                    String.format("UTF8TOBIG5 Faill return Code : [%d]", returnCode));
    }

    private char[] big5ToUcs2(byte[] dataByte) {
        ConvertBig5_UCS2 convertBig5_ucs2 = new ConvertBig5_UCS2();
        long returnCode = convertBig5_ucs2.convert(tb_big5_ucs2, dataByte, dataByte.length);
        if (returnCode == TBConvertDefine.CONVT_SUCCESS) return convertBig5_ucs2.getResult();
        else
            throw new TxCtrlException(
                    ErrMsgId.E999.getCode(),
                    String.format("BIG5TOUTF8 Faill return Code : [%d]", returnCode));
    }

    private byte[] burToBig5(byte[] dataByte) throws Exception {
        ConvertBUR_Big5 convertBUR_Big5 = new ConvertBUR_Big5();

        ByteArrayOutputStream tempByte = new ByteArrayOutputStream();

        int sosiCount = 0;
        long returnCode = 0L;

        byte[] dataUse = Arrays.copyOfRange(dataByte, 0, dataByte.length);

        for (int i = 0; i < dataUse.length; i++)
            if ((dataUse[i] == 0x2B) || (dataUse[i] == 0x2C)) sosiCount++;

        byte[] newDatasix = new byte[dataUse.length + sosiCount];

        for (int i = 0, j = 0; i < dataUse.length; i++) {
            if (dataUse[i] == 0x2B) {
                newDatasix[j++] = 0x40;
                // newDatasix[j++] = 0x37; // 0x37 會被轉成 0x04
                newDatasix[j++] = 0x2B; // 0x0E 會被刪除
            } else {
                if (dataUse[i] == 0x2C) {
                    newDatasix[j++] = 0x2C; // 0x0F 會被刪除
                    newDatasix[j++] = 0x40;
                    //                    newDatasix[j++] = 0x2F; // 0x2F 會被轉成 0x07
                    //                     newData2[j++] = 0x40;
                    // newData2[j++] = 0x40;
                } else {
                    if (dataUse[i] == 0x00) newDatasix[j++] = 0x40;
                    else newDatasix[j++] = dataUse[i];
                }
            }
        }

        returnCode = convertBUR_Big5.convert(tb_bur_big5, newDatasix, newDatasix.length, false);
        if (returnCode == TBConvertDefine.CONVT_SUCCESS) return convertBUR_Big5.getResult();
        else
            throw new TxCtrlException(
                    ErrMsgId.E999.getCode(),
                    String.format("BIG5TOUTF8 Faill return Code : [%d]", returnCode));
    }

    private byte[] burToUcs2ForCol(byte[] dataByte) throws Exception {
        ConvertBUR_UTF8 convertBUR_UTF8 = new ConvertBUR_UTF8();

        ByteArrayOutputStream tempByte = new ByteArrayOutputStream();

        int sosiCount = 0;
        long returnCode = 0L;

        for (int i = 0; i < dataByte.length; i++) if (dataByte[i] == 0x00) dataByte[i] = 0x40;

        if (!Objects.isNull(ThreadVariable.getObject("0407"))
                && (boolean) ThreadVariable.getObject("0407")) {
            tempByte.write(burToUcs2(dataByte));
            return tempByte.toByteArray();
        } else returnCode = convertBUR_UTF8.convert(tb_bur_ucs2, dataByte, dataByte.length);

        if (returnCode == TBConvertDefine.CONVT_SUCCESS) {
            tempByte.write(convertBUR_UTF8.getResult());
        } else {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.SYSTEM.getCode(),
                    "BUR2UTF8 Faill return Code : [{}]",
                    returnCode);
            throw new TxCtrlException(
                    ErrMsgId.E999.getCode(),
                    String.format("BUR2UTF8 Faill return Code : [%d]", returnCode));
        }
        return tempByte.toByteArray();
    }

    private byte[] ucs2ToBurForCol(byte[] dataByte) throws IOException {
        ConvertUTF8_BUR convertUTF8_BUR = new ConvertUTF8_BUR();

        ByteArrayOutputStream tempByte = new ByteArrayOutputStream();
        int sosiCount = 0;

        long returnCode = convertUTF8_BUR.convert(this.tb_ucs2_bur, dataByte, dataByte.length);
        if (TBConvertDefine.CONVT_SUCCESS == returnCode) return convertUTF8_BUR.getResult();
        else {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.SYSTEM.getCode(),
                    "UTF8TOBUR Faill return Code : [{}]",
                    returnCode);
            throw new TxCtrlException(
                    ErrMsgId.E999.getCode(),
                    String.format("BURTOUTF8 Faill return Code : [{}]", returnCode));
        }
    }

    private byte[] burToUcs2(byte[] dataByte) throws Exception {
        ConvertBUR_UTF8 convertBUR_UTF8 = new ConvertBUR_UTF8();

        ByteArrayOutputStream tempByte = new ByteArrayOutputStream();

        int sosiCount = 0;
        long returnCode = 0L;

        byte[] dataUse = Arrays.copyOfRange(dataByte, 0, dataByte.length);

        for (int i = 0; i < dataUse.length; i++)
            if ((dataUse[i] == 0x2B) || (dataUse[i] == 0x2C)) sosiCount++;

        byte[] newDatasix = new byte[dataUse.length + sosiCount];

        for (int i = 0, j = 0; i < dataUse.length; i++) {
            if (dataUse[i] == 0x2B) {
                newDatasix[j++] = 0x40;
                // newDatasix[j++] = 0x37; // 0x37 會被轉成 0x04
                newDatasix[j++] = 0x2B; // 0x0E 會被刪除
            } else {
                if (dataUse[i] == 0x2C) {
                    newDatasix[j++] = 0x2C; // 0x0F 會被刪除
                    newDatasix[j++] = 0x40;
                    //                    newDatasix[j++] = 0x2F; // 0x2F 會被轉成 0x07
                    //                     newData2[j++] = 0x40;
                    // newData2[j++] = 0x40;
                } else {
                    if (dataUse[i] == 0x00) newDatasix[j++] = 0x40;
                    else newDatasix[j++] = dataUse[i];
                }
            }
        }

        returnCode = convertBUR_UTF8.convert(tb_bur_ucs2, newDatasix, newDatasix.length);

        if (returnCode == TBConvertDefine.CONVT_SUCCESS) {
            tempByte.write(convertBUR_UTF8.getResult());
        } else {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.SYSTEM.getCode(),
                    "UTF8TOBUR Faill return Code : [{}]",
                    returnCode);
            throw new TxCtrlException(
                    ErrMsgId.E999.getCode(),
                    String.format("UTF8TOBUR Faill return Code : [{}]", returnCode));
        }
        return tempByte.toByteArray();
    }

    private byte[] ucs2ToBur(byte[] dataByte) throws IOException {
        ConvertUTF8_BUR convertUTF8_BUR = new ConvertUTF8_BUR();

        ByteArrayOutputStream tempByte = new ByteArrayOutputStream();
        int sosiCount = 0;

        long returnCode = convertUTF8_BUR.convert(this.tb_ucs2_bur, dataByte, dataByte.length);
        if (TBConvertDefine.CONVT_SUCCESS == returnCode) {
            byte[] dataUse = convertUTF8_BUR.getResult();

            for (int i = 0; i < dataUse.length; i++)
                if ((dataUse[i] == 0x2B && dataUse[i - 1] == 0x37)
                        || (dataUse[i] == 0x2F && dataUse[i - 1] == 0x2C)) sosiCount++;

            if (sosiCount > 0) {
                byte[] newDatasix = new byte[dataUse.length - sosiCount];

                for (int i = 0, j = 0; i < dataUse.length; i++) {
                    if (dataUse[i] == 0x37 && dataUse[i + 1] == 0x2B) {
                        ;
                    } else {
                        if (dataUse[i] == 0x2C) {
                            newDatasix[j] = dataUse[i];
                            j++;
                        } else {
                            if (dataUse[i] == 0x2f && dataUse[i - 1] == 0x2C) {
                                ;
                            } else {
                                newDatasix[j] = dataUse[i];
                                j++;
                            }
                        }
                    }
                }
                return newDatasix;
            } else {
                return dataUse;
            }
        } else {
            ApLogHelper.error(
                    log,
                    false,
                    LogType.SYSTEM.getCode(),
                    "UTF8TOBUR Faill return Code : [{}]",
                    returnCode);
        }

        return tempByte.toByteArray();
    }
}
