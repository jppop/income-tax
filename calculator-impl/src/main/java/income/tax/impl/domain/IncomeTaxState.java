package income.tax.impl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.serialization.CompressedJsonable;
import income.tax.api.Income;
import lombok.NonNull;
import lombok.Value;
import org.pcollections.HashTreePMap;
import org.pcollections.IntTreePMap;
import org.pcollections.PMap;

import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * The state for the {@link IncomeTaxEntity} entity.
 */
@SuppressWarnings("serial")
@Value
@JsonDeserialize
public final class IncomeTaxState implements CompressedJsonable {

  public static IncomeTaxState initial = IncomeTaxState.of("john doe", OffsetDateTime.now(ZoneOffset.UTC));

  public final @NonNull
  String contributorId;

  public final @NonNull
  OffsetDateTime registeredDate;

  public final @NonNull
  PMap<Integer, Income> previousYearlyIncomes;

  public final int contributionYear;

  public final @NonNull
  PMap<Month, Income> currentIncomes;

  public final @NonNull
  ContributionState contributions;

  @JsonCreator
  public IncomeTaxState(
      @NonNull String contributorId, @NonNull OffsetDateTime registeredDate,
      @NonNull PMap<Integer, Income> previousYearlyIncomes,
      int contributionYear, PMap<Month, Income> currentIncomes,
      @NonNull ContributionState contributions) {
    this.contributorId = Preconditions.checkNotNull(contributorId, "message");
    this.registeredDate = Preconditions.checkNotNull(registeredDate, "registeredDate");
    this.previousYearlyIncomes = Preconditions.checkNotNull(previousYearlyIncomes, "yearlyPreviousIncomes");
    this.contributionYear = contributionYear;
    this.currentIncomes = Preconditions.checkNotNull(currentIncomes, "currentIncomes");
    this.contributions = Preconditions.checkNotNull(contributions, "contributions");
  }

  static IncomeTaxState of(String contributorId, OffsetDateTime registeredDate) {
    return new IncomeTaxState(contributorId, registeredDate,
        IntTreePMap.empty(),
        registeredDate.getYear(), HashTreePMap.empty(), ContributionState.empty());
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
    private PMap<Month, Income> currentIncomes;
    private ContributionState contributions;
    private int contributionYear;

    public Modifier(IncomeTaxState currentState) {
      this.currentState = currentState;
      this.previousYearlyIncomes = currentState.previousYearlyIncomes;
      this.currentIncomes = currentState.currentIncomes;
      this.contributions = currentState.contributions;
      this.contributionYear = currentState.contributionYear;
    }

    public Modifier withNewPreviousYearlyIncome(PMap<Integer, Income> newPreviousYearlyIncome) {
      this.previousYearlyIncomes = newPreviousYearlyIncome;
      return this;
    }

    public Modifier withNewCurrentIncomes(PMap<Month, Income> newCurrentIncomes) {
      this.currentIncomes = newCurrentIncomes;
      return this;
    }

    public Modifier withNewContributions(ContributionState newContributions) {
      this.contributions = newContributions;
      return this;
    }

    public IncomeTaxState modify() {
      return new IncomeTaxState(
          currentState.contributorId, currentState.registeredDate,
          this.previousYearlyIncomes,
          this.contributionYear, this.currentIncomes, this.contributions);
    }

    public Modifier withNewContributionYear(int year) {
      this.contributionYear = year;
      return this;
    }
  }

}
