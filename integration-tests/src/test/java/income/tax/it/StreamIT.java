package income.tax.it;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.lightbend.lagom.javadsl.client.integration.LagomClientFactory;
import income.tax.api.CalculationService;
import income.tax.api.Income;
import income.tax.api.IncomeType;
import income.tax.api.RegistrationRequest;
import income.tax.stream.api.StreamService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class StreamIT {

  private static final String SERVICE_LOCATOR_URI = "http://localhost:9008";

  private static LagomClientFactory clientFactory;
  private static CalculationService calculationService;
  private static StreamService streamService;
  private static ActorSystem system;
  private static Materializer mat;

  @BeforeAll
  public static void setup() {
    clientFactory = LagomClientFactory.create("integration-test", StreamIT.class.getClassLoader());
    // One of the clients can use the service locator, the other can use the service gateway, to test them both.
    calculationService = clientFactory.createDevClient(CalculationService.class, URI.create(SERVICE_LOCATOR_URI));
    streamService = clientFactory.createDevClient(StreamService.class, URI.create(SERVICE_LOCATOR_URI));

    system = ActorSystem.create();
    mat = ActorMaterializer.create(system);
  }

  @AfterAll
  public static void tearDown() {
    if (clientFactory != null) {
      clientFactory.close();
    }
    if (system != null) {
      system.terminate();
    }
  }

  @Test
  public void helloWorld() throws Exception {
    OffsetDateTime registrationDate =
        OffsetDateTime.of(
            LocalDate.of(
                2019, Month.APRIL, 12),
            LocalTime.NOON,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());

    OffsetDateTime lastYear = registrationDate.minusYears(1);
    OffsetDateTime lastYearStart = lastYear.with(TemporalAdjusters.firstDayOfYear());
    OffsetDateTime lastYearEnd = lastYear.with(TemporalAdjusters.lastDayOfYear());
    Income previousYearlyIncome = new Income(12 * 1000, IncomeType.estimated, lastYearStart, lastYearEnd);

    Done answer = await(
        calculationService.register().invoke(
            new RegistrationRequest("#contributor", registrationDate,
                previousYearlyIncome.income, previousYearlyIncome.incomeType)));
    Assertions.assertThat(answer).isEqualTo(Done.getInstance());
  }

  @Test
  public void helloStream() throws Exception {
    // Important to concat our source with a maybe, this ensures the connection doesn't get closed once we've
    // finished feeding our elements in, and then also to take 3 from the response stream, this ensures our
    // connection does get closed once we've received the 3 elements.
//    Source<String, ?> response = await(streamService.directStream().invoke(
//        Source.from(Arrays.asList("a", "b", "c"))
//            .concat(Source.maybe())));
//    List<String> messages = await(response.take(3).runWith(Sink.seq(), mat));
//    assertEquals(Arrays.asList("Hello, a!", "Hello, b!", "Hello, c!"), messages);
  }

  private <T> T await(CompletionStage<T> future) throws Exception {
    return future.toCompletableFuture().get(10, TimeUnit.SECONDS);
  }


}
