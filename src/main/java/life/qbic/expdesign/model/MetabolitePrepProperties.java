package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.Map;

public class MetabolitePrepProperties {

  private String harvestingConditions;
  private String cellLysis;

  public MetabolitePrepProperties(String harvestingConditions, String cellLysis) {
    super();
    this.harvestingConditions = harvestingConditions;
    this.cellLysis = cellLysis;
  }

  public String getHarvestingConditions() {
    return harvestingConditions;
  }

  public String getCellLysis() {
    return cellLysis;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cellLysis == null) ? 0 : cellLysis.hashCode());
    result =
        prime * result + ((harvestingConditions == null) ? 0 : harvestingConditions.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MetabolitePrepProperties other = (MetabolitePrepProperties) obj;
    if (cellLysis == null) {
      if (other.cellLysis != null)
        return false;
    } else if (!cellLysis.equals(other.cellLysis))
      return false;
    if (harvestingConditions == null) {
      if (other.harvestingConditions != null)
        return false;
    } else if (!harvestingConditions.equals(other.harvestingConditions))
      return false;
    return true;
  }

  public Map<String, Object> getPropertyMap() {
    Map<String, Object> res = new HashMap<String, Object>();

    if (harvestingConditions != null && !harvestingConditions.isEmpty()) {
      res.put("Q_HARVESTING_CONDITIONS", harvestingConditions);
    }
    if (cellLysis != null && !cellLysis.isEmpty()) {
      res.put("Q_CELL_LYSIS", cellLysis);
    }
    return res;
  }

}
