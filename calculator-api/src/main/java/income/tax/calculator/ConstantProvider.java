package income.tax.calculator;

import java.math.BigDecimal;

public interface ConstantProvider {
  BigDecimal getCalculationConstant(String name);
}
