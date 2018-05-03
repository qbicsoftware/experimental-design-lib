package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SampleSummaryBean {

  Map<String, String> sampleTypeMap;
  // private String ID_Range;
  private String sampleType;
  private String sampleContent;
  private String amount;
  private boolean isPartOfSplit; // parent sample was split into this and other samples (e.g.
                                 // replicates, fractionation or enrichment)
  private boolean isPool;

  public SampleSummaryBean(String bioType, Set<String> content, String amount, boolean isPool,
      boolean isPartOfSplit) {
    Map<String, String> typeMap = new HashMap<String, String>() {
      /**
       * 
       */
      private static final long serialVersionUID = 1442236408610900500L;

      {
        put("Q_BIOLOGICAL_ENTITY", "Sample Sources");
        put("Q_BIOLOGICAL_SAMPLE", "Sample Extracts");
        put("Q_TEST_SAMPLE", "Sample Preparations");
        put("Q_NGS_SINGLE_SAMPLE_RUN", "Next-Generation Sequencing Run");
        put("Q_MS_RUN", "Mass Spectrometry Run(s)");
        put("Q_MHC_LIGAND_EXTRACT", "MHC Ligand Extracts");
      };
    };
    String sampleContent = "";
    for (String c : content) {
      sampleContent += c + ", ";
    }
    if (sampleContent.endsWith(", "))
      sampleContent = sampleContent.substring(0, sampleContent.length() - 2);
    // this.ID_Range = iD_Range;
    if (typeMap.containsKey(bioType))
      bioType = typeMap.get(bioType);
    this.sampleType = bioType;
    this.amount = amount;
    this.isPartOfSplit = isPartOfSplit;
    this.isPool = isPool;
    this.sampleContent = sampleContent;
  }

  // public String getID_Range() {
  // return ID_Range;
  // }

  public boolean isPool() {
    return isPool;
  }

  // public void setID_Range(String iD_Range) {
  // ID_Range = iD_Range;
  // }

  public String getSampleType() {
    return sampleType;
  }

  public String getFullSampleContent() {
    return sampleContent;
  }

  /**
   * this is shortened if too long
   * 
   * @return
   */
  public String getSampleContent() {
    String res = sampleContent;
    if (sampleContent.length() > 50) {
      res = "";
      for (String single : sampleContent.split(", ")) {
        if (res.length() < 50) {
          res += single + ", ";
        } else {
          res += single + " etc.";
          break;
        }
      }
    }
    return res;
  }

  public void setSampleContent(String content) {
    this.sampleContent = content;
  }

  public void setType(String type) {
    if (sampleTypeMap.containsKey(type))
      type = sampleTypeMap.get(type);
    sampleType = type;
  }

  public boolean isPartOfSplit() {
    return isPartOfSplit;
  }

  public String getAmount() {
    return amount;
  }

  public void setAmount(String amount) {
    this.amount = amount;
  }

  public String toString() {
    String split = "Split ";
    String pool = "Pooled ";
    String content = "";
    if (!sampleContent.isEmpty())
      content = "(" + sampleContent + ") ";
    String res = sampleType + " " + content + amount;
    if (isPool)
      res = pool + res;
    if (isPartOfSplit)
      res = split + res;
    return res;
  }

  public SampleSummaryBean copy() {
    SampleSummaryBean b =
        new SampleSummaryBean(sampleType, new HashSet<String>(), amount, isPool, isPartOfSplit);
    b.setSampleContent(sampleContent);
    return b;
  }

}
