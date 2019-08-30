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

  @JsonCreator
  public Contributor(String contributorId, OffsetDateTime registrationDate) {
    this.contributorId = contributorId;
    this.registrationDate = registrationDate;
  }
}
