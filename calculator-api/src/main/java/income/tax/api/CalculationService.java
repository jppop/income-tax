package income.tax.api;

import akka.Done;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.broker.kafka.KafkaProperties;

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
   *        -d '{ "contributorId": "#id", "registrationDate": "2019-08-19T14:20:38Z"}' \
   *        /api/income-tax/contributors
   *   </pre>
   * </p>
   */
  ServiceCall<RegistrationRequest, Done> register();

  ServiceCall<Income, Done> applyIncome(String contributorId);
  /**
   * This gets published to Kafka.
   */
  Topic<CalculationEvent> calculationEvents();

  @Override
  default Descriptor descriptor() {
    // @formatter:off
    return named("calculation").withCalls(
        pathCall("/api/income-tax/contributors", this::register),
        pathCall("/api/income-tax/contributors/:contributorId/apply", this::applyIncome)
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
