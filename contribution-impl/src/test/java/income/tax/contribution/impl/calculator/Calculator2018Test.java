package income.tax.contribution.impl.calculator;

import income.tax.contribution.impl.Calculator;
import income.tax.contribution.impl.ContributionInternal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.util.Map;
import java.util.Optional;

import static income.tax.contribution.impl.calculator.BaseCalculator.ContributionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class Calculator2018Test {

  private static final BigDecimal pass = new BigDecimal("39732");
  private static final BigDecimal prci = new BigDecimal("37846");
  private static final BigDecimal passX4 = pass.multiply(BigDecimal.valueOf(4));
  private static final BigDecimal passX5 = pass.multiply(BigDecimal.valueOf(5));
  private static final BigDecimal pass40percent = pass.multiply(new BigDecimal("0.4"));
  private static final BigDecimal pass0115 = pass.multiply(new BigDecimal("0.115"));
  private static final BigDecimal pass110 = pass.multiply(new BigDecimal("1.1"));
  private Calculator2018 calculator = new Calculator2018();
  private Map<String, Calculator.ContributionConfig> contributionConfigs = calculator.contributionConfigs();

  @Test
  public void highIncome() {

    final BigDecimal income = BigDecimal.valueOf(240_000);

    BigDecimal expectedContribution;
    BigDecimal baseIncome;
    BigDecimal rate;

    // MLD1T2: Maladie 1 T2 pour la tranche des revenus supérieurs à 5 x PASS
    rate = new BigDecimal("6.50");
    baseIncome = income.subtract(passX5);
    expectedContribution = new BigDecimal("2687.10");
    compute(Maladie1T2.code(), income, rate, baseIncome, expectedContribution, "2687");

    // MLD1T1: Maladie 1 T1 pour la tranche des revenus inférieurs à 5 x PASS
    rate = new BigDecimal("6.35");
    baseIncome = passX5;
    expectedContribution = new BigDecimal("12614.91");
    compute(Maladie1T1.code(), income, rate, baseIncome, expectedContribution, "12615");

    // MAL1: Maladie 1 T1 + T2
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("15302.01");
//    compute(Maladie1.code(), income, rate, baseIncome, expectedContribution, "15302");

    // MAL2
    rate = new BigDecimal("0.85");
    baseIncome = passX5;
    expectedContribution = new BigDecimal("1688.61");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "1689");

    // RVB T1
    rate = new BigDecimal("17.75"); // not significant
    baseIncome = pass;
    expectedContribution = new BigDecimal("7052.43");
    compute(RetraiteT1.code(), income, rate, baseIncome, expectedContribution, "7052");

    // RVB T2
    rate = new BigDecimal("0.60"); // not significant
    baseIncome = income.subtract(pass);
    expectedContribution = new BigDecimal("1201.61");
    compute(RetraiteT2.code(), income, rate, baseIncome, expectedContribution, "1202");

    // RVB
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("8254.04");
//    compute(Retraite.code(), income, rate, baseIncome, expectedContribution, "8254");

    // RCI T1
    rate = new BigDecimal("7.00");
    baseIncome = prci;
    expectedContribution = new BigDecimal("2649.22");
    compute(RetraiteComplementaireT1.code(), income, rate, baseIncome, expectedContribution, "2649");

    // RCI T2
    rate = new BigDecimal("8.00");
    baseIncome = passX4.subtract(prci);
    expectedContribution = new BigDecimal("9686.56");
    compute(RetraiteComplementaireT2.code(), income, rate, baseIncome, expectedContribution, "9687");

    // RCI
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("12335.78");
//    compute(RetraiteComplémentaire.code(), income, rate, baseIncome, expectedContribution, "12335");

    // RID
    rate = new BigDecimal("1.30");
    baseIncome = pass;
    expectedContribution = new BigDecimal("516.52");
    compute(InvalidititeDeces.code(), income, rate, baseIncome, expectedContribution, "517");

    // AF
    rate = new BigDecimal("3.10");
    baseIncome = income;
    expectedContribution = new BigDecimal("7440.00");
    compute(AllocationsFamiliales.code(), income, rate, baseIncome, expectedContribution, "7440");

    // CSG/CRDS
    BigDecimal csgRate = calculator.getCalculationConstant("CSG");
    rate = new BigDecimal("9.70");
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

    // MLD1T2: Maladie 1 T2 pour la tranche des revenus supérieurs à 5 x PASS
    rate = new BigDecimal("6.50");
    baseIncome = BigDecimal.ZERO;
    expectedContribution = BigDecimal.ZERO.setScale(2);
    compute(Maladie1T2.code(), income, rate, baseIncome, expectedContribution);

    // MLD1T1: Maladie 1 T1 pour la tranche des revenus inférieurs à 5 x PASS
    rate = new BigDecimal("4.08");
    baseIncome = income;
    expectedContribution = new BigDecimal("972.32");
    compute(Maladie1T1.code(), income, rate, baseIncome, expectedContribution, "972");

    // MAL1: Maladie 1 T1 + T2
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("972.32");
//    compute(Maladie1.code(), income, rate, baseIncome, expectedContribution, "972");

    // MAL2
    rate = new BigDecimal("0.85");
    baseIncome = income;
    expectedContribution = new BigDecimal("202.68");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "203");

    // RVB T1
    rate = new BigDecimal("17.75");
    baseIncome = income;
    expectedContribution = new BigDecimal("4232.31");
    compute(RetraiteT1.code(), income, rate, baseIncome, expectedContribution, "4232");

    // RVB T2
    rate = new BigDecimal("0.60");
    baseIncome = BigDecimal.ZERO;
    expectedContribution = BigDecimal.ZERO.setScale(2);
    compute(RetraiteT2.code(), income, rate, baseIncome, expectedContribution, "0");

//    // RVB
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("4232.31");
//    compute(Retraite.code(), income, rate, baseIncome, expectedContribution, "4232");

    // RCI T1
    rate = new BigDecimal("7.00");
    baseIncome = income;
    expectedContribution = new BigDecimal("1669.08");
    compute(RetraiteComplementaireT1.code(), income, rate, baseIncome, expectedContribution, "1669");

    // RCI T2
    rate = new BigDecimal("8.00");
    baseIncome = BigDecimal.ZERO;
    expectedContribution = BigDecimal.ZERO.setScale(2);
    compute(RetraiteComplementaireT2.code(), income, rate, baseIncome, expectedContribution, "0");

//    // RCI
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("1669.08");
//    compute(RetraiteComplémentaire.code(), income, rate, baseIncome, expectedContribution, "1669");

    // RID
    rate = new BigDecimal("1.30");
    baseIncome = income;
    expectedContribution = new BigDecimal("309.98");
    compute(InvalidititeDeces.code(), income, rate, baseIncome, expectedContribution, "310");

    // AF
    rate = BigDecimal.ZERO.setScale(2); // R < 110% * PASS
    baseIncome = income;
    expectedContribution = BigDecimal.ZERO.setScale(2);
    compute(AllocationsFamiliales.code(), income, rate, baseIncome, expectedContribution, "0");

    // CSG/CRDS
    BigDecimal csgRate = calculator.getCalculationConstant("CSG");
    rate = new BigDecimal("9.70");
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

    // MLD1T2: Maladie 1 T2 pour la tranche des revenus supérieurs à 5 x PASS
    rate = new BigDecimal("6.50");
    baseIncome = BigDecimal.ZERO;
    expectedContribution = BigDecimal.ZERO.setScale(2);
    compute(Maladie1T2.code(), income, rate, baseIncome, expectedContribution, "0");

    // MLD1T1: Maladie 1 T1 pour la tranche des revenus inférieurs à 5 x PASS
    rate = new BigDecimal("2.30");
    baseIncome = pass40percent;
    expectedContribution = new BigDecimal("364.98");
    compute(Maladie1T1.code(), income, rate, baseIncome, expectedContribution, "365");

    // MAL1: Maladie 1 T1 + T2
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("364.98");
//    compute(Maladie1.code(), income, rate, baseIncome, expectedContribution, "364");

    // MAL2
    rate = new BigDecimal("0.85");
    baseIncome = pass40percent;
    expectedContribution = new BigDecimal("135.09");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "135");

    // RVB T1
    rate = new BigDecimal("17.75");
    baseIncome = income;
    expectedContribution = new BigDecimal("2044.80");
    compute(RetraiteT1.code(), income, rate, baseIncome, expectedContribution, "2045");

    // RVB T2
    rate = new BigDecimal("0.60");
    baseIncome = BigDecimal.ZERO;
    expectedContribution = BigDecimal.ZERO.setScale(2);
    compute(RetraiteT2.code(), income, rate, baseIncome, expectedContribution, "0");

    // RCI T1
    rate = new BigDecimal("7.00");
    baseIncome = income;
    expectedContribution = new BigDecimal("806.40");
    compute(RetraiteComplementaireT1.code(), income, rate, baseIncome, expectedContribution, "806");

    // RCI T2
    rate = new BigDecimal("8.00");
    baseIncome = BigDecimal.ZERO;
    expectedContribution = BigDecimal.ZERO.setScale(2);
    compute(RetraiteComplementaireT2.code(), income, rate, baseIncome, expectedContribution, "0");

    // RID
    rate = new BigDecimal("1.30");
    baseIncome = income;
    expectedContribution = new BigDecimal("149.76");
    compute(InvalidititeDeces.code(), income, rate, baseIncome, expectedContribution, "150");

    // AF
    rate = BigDecimal.ZERO.setScale(2); // R < 110% * PASS
    baseIncome = income;
    expectedContribution = new BigDecimal("0.00");;
    compute(AllocationsFamiliales.code(), income, rate, baseIncome, expectedContribution, "0");

    // CSG/CRDS
    BigDecimal csgRate = calculator.getCalculationConstant("CSG");
    rate = new BigDecimal("9.70");
    baseIncome = income.multiply(csgRate);
    expectedContribution = new BigDecimal("1564.42");
    compute(CSG_CRDS.code(), income, rate, baseIncome, expectedContribution, "1564");
  }

  @Test
  public void monthBasedCalculation() {

    final BigDecimal monthCount = BigDecimal.valueOf(12);
    final BigDecimal yearlyIncome = BigDecimal.valueOf(12 * 2000 + 1);

    // yearly income based calculation
    Map<String, ContributionInternal> yearlyContributions = calculator.computeFromYearlyIncome(yearlyIncome);
    BigDecimal totalYearlyContributions = yearlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    BigDecimal monthlyIncome = yearlyIncome.divide(monthCount, 8, RoundingMode.CEILING);
    Map<String, ContributionInternal> monthlyContributions = calculator.computeFromYearlyIncome(monthlyIncome.multiply(monthCount));
    BigDecimal totalMonthlyContributions = monthlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    assertThat(totalYearlyContributions).isCloseTo(totalMonthlyContributions, within(new BigDecimal("0.01")));
  }

  @Test
  public void monthBasedCalculation2() {

    final BigDecimal monthCount = BigDecimal.valueOf(12);
    final BigDecimal monthlyIncome = new BigDecimal("24000");

    // monthly income based calculation
    Map<String, ContributionInternal> monthlyContributions =
        calculator.computeFromMonthlyIncome(Month.JANUARY, monthlyIncome, Optional.empty());
    BigDecimal totalMonthlyContributions = monthlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    BigDecimal yearlyIncome = monthlyIncome.multiply(monthCount);
    Map<String, ContributionInternal> yearlyContributions = calculator.computeFromYearlyIncome(yearlyIncome);
    BigDecimal totalYearlyContributions = yearlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    assertThat(totalMonthlyContributions.multiply(monthCount))
        .isCloseTo(totalYearlyContributions, within(new BigDecimal("0.001")));
  }

  static class MonthCalculator2018 extends BaseCalculator {

    public MonthCalculator2018() {
      super(RoundingMode.DOWN, new BigDecimal("3311"), new BigDecimal("3153.833333333333333"), new BigDecimal("1.4"));
    }

    @Override
    public int getYear() {
      return 2018;
    }
  }
  @Test
  public void monthBasedCalculation3() {

    MonthCalculator2018 monthCalculator2018 = new MonthCalculator2018();
    Map<String, Calculator.ContributionConfig> contributionConfigs = monthCalculator2018.contributionConfigs();
    final BigDecimal monthCount = BigDecimal.valueOf(12);
    final BigDecimal monthlyIncome = new BigDecimal("24000");

    // monthly income based calculation
    Map<String, ContributionInternal> monthlyContributions =
        monthCalculator2018.computeFromMonthlyIncome(Month.APRIL, monthlyIncome, Optional.empty());
    BigDecimal totalMonthlyContributions = monthlyContributions.values().stream()
        .map(contribution -> contribution.contribution)
        .reduce(BigDecimal.ZERO, (sum, contribution) -> sum.add(contribution));

    System.out.println("total contributions: " + totalMonthlyContributions);
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

    Calculator.ContributionConfig contributionConfig = contributionConfigs.get(code);
    assertThat(contributionConfig).as(code).isNotNull();

    BigDecimal actualRate = contributionConfig.rateCalculator.compute(income).setScale(2, RoundingMode.CEILING);
    BigDecimal actualBaseIncome = contributionConfig.baseIncomeCalculator.compute(income);
    BigDecimal actualContribution = contributionConfig.compute(income).setScale(2, RoundingMode.CEILING);
    assertThat(actualRate).as("%s -- rate", code)
        .isEqualTo(expectedRate);
    assertThat(actualBaseIncome).as("%s -- base income", code)
        .isEqualTo(expectedBaseIncome);
    assertThat(actualContribution).as("%s -- contribution", code)
        .isEqualTo(expectedContribution);
    if (mayBeRoundedUpValue.isPresent()) {
      assertThat(calculator.round(actualContribution)).as(code)
          .isEqualTo(new BigDecimal(mayBeRoundedUpValue.get()));
    }

  }
}