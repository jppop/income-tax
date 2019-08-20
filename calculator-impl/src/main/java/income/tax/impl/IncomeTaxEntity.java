package income.tax.impl;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import income.tax.api.Income;
import income.tax.impl.calculation.IncomeAdjuster;
import income.tax.impl.calculation.IncomeAdjusters;
import income.tax.impl.IncomeTaxCommand.ApplyIncome;
import income.tax.impl.IncomeTaxCommand.Register;
import income.tax.impl.IncomeTaxEvent.Registered;
import income.tax.impl.message.Message;
import income.tax.impl.tools.DateUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

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
    BehaviorBuilder b = newBehaviorBuilder(snapshotState.orElse(IncomeTaxState.empty));

    /*
     * Command handler for the Register command.
     */
    b.setCommandHandler(Register.class, (cmd, ctx) -> {
      // In response to this command, we want to first persist it as a
      // Registered event
      log.debug("processing command {}", cmd);
      return ctx.thenPersist(new IncomeTaxEvent.Registered(entityId(), cmd.registrationDate, cmd.previousYearlyIncome),
          // Then once the event is successfully persisted, we respond with done.
          evt -> ctx.reply(Done.getInstance()));
    });

    b.setCommandHandler(IncomeTaxCommand.ApplyIncome.class, (cmd, ctx) -> {
      log.debug("processing command {}", cmd);
      Optional<String> possibleError = checkIncomeCommandArguments(cmd.income);
      if (possibleError.isPresent()) {
        ctx.invalidCommand(possibleError.get());
        return ctx.done();
      }
      IncomeTaxEvent incomeTaxEvent;
      if (isAppliedInTheCurrentYear(cmd.income)) {
        incomeTaxEvent = new IncomeTaxEvent.IncomeApplied(entityId(), cmd.income, now());
      } else {
        incomeTaxEvent = new IncomeTaxEvent.PreviousIncomeApplied(entityId(), cmd.income, now());
      }
      return ctx.thenPersist(incomeTaxEvent,
          // Then once the event is successfully persisted, we respond with done.
          evt -> ctx.reply(Done.getInstance()));
    });
    /*
     * Event handler for the Registered event.
     */
    b.setEventHandler(IncomeTaxEvent.Registered.class,
        // Update the current state with the contributor id and the registered date
        evt -> new IncomeTaxState(evt.contributorId, evt.registrationDate, evt.previousYearlyIncome));

    /*
    * Event handler for Income application events
     */
    b.setEventHandler(IncomeTaxEvent.IncomeApplied.class,
        evt -> state().with(incomeAdjuster(evt.income)));
    b.setEventHandler(IncomeTaxEvent.PreviousIncomeApplied.class,
        evt -> state().with(yearlyIncomeAdjuster(evt.income)));
    /*
     * We've defined all our behaviour, so build and return it.
     */
    return b.build();
  }

  private Optional<String> checkIncomeCommandArguments(Income income) {

    // adjust start to the 1st of month
    OffsetDateTime start = DateUtils.minFirstDayOfMonth.apply(income.start);
    // adjust end to the last day of month
    OffsetDateTime end = DateUtils.maxLastDayOfMonth.apply(income.end);

    if (start.isAfter(end)) {
      return Optional.of(Message.E_ILLEGAL_PERIOD.get());
    }
    if (start.getYear() != end.getYear()) {
      return Optional.of(Message.E_NOT_SINGLE_YEAR_PERIOD.get());
    }
    return Optional.empty();
  }

  private boolean isAppliedInTheCurrentYear(Income income) {
    return income.start.getYear() == state().contributionYear;
  }

  private IncomeAdjuster incomeAdjuster(Income income) {
    return IncomeAdjusters.currentYear(income);
  }

  private IncomeAdjuster yearlyIncomeAdjuster(Income income) {
    return IncomeAdjusters.previousYear(income);
  }

  private OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

}
