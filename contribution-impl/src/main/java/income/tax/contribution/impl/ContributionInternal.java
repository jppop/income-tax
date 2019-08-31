package income.tax.contribution.impl;

import lombok.Value;

import java.math.BigDecimal;

@Value(staticConstructor = "of")
public class ContributionInternal {

  public final String type;
  public final BigDecimal income;
  public final BigDecimal baseIncome;
  public final BigDecimal rate;
  public final BigDecimal contribution;

}
