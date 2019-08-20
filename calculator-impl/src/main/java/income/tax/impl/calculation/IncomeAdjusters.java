package income.tax.impl.calculation;

import income.tax.api.Income;
import income.tax.api.IncomeType;
import income.tax.impl.IncomeTaxState;
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

      // FIXME: updateIncome should use a copy of state.yearlyPreviousIncomes (given as an argument)
      // so, we can avoid creating a new state
      IncomeTaxState newState = new IncomeTaxState(state.contributorId, state.registeredDate,
          state.yearlyPreviousIncomes.plus(income.start.getYear(), scaleToFullYear(income)),
          state.contributionYear, state.currentIncomes, state.contributions);
      PMap<ContributionType, Contribution> newContributions = newContributions(state, state.currentIncomes);
      return new IncomeTaxState(state.contributorId, state.registeredDate,
          newState.yearlyPreviousIncomes,
          state.contributionYear, state.currentIncomes, newContributions);
    };
  }

  public static IncomeAdjuster month(Income income) {
    return (state) -> {

      PVector<Income> newIncomes = updateIncomes(state, income);
      PMap<ContributionType, Contribution> newContributions = newContributions(state, newIncomes);
      return new IncomeTaxState(state.contributorId, state.registeredDate,
          state.yearlyPreviousIncomes.plus(income.start.getYear(), income),
          state.contributionYear, newIncomes, newContributions);
    };
  }

  private static PMap<ContributionType, Contribution> newContributions(IncomeTaxState state, PVector<Income> newIncomes) {
    return state.contributions;
  }

  private static PVector<Income> updateIncomes(IncomeTaxState state, Income income) {
    Map<Integer, Income> monthlyIncomes = new HashMap<>(12);

    // populate with the known incomes
    for (int month = 1; month <= 12; month++) {
      if (state.currentIncomes.contains(month)) {
        monthlyIncomes.put(month, state.currentIncomes.get(month));
      }
    }

    // populates monthly income with incomes before registration
    Income incomeBeforeRegistration = beforeRegistration(state, income);
    if (incomeBeforeRegistration.start.getYear() == income.start.getYear()) {
      for (int month = 1; month <= incomeBeforeRegistration.end.getMonthValue(); month++) {
        monthlyIncomes.put(month, scaleToMonth(incomeBeforeRegistration, Month.of(month)));
      }
    }

    // populate with the new income
    Income incomeToTheEndOfYear = scaleToEndOfYear(income);
    for (int month = income.start.getMonthValue(); month <= income.end.getMonthValue(); month++) {
      monthlyIncomes.put(month, scaleToMonth(incomeToTheEndOfYear, Month.of(month)));
    }

    // then verify there no missing month
    if (!monthlyIncomes.containsKey(1)) {
      int lastYear = income.start.getYear() - 1;
      if (state.yearlyPreviousIncomes.containsKey(lastYear)) {
        monthlyIncomes.put(1,
            scaleToMonth(state.yearlyPreviousIncomes.get(lastYear), income.start.getYear(), Month.JANUARY));
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

  private static Income beforeRegistration(IncomeTaxState state, Income income) {
    OffsetDateTime firstDayOfYear = income.start.withDayOfYear(1);
    if (state.registeredDate.isBefore(firstDayOfYear)) {
      return new Income(0, IncomeType.automatic, firstDayOfYear.minusYears(1), justBefore.apply(firstDayOfYear));
    }
    int lastYear = state.registeredDate.getYear() - 1;
    if (!state.yearlyPreviousIncomes.containsKey(lastYear)) {
      return new Income(0, IncomeType.automatic, firstDayOfYear.minusYears(1), justBefore.apply(firstDayOfYear));
    }
    Income lastYearIncome = state.yearlyPreviousIncomes.get(lastYear);
    OffsetDateTime end;
    if (income.start.isBefore(state.registeredDate)) {
      end = justBefore.apply(income.start);
    } else {
      end = justBefore.apply(state.registeredDate);
    }
    Period period = Period.between(firstDayOfYear.toLocalDate(), end.toLocalDate());
    BigDecimal incomeBeforeRegistration = BigDecimal.valueOf(lastYearIncome.income, 2)
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
        BigDecimal.valueOf(income.income, 2)
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
    BigDecimal scaledIncome = BigDecimal.valueOf(income.income, 2)
        .divide(BigDecimal.valueOf(period.getMonths() + 1))
        .setScale(0, RoundingMode.DOWN);
    LocalDate firstOfMonthDate = LocalDate.of(year, month, 1);
    OffsetDateTime firstOfMonth = OffsetDateTime.of(firstOfMonthDate, LocalTime.MIN, income.start.getOffset());
    return new Income(scaledIncome.longValue(), income.incomeType,
        firstOfMonth, maxLastDayOfMonth.apply(firstOfMonth));
  }
}
