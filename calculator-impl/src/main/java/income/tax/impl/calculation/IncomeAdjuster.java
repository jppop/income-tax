package income.tax.impl.calculation;

import income.tax.impl.IncomeTaxState;

@FunctionalInterface
public interface IncomeAdjuster {
  IncomeTaxState adjust(IncomeTaxState state);
}
