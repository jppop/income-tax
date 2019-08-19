package income.tax.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Preconditions;
import lombok.Value;

import java.time.OffsetDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = CalculationEvent.Registered.class, name = "income-tax-calculation-registered")
})
public interface CalculationEvent {

  String getContributorId();

  @Value
  final class Registered implements CalculationEvent {
    public final String contributorId;
    public final OffsetDateTime registrationDate;

    @JsonCreator
    public Registered(String contributorId, OffsetDateTime registrationDate) {
        this.contributorId = Preconditions.checkNotNull(contributorId, "contributorId");
        this.registrationDate = Preconditions.checkNotNull(registrationDate, "registrationDate");
    }
  }

}
