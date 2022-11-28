package life.qbic.expdesign.model;

public enum LigandomicsExperimentProperties {

  Organism("Organism"),
  Patient_ID("Patient ID"),
  Antibody("Antibody"),
  Antibody_Mass("Antibody Mass"),
  Prep_Date("Prep Date"),
  MS_Run_Date("MS Run Date"),
  MS_Device("MS Device"),
  HLA_Typing("HLA Typing"),
  File_Name("Filename"),
  Share("Share"),
  Tissue("Tissue"),
  LCMS_Method("LCMS Method"),
  Replicate("Replicate"),
  Workflow_Type("Workflow Type"),
  Sequencing("Sequencing"),
  Tumor_Type("Tumor Type"),
  Cell_Type("Cell Type"),
  MS_Comment("MS Comment"),
  Sample_Volume("Sample Volume"),
  Sample_Mass("Sample Mass"),
  Cell_Count("Cell Count"),
  Source_Comment("Source Comment"),
  Other_Data_Reference("Other Data"),
  Tissue_Comment("Tissue Comment"),
  Detailed_Tissue("Detailed Tissue"),
  Dignity("Dignity"),
  Location("Location"),
  TNM("TNM"),
  Metastasis("Metastasis"),
  MHC_Class("MHC Class");

  public final String label;

  private LigandomicsExperimentProperties(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
