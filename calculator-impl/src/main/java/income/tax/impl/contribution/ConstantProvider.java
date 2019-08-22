package income.tax.impl.contribution;

import java.math.BigDecimal;

public interface ConstantProvider {
  BigDecimal getCalculationConstant(String name);
}
