package life.qbic.expdesign.model;

public enum Unit {
  Second("s"), Minute("min"), Hour("h"), Day("d"), Picogram("pg"), Nanogram("ng"), Microgram(
      "\u00B5" + "g"), Milligram("mg"), Gram("g"), Kilogram("kg"), Meter("m"), Ampere("A"), Kelvin(
      "K"), Mole("mol"), Candela("cd"), Pascal("Pa"), Joule("J"), Watt("W"), Newton("N"), Tesla("T"), Henry(
      "H"), Coulomb("C"), Volt("V"), Farad("F"), Siemens("S"), Weber("Wb"), Ohm("\u2126"), Hertz(
      "Hz"), Lux("lx"), Lumen("lm"), Becquerel("Bq"), Gray("Gy"), Sievert("Sv"), Katal("kat"), Microliter(
      "\u00B5" + "l"), Milliliter("ml"), Liter("l"), Picogram_Per_Liter("pg/l"), Nanogram_Per_Liter(
      "ng/l"), Microgram_Per_Liter("\u00B5" + "g/l"), Milligram_Per_Liter("mg/l"), Gram_Per_Liter(
      "g/l"), Picomol_Per_Liter("pmol/l"), Nanomol_Per_Liter("nmol/l"), Micromol_Per_Liter("\u00B5"
      + "mol/l"), Millimol_Per_Liter("mmol/l"), Mol_Per_Liter("mol/l"), Arbitrary_Unit("arb.unit");
  private String value;

  private Unit(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
}
