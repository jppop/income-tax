package income.tax.impl;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import income.tax.api.CalculationService;
import income.tax.calculator.Calculator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * The module that binds the HelloService so that it can be served.
 */
public class CalculationModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    bindService(CalculationService.class, CalculationServiceImpl.class);

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
    implementations.keySet().stream().sorted((o1, o2) -> o2.compareTo(o1));

    return implementations;
  }
}
