package income.tax.impl;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver;
import com.lightbend.lagom.javadsl.testkit.PersistentEntityTestDriver.Outcome;
import org.junit.jupiter.api.*;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

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

    // Act
    Outcome<IncomeTaxEvent, IncomeTaxState> outcome =
        driver.run(new IncomeTaxCommand.Register(entityId, registrationDate));

    // Assert
    assertThat(outcome.events()).hasSize(1);
    assertThat(outcome.events().get(0)).isEqualTo(new IncomeTaxEvent.Registered(entityId, registrationDate));
    assertThat(outcome.state().contributorId).isEqualTo(entityId);
    assertThat(outcome.state().registeredDate).isEqualTo(registrationDate);
  }

}
