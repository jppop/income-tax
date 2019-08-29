package income.tax.impl.domain;

import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import income.tax.api.Contributions;
import income.tax.api.Income;
import income.tax.contribution.api.Contribution;
import income.tax.impl.IncomeTaxException;
import income.tax.impl.domain.IncomeTaxCommand.ApplyIncome;
import income.tax.impl.domain.IncomeTaxCommand.Register;
import income.tax.impl.domain.IncomeTaxEvent.Registered;
import income.tax.impl.message.Messages;
import income.tax.impl.tools.IncomeUtils;
import lombok.extern.slf4j.Slf4j;
import org.pcollections.PMap;

import java.math.BigDecimal;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is an event sourced entity. It has a state, {@link IncomeTaxState}, which
 * stores what the greeting should be (eg, "Hello").
 * <p>
 * Event sourced entities are interacted with by sending them commands. This
 * entity supports two commands, a {@link Register} command, which is
 * used to change the greeting, and a {@link ApplyIncome} command, which is a read
 * only command which returns a greeting to the name specified by the command.
 * <p>
 * Commands get translated to events, and it's the events that get persisted by
 * the entity. Each event will have an event handler registered for it, and an
 * event handler simply applies an event to the current state. This will be done
 * when the event is first created, and it will also be done when the entity is
 * loaded from the database - each event will be replayed to recreate the state
 * of the entity.
 * <p>
 * This entity defines one event, the {@link Registered} event,
 * which is emitted when a {@link Register} command is received.
 */
@Slf4j
public class IncomeTaxEntity extends PersistentEntity<IncomeTaxCommand, IncomeTaxEvent, IncomeTaxState> {

  /**
   * An entity can define different behaviours for different states, but it will
   * always start with an initial behaviour. This entity only has one behaviour.
   */
  @Override
  public Behavior initialBehavior(Optional<IncomeTaxState> snapshotState) {

    /*
     * Behaviour is defined using a behaviour builder. The behaviour builder
     * starts with a state, if this entity supports snapshotting (an
     * optimisation that allows the state itself to be persisted to combine many
     * events into one), then the passed in snapshotState may have a value that
     * can be used.
     *
     * Otherwise, the default state is to use the Hello greeting.
     */
    BehaviorBuilder b = newBehaviorBuilder(snapshotState.orElse(IncomeTaxState.initial));

    /*
     * Command handler for the Register command.
     */
    b.setCommandHandler(Register.class, (cmd, ctx) -> {
      // In response to this command, we want to first persist it as a
      // Registered event
      log.debug("processing command {} for #{}", cmd.getClass().getSimpleName(), cmd.getContributorId());
      if (state().isRegistered) {
        ctx.commandFailed(new IncomeTaxException(Messages.E_ALREADY_REGISTERED.get(state().contributorId)));
        return ctx.done();
      }
      final Income yearlyIncome = IncomeUtils.scaleToFullYear(cmd.previousYearlyIncome);
      return ctx.thenPersistAll(
          () -> ctx.reply(
              contributionsFrom(
                  state().contributorId, state().contributionYear, state().currentIncomes, state().contributions.contributions
              )),
          new IncomeTaxEvent.Registered(entityId(), cmd.registrationDate, cmd.previousYearlyIncome),
          new IncomeTaxEvent.IncomeApplied(entityId(), yearlyIncome, now(), cmd.contributions));
    });

    b.setCommandHandler(IncomeTaxCommand.ApplyIncome.class, (cmd, ctx) -> {
      log.debug("processing command {} for #{}", cmd.getClass().getSimpleName(), cmd.getContributorId());
      if (!state().isRegistered) {
        ctx.commandFailed(new IncomeTaxException(Messages.E_NOT_REGISTERED_YET.get(cmd.contributorId)));
        return ctx.done();
      }
      if (cmd.income.start.getYear() != state().contributionYear) {
        ctx.commandFailed(new IncomeTaxException(
            Messages.E_NOT_CURRENT_CONTRIBUTION_YEAR.get(cmd.income.start, cmd.income.end, state().contributionYear)));
        return ctx.done();
      }
      if (cmd.dryRun) {
        IncomeTaxState newStateNotPersisted = state().modifier()
            .withNewIncome(cmd.income)
            .withNewContributions(cmd.contributions)
            .modify();
        ctx.reply(contributionsFrom(
            newStateNotPersisted.contributorId, newStateNotPersisted.contributionYear, newStateNotPersisted.currentIncomes, newStateNotPersisted.contributions.contributions
        ));
        return ctx.done();
      }
      return ctx.thenPersist(new IncomeTaxEvent.IncomeApplied(entityId(), cmd.income, now(), cmd.contributions),
          // Then once the event is successfully persisted, we respond with calculated contributions.
          evt -> ctx.reply(
              contributionsFrom(
                state().contributorId, state().contributionYear, state().currentIncomes, state().contributions.contributions
          )));
    });
    /*
     * Event handler for the Registered event.
     */
    b.setEventHandler(IncomeTaxEvent.Registered.class,
        // update the contributor id and the registration date
        evt -> {
          log.debug("persisted event {} for #{}", evt.getClass().getSimpleName(), evt.contributorId);
          return IncomeTaxState.of(evt.contributorId, true, evt.registrationDate, evt.previousYearlyIncome);
        });

    /*
     * Event handler for Income application events
     */
    b.setEventHandler(IncomeTaxEvent.IncomeApplied.class,
        evt -> {
          log.debug("persisted event {} for #{}", evt.getClass().getSimpleName(), evt.contributorId);
          return state().modifier()
              .withNewIncome(evt.income)
              .withNewContributions(evt.contributions)
              .modify();
        });

    /*
     * We've defined all our behaviour, so build and return it.
     */
    return b.build();
  }

  private OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

  // FIXME: should be done in the service layer
  private Contributions contributionsFrom(
      String contributorId, int year, PMap<Month, Income> currentIncomes, PMap<Month, PMap<String, Contribution>> yearlyContributions) {

    Map<Month, List<Contribution>> contributions = new HashMap<>();
    for (Map.Entry<Month, PMap<String, Contribution>> entry : yearlyContributions.entrySet()) {
      List<Contribution> monthlyContributions = new ArrayList<>();
      for (Map.Entry<String, Contribution> entry2 : entry.getValue().entrySet()) {
        Contribution monthlyContributionInternal = entry2.getValue();
        monthlyContributions.add(
            new Contribution(
                monthlyContributionInternal.type,
                monthlyContributionInternal.income,
                monthlyContributionInternal.baseIncome,
                monthlyContributionInternal.rate,
                monthlyContributionInternal.contribution)
        );
      }
      contributions.put(entry.getKey(), monthlyContributions);
    }
    // sort by month
    Map<Month, List<Contribution>> result = contributions.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
            (oldValue, newValue) -> oldValue, LinkedHashMap::new));

    // sum all contributions by type
    Map<String, BigDecimal> total = result.values().stream()
        .flatMap(o -> o.stream())
        .collect(Collectors.groupingBy(
            Contribution::getType,
            LinkedHashMap::new,
            Collectors.reducing(BigDecimal.ZERO, Contribution::getContribution, (sum, c) -> sum.add(c))));

    long yearlyIncome =
        currentIncomes.values().stream()
            .map(income -> income.income)
            .reduce(0L, (sum, income) -> sum + income);

    return new Contributions(contributorId, year, yearlyIncome, total, result);
  }

}
