package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetabolitePrepProperties {

  private String harvestingConditions;
  private List<String> cellLysisTypes;
  private String lysisParameters;

  public MetabolitePrepProperties(String harvestingConditions, List<String> lysisList,
      String lysisParams) {
    super();
    this.harvestingConditions = harvestingConditions;
    this.cellLysisTypes = lysisList;
    this.lysisParameters = lysisParams;
  }

  public String getHarvestingConditions() {
    return harvestingConditions;
  }

  public List<String> getCellLysisTypes() {
    return cellLysisTypes;
  }

  public String getLysisParameters() {
    return lysisParameters;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cellLysisTypes == null) ? 0 : cellLysisTypes.hashCode());
    result = prime * result + ((lysisParameters == null) ? 0 : lysisParameters.hashCode());
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
      res.put("Q_CELL_HARVESTING_METHOD", harvestingConditions);
    }
    if (cellLysisTypes != null && !cellLysisTypes.isEmpty()) {
      res.put("Q_CELL_LYSIS_METHOD", cellLysisTypes);
    }
    if (lysisParameters != null && !lysisParameters.isEmpty()) {
      res.put("Q_CELL_LYSIS_PARAMETERS", lysisParameters);
    }
    System.err.println(res);
    return res;
  }

}
