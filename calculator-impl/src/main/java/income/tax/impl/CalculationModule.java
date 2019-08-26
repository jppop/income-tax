package income.tax.impl;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import income.tax.api.CalculationService;
import income.tax.calculator.Calculator;
import income.tax.impl.readside.ContributionRepository;
import income.tax.impl.readside.ContributionRepositoryCassandraImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The module that binds the HelloService so that it can be served.
 */
public class CalculationModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    bindService(CalculationService.class, CalculationServiceImpl.class);
    bind(ContributionRepository.class).to(ContributionRepositoryCassandraImpl.class);

    // get Calculator implementations
    Map<Integer, Calculator> implementations = loadCalculators();

    // bind implementation by year
    MapBinder<Integer, Calculator> calculatorBindings = MapBinder.newMapBinder(binder(), Integer.class, Calculator.class);
    implementations.forEach((year, calculator) ->
        calculatorBindings.addBinding(year).toInstance(calculator));
  }

  public static Map<Integer, Calculator> loadCalculators() {
    final Map<Integer, Calculator> implementations = new HashMap<>();
    ServiceLoader<Calculator> serviceLoader = ServiceLoader.load(Calculator.class);

    for (Iterator<Calculator> it = serviceLoader.iterator(); it.hasNext(); ) {
      Calculator impl = it.next();
      implementations.put(impl.getYear(), impl);
    }

    // sort in the reverse order (the most recent will be the default implementation)
    Map result = implementations.entrySet().stream()
        .sorted((o1, o2) -> o2.getKey().compareTo(o1.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
            (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    return result;
  }
}
