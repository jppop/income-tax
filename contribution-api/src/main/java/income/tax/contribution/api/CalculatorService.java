package income.tax.contribution.api;

import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;

import java.util.Map;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.pathCall;

public interface CalculatorService extends Service {

  ServiceCall<MonthlyIncomeRequest, Map<String, Contribution>> compute();

  @Override
  default Descriptor descriptor() {
    return named("contribution")
        .withCalls(
            pathCall("/api/contributions", this::compute)
        )
        .withAutoAcl(true);
  }
}
