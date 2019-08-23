package income.tax.impl.domain;

import income.tax.api.Income;
import income.tax.api.IncomeType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.*;

class IncomeAdjustersTest {

  @Test
  void beforeRegistration() {
    // Arrange
    OffsetDateTime registrationDate =
        OffsetDateTime.of(LocalDate.of(2020, Month.APRIL, 1), LocalTime.NOON, ZoneOffset.UTC);
    IncomeTaxState state =
        IncomeTaxState.of("#contributorId", registrationDate);
    Income yearlyIncomeBeforeRegistration = Income.ofYear(24000, registrationDate.getYear() - 1, IncomeType.estimated);

    // Act
    IncomeTaxState newState = state.with(IncomeAdjusters.beforeRegistration(yearlyIncomeBeforeRegistration));

    // Assert
    Assertions.assertThat(newState.contributionYear).isEqualTo(registrationDate.getYear());
    Assertions.assertThat(newState.previousYearlyIncomes)
        .contains(Assertions.entry(registrationDate.getYear() - 1, yearlyIncomeBeforeRegistration));
    Assertions.assertThat(newState.currentIncomes.values())
        .extracting("start.year")
        .containsExactly(2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020);
    Assertions.assertThat(newState.contributions).isEmpty();
  }

  @Test
  void currentYear() {
  }

  @Test
  void newYear() {
  }
}