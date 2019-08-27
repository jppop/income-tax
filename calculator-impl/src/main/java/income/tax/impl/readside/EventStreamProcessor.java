package income.tax.impl.readside;

import com.lightbend.lagom.javadsl.persistence.AggregateEventTag;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import income.tax.impl.domain.IncomeTaxEvent;
import org.pcollections.PSequence;

import javax.inject.Inject;

public class EventStreamProcessor extends ReadSideProcessor<IncomeTaxEvent> {

  private final ContributionRepository repository;

  @Inject
  public EventStreamProcessor(ContributionRepository repository) {
    this.repository = repository;
  }

  @Override
  public ReadSideHandler<IncomeTaxEvent> buildHandler() {
    return repository.buildHandler();
  }

  @Override
  public PSequence<AggregateEventTag<IncomeTaxEvent>> aggregateTags() {
    return IncomeTaxEvent.TAG.allTags();
  }
}
