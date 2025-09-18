/* (C) 2024 */
package com.bot.mask.mask.vo;

import com.bot.mask.util.xml.mask.xmltag.Field;
import com.bot.mask.util.xml.mask.xmltag.Mapping;
import com.bot.mask.util.xml.mask.xmltag.Table;
import com.bot.mask.util.xml.vo.XmlHeaderBodyFooter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class MaskSqlVo {


   public String tableName;

   public String fileName;
}
