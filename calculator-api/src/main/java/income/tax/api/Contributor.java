package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@JsonDeserialize
public class Contributor {

  public final @NonNull
  String contributorId;
  public final @NonNull
  OffsetDateTime registrationDate;
  public final
  Long yearlyIncome;
  public final
  Long yearlyContribution;

  @JsonCreator
  public Contributor(String contributorId, OffsetDateTime registrationDate, Long yearlyIncome, Long yearlyContribution) {
    this.contributorId = contributorId;
    this.registrationDate = registrationDate;
    this.yearlyIncome = yearlyIncome;
    this.yearlyContribution = yearlyContribution;
  }
}
