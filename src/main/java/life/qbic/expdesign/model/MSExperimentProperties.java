package life.qbic.expdesign.model;

public enum MSExperimentProperties {

  Species("Species"),
  Organism_ID("Organism ID"),
  Secondary_Name("Secondary Name"),
  Sample_Name("Sample Name"),
  Injection_Volume("Injection Volume (uL)"),
  LCMS_Method("LCMS Method"),
  MS_Device("MS Device"),
  LC_Column("LC Column"),
  File_Name("File Name"),
  Labeling_Type("Labeling Type"),
  Tissue("Tissue"),
  Technical_Replicates("Technical Replicates"),
  Expression_System("Expression System"),
  Pooled_Sample("Pooled Sample"),
  Cycle_Fraction_Name("Cycle/Fraction Name"),
  Fractionation_Type("Fractionation Type"),
  Sample_Preparation("Sample Preparation"),
  Sample_Cleanup_Protein("Sample Cleanup (Protein)"),
  Digestion_Method("Digestion Method"),
  Enrichment_Method("Enrichment Method"),
  Sample_Cleanup_Peptide("Sample Cleanup (Peptide)"),
  Label("Label"),
  Customer_Comment("Customer Comment"),
  Facility_Comment("Facility Comment"),
  Digestion_Enzyme("Digestion Enzyme");

  public final String label;

  private MSExperimentProperties(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
