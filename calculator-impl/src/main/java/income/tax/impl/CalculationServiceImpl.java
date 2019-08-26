package income.tax.impl;

import akka.NotUsed;
import akka.japi.Pair;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import com.lightbend.lagom.javadsl.persistence.ReadSide;
import income.tax.api.*;
import income.tax.impl.domain.IncomeTaxCommand;
import income.tax.impl.domain.IncomeTaxEntity;
import income.tax.impl.domain.IncomeTaxEvent;
import income.tax.impl.readside.ContributionRepository;
import income.tax.impl.readside.EventStreamProcessor;
import org.pcollections.PSequence;

import javax.inject.Inject;

/**
 * Implementation of the HelloService.
 */
public class CalculationServiceImpl implements CalculationService {

  private final PersistentEntityRegistry persistentEntityRegistry;
  private final ContributionRepository repository;

  @Inject
  public CalculationServiceImpl(PersistentEntityRegistry persistentEntityRegistry, ReadSide readSide, ContributionRepository repository) {
    this.persistentEntityRegistry = persistentEntityRegistry;
    this.repository = repository;

    persistentEntityRegistry.register(IncomeTaxEntity.class);
    readSide.register(EventStreamProcessor.class);
  }

  @Override
  public ServiceCall<RegistrationRequest, Contributions> register() {
    return contributor -> {
      // Look up the IncomeTax entity for the given ID.
      PersistentEntityRef<IncomeTaxCommand> ref =
          persistentEntityRegistry.refFor(IncomeTaxEntity.class, contributor.contributorId);
      // Tell the entity to use the greeting message specified.
      return ref.ask(new IncomeTaxCommand.Register(
          contributor.contributorId, contributor.registrationDate,
          contributor.previousYearlyIncome, contributor.incomeType));
    };

  }

  @Override
  public ServiceCall<NotUsed, PSequence<Contributor>> getContributors() {
    return request -> repository.findContributors();
  }

  @Override
  public ServiceCall<Income, Contributions> applyIncome(String contributorId, boolean scaleToEnd, boolean dryRun) {
    return income -> {
      // Look up the IncomeTax entity for the given ID.
      PersistentEntityRef<IncomeTaxCommand> ref = persistentEntityRegistry.refFor(IncomeTaxEntity.class, contributorId);
      // Tell the entity to apply the income.
      return ref.ask(new IncomeTaxCommand.ApplyIncome(contributorId, income, scaleToEnd, dryRun));
    };
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
          } else if (eventAndOffset.first() instanceof IncomeTaxEvent.ContributionScheduleStarted) {
            IncomeTaxEvent.ContributionScheduleStarted incomeApplied = (IncomeTaxEvent.ContributionScheduleStarted) eventAndOffset.first();
            eventToPublish =
                new CalculationEvent.IncomeApplied(
                    incomeApplied.contributorId, incomeApplied.previousYearlyIncome);
          } else {
            throw new IllegalArgumentException("Unknown event: " + eventAndOffset.first());
          }

          // We return a pair of the translated event, and its offset, so that
          // Lagom can track which offsets have been published.
          return Pair.create(eventToPublish, eventAndOffset.second());
        })
    );
  }
}
