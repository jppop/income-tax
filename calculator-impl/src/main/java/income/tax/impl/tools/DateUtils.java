package income.tax.impl.tools;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.function.UnaryOperator;

public class DateUtils {

  public static UnaryOperator<OffsetDateTime> minFirstDayOfMonth = dateTime ->
      OffsetDateTime.of(dateTime.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate(),
          LocalTime.MIN,
          dateTime.getOffset());

  public static UnaryOperator<OffsetDateTime> maxLastDayOfMonth = dateTime ->
      OffsetDateTime.of(dateTime.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate(),
          LocalTime.MAX,
          dateTime.getOffset());

  public static UnaryOperator<OffsetDateTime> justBefore = (dateTime ->
      OffsetDateTime.of(dateTime.minusDays(1).toLocalDate(), LocalTime.MAX, dateTime.getOffset()));

}
