package lan.chaos.finance.reconciliation;

import lan.chaos.finance.common.constant.Currency;
import lan.chaos.finance.common.model.Money;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道对账文件解析（P3，对应对话「对账文件解析」）。
 *
 * <p>WHY：渠道（银联/网联/微信）每日下发对账文件，格式通常是 CSV 或固定宽度文本。
 * 解析是差错处理的第一步，必须容错：跳过空行与表头、忽略多余空白、金额用 String
 * 构造（Level 0 的规矩，绝不走 double）。
 */
public class ReconParser {

    /** 解析 CSV 行：{@code bizNo,amount}。返回 bizNo → 金额，保持文件顺序。 */
    public Map<String, Money> parseCsv(List<String> lines) {
        Map<String, Money> result = new LinkedHashMap<>();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < 2) {
                continue;
            }
            String bizNo = parts[0].trim();
            String amount = parts[1].trim();
            if (bizNo.isEmpty() || "bizNo".equalsIgnoreCase(bizNo)) {
                continue; // 跳过表头
            }
            result.put(bizNo, Money.of(amount, Currency.CNY));
        }
        return result;
    }

    /** 样例渠道对账文件（含表头、空行，演示容错）。 */
    public static List<String> sampleChannelFile() {
        List<String> lines = new ArrayList<>();
        lines.add("bizNo,amount");
        lines.add("P001,100.00");
        lines.add("P002,200.00");
        lines.add("");
        lines.add("P004,50.00");
        return lines;
    }
}
