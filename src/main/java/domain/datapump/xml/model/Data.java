//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.7-b41 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2020.08.25 at 07:49:05 PM BST
//


package domain.datapump.xml.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.lang.JAXBEqualsStrategy;
import org.jvnet.jaxb2_commons.lang.JAXBHashCodeStrategy;
import org.jvnet.jaxb2_commons.lang.ToString;
import org.jvnet.jaxb2_commons.lang.ToStringStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;
import domain.datapump.xml.model.strategy.SimpleToStringStrategy;


/**
 * <p>Java class for Data complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Data">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="AppointmentId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TimestampUtc" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Discipline" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Data", propOrder = {
        "appointmentId",
        "timestampUtc",
        "disciplines"
})
public class Data
        implements Serializable, Equals, HashCode, ToString
{

    private final static long serialVersionUID = -1L;
    @XmlElement(name = "AppointmentId")
    protected String appointmentId;
    @XmlElement(name = "TimestampUtc")
    protected String timestampUtc;
    @XmlElement(name = "Discipline")
    protected List<String> disciplines;

    /**
     * Gets the value of the appointmentId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getAppointmentId() {
        return appointmentId;
    }

    /**
     * Sets the value of the appointmentId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setAppointmentId(String value) {
        this.appointmentId = value;
    }

    /**
     * Gets the value of the timestampUtc property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getTimestampUtc() {
        return timestampUtc;
    }

    /**
     * Sets the value of the timestampUtc property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setTimestampUtc(String value) {
        this.timestampUtc = value;
    }

    /**
     * Gets the value of the disciplines property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the disciplines property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDisciplines().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    public List<String> getDisciplines() {
        if (disciplines == null) {
            disciplines = new ArrayList<String>();
        }
        return this.disciplines;
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof Data)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final Data that = ((Data) object);
        {
            String lhsAppointmentId;
            lhsAppointmentId = this.getAppointmentId();
            String rhsAppointmentId;
            rhsAppointmentId = that.getAppointmentId();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "appointmentId", lhsAppointmentId), LocatorUtils.property(thatLocator, "appointmentId", rhsAppointmentId), lhsAppointmentId, rhsAppointmentId)) {
                return false;
            }
        }
        {
            String lhsTimestampUtc;
            lhsTimestampUtc = this.getTimestampUtc();
            String rhsTimestampUtc;
            rhsTimestampUtc = that.getTimestampUtc();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "timestampUtc", lhsTimestampUtc), LocatorUtils.property(thatLocator, "timestampUtc", rhsTimestampUtc), lhsTimestampUtc, rhsTimestampUtc)) {
                return false;
            }
        }
        {
            List<String> lhsDisciplines;
            lhsDisciplines = (((this.disciplines!= null)&&(!this.disciplines.isEmpty()))?this.getDisciplines():null);
            List<String> rhsDisciplines;
            rhsDisciplines = (((that.disciplines!= null)&&(!that.disciplines.isEmpty()))?that.getDisciplines():null);
            if (!strategy.equals(LocatorUtils.property(thisLocator, "disciplines", lhsDisciplines), LocatorUtils.property(thatLocator, "disciplines", rhsDisciplines), lhsDisciplines, rhsDisciplines)) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object object) {
        final EqualsStrategy strategy = JAXBEqualsStrategy.INSTANCE;
        return equals(null, null, object, strategy);
    }

    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = 1;
        {
            String theAppointmentId;
            theAppointmentId = this.getAppointmentId();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "appointmentId", theAppointmentId), currentHashCode, theAppointmentId);
        }
        {
            String theTimestampUtc;
            theTimestampUtc = this.getTimestampUtc();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "timestampUtc", theTimestampUtc), currentHashCode, theTimestampUtc);
        }
        {
            List<String> theDisciplines;
            theDisciplines = (((this.disciplines!= null)&&(!this.disciplines.isEmpty()))?this.getDisciplines():null);
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "disciplines", theDisciplines), currentHashCode, theDisciplines);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = JAXBHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    public String toString() {
        final ToStringStrategy strategy = new SimpleToStringStrategy();
        final StringBuilder buffer = new StringBuilder();
        append(null, buffer, strategy);
        return buffer.toString();
    }

    public StringBuilder append(ObjectLocator locator, StringBuilder buffer, ToStringStrategy strategy) {
        strategy.appendStart(locator, this, buffer);
        appendFields(locator, buffer, strategy);
        strategy.appendEnd(locator, this, buffer);
        return buffer;
    }

    public StringBuilder appendFields(ObjectLocator locator, StringBuilder buffer, ToStringStrategy strategy) {
        {
            String theAppointmentId;
            theAppointmentId = this.getAppointmentId();
            strategy.appendField(locator, this, "appointmentId", buffer, theAppointmentId);
        }
        {
            String theTimestampUtc;
            theTimestampUtc = this.getTimestampUtc();
            strategy.appendField(locator, this, "timestampUtc", buffer, theTimestampUtc);
        }
        {
            List<String> theDisciplines;
            theDisciplines = (((this.disciplines!= null)&&(!this.disciplines.isEmpty()))?this.getDisciplines():null);
            strategy.appendField(locator, this, "disciplines", buffer, theDisciplines);
        }
        return buffer;
    }

}

