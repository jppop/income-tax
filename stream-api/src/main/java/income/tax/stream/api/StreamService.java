package income.tax.stream.api;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.lightbend.lagom.javadsl.api.Descriptor;
import com.lightbend.lagom.javadsl.api.Service;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import income.tax.api.Contributions;
import income.tax.api.RegistrationRequest;

import static com.lightbend.lagom.javadsl.api.Service.named;
import static com.lightbend.lagom.javadsl.api.Service.namedCall;

/**
 * The stream interface.
 * <p>
 * This describes everything that Lagom needs to know about how to serve and
 * consume the HelloStream service.
 */
public interface StreamService extends Service {

  /**
   * This stream is implemented by asking the hello service directly to say
   * hello to each passed in name. It requires the hello service to be up
   * and running to function.
   * @return
   */
  ServiceCall<Source<RegistrationRequest, NotUsed>, Source<Contributions, NotUsed>> directStream();

  /**
   * This stream is implemented autonomously, it uses its own store, populated
   * by subscribing to the events published by the hello service, to say hello
   * to each passed in name. It can function even when the hello service is
   * down.
   */
  ServiceCall<Source<String, NotUsed>, Source<RegistrationRequest, NotUsed>> autonomousStream();

  @Override
  default Descriptor descriptor() {
    return named("stream")
            .withCalls(
              namedCall("direct-stream", this::directStream),
              namedCall("auto-stream", this::autonomousStream)
            ).withAutoAcl(true);
  }
}
