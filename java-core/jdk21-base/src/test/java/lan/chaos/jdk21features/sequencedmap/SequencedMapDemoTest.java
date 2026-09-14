package lan.chaos.jdk21features.sequencedmap;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SequencedMapDemoTest {

    @Test
    void firstLastReversedAndPut() {
        SequencedMap<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        assertEquals("one", map.firstEntry().getKey());
        assertEquals("three", map.lastEntry().getKey());
        assertEquals(List.of("three", "two", "one"), new ArrayList<>(map.reversed().keySet()));

        map.putFirst("zero", 0);
        assertEquals("zero", map.firstEntry().getKey());
        map.putLast("four", 4);
        assertEquals("four", map.lastEntry().getKey());
    }
}
