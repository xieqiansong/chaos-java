package lan.chaos.flink.cdc.sync.mapping;

import java.io.Serializable;
import java.util.function.Function;

@FunctionalInterface
public interface SerializableFunction<T, R> extends Function<T, R>, Serializable {
    // 接口体为空，仅用于组合Function和Serializable
}
