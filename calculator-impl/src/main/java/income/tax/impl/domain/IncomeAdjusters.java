package income.tax.impl.domain;

import income.tax.api.Income;
import income.tax.api.IncomeType;
import income.tax.calculator.Calculator;
import income.tax.calculator.Contribution;
import income.tax.impl.CalculationModule;
import income.tax.impl.contribution.Calculator2018;
import income.tax.impl.tools.IncomeUtils;
import org.pcollections.HashTreePMap;
import org.pcollections.IntTreePMap;
import org.pcollections.PMap;

import java.math.BigDecimal;
import java.time.Month;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class IncomeAdjusters {

  private static Map<Integer, Calculator> calculators;

  static {
    calculators = CalculationModule.loadCalculators();
    if (calculators.isEmpty()) {
      // FIXME: Should stop the application
      calculators.put(2018, new Calculator2018());
    }
  }

  /**
   * Tax state at registration time.
   *
   * @param income
   * @return the new state
   */
  public static IncomeAdjuster beforeRegistration(Income income) {
    return (state) -> {

      // apply income before registration to the current year (ie, the registration year)
      // scale income to a full year
      Income yearlyIncome = IncomeUtils.scaleToFullYear(income, state.registeredDate.getYear());

      // spread out income over every month
      Map<Month, Income> spreadIncome = IncomeUtils.spreadOutOverMonths(yearlyIncome);
      PMap<Month, Income> newIncomes = HashTreePMap.from(spreadIncome);

      // register the yearly income before registration
      Income incomeBeforeRegistration = Income.ofYear(income.income, state.contributionYear - 1, income.incomeType);

      // calculate contributions
      Map<Month, PMap<String, Contribution>> newContributions =
          calculateContributions(state.contributionYear, spreadIncome);
      ContributionState newContributionState = state.contributions.update(newContributions);

      // return new state
      return state.modifier()
          .withNewPreviousYearlyIncome(IntTreePMap.singleton(state.contributionYear - 1, incomeBeforeRegistration))
          .withNewCurrentIncomes(newIncomes)
          .withNewContributions(newContributionState)
          .modify();
    };
  }

  /**
   * Mutates state with a new income declaration.
   *
   * @param income
   * @return the new state
   */
  public static IncomeAdjuster currentYear(Income income) {
    return (state) -> {

      // calculate new current year incomes
      Map<Month, Income> spreadIncome = IncomeUtils.spreadOutOverMonths(income);
      PMap<Month, Income> newIncomes = state.currentIncomes.plusAll(spreadIncome);

      // calculate contributions
      Map<Month, PMap<String, Contribution>> newContributions =
          calculateContributions(state.contributionYear, spreadIncome);
      ContributionState newContributionState = state.contributions.update(newContributions);

      // return new state
      return state.modifier()
          .withNewCurrentIncomes(newIncomes)
          .withNewContributions(newContributionState)
          .modify();
    };
  }

  /**
   * Start a new year. Incomes of the current year are summed and spread over the next year.
   * Current yearly income is backed up into the list of previous year incomes.
   *
   * @return the new state
   */
  public static IncomeAdjuster newYear() {
    return (state) -> {

      // next year
      int nextYear = state.contributionYear + 1;

      // sum the income
      long currentYearlyIncome = state.currentIncomes.values().stream().mapToLong(income -> income.income).sum();
      Income nextYearlyIncome = Income.ofYear(currentYearlyIncome, nextYear, IncomeType.system);

      // register the current yearly income into the previous yearly income list
      PMap<Integer, Income> newPreviousYearlyIncome =
          state.previousYearlyIncomes.plus(state.contributionYear,
              Income.ofYear(currentYearlyIncome, state.contributionYear, IncomeType.system));

      // spread out income over every month
      Map<Month, Income> spreadIncome = IncomeUtils.spreadOutOverMonths(nextYearlyIncome);
      PMap<Month, Income> newIncomes = HashTreePMap.from(spreadIncome);

      // calculate contributions
      Map<Month, PMap<String, Contribution>> newContributions =
          calculateContributions(state.contributionYear, spreadIncome);
      ContributionState newContributionState = state.contributions.update(newContributions);

      // mutate state
      return state.modifier()
          .withNewContributionYear(nextYear)
          .withNewPreviousYearlyIncome(newPreviousYearlyIncome)
          .withNewCurrentIncomes(newIncomes)
          .withNewContributions(newContributionState)
          .modify();
    };
  }

  private static
  Map<Month, PMap<String, Contribution>> calculateContributions(int contributionYear, Map<Month, Income> newIncomes) {

    if (newIncomes.isEmpty()) {
      return Collections.emptyMap();
    }

    // get the calculator
    Calculator calculator = calculators.get(contributionYear);
    if (calculator == null) {
      Optional<Calculator> maybeCalculator = calculators.values().stream().findFirst();
      calculator = maybeCalculator.orElseThrow(() -> new IllegalStateException("Something went really bad. No calculator defined"));
    }
    Map<Month, PMap<String, Contribution>> contributions = new HashMap<>();
    for (Map.Entry<Month, Income> entry : newIncomes.entrySet()) {
      Month month = entry.getKey();
      Income income = entry.getValue();
      Map<String, Contribution> monthContributions =
          calculator.computeFromMonthlyIncome(new BigDecimal(income.income), false);
      contributions.put(month, HashTreePMap.from(monthContributions));
    }
    return contributions;
  }

}
