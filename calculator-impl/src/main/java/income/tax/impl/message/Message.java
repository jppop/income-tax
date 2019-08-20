package income.tax.impl.message;

import java.util.ResourceBundle;

public enum Message {

  E_OOPS_ERROR,
  E_ILLEGAL_PERIOD,
  E_NOT_SINGLE_YEAR_PERIOD;

  private static final String COMPONENT_CODE = "TAX"; //

  private static final ResourceBundle resource = ResourceBundle.getBundle(Message.class.getName());

  public String get(Object... args) {
    return Helper.getMessage(resource, COMPONENT_CODE, this, args);
  }
}
