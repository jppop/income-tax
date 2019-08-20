package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;

@Value(staticConstructor = "of")
@JsonDeserialize
public class Income {

  public final long income;
  public final IncomeType incomeType;
  public final OffsetDateTime start;
  public final OffsetDateTime end;

  @JsonCreator
  public Income(long income, IncomeType incomeType, OffsetDateTime start, OffsetDateTime end) {
    Preconditions.checkArgument(income > 0, "income must be positive");
    this.income = income;
    this.incomeType = Preconditions.checkNotNull(incomeType, "incomeType");
    this.start = Preconditions.checkNotNull(start, "start");
    this.end = Preconditions.checkNotNull(end, "end");
  }

  public Income withAdjustedDates(OffsetDateTime start, OffsetDateTime end) {
    Preconditions.checkNotNull(start, "start");
    Preconditions.checkNotNull(end, "end");
    return new Income(this.income, this.incomeType, start, end);
  }
}
