package income.tax.impl.contribution;

import income.tax.calculator.Calculator;
import income.tax.calculator.ConstantProvider;
import income.tax.calculator.Contribution;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static income.tax.impl.contribution.Calculator2018.ContributionType.*;

public class Calculator2018 implements Calculator, ConstantProvider {

  private static final long PASS = 39732;
  private static final long PRCI = 37846;
  private static final BigDecimal csgRate = new BigDecimal("1.4");

  private static final MathContext mc = new MathContext(8, RoundingMode.DOWN);

  private static final BigDecimal pass = BigDecimal.valueOf(PASS);
  private static final BigDecimal prci = BigDecimal.valueOf(PRCI);
  private static final BigDecimal passX4 = pass.multiply(BigDecimal.valueOf(4));
  private static final BigDecimal passX5 = pass.multiply(BigDecimal.valueOf(5));
  private static final BigDecimal pass40percent = pass.multiply(new BigDecimal("0.4"), mc);
  private static final BigDecimal pass110 = pass.multiply(new BigDecimal("1.1"), mc);
  private static final BigDecimal pass0115 = pass.multiply(new BigDecimal("0.115"), mc);

  private static ContributionCalculator defaultContributionCalculator
      = (income, baseIncome, rate) -> baseIncome.multiply(rate.scaleByPowerOfTen(-2), mc);

  private Map<String, ContributionConfig> contributionConfigs = new HashMap<>();

  Calculator2018() {
    configure();
  }

  @Override
  public int getYear() {
    return 2018;
  }

  @Override
  public BigDecimal round(BigDecimal value) {
    return value.setScale(0, RoundingMode.FLOOR);
  }

  private static final ContributionType[] contributionToBeComputed = new ContributionType[] {
      Maladie1, Maladie2, Retraite, RetraiteComplémentaire, InvalidititéDécès, AllocationsFamiliales, CSG_CRDS
  };

  @Override
  public Map<String, Contribution> compute(BigDecimal income, boolean round) {
    Map<String, Contribution> contributions = new HashMap<>();

    for (ContributionType type: contributionToBeComputed) {
      Contribution contribution = compute(income, round, type.code());
      contributions.put(contribution.type, contribution);
    }

    return contributions;
  }

  @Override
  public Map<String, Contribution> computeFromMonthlyIncome(BigDecimal income, boolean round) {
    Map<String, Contribution> contributions = new HashMap<>();
    final BigDecimal monthCount = BigDecimal.valueOf(12);
    Map<String, Contribution> yearlyContributions = compute(income.multiply(monthCount), round);
    for (Map.Entry<String, Contribution> entry : yearlyContributions.entrySet()) {
      Contribution yearlyContribution = entry.getValue();
      contributions.put(
          entry.getKey(),
          Contribution.of(
              yearlyContribution.type,
              yearlyContribution.income.divide(monthCount, mc),
              yearlyContribution.baseIncome.divide(monthCount, mc),
              yearlyContribution.rate,
              yearlyContribution.contribution.divide(monthCount, mc)));
    }
    return contributions;
  }
  
  private Contribution compute(BigDecimal income, boolean round, String code) {
    ContributionConfig contributionConfig = contributionConfigs.get(code);
    BigDecimal baseIncome = contributionConfig.baseIncomeCalculator.compute(income);
    BigDecimal contributionAmount = contributionConfig.compute(income);
    if (round) {
      contributionAmount = round(contributionAmount);
    }
    // informative rate
    BigDecimal rate;
    if (baseIncome.compareTo(BigDecimal.ZERO) == 0) {
      rate = BigDecimal.ZERO;
    } else {
      rate = contributionAmount.divide(baseIncome, mc).scaleByPowerOfTen(2).setScale(2, RoundingMode.CEILING);
    }
    return Contribution.of(code, income, baseIncome, rate, contributionAmount);
  }

  @Override
  public BigDecimal getCalculationConstant(String name) {
    switch (name.toUpperCase()) {
      case "PASS":
        return pass;
      case "PRCI":
        return prci;
      case "CSG":
        return csgRate;
      default:
        return BigDecimal.ZERO;
    }
  }

  Map<String, ContributionConfig> contributionConfigs() {
    return Collections.unmodifiableMap(contributionConfigs);
  }

  private void configure() {

    ContributionConfig contributionConfig;

    // MLD1T2: Maladie 1 T2 pour la tranche des revenus supérieurs à 5 x PASS
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> {
          if (income.compareTo(passX5) > 0) {
            return income.subtract(passX5);
          } else {
            return BigDecimal.ZERO;
          }
        },
        // rate
        (income) -> new BigDecimal("6.5"),
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.Maladie1T2.code(), contributionConfig);

    // MLD1T1: Maladie 1 T2 pour la tranche des revenus inférieurs à 5 x PASS
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> income.max(pass40percent).min(passX5),
        // rate
        (income) -> {
          BigDecimal rate;
          if (income.compareTo(BigDecimal.ZERO) >= 0 && income.compareTo(pass40percent) <= 0) {
            // contribution.rate = ((6.35 - 1.35) / (1.1 * PASS)) * yearlyIncome + (1.35 - 0) / (0.4 * PASS) * yearlyIncome;
            // contribution.rate = ((5 / (1.1 * PASS) + (1.35/(0.4 * PASS)) * yearlyIncome;
            // contribution.rate = ((5/1.1 + 1.35/0.4) / PASS) * yearlyIncome;
            rate = new BigDecimal(5).divide(new BigDecimal("1.1"), mc);
            rate = rate.add(new BigDecimal("1.35").divide(new BigDecimal("0.4"), mc));
            rate = rate.divide(pass, mc);
            rate = rate.multiply(income, mc);
          } else if (income.compareTo(BigDecimal.ZERO) > 0 && income.compareTo(pass110) <= 0) {
            // contribution.rate = (((6.35 - 1.35) / (1.1 * PASS)) * yearlyIncome) + 1.35;
            // contribution.rate = 1.35 + (5 / (1.1 * PASS)) * yearlyIncome;
            rate = new BigDecimal(5).divide(new BigDecimal("1.1"), mc);
            rate = rate.divide(pass, mc);
            rate = rate.multiply(income, mc);
            rate = rate.add(new BigDecimal("1.35"), mc);
          } else {
            rate = new BigDecimal("6.35");
          }
          return rate;
        },
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.Maladie1T1.code(), contributionConfig);

    // MAL1: MAL1T1 + MAL1T2
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> income,
        // rate
        (income) -> new BigDecimal("1"), // not really significant
        (income, baseIncome, rate) -> {
          // Get MAL1 T1 contributions
          ContributionConfig t1 = contributionConfigs.get(ContributionType.Maladie1T1.code());
          BigDecimal baseIncomeT1 = t1.baseIncomeCalculator.compute(income);
          BigDecimal rateT1 = t1.rateCalculator.compute(income);
          BigDecimal contribT1 = t1.contributionCalculator.compute(income, baseIncomeT1, rateT1);
          // Get MAL1 T2 contributions
          ContributionConfig t2 = contributionConfigs.get(ContributionType.Maladie1T2.code());
          BigDecimal baseIncomeT2 = t2.baseIncomeCalculator.compute(income);
          BigDecimal rateT2 = t2.rateCalculator.compute(income);
          BigDecimal contribT2 = t2.contributionCalculator.compute(income, baseIncomeT2, rateT2);

          return contribT1.add(contribT2);
        }
    );
    contributionConfigs.put(ContributionType.Maladie1.code(), contributionConfig);

    // MAL2: Maladie 2
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> income.max(pass40percent).min(passX5),
        // rate
        (income) -> new BigDecimal("0.85"),
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.Maladie2.code(), contributionConfig);

    // RVB T1: Retraite de base T1
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> {
          if (income.compareTo(pass) >= 0) {
            return pass;
          } else if (income.compareTo(pass0115) > 0) {
            return income;
          } else {
            return pass0115;
          }
        },
        // rate
        (income) -> new BigDecimal("17.75"),
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.RetraiteT1.code(), contributionConfig);

    // RVB T2: Retraite de base T2
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> {
          if (income.compareTo(pass) >= 0) {
            return income.subtract(pass);
          } else {
            return BigDecimal.ZERO;
          }
        },
        // rate
        (income) -> new BigDecimal("0.6"),
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.RetraiteT2.code(), contributionConfig);

    contributionConfig = new ContributionConfig(
        // base income
        (income) -> income,
        // rate
        (income) -> new BigDecimal("1"), // not really significant
        (income, baseIncome, rate) -> {
          // Get RVB T1 contributions
          ContributionConfig t1 = contributionConfigs.get(ContributionType.RetraiteT1.code());
          BigDecimal baseIncomeT1 = t1.baseIncomeCalculator.compute(income);
          BigDecimal rateT1 = t1.rateCalculator.compute(income);
          BigDecimal contribT1 = t1.contributionCalculator.compute(income, baseIncomeT1, rateT1);
          // Get RVB T2 contributions
          ContributionConfig t2 = contributionConfigs.get(ContributionType.RetraiteT2.code());
          BigDecimal baseIncomeT2 = t2.baseIncomeCalculator.compute(income);
          BigDecimal rateT2 = t2.rateCalculator.compute(income);
          BigDecimal contribT2 = t2.contributionCalculator.compute(income, baseIncomeT2, rateT2);

          return contribT1.add(contribT2);
        }
    );
    contributionConfigs.put(ContributionType.Retraite.code(), contributionConfig);

    // RCI T1: Retraite complémentaire T1
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> {
          if (income.compareTo(prci) < 0) {
            return income;
          } else {
            return prci;
          }
        },
        // rate
        (income) -> new BigDecimal("7"),
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.RetraiteComplémentaireT1.code(), contributionConfig);

    // RCI T2: Retraite complémentaire T2
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> {
          if (income.compareTo(prci) < 0) {
            return BigDecimal.ZERO;
          } else if (income.compareTo(passX4) >= 0) {
            return passX4.subtract(prci, mc);
          } else {
            return income.subtract(prci, mc);
          }
        },
        // rate
        (income) -> new BigDecimal("8"),
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.RetraiteComplémentaireT2.code(), contributionConfig);

    // RCI = RCI T1 + RCI T2
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> income,
        // rate
        (income) -> new BigDecimal("1"), // not really significant
        (income, baseIncome, rate) -> {
          // Get RVB T1 contributions
          ContributionConfig t1 = contributionConfigs.get(ContributionType.RetraiteComplémentaireT1.code());
          BigDecimal baseIncomeT1 = t1.baseIncomeCalculator.compute(income);
          BigDecimal rateT1 = t1.rateCalculator.compute(income);
          BigDecimal contribT1 = t1.contributionCalculator.compute(income, baseIncomeT1, rateT1);
          // Get RVB T2 contributions
          ContributionConfig t2 = contributionConfigs.get(ContributionType.RetraiteComplémentaireT2.code());
          BigDecimal baseIncomeT2 = t2.baseIncomeCalculator.compute(income);
          BigDecimal rateT2 = t2.rateCalculator.compute(income);
          BigDecimal contribT2 = t2.contributionCalculator.compute(income, baseIncomeT2, rateT2);

          return contribT1.add(contribT2);
        }
    );
    contributionConfigs.put(ContributionType.RetraiteComplémentaire.code(), contributionConfig);

    // RID: Invalidité-décès
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> {
          if (income.compareTo(pass0115) < 0) {
            return pass0115;
          } else if (income.compareTo(pass) < 0) {
            return income;
          } else {
            return pass;
          }
        },
        // rate
        (income) -> new BigDecimal("1.3"),
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.InvalidititéDécès.code(), contributionConfig);

    // AF: Allocation familiales
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> income,
        // rate
        (income) -> {
          if (income.compareTo(pass110) < 0) {
            return BigDecimal.ZERO;
          } else if (income.compareTo(pass.multiply(new BigDecimal("1.4"), mc)) > 0) {
            return new BigDecimal("3.1");
          } else {
            BigDecimal r1 = new BigDecimal("3.1").divide(pass.multiply(new BigDecimal("0.3"), mc), mc);
            return r1.multiply(income.subtract(new BigDecimal("1.1").multiply(pass, mc), mc), mc);
          }
        },
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.AllocationsFamiliales.code(), contributionConfig);

    // CSG_CRDS
    contributionConfig = new ContributionConfig(
        // base income
        (income) -> income.multiply(csgRate, mc),
        // rate
        (income) -> new BigDecimal("9.7"),
        defaultContributionCalculator
    );
    contributionConfigs.put(ContributionType.CSG_CRDS.code(), contributionConfig);
  }

  enum ContributionType {
    Maladie1T2("MLD1T2"), // Maladie 1 dans la limite de 5 PASS
    Maladie1T1("MLD1T1"), // Maladie 1 au delà de de 5 PASS
    Maladie1("MAL1"), // Maladie 1 = MLD1T1 + MLDT2
    Maladie2("MAL2"), // Maladie 2
    RetraiteT1("RVB T1"), // Retraite de base dans la limite de PASS
    RetraiteT2("RVB T2"), // Retraite de base au delà de PASS
    Retraite("RVB"), // Retraite de base = RVBT1 + RVBT2
    RetraiteComplémentaireT1("RCI T1"), // Retraite complémentaire dans la limite de PRCI
    RetraiteComplémentaireT2("RCI T2"), // Retraite complémentaire entre PRCI et 4 x PASS
    RetraiteComplémentaire("RCI"), // Retraite complémentaire = RCI T1 + RCI T2
    InvalidititéDécès("RID"), // Invalidité-décès dans la limite de PASS
    AllocationsFamiliales("AF"),
    CSG_CRDS("CSG/CRDS");

    private final String code;

    ContributionType(String code) {
      this.code = code;
    }

    public String code() {
      return code;
    }
  }
}
