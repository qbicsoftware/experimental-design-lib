package life.qbic.expdesign.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import life.qbic.xml.properties.Property;
import life.qbic.xml.study.Qproperty;

public class ExperimentalDesignPropertyWrapper {

  private Map<String, Map<Pair<String, String>, List<String>>> experimentalDesign;
  private Map<String, List<Qproperty>> properties = new HashMap<String, List<Qproperty>>();

  public ExperimentalDesignPropertyWrapper(
      Map<String, Map<Pair<String, String>, List<String>>> experimentalDesign,
      Map<String, List<Property>> properties) {
    super();
    this.experimentalDesign = experimentalDesign;
    this.properties = translateOldProperties(properties);
  }

  private Map<String, List<Qproperty>> translateOldProperties(Map<String, List<Property>> props) {
    Map<String, List<Qproperty>> res = new HashMap<String, List<Qproperty>>();
    for (String id : props.keySet()) {
      List<Qproperty> newProps = new ArrayList<Qproperty>();
      for (Property p : props.get(id)) {
        Qproperty newProp = null;
        if (p.hasUnit()) {
          newProp = new Qproperty(id, p.getLabel(), p.getValue(), p.getUnit());
        } else {
          newProp = new Qproperty(id, p.getLabel(), p.getValue());
        }
        newProps.add(newProp);
      }
      res.put(id, newProps);
    }
    return res;
  }

  public Map<String, Map<Pair<String, String>, List<String>>> getExperimentalDesign() {
    return experimentalDesign;
  }

  public void setExperimentalDesign(
      Map<String, Map<Pair<String, String>, List<String>>> experimentalDesign) {
    this.experimentalDesign = experimentalDesign;
  }

  public Map<String, List<Qproperty>> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, List<Property>> properties) {
    this.properties = translateOldProperties(properties);
  }

}
