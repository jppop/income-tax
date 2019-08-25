package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

@Value
@JsonDeserialize
public class Contributions {

  public final @NonNull
  String contributorId;
  public final
  int year;
  public final
  long yearlyIncome;
  public final @NonNull
  Map<String, BigDecimal> yearlyContributions;
  public final @NonNull
  Map<Month, List<Contribution>> contributions;

  @JsonCreator
  public Contributions(String contributorId, int year, long yearlyIncome, Map<String, BigDecimal> yearlyContributions, Map<Month, List<Contribution>> contributions) {
    this.contributorId = contributorId;
    this.year = year;
    this.yearlyIncome = yearlyIncome;
    this.yearlyContributions = yearlyContributions;
    this.contributions = contributions;
  }

}
