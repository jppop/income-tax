package income.tax.contribution.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Value;

import java.math.BigDecimal;

@Value
@JsonDeserialize
public class Contribution {

  public final String type;
  public final BigDecimal income;
  public final BigDecimal baseIncome;
  public final BigDecimal rate;
  public final BigDecimal contribution;

  @JsonCreator
  public Contribution(String type, BigDecimal income, BigDecimal baseIncome, BigDecimal rate, BigDecimal contribution) {
    this.type = type;
    this.income = income;
    this.baseIncome = baseIncome;
    this.rate = rate;
    this.contribution = contribution;
  }
}
