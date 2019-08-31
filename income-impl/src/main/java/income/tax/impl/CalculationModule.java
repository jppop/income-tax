package income.tax.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import income.tax.api.CalculationService;
import income.tax.contribution.api.CalculatorService;
import income.tax.impl.readside.ContributionRepository;
import income.tax.impl.readside.ContributionRepositoryCassandraImpl;

/**
 * The module that binds the HelloService so that it can be served.
 */
public class CalculationModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    bindService(CalculationService.class, CalculationServiceImpl.class);
    bind(ContributionRepository.class).to(ContributionRepositoryCassandraImpl.class);
    bindClient(CalculatorService.class);
  }
}
