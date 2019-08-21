package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

@Value
@JsonDeserialize
public class Income {

  public static final Income ZERO = new Income(0, IncomeType.system, OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));

  public final long income;
  public final IncomeType incomeType;
  public final OffsetDateTime start;
  public final OffsetDateTime end;

  @JsonCreator
  public Income(long income, IncomeType incomeType, OffsetDateTime start, OffsetDateTime end) {
    Preconditions.checkArgument(income >= 0, "income must be positive");
    this.income = income;
    this.incomeType = Preconditions.checkNotNull(incomeType, "incomeType");
    this.start = Preconditions.checkNotNull(start, "start");
    this.end = Preconditions.checkNotNull(end, "end");
  }

  public static Income zero(int year, Month month) {
    OffsetDateTime monthStart =
        OffsetDateTime.of(
            LocalDate.of(year, month, 1),
            LocalTime.MIN,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());
    OffsetDateTime monthEnd =
        OffsetDateTime.of(
            LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth()),
            LocalTime.MAX,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());
    return new Income(0, IncomeType.system, monthStart, monthEnd);
  }
}
