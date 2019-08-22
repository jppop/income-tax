package income.tax.impl.contribution;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static income.tax.impl.contribution.CalculatorConfig.ContributionConfig;
import static income.tax.impl.contribution.CalculatorConfig2018.ContributionType.*;
import static org.assertj.core.api.Assertions.assertThat;

class CalculatorConfig2018Test {

  private static final BigDecimal pass = new BigDecimal("39732");
  private static final BigDecimal prci = new BigDecimal("37846");
  private static final BigDecimal passX4 = pass.multiply(BigDecimal.valueOf(4));
  private static final BigDecimal passX5 = pass.multiply(BigDecimal.valueOf(5));
  private static final BigDecimal pass40percent = pass.multiply(new BigDecimal("0.4"));
  private static final BigDecimal pass110 = pass.multiply(new BigDecimal("1.1"));
  private CalculatorConfig2018 calculatorConfig = new CalculatorConfig2018();
  private Map<String, ContributionConfig> contributionConfigs = calculatorConfig.contributionConfigs();

  @Test
  public void whenIncomeGreaterThan5Ceil() {

    final long income = 240_000;
    final BigDecimal incomeValue = BigDecimal.valueOf(income);

    BigDecimal expectedContribution;
    BigDecimal baseIncome;
    BigDecimal rate;

    // MLD1T2: Maladie 1 T2 pour la tranche des revenus supérieurs à 5 x PASS
    rate = new BigDecimal("6.5");
    baseIncome = incomeValue.subtract(passX5);
    expectedContribution = new BigDecimal("2687.10");
    compute(Maladie1T2.code(), income, rate, baseIncome, expectedContribution);

    // MLD1T1: Maladie 1 T1 pour la tranche des revenus inférieurs à 5 x PASS
    rate = new BigDecimal("6.35");
    baseIncome = passX5;
    expectedContribution = new BigDecimal("12614.91");
    compute(Maladie1T1.code(), income, rate, baseIncome, expectedContribution);

    // MAL1: Maladie 1 T1 + T2
    rate = new BigDecimal("1"); // not significant
    baseIncome = incomeValue;
    expectedContribution = new BigDecimal("15302.01");
    compute(Maladie1.code(), income, rate, baseIncome, expectedContribution, "15302");

    // MAL2
    rate = new BigDecimal("0.85");
    baseIncome = passX5;
    expectedContribution = new BigDecimal("1688.61");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "1688");

    // RVB
    rate = new BigDecimal("1"); // not significant
    baseIncome = incomeValue;
    expectedContribution = new BigDecimal("8254.04");
    compute(Retraite.code(), income, rate, baseIncome, expectedContribution, "8254");

    // RCI
    rate = new BigDecimal("1"); // not significant
    baseIncome = incomeValue;
    expectedContribution = new BigDecimal("12335.78");
    compute(RetraiteComplémentaire.code(), income, rate, baseIncome, expectedContribution, "12335");

    // RID
    rate = new BigDecimal("1.3");
    baseIncome = pass;
    expectedContribution = new BigDecimal("516.52");
    compute(InvalidititéDécès.code(), income, rate, baseIncome, expectedContribution, "516");

    // AF
    rate = new BigDecimal("3.1");
    baseIncome = incomeValue;
    expectedContribution = new BigDecimal("7440.00");
    compute(AllocationsFamiliales.code(), income, rate, baseIncome, expectedContribution, "7440");

    // CSG/CRDS
    BigDecimal csgRate = calculatorConfig.getCalculationConstant("CSG");
    rate = new BigDecimal("9.7");
    baseIncome = incomeValue.multiply(csgRate);
    expectedContribution = new BigDecimal("32592.00");
    compute(CSG_CRDS.code(), income, rate, baseIncome, expectedContribution, "32592");
  }

  private void compute(String code, long income,
                       BigDecimal expectedRate, BigDecimal expectedBaseIncome, BigDecimal expectedContribution) {
    compute(code, income, expectedRate, expectedBaseIncome, expectedContribution, Optional.empty());
  }

  private void compute(String code, long income,
                       BigDecimal expectedRate, BigDecimal expectedBaseIncome, BigDecimal expectedContribution,
                       String expectedRoundedValue) {
    compute(code, income, expectedRate, expectedBaseIncome, expectedContribution, Optional.ofNullable(expectedRoundedValue));
  }

  private void compute(String code, long income,
                       BigDecimal expectedRate, BigDecimal expectedBaseIncome, BigDecimal expectedContribution,
                       Optional<String> mayBeRoundedUpValue) {

    ContributionConfig contributionConfig = contributionConfigs.get(code);
    assertThat(contributionConfig).as(code).isNotNull();

    assertThat(contributionConfig.rateCalculator.compute(income)).as(code)
        .isEqualTo(expectedRate);
    assertThat(contributionConfig.baseIncomeCalculator.compute(income)).as(code)
        .isEqualTo(expectedBaseIncome);
    BigDecimal actualContribution = contributionConfig.compute(income);
    assertThat(actualContribution).as(code)
        .isEqualTo(expectedContribution);
    if (mayBeRoundedUpValue.isPresent()) {
      assertThat(calculatorConfig.round(actualContribution)).as(code)
          .isEqualTo(new BigDecimal(mayBeRoundedUpValue.get()));
    }

  }
}