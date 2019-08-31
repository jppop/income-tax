package income.tax.api;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;
import org.pcollections.PSequence;

import java.util.Optional;

import static com.lightbend.lagom.javadsl.api.Service.*;

/**
 * The Hello service interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume the Hello.
 */
public interface CalculationService extends Service {


  /**
   * Register a contributor to the calculation service.
   *
   * <p>
   * Example: <br>
   *   <pre>
   *     curl -X POST -H "Content-Type: application/json" \
   *        -d '{ "contributorId": "#id", "registrationDate": "2019-08-19T14:20:38Z"}, "previousYearlyIncome": 24000, "incomeType": "estimated"' \
   *        /api/income-tax/contributors
   *   </pre>
   * </p>
   */
  ServiceCall<RegistrationRequest, Contributions> register();

  ServiceCall<NotUsed, PSequence<Contributor>> getContributors();

  ServiceCall<NotUsed, Contributions> getContributions(String contributorId, Optional<Integer> year);

  ServiceCall<Income, Contributions> applyIncome(String contributorId, boolean scaleToEnd, boolean dryRun);

  /**
   * This gets published to Kafka.
   */
  Topic<CalculationEvent> calculationEvents();

  @Override
  default Descriptor descriptor() {
    // @formatter:off
    return named("income").withCalls(
        pathCall("/api/income/contributors", this::register),
        pathCall("/api/income/contributors", this::getContributors),
        pathCall("/api/income/contributions/:contributorId/declare?scaleToEnd&dryRun", this::applyIncome),
        pathCall("/api/income/contributions/:contributorId?year", this::getContributions)
    ).withTopics(
        topic("calculation-events", this::calculationEvents)
            // Kafka partitions messages, messages within the same partition will
            // be delivered in order, to ensure that all messages for the same user
            // go to the same partition (and hence are delivered in order with respect
            // to that user), we configure a partition key strategy that extracts the
            // name as the partition key.
            .withProperty(KafkaProperties.partitionKeyStrategy(), CalculationEvent::getContributorId)
    ).withAutoAcl(true);
    // @formatter:on
  }
}
