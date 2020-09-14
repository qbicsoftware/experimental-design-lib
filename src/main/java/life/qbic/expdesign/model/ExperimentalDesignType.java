package life.qbic.expdesign.model;

import java.util.Arrays;
import java.util.List;

public enum ExperimentalDesignType {

  Standard("Standard Format",
      "The Standard Import format for experimental designs containing information about organism, tissues/cell cultures and the analyte preparations.",
      Arrays.asList("Organism", "Organism ID", "Tissue", "Extract ID", "Analyte", "Analyte ID"),
      Arrays.asList("Conditions")),

  QBIC("Internal QBiC Format",
      "QBiC openBIS import format. Not recommended for external users. In this format Source Organism, Tissue Extracts and Analytes are all described in their own rows. Barcodes have to be predefined.",
      Arrays.asList("Identifier", "SAMPLE TYPE", "SPACE", "EXPERIMENT", "PARENT",
          "Q_PRIMARY_TISSUE", "Q_NCBI_ORGANISM", "Q_SAMPLE_TYPE"),
      Arrays.asList("Q_TISSUE_DETAILED", "Q_SECONDARY_NAME", "Q_ADDITIONAL_INFO", "Q_EXTERNALDB_ID",
          "Conditions")),

  ISA("ISA-Tab format",
      "The Investigation/Study/Assay (ISA) tab-delimited (TAB) format is a general purpose framework with which to collect and communicate complex metadata.",
      Arrays.asList(""), Arrays.asList("")),

  MHC_Ligands_Finished("Ligandomics Format (measured)",
      "Format to describe MHC Ligand extraction and measurement experiments. Tissue, Antibody and Mass spectrometry information needs to be provided. Measurements should be from the same project/group of patients.",
      Arrays.asList(""), Arrays.asList("")),

  Proteomics_MassSpectrometry("Protein mass spectrometry",
      "Format to describe sample preparation and MS / LCMS measurement of protein or peptide samples.",
      Arrays.asList(""), Arrays.asList("")),

  MHC_Ligands_Plan("", "", Arrays.asList(), Arrays.asList());

  private final String name;
  private final String description;
  private final List<String> required;
  private final List<String> optional;

  private ExperimentalDesignType(String name, String description, List<String> required,
      List<String> optional) {
    this.name = name;
    this.description = description;
    this.required = required;
    this.optional = optional;
  }

  public String getName() {
    return name;
  }

  public String getFileName() {
    return name.replace(" ", "_") + ".tsv";
  }

  public String getDescription() {
    return description;
  }

  public List<String> getRequired() {
    return required;
  }

  public List<String> getOptional() {
    return optional;
  }

}
