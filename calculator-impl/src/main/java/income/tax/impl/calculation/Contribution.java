package income.tax.impl.calculation;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class Contribution {

  public final ContributionType type;
  public final BigDecimal rate;
  public final BigDecimal baseIncome;
  public final BigDecimal income;
  public final BigDecimal cell;
}
