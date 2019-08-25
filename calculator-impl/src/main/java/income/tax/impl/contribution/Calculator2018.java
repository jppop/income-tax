package income.tax.impl.contribution;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Calculator2018 extends BaseCalculator {

  public Calculator2018() {
    super(RoundingMode.HALF_EVEN, new BigDecimal("39732"), new BigDecimal("37846"), new BigDecimal("1.4"));
  }

  @Override
  public int getYear() {
    return 2018;
  }

}
