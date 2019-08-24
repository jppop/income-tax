package income.tax.impl.contribution;

import java.math.BigDecimal;

public class Calculator2018 extends BaseCalculator {

  public Calculator2018() {
    super(new BigDecimal("39732"), new BigDecimal("37846"), new BigDecimal("1.4"));
  }

  @Override
  public int getYear() {
    return 2018;
  }

}
