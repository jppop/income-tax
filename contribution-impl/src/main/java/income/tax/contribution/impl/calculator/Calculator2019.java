package income.tax.contribution.impl.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Calculator2019 extends BaseCalculator {

  public Calculator2019() {
    super(RoundingMode.HALF_EVEN, new BigDecimal("40524"), new BigDecimal("37960"), new BigDecimal("1.35"));
  }

  @Override
  public int getYear() {
    return 2019;
  }

}
