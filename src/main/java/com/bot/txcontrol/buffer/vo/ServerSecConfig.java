/* (C) 2023 */
package com.bot.txcontrol.buffer.vo;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "grpc")
public class ServerSecConfig {
    private List<ServerSec> serverSecLi;
}
