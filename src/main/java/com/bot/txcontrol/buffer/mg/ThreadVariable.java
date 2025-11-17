/* (C) 2023 */
package com.bot.txcontrol.buffer.mg;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import lombok.Getter;
import lombok.Setter;

/**
 * 各線程變數
 *
 * @author AdamPan
 */
@Getter
@Setter
public class ThreadVariable {
    private static final ConcurrentMap<String, Future<?>> taskRegistry = new ConcurrentHashMap<>();

    private static ThreadLocal<Map<String, Object>> threadLocal =
            new ThreadLocal<Map<String, Object>>();

    public static Object getObject(String id) {
        if (threadLocal.get() == null) return null;

        return threadLocal.get().get(id);
    }

    public static void setObject(String id, Object value) {
        if (Objects.isNull(value)) return;

        if (threadLocal.get() == null) {
            threadLocal.set(new LinkedHashMap<String, Object>());
            threadLocal.get().put(id, value);
        } else threadLocal.get().put(id, value);
    }

    public static void clearThreadLocal() {
        if (!Objects.isNull(threadLocal.get())) threadLocal.get().clear();
        threadLocal.remove();
    }

    public static ConcurrentMap getTaskRegistry() {
        return taskRegistry;
    }
}
