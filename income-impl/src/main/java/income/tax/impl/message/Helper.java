package income.tax.impl.message;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Helper {

    public static String getMessage(ResourceBundle resource,
                                    String componentCode,
                                    @SuppressWarnings("rawtypes") Enum messageCode,
                                    Object... args) {
        StringBuffer buffer = new StringBuffer();
        int code;
        if (CodeSupplier.class.isAssignableFrom(messageCode.getClass())) {
            code = ((CodeSupplier)messageCode).code();
        } else {
            code = messageCode.ordinal();
        }
        buffer.append("[").append(componentCode).append(String.format("%04d", code)).append("] ")
            .append(messageCode.toString()).append(" - ");
        try {
            final String format = resource.getString(messageCode.toString());
            final String msg = MessageFormat.format(format, args);
            buffer.append(msg);
        } catch (MissingResourceException e) {
            buffer.append("!! message not found in the resource bundle !!");
        }
        return buffer.toString();
    }
}
