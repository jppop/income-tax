package income.tax.impl.readside;

import akka.Done;
import akka.japi.Pair;
import com.datastax.driver.core.*;
import com.datastax.driver.core.schemabuilder.CreateType;
import com.lightbend.lagom.javadsl.persistence.ReadSideProcessor;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraReadSide;
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession;
import income.tax.api.Contributor;
import income.tax.contribution.api.Contribution;
import income.tax.impl.domain.IncomeTaxEvent;
import income.tax.impl.message.Messages;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PSequence;
import org.pcollections.TreePVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
  private final String keyspace = "tax_calculation";
  private PreparedStatement writeContributors; // initialized in prepareStatement
  private PreparedStatement writeContributions; // initialized in prepareStatement
  private UserType contributionUdtType;  // initialized in prepareStatement

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
    builder.setEventHandler(IncomeTaxEvent.IncomeApplied.class, this::processIncomeApplied);
    return builder.build();
  }

  @Override
  public CompletionStage<PSequence<Contributor>> findContributors() {
    return session.selectAll("SELECT * FROM contributors")
        .thenApply(rows -> {
              List<Contributor> contributors = rows.stream()
                  .map(row -> {
                    String id = row.getString("id");
                    Date timestamp = row.getTimestamp("registration_date");
                    OffsetDateTime registrationDate = OffsetDateTime.ofInstant(timestamp.toInstant(), ZoneId.of("UTC"));
                    return new Contributor(id, registrationDate);
                  })
                  .collect(Collectors.toList());
              return TreePVector.from(contributors);
            }
        );
  }

  @Override
  public CompletionStage<PMap<Month, PSequence<Contribution>>>
  findContributions(final String contributorId, final int year) {
    return session.selectAll(
        "SELECT * FROM contributions WHERE region = ? and contributor_id = ? and year = ?",
        regionFromContributorId.apply(contributorId), contributorId, year)
        .thenApply(rows -> rows.stream().map(row -> {
           int month = row.getInt("month");
          List<UDTValue> contributionValues = row.getList("contributions", UDTValue.class);
          final List<Contribution> contributions = contributionValues.stream()
              .map(udtValue ->
                  new Contribution(
                      udtValue.getString("type"),
                      udtValue.getDecimal("income"),
                      udtValue.getDecimal("base_income"),
                      udtValue.getDecimal("rate"),
                      udtValue.getDecimal("contribution"))
              ).collect(Collectors.toList());
          return new Pair<>(Month.of(month), TreePVector.from(contributions));
        }).collect(Collectors.toMap(Pair::first, Pair::second, (oldValue, newValue) -> newValue)))
        .thenApply(HashTreePMap::from);
  }

  private CompletionStage<List<BoundStatement>> processRegistered(IncomeTaxEvent.Registered event) {
    logger.debug("registering a new contributor: {}", event);

    BoundStatement bindWriteContributor = writeContributors.bind();
    bindWriteContributor.setString("id", event.getContributorId());
    bindWriteContributor.setString("region", regionFromContributorId.apply(event.getContributorId()));
    Date registrationTimestamp = Date.from(event.registrationDate.toInstant());
    bindWriteContributor.setTimestamp("registration_date", registrationTimestamp);
    return completedStatements(Collections.singletonList(bindWriteContributor));
  }

  private CompletionStage<List<BoundStatement>> processIncomeApplied(final IncomeTaxEvent.IncomeApplied event) {
    logger.debug("record income and contributions: {}", event);
    List<BoundStatement> boundStatements = event.contributions.entrySet().stream()
        .map(entry -> {
          final Month month = entry.getKey();
          PMap<String, Contribution> monthContributions = entry.getValue();
          BoundStatement bindWrite = writeContributions.bind();
          bindWrite.setString("contributor_id", event.getContributorId());
          bindWrite.setString("region", regionFromContributorId.apply(event.getContributorId()));
          bindWrite.setInt("year", event.year);
          bindWrite.setInt("month", month.getValue());

          List<UDTValue> udtContributions = monthContributions.values().stream().map(contribution -> {
            UDTValue udtContribution = this.contributionUdtType.newValue();
            udtContribution
                .setString("type", contribution.type)
                .setDecimal("income", contribution.income)
                .setDecimal("base_income", contribution.baseIncome)
                .setDecimal("rate", contribution.rate)
                .setDecimal("contribution", contribution.contribution);
            return udtContribution;
          }).collect(Collectors.toList());

          bindWrite.setList("contributions", udtContributions);
          return bindWrite;

        }).collect(Collectors.toList());
    return completedStatements(boundStatements);
  }

  private CompletionStage<Done> ensureTables() {

    final CreateType createTypeStmt = createType("contribution")
        .addColumn("type", DataType.text())
        .addColumn("income", DataType.decimal())
        .addColumn("base_income", DataType.decimal())
        .addColumn("rate", DataType.decimal())
        .addColumn("contribution", DataType.decimal())
        .ifNotExists();

    final Statement createContributors = createTable("contributors")
        .addPartitionKey("region", DataType.text())
        .addPartitionKey("id", DataType.text())
        .addColumn("registration_date", DataType.timestamp())
        .ifNotExists();

    final Statement createContributions = createTable("contributions")
        .addPartitionKey("region", DataType.text())
        .addPartitionKey("contributor_id", DataType.text())
        .addClusteringColumn("year", DataType.cint())
        .addClusteringColumn("month", DataType.cint())
        .addUDTListColumn("contributions", frozen("contribution"))
        .ifNotExists()
        .withOptions()
        .clusteringOrder("year", Direction.DESC)
        .clusteringOrder("month", Direction.ASC);

    final BatchStatement batch = new BatchStatement();
    batch.add(createTypeStmt);
    batch.add(createContributors);
    batch.add(createContributions);

    return session.underlying().thenApply(underlyingSession -> {
      logger.info("Creating schema..");
      logger.info("Schema creation statements: {}", batch.getStatements());
      batch.getStatements().forEach(underlyingSession::execute);

      // keep around UDT type definition for later use when writing contributions
      // TODO: Configuration should be injected to get the keyspace name

      final KeyspaceMetadata keySpace = underlyingSession.getCluster().getMetadata()
          .getKeyspace(keyspace);
      if (keySpace == null) {
        throw new IllegalStateException(Messages.E_CASSANDRA_NO_KEYSPACE.get(keyspace));
      }
      this.contributionUdtType = keySpace.getUserType("contribution");
      return Done.getInstance();
    });
  }

  private CompletionStage<Done> prepareStatements() {

    return CompletableFuture
        .supplyAsync(this::prepareWriteContributors)
        .thenCompose(done -> prepareWriteContributions());
  }

  private CompletionStage<Done> prepareWriteContributors() {
    return session.prepare(
        "INSERT INTO contributors (id, region, registration_date)" +
            " VALUES (?, ?, ?)")
        .thenApply(ps -> {
          this.writeContributors = ps;
          return Done.getInstance();
        });
  }

  private CompletionStage<Done> prepareWriteContributions() {
    return session.prepare(
        "INSERT INTO contributions (contributor_id, region, year, month, contributions)" +
            " VALUES (?, ?, ?, ?, ?)")
        .thenApply(ps -> {
          this.writeContributions = ps;
          return Done.getInstance();
        });
  }
}
