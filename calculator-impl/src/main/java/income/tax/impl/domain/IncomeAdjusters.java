package income.tax.impl.domain;

import income.tax.api.Income;
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

  public static IncomeAdjuster previousYear(Income income) {
    return (state) -> {

      // Apply new previous income
      PMap<Integer, Income> newPreviousYearlyIncome =
          state.previousYearlyIncomes.plus(income.start.getYear(), IncomeUtils.scaleToFullYear(income));

      // calculate new current year incomes
      PMap<Integer, Income> newIncomes = applyIncome(income, state.currentIncomes);

      // calculate contributions
      PMap<String, Contribution> newContributions = calculateContributions(newIncomes);

      // return new state
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

  public static IncomeAdjuster newYear(Income previousYearlyIncomes) {
    return (state) -> {

      // Apply previous income to the new year
      Income newYearIncome = IncomeUtils.scaleToFullYear(previousYearlyIncomes, previousYearlyIncomes.start.getYear() + 1);
      PMap<Integer, Income> newIncomes = applyIncome(newYearIncome, IntTreePMap.empty());

      // calculate contributions
      PMap<String, Contribution> newContributions = calculateContributions(newIncomes);

      // return new state
      return state.modifier()
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
