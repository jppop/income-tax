package income.tax.impl.readside;

import akka.japi.Effect;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.javadsl.TestKit;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import com.lightbend.lagom.javadsl.testkit.ServiceTest;
import com.typesafe.config.ConfigValueFactory;
import income.tax.api.Income;
import income.tax.api.IncomeType;
import income.tax.impl.domain.IncomeTaxCommand;
import income.tax.impl.domain.IncomeTaxEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pcollections.HashTreePMap;

import java.time.*;
import java.util.UUID;

import static com.lightbend.lagom.javadsl.testkit.ServiceTest.defaultSetup;
import static com.lightbend.lagom.javadsl.testkit.ServiceTest.startServer;
import static income.tax.impl.tools.DateUtils.maxLastDayOfYear;
import static income.tax.impl.tools.DateUtils.minFirstDayOfYear;
import static java.util.concurrent.TimeUnit.SECONDS;

@Disabled
class ContributionRepositoryCassandraImplTest {

  private static ServiceTest.TestServer server;

  @BeforeAll
  public static void setup() {
    server =
        startServer(
            defaultSetup()
                .withCassandra()
                .configureBuilder(
                    b ->
                        b.configure(
                            "akka.test.single-expect-default",
                            ConfigValueFactory.fromAnyRef("19s"))));
  }

  @AfterAll
  public static void teardown() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  private PersistentEntityRegistry registry() {
    PersistentEntityRegistry reg = server.injector().instanceOf(PersistentEntityRegistry.class);
    reg.register(IncomeTaxEntity.class);
    return reg;
  }

  private ReadSide readSide() {
    return server.injector().instanceOf(ReadSide.class);
  }

  private void eventually(Effect block) {
    new TestKit(server.system()) {
      {
        awaitAssert(
            Duration.ofSeconds(20),
            () -> {
              try {
                block.apply();
              } catch (RuntimeException e) {
                throw e;
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
              return null;
            });
      }
    };
  }

  // DISCLAIMER: This tests uses non-uniform timeout values (13s, 14s, 15s, ...) as a good practice
  // to help isolate
  // the cause of a timeout in case of test failure.

  @Test
  public void testRegisterAndUpdateReadSide() throws Exception {

    // At system startup event processor is started.
//    readSide().register(EventStreamProcessor.class);

    // persist some events via the Post PersistentEntity
    final String contributorId = UUID.randomUUID().toString();
    final PersistentEntityRef<IncomeTaxCommand> ref1 = registry().refFor(IncomeTaxEntity.class, contributorId);
    final IncomeTaxCommand.Register cmd1 = registerComd(contributorId);
    ref1.ask(cmd1).toCompletableFuture().get(13, SECONDS); // await only for deterministic order

    final CassandraSession cassandraSession = server.injector().instanceOf(CassandraSession.class);

    // Eventually (when the BlogEventProcessor is ready), we can create a PreparedStatement to query
    // the
    // blogsummary table via the CassandraSession,
    // e.g. a Service API request
    eventually(
        () -> {
          // the creation of this PreparedStatement will fail while `contributors` doesn't exist.
          cassandraSession
              .prepare("SELECT id, region, registration_date FROM contributors")
              .toCompletableFuture()
              .get(5, SECONDS);
        });

    final PreparedStatement selectStmt =
        cassandraSession
            .prepare("SELECT id, region, registration_date FROM contributors")
            .toCompletableFuture()
            .get(15, SECONDS);
    final BoundStatement boundSelectStmt = selectStmt.bind();

    eventually(
        () -> {
          // stream from a Cassandra select result set, e.g. response to a Service API request
          final Source<String, ?> queryResult =
              cassandraSession.select(boundSelectStmt).map(row -> row.getString("id"));

          final TestSubscriber.Probe<String> probe =
              queryResult
                  .runWith(TestSink.probe(server.system()), server.materializer())
                  .request(10);
          probe.expectNext(contributorId);
          probe.expectComplete();

        });

  }

  private IncomeTaxCommand.Register registerComd(String contributorId) {
    OffsetDateTime registrationDate =
        OffsetDateTime.of(
            LocalDate.of(
                2019, Month.APRIL, 12),
            LocalTime.NOON,
            OffsetDateTime.now(ZoneOffset.UTC).getOffset());

    OffsetDateTime lastYear = registrationDate.minusYears(1);
    OffsetDateTime lastYearStart = minFirstDayOfYear.apply(lastYear);
    OffsetDateTime lastYearEnd = maxLastDayOfYear.apply(lastYear);
    Income previousYearlyIncome = new Income(12000, IncomeType.estimated, lastYearStart, lastYearEnd);

    return new IncomeTaxCommand.Register(contributorId, registrationDate, previousYearlyIncome, HashTreePMap.empty());
  }
}