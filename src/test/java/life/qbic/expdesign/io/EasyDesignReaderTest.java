package life.qbic.expdesign.io;


import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

import life.qbic.datamodel.samples.ISampleBean;
import life.qbic.datamodel.samples.SampleSummary;

public class EasyDesignReaderTest {

  private File tsv = new File(getClass().getResource("easy.tsv").getFile());

  @Before
  public void setUp() {}

  @Test
  public void testCountEntities() throws IOException {
    EasyDesignReader r = new EasyDesignReader();
    assertEquals(r.countEntities(tsv), 10);
  }

  @Test
  public void testReadSamples() throws IOException, JAXBException {
    EasyDesignReader r = new EasyDesignReader();
    List<ISampleBean> samples1 = r.readSamples(tsv, false);

    r = new EasyDesignReader();
    List<ISampleBean> samples2 = r.readSamples(tsv, true);

    assertEquals(samples1, samples2);

    assertEquals(samples1.size(), 10);
  }

  @Test
  public void testGetGraphStructure() throws IOException, JAXBException {
    EasyDesignReader r = new EasyDesignReader();
    r.readSamples(tsv, true);
    Map<String, List<SampleSummary>> factorsToSamples = r.getGraphStructure().getFactorsToSamples();
    assert (!factorsToSamples.isEmpty());

    assert (factorsToSamples.containsKey("origin"));

    r = new EasyDesignReader();
    r.readSamples(tsv, false);
    assert (r.getGraphStructure().getFactorsToSamples().isEmpty());
  }

  @Test
  public void testGetTSVByRows() throws IOException, JAXBException {
    EasyDesignReader r = new EasyDesignReader();
    r.readSamples(tsv, true);
    assertEquals(r.getTSVByRows().size(), 5);
  }
}
