package income.tax.impl;

import com.lightbend.lagom.javadsl.api.ServiceCall;
import income.tax.api.*;
import income.tax.contribution.api.CalculatorService;
import income.tax.contribution.api.Contribution;
import income.tax.contribution.api.MonthlyIncomeRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pcollections.PSequence;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.*;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.*;
import static income.tax.impl.tools.DateUtils.maxLastDayOfMonth;
import static income.tax.impl.tools.DateUtils.minFirstDayOfMonth;
import static java.util.concurrent.TimeUnit.SECONDS;

public class CalculationServiceTest {

  public static final String CONTRIBUTOR_ID = "#contributorId";
  public static final int TIMEOUT = 500;
  private static TestServer server;

  @BeforeAll
  public static void setUp() {
    server = startServer(
        defaultSetup()
            .withCluster()
            .withCassandra()
            .configureBuilder(builder -> builder.overrides(bind(CalculatorService.class).to(MockedCalculator.class)))
    );
  }

  @AfterAll
  public static void tearDown() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  @Test
  public void shouldRegisterContributor() throws Exception {
    // Arrange
    CalculationService service = server.client(CalculationService.class);

    OffsetDateTime registrationDate =
        OffsetDateTime.of(
            LocalDate.of(2019, Month.APRIL, 12), LocalTime.NOON, OffsetDateTime.now().getOffset());

    long lastYerIncome = (registrationDate.getMonthValue() - 1) * 2000;
    IncomeType incomeType = IncomeType.estimated;

    // Act
    Contributions contributions =
        service.register().invoke(
            new RegistrationRequest(CONTRIBUTOR_ID, registrationDate, lastYerIncome, incomeType)
        ).toCompletableFuture().get(TIMEOUT, SECONDS);

    // Assert
    Assertions.assertThat(contributions).isNotNull();
    System.out.println(contributions);
  }

  @Test
  public void shouldApplyIncome() throws Exception {
    // Arrange
    CalculationService service = server.client(CalculationService.class);

    String contributorId = UUID.randomUUID().toString();
    registerContributor(contributorId);

    OffsetDateTime month =
        OffsetDateTime.of(
            LocalDate.of(
                2019, Month.APRIL, 15),
            LocalTime.NOON,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());
    Income monthlyIncome =
        new Income(1500, IncomeType.estimated,
            minFirstDayOfMonth.apply(month), maxLastDayOfMonth.apply(month));

    // Act
    Contributions contributions =
        service.applyIncome(contributorId, true, false)
            .invoke(monthlyIncome)
            .toCompletableFuture()
            .get(TIMEOUT, SECONDS);

    // Assert
    Assertions.assertThat(contributions).isNotNull();
    System.out.println(contributions);
  }

  @Disabled("shouldFindContributors: failed to connect to cassandra. lagom issue ?")
  @Test
  public void shouldFindContributors() throws Exception {
    // Arrange
    CalculationService service = server.client(CalculationService.class);

    String contributorId = UUID.randomUUID().toString();
    registerContributor(contributorId);

    // Act
    PSequence<Contributor> contributors = service.getContributors().invoke()
        .toCompletableFuture().get(TIMEOUT, SECONDS);

    // Assert
    Assertions.assertThat(contributors)
        .extracting("contributorId")
        .contains(contributorId);
  }

  private static class MockedCalculator implements CalculatorService {

    private static final MathContext mc = new MathContext(8, RoundingMode.HALF_DOWN);
    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public ServiceCall<MonthlyIncomeRequest, Map<String, Contribution>> compute() {
      return monthlyIncomeRequest -> {
        int contributionIndex = counter.addAndGet(1) % TIMEOUT;
        String contributionType = String.format("MOCK%03d", contributionIndex);
        BigDecimal baseIncome = monthlyIncomeRequest.income.multiply(new BigDecimal("0.75"));
        BigDecimal rate = new BigDecimal("0.075");
        BigDecimal contribution = baseIncome.multiply(rate, mc);
        final Map<String, Contribution> mockedContribution =
            Collections.singletonMap(contributionType,
                new Contribution(contributionType, monthlyIncomeRequest.income, baseIncome, rate, contribution));
        return CompletableFuture.completedFuture(mockedContribution);
      };
    }
  }

  private Contributions registerContributor(String contributorId) throws InterruptedException, ExecutionException, TimeoutException {
    OffsetDateTime registrationDate =
        OffsetDateTime.of(
            LocalDate.of(2019, Month.APRIL, 12), LocalTime.NOON, OffsetDateTime.now().getOffset());

    long lastYerIncome = (registrationDate.getMonthValue() - 1) * 2000;
    IncomeType incomeType = IncomeType.estimated;

    CalculationService service = server.client(CalculationService.class);
    Contributions contributions =
        service.register().invoke(
            new RegistrationRequest(contributorId, registrationDate, lastYerIncome, incomeType)
        ).toCompletableFuture().get(TIMEOUT, SECONDS);

    return contributions;
  }
}
