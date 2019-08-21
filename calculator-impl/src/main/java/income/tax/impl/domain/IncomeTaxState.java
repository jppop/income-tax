package income.tax.impl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.serialization.CompressedJsonable;
import income.tax.api.Income;
import income.tax.impl.calculation.Contribution;
import income.tax.impl.calculation.ContributionType;
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

  public static IncomeTaxState empty = new IncomeTaxState("nobody", OffsetDateTime.now(ZoneOffset.UTC), Income.ZERO);

  public final @NonNull
  String contributorId;

  public final @NonNull
  OffsetDateTime registeredDate;

  public final @NonNull
  PMap<Integer, Income> previousYearlyIncomes;

  public final int contributionYear;

  public final @NonNull
  PVector<Income> currentIncomes;

  public final @NonNull
  PMap<ContributionType, Contribution> contributions;

  @JsonCreator
  public IncomeTaxState(
      @NonNull String contributorId, @NonNull OffsetDateTime registeredDate,
      @NonNull PMap<Integer, Income> previousYearlyIncomes,
      int contributionYear, PVector<Income> currentIncomes,
      @NonNull PMap<ContributionType, Contribution> contributions) {
    this.contributorId = Preconditions.checkNotNull(contributorId, "message");
    this.registeredDate = Preconditions.checkNotNull(registeredDate, "registeredDate");
    this.previousYearlyIncomes = Preconditions.checkNotNull(previousYearlyIncomes, "yearlyPreviousIncomes");
    this.contributionYear = contributionYear;
    this.currentIncomes = Preconditions.checkNotNull(currentIncomes, "currentIncomes");
    this.contributions = Preconditions.checkNotNull(contributions, "contributions");
  }

  IncomeTaxState(@NonNull String contributorId, @NonNull OffsetDateTime registeredDate, Income previousYearlyIncomes) {
    this(contributorId, registeredDate,
        IntTreePMap.singleton(previousYearlyIncomes.start.getYear(), previousYearlyIncomes),
        registeredDate.getYear(), TreePVector.empty(), HashTreePMap.empty());
  }

  IncomeTaxState with(IncomeAdjuster adjuster) {
    return adjuster.adjust(this);
  }

  public Modifier modifier() {
    return new Modifier(this);
  }

  public static class Modifier {
    private final IncomeTaxState currentState;
    private PMap<Integer, Income> previousYearlyIncomes;
    private PVector<Income> currentIncomes;
    private PMap<ContributionType, Contribution> contributions;

    public Modifier(IncomeTaxState currentState) {
      this.currentState = currentState;
      this.previousYearlyIncomes = currentState.previousYearlyIncomes;
      this.currentIncomes = currentState.currentIncomes;
      this.contributions = currentState.contributions;
    }

    public Modifier withNewPreviousYearlyIncome(PMap<Integer, Income> newPreviousYearlyIncome) {
      this.previousYearlyIncomes = newPreviousYearlyIncome;
      return this;
    }

    public Modifier withNewCurrentIncomes(PVector<Income> newCurrentIncomes) {
      this.currentIncomes = newCurrentIncomes;
      return this;
    }

    public Modifier withNewContributions(PMap<ContributionType, Contribution> newContributions) {
      this.contributions = newContributions;
      return this;
    }

    public IncomeTaxState modify() {
      return new IncomeTaxState(
          currentState.contributorId, currentState.registeredDate,
          this.previousYearlyIncomes,
          currentState.contributionYear, this.currentIncomes, this.contributions);
    }
  }

}