package lan.chaos.jdk21features.sequencedcollection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.SequencedSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SequencedCollectionDemoTest {

    @Test
    void firstLastAndReversed() {
        SequencedCollection<String> seq = new ArrayList<>(List.of("a", "b", "c"));
        assertEquals("a", seq.getFirst());
        assertEquals("c", seq.getLast());

        SequencedCollection<String> rev = seq.reversed();
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(rev));
        // reversed() 是视图，不修改原集合
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(seq));
    }

    @Test
    void sequencedSetReversed() {
        SequencedSet<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3));
        assertEquals(List.of(3, 2, 1), new ArrayList<>(set.reversed()));
    }
}
