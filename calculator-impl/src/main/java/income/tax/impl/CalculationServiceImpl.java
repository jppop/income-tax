package income.tax.impl;

import akka.Done;
import akka.japi.Pair;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.broker.Topic;
import com.lightbend.lagom.javadsl.broker.TopicProducer;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRef;
import com.lightbend.lagom.javadsl.persistence.PersistentEntityRegistry;
import income.tax.api.CalculationEvent;
import income.tax.api.CalculationService;
import income.tax.api.Contributor;
import income.tax.api.Income;

import javax.inject.Inject;

/**
 * Implementation of the HelloService.
 */
public class CalculationServiceImpl implements CalculationService {

  private final PersistentEntityRegistry persistentEntityRegistry;

  @Inject
  public CalculationServiceImpl(PersistentEntityRegistry persistentEntityRegistry) {
    this.persistentEntityRegistry = persistentEntityRegistry;
    persistentEntityRegistry.register(IncomeTaxEntity.class);
  }

  @Override
  public ServiceCall<Contributor, Done> register() {
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
  public ServiceCall<Income, Done> applyIncome(String contributorId) {
    return income -> {
      // Look up the IncomeTax entity for the given ID.
      PersistentEntityRef<IncomeTaxCommand> ref = persistentEntityRegistry.refFor(IncomeTaxEntity.class, contributorId);
      // Tell the entity to use the greeting message specified.
      return ref.ask(new IncomeTaxCommand.ApplyIncome(contributorId, income));
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
                registered.getContributorId(), registered.getRegistrationDate(),
                registered.previousYearlyIncome.income, registered.previousYearlyIncome.incomeType);
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
