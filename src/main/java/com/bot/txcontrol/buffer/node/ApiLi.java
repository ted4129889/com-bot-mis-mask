/* (C) 2023 */
package com.bot.txcontrol.buffer.node;

import java.util.List;
import lombok.Data;

@Data
public class ApiLi {

    private String name;

    private String fmtId;

    private String txType;

    private String exProc;

    private String transactionTimeOut;

    private List<Service> service;
}
