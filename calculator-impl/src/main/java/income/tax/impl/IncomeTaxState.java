package income.tax.impl;

import lombok.NonNull;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.serialization.CompressedJsonable;
import lombok.experimental.NonFinal;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * The state for the {@link IncomeTaxEntity} entity.
 */
@SuppressWarnings("serial")
@Value
@JsonDeserialize
public final class IncomeTaxState implements CompressedJsonable {

  public @NonNull String contributorId;
  public @NonNull OffsetDateTime registeredDate;

  @NonFinal
  private BigDecimal yearlyIncome;

  @JsonCreator
  public IncomeTaxState(@NonNull String contributorId, @NonNull OffsetDateTime registeredDate) {
    this.contributorId = Preconditions.checkNotNull(contributorId, "message");
    this.registeredDate = Preconditions.checkNotNull(registeredDate, "registeredDate");
  }

  public void applyPreviousIncome(long income) {
    this.yearlyIncome = BigDecimal.valueOf(income, 2);
  }

  public static IncomeTaxState empty = new IncomeTaxState("nobody", OffsetDateTime.now(ZoneOffset.UTC));
}
