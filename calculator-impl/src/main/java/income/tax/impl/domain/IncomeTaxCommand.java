package income.tax.impl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import com.lightbend.lagom.serialization.CompressedJsonable;
import com.lightbend.lagom.serialization.Jsonable;
import income.tax.api.Contributions;
import income.tax.api.Income;
import income.tax.api.IncomeType;
import income.tax.contribution.api.Contribution;
import income.tax.impl.tools.DateUtils;
import lombok.NonNull;
import lombok.Value;
import org.pcollections.PMap;

import java.time.Month;
import java.time.OffsetDateTime;

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
  final class Register implements IncomeTaxCommand, CompressedJsonable, PersistentEntity.ReplyType<Contributions> {
    public final @NonNull String contributorId;
    public final @NonNull OffsetDateTime registrationDate;
    public final @NonNull Income previousYearlyIncome;
    public final PMap<Month, PMap<String, Contribution>> contributions;

    @JsonCreator
    public Register(@NonNull String contributorId, @NonNull OffsetDateTime registrationDate, Income previousYearlyIncome, PMap<Month, PMap<String, Contribution>> contributions) {
      this.contributorId = Preconditions.checkNotNull(contributorId, "contributorId");
      this.registrationDate = Preconditions.checkNotNull(registrationDate, "registrationDate");
      this.previousYearlyIncome = Preconditions.checkNotNull(previousYearlyIncome, "previousYearlyIncome");
      this.contributions = contributions;
    }

    public Register(String contributorId, OffsetDateTime registrationDate, long previousYearlyIncome, IncomeType incomeType, PMap<Month, PMap<String, Contribution>> contributions) {
      this.contributorId = Preconditions.checkNotNull(contributorId, "name");
      this.registrationDate = Preconditions.checkNotNull(registrationDate, "registrationDate");
      this.contributions = contributions;
      Preconditions.checkNotNull(incomeType, "incomeType");
      OffsetDateTime lastYear = registrationDate.minusYears(1);
      OffsetDateTime lastYearStart = DateUtils.minFirstDayOfYear.apply(lastYear);
      OffsetDateTime lastYearEnd = DateUtils.maxLastDayOfYear.apply(lastYear);
      this.previousYearlyIncome = new Income(previousYearlyIncome, incomeType, lastYearStart, lastYearEnd);
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
    final class ApplyIncome implements IncomeTaxCommand, PersistentEntity.ReplyType<Contributions> {

    public final @NonNull String contributorId;
    public final @NonNull Income income;
    public final boolean scaleToEnd;
    public final boolean dryRun;
    public final PMap<Month, PMap<String, Contribution>> contributions;

    @JsonCreator
    public ApplyIncome(String contributorId, Income income, boolean scaleToEnd, boolean dryRun, PMap<Month, PMap<String, Contribution>> contributions) {
      this.contributorId = Preconditions.checkNotNull(contributorId, "contributorId");
      this.income = Preconditions.checkNotNull(income, "income");
      this.scaleToEnd = scaleToEnd;
      this.dryRun = dryRun;
      this.contributions = Preconditions.checkNotNull(contributions);
    }
  }

}
