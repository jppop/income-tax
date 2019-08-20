package income.tax.impl.calculation;

import income.tax.api.Income;
import income.tax.api.IncomeType;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static income.tax.impl.tools.DateUtils.justBefore;
import static income.tax.impl.tools.DateUtils.maxLastDayOfMonth;

public final class IncomeAdjusters {

  public static IncomeAdjuster previousYear(Income income) {
    return (state) -> {

      // Apply new previous income
      PMap<Integer, Income> newPreviousYearlyIncome =
          state.previousYearlyIncomes.plus(income.start.getYear(), scaleToFullYear(income));
      // calculate new current year incomes
      PVector<Income> newIncomes =
          calculateIncomes(income, state.registeredDate, state.previousYearlyIncomes, state.currentIncomes);
      // calculate contributions
      PMap<ContributionType, Contribution> newContributions = calculateContributions(state.currentIncomes);
      return state.modifier()
          .withNewPreviousYearlyIncome(newPreviousYearlyIncome)
          .withNewCurrentIncomes(newIncomes)
          .withNewContributions(newContributions)
          .modify();
    };
  }

  public static IncomeAdjuster currentYear(Income income) {
    return (state) -> {

      // calculate new current year incomes
      PVector<Income> newIncomes =
          calculateIncomes(income, state.registeredDate, state.previousYearlyIncomes, state.currentIncomes);
      // calculate contributions
      PMap<ContributionType, Contribution> newContributions = calculateContributions(newIncomes);
      return state.modifier()
          .withNewCurrentIncomes(newIncomes)
          .withNewContributions(newContributions)
          .modify();
    };
  }

  private static PMap<ContributionType, Contribution> calculateContributions(PVector<Income> newIncomes) {
    return HashTreePMap.empty();
  }

  private static PVector<Income> calculateIncomes(
      Income income,
      OffsetDateTime registeredDate, PMap<Integer, Income> yearlyPreviousIncomes,
      PVector<Income> currentIncomes) {

    Map<Integer, Income> monthlyIncomes = new HashMap<>(12);

    // populate with the known incomes
    if (!currentIncomes.isEmpty()) {
      for (int month = 1; month <= 12; month++) {
        if (currentIncomes.contains(month)) {
          monthlyIncomes.put(month, currentIncomes.get(month));
        }
      }
    }

    // populates monthly income with incomes before registration
    Income incomeBeforeRegistration = beforeRegistration(income, registeredDate, yearlyPreviousIncomes);
    if (incomeBeforeRegistration.start.getYear() == income.start.getYear()) {
      for (int month = 1; month <= incomeBeforeRegistration.end.getMonthValue(); month++) {
        monthlyIncomes.put(month, scaleToMonth(incomeBeforeRegistration, Month.of(month)));
      }
    }

    // populate with the new income
    Income incomeToTheEndOfYear = scaleToEndOfYear(income);
    for (int month = incomeToTheEndOfYear.start.getMonthValue(); month <= incomeToTheEndOfYear.end.getMonthValue(); month++) {
      monthlyIncomes.put(month, scaleToMonth(incomeToTheEndOfYear, Month.of(month)));
    }

    // then verify there no missing month
    if (!monthlyIncomes.containsKey(1)) {
      int lastYear = income.start.getYear() - 1;
      if (yearlyPreviousIncomes.containsKey(lastYear)) {
        monthlyIncomes.put(1,
            scaleToMonth(yearlyPreviousIncomes.get(lastYear), income.start.getYear(), Month.JANUARY));
      }
    }
    // if any month is missing, use the previous one
    for (int month = 2; month <= 12; month++) {
      if (!monthlyIncomes.containsKey(month)) {
        Income previousMonth = monthlyIncomes.get(month - 1);
        monthlyIncomes.put(month, scaleToMonth(previousMonth, Month.of(month)));
      }
    }

    return TreePVector.from(new ArrayList(monthlyIncomes.values()));
  }

  private static Income beforeRegistration(
      Income income,
      OffsetDateTime registeredDate,
      PMap<Integer, Income> yearlyPreviousIncomes) {

    OffsetDateTime firstDayOfYear = income.start.withDayOfYear(1);
    if (registeredDate.isBefore(firstDayOfYear)) {
      return new Income(0, IncomeType.automatic, firstDayOfYear.minusYears(1), justBefore.apply(firstDayOfYear));
    }
    int lastYear = registeredDate.getYear() - 1;
    if (!yearlyPreviousIncomes.containsKey(lastYear)) {
      return new Income(0, IncomeType.automatic, firstDayOfYear.minusYears(1), justBefore.apply(firstDayOfYear));
    }
    Income lastYearIncome = yearlyPreviousIncomes.get(lastYear);
    OffsetDateTime end;
    if (income.start.isBefore(registeredDate)) {
      end = justBefore.apply(income.start);
    } else {
      end = justBefore.apply(registeredDate);
    }
    Period period = Period.between(firstDayOfYear.toLocalDate(), end.toLocalDate());
    BigDecimal incomeBeforeRegistration = BigDecimal.valueOf(lastYearIncome.income)
        .divide(BigDecimal.valueOf(12))
        .multiply(BigDecimal.valueOf(period.getMonths() + 1))
        .setScale(0, RoundingMode.DOWN);
    return new Income(incomeBeforeRegistration.longValue(), lastYearIncome.incomeType, firstDayOfYear, end);

  }

  private static Income scaleToFullYear(Income income) {
    return scale(income, income.start.with(TemporalAdjusters.firstDayOfYear()), income.end.with(TemporalAdjusters.lastDayOfYear()));
  }

  private static Income scaleToEndOfYear(Income income) {
    return scale(income, income.start, income.end.with(TemporalAdjusters.lastDayOfYear()));
  }

  private static Income scale(Income income, OffsetDateTime start, OffsetDateTime end) {
    Period period = Period.between(income.start.toLocalDate(), income.end.toLocalDate());
    int months = period.getMonths() + 1;
    if (months == 12) {
      return income;
    }
    Period newPeriod = Period.between(start.toLocalDate(), end.toLocalDate());
    BigDecimal yearlyIncome =
        BigDecimal.valueOf(income.income)
            .divide(BigDecimal.valueOf(months))
            .multiply(BigDecimal.valueOf(newPeriod.getMonths() + 1)
                .setScale(0, RoundingMode.DOWN));

    return new Income(yearlyIncome.longValue(), income.incomeType, start, end);
  }

  public static Income scaleToMonth(Income income, Month month) {
    return scaleToMonth(income, income.start.getYear(), month);
  }

  public static Income scaleToMonth(Income income, int year, Month month) {
    Period period = Period.between(income.start.toLocalDate(), income.end.toLocalDate());
    BigDecimal scaledIncome = BigDecimal.valueOf(income.income)
        .divide(BigDecimal.valueOf(period.getMonths() + 1))
        .setScale(0, RoundingMode.DOWN);
    LocalDate firstOfMonthDate = LocalDate.of(year, month, 1);
    OffsetDateTime firstOfMonth = OffsetDateTime.of(firstOfMonthDate, LocalTime.MIN, income.start.getOffset());
    return new Income(scaledIncome.longValue(), income.incomeType,
        firstOfMonth, maxLastDayOfMonth.apply(firstOfMonth));
  }
}
