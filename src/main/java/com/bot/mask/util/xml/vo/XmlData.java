/* (C) 2024 */
package com.bot.mask.util.xml.vo;

import com.bot.mask.util.xml.mask.xmltag.Field;
import com.bot.mask.util.xml.mask.xmltag.Mapping;
import com.bot.mask.util.xml.mask.xmltag.Table;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class XmlData {


    @JacksonXmlProperty(localName = "header")
    private XmlHeaderBodyFooter header;

    @JacksonXmlProperty(localName = "body")
    private XmlHeaderBodyFooter body;

    @JacksonXmlProperty(localName = "footer")
    private XmlHeaderBodyFooter footer;


    @JacksonXmlProperty(localName = "table")
    private Table table;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "field")
    private List<Field> fieldList = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "mapping")
    private List<Mapping> mappingList = new ArrayList<>();

    @JacksonXmlProperty(isAttribute = true, localName = "fileName")
    public String fileName;

    @JacksonXmlProperty(localName = "paramDate")
    private String  paramDate;

    @JacksonXmlProperty(localName = "suffix")
    private String  suffix;
    // Getters and Setters
}
