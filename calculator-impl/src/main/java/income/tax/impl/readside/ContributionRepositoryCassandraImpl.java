package income.tax.impl.readside;

import akka.Done;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import income.tax.api.Contributor;
import income.tax.impl.domain.IncomeTaxEvent;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide.completedStatements;

@Singleton
public class ContributionRepositoryCassandraImpl implements ContributionRepository {

  private static final Logger logger = LoggerFactory.getLogger(ContributionRepositoryCassandraImpl.class);

  private static final UnaryOperator<String> regionFromContributorId = id -> id.length() > 3 ? id.substring(0, 3) : "???";
  private final CassandraSession session;
  private final CassandraReadSide readSide;
  private PreparedStatement writeContributor; // initialized in prepareStatement

  @Inject
  public ContributionRepositoryCassandraImpl(CassandraSession session, CassandraReadSide readSide) {
    logger.info("Cassandra Read Side implementation");
    this.session = session;
    this.readSide = readSide;
  }

  @Override
  public ReadSideProcessor.ReadSideHandler<IncomeTaxEvent> buildHandler() {
    // use the build-in Cassandra builder
    CassandraReadSide.ReadSideHandlerBuilder<IncomeTaxEvent> builder =
        readSide.builder("contributionsoffset");
    builder.setGlobalPrepare(this::ensureTables);
    builder.setPrepare(tag -> prepareStatements());
    builder.setEventHandler(IncomeTaxEvent.Registered.class, this::processRegistered);
    return builder.build();
  }

  @Override
  public CompletionStage<PSequence<Contributor>> findContributors() {
    return session.selectAll("SELECT id, registrationDate, yearlyIncome, yearlyContribution FROM contributors")
        .thenApply(rows -> {
              List<Contributor> contributors = rows.stream()
                  .map(row -> {
                    String id = row.getString("id");
                    Date timestamp = row.getTimestamp("registrationDate");
                    OffsetDateTime registrationDate = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
                    long yearlyIncome = row.getLong("yearlyIncome");
                    long yearlyContribution = row.getLong("yearlyContribution");
                    return new Contributor(id, registrationDate, yearlyIncome, yearlyContribution);
                  })
                  .collect(Collectors.toList());
              return TreePVector.from(contributors);
            }
        );
  }

  private CompletionStage<List<BoundStatement>> processRegistered(IncomeTaxEvent.Registered event) {
    logger.debug("registering a new contributor: {}", event);

    BoundStatement bindWriteContributor = writeContributor.bind();
    bindWriteContributor.setString("id", event.getContributorId());
    bindWriteContributor.setString("region", regionFromContributorId.apply(event.getContributorId()));
    Date registrationTimestamp = Date.from(event.registrationDate.toInstant());
    bindWriteContributor.setTimestamp("registrationDate", registrationTimestamp);
    bindWriteContributor.setLong("yearlyIncome", 0L);
    bindWriteContributor.setLong("yearlyContribution", 0L);
    bindWriteContributor.unset("yearlyIncome");
    bindWriteContributor.unset("yearlyContribution");
    return completedStatements(Arrays.asList(bindWriteContributor));
  }

  private CompletionStage<Done> ensureTables() {
    return session.executeCreateTable(
        "CREATE TABLE IF NOT EXISTS contributors (" +
            " id TEXT," +
            " region TEXT," +
            " registrationDate TIMESTAMP," +
            " yearlyIncome BIGINT," +
            " yearlyContribution BIGINT," +
            " PRIMARY KEY (region, id)" +
            ")");
  }

  private CompletionStage<Done> prepareStatements() {
    return session.prepare(
        "INSERT INTO contributors (id, region, registrationDate, yearlyIncome, yearlyContribution)" +
            " VALUES (?, ?, ?, ?, ?)")
        .thenApply(ps -> {
          this.writeContributor = ps;
          return Done.getInstance();
        });
  }
}
