package lan.chaos.security.jwt;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @Test
    void issueThenParseReturnsSubject() {
        String token = JwtService.issue("alice", 3600);
        assertEquals("alice", JwtService.parseSubject(token), "合法 Token 应解析出 subject");
        assertTrue(token.split("\\.").length == 3, "JWT 应为 header.payload.signature 三段");
    }

    @Test
    void expiredTokenThrows() throws InterruptedException {
        // 签发 1 秒过期的 Token，等其过期后解析应抛异常
        String token = JwtService.issue("alice", 1);
        Thread.sleep(1200);
        assertThrows(JwtException.class, () -> JwtService.parseSubject(token),
                "过期 Token 解析应抛异常");
    }

    @Test
    void tamperedTokenThrows() {
        String token = JwtService.issue("alice", 3600);
        String bad = token.substring(0, token.length() - 2) + (token.endsWith("aa") ? "bb" : "aa");
        assertThrows(JwtException.class, () -> JwtService.parseSubject(bad),
                "被篡改签名的 Token 应解析失败");
    }
}
