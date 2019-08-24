package income.tax.impl;

import income.tax.api.CalculationService;
import income.tax.api.Contributions;
import income.tax.api.IncomeType;
import income.tax.api.RegistrationRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.withServer;
import static java.util.concurrent.TimeUnit.SECONDS;

public class CalculationServiceTest {

  @Test
  public void shouldRegisterContributor() throws Exception {
    withServer(defaultSetup().withCassandra(), server -> {
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
              new RegistrationRequest("#contributorId", registrationDate, lastYerIncome, incomeType)
          ).toCompletableFuture().get(5, SECONDS);

      // Assert
      Assertions.assertThat(contributions).isNotNull();
    });
  }

}
