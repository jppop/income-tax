package income.tax.impl.readside;

import akka.Done;
import com.datastax.driver.core.*;
import com.datastax.driver.core.schemabuilder.CreateType;
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
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.datastax.driver.core.schemabuilder.SchemaBuilder.*;
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
    return session.selectAll("SELECT id, registration_date, yearly_income, yearly_contribution FROM contributors")
        .thenApply(rows -> {
              List<Contributor> contributors = rows.stream()
                  .map(row -> {
                    String id = row.getString("id");
                    Date timestamp = row.getTimestamp("registration_date");
                    OffsetDateTime registrationDate = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
                    BigDecimal yearlyIncome =
                        row.isNull("yearly_income") ? BigDecimal.ZERO : row.getDecimal("yearly_income");
                    BigDecimal yearlyContribution =
                        row.isNull("yearly_contribution") ? BigDecimal.ZERO : row.getDecimal("yearly_contribution");
                    return new Contributor(id, registrationDate, yearlyIncome.longValue(), yearlyContribution.longValue());
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
    bindWriteContributor.setTimestamp("registration_date", registrationTimestamp);
    bindWriteContributor.setDecimal("yearly_income", BigDecimal.ZERO);
    bindWriteContributor.setDecimal("yearly_contribution", BigDecimal.ZERO);
    bindWriteContributor.unset("yearly_income");
    bindWriteContributor.unset("yearly_contribution");
    return completedStatements(Arrays.asList(bindWriteContributor));
  }

  private CompletionStage<Done> ensureTables() {

    final CreateType createTypeStmt = createType("contribution")
        .addColumn("type", DataType.text())
        .addColumn("base_income", DataType.decimal())
        .addColumn("rate", DataType.decimal())
        .addColumn("contribution", DataType.decimal())
        .ifNotExists();

    final Statement createContributors = createTable("contributors")
        .addPartitionKey("id", DataType.text())
        .addPartitionKey("region", DataType.text())
        .addColumn("registration_date", DataType.timestamp())
        .addColumn("yearly_income", DataType.decimal())
        .addColumn("yearly_contribution", DataType.decimal())
        .ifNotExists();

    final Statement createContributions = createTable("contributions")
        .addPartitionKey("contributor_id", DataType.text())
        .addClusteringColumn("year", DataType.cint())
        .addClusteringColumn("month", DataType.cint())
        .addColumn("income", DataType.decimal())
        .addUDTListColumn("contributions", frozen("contribution"))
        .ifNotExists()
        .withOptions()
        .clusteringOrder("year", Direction.DESC)
        .clusteringOrder("month", Direction.DESC);

    final BatchStatement batch = new BatchStatement();
    batch.add(createTypeStmt);
    batch.add(createContributors);
    batch.add(createContributions);

    return session.underlying().thenApply(underlyingSession -> {
      logger.info("Creating schema..");
      logger.info("Schema creation statements: {}", batch.getStatements());
      batch.getStatements().stream().forEach(statement -> underlyingSession.execute(statement));
      return Done.getInstance();
    });
  }

  private CompletionStage<Done> prepareStatements() {
    return session.prepare(
        "INSERT INTO contributors (id, region, registration_date, yearly_income, yearly_contribution)" +
            " VALUES (?, ?, ?, ?, ?)")
        .thenApply(ps -> {
          this.writeContributor = ps;
          return Done.getInstance();
        });
  }
}
