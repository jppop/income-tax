package income.tax.impl;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lightbend.lagom.serialization.Jsonable;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize
public class IncomeTaxException extends RuntimeException implements Jsonable {

  public final String message;

  public IncomeTaxException(String message) {
    super(message);
    this.message = message;
  }
}
