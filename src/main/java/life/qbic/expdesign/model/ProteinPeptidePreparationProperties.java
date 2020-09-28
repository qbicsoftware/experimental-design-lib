package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProteinPeptidePreparationProperties {

  private List<String> enzymes;
  private String digestionMethod;
  private String sampleType;
  private String sampleCleanup;
  private String samplePrep;
  private String fractionationMethod;
  private String enrichmentMethod;
  private String labelingMethod;

  public ProteinPeptidePreparationProperties(List<String> enzymes, String digestionMethod,
      String sampleType, String sampleCleanup, String fractionationMethod, String enrichmentMethod,
      String labelingMethod, String samplePrep) {
    super();
    this.enzymes = enzymes;
    this.digestionMethod = digestionMethod;
    this.sampleType = sampleType;
    this.sampleCleanup = sampleCleanup;
    this.fractionationMethod = fractionationMethod;
    this.enrichmentMethod = enrichmentMethod;
    this.labelingMethod = labelingMethod;
    this.samplePrep = samplePrep;
  }
  
  private Map<String, Object> parseSamplePrepData(String[] row, Map<String, Integer> headerMapping,
      HashMap<String, Object> metadata) {
    Map<String, String> designMap = new HashMap<String, String>();

    designMap.put("Fractionation Type", "Q_MS_FRACTIONATION_METHOD");

    designMap.put("Labeling Type", "Q_LABELING_METHOD");
    // TODO digestion type
    // designMap.put("Digestion Method", "Q_DIGESTION_METHOD");
    designMap.put("Digestion enzyme", "Q_DIGESTION_METHOD");

    designMap.put("Sample Cleanup (protein)", "Q_MS_PURIFICATION_METHOD");

    designMap.put("Sample Cleanup (peptide)", "Q_MS_PURIFICATION_METHOD");

    designMap.put("Enrichment Method", "Q_MS_ENRICHMENT_METHOD");
    for (String col : designMap.keySet()) {
      Object val = "";
      String openbisType = designMap.get(col);
      if (headerMapping.containsKey(col)) {
        val = row[headerMapping.get(col)];
        if (parsers.containsKey(openbisType)) {
          val = parsers.get(openbisType).parse((String) val);
        }
      }
      metadata.put(openbisType, val);
    }
    return metadata;
  }

  public List<String> getEnzymes() {
    return enzymes;
  }

  public String getSamplePrep() {
    return samplePrep;
  }

  public String getDigestionMethod() {
    return digestionMethod;
  }

  public String getSampleType() {
    return sampleType;
  }

  public String getSampleCleanup() {
    return sampleCleanup;
  }

  public String getFractionationMethod() {
    return fractionationMethod;
  }

  public String getEnrichmentMethod() {
    return enrichmentMethod;
  }

  public String getLabelingMethod() {
    return labelingMethod;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((enrichmentMethod == null) ? 0 : enrichmentMethod.hashCode());
    result = prime * result + ((enzymes == null) ? 0 : enzymes.hashCode());
    result = prime * result + ((fractionationMethod == null) ? 0 : fractionationMethod.hashCode());
    result = prime * result + ((labelingMethod == null) ? 0 : labelingMethod.hashCode());
    result = prime * result + ((sampleCleanup == null) ? 0 : sampleCleanup.hashCode());
    result = prime * result + ((sampleType == null) ? 0 : sampleType.hashCode());
    result = prime * result + ((samplePrep == null) ? 0 : samplePrep.hashCode());
    result = prime * result + ((digestionMethod == null) ? 0 : digestionMethod.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ProteinPeptidePreparationProperties other = (ProteinPeptidePreparationProperties) obj;
    if (enrichmentMethod == null) {
      if (other.enrichmentMethod != null)
        return false;
    } else if (!enrichmentMethod.equals(other.enrichmentMethod))
      return false;
    if (enzymes == null) {
      if (other.enzymes != null)
        return false;
    } else if (!enzymes.equals(other.enzymes))
      return false;
    if (fractionationMethod == null) {
      if (other.fractionationMethod != null)
        return false;
    } else if (!fractionationMethod.equals(other.fractionationMethod))
      return false;
    if (digestionMethod == null) {
      if (other.digestionMethod != null)
        return false;
    } else if (!digestionMethod.equals(other.digestionMethod))
      return false;
    if (labelingMethod == null) {
      if (other.labelingMethod != null)
        return false;
    } else if (!labelingMethod.equals(other.labelingMethod))
      return false;
    if (sampleCleanup == null) {
      if (other.sampleCleanup != null)
        return false;
    } else if (!sampleCleanup.equals(other.sampleCleanup))
      return false;
    if (samplePrep == null) {
      if (other.samplePrep != null)
        return false;
    } else if (!samplePrep.equals(other.samplePrep))
      return false;
    if (sampleType == null) {
      if (other.sampleType != null)
        return false;
    } else if (!sampleType.equals(other.sampleType))
      return false;
    return true;
  }

  public Map<String, Object> getPropertyMap() {
    Map<String, Object> res = new HashMap<String, Object>();
    if (fractionationMethod != null && !fractionationMethod.isEmpty()) {
      res.put("Q_MS_FRACTIONATION_METHOD", fractionationMethod);
    }
    if (labelingMethod != null && !labelingMethod.isEmpty()) {
      res.put("Q_LABELING_METHOD", labelingMethod);
    }
    if (enzymes != null && !enzymes.isEmpty()) {
      res.put("Q_DIGESTION_ENZYMES", enzymes);
    }
    if (digestionMethod != null && !digestionMethod.isEmpty()) {
      res.put("Q_DIGESTION_METHOD", digestionMethod);
    }
    if (sampleCleanup != null && !sampleCleanup.isEmpty()) {
      res.put("Q_MS_PURIFICATION_METHOD", sampleCleanup);
    }
    if (enrichmentMethod != null && !enrichmentMethod.isEmpty()) {
      res.put("Q_MS_ENRICHMENT_METHOD", enrichmentMethod);// TODO METHOD_DETAILED?
    }
    if (samplePrep != null && !samplePrep.isEmpty()) {
      res.put("Q_SAMPLE_PREPARATION_METHOD", samplePrep);
    }
    return res;
  }

}
