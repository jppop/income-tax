package income.tax.impl.tools;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class DateUtils {

  public static UnaryOperator<OffsetDateTime> minFirstDayOfMonth = dateTime ->
      OffsetDateTime.of(dateTime.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate(),
          LocalTime.MIN,
          dateTime.getOffset());

  public static Function<LocalDate, OffsetDateTime> minFirstDayOfMonthFromDate = localDate ->
      OffsetDateTime.of(localDate.with(TemporalAdjusters.firstDayOfMonth()),
          LocalTime.MIN,
          ZoneOffset.UTC);

  public static UnaryOperator<OffsetDateTime> minFirstDayOfYear = dateTime ->
      OffsetDateTime.of(dateTime.with(TemporalAdjusters.firstDayOfYear()).toLocalDate(),
          LocalTime.MIN,
          dateTime.getOffset());

  public static Function<LocalDate, OffsetDateTime> maxLastDayOfMonthFromDate = localDate ->
      OffsetDateTime.of(localDate.with(TemporalAdjusters.lastDayOfMonth()),
          LocalTime.MAX,
          ZoneOffset.UTC);

  public static UnaryOperator<OffsetDateTime> maxLastDayOfMonth = dateTime ->
      OffsetDateTime.of(dateTime.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate(),
          LocalTime.MAX,
          dateTime.getOffset());

  public static UnaryOperator<OffsetDateTime> maxLastDayOfYear = dateTime ->
      OffsetDateTime.of(dateTime.with(TemporalAdjusters.lastDayOfYear()).toLocalDate(),
          LocalTime.MAX,
          dateTime.getOffset());

  public static UnaryOperator<OffsetDateTime> justBefore = (dateTime ->
      OffsetDateTime.of(dateTime.minusDays(1).toLocalDate(), LocalTime.MAX, dateTime.getOffset()));

}
