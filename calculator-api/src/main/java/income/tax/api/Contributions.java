package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import lombok.Value;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.time.Month;
import java.util.HashMap;
import java.util.Map;

@Value
@JsonDeserialize
public class Contributions {

  public final @NonNull
  String contributorId;
  public final
  int year;
  public final @NonNull
  PMap<Month, PMap<String, Contribution>> contributions;

  @JsonCreator
  public Contributions(String contributorId, int year, PMap<Month, PMap<String, Contribution>> contributions) {
    this.contributorId = contributorId;
    this.year = year;
    this.contributions = contributions;
  }

  public static Contributions from(String contributorId, int year, PMap<Month, PMap<String, income.tax.calculator.Contribution>> yearlyContributions) {
    Map<Month, PMap<String, Contribution>> contributions = new HashMap<>();
    for (Map.Entry<Month, PMap<String, income.tax.calculator.Contribution>> entry : yearlyContributions.entrySet()) {
      Map<String, Contribution> monthlyContributions = new HashMap<>();
      for (Map.Entry<String, income.tax.calculator.Contribution> entry2 : entry.getValue().entrySet()) {
        income.tax.calculator.Contribution monthlyContribution = entry2.getValue();
        monthlyContributions.put(
            entry2.getKey(),
            new Contribution(
                monthlyContribution.type,
                monthlyContribution.income,
                monthlyContribution.baseIncome,
                monthlyContribution.rate,
                monthlyContribution.contribution)
            );
      }
      contributions.put(entry.getKey(), HashTreePMap.from(monthlyContributions));
    }
    return new Contributions(contributorId, year, HashTreePMap.from(contributions));
  }
}
