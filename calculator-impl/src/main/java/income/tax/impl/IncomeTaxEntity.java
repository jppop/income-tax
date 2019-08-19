package income.tax.impl;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.PersistentEntity;
import income.tax.impl.IncomeTaxEvent.Registered;
import income.tax.impl.IncomeTaxCommand.IncomeTax;
import income.tax.impl.IncomeTaxCommand.Register;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * This is an event sourced entity. It has a state, {@link IncomeTaxState}, which
 * stores what the greeting should be (eg, "Hello").
 * <p>
 * Event sourced entities are interacted with by sending them commands. This
 * entity supports two commands, a {@link Register} command, which is
 * used to change the greeting, and a {@link IncomeTax} command, which is a read
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
    b.setCommandHandler(Register.class, (cmd, ctx) ->
        // In response to this command, we want to first persist it as a
        // Registered event
        ctx.thenPersist(new IncomeTaxEvent.Registered(entityId(), cmd.registrationDate),
            // Then once the event is successfully persisted, we respond with done.
            evt -> ctx.reply(Done.getInstance())));

    b.setCommandHandler(IncomeTaxCommand.ApplyIncome.class, (cmd, ctx) -> {
      switch (cmd.income.incomeType) {
        case PreviousYearlyIncome:
          if (!cmd.income.year.isPresent()) {
            ctx.commandFailed(new IllegalArgumentException("Year is mandatory"));
            return ctx.done();
          }
        case MonthlyIncome:
          if (!cmd.income.month.isPresent()) {
            ctx.commandFailed(new IllegalArgumentException("Month is mandatory"));
            return ctx.done();
          }
          ctx.thenPersist(new IncomeTaxEvent.IncomeApplied(entityId(), cmd.income, now()),
              // Then once the event is successfully persisted, we respond with done.
              evt -> ctx.reply(Done.getInstance())));

      }
    });
    /*
     * Event handler for the Registered event.
     */
    b.setEventHandler(IncomeTaxEvent.Registered.class,
        // Update the current state with the contributor id and the registered date
        evt -> new IncomeTaxState(evt.contributorId, evt.registrationDate));

    b.setEventHandler(IncomeTaxEvent.IncomeApplied.class,
        evt -> state().applyPreviousIncome(evt.income));
    /*
     * We've defined all our behaviour, so build and return it.
     */
    return b.build();
  }

  private OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

}
