package income.tax.contribution.impl.calculator;

import income.tax.contribution.impl.Calculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

import static income.tax.contribution.impl.calculator.BaseCalculator.ContributionType.*;
import static org.assertj.core.api.Assertions.assertThat;

class Calculator2019Test {

  private static final BigDecimal pass = new BigDecimal("40524");
  private static final BigDecimal prci = new BigDecimal("37960");
  private static final BigDecimal passX4 = pass.multiply(BigDecimal.valueOf(4));
  private static final BigDecimal passX5 = pass.multiply(BigDecimal.valueOf(5));
  private static final BigDecimal pass40percent = pass.multiply(new BigDecimal("0.4"));
  private static final BigDecimal pass0115 = pass.multiply(new BigDecimal("0.115"));
  private static final BigDecimal pass110 = pass.multiply(new BigDecimal("1.1"));
  private Calculator2019 calculator = new Calculator2019();
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
    expectedContribution = new BigDecimal("2429.70");
    compute(Maladie1T2.code(), income, rate, baseIncome, expectedContribution, "2430");

    // MLD1T1: Maladie 1 T1 pour la tranche des revenus inférieurs à 5 x PASS
    rate = new BigDecimal("6.35");
    baseIncome = passX5;
    expectedContribution = new BigDecimal("12866.37");
    compute(Maladie1T1.code(), income, rate, baseIncome, expectedContribution, "12866");

    // MAL1: Maladie 1 T1 + T2
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("15302.01");
//    compute(Maladie1.code(), income, rate, baseIncome, expectedContribution, "15302");

    // MAL2
    rate = new BigDecimal("0.85");
    baseIncome = passX5;
    expectedContribution = new BigDecimal("1722.27");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "1722");

    // RVB T1
    rate = new BigDecimal("17.75"); // not significant
    baseIncome = pass;
    expectedContribution = new BigDecimal("7193.01");
    compute(RetraiteT1.code(), income, rate, baseIncome, expectedContribution, "7193");

    // RVB T2
    rate = new BigDecimal("0.60"); // not significant
    baseIncome = income.subtract(pass);
    expectedContribution = new BigDecimal("1196.86");
    compute(RetraiteT2.code(), income, rate, baseIncome, expectedContribution, "1197");

    // RVB
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("8254.04");
//    compute(Retraite.code(), income, rate, baseIncome, expectedContribution, "8254");

    // RCI T1
    rate = new BigDecimal("7.00");
    baseIncome = prci;
    expectedContribution = new BigDecimal("2657.20");
    compute(RetraiteComplementaireT1.code(), income, rate, baseIncome, expectedContribution, "2657");

    // RCI T2
    rate = new BigDecimal("8.00");
    baseIncome = passX4.subtract(prci);
    expectedContribution = new BigDecimal("9930.88");
    compute(RetraiteComplementaireT2.code(), income, rate, baseIncome, expectedContribution, "9931");

    // RCI
//    rate = new BigDecimal("1"); // not significant
//    baseIncome = income;
//    expectedContribution = new BigDecimal("12335.78");
//    compute(RetraiteComplémentaire.code(), income, rate, baseIncome, expectedContribution, "12335");

    // RID
    rate = new BigDecimal("1.30");
    baseIncome = pass;
    expectedContribution = new BigDecimal("526.82");
    compute(InvalidititeDeces.code(), income, rate, baseIncome, expectedContribution, "527");

    // AF
    rate = new BigDecimal("3.10");
    baseIncome = income;
    expectedContribution = new BigDecimal("7440.00");
    compute(AllocationsFamiliales.code(), income, rate, baseIncome, expectedContribution, "7440");

    // CSG/CRDS
    BigDecimal csgRate = calculator.getCalculationConstant("CSG");
    rate = new BigDecimal("9.70");
    baseIncome = income.multiply(csgRate);
    expectedContribution = new BigDecimal("31428.00");
    compute(CSG_CRDS.code(), income, rate, baseIncome, expectedContribution, "31428");
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
    rate = new BigDecimal("4.03");
    baseIncome = income;
    expectedContribution = new BigDecimal("959.61");
    compute(Maladie1T1.code(), income, rate, baseIncome, expectedContribution, "960");

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
    expectedContribution = new BigDecimal("3122.38");
    compute(CSG_CRDS.code(), income, rate, baseIncome, expectedContribution, "3122");
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
    rate = new BigDecimal("2.26");
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
    expectedContribution = new BigDecimal("137.79");
    compute(Maladie2.code(), income, rate, baseIncome, expectedContribution, "138");

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
    expectedContribution = new BigDecimal("1508.55");
    compute(CSG_CRDS.code(), income, rate, baseIncome, expectedContribution, "1509");
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