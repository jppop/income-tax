package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@JsonDeserialize
public final class Contributor {

  public final String contributorId;
  public final OffsetDateTime registrationDate;
  public final long previousYearlyIncome;
  public final IncomeType incomeType;

  @JsonCreator
  public Contributor(String contributorId, OffsetDateTime registrationDate, long previousYearlyIncome, IncomeType incomeType) {
    this.contributorId = Preconditions.checkNotNull(contributorId, "message");
    this.registrationDate = Preconditions.checkNotNull(registrationDate, "registrationDate");
    this.incomeType = Preconditions.checkNotNull(incomeType, "incomeType");
    Preconditions.checkState(previousYearlyIncome >=0, "incomeBeforeRegistration must be positive");
    this.previousYearlyIncome = previousYearlyIncome;
  }
}
