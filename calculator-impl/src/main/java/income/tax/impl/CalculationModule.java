package income.tax.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import income.tax.api.CalculationService;

/**
 * The module that binds the HelloService so that it can be served.
 */
public class CalculationModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    bindService(CalculationService.class, CalculationServiceImpl.class);
  }
}
