package life.qbic.expdesign.io;


import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;

import life.qbic.datamodel.samples.ISampleBean;

public class QBiCDesignReaderTest {

  private File tsv = new File(getClass().getResource("internal.tsv").getFile());

  @Before
  public void setUp() {}


  // public void testGetDescription() {
  // return description;
  // }
  //
  // public void testGetInvestigator() {
  // return investigator;
  // }
  //
  // public void testGetContact() {
  // return contact;
  // }
  //
  // public void testGetSecondaryName() {
  // return secondaryName;
  // }
  //
  // public void testIsPilot() {
  // return isPilot;
  // }
  //
  // public void testGetManager() {
  // return manager;
  // }
  //
  // public void testGetTechnologyTypes() {
  // return technologyTypes;
  // }
  //
  // public void testGetSpace() {
  // return space;
  // }
  //
  // public void testGetProject() {
  // return project;
  // }

  @Test
  public void testCountEntities() throws IOException {
    QBiCDesignReader r = new QBiCDesignReader();
    assertEquals(r.countEntities(tsv), 10);
  }

  @Test
  public void testReadSamples() throws IOException, JAXBException {
    QBiCDesignReader r = new QBiCDesignReader();
    List<ISampleBean> samples1 = r.readSamples(tsv, false);

    r = new QBiCDesignReader();
    List<ISampleBean> samples2 = r.readSamples(tsv, true);

    assertEquals(samples1, samples2);
    assertEquals(samples1.size(), 10);
  }

  @Test
  public void testGetGraphStructure() throws IOException, JAXBException {
    QBiCDesignReader r = new QBiCDesignReader();
    r.readSamples(tsv, true);
    assertEquals(r.getGraphStructure(), null);

    // TODO
  }

  @Test
  public void testGetTSVByRows() throws IOException, JAXBException {
    QBiCDesignReader r = new QBiCDesignReader();
    r.readSamples(tsv, true);
    assertEquals(r.getTSVByRows().size(), 11);
  }

}
