package income.tax.impl.tools;

import income.tax.api.Income;
import income.tax.api.IncomeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IncomeUtils {

  public static Income scaleToFullYear(Income income) {
    return scaleToFullYear(income, income.start.getYear());
  }

  public static Income yearIncome(long income, int year, IncomeType incomeType) {
    LocalDate firstDay = LocalDate.of(year, 1, 1);
    OffsetDateTime firstDayTime = OffsetDateTime.of(firstDay, LocalTime.MIN, ZoneOffset.UTC);
    OffsetDateTime lastDayTime = OffsetDateTime.of(firstDay.with(TemporalAdjusters.lastDayOfYear()), LocalTime.MAX, ZoneOffset.UTC);
    return new Income(income, incomeType, firstDayTime, lastDayTime);
  }

  public static Income scaleToFullYear(Income income, int year) {
    LocalDate firstDay = LocalDate.of(year, 1, 1);
    OffsetDateTime firstDayTime = OffsetDateTime.of(firstDay, LocalTime.MIN, ZoneOffset.UTC);
    OffsetDateTime lastDayTime = OffsetDateTime.of(firstDay.with(TemporalAdjusters.lastDayOfYear()), LocalTime.MAX, ZoneOffset.UTC);
    return scale(income, firstDayTime, lastDayTime);
  }

  public static Income scaleToEndOfYear(Income income) {
    return scale(
        income,
        DateUtils.minFirstDayOfMonth.apply(income.start), income.end.with(TemporalAdjusters.lastDayOfYear()));
  }

  public static Income toCompleteMonths(Income income) {
    OffsetDateTime start = DateUtils.minFirstDayOfMonth.apply(income.start);
    OffsetDateTime end = DateUtils.maxLastDayOfMonth.apply(income.start);
    return new Income(income.income, income.incomeType, start, end);
  }

  private static Income scale(Income income, OffsetDateTime start, OffsetDateTime end) {
    Period period = Period.between(income.start.toLocalDate(), income.end.toLocalDate());
    int months = period.getMonths() + 1;
    Period newPeriod = Period.between(start.toLocalDate(), end.toLocalDate());
    BigDecimal yearlyIncome =
        BigDecimal.valueOf(income.income)
            .multiply(BigDecimal.valueOf(newPeriod.getMonths() + 1))
            .divide(BigDecimal.valueOf(months), RoundingMode.DOWN);

    return new Income(yearlyIncome.longValue(), income.incomeType, start, end);
  }

  public static Map<Month, Income> spreadOutOverMonths(Income income) {

    // scale the income value
    Period period = Period.between(income.start.toLocalDate(), income.end.toLocalDate());
    int months = period.getMonths() + 1;
    if (months == 1) {
      return Collections.singletonMap(income.start.getMonth(), income);
    }
    Map<Month, Income> incomes = new HashMap<>(months);
    long scaledIncome = BigDecimal.valueOf(income.income)
        .divide(BigDecimal.valueOf(months), RoundingMode.DOWN)
        .longValue();

    // add the scaled income until the last month minus 1
    ZoneOffset offset = income.start.getOffset();
    LocalDate firstOfMonthDate = LocalDate.of(income.start.getYear(), income.start.getMonthValue(), 1);
    for (int month = income.start.getMonthValue(); month < income.end.getMonthValue(); month++) {
      OffsetDateTime startDay = OffsetDateTime.of(firstOfMonthDate, LocalTime.MIN, offset);
      OffsetDateTime endDay = OffsetDateTime.of(firstOfMonthDate.with(TemporalAdjusters.lastDayOfMonth()), LocalTime.MAX, offset);
      incomes.put(firstOfMonthDate.getMonth(), new Income(scaledIncome, income.incomeType, startDay, endDay));
      firstOfMonthDate = firstOfMonthDate.plusMonths(1);
    }
    // add the remainder to the last month
    long remainder = income.income - (scaledIncome * months);
    OffsetDateTime startDay = OffsetDateTime.of(firstOfMonthDate, LocalTime.MIN, offset);
    OffsetDateTime endDay = OffsetDateTime.of(firstOfMonthDate.with(TemporalAdjusters.lastDayOfMonth()), LocalTime.MAX, offset);
    incomes.put(income.end.getMonth(), new Income(scaledIncome + remainder, income.incomeType, startDay, endDay));
    return incomes;
  }
}
