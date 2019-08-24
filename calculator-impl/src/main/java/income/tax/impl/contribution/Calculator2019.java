package income.tax.impl.contribution;

import java.math.BigDecimal;

public class Calculator2019 extends BaseCalculator {

  public Calculator2019() {
    super(new BigDecimal("40524"), new BigDecimal("37846"), new BigDecimal("1.4"));
  }

  @Override
  public int getYear() {
    return 2019;
  }

}
