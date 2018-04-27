package life.qbic.expdesign.model;

import java.util.List;
import java.util.Map;

import life.qbic.datamodel.entities.SampleSummary;

public class StructuredExperiment {

  private Map<String, List<SampleSummary>> factorsToSamples;

  public StructuredExperiment(Map<String, List<SampleSummary>> factorsToSamples2) {

    super();
    this.factorsToSamples = factorsToSamples2;
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

}
