/* (C) 2023 */
package com.bot.txcontrol.buffer.node;

import java.util.List;
import lombok.Data;

@Data
public class ResMapping {

    private String serviceName;

    private List<String> msgCode;
}
