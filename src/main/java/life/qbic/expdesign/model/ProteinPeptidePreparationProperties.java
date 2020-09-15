package life.qbic.expdesign.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProteinPeptidePreparationProperties {

  private List<String> enzymes;
  private String sampleType;
  private String sampleCleanup;
  private String fractionationMethod;
  private String enrichmentMethod;
  private String labelingMethod;

  public ProteinPeptidePreparationProperties(List<String> enzymes, String sampleType,
      String sampleCleanup, String fractionationMethod, String enrichmentMethod,
      String labelingMethod) {
    super();
    this.enzymes = enzymes;
    this.sampleType = sampleType;
    this.sampleCleanup = sampleCleanup;
    this.fractionationMethod = fractionationMethod;
    this.enrichmentMethod = enrichmentMethod;
    this.labelingMethod = labelingMethod;
  }

  public List<String> getEnzymes() {
    return enzymes;
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
      res.put("Q_DIGESTION_METHOD", enzymes);
    }
    if (sampleCleanup != null && !sampleCleanup.isEmpty()) {
      res.put("Q_MS_PURIFICATION_METHOD", sampleCleanup);
    }
    if (enrichmentMethod != null && !enrichmentMethod.isEmpty()) {
      res.put("Q_MS_ENRICHMENT_METHOD", enrichmentMethod);// TODO METHOD_DETAILED?
    }
    return res;
  }

}
