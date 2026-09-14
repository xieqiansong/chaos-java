package lan.chaos.jdk25features.gatherers;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatherersDemoTest {

    @Test
    void builtInWindowFixed() {
        List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5)
                .gather(Gatherers.windowFixed(2))
                .toList();
        assertEquals(List.of(1, 2), windows.get(0));
        assertEquals(List.of(5), windows.get(2));
    }

    @Test
    void customAdjacentDelta() {
        List<Integer> deltas = Stream.of(1, 4, 2, 7)
                .gather(GatherersDemo.adjacentDelta())
                .toList();
        assertEquals(List.of(3, -2, 5), deltas);
    }
}
