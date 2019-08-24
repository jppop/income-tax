package income.tax.impl.message;

import java.util.ResourceBundle;

public enum Messages {

  E_OOPS_ERROR,
  E_ILLEGAL_PERIOD,
  E_NOT_SINGLE_YEAR_PERIOD,
  E_NOT_CURRENT_CONTRIBUTION_YEAR,
  E_ALREADY_REGISTERED;

  private static final String COMPONENT_CODE = "TAX"; //

  private static final ResourceBundle resource = ResourceBundle.getBundle(Messages.class.getName());

  public String get(Object... args) {
    return Helper.getMessage(resource, COMPONENT_CODE, this, args);
  }
}
