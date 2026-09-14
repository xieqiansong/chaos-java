package lan.chaos.jdk8features.completablefuture;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletableFutureDemoTest {

    @Test
    void combineAndChain() throws Exception {
        CompletableFuture<Integer> a = CompletableFuture.supplyAsync(() -> 10);
        CompletableFuture<Integer> b = CompletableFuture.supplyAsync(() -> 20);
        assertEquals(30, a.thenCombine(b, Integer::sum).get().intValue());

        String chained = CompletableFuture.supplyAsync(() -> "hello")
                .thenApply(s -> s + " world")
                .thenApply(String::toUpperCase)
                .get();
        assertEquals("HELLO WORLD", chained);
    }
}
