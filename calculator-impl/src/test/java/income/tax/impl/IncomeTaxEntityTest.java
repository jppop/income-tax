package income.tax.impl;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import income.tax.api.Income;
import income.tax.api.IncomeType;
import org.junit.jupiter.api.*;
import org.pcollections.PVector;

import java.time.*;

import static income.tax.impl.tools.DateUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class IncomeTaxEntityTest {

  public static final String ENTITY_ID = "#ContributorId";
  static ActorSystem system;

  PersistentEntityTestDriver<IncomeTaxCommand, IncomeTaxEvent, IncomeTaxState> driver;

  @BeforeAll
  public static void setup() {
    system = ActorSystem.create(IncomeTaxEntityTest.class.getSimpleName());
  }

  @AfterAll
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  @BeforeEach
  public void setupDriver() {
    driver = new PersistentEntityTestDriver<>(system, new IncomeTaxEntity(), ENTITY_ID);
  }

  @AfterEach
  public void verifyNoIssues() {
    assertThat(driver.getAllIssues()).isEmpty();
  }

  @Test
  public void register() {
    // Arrange
    final String entityId = ENTITY_ID;

    OffsetDateTime registrationDate =
        OffsetDateTime.of(
            LocalDate.of(
                2019, Month.APRIL, 12),
            LocalTime.NOON,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());

    OffsetDateTime lastYear = registrationDate.minusYears(1);
    OffsetDateTime lastYearStart = minFirstDayOfYear.apply(lastYear);
    OffsetDateTime lastYearEnd = maxLastDayOfYear.apply(lastYear);
    Income previousYearlyIncome = new Income(12 * 1000, IncomeType.estimated, lastYearStart, lastYearEnd);

    // Act
    Outcome<IncomeTaxEvent, IncomeTaxState> outcome =
        driver.run(new IncomeTaxCommand.Register(entityId, registrationDate, previousYearlyIncome));

    // Assert
    assertThat(outcome.events()).hasSize(1);
    assertThat(outcome.events().get(0)).isEqualTo(new IncomeTaxEvent.Registered(entityId, registrationDate, previousYearlyIncome));
    assertThat(outcome.state().contributorId).isEqualTo(entityId);
    assertThat(outcome.state().registeredDate).isEqualTo(registrationDate);
    assertThat(outcome.state().previousYearlyIncomes)
        .isNotEmpty()
        .contains(entry(lastYear.getYear(), previousYearlyIncome));
  }

  @Test
  public void applyIncome() {
    // Arrange
    final String entityId = ENTITY_ID;

    OffsetDateTime registrationDate =
        OffsetDateTime.of(
            LocalDate.of(
                2019, Month.APRIL, 12),
            LocalTime.NOON,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());

    OffsetDateTime lastYear = registrationDate.minusYears(1);
    OffsetDateTime lastYearStart = minFirstDayOfYear.apply(lastYear);
    OffsetDateTime lastYearEnd = maxLastDayOfYear.apply(lastYear);
    Income previousYearlyIncome = new Income(12 * 1000, IncomeType.estimated, lastYearStart, lastYearEnd);
    Outcome<IncomeTaxEvent, IncomeTaxState> initialIncome =
        driver.run(new IncomeTaxCommand.Register(entityId, registrationDate, previousYearlyIncome));

    // Act
    OffsetDateTime month =
        OffsetDateTime.of(
            LocalDate.of(
                2019, Month.APRIL, 15),
            LocalTime.NOON,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());
    Income monthlyIncome =
        new Income(1500, IncomeType.estimated, minFirstDayOfMonth.apply(month), maxLastDayOfMonth.apply(month));

    Outcome<IncomeTaxEvent, IncomeTaxState> outcome =
        driver.run(new IncomeTaxCommand.ApplyIncome(entityId, monthlyIncome));

    // Assert
    assertThat(outcome.events()).hasSize(1);
    assertThat(outcome.events().get(0)).isInstanceOf(IncomeTaxEvent.IncomeApplied.class);
    assertThat(outcome.state().contributorId).isEqualTo(entityId);
    assertThat(outcome.state().currentIncomes)
        .hasSize(12)
        .contains(monthlyIncome);

    PVector<Income> currentIncomes = outcome.state().currentIncomes;
    long yearlyIncome = currentIncomes.stream().mapToLong(Income::getIncome).sum();
    long expectedYearlyIncome =
        (registrationDate.getMonthValue() - 1) * (previousYearlyIncome.income / 12)
        + (12 - registrationDate.getMonthValue() + 1) * monthlyIncome.income;
    assertThat(yearlyIncome).isEqualTo(expectedYearlyIncome);

    long incomeBeforeRegistration =
        currentIncomes.stream()
            .filter(income -> income.end.isBefore(registrationDate)).mapToLong(Income::getIncome).sum();
    long expectedIncomeBeforeRegistration =
        (registrationDate.getMonthValue() - 1) * (previousYearlyIncome.income / 12);
    assertThat(incomeBeforeRegistration).isEqualTo(expectedIncomeBeforeRegistration);
  }

}
