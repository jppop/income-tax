package income.tax.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.serialization.CompressedJsonable;
import income.tax.api.Income;
import income.tax.impl.calculation.Contribution;
import income.tax.impl.calculation.ContributionType;
import income.tax.impl.calculation.IncomeAdjuster;
import lombok.NonNull;
import lombok.Value;
import org.pcollections.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * The state for the {@link IncomeTaxEntity} entity.
 */
@SuppressWarnings("serial")
@Value
@JsonDeserialize
public final class IncomeTaxState implements CompressedJsonable {

  public static IncomeTaxState empty = new IncomeTaxState("nobody", OffsetDateTime.now(ZoneOffset.UTC));

  public final @NonNull
  String contributorId;

  public final @NonNull
  OffsetDateTime registeredDate;

  public final @NonNull
  IntTreePMap<Income> yearlyPreviousIncomes;

  public final int contributionYear;

  public final @NonNull
  PVector<Income> currentIncomes;

  public final @NonNull
  PMap<ContributionType, Contribution> contributions;

  @JsonCreator
  public IncomeTaxState(
      @NonNull String contributorId, @NonNull OffsetDateTime registeredDate,
      @NonNull IntTreePMap<Income> yearlyPreviousIncomes,
      int contributionYear, PVector<Income> currentIncomes, @NonNull PMap<ContributionType, Contribution> contributions) {
    this.contributorId = Preconditions.checkNotNull(contributorId, "message");
    this.registeredDate = Preconditions.checkNotNull(registeredDate, "registeredDate");
    this.yearlyPreviousIncomes = Preconditions.checkNotNull(yearlyPreviousIncomes, "yearlyPreviousIncomes");
    this.contributionYear = contributionYear;
    this.currentIncomes = Preconditions.checkNotNull(currentIncomes, "currentIncomes");
    this.contributions = Preconditions.checkNotNull(contributions, "contributions");
  }

  IncomeTaxState(@NonNull String contributorId, @NonNull OffsetDateTime registeredDate) {
    this(contributorId, registeredDate, IntTreePMap.empty(), registeredDate.getYear(), TreePVector.empty(), HashTreePMap.empty());
  }

  IncomeTaxState applyIncome(Income income) {
    return this;
  }

  IncomeTaxState with(IncomeAdjuster adjuster) {
    return adjuster.adjust(this);
  }

}
