package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetabolitePrepProperties {

  private String harvestingMethod;
  private String harvestingVolume;
  private List<String> cellLysisTypes;
  private String lysisParameters;

  public MetabolitePrepProperties(String harvestingConditions, String harvestingVolume,
      List<String> lysisList, String lysisParams) {
    super();
    this.harvestingMethod = harvestingConditions;
    this.harvestingVolume = harvestingVolume;
    this.cellLysisTypes = lysisList;
    this.lysisParameters = lysisParams;
  }

  public String getHarvestingConditions() {
    return harvestingMethod;
  }

  public List<String> getCellLysisTypes() {
    return cellLysisTypes;
  }

  public String getLysisParameters() {
    return lysisParameters;
  }

  public String getHarvestingVolume() {
    return harvestingVolume;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cellLysisTypes == null) ? 0 : cellLysisTypes.hashCode());
    result = prime * result + ((lysisParameters == null) ? 0 : lysisParameters.hashCode());
    result = prime * result + ((harvestingMethod == null) ? 0 : harvestingMethod.hashCode());
    result = prime * result + ((harvestingVolume == null) ? 0 : harvestingVolume.hashCode());
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
    if (cellLysisTypes == null) {
      if (other.cellLysisTypes != null)
        return false;
    } else if (!cellLysisTypes.equals(other.cellLysisTypes))
      return false;
    if (lysisParameters == null) {
      if (other.lysisParameters != null)
        return false;
    } else if (!lysisParameters.equals(other.lysisParameters))
      return false;
    if (harvestingMethod == null) {
      if (other.harvestingMethod != null)
        return false;
    } else if (!harvestingMethod.equals(other.harvestingMethod))
      return false;
    if (harvestingVolume == null) {
      if (other.harvestingVolume != null)
        return false;
    } else if (!harvestingVolume.equals(other.harvestingVolume))
      return false;
    return true;
  }

  public Map<String, Object> getPropertyMap() {
    Map<String, Object> res = new HashMap<String, Object>();

    if (harvestingMethod != null && !harvestingMethod.isEmpty()) {
      res.put("Q_CELL_HARVESTING_METHOD", harvestingMethod);
    }
    if (cellLysisTypes != null && !cellLysisTypes.isEmpty()) {
      res.put("Q_CELL_LYSIS_METHOD", cellLysisTypes);
    }
    if (lysisParameters != null && !lysisParameters.isEmpty()) {
      res.put("Q_CELL_LYSIS_PARAMETERS", lysisParameters);
    }
    if (harvestingVolume != null && !harvestingVolume.isEmpty()) {
      res.put("Q_CELL_HARVESTING_VOLUME", harvestingVolume);
    }
    return res;
  }

}
