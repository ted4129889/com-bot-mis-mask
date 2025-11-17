/* (C) 2023 */
package com.bot.txcontrol.eum;

public enum Env {
    LOCAL("local"),
    DEV("dev"),
    UAT("uat"),
    SIT("sit"),
    PROD("prod");

    private final String env;

    Env(final String env) {
        this.env = env;
    }

    public String getCode() {
        return this.env;
    }
}
