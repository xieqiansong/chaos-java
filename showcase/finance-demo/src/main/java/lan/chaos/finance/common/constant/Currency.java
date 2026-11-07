package lan.chaos.finance.common.constant;

/**
 * 币种常量。
 *
 * <p>WHY：金融金额必须绑定币种，且每种币种有固定的最小计量单位（小数位）。
 * 例如人民币 CNY 默认 2 位小数（精确到分）。不同币种不可直接加减，否则会出现
 * "100 日元 + 100 人民币" 这种无意义运算 —— 这是资金准确性的第一道防线。
 */
public enum Currency {

    CNY("156", 2),
    USD("840", 2);

    private final String isoCode;
    private final int defaultScale;

    Currency(String isoCode, int defaultScale) {
        this.isoCode = isoCode;
        this.defaultScale = defaultScale;
    }

    public String getIsoCode() {
        return isoCode;
    }

    /** 该币种默认小数位（如 CNY=2，即精确到分）。 */
    public int getDefaultScale() {
        return defaultScale;
    }
}
