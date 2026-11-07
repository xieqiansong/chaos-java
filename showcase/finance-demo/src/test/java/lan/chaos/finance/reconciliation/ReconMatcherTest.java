package lan.chaos.finance.reconciliation;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.constant.DiffType;
import lan.chaos.finance.common.model.Money;
import lan.chaos.finance.common.model.ReconRecord;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 对账解析、勾对、差异分类、差错处理断言测试。 */
class ReconMatcherTest {

    private final ReconParser parser = new ReconParser();
    private final ReconMatcher matcher = new ReconMatcher();

    @Test
    void 渠道文件解析跳过表头空行() {
        Map<String, Money> channel = parser.parseCsv(ReconParser.sampleChannelFile());
        assertThat(channel).hasSize(3);
        assertThat(channel.get("P001").compareTo(Money.of("100.00", Currency.CNY))).isEqualTo(0);
        assertThat(channel).doesNotContainKey("bizNo");
    }

    @Test
    void 四类差异全部正确检出() {
        Map<String, Money> channel = parser.parseCsv(ReconParser.sampleChannelFile());

        Map<String, Money> local = new LinkedHashMap<>();
        local.put("P001", Money.of("100.00", Currency.CNY)); // NONE
        local.put("P002", Money.of("199.00", Currency.CNY)); // MISMATCH
        local.put("P003", Money.of("30.00", Currency.CNY));  // LONG
        // P004 仅渠道有 → SHORT

        List<ReconRecord> records = matcher.match(local, channel);
        Map<DiffType, Long> counts = matcher.countByType(records);
        assertThat(counts.get(DiffType.NONE)).isEqualTo(1);
        assertThat(counts.get(DiffType.LONG)).isEqualTo(1);
        assertThat(counts.get(DiffType.SHORT)).isEqualTo(1);
        assertThat(counts.get(DiffType.MISMATCH)).isEqualTo(1);

        DiffHandler handler = new DiffHandler();
        handler.handle(records);
        assertThat(handler.getActions()).hasSize(3); // 三类差异各一条动作
    }

    @Test
    void 完全一致时无差错动作() {
        Map<String, Money> local = new LinkedHashMap<>();
        local.put("P001", Money.of("100.00", Currency.CNY));
        Map<String, Money> channel = new LinkedHashMap<>();
        channel.put("P001", Money.of("100.00", Currency.CNY));
        List<ReconRecord> records = matcher.match(local, channel);
        assertThat(records.get(0).getDiffType()).isEqualTo(DiffType.NONE);
        DiffHandler handler = new DiffHandler();
        handler.handle(records);
        assertThat(handler.getActions()).isEmpty();
    }

    @Test
    void 解析容错脏数据() {
        Map<String, Money> channel = parser.parseCsv(Arrays.asList(
                "  P009 , 88.80 ", "   ", "badline", null));
        // 空行、缺列行、null 行被跳过；带空白的字段被 trim
        assertThat(channel).hasSize(1);
        assertThat(channel.get("P009").compareTo(Money.of("88.80", Currency.CNY))).isEqualTo(0);
    }
}
