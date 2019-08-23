package income.tax.impl.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import income.tax.calculator.Contribution;
import lombok.NonNull;
import lombok.Value;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;

import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Value
@JsonDeserialize
public class ContributionState {

  public final @NonNull
  PMap<Month, PMap<String, Contribution>> contributions;

  @JsonCreator
  public ContributionState(PMap<Month, PMap<String, Contribution>> contributions) {
    this.contributions = Preconditions.checkNotNull(contributions);
  }

  public static ContributionState empty() {
    Map<Month, PMap<String, Contribution>> contributions = new HashMap<>();
    Stream.of(Month.values())
        .forEach(month -> contributions.put(month, HashTreePMap.empty()));
    return new ContributionState(HashTreePMap.from(contributions));
  }

  public ContributionState update(Map<Month, PMap<String, Contribution>> newContributions) {
    return new ContributionState(this.contributions.plusAll(newContributions));
  }
}
