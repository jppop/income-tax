package income.tax.impl.domain;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import income.tax.api.Contributions;
import income.tax.api.Income;
import income.tax.api.IncomeType;
import income.tax.contribution.api.Contribution;
import income.tax.impl.tools.IncomeUtils;
import org.junit.jupiter.api.*;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.NoSerializer;
import static income.tax.impl.tools.DateUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class IncomeTaxEntityTest {

  private static final int LASTYEAR_MONTH_INCOME = 1000;
  private static final String ENTITY_ID = "#ContributorId";
  private static ActorSystem system;

  private PersistentEntityTestDriver<IncomeTaxCommand, IncomeTaxEvent, IncomeTaxState> driver;

  @BeforeAll
  public static void setup() {
    system = ActorSystem.create(IncomeTaxEntityTest.class.getSimpleName());
  }

  @AfterAll
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
    system = null;
  }

  static private long quaterIncome(int month) {
    int quarter = ((month - 1) / 3) + 1;
    int monthOfQuarter = (month - 1) % 3;
    int income = 1100 + (100 * quarter) + (10 * monthOfQuarter);
    return income;
  }

  @BeforeEach
  public void setupDriver() {
    driver = new PersistentEntityTestDriver<>(system, new IncomeTaxEntity(), ENTITY_ID);
  }

  @AfterEach
  public void verifyNoIssues() {
    // FIXME: the driver complains that there is no serializer for Contributions class.
    List<PersistentEntityTestDriver.Issue> issues = driver.getAllIssues().stream()
        .filter(issue -> {
          if (issue.getClass().isAssignableFrom(NoSerializer.class)) {
            NoSerializer noSerializerIssue = (NoSerializer) issue;
            return !noSerializerIssue.obj().getClass().isAssignableFrom(Contributions.class);
          }
          return true;
        })
        .collect(Collectors.toList());
    assertThat(issues).isEmpty();
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
    OffsetDateTime decemberOfLastYear = lastYear.withMonth(12);
    OffsetDateTime lastYearStart = minFirstDayOfYear.apply(lastYear);
    OffsetDateTime lastYearEnd = maxLastDayOfYear.apply(lastYear);
    Income previousYearlyIncome = new Income(12 * LASTYEAR_MONTH_INCOME, IncomeType.estimated, lastYearStart, lastYearEnd);

    // Act
    Outcome<IncomeTaxEvent, IncomeTaxState> outcome =
        driver.run(new IncomeTaxCommand.Register(entityId, registrationDate, previousYearlyIncome, HashTreePMap.empty()));

    // Assert
    assertThat(outcome.events()).hasSize(2);
    assertThat(outcome.events().get(0)).isEqualTo(new IncomeTaxEvent.Registered(entityId, registrationDate, previousYearlyIncome));
    assertThat(outcome.events().get(1)).isInstanceOf(IncomeTaxEvent.IncomeApplied.class);
    assertThat(outcome.state().contributorId).isEqualTo(entityId);
    assertThat(outcome.state().registeredDate).isEqualTo(registrationDate);
    assertThat(outcome.state().previousYearlyIncomes)
        .isNotEmpty()
        .contains(entry(lastYear.getYear(), previousYearlyIncome));
  }

  @Test
  public void applyMonthlyIncome() {
    // Arrange
    final String contributorId = ENTITY_ID;

    IncomeTaxState incomeTaxState = initialState(contributorId, 2020);
    driver.initialize(Optional.of(incomeTaxState));

    OffsetDateTime registrationDate = incomeTaxState.registeredDate;
    Income previousYearlyIncome = incomeTaxState.previousYearlyIncomes.get(registrationDate.getYear() - 1);

    // Act
    OffsetDateTime month =
        OffsetDateTime.of(
            LocalDate.of(
                incomeTaxState.contributionYear, Month.APRIL, 15),
            LocalTime.NOON,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());
    Income monthlyIncome =
        new Income(1500, IncomeType.estimated,
            minFirstDayOfMonth.apply(month), maxLastDayOfMonth.apply(month));
    Income incomeToTheEndOfYear = IncomeUtils.scaleToEndOfYear(monthlyIncome);
    final PMap<Month, PMap<String, Contribution>> contributions = getContributions(IncomeUtils.spreadOutOverMonths(incomeToTheEndOfYear));

    Outcome<IncomeTaxEvent, IncomeTaxState> outcome =
        driver.run(new IncomeTaxCommand.ApplyIncome(contributorId, incomeToTheEndOfYear, true, false, contributions));

    // Assert
    assertThat(outcome.events()).hasSize(1);
    assertThat(outcome.events().get(0)).isInstanceOf(IncomeTaxEvent.IncomeApplied.class);
    assertThat(outcome.state().contributorId).isEqualTo(contributorId);
    assertThat(outcome.state().currentIncomes)
        .hasSize(12)
        .contains(entry(monthlyIncome.start.getMonth(), monthlyIncome));

    PMap<Month, Income> currentIncomes = outcome.state().currentIncomes;
    long yearlyIncome = currentIncomes.values().stream().mapToLong(Income::getIncome).sum();
    long expectedYearlyIncome =
        (registrationDate.getMonthValue() - 1) * (previousYearlyIncome.income / 12)
            + (12 - registrationDate.getMonthValue() + 1) * monthlyIncome.income;
    assertThat(yearlyIncome).isEqualTo(expectedYearlyIncome);

    long incomeBeforeRegistration =
        currentIncomes.values().stream()
            .filter(income -> income.end.isBefore(registrationDate)).mapToLong(Income::getIncome).sum();
    long expectedIncomeBeforeRegistration =
        (registrationDate.getMonthValue() - 1) * (previousYearlyIncome.income / 12);
    assertThat(incomeBeforeRegistration).isEqualTo(expectedIncomeBeforeRegistration);
  }

  @Test
  public void applyQuarterIncome() {
    // Arrange
    final String contributorId = ENTITY_ID;

    IncomeTaxState incomeTaxState = initialState(contributorId, 2021);
    driver.initialize(Optional.of(incomeTaxState));

    OffsetDateTime registrationDate = incomeTaxState.registeredDate;
    Income previousYearlyIncome = incomeTaxState.previousYearlyIncomes.get(registrationDate.getYear() - 1);
    PMap<Month, Income> currentIncomes = incomeTaxState.currentIncomes;
    long previousQuaterIncome = currentIncomes.values().stream()
        .filter(income -> 7 <= income.start.getMonthValue() && income.start.getMonthValue() <= 9)
        .mapToLong(Income::getIncome).sum();

    // Act
    // apply income from july to september
    LocalDate start = LocalDate.of(incomeTaxState.contributionYear, Month.JULY, 1);
    LocalDate end = start.plusMonths(2);
    Income quarterIncome =
        new Income(2000L + previousQuaterIncome, IncomeType.estimated,
            minFirstDayOfMonthFromDate.apply(start), maxLastDayOfMonthFromDate.apply(end));

    Outcome<IncomeTaxEvent, IncomeTaxState> outcome =
        driver.run(new IncomeTaxCommand.ApplyIncome(contributorId, quarterIncome, false, false, HashTreePMap.empty()));

    // Assert
    assertThat(outcome.events()).hasSize(1);
    assertThat(outcome.events().get(0)).isInstanceOf(IncomeTaxEvent.IncomeApplied.class);
    assertThat(outcome.state().contributorId).isEqualTo(contributorId);
    assertThat(outcome.state().currentIncomes)
        .hasSize(12);
    long spreadOutValue = quarterIncome.income / 3;
    long remainder = quarterIncome.income % 3;
    assertThat(outcome.state().currentIncomes.get(quarterIncome.start.getMonth()))
        .hasFieldOrPropertyWithValue("income", spreadOutValue);
    assertThat(outcome.state().currentIncomes.get(quarterIncome.start.plusMonths(1).getMonth()))
        .hasFieldOrPropertyWithValue("income", spreadOutValue);
    assertThat(outcome.state().currentIncomes.get(quarterIncome.start.plusMonths(2).getMonth()))
        .hasFieldOrPropertyWithValue("income", spreadOutValue + remainder);

    PMap<Month, Income> newCurrentIncomes = outcome.state().currentIncomes;
    long yearlyIncome = newCurrentIncomes.values().stream().mapToLong(Income::getIncome).sum();
    long expectedYearlyIncome =
        2000 + currentIncomes.values().stream().mapToLong(Income::getIncome).sum();
    assertThat(yearlyIncome).isEqualTo(expectedYearlyIncome);

  }

  private Map<Month, Income> yearlyIncome(int year, long... amounts) {
    assertThat(amounts).hasSize(12);
    Map<Month, Income> yearlyIncomes = new HashMap<>(12);
    int month = 0;
    for (long monthlyIncome : amounts) {
      month++;
      LocalDate monthDate = LocalDate.of(year, month, 1);
      OffsetDateTime monthTime = OffsetDateTime.of(monthDate, LocalTime.MIN, ZoneOffset.UTC);
      Income income =
          new Income(monthlyIncome, IncomeType.estimated, monthTime, monthTime.with(TemporalAdjusters.lastDayOfMonth()));
      yearlyIncomes.put(Month.of(month), income);
    }
    return yearlyIncomes;
  }

  final static MathContext mc = new MathContext(8, RoundingMode.HALF_DOWN);

  private IncomeTaxState initialState(String contributorId, int registrationYear) {

    OffsetDateTime registrationDate =
        OffsetDateTime.of(
            LocalDate.of(
                registrationYear, Month.APRIL, 12),
            LocalTime.NOON,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());

    OffsetDateTime lastYear = registrationDate.minusYears(1);
    OffsetDateTime lastYearStart = minFirstDayOfYear.apply(lastYear);
    OffsetDateTime lastYearEnd = maxLastDayOfYear.apply(lastYear);
    Income previousYearlyIncome = new Income(12 * LASTYEAR_MONTH_INCOME, IncomeType.estimated, lastYearStart, lastYearEnd);
    Map<Month, Income> currentIncomes = yearlyIncome(registrationYear,
        LASTYEAR_MONTH_INCOME, LASTYEAR_MONTH_INCOME, LASTYEAR_MONTH_INCOME,
        quaterIncome(4), quaterIncome(5), quaterIncome(6),
        quaterIncome(7), quaterIncome(8), quaterIncome(9),
        quaterIncome(10), quaterIncome(11), quaterIncome(12)
    );

    final PMap<Month, PMap<String, Contribution>> contributions = getContributions(currentIncomes);
    return
        IncomeTaxState.of(contributorId, true, registrationDate, previousYearlyIncome)
            .modifier()
            .withNewCurrentIncomes(currentIncomes)
            .withNewContributions(contributions)
            .modify();

  }

  private PMap<Month, PMap<String, Contribution>> getContributions(Map<Month, Income> currentIncomes) {
    final Map<Month, PMap<String, Contribution>> contributions = new HashMap<>();
    currentIncomes.forEach((month, income) -> {
      Map<String, Contribution> calculatedContributions =
          IntStream.rangeClosed(1, 5)
              .mapToObj(index -> {
                String contributionType = String.format("MOCK%03d", index);
                BigDecimal incomeValue = BigDecimal.valueOf(income.income);
                BigDecimal baseIncome = incomeValue.multiply(new BigDecimal("0.75"));
                BigDecimal rate = new BigDecimal("0.075");
                BigDecimal contribution = baseIncome.multiply(rate, mc);
                return new Contribution(contributionType, incomeValue, baseIncome, rate, contribution);
              })
              .collect(Collectors.toMap(Contribution::getType, contribution -> contribution));
      contributions.put(month, HashTreePMap.from(calculatedContributions));
    });
    return HashTreePMap.from(contributions);
  }
}
