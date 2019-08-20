package income.tax.impl.calculation;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ContributionType {
  Maladie1T2("MLD1T2"), // Maladie 1 dans la limite de 5 PASS
  Maladie1T1("MLD1T1"), // Maladie 1 au delà de de 5 PASS
  Maladie2("MLD2"), // Maladie 2
  RetraiteT1("RVB T1"), // Retraite de base dans la limite de PASS
  RetraiteT2("RVB T2"), // Retraite de base au delà de PASS
  RetraiteComplémentaireT1("RCI T1"), // Retraite de complémentaire dans la limite de PRCI
  RetraiteComplémentaireT2("RCI T2"), // Retraite de complémentaire entre PRCI et 4 x PASS
  InvalidtitéDécès("RID"), // Invalidité-décès dans la limite de PASS
  AllocationsFamiliales("AF"),
  CSG_CRDS("CSG/CRDS"),
  ;

  private final String code;

  ContributionType(String code) {
    this.code = code;
  }

  @JsonValue
  public String code() {
    return code;
  }
}
