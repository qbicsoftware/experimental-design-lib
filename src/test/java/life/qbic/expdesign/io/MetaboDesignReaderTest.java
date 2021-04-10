package life.qbic.expdesign.io;


import static org.junit.Assert.assertEquals;
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
import org.junit.Before;
import org.junit.Test;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleType;
import life.qbic.expdesign.SamplePreparator;

public class MetaboDesignReaderTest {

  private File fullExample1 =
      new File(getClass().getResource("mtx/metabo_full_example1.tsv").getFile());
  private File standardtest = new File(getClass().getResource("mtx/metabo_small_test.tsv").getFile());


  @Before
  public void setUp() {}

  @Test
  public void testExperimentMetadata() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(fullExample1, new MetaboDesignReader(), false);
    System.out.println("ERROR? " + p.getError());


    Map<String, Object> tissuePrepProps = new HashMap<>();
    tissuePrepProps.put("Q_CULTURE_MEDIUM", "MH");
    tissuePrepProps.put("Q_CULTURE_TYPE", "solid");
    tissuePrepProps.put("Q_HARVESTING_CONDITIONS", "centrifugation");
    tissuePrepProps.put("Q_CELL_LYSIS", "boiling");

    Map<String, Object> msProps = new HashMap<>();
    msProps.put("Q_MS_LCMS_METHOD", "1_D_M_ZIC_pHILIC_neg_85-905");
    msProps.put("Q_MS_DEVICE", "HFX");
    msProps.put("Q_LC_DEVICE", "Dionex");
    msProps.put("Q_IONIZATION_MODE", "negative");
    msProps.put("Q_CHROMATOGRAPHY_COLUMN_NAME", "SeQuant ZIC-pHILIC");
//    msProps.put("Q_MS_LCMS_METHOD_INFO", "UV");
    msProps.put("Q_LC_DETECTION_METHOD", "UV");

    ExperimentType msType = ExperimentType.Q_MS_MEASUREMENT;
    // ExperimentType prepType = ExperimentType.Q_SAMPLE_PREPARATION;
    ExperimentType extrType = ExperimentType.Q_SAMPLE_EXTRACTION;
    Map<String, Map<String, Object>> extrExps =
        p.transformAndReturnSpecialExperimentsOfTypeOrNull(extrType.toString());
    // Map<String, Map<String, Object>> prepExps =
    // p.transformAndReturnSpecialExperimentsOfTypeOrNull(prepType.toString());
    Map<String, Map<String, Object>> msExps =
        p.transformAndReturnSpecialExperimentsOfTypeOrNull(msType.toString());
    for (Entry<String, Object> entry : tissuePrepProps.entrySet()) {
      assert (searchExperimentsForProperty(extrExps, extrType, entry));
    }
    // for (Entry<String, Object> entry : samplePrepProps.entrySet()) {
    // assert (searchExperimentsForProperty(prepExps, prepType, entry));
    // }
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
        if (experiment.get(entry.getKey()).equals(entry.getValue())) {
          return true;
        }
      }
    }
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
  public void testSampleMetadata() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    p.processTSV(fullExample1, new MetaboDesignReader(), false);
    List<List<ISampleBean>> processed = p.getProcessed();
    Map<String, String> entityMetadata = new HashMap<>();
    entityMetadata.put("Q_NCBI_ORGANISM", "E. coli");
    entityMetadata.put("Q_STRAIN_LAB_COLLECTION_NUMBER", "12345");

    Map<String, String> tissueMetadata = new HashMap<>();
    tissueMetadata.put("Q_PRIMARY_TISSUE", "Whole organism");
    tissueMetadata.put("Q_TISSUE_DETAILED", "cell wall");

    Map<String, String> molMetadata = new HashMap<>();
    molMetadata.put("Q_SAMPLE_TYPE", "SMALLMOLECULES");
    molMetadata.put("Q_SAMPLE_SOLVENT", "beer");
    molMetadata.put("Q_WASHING_SOLVENT", "alcohol");

    Map<String, String> msRunMetadata = new HashMap<>();
    msRunMetadata.put("Q_INJECTION_VOLUME", "10");

    for (Entry<String, String> entry : entityMetadata.entrySet()) {
      assert (searchSamplesForProperty(processed, SampleType.Q_BIOLOGICAL_ENTITY, entry));
    }
    for (Entry<String, String> entry : tissueMetadata.entrySet()) {
      assert (searchSamplesForProperty(processed, SampleType.Q_BIOLOGICAL_SAMPLE, entry));
    }
    for (Entry<String, String> entry : molMetadata.entrySet()) {
      assert (searchSamplesForProperty(processed, SampleType.Q_TEST_SAMPLE, entry));
    }
    System.err.println(msRunMetadata);
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
  public void testGetVocabularyValues() throws IOException, JAXBException {
    SamplePreparator p = new SamplePreparator();
    MetaboDesignReader r = new MetaboDesignReader();
    p.processTSV(fullExample1, r, false);
    System.out.println("vocabs");
    System.out.println(p.getParsedCategoriesToValues(new ArrayList<String>(
        Arrays.asList("Sample type", "Mediumn", "Harvesting conditions", "LCMS Method Name",
            "LC Device", "LC detection method", "MS Device", "MS ion mode", "Species", "Tissue"))));

    if (r.getError() != null)
      System.out.println(r.getError());
    assertNull(r.getError());
  }

  // @Test
  // public void testReadSamples() throws IOException, JAXBException {
  // MSDesignReader r = new MSDesignReader();
  //
  // List<ISampleBean> samples1 = r.readSamples(tsv, false);
  //
  // if (r.getError() != null)
  // System.err.println("2" + r.getError());
  //
  // r = new MSDesignReader();
  // List<ISampleBean> samples2 = r.readSamples(tsv, true);
  //
  // assertEquals(samples1, samples2);
  //
  // SamplePreparator p = new SamplePreparator();
  // p.processTSV(tsv, new MSDesignReader(), false);
  // System.err.print(p.getSummary());
  // assert (p.getSummary().size() == 7);
  // List<List<ISampleBean>> levels = p.getProcessed();
  // assertEquals(levels.get(0).size(), 1);// organism
  // assertEquals(levels.get(1).size(), 2);// tissue
  // assertEquals(levels.get(2).size(), 6);// proteins
  // assertEquals(levels.get(3).size(), 5);// peptides
  // assertEquals(levels.get(4).size(), 6);// pool of peptides + fractions of peptides
  // assertEquals(levels.get(5).size(), 4);// fractions of pool, pool of other peptide fractions
  // assertEquals(levels.get(6).size(), 11);// ms measurements
  // assertEquals(samples1.size(), 35);
  // for (List<ISampleBean> l : levels) {
  // System.out.println("level");
  // System.out.println(l);
  // }
  //
  // p.processTSV(altTSV, new MSDesignReader(), false);
  // assert (p.getSummary().size() == 7);
  // levels = p.getProcessed();
  // assertEquals(levels.get(0).size(), 1);// organism
  // assertEquals(levels.get(1).size(), 2);// tissue
  // assertEquals(levels.get(2).size(), 4);// proteins
  // assertEquals(levels.get(3).size(), 3);// peptides (unpooled samples) + protein pool
  // assertEquals(levels.get(4).size(), 6);// pool of peptides + fractions of peptides
  // assertEquals(levels.get(5).size(), 4);// fractions of pool, pool of other fractions
  // assertEquals(levels.get(6).size(), 9);// ms measurements
  // assertEquals(samples1.size(), 35);
  //
  // assert (p.getSpeciesSet().contains("Homo sapiens"));
  // assert (p.getTissueSet().contains("Liver"));
  // assert (p.getTissueSet().contains("Whole blood"));
  // }

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
    // for (String l : r.getTSVByRows()) {
    // System.out.println(l);
    // }
    assertEquals(r.getTSVByRows().size(), 14);
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
