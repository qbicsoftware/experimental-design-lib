package life.qbic.expdesign.model;

public enum MetaboExperimentProperties {

  Species("Species"),
  Organism_ID("Organism ID"),
  Secondary_Name("Secondary name"),
  Injection_Volume("Injection volume (uL)"),
  LCMS_Method_Name("LCMS method name"),
  LC_Device("LC device"),
  Biospecimen("Biospecimen"),
  Expression_System("Expression system"),
  LC_Detection_Method("LC detection method"),
  LC_Column_Name("LC column name"),
  MS_Device("MS device"),
  MS_Ion_Mode("MS ion mode"),
  Harvesting_Method("Harvesting method"),
  Harvesting_Volume("Harvesting volume (ml)"),
  Technical_Comments("Technical comments"),
  Strain_Lab_Collection_Number("Strain lab collection number"),
  Medium("Medium"),
  Harvesting_Conditions("Harvesting conditions"),
  Washing_Solvent("Washing solvent"),
  Cell_Lysis("Cell lysis"),
  Lysis_Parameters("Lysis parameters"),
  Sample_Solvent("Sample solvent"),
  Mass_Resolving_Power("Mass resolving power"),
  Dissociation_Method("Dissociation method"),
  Dissociation_Energy("Dissociation energy (eV)");

  public final String label;

  private MetaboExperimentProperties(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
