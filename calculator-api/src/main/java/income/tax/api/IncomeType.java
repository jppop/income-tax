package income.tax.api;

import com.fasterxml.jackson.annotation.JsonValue;

public enum IncomeType {
  real("real"), estimated("estimated"), automatic("auto"), system("system");

  private final String name;

  IncomeType(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return this.name;
  }
}
