package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Value;

import java.util.Optional;

@Value(staticConstructor = "of")
@JsonDeserialize
public class Income {

  public final long income;
  public final IncomeType incomeType;
  public final Optional<Integer> year;
  public final Optional<Integer> month;

  @JsonCreator
  public Income(long income, IncomeType incomeType, Optional<Integer> year, Optional<Integer> month) {
    this.income = income;
    this.incomeType = incomeType;
    this.year = year;
    this.month = month;

  }
}
