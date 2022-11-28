package life.qbic.expdesign;

import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.projects.ProjectInfo;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.expdesign.io.IExperimentalDesignReader;
import life.qbic.expdesign.io.QBiCDesignReader;
import life.qbic.expdesign.model.ExperimentalDesignPropertyWrapper;
import life.qbic.expdesign.model.OpenbisPropertyCodes;
import life.qbic.expdesign.model.SampleSummaryBean;
import life.qbic.expdesign.model.StructuredExperiment;
import life.qbic.xml.study.TechnologyType;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;

public class SamplePreparator {

  private IExperimentalDesignReader reader;
  private List<ISampleBean> samples;
  private List<List<ISampleBean>> processed;
  private ArrayList<SampleSummaryBean> summary;
  private ProjectInfo projectinfo;
  private Map<String, ISampleBean> idsToSamples;
  private ExperimentalDesignPropertyWrapper experimentalDesignXML;

  public SamplePreparator() {
    processed = new ArrayList<List<ISampleBean>>();
    summary = new ArrayList<SampleSummaryBean>();
  }

  public List<String> getOriginalTSV() {
    return reader.getTSVByRows();
  }

  /**
   * Reads in a TSV File containing samples for openBIS registration and their metadata.
   * 
   * @param file A TSV File that must contain Identifier, space, project, experiment and sample type
   *        of each sample and optionally the parents of each sample. Additional metadata columns
   *        must be known to openBIS and fit to the sample type in question.
   * @param reader
   * @throws IOException
   * @throws JAXBException
   */
  public boolean processTSV(File file, IExperimentalDesignReader reader, boolean parseGraph)
      throws IOException, JAXBException {
    this.reader = reader;

    List<ISampleBean> rawSamps = reader.readSamples(file, parseGraph);
    if (reader instanceof QBiCDesignReader) {
      QBiCDesignReader qReader = (QBiCDesignReader) reader;
      projectinfo = new ProjectInfo(qReader.getSpace(), qReader.getProject(),
          qReader.getDescription(), qReader.getSecondaryName(), qReader.isPilot(),
          qReader.getInvestigator(), qReader.getContact(), qReader.getManager());
    }
    if (reader.getError() != null)
      return false;

    // map to save children, used to find out more about project structure to present in summary
    // beans
    Map<String, List<ISampleBean>> sampleToChildrenMap = new HashMap<String, List<ISampleBean>>();
    idsToSamples = new HashMap<String, ISampleBean>();
    experimentalDesignXML =
        ParserHelpers.samplesWithMetadataToExperimentalFactorStructure(rawSamps);
    for (ISampleBean b : rawSamps) {
      idsToSamples.put(b.getCode(), b);
      // translate tsv presentation of special metadata to xml
      // ParserHelpers.fixXMLProps(b.getMetadata());

      // fill children map
      for (String parent : b.getParentIDs()) {
        if (sampleToChildrenMap.containsKey(parent))
          sampleToChildrenMap.get(parent).add(b);
        else
          sampleToChildrenMap.put(parent, new ArrayList<ISampleBean>(Arrays.asList(b)));
      }
    }
    processTSV(rawSamps, sampleToChildrenMap);
    return true;
  }

  private String convert(String value, Charset fromEncoding, Charset toEncoding)
      throws UnsupportedEncodingException {
    return new String(value.getBytes(fromEncoding), toEncoding);
  }

  private boolean probe(String value, Charset charset) throws UnsupportedEncodingException {
    Charset probe = StandardCharsets.UTF_8;
    return value.equals(convert(convert(value, charset, probe), probe, charset));
  }

  public String convert(String value, Charset charsetWanted, List<Charset> charsetsOther)
      throws UnsupportedEncodingException {
    if (probe(value, charsetWanted)) {
      return value;
    }
    for (Charset other : charsetsOther) {
      if (probe(value, other)) {
        return convert(value, other, charsetWanted);
      }
    }
    System.err.println("WARNING: Could not convert string: " + value);
    return value;
  }

  private void processTSV(List<ISampleBean> samples,
      Map<String, List<ISampleBean>> sampleToChildrenMap) {
    this.samples = samples;
    ArrayList<SampleSummaryBean> summary = new ArrayList<SampleSummaryBean>();
    Map<Integer, List<ISampleBean>> levels = null;
    levels = readLevels();
    ArrayList<Integer> numLevels = new ArrayList<Integer>(levels.keySet());
    Collections.sort(numLevels);
    List<List<ISampleBean>> processed = new ArrayList<List<ISampleBean>>();
    Comparator<ISampleBean> comp = SampleTypeComparator.getInstance();
    for (int i : numLevels) {
      List<ISampleBean> level = new ArrayList<ISampleBean>();

      List<ISampleBean> currentLevel = levels.get(i);
      Collections.sort(currentLevel, comp);

      SampleType type = currentLevel.get(0).getType();
      ArrayList<ISampleBean> currList = new ArrayList<ISampleBean>();
      for (ISampleBean s : currentLevel) {
        if (type.equals(s.getType()))
          currList.add(s);
        else {
          level.addAll(currList);
          fetchSummary(summary, currList, i, sampleToChildrenMap);
          currList = new ArrayList<ISampleBean>(Arrays.asList(s));
          type = s.getType();
        }
      }
      fetchSummary(summary, currList, i, sampleToChildrenMap);

      level.addAll(currList);
      processed.add(level);
    }
    this.processed = processed;
    this.summary = summary;
  }

  /**
   * should only be called once after each parsing
   * 
   * @param experimentType
   * @return
   */
  public Map<String, Map<String, Object>> transformAndReturnSpecialExperimentsOfTypeOrNull(
      ExperimentType experimentType) {
    if (reader.getExperimentInfos() == null)
      return null;
    List<Map<String, Object>> exps = reader.getExperimentInfos().get(experimentType);
    if (exps != null) {
      Map<String, Map<String, Object>> res = new HashMap<String, Map<String, Object>>();
      for (Map<String, Object> exp : exps) {
        String code = (String) exp.get("Code");
        exp.remove("Code");
        res.put(code, exp);
      }
      return res;
    } else
      return null;
  }

  /**
   * Returns a useful error text if an error occured while reading the TSV or null otherwise.
   * 
   * @return String error text
   */
  public String getError() {
    return reader.getError();
  }

  /**
   * Returns a summary (as list of SampleSummaryBeans) of the sample hierarchy contained in the
   * processed tsv file.
   * 
   * @return An ArrayList containing SampleSummaryBeans.
   */
  public List<SampleSummaryBean> getSummary() {
    List<SampleSummaryBean> res = new ArrayList<SampleSummaryBean>();
    for (SampleSummaryBean b : summary)
      res.add(b.copy());
    return res;
  }

  public List<TechnologyType> getTechnologyTypes() {
    return reader.getTechnologyTypes();
  }

  public Set<String> getSpeciesSet() {
    return reader.getSpeciesSet();
  }

  public Set<String> getTissueSet() {
    return reader.getTissueSet();
  }

  public Set<String> getAnalyteSet() {
    return reader.getAnalyteSet();
  }

  /**
   * Returns a nested list structure of all processed samples to be registered, using
   * TSVSampleBeans. The outer list sorts samples by their hierarchy (parent-child-structure of
   * openBIS), beginning with the highest samples.
   * 
   * @return Nested list of samples to be registered, sorted by hierarchy level and sample type.
   */
  public List<List<ISampleBean>> getProcessed() {
    List<List<ISampleBean>> res = new ArrayList<List<ISampleBean>>();
    for (List<ISampleBean> level : processed) {
      List<ISampleBean> newLevel = new ArrayList<ISampleBean>();
      for (ISampleBean b : level)
        newLevel.add(b.copy());
      res.add(newLevel);
    }
    return res;
  }

  public StructuredExperiment getSampleGraph() {
    return reader.getGraphStructure();
  }

  public ProjectInfo getProjectInfo() {
    return projectinfo;
  }

  public String toGraphML() {
    if (processed == null)
      return "";
    else {
      String res = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?> \n"
          + "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" "
          + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
          + "xmlns:y=\"http://www.yworks.com/xml/graphml\" "
          + "xmlns:yed=\"http://www.yworks.com/xml/yed/3\" "
          + "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns "
          + "http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd\">"
          + "<key for=\"node\" id=\"d1\" yfiles.type=\"nodegraphics\"/> \n"
          + "<key for=\"edge\" id=\"d2\" yfiles.type=\"edgegraphics\"/> \n"
          + "<graph id=\"Experimental Design\" edgedefault=\"undirected\"> \n";
      int e = 1;
      for (List<ISampleBean> lis : processed) {
        for (ISampleBean b : lis) {
          String code = b.getCode();
          res += "<node id=\"" + code + "\"> \n" + "<data key=\"d1\"> \n" + "<y:ShapeNode> \n"
              + "<y:Geometry width=\"105\"/>" + "<y:NodeLabel>" + code + " \n" + "</y:NodeLabel> \n"
              + "</y:ShapeNode> \n" + "</data> \n" + "</node> \n";
          if (b.hasParents()) {
            List<String> parents = b.getParentIDs();
            for (String p : parents) {
              res += "<edge id=\"" + e + "\" source=\"" + code + "\" target=\"" + p + "\"> \n"
                  + "<data key=\"d2\"> \n" + "<y:PolyLineEdge> \n"
                  + "<y:Arrows source=\"none\" target=\"none\"/> \n" + "</y:PolyLineEdge> \n"
                  + "</data> \n" + "</edge> \n";
              e++;
            }
          }
        }
      }
      res += "</graph> \n" + "</graphml>";
      return res;
    }
  }

  // Helper for creating a summary (as list of SampleSummaryBeans) of the processed tsv file
  private void fetchSummary(ArrayList<SampleSummaryBean> summary, ArrayList<ISampleBean> currList,
      int i, Map<String, List<ISampleBean>> sampleToChildrenMap) {
    boolean isPartOfSplit = false;
    boolean pool = false;
    Set<String> sampleContent = new HashSet<>();
    for (ISampleBean s : currList) {
      String queryType = null;
      switch (s.getType()) {
        case Q_BIOLOGICAL_ENTITY:
          queryType = OpenbisPropertyCodes.Q_NCBI_ORGANISM.name();
          break;
        case Q_BIOLOGICAL_SAMPLE:
          queryType = OpenbisPropertyCodes.Q_PRIMARY_TISSUE.name();
          break;
        case Q_TEST_SAMPLE:
          queryType = OpenbisPropertyCodes.Q_SAMPLE_TYPE.name();
          break;
        default:
          queryType = null;
          break;
      }
      if (queryType != null) {
        String content = (String) s.getMetadata().get(queryType);
        sampleContent.add(content);
      }

      List<String> parentCodes = s.getParentIDs();
      if (parentCodes.size() > 1) {
        pool = true;
      } else {
        SampleType type = s.getType();
        for (String parent : parentCodes) {
          // at least one parent leads to this child sample and at least one other child sample
          // (sibling) of the same type
          for (ISampleBean sibling : sampleToChildrenMap.get(parent)) {
            if (!s.getCode().equals(sibling.getCode()) && sibling.getType().equals(type))
              isPartOfSplit = true;
          }
        }

      }
    }
    summary.add(new SampleSummaryBean(currList.get(0).getType().toString(), sampleContent,
        Integer.toString(currList.size()), pool, isPartOfSplit));
  }

  // expects samples to be in the right order already, with the first tier coming first and the last
  // tier coming later
  // new levels are created when the sample type changes or if a sample is found with a parent in an
  // existing tier
  private Map<Integer, List<ISampleBean>> readLevels() {
    Map<Integer, List<ISampleBean>> knownLevels = new HashMap<>();
    SampleType type = samples.get(0).getType();
    int i = 1;
    List<ISampleBean> level = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    for (ISampleBean b : samples) {
      boolean newLvl = false;
      if (!b.getType().equals(type)) {// || !b.getExperiment().equals(exp)) {
        newLvl = true;
      } else if (b.hasParents()) {
        for (String id : b.getParentIDs()) {
          newLvl |= ids.contains(id);
        }
      }
      if (newLvl) {
        knownLevels.put(i, level);
        i++;
        type = b.getType();
        level = new ArrayList<ISampleBean>();
        ids = new HashSet<String>();
      }
      ids.add(b.getCode());
      level.add(b);
    }
    knownLevels.put(i, level);
    return knownLevels;
  }

  public Map<String, ISampleBean> getIDsToSamples() {
    return idsToSamples;
  }

  public ExperimentalDesignPropertyWrapper getExperimentalDesignProperties() {
    return experimentalDesignXML;
  }

  /**
   * Returns a map containing lists of unique values from an experimental design format based on property names
   * @param names a list of property names found in the format for which data should be returned
   * @return a mapping between property names and a list of unique values read by the respective parser of the format
   */
  public Map<String, List<String>> getParsedValuesForCategories(ArrayList<String> names) {
    return reader.getParsedValuesForColumns(names);
  }

}
