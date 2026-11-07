package lan.chaos.finance.common.store;

import lan.chaos.finance.common.model.Subject;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 总账科目内存存储（演示用）。生产环境替换为 JPA/H2/真实数据库，接口不变。
 */
public class SubjectStore {
    private final Map<String, Subject> map = new ConcurrentHashMap<>();

    public Subject save(Subject subject) {
        map.put(subject.getCode(), subject);
        return subject;
    }

    public Subject get(String code) {
        Subject subject = map.get(code);
        if (subject == null) {
            throw new IllegalArgumentException("科目不存在: " + code);
        }
        return subject;
    }

    public Collection<Subject> all() {
        return map.values();
    }
}
