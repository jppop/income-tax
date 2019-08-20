package income.tax.stream.impl;

import akka.Done;
import akka.stream.javadsl.Flow;
import income.tax.api.CalculationEvent;
import income.tax.api.CalculationService;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;

/**
 * This subscribes to the HelloService event stream.
 */
public class StreamSubscriber {

  @Inject
  public StreamSubscriber(CalculationService calculationService, StreamRepository repository) {
    // Create a subscriber
    calculationService.calculationEvents().subscribe()
      // And subscribe to it with at least once processing semantics.
      .atLeastOnce(
        // Create a flow that emits a Done for each message it processes
        Flow.<CalculationEvent>create().mapAsync(1, event -> {

          if (event instanceof CalculationEvent.Registered) {
            CalculationEvent.Registered registeredEvent = (CalculationEvent.Registered) event;
            // Update the message
            return repository.registerContributor(
                registeredEvent.getContributorId(), registeredEvent.getRegistrationDate(),
                registeredEvent.previousIncome, registeredEvent.previousIncomeType);

          } else {
            // Ignore all other events
            return CompletableFuture.completedFuture(Done.getInstance());
          }
        })
      );

  }
}
