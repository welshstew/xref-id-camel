package com.nullendpoint;
import java.util.ArrayList;
import java.util.List;


public class Relation {

	private Integer id;
    private String commonID;
    private List<Reference> references;

    public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	/**
     * Gets the value of the commonID property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCommonID() {
        return commonID;
    }

    /**
     * Sets the value of the commonID property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCommonID(String value) {
        this.commonID = value;
    }

    /**
     * Gets the value of the reference property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reference property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReference().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Relation.Reference }
     *
     *
     */
    public List<Reference> getReferences() {
        if (references == null) {
            references = new ArrayList<Reference>();
        }
        return this.references;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Endpoint" type="{http://www.w3.org/2001/XMLSchema}string"/>
     *         &lt;element name="Id" type="{http://www.w3.org/2001/XMLSchema}int"/>
     *       &lt;/sequence>
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */


public static class Reference {

    	private Integer id;
        private String endpoint;
        private String endpointId;


        public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		/**
         * Gets the value of the endpoint property.
         *
         * @return
         *     possible object is
         *     {@link String }
         *
         */
        public String getEndpoint() {
            return endpoint;
        }

        /**
         * Sets the value of the endpoint property.
         *
         * @param value
         *     allowed object is
         *     {@link String }
         *
         */
        public void setEndpoint(String value) {
            this.endpoint = value;
        }

        /**
         * Gets the value of the id property.
         *
         */
        public String getEndpointId() {
            return endpointId;
        }

        /**
         * Sets the value of the id property.
         *
         */
        public void setEndpointId(String value) {
            this.endpointId = value;
        }

    }

}
