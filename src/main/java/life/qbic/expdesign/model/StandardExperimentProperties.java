package life.qbic.expdesign.model;

public enum StandardExperimentProperties {

  Organism("Organism"),
  Organism_ID("Organism ID"),
  Tissue("Tissue"),
  Extract_ID("Extract ID"),
  Source_Comment("Source Comment"),
  Tissue_Comment("Tissue Comment"),
  Detailed_Tissue("Detailed Tissue"),
  Analyte("Analyte"),
  Analyte_ID("Analyte ID"),
  Preparation_Comment("Preparation Comment"),
  Expression_System("Expression System");

  public final String label;

  private StandardExperimentProperties(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
