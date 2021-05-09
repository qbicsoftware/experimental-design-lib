package life.qbic.expdesign.io;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.expdesign.SamplePreparator;

public class MetaboDesignReaderTest {

  private File fullExample1 =
      new File(getClass().getResource("mtx/metabo_full_example1.tsv").getFile());
  private File standardtest =
      new File(getClass().getResource("mtx/metabo_small_test.tsv").getFile());
  private File noReplicates = new File(getClass().getResource("mtx/noRepl.tsv").getFile());
  private File threeGroupsAThree = new File(getClass().getResource("mtx/3a3.tsv").getFile());
  private File allIDsMissing =
      new File(getClass().getResource("mtx/metabo_one_id_missing.tsv").getFile());
  private File oneIDMissing =
      new File(getClass().getResource("mtx/metabo_one_id_missing.tsv").getFile());

  @Before
  public void setUp() {}

  @Test
  public void testExperimentMetadata() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(fullExample1, new MetaboDesignReader(), false);

    assertEquals(null, p.getError());

    Map<String, Object> speciesPrepProps = new HashMap<>();
    speciesPrepProps.put("Q_CULTURE_MEDIUM", "MH");
    // speciesPrepProps.put("Q_CULTURE_TYPE", "solid");

    Map<String, Object> tissuePrepProps = new HashMap<>();
    // tissuePrepProps.put("Q_CELL_HARVESTING_METHOD", "centrifugation");
    tissuePrepProps.put("Q_CELL_LYSIS_METHOD", Arrays.asList("beads", "boiling"));
    tissuePrepProps.put("Q_CELL_LYSIS_PARAMETERS", "lysis stuff");

    Map<String, Object> msProps = new HashMap<>();
    msProps.put("Q_MS_LCMS_METHOD", "1_D_M_ZIC_pHILIC_neg_85-905");
    msProps.put("Q_MS_DEVICE", "HFX");
    msProps.put("Q_LC_DEVICE", "Dionex");
    msProps.put("Q_IONIZATION_MODE", "negative");
    msProps.put("Q_CHROMATOGRAPHY_COLUMN_NAME", "SeQuant ZIC-pHILIC");
    msProps.put("Q_CHROMATOGRAPHY_TYPE", "HILIC");
    // msProps.put("Q_MS_LCMS_METHOD_INFO", "UV");
    msProps.put("Q_LC_DETECTION_METHOD", "UV");
    msProps.put("Q_WASHING_SOLVENT", "alcohol");
    msProps.put("Q_MS_DISSOCIATION_METHOD", "hammer");
    msProps.put("Q_MS_DISSOCIATION_ENERGY", 0.2);
    msProps.put("Q_MS_RESOLVING_POWER", "over 9000");

    ExperimentType msType = ExperimentType.Q_MS_MEASUREMENT;
    ExperimentType orgType = ExperimentType.Q_EXPERIMENTAL_DESIGN;
    ExperimentType extrType = ExperimentType.Q_SAMPLE_EXTRACTION;
    Map<String, Map<String, Object>> extrExps =
        p.transformAndReturnSpecialExperimentsOfTypeOrNull(extrType.toString());
    Map<String, Map<String, Object>> organismExps =
        p.transformAndReturnSpecialExperimentsOfTypeOrNull(orgType.toString());
    Map<String, Map<String, Object>> msExps =
        p.transformAndReturnSpecialExperimentsOfTypeOrNull(msType.toString());
    for (Entry<String, Object> entry : tissuePrepProps.entrySet()) {
      assert (searchExperimentsForProperty(extrExps, extrType, entry));
    }
    for (Entry<String, Object> entry : speciesPrepProps.entrySet()) {
      assert (searchExperimentsForProperty(organismExps, orgType, entry));
    }
    for (Entry<String, Object> entry : msProps.entrySet()) {
      assert (searchExperimentsForProperty(msExps, msType, entry));
    }
  }

  private boolean searchExperimentsForProperty(
      Map<String, Map<String, Object>> specialExperimentsOfTypeOrNull, ExperimentType msType,
      Entry<String, Object> entry) {
    for (String exp : specialExperimentsOfTypeOrNull.keySet()) {
      Map<String, Object> experiment = specialExperimentsOfTypeOrNull.get(exp);
      if (experiment.containsKey(entry.getKey())) {
        System.err.println("search " + experiment.get(entry.getKey()));
        if (experiment.get(entry.getKey()) instanceof List<?>) {
          List<Object> a = (List<Object>) experiment.get(entry.getKey());
          List<Object> b = (List<Object>) entry.getValue();
          for (Object o : a) {
            if (!b.contains(o)) {
              return false;
            }
          }
          return true;
        } else {
          if (experiment.get(entry.getKey()).equals(entry.getValue())) {
            return true;
          }
        }
      }
    }
    System.err.println("--debug--");
    System.err.println(specialExperimentsOfTypeOrNull);
    System.err.println("not found in experiment " + entry);
    return false;
  }

  public String convert(String value, String fromEncoding, String toEncoding)
      throws UnsupportedEncodingException {
    return new String(value.getBytes(fromEncoding), toEncoding);
  }

  public String charset(String value, String charsets[]) throws UnsupportedEncodingException {
    String probe = StandardCharsets.UTF_8.name();
    for (String c : charsets) {
      Charset charset = Charset.forName(c);
      if (charset != null) {
        if (value.equals(convert(convert(value, charset.name(), probe), probe, charset.name()))) {
          return c;
        }
      }
    }
    return StandardCharsets.UTF_8.name();
  }

  @Test
  public void testOrganismIDNeeded() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(allIDsMissing, new MetaboDesignReader(), false);
    assert (!p.getError().isEmpty());

    p.processTSV(oneIDMissing, new MetaboDesignReader(), false);
    assert (!p.getError().isEmpty());
  }

  @Test
  public void testSampleMetadata() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(fullExample1, new MetaboDesignReader(), false);
    List<List<ISampleBean>> processed = p.getProcessed();
    Map<String, String> entityMetadata = new HashMap<>();
    entityMetadata.put("Q_NCBI_ORGANISM", "E. coli");
    entityMetadata.put("Q_STRAIN_LAB_COLLECTION_NUMBER", "12345");

    Map<String, String> tissueMetadata = new HashMap<>();
    tissueMetadata.put("Q_PRIMARY_TISSUE", "cell wall");
    // tissueMetadata.put("Q_TISSUE_DETAILED", "cell wall");

    Map<String, String> molMetadata = new HashMap<>();
    molMetadata.put("Q_SAMPLE_TYPE", "SMALLMOLECULES");

    Map<String, String> msRunMetadata = new HashMap<>();
    msRunMetadata.put("Q_INJECTION_VOLUME", "10");
    msRunMetadata.put("Q_SAMPLE_SOLVENT", "beer");

    for (Entry<String, String> entry : entityMetadata.entrySet()) {
      assert (searchSamplesForProperty(processed, SampleType.Q_BIOLOGICAL_ENTITY, entry));
    }
    for (Entry<String, String> entry : tissueMetadata.entrySet()) {
      assert (searchSamplesForProperty(processed, SampleType.Q_BIOLOGICAL_SAMPLE, entry));
    }
    for (Entry<String, String> entry : molMetadata.entrySet()) {
      assert (searchSamplesForProperty(processed, SampleType.Q_TEST_SAMPLE, entry));
    }
    for (Entry<String, String> entry : msRunMetadata.entrySet()) {
      assert (searchSamplesForProperty(processed, SampleType.Q_MS_RUN, entry));
    }
  }

  private boolean searchSamplesForProperty(List<List<ISampleBean>> hierarchy, SampleType type,
      Entry<String, String> entry) {
    for (List<ISampleBean> level : hierarchy) {
      if (level.get(0).getType().equals(type)) {
        for (ISampleBean s : level) {
          if (s.getMetadata().containsKey(entry.getKey())) {
            if (s.getMetadata().get(entry.getKey()).equals(entry.getValue())) {
              return true;
            }
          }
        }
      }
    }
    System.err.println("not found " + entry + " in " + hierarchy);
    return false;
  }

  @Test
  public void testCountEntities() throws IOException {
    MetaboDesignReader r = new MetaboDesignReader();
    assertEquals(r.countEntities(fullExample1), -1);// not implemented
  }

  // @Test
  // public void testTrimHeader() throws IOException {
  // MetaboDesignReader r = new MetaboDesignReader();
  // r.readSamples(trimmableHeader, false);
  // assert (r.getError() == null);
  // r.readSamples(falseHeader, false);
  // assert (!r.getError().isEmpty());
  // assert (r.getError().contains("Organism ID"));
  // }


  @Test
  public void testXMLDesign() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    MetaboDesignReader r = new MetaboDesignReader();
    p.processTSV(fullExample1, r, false);
    assertEquals(null, r.getError());
    Map<String, Map<Pair<String, String>, List<String>>> design =
        p.getExperimentalDesignProperties().getExperimentalDesign();
    assert (design.containsKey("growth_temperature"));
    assert (design.containsKey("growth_time"));
    assert (design.containsKey("growth_rpm"));
    assert (design.containsKey("treatment"));
    assert (design.containsKey("stimulus"));
    System.err.println(design.keySet());
    assert (design.containsKey("stimulation_od"));
    assert (design.containsKey("stimulation_time"));
    assertEquals(design.get("treatment").keySet().size(), 4);
  }

  @Test
  public void testGetVocabularyValues() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    MetaboDesignReader r = new MetaboDesignReader();
    p.processTSV(fullExample1, r, false);
    System.out.println("vocabs");
    System.out.println(p.getParsedCategoriesToValues(new ArrayList<String>(
        Arrays.asList("Medium", "Harvesting conditions", "LCMS method name", "LC device",
            "LC detection method", "MS device", "MS ion mode", "Species", "Biospecimen"))));

    if (r.getError() != null)
      System.out.println(r.getError());
    assertNull(r.getError());
  }

  @Test
  public void testReadSamples() throws IOException, JAXBException {
    MetaboDesignReader r = new MetaboDesignReader();

    List<ISampleBean> samples1 = r.readSamples(noReplicates, false);

    assertEquals(null, r.getError());

    if (r.getError() != null)
      System.err.println("testReadSamples error? " + r.getError());

    r = new MetaboDesignReader();
    List<ISampleBean> samples2 = r.readSamples(threeGroupsAThree, true);

    assertEquals(null, r.getError());
    assertNotEquals(samples1, samples2);

    SamplePreparator p = new SamplePreparator();
    p.processTSV(noReplicates, new MetaboDesignReader(), false);
    System.err.print(p.getSummary());
    assert (p.getSummary().size() == 4);
    List<List<ISampleBean>> levels = p.getProcessed();
    assertEquals(levels.get(0).size(), 1);// organism
    assertEquals(levels.get(1).size(), 1);// tissue
    assertEquals(levels.get(2).size(), 3);// metabolome
    assertEquals(levels.get(3).size(), 3);// ms measurements
    assertEquals(samples1.size(), 8);
    for (List<ISampleBean> l : levels) {
      System.out.println("level");
      System.out.println(l);
    }

    p.processTSV(threeGroupsAThree, new MetaboDesignReader(), false);
    assert (p.getSummary().size() == 4);
    levels = p.getProcessed();
    assertEquals(levels.get(0).size(), 3);// organism
    assertEquals(levels.get(1).size(), 3);// tissue
    assertEquals(levels.get(2).size(), 9);// metabolome
    assertEquals(levels.get(3).size(), 9);// ms measurements
    assertEquals(samples2.size(), 24);

    assert (p.getSpeciesSet().contains("E. coli"));
    assert (p.getTissueSet().contains("cell wall"));
    for (List<ISampleBean> l : levels) {
      System.out.println("level");
      System.out.println(l);
    }
  }

  @Test
  public void testGetGraphStructure() throws IOException, JAXBException {
    MetaboDesignReader r = new MetaboDesignReader();
    r.readSamples(standardtest, true);
    assertEquals(r.getGraphStructure(), null);
    // TODO
  }

  @Test
  public void testGetTSVByRows() throws IOException, JAXBException {
    MetaboDesignReader r = new MetaboDesignReader();
    r.readSamples(standardtest, true);
    assertEquals(null, r.getError());
    // for (String l : r.getTSVByRows()) {
    // System.out.println(l);
    // }
    assertEquals(3, r.getTSVByRows().size());
  }

  public void testGetSpeciesSet() throws IOException {
    MetaboDesignReader r = new MetaboDesignReader();
    r.readSamples(standardtest, false);
    assertEquals(r.getSpeciesSet(), new HashSet<String>(Arrays.asList("Homo Sapiens")));
  }

  public void testGetTissueSet() throws IOException {
    MetaboDesignReader r = new MetaboDesignReader();
    r.readSamples(standardtest, false);
    assertEquals(r.getTissueSet(), new HashSet<String>(Arrays.asList("Whole blood")));
  }

}
