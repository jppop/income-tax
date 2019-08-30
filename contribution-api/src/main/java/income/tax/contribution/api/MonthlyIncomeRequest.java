package income.tax.contribution.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Month;
import java.util.Map;
import java.util.Optional;

@Value
@JsonDeserialize
public class MonthlyIncomeRequest {
  public final @NonNull
  BigDecimal income;
  public final
  int year;
  public final @NonNull
  Month month;
  public final boolean round;

  // further usage
  public final
  Optional<Map<String, Object>> additionalArgs;

  @JsonCreator
  public MonthlyIncomeRequest(BigDecimal income, int year, Month month, boolean round, Optional<Map<String, Object>> additionalArgs) {
    this.income = income;
    this.year = year;
    this.month = month;
    this.round = round;
    this.additionalArgs = additionalArgs;
  }
}
