package income.tax.contribution.impl;

import java.math.BigDecimal;

public interface CalculationConstantProvider {
  BigDecimal getCalculationConstant(String name);

  String[] getContributionTypes();

}
