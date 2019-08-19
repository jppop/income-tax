package income.tax.impl;

import akka.Done;
import income.tax.api.CalculationService;
import income.tax.api.Contributor;
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
      CalculationService service = server.client(CalculationService.class);

      OffsetDateTime registrationDate =
          OffsetDateTime.of(
              LocalDate.of(2019, Month.APRIL, 4), LocalTime.NOON, OffsetDateTime.now().getOffset());

      Done done = service.register().invoke(new Contributor("#contributorId", registrationDate)).toCompletableFuture().get(5, SECONDS);
      Assertions.assertThat(done).isNotNull();
    });
  }

}
