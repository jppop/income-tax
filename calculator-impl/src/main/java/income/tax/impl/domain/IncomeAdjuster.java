package income.tax.impl.domain;

@FunctionalInterface
public interface IncomeAdjuster {
  IncomeTaxState adjust(IncomeTaxState state);
}
