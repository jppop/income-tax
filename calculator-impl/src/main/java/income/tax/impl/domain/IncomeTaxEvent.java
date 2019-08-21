package income.tax.impl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.javadsl.persistence.AggregateEvent;
import com.lightbend.lagom.javadsl.persistence.AggregateEventShards;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.AggregateEventTagger;
import com.lightbend.lagom.serialization.Jsonable;
import income.tax.api.Income;
import lombok.Value;

import java.time.OffsetDateTime;

/**
 * This interface defines all the events that the IncomeTax entity supports.
 * <p>
 * By convention, the events should be inner classes of the interface, which
 * makes it simple to get a complete picture of what events an entity has.
 */
public interface IncomeTaxEvent extends Jsonable, AggregateEvent<IncomeTaxEvent> {

  /**
   * Tags are used for getting and publishing streams of events. Each event
   * will have this tag, and in this case, we are partitioning the tags into
   * 4 shards, which means we can have 4 concurrent processors/publishers of
   * events.
   */
  AggregateEventShards<IncomeTaxEvent> TAG = AggregateEventTag.sharded(IncomeTaxEvent.class, 4);

  /**
   * An event that represents a change in greeting message.
   */
  @SuppressWarnings("serial")
  @Value
  @JsonDeserialize
  final class Registered implements IncomeTaxEvent {

    public final String contributorId;
    public final OffsetDateTime registrationDate;
    public final Income previousYearlyIncome;

    @JsonCreator
    public Registered(String contributorId, OffsetDateTime registrationDate, Income previousYearlyIncome) {
      this.contributorId = Preconditions.checkNotNull(contributorId, "name");
      this.registrationDate = Preconditions.checkNotNull(registrationDate, "registrationDate");
      this.previousYearlyIncome = Preconditions.checkNotNull(previousYearlyIncome, "previousYearlyIncome");
    }
  }

  @Value
  final class IncomeApplied implements IncomeTaxEvent {
    public final String contributorId;
    public final OffsetDateTime createdAt;
    public final Income income;

    @JsonCreator
    public IncomeApplied(String contributorId, Income income, OffsetDateTime createdAt) {
      this.contributorId = Preconditions.checkNotNull(contributorId, "contributorId");
      this.income =  Preconditions.checkNotNull(income, "income");
      this.createdAt = Preconditions.checkNotNull(createdAt, "createdAt");
    }
  }

  final class PreviousIncomeApplied implements IncomeTaxEvent {
    public final String contributorId;
    public final OffsetDateTime createdAt;
    public final Income income;

    @JsonCreator
    public PreviousIncomeApplied(String contributorId, Income income, OffsetDateTime createdAt) {
      this.contributorId = Preconditions.checkNotNull(contributorId, "contributorId");
      this.income =  Preconditions.checkNotNull(income, "income");
      this.createdAt = Preconditions.checkNotNull(createdAt, "createdAt");
    }
  }

  @Override
  default AggregateEventTagger<IncomeTaxEvent> aggregateTag() {
    return TAG;
  }

}