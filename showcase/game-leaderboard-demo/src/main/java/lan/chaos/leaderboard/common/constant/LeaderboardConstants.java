package lan.chaos.leaderboard.common.constant;

/**
 * 排行榜 Key / 容量设计常量。
 *
 * <p>Redis ZSET 的 key 统一前缀，避免与其它业务冲突；各能力包各自持有独立的 board
 * （通过 {@link #boardKey(String)} 区分），互不影响。</p>
 */
public final class LeaderboardConstants {

    private LeaderboardConstants() {
    }

    /** ZSET 排行榜 key 前缀 */
    public static final String BOARD_PREFIX = "leaderboard:game:zset:";

    /** 双层缓存里 Top100 的缓存 key */
    public static final String CACHE_TOP100_KEY = "leaderboard:cache:top100";

    /** 按 board 名生成 Redis ZSET key */
    public static String boardKey(String name) {
        return BOARD_PREFIX + name;
    }
}
