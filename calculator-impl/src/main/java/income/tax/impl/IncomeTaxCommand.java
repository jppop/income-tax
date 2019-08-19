package income.tax.impl;

import income.tax.api.Income;
import lombok.NonNull;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;

import akka.Done;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * This interface defines all the commands that the IncomeTax entity supports.
 * 
 * By convention, the commands should be inner classes of the interface, which
 * makes it simple to get a complete picture of what commands an entity
 * supports.
 */
public interface IncomeTaxCommand extends Jsonable {

  /**
   * A command to register a contributor.
   * <p>
   * It has a reply type of {@link akka.Done}, which is sent back to the caller
   * when all the events emitted by this command are successfully persisted.
   */
  @SuppressWarnings("serial")
  @Value
  @JsonDeserialize
  final class Register implements IncomeTaxCommand, CompressedJsonable, PersistentEntity.ReplyType<Done> {
    public final @NonNull String contributorId;
    public final @NonNull OffsetDateTime registrationDate;

    @JsonCreator
    public Register(@NonNull String contributorId, @NonNull OffsetDateTime registrationDate) {
      this.contributorId = Preconditions.checkNotNull(contributorId, "contributorId");
      this.registrationDate = Preconditions.checkNotNull(registrationDate, "registrationDate");
    }
  }

  /**
   * A command to say hello to someone using the current greeting message.
   * <p>
   * The reply type is String, and will contain the message to say to that
   * person.
   */
  @SuppressWarnings("serial")
  @Value
  @JsonDeserialize
    final class ApplyIncome implements IncomeTaxCommand, PersistentEntity.ReplyType<Done> {

    public final @NonNull String contributorId;
    public final @NonNull Income income;

    @JsonCreator
    public ApplyIncome(String contributorId, Income income) {
      this.contributorId = Preconditions.checkNotNull(contributorId, "contributorId");
      this.income = Preconditions.checkNotNull(income, "income");
    }
  }

}
