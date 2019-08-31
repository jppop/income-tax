package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import income.tax.contribution.api.Contribution;
import lombok.NonNull;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;

@Value
@JsonDeserialize
public class Contributions {

  public final @NonNull
  String contributorId;
  public final @NonNull
  LocalDate start;
  public final @NonNull
  LocalDate end;
  public final @NonNull
  BigDecimal totalIncome;
  public final @NonNull
  Map<String, BigDecimal> totalContributions;
  public final @NonNull
  Map<Month, List<Contribution>> contributions;

  @JsonCreator
  public Contributions(
      String contributorId, LocalDate start, LocalDate end, BigDecimal totalIncome,
      Map<String, BigDecimal> totalContributions, Map<Month, List<Contribution>> contributions) {
    this.contributorId = contributorId;
    this.start = start;
    this.end = end;
    this.totalIncome = totalIncome;
    this.totalContributions = totalContributions;
    this.contributions = contributions;
  }

}
