package lan.chaos.redis.collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 集合类结构演示：Hash / List / Set ★★★。
 *
 * <ul>
 *     <li><b>Hash</b>：存储对象字段/购物车/用户属性（一个 key 下多个 field-value）。</li>
 *     <li><b>List</b>：有序可重复，常做最新消息、简单队列（左进右出）。</li>
 *     <li><b>Set</b>：无序去重，常做点赞、标签、共同好友（支持交集/并集）。</li>
 * </ul>
 *
 * <p><b>坑点：</b>Hash/Set 的 field 过多会变成「大 key」，拖慢集群迁移与删除；
 * List 若只进不出会无限增长，建议配合 {@code LTRIM} 截断保留最近 N 条。</p>
 */
@Service
public class CollectionService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /* ---------------- Hash ---------------- */

    public void hSet(String hashKey, String field, String value) {
        stringRedisTemplate.opsForHash().put(hashKey, field, value);
    }

    public Object hGet(String hashKey, String field) {
        return stringRedisTemplate.opsForHash().get(hashKey, field);
    }

    public Map<Object, Object> hGetAll(String hashKey) {
        return stringRedisTemplate.opsForHash().entries(hashKey);
    }

    public Long hDelete(String hashKey, String field) {
        return stringRedisTemplate.opsForHash().delete(hashKey, field);
    }

    /* ---------------- List ---------------- */

    /** 左侧入队（最新在前） */
    public Long lPush(String key, String value) {
        return stringRedisTemplate.opsForList().leftPush(key, value);
    }

    /** 右侧出队（配合 lPush 即 FIFO 队列） */
    public String rPop(String key) {
        return stringRedisTemplate.opsForList().rightPop(key);
    }

    public List<String> lRange(String key, long start, long end) {
        return stringRedisTemplate.opsForList().range(key, start, end);
    }

    public Long lSize(String key) {
        return stringRedisTemplate.opsForList().size(key);
    }

    /* ---------------- Set ---------------- */

    public Long sAdd(String key, String value) {
        return stringRedisTemplate.opsForSet().add(key, value);
    }

    public Boolean sIsMember(String key, String value) {
        return stringRedisTemplate.opsForSet().isMember(key, value);
    }

    public Set<String> sMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    public Long sRemove(String key, String value) {
        return stringRedisTemplate.opsForSet().remove(key, value);
    }
}
