package income.tax.calculator;

import lombok.Value;

import java.math.BigDecimal;

@Value(staticConstructor = "of")
public class Contribution {

  public final String type;
  public final BigDecimal income;
  public final BigDecimal baseIncome;
  public final BigDecimal rate;
  public final BigDecimal contribution;
}
