package life.qbic.expdesign.io;


import static org.junit.Assert.assertEquals;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.junit.Before;
import org.junit.Test;
import life.qbic.datamodel.experiments.ExperimentType;
import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.expdesign.SamplePreparator;

public class MSDesignReaderTest {


  private File tsv = new File(getClass().getResource("ptx_example1.tsv").getFile());
  private File altTSV = new File(getClass().getResource("ptx_example_noParents.tsv").getFile());

  @Before
  public void setUp() {}

  @Test
  public void testCountEntities() throws IOException {
    MSDesignReader r = new MSDesignReader();
    assertEquals(r.countEntities(tsv), -1);// not implemented
  }

  @Test
  public void testReadSamples() throws IOException, JAXBException {
    MSDesignReader r = new MSDesignReader();
    List<ISampleBean> samples1 = r.readSamples(tsv, false);

    if (r.getError() != null)
      System.out.println(r.getError());

    r = new MSDesignReader();
    List<ISampleBean> samples2 = r.readSamples(tsv, true);

    assertEquals(samples1, samples2);

    SamplePreparator p = new SamplePreparator();
    p.processTSV(tsv, new MSDesignReader(), false);
    assert (p.getSummary().size() == 7);
    List<List<ISampleBean>> levels = p.getProcessed();
    assertEquals(levels.get(0).size(), 1);// organism
    assertEquals(levels.get(1).size(), 2);// tissue
    assertEquals(levels.get(2).size(), 4);// proteins
    assertEquals(levels.get(3).size(), 4);// peptides
    assertEquals(levels.get(4).size(), 4);// pool of peptides + fractions of peptides
    assertEquals(levels.get(5).size(), 4);// fractions of pool, pool of other peptide fractions
    assertEquals(levels.get(6).size(), 11);// ms measurements
    assertEquals(samples1.size(), 30);
    
    p.processTSV(altTSV, new MSDesignReader(), false);
    assert (p.getSummary().size() == 7);
    levels = p.getProcessed();
    assertEquals(levels.get(0).size(), 1);// organism
    assertEquals(levels.get(1).size(), 2);// tissue
    assertEquals(levels.get(2).size(), 4);// proteins
    assertEquals(levels.get(3).size(), 3);// peptides (unpooled samples) + protein pool
    assertEquals(levels.get(4).size(), 6);// pool of peptides + fractions of peptides
    assertEquals(levels.get(5).size(), 4);// fractions of pool, pool of other fractions
    assertEquals(levels.get(6).size(), 9);// ms measurements
    assertEquals(samples1.size(), 30);
    
    System.out.println(p.getExperimentalDesignProperties());
    System.out.println(p.getSpecialExperimentsOfTypeOrNull(ExperimentType.Q_MS_MEASUREMENT.toString()));
    System.out.println(p.getSpecialExperimentsOfTypeOrNull(ExperimentType.Q_SAMPLE_PREPARATION.toString()));
    
    assert(p.getSpeciesSet().contains("Homo sapiens"));
    assert(p.getTissueSet().contains("Liver"));
    assert(p.getTissueSet().contains("Whole blood"));
  }

  @Test
  public void testGetGraphStructure() throws IOException, JAXBException {
    MSDesignReader r = new MSDesignReader();
    r.readSamples(tsv, true);
    assertEquals(r.getGraphStructure(), null);
    // TODO
  }

  @Test
  public void testGetTSVByRows() throws IOException, JAXBException {
    MSDesignReader r = new MSDesignReader();
    r.readSamples(tsv, true);
    // for (String l : r.getTSVByRows()) {
    // System.out.println(l);
    // }
    assertEquals(r.getTSVByRows().size(), 12);
  }

  public void testGetSpeciesSet() throws IOException {
    MSDesignReader r = new MSDesignReader();
    r.readSamples(tsv, false);
    assertEquals(r.getSpeciesSet(), new HashSet<String>(Arrays.asList("Homo Sapiens")));
  }

  public void testGetTissueSet() throws IOException {
    MSDesignReader r = new MSDesignReader();
    r.readSamples(tsv, false);
    assertEquals(r.getTissueSet(), new HashSet<String>(Arrays.asList("Whole blood")));
  }

}
