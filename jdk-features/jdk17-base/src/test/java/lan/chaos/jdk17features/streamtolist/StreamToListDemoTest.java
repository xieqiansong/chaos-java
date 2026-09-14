package lan.chaos.jdk17features.streamtolist;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamToListDemoTest {

    @Test
    void toListImmutable() {
        List<Integer> list = Stream.of(1, 2, 3).filter(n -> n % 2 == 1).toList();
        assertEquals(List.of(1, 3), list);
        // toList() 返回不可变列表
        assertThrows(UnsupportedOperationException.class, () -> list.add(0));
    }

    @Test
    void mapMultiOneToMany() {
        List<Integer> expanded = Stream.of(1, 2)
                .mapMulti((Integer n, java.util.function.Consumer<Integer> c) -> {
                    c.accept(n);
                    c.accept(n * 10);
                })
                .toList();
        assertEquals(List.of(1, 10, 2, 20), expanded);
    }
}
