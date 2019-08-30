package income.tax.impl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.lightbend.lagom.serialization.CompressedJsonable;
import income.tax.api.Income;
import income.tax.contribution.api.Contribution;
import income.tax.impl.tools.IncomeUtils;
import lombok.NonNull;
import lombok.Value;
import org.pcollections.HashTreePMap;
import org.pcollections.IntTreePMap;
import org.pcollections.PMap;

import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * The state for the {@link IncomeTaxEntity} entity.
 */
@SuppressWarnings("serial")
@Value
@JsonDeserialize
public final class IncomeTaxState implements CompressedJsonable {

  public static IncomeTaxState initial
      = IncomeTaxState.of("john doe", OffsetDateTime.now(ZoneOffset.UTC));

  public final @NonNull
  String contributorId;

  public final
  boolean isRegistered;

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
      @NonNull String contributorId, boolean isRegistered, @NonNull OffsetDateTime registeredDate,
      @NonNull PMap<Integer, Income> previousYearlyIncomes,
      int contributionYear, PMap<Month, Income> currentIncomes,
      @NonNull ContributionState contributions) {
    this.contributorId = Preconditions.checkNotNull(contributorId, "message");
    this.isRegistered = isRegistered;
    this.registeredDate = Preconditions.checkNotNull(registeredDate, "registeredDate");
    this.previousYearlyIncomes = Preconditions.checkNotNull(previousYearlyIncomes, "yearlyPreviousIncomes");
    this.contributionYear = contributionYear;
    this.currentIncomes = Preconditions.checkNotNull(currentIncomes, "currentIncomes");
    this.contributions = Preconditions.checkNotNull(contributions, "contributions");
  }

  static IncomeTaxState of(String contributorId, boolean isRegistered, OffsetDateTime registeredDate, Income previousYearlyIncome) {
    return new IncomeTaxState(contributorId, isRegistered, registeredDate,
        IntTreePMap.singleton(registeredDate.getYear() - 1, previousYearlyIncome),
        registeredDate.getYear(), HashTreePMap.empty(), ContributionState.empty());
  }
  static IncomeTaxState of(String contributorId, OffsetDateTime registeredDate) {
    return IncomeTaxState.of(contributorId, false, registeredDate, Income.ZERO);
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
    private ContributionState contributionState;
    private int contributionYear;

    public Modifier(IncomeTaxState currentState) {
      this.currentState = currentState;
      this.previousYearlyIncomes = currentState.previousYearlyIncomes;
      this.currentIncomes = currentState.currentIncomes;
      this.contributionState = currentState.contributions;
      this.contributionYear = currentState.contributionYear;
    }

    public Modifier withNewPreviousYearlyIncome(Map<Integer, Income> newPreviousYearlyIncome) {
      this.previousYearlyIncomes = this.previousYearlyIncomes.plusAll(newPreviousYearlyIncome);
      return this;
    }

    public Modifier withNewCurrentIncomes(Map<Month, Income> newCurrentIncomes) {
      this.currentIncomes = this.currentIncomes.plusAll(newCurrentIncomes);
      return this;
    }

    public Modifier withNewIncome(Income newIncome) {
      // spread income over months
      Map<Month, Income> spreadIncome = IncomeUtils.spreadOutOverMonths(newIncome);
      this.currentIncomes = this.currentIncomes.plusAll(spreadIncome);
      return this;
    }

    public Modifier withNewContributions(ContributionState newContributionState) {
      this.contributionState = newContributionState;
      return this;
    }

    public Modifier withNewContributions(PMap<Month, PMap<String, Contribution>> contributions) {
      this.contributionState = this.contributionState.update(contributions);
      return this;
    }

    public IncomeTaxState modify() {
      return new IncomeTaxState(
          currentState.contributorId, currentState.isRegistered, currentState.registeredDate,
          this.previousYearlyIncomes,
          this.contributionYear, this.currentIncomes, this.contributionState);
    }

    public Modifier withNewContributionYear(int year) {
      this.contributionYear = year;
      return this;
    }
  }

}
