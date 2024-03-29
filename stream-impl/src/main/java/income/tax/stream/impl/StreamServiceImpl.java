package income.tax.stream.impl;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import income.tax.api.CalculationService;
import income.tax.api.Contributions;
import income.tax.api.RegistrationRequest;
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
  public ServiceCall<Source<RegistrationRequest, NotUsed>, Source<Contributions, NotUsed>> directStream() {
    return registrations -> completedFuture(
        registrations.mapAsync(8, request ->  calculationService.register().invoke(request)));
  }

  @Override
  public ServiceCall<Source<String, NotUsed>, Source<RegistrationRequest, NotUsed>> autonomousStream() {
    return hellos -> completedFuture(
        hellos.mapAsync(8, id -> repository.getContributor(id).thenApply(Optional::get)));
  }
}
