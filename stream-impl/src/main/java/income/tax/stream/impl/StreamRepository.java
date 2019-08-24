package income.tax.stream.impl;

import akka.Done;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import income.tax.api.IncomeType;
import income.tax.api.RegistrationRequest;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class StreamRepository {
  private final CassandraSession uninitialisedSession;

  // Will return the session when the Cassandra tables have been successfully created
  private volatile CompletableFuture<CassandraSession> initialisedSession;

  @Inject
  public StreamRepository(CassandraSession uninitialisedSession) {
    this.uninitialisedSession = uninitialisedSession;
    // Eagerly create the session
    session();
  }

  private CompletionStage<CassandraSession> session() {
    // If there's no initialised session, or if the initialised session future completed
    // with an exception, then reinitialise the session and attempt to create the tables
    if (initialisedSession == null || initialisedSession.isCompletedExceptionally()) {
      initialisedSession = uninitialisedSession.executeCreateTable(
          "CREATE TABLE IF NOT EXISTS contributor" +
              " (id text PRIMARY KEY, registrationDate timestamp, previousIncome bigint, previousIncomeType text)"
      ).thenApply(done -> uninitialisedSession).toCompletableFuture();
    }
    return initialisedSession;
  }

  public CompletionStage<Done> registerContributor(
      String contributorId, OffsetDateTime registrationDate, long previousIncome, IncomeType incomeType) {
    return session().thenCompose(session -> {
      Date timestamp = Date.from(registrationDate.toInstant());
      return session.executeWrite(
          "INSERT INTO contributor (id, registrationDate, previousIncome, previousIncomeType)" +
              " VALUES (?, ?, ?, ?)",
          contributorId, timestamp, previousIncome, incomeType.name());
    });
  }

  public CompletionStage<Optional<RegistrationRequest>> getContributor(String id) {
    return session().thenCompose(session ->
        session.selectOne(
            "SELECT registrationDate, previousIncome, previousIncomeType" +
                " FROM contributor WHERE id = ?", id)
    ).thenApply(maybeRow -> maybeRow.map(
        row -> {
          Date timestamp = row.getTimestamp("registrationDate");
          OffsetDateTime registrationDate = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
          long previousIncome = row.getLong("previousIncome");
          IncomeType incomeType = IncomeType.valueOf(row.getString("previousIncomeType"));
          return new RegistrationRequest(id, registrationDate, previousIncome, incomeType);
        }));
  }
}
