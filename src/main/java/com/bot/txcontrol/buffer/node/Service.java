/* (C) 2023 */
package com.bot.txcontrol.buffer.node;

import java.util.List;
import lombok.Data;

@Data
public class Service {

    private String name;

    private String rName;

    private String exProc;

    private String mapper;

    private String funcName;

    private List<ResMapping> resMapping;

    private boolean commit;

    private boolean wait = false;
}
