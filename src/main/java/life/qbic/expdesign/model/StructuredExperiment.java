package life.qbic.expdesign.model;

import java.util.List;
import java.util.Map;
 import org.isatools.isacreator.model.Study;
import life.qbic.datamodel.samples.SampleSummary;

public class StructuredExperiment {

   private Study study;
  private Map<String, List<SampleSummary>> factorsToSamples;

   public StructuredExperiment(Map<String, List<SampleSummary>> factorsToSamples2, Study study) {
   super();
   this.factorsToSamples = factorsToSamples2;
   this.study = study;
   }
  
  public StructuredExperiment(Map<String, List<SampleSummary>> factorsToSamples2) {
    factorsToSamples = factorsToSamples2;
    // this(factorsToSamples2, null);
  }

  public Map<String, List<SampleSummary>> getFactorsToSamples() {
    return factorsToSamples;
  }

  public void setFactorsToSamples(Map<String, List<SampleSummary>> factorsToSamples) {
    this.factorsToSamples = factorsToSamples;
  }

  @Override
  public String toString() {
    return factorsToSamples.toString();
  }

   public boolean hasStudy() {
   return study != null;
   }
  
   public Study getStudy() {
   return study;
   }
  
   public void setStudy(Study study) {
   this.study = study;
   }

}
