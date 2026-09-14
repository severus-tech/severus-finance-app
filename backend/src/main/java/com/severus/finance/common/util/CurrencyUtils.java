package com.severus.finance.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyUtils {

    public static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, DEFAULT_ROUNDING);
    }

    public static BigDecimal format(BigDecimal amount) {
        if (amount == null) {
            return zero();
        }
        return amount.setScale(2, DEFAULT_ROUNDING);
    }

    public static BigDecimal add(BigDecimal b1, BigDecimal b2) {
        BigDecimal v1 = b1 != null ? b1 : BigDecimal.ZERO;
        BigDecimal v2 = b2 != null ? b2 : BigDecimal.ZERO;
        return v1.add(v2).setScale(2, DEFAULT_ROUNDING);
    }

    public static BigDecimal subtract(BigDecimal b1, BigDecimal b2) {
        BigDecimal v1 = b1 != null ? b1 : BigDecimal.ZERO;
        BigDecimal v2 = b2 != null ? b2 : BigDecimal.ZERO;
        return v1.subtract(v2).setScale(2, DEFAULT_ROUNDING);
    }
}
