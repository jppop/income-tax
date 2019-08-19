package income.tax.api;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IncomeType {
  PreviousYearlyIncome("previous-yearly-income"), MonthlyIncome("monthly-income");

  private final String name;

  IncomeType(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return this.name;
  }
}
