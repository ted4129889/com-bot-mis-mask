/* (C) 2024 */
package com.bot.mask.util.xml.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
/** 封裝Output定義檔的內容格式 */
public class FieldProperties {
    String fieldName;
    String fieldType;
    String format;
    int length;
    String align;
    String oTableName;
    String oFieldName;
}
