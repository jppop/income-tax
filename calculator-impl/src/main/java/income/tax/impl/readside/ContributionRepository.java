package income.tax.impl.readside;

import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import income.tax.api.Contributor;
import income.tax.impl.domain.IncomeTaxEvent;
import org.pcollections.PSequence;

import java.util.concurrent.CompletionStage;

public interface ContributionRepository {

  ReadSideProcessor.ReadSideHandler<IncomeTaxEvent> buildHandler();

  CompletionStage<PSequence<Contributor>> findContributors();
}
