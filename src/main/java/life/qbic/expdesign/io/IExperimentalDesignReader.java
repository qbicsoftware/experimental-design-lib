package life.qbic.expdesign.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleSummary;

public interface IExperimentalDesignReader {

  List<ISampleBean> readSamples(File file, boolean parseGraph) throws IOException, JAXBException;

  String getError();

  Map<String, List<Map<String, Object>>> getExperimentInfos();
  
  Set<String> getSpeciesSet();
  Set<String> getTissueSet();
  Set<String> getAnalyteSet();
  List<String> getTSVByRows();

  Map<String, List<SampleSummary>> getSampleGraphNodes();

  int countEntities(File file) throws IOException;

}
