package income.tax.contribution.impl;

import com.lightbend.lagom.javadsl.api.ServiceCall;
import com.lightbend.lagom.javadsl.api.transport.TransportErrorCode;
import com.lightbend.lagom.javadsl.api.transport.TransportException;
import income.tax.contribution.api.CalculatorService;
import income.tax.contribution.api.Contribution;
import income.tax.contribution.api.MonthlyIncomeRequest;
import org.pcollections.HashTreePMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CalculatorServiceImpl implements CalculatorService {

  private static Logger logger = LoggerFactory.getLogger(CalculatorServiceImpl.class);

  private final Map<Integer, Calculator> calculators;

  @Inject
  public CalculatorServiceImpl(Map<Integer, Calculator> calculators) {
    this.calculators = calculators;
  }

  @Override
  public ServiceCall<MonthlyIncomeRequest, Map<String, Contribution>> compute() {
    return monthlyIncomeRequest ->
        convertErrors(
            CompletableFuture.supplyAsync(() -> doCompute(monthlyIncomeRequest))
                .thenApply(contributions -> convertInternalContribution(contributions))
        );
  }

  private Map<String, ContributionInternal> doCompute(MonthlyIncomeRequest monthlyIncomeRequest) {
    // get the calculator
    logger.debug("Configured calculators: {}", calculators);
    Calculator calculator = calculators.get(monthlyIncomeRequest.year);
    if (calculator == null) {
      Optional<Calculator> maybeCalculator = calculators.values().stream().findFirst();
      calculator = maybeCalculator.orElseThrow(() -> new IllegalStateException("Something went really bad. No calculator defined"));
    }
    Map<String, ContributionInternal> monthContributions =
        calculator.computeFromMonthlyIncome(
            monthlyIncomeRequest.month,
            monthlyIncomeRequest.income,
            monthlyIncomeRequest.round,
            monthlyIncomeRequest.additionalArgs);
    return monthContributions;
  }

  private Map<String, Contribution> convertInternalContribution(Map<String, ContributionInternal> contributionsInternal) {
    Map<String, Contribution> contributions = new HashMap<>();
    for (Map.Entry<String, ContributionInternal> entry : contributionsInternal.entrySet()) {
      ContributionInternal contributionInternal = entry.getValue();
      contributions.put(
          entry.getKey(),
          new Contribution(
              contributionInternal.type,
              contributionInternal.income,
              contributionInternal.baseIncome,
              contributionInternal.rate,
              contributionInternal.contribution));
    }
    return HashTreePMap.from(contributions);
  }

  private <T> CompletionStage<T> convertErrors(CompletionStage<T> future) {
    return future.exceptionally(ex -> {
      throw new TransportException(TransportErrorCode.InternalServerError, "Unexpected error", ex);
    });
  }
}
