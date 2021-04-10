package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetabolitePrepProperties {

  private String cultureMedium;
  private String cultureType;
  private String harvestingConditions;
  private String cellLysis;

  public MetabolitePrepProperties(String cultureMedium, String cultureType,
      String harvestingConditions, String cellLysis) {
    super();
    this.cultureMedium = cultureMedium;
    this.cultureType = cultureType;
    this.harvestingConditions = harvestingConditions;
    this.cellLysis = cellLysis;
  }

  public String getCultureMedium() {
    return cultureMedium;
  }

  public String getCultureType() {
    return cultureType;
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
    result = prime * result + ((cultureMedium == null) ? 0 : cultureMedium.hashCode());
    result = prime * result + ((cultureType == null) ? 0 : cultureType.hashCode());
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
    if (cultureMedium == null) {
      if (other.cultureMedium != null)
        return false;
    } else if (!cultureMedium.equals(other.cultureMedium))
      return false;
    if (cultureType == null) {
      if (other.cultureType != null)
        return false;
    } else if (!cultureType.equals(other.cultureType))
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
    
    if (cultureMedium != null && !cultureMedium.isEmpty()) {
      res.put("Q_CULTURE_MEDIUM", cultureMedium);
    }
    if (cultureType != null && !cultureType.isEmpty()) {
      res.put("Q_CULTURE_TYPE", cultureType);
    }
    if (harvestingConditions != null && !harvestingConditions.isEmpty()) {
      res.put("Q_HARVESTING_CONDITIONS", harvestingConditions);
    }
    if (cellLysis != null && !cellLysis.isEmpty()) {
      res.put("Q_CELL_LYSIS", cellLysis);
    }
    return res;
  }

}
