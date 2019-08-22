package income.tax.impl.contribution;

import lombok.Value;

import java.math.BigDecimal;

public interface CalculatorConfig {

  int getYear();
  BigDecimal round(BigDecimal value);

  @FunctionalInterface
  interface IncomeBasedCalculator {
    BigDecimal compute(long income);
  }

  @FunctionalInterface
  interface ContributionCalculator {
    BigDecimal compute(long income, BigDecimal baseIncome, BigDecimal rate);
  }

  @Value
  class ContributionConfig {
    public final IncomeBasedCalculator baseIncomeCalculator;
    public final IncomeBasedCalculator rateCalculator;
    public final ContributionCalculator contributionCalculator;

    public BigDecimal compute(long income) {
      BigDecimal baseIncome = baseIncomeCalculator.compute(income);
      BigDecimal rate = rateCalculator.compute(income);
      return contributionCalculator.compute(income, baseIncome, rate);
    }
  }
}
