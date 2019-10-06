package income.tax.contribution.impl;

import lombok.Value;

import java.math.BigDecimal;
import java.time.Month;
import java.util.Map;
import java.util.Optional;

public interface Calculator {

  int getYear();
  BigDecimal round(BigDecimal value);

  Map<String, ContributionInternal> computeFromYearlyIncome(BigDecimal income);

  Map<String, ContributionInternal> computeFromMonthlyIncome(Month month, BigDecimal income, Optional<Map<String, Object>> additionalArgs);

  @FunctionalInterface
  interface IncomeBasedCalculator {
    BigDecimal compute(BigDecimal income);
  }

  @FunctionalInterface
  interface ContributionCalculator {
    BigDecimal compute(BigDecimal income, BigDecimal baseIncome, BigDecimal rate);
  }

  @Value
  class ContributionConfig {
    public final IncomeBasedCalculator baseIncomeCalculator;
    public final IncomeBasedCalculator rateCalculator;
    public final ContributionCalculator contributionCalculator;

    public BigDecimal compute(BigDecimal income) {
      BigDecimal baseIncome = baseIncomeCalculator.compute(income);
      BigDecimal rate = rateCalculator.compute(income);
      return contributionCalculator.compute(income, baseIncome, rate);
    }
  }
}
