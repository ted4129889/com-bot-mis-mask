/* (C) 2024 */
package com.bot.mask.util.cobol;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class CobolField {

    public enum Type {
        DISPLAY,
        COMP, // Binary
        COMP3, // Packed Decimal
        X // Alias of DISPLAY
    }

    public String name;
    public Type type;
    public double digits;
    public int decimal; // 小數位數（只有 COMP3 有用到，其他可為 0）

    // 建構子：完整參數
    public CobolField(String name, Type type, double digits, int decimal) {
        this.name = name;
        this.type = type;
        this.decimal = decimal;
        this.digits = digits;
    }

    // 若沒小數位數，可簡寫
    public CobolField(String name, Type type, double digits) {
        this(name, type, digits, 0);
    }

    /** 是否為 Binary COMP 型別 */
    public boolean isBinaryComp() {
        return this.type == Type.COMP;
    }

    /** 實際佔用的 byte 數 */
    //    public int getByteLength() {
    //        return switch (type) {
    //            case DISPLAY, X -> digits * 2;
    //            case COMP -> getBinaryLengthForComp(digits);
    //            case COMP3 -> (int) Math.ceil((digits + 1) / 2.0);
    //        };
    //    }
    //
    //    /** HEX 長度（每 byte 對應 2 hex 字元） */
    //    public int getHexLength() {
    //        return getByteLength() * 2;
    //    }

    /** 判斷 Binary COMP 所需 byte 數 */
    private int getBinaryLengthForComp(double digits) {
        if (digits <= 4) return 2;
        else if (digits <= 9) return 4;
        else return 8;
    }
}
