package income.tax.impl;

import akka.NotUsed;
import akka.japi.Pair;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.api.transport.BadRequest;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import income.tax.api.*;
import income.tax.contribution.api.CalculatorService;
import income.tax.contribution.api.Contribution;
import income.tax.contribution.api.MonthlyIncomeRequest;
import income.tax.impl.domain.IncomeTaxCommand;
import income.tax.impl.domain.IncomeTaxEntity;
import income.tax.impl.domain.IncomeTaxEvent;
import income.tax.impl.message.Messages;
import income.tax.impl.readside.ContributionRepository;
import income.tax.impl.readside.EventStreamProcessor;
import income.tax.impl.tools.IncomeUtils;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the HelloService.
 */
public class CalculationServiceImpl implements CalculationService {

  private static Logger logger = LoggerFactory.getLogger(CalculationServiceImpl.class);

  private final PersistentEntityRegistry persistentEntityRegistry;
  private final ContributionRepository repository;
  private final CalculatorService calculatorService;

  @Inject
  public CalculationServiceImpl(
      PersistentEntityRegistry persistentEntityRegistry,
      ReadSide readSide,
      ContributionRepository repository,
      CalculatorService calculatorService
  ) {
    this.persistentEntityRegistry = persistentEntityRegistry;
    this.repository = repository;
    this.calculatorService = calculatorService;

    persistentEntityRegistry.register(IncomeTaxEntity.class);
    readSide.register(EventStreamProcessor.class);
  }

  @Override
  public ServiceCall<RegistrationRequest, Contributions> register() {
    return contributor ->
        convertErrors(doRegister(
            contributor.contributorId, contributor.registrationDate,
            contributor.previousYearlyIncome, contributor.incomeType));
  }

  @Override
  public ServiceCall<NotUsed, PSequence<Contributor>> getContributors() {
    return request -> convertErrors(repository.findContributors());
  }

  @Override
  public ServiceCall<Income, Contributions> applyIncome(String contributorId, boolean scaleToEnd, boolean dryRun) {
    return income ->
        convertErrors(doApplyIncome(contributorId, income, scaleToEnd, dryRun));
  }

  private CompletionStage<Contributions>
  doRegister(String contributorId, OffsetDateTime registrationDate, long previousYearlyIncome, IncomeType incomeType) {

    CompletableFuture<IncomeTaxCommand.Register> getCommand = CompletableFuture.supplyAsync(() -> {
      int year = registrationDate.getYear();
      // apply income before registration to the current year (ie, the registration year)
      // scale income to a full year
      Income yearlyIncome = IncomeUtils.yearIncome(previousYearlyIncome, year, incomeType);

      // spread out income over every month
      Map<Month, Income> spreadIncome = IncomeUtils.spreadOutOverMonths(yearlyIncome);

      // compute contribution for each months
      final PMap<Month, PMap<String, Contribution>> calculatedContributions = getContributions(year, spreadIncome);

      return new IncomeTaxCommand.Register(
          contributorId, registrationDate,
          previousYearlyIncome, incomeType, calculatedContributions
      );
    });

    return getCommand.thenCompose(cmd -> entityRef(contributorId).ask(cmd));
  }

  private PersistentEntityRef<IncomeTaxCommand> entityRef(String contributorId) {
    return persistentEntityRegistry.refFor(IncomeTaxEntity.class, contributorId);
  }

  private CompletionStage<Contributions>
  doApplyIncome(String contributorId, Income income, boolean scaleToEnd, boolean dryRun) {

    CompletableFuture<IncomeTaxCommand.ApplyIncome> getCommand = CompletableFuture.supplyAsync(() -> {
      logger.debug("Applying income for #{}: {}", contributorId, income);
      int year = income.start.getYear();

      // scale income or adjust to complete month
      Income normalizedIncomeIncome = normalizeIncome(income, scaleToEnd);
      logger.debug("Normalized income for #{}: {}", contributorId, normalizedIncomeIncome);

      // spread income over months
      PMap<Month, Income> spreadIncome = HashTreePMap.from(IncomeUtils.spreadOutOverMonths(normalizedIncomeIncome));
      logger.debug("Spread out income for #{}: {}", contributorId, spreadIncome);

      // compute contribution for each months
      final PMap<Month, PMap<String, Contribution>> calculatedContributions = getContributions(year, spreadIncome);
      logger.debug("Contributions for #{}: {}", contributorId, calculatedContributions);

      return new IncomeTaxCommand.ApplyIncome(contributorId, normalizedIncomeIncome, scaleToEnd, dryRun, calculatedContributions);
    });

    return getCommand.thenCompose(applyIncome -> entityRef(contributorId).ask(applyIncome));
  }

  private PMap<Month, PMap<String, Contribution>> getContributions(int year, Map<Month, Income> incomes) {
    final Map<Month, CompletableFuture<Map<String, Contribution>>> futures = new ConcurrentHashMap<>();
    incomes.forEach((month, income) -> {
      MonthlyIncomeRequest request =
          new MonthlyIncomeRequest(
              BigDecimal.valueOf(income.income),
              year, month,
              false, Optional.empty());
      CompletableFuture<Map<String, Contribution>> future = calculatorService.compute().invoke(request).toCompletableFuture();
      futures.put(month, future);
    });

    Map<Month, PMap<String, Contribution>> allContributions = futures.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> HashTreePMap.from(e.getValue().join())));
    return HashTreePMap.from(allContributions);

  }

  @Override
  public Topic<CalculationEvent> calculationEvents() {
    // We want to publish all the shards of the hello event
    return TopicProducer.taggedStreamWithOffset(IncomeTaxEvent.TAG.allTags(), (tag, offset) ->

        // Load the event stream for the passed in shard tag
        persistentEntityRegistry.eventStream(tag, offset).map(eventAndOffset -> {

          // Now we want to convert from the persisted event to the published event.
          // Although these two events are currently identical, in future they may
          // change and need to evolve separately, by separating them now we save
          // a lot of potential trouble in future.
          CalculationEvent eventToPublish;

          if (eventAndOffset.first() instanceof IncomeTaxEvent.Registered) {
            IncomeTaxEvent.Registered registered = (IncomeTaxEvent.Registered) eventAndOffset.first();
            eventToPublish = new CalculationEvent.Registered(
                registered.contributorId, registered.registrationDate,
                registered.previousYearlyIncome.income, registered.previousYearlyIncome.incomeType);
          } else if (eventAndOffset.first() instanceof IncomeTaxEvent.IncomeApplied) {
            IncomeTaxEvent.IncomeApplied incomeApplied = (IncomeTaxEvent.IncomeApplied) eventAndOffset.first();
            eventToPublish =
                new CalculationEvent.IncomeApplied(
                    incomeApplied.contributorId, incomeApplied.income);
          } else {
            throw new IllegalArgumentException("Unknown event: " + eventAndOffset.first());
          }

          // We return a pair of the translated event, and its offset, so that
          // Lagom can track which offsets have been published.
          return Pair.create(eventToPublish, eventAndOffset.second());
        })
    );
  }

  private Income normalizeIncome(Income income, boolean scaleToEnd) {

    if (income.start.isAfter(income.end)) {
      throw new IncomeTaxException(Messages.E_ILLEGAL_PERIOD.get(income.start, income.end));
    }
    if (income.start.getYear() != income.end.getYear()) {
      throw new IncomeTaxException(Messages.E_ILLEGAL_PERIOD.get(income.start, income.end));
    }
    // scale income or adjust to complete month
    Income scaledIncome = scaleToEnd ? IncomeUtils.scaleToEndOfYear(income) : IncomeUtils.toCompleteMonths(income);

    return scaledIncome;
  }

  private <T> CompletionStage<T> convertErrors(CompletionStage<T> future) {
    return future.exceptionally(ex -> {
      if (ex instanceof IncomeTaxException) {
        throw new BadRequest(ex.getMessage());
      } else if ((ex instanceof CompletionException) && (ex.getCause() != null) && (ex.getCause() instanceof IncomeTaxException)) {
        throw new BadRequest(ex.getCause().getMessage());
      } else {
        throw new TransportException(TransportErrorCode.InternalServerError, "Unexpected error", ex);
      }
    });
  }
}
