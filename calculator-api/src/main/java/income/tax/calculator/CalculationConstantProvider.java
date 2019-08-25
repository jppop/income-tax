package income.tax.calculator;

import java.math.BigDecimal;

public interface CalculationConstantProvider {
  BigDecimal getCalculationConstant(String name);

  String[] getContributionTypes();

}
