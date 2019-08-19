package income.tax.stream.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import income.tax.api.CalculationService;
import income.tax.api.Contributor;
import income.tax.stream.api.StreamService;

import javax.inject.Inject;

import java.util.Optional;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Implementation of the HelloString.
 */
public class StreamServiceImpl implements StreamService {

  private final CalculationService calculationService;
  private final StreamRepository repository;

  @Inject
  public StreamServiceImpl(CalculationService calculationService, StreamRepository repository) {
    this.calculationService = calculationService;
    this.repository = repository;
  }

  @Override
  public ServiceCall<Source<Contributor, NotUsed>, Source<Done, NotUsed>> directStream() {
    return registrations -> completedFuture(
        registrations.mapAsync(8, contributor ->  calculationService.register().invoke(contributor)));
  }

  @Override
  public ServiceCall<Source<String, NotUsed>, Source<Contributor, NotUsed>> autonomousStream() {
    return hellos -> completedFuture(
        hellos.mapAsync(8, id -> repository.getContributor(id).thenApply(Optional::get)));
  }
}
