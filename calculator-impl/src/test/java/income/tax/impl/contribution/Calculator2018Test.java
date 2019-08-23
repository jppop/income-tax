package income.tax.impl.contribution;

import income.tax.calculator.Contribution;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

import static income.tax.calculator.Calculator.ContributionConfig;
import static income.tax.impl.contribution.Calculator2018.ContributionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class Calculator2018Test {

  private static final BigDecimal pass = new BigDecimal("39732");
  private static final BigDecimal prci = new BigDecimal("37846");
  private static final BigDecimal passX4 = pass.multiply(BigDecimal.valueOf(4));
  private static final BigDecimal passX5 = pass.multiply(BigDecimal.valueOf(5));
  private static final BigDecimal pass40percent = pass.multiply(new BigDecimal("0.4"));
  private static final BigDecimal pass110 = pass.multiply(new BigDecimal("1.1"));
  private Calculator2018 calculatorConfig = new Calculator2018();
  private Map<String, ContributionConfig> contributionConfigs = calculatorConfig.contributionConfigs();

  @Test
  public void highIncome() {

    final BigDecimal income = BigDecimal.valueOf(240_000);

    BigDecimal expectedContribution;
    BigDecimal baseIncome;
    BigDecimal rate;

    // MLD1T2: Maladie 1 T2 pour la tranche des revenus supérieurs à 5 x PASS
    rate = new BigDecimal("6.5");
    baseIncome = income.subtract(passX5);
    expectedContribution = new BigDecimal("2687.10");
    compute(Maladie1T2.code(), income, rate, baseIncome, expectedContribution);

    // MLD1T1: Maladie 1 T1 pour la tranche des revenus inférieurs à 5 x PASS
    rate = new BigDecimal("6.35");
    baseIncome = passX5;
    expectedContribution = new BigDecimal("12614.91");
    compute(Maladie1T1.code(), income, rate, baseIncome, expectedContribution);

    // MAL1: Maladie 1 T1 + T2
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("15302.01");
    compute(Maladie1.code(), income, rate, baseIncome, expectedContribution, "15302");

    // MAL2
    rate = new BigDecimal("0.85");
    baseIncome = passX5;
    expectedContribution = new BigDecimal("1688.61");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "1688");

    // RVB
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("8254.04");
    compute(Retraite.code(), income, rate, baseIncome, expectedContribution, "8254");

    // RCI
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("12335.78");
    compute(RetraiteComplémentaire.code(), income, rate, baseIncome, expectedContribution, "12335");

    // RID
    rate = new BigDecimal("1.3");
    baseIncome = pass;
    expectedContribution = new BigDecimal("516.52");
    compute(InvalidititéDécès.code(), income, rate, baseIncome, expectedContribution, "516");

    // AF
    rate = new BigDecimal("3.1");
    baseIncome = income;
    expectedContribution = new BigDecimal("7440.00");
    compute(AllocationsFamiliales.code(), income, rate, baseIncome, expectedContribution, "7440");

    // CSG/CRDS
    BigDecimal csgRate = calculatorConfig.getCalculationConstant("CSG");
    rate = new BigDecimal("9.7");
    baseIncome = income.multiply(csgRate);
    expectedContribution = new BigDecimal("32592.00");
    compute(CSG_CRDS.code(), income, rate, baseIncome, expectedContribution, "32592");
  }

  @Test
  public void meanIncome() {

    final BigDecimal income = BigDecimal.valueOf(23_844);

    BigDecimal expectedContribution;
    BigDecimal baseIncome;
    BigDecimal rate;

    // MAL1: Maladie 1 T1 + T2
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("972.32");
    compute(Maladie1.code(), income, rate, baseIncome, expectedContribution, "972");

    // MAL2
    rate = new BigDecimal("0.85");
    baseIncome = income;
    expectedContribution = new BigDecimal("202.68");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "202");

    // RVB
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("4232.31");
    compute(Retraite.code(), income, rate, baseIncome, expectedContribution, "4232");

    // RCI
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("1669.08");
    compute(RetraiteComplémentaire.code(), income, rate, baseIncome, expectedContribution, "1669");

    // RID
    rate = new BigDecimal("1.3");
    baseIncome = income;
    expectedContribution = new BigDecimal("309.98");
    compute(InvalidititéDécès.code(), income, rate, baseIncome, expectedContribution, "309");

    // AF
    rate = BigDecimal.ZERO; // R < 110% * PASS
    baseIncome = income;
    expectedContribution = new BigDecimal("0.00");;
    compute(AllocationsFamiliales.code(), income, rate, baseIncome, expectedContribution, "0");

    // CSG/CRDS
    BigDecimal csgRate = calculatorConfig.getCalculationConstant("CSG");
    rate = new BigDecimal("9.7");
    baseIncome = income.multiply(csgRate);
    expectedContribution = new BigDecimal("3238.02");
    compute(CSG_CRDS.code(), income, rate, baseIncome, expectedContribution, "3238");
  }

  @Test
  public void lowIncome() {

    final BigDecimal income = BigDecimal.valueOf(11_520);

    BigDecimal expectedContribution;
    BigDecimal baseIncome;
    BigDecimal rate;

    // MAL1: Maladie 1 T1 + T2
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("364.98");
    compute(Maladie1.code(), income, rate, baseIncome, expectedContribution, "364");

    // MAL2
    rate = new BigDecimal("0.85");
    baseIncome = pass40percent;
    expectedContribution = new BigDecimal("135.09");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "135");

    // RVB
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("2044.80");
    compute(Retraite.code(), income, rate, baseIncome, expectedContribution, "2044");

    // RCI
    rate = new BigDecimal("1"); // not significant
    baseIncome = income;
    expectedContribution = new BigDecimal("806.40");
    compute(RetraiteComplémentaire.code(), income, rate, baseIncome, expectedContribution, "806");

    // RID
    rate = new BigDecimal("1.3");
    baseIncome = income;
    expectedContribution = new BigDecimal("149.76");
    compute(InvalidititéDécès.code(), income, rate, baseIncome, expectedContribution, "149");

    // AF
    rate = BigDecimal.ZERO; // R < 110% * PASS
    baseIncome = income;
    expectedContribution = new BigDecimal("0.00");;
    compute(AllocationsFamiliales.code(), income, rate, baseIncome, expectedContribution, "0");

    // CSG/CRDS
    BigDecimal csgRate = calculatorConfig.getCalculationConstant("CSG");
    rate = new BigDecimal("9.7");
    baseIncome = income.multiply(csgRate);
    expectedContribution = new BigDecimal("1564.42");
    compute(CSG_CRDS.code(), income, rate, baseIncome, expectedContribution, "1564");
  }

  @Test
  public void monthBasedCalculation() {

    final BigDecimal monthCount = BigDecimal.valueOf(12);
    final BigDecimal yearlyIncome = BigDecimal.valueOf(12 * 2000 + 1);

    // yearly income based calculation
    Map<String, Contribution> yearlyContributions = calculatorConfig.compute(yearlyIncome, false);
    BigDecimal totalYearlyContributions = yearlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    BigDecimal monthlyIncome = yearlyIncome.divide(monthCount, 8, RoundingMode.CEILING);
    Map<String, Contribution> monthlyContributions = calculatorConfig.compute(monthlyIncome.multiply(monthCount), false);
    BigDecimal totalMonthlyContributions = monthlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    assertThat(totalYearlyContributions).isCloseTo(totalMonthlyContributions, within(new BigDecimal("0.01")));
  }

  @Test
  public void monthBasedCalculation2() {

    final BigDecimal monthCount = BigDecimal.valueOf(12);
    final BigDecimal monthlyIncome = new BigDecimal("1500.5675");

    // monthly income based calculation
    Map<String, Contribution> monthlyContributions = calculatorConfig.computeFromMonthlyIncome(monthlyIncome, false);
    BigDecimal totalMonthlyContributions = monthlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    BigDecimal yearlyIncome = monthlyIncome.multiply(monthCount);
    Map<String, Contribution> yearlyContributions = calculatorConfig.compute(yearlyIncome, false);
    BigDecimal totalYearlyContributions = yearlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    assertThat(totalMonthlyContributions.multiply(monthCount))
        .isCloseTo(totalYearlyContributions, within(new BigDecimal("0.001")));
  }

  private void compute(String code, BigDecimal income,
                       BigDecimal expectedRate, BigDecimal expectedBaseIncome, BigDecimal expectedContribution) {
    compute(code, income, expectedRate, expectedBaseIncome, expectedContribution, Optional.empty());
  }

  private void compute(String code, BigDecimal income,
                       BigDecimal expectedRate, BigDecimal expectedBaseIncome, BigDecimal expectedContribution,
                       String expectedRoundedValue) {
    compute(code, income, expectedRate, expectedBaseIncome, expectedContribution, Optional.ofNullable(expectedRoundedValue));
  }

  private void compute(String code, BigDecimal income,
                       BigDecimal expectedRate, BigDecimal expectedBaseIncome, BigDecimal expectedContribution,
                       Optional<String> mayBeRoundedUpValue) {

    ContributionConfig contributionConfig = contributionConfigs.get(code);
    assertThat(contributionConfig).as(code).isNotNull();

    assertThat(contributionConfig.rateCalculator.compute(income)).as(code)
        .isEqualTo(expectedRate);
    assertThat(contributionConfig.baseIncomeCalculator.compute(income)).as(code)
        .isEqualTo(expectedBaseIncome);
    BigDecimal actualContribution = contributionConfig.compute(income).setScale(2, RoundingMode.CEILING);
    assertThat(actualContribution).as(code)
        .isEqualTo(expectedContribution);
    if (mayBeRoundedUpValue.isPresent()) {
      assertThat(calculatorConfig.round(actualContribution)).as(code)
          .isEqualTo(new BigDecimal(mayBeRoundedUpValue.get()));
    }

  }
}