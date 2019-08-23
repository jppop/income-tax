package income.tax.impl.domain;

import income.tax.api.Income;
import income.tax.api.IncomeType;
import income.tax.calculator.Calculator;
import income.tax.calculator.Contribution;
import income.tax.impl.CalculationModule;
import income.tax.impl.tools.IncomeUtils;
import org.pcollections.HashTreePMap;
import org.pcollections.IntTreePMap;
import org.pcollections.PMap;

import java.util.Map;

public final class IncomeAdjusters {

  private static Map<Integer, Calculator> calculators;

  static {
    calculators = CalculationModule.loadCalculators();
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

      // spread out over income every month
      PMap<Integer, Income> newIncomes = applyIncome(yearlyIncome, IntTreePMap.empty());

      // register the yearly income before registration
      Income incomeBeforeRegistration = Income.ofYear(income.income, state.contributionYear - 1, income.incomeType);

      // return new state
      return state.modifier()
          .withNewPreviousYearlyIncome(IntTreePMap.singleton(state.contributionYear - 1, incomeBeforeRegistration))
          .withNewCurrentIncomes(newIncomes)
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
      PMap<Integer, Income> newIncomes = applyIncome(income, state.currentIncomes);

      // calculate contributions
      PMap<String, Contribution> newContributions = calculateContributions(newIncomes);

      // return new state
      return state.modifier()
          .withNewCurrentIncomes(newIncomes)
          .withNewContributions(newContributions)
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

      // spread out over income every month
      PMap<Integer, Income> newIncomes = applyIncome(nextYearlyIncome, IntTreePMap.empty());

      // calculate contributions
      PMap<String, Contribution> newContributions = calculateContributions(newIncomes);

      // mutate state
      return state.modifier()
          .withNewContributionYear(nextYear)
          .withNewPreviousYearlyIncome(newPreviousYearlyIncome)
          .withNewCurrentIncomes(newIncomes)
          .withNewContributions(newContributions)
          .modify();
    };
  }

  private static PMap<String, Contribution> calculateContributions(PMap<Integer, Income> newIncomes) {
    return HashTreePMap.empty();
  }

  private static PMap<Integer, Income> applyIncome(Income income, PMap<Integer, Income> currentIncomes) {

    // spread out the income over the months of the period
    Map<Integer, Income> spreadIncome = IncomeUtils.spreadOutOverMonths(income);
    return currentIncomes.plusAll(spreadIncome);
  }

}
