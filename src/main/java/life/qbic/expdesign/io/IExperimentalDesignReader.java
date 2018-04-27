package life.qbic.expdesign.io;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import life.qbic.datamodel.entities.ISampleBean;
import life.qbic.datamodel.entities.SampleSummary;

public interface IExperimentalDesignReader {

  List<ISampleBean> readSamples(File file) throws IOException, JAXBException;

  String getError();

  Map<String, List<Map<String, Object>>> getExperimentInfos();
  
  Set<String> getSpeciesSet();
  Set<String> getTissueSet();
  Set<String> getAnalyteSet();
  List<String> getTSVByRows();

  Map<String, List<SampleSummary>> getSampleGraphNodes();

}
