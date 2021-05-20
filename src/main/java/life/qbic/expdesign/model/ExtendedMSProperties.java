package life.qbic.expdesign.model;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExtendedMSProperties {

  private final Logger logger = LogManager.getLogger(ExtendedMSProperties.class);

  private String lcmsMethod;
  private String msDevice;
  private String lcmsDescription;
  private String chromatographyType;
  private String columnName;
  private String lcDevice;
  private String lcDetectionMethod;
  private String ionizationMode;
  private String washingSolvent;
  private String additionalInformation;
  private String dissociationMethod;
  private double dissociationEnergy;
  private String massResolvingPower;

  public ExtendedMSProperties(String lcmsMethod, String msDevice) {
    super();
    this.lcmsMethod = lcmsMethod;
    this.msDevice = msDevice;
    this.dissociationEnergy = -1;
  }

  public String getLcmsMethod() {
    return lcmsMethod;
  }

  public void setLcmsMethod(String lcmsMethod) {
    this.lcmsMethod = lcmsMethod;
  }

  public String getMSDevice() {
    return msDevice;
  }

  public void setDevice(String device) {
    this.msDevice = device;
  }

  public String getLCDevice() {
    return lcDevice;
  }

  public void setLCDevice(String device) {
    this.lcDevice = device;
  }

  public String getLCMSDescription() {
    return lcmsDescription;
  }

  public void setLCMSDescription(String lcmsDescription) {
    this.lcmsDescription = lcmsDescription;
  }

  public String getChromatographyType() {
    return chromatographyType;
  }

  public void setChromatographyType(String chromatographyType) {
    this.chromatographyType = chromatographyType;
  }

  public String getColumnName() {
    return columnName;
  }

  public void setColumnName(String columnName) {
    this.columnName = columnName;
    matchChromatographyType(columnName);
  }

  private void matchChromatographyType(String columnName) {
    Map<String, String> colNamesToChromatographyTypes = new HashMap<>();
    colNamesToChromatographyTypes.put("C18 Gemini", "RP_HPLC_C18_COLUMN");
    colNamesToChromatographyTypes.put("HILIC", "HILIC");
    for (String key : colNamesToChromatographyTypes.keySet()) {
      if (columnName.contains(key)) {
        String value = colNamesToChromatographyTypes.get(key);
        logger.info("Setting chromatography type for " + columnName + " column to " + value);
        this.chromatographyType = value;
        return;
      }
    }
    logger.info("Chromatography type could not be determined for column " + columnName);
  }

  public String getLCDetectionMethod() {
    return lcDetectionMethod;
  }

  public void setLCDetectionMethod(String lcDetectionMethod) {
    this.lcDetectionMethod = lcDetectionMethod;
  }

  public String getIonizationMode() {
    return ionizationMode;
  }

  public void setIonizationMode(String ionizationMode) {
    this.ionizationMode = ionizationMode;
  }

  public String getWashingSolvent() {
    return washingSolvent;
  }

  public void setWashingSolvent(String washingSolvent) {
    this.washingSolvent = washingSolvent;
  }

  public Map<String, Object> getPropertyMap() {
    Map<String, Object> res = new HashMap<String, Object>();
    if (msDevice != null)
      res.put("Q_MS_DEVICE", msDevice);
    if (lcDevice != null)
      res.put("Q_LC_DEVICE", lcDevice);
    if (lcmsMethod != null)
      res.put("Q_MS_LCMS_METHOD", lcmsMethod);
    if (chromatographyType != null)
      res.put("Q_CHROMATOGRAPHY_TYPE", chromatographyType);
    if (lcmsDescription != null && !lcmsDescription.isEmpty())
      res.put("Q_MS_LCMS_METHOD_INFO", lcmsDescription);
    if (!ionizationMode.isEmpty())
      res.put("Q_IONIZATION_MODE", ionizationMode);
    if (!lcDetectionMethod.isEmpty())
      res.put("Q_LC_DETECTION_METHOD", lcDetectionMethod);
    if (!columnName.isEmpty())
      res.put("Q_CHROMATOGRAPHY_COLUMN_NAME", columnName);
    if (!washingSolvent.isEmpty())
      res.put("Q_WASHING_SOLVENT", washingSolvent);
    if (additionalInformation != null && !additionalInformation.isEmpty())
      res.put("Q_ADDITIONAL_INFO", additionalInformation);
    if (dissociationEnergy > 0)
      res.put("Q_MS_DISSOCIATION_ENERGY", dissociationEnergy);
    if (dissociationMethod != null && !dissociationMethod.isEmpty())
      res.put("Q_MS_DISSOCIATION_METHOD", dissociationMethod);
    if (massResolvingPower != null && !massResolvingPower.isEmpty())
      res.put("Q_MS_RESOLVING_POWER", massResolvingPower);
    return res;
  }

  public void setDissociationMethod(String dissociationMethod) {
    this.dissociationMethod = dissociationMethod;
  }

  public void setDissociationEnergy(double dissociationEnergy2) {
    this.dissociationEnergy = dissociationEnergy2;
  }

  public void setMassResolvingPower(String massResolvingPower) {
    this.massResolvingPower = massResolvingPower;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((additionalInformation == null) ? 0 : additionalInformation.hashCode());
    result = prime * result + ((chromatographyType == null) ? 0 : chromatographyType.hashCode());
    result = prime * result + ((columnName == null) ? 0 : columnName.hashCode());
    long temp;
    temp = Double.doubleToLongBits(dissociationEnergy);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((dissociationMethod == null) ? 0 : dissociationMethod.hashCode());
    result = prime * result + ((ionizationMode == null) ? 0 : ionizationMode.hashCode());
    result = prime * result + ((lcDetectionMethod == null) ? 0 : lcDetectionMethod.hashCode());
    result = prime * result + ((lcDevice == null) ? 0 : lcDevice.hashCode());
    result = prime * result + ((lcmsDescription == null) ? 0 : lcmsDescription.hashCode());
    result = prime * result + ((lcmsMethod == null) ? 0 : lcmsMethod.hashCode());
    result = prime * result + ((massResolvingPower == null) ? 0 : massResolvingPower.hashCode());
    result = prime * result + ((msDevice == null) ? 0 : msDevice.hashCode());
    result = prime * result + ((washingSolvent == null) ? 0 : washingSolvent.hashCode());
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
    ExtendedMSProperties other = (ExtendedMSProperties) obj;
    if (additionalInformation == null) {
      if (other.additionalInformation != null)
        return false;
    } else if (!additionalInformation.equals(other.additionalInformation))
      return false;
    if (chromatographyType == null) {
      if (other.chromatographyType != null)
        return false;
    } else if (!chromatographyType.equals(other.chromatographyType))
      return false;
    if (columnName == null) {
      if (other.columnName != null)
        return false;
    } else if (!columnName.equals(other.columnName))
      return false;
    if (Double.doubleToLongBits(dissociationEnergy) != Double
        .doubleToLongBits(other.dissociationEnergy))
      return false;
    if (dissociationMethod == null) {
      if (other.dissociationMethod != null)
        return false;
    } else if (!dissociationMethod.equals(other.dissociationMethod))
      return false;
    if (ionizationMode == null) {
      if (other.ionizationMode != null)
        return false;
    } else if (!ionizationMode.equals(other.ionizationMode))
      return false;
    if (lcDetectionMethod == null) {
      if (other.lcDetectionMethod != null)
        return false;
    } else if (!lcDetectionMethod.equals(other.lcDetectionMethod))
      return false;
    if (lcDevice == null) {
      if (other.lcDevice != null)
        return false;
    } else if (!lcDevice.equals(other.lcDevice))
      return false;
    if (lcmsDescription == null) {
      if (other.lcmsDescription != null)
        return false;
    } else if (!lcmsDescription.equals(other.lcmsDescription))
      return false;
    if (lcmsMethod == null) {
      if (other.lcmsMethod != null)
        return false;
    } else if (!lcmsMethod.equals(other.lcmsMethod))
      return false;
    if (massResolvingPower == null) {
      if (other.massResolvingPower != null)
        return false;
    } else if (!massResolvingPower.equals(other.massResolvingPower))
      return false;
    if (msDevice == null) {
      if (other.msDevice != null)
        return false;
    } else if (!msDevice.equals(other.msDevice))
      return false;
    if (washingSolvent == null) {
      if (other.washingSolvent != null)
        return false;
    } else if (!washingSolvent.equals(other.washingSolvent))
      return false;
    return true;
  }

  public void setAdditionalInformation(String info) {
    this.additionalInformation = info;
  }

}
