package income.tax.api;

import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

import java.time.OffsetDateTime;

@Value
@JsonDeserialize
public final class Contributor {

  public final String contributorId;
  public final OffsetDateTime registrationDate;

  @JsonCreator
  public Contributor(String contributorId, OffsetDateTime registrationDate) {
    this.contributorId = Preconditions.checkNotNull(contributorId, "message");
    this.registrationDate = Preconditions.checkNotNull(registrationDate, "registrationDate");
  }
}
