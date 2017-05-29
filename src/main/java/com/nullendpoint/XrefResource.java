//
//package com.nullendpoint;
//
//import org.apache.log4j.Logger;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.datasource.SingleConnectionDataSource;
//import org.springframework.stereotype.Component;
//
//import javax.sql.DataSource;
//import javax.ws.rs.*;
//import javax.ws.rs.core.Response;
//import javax.ws.rs.core.Response.Status;
//
//@Component
//public class XrefResource {
//
//	private static final Logger log = Logger.getLogger(XrefResource.class);
//	private JDBCXrefOperation xrefOperation = new JDBCXrefOperation(jdbcTemplate);
//
//
//    public XrefResource() {
//    	try {
//        	DataSource dataSource = new SingleConnectionDataSource("jdbc:mysql://localhost:3306/xref",
//          	      "root",
//          	      "sqladmin1!",
//          	      false);
//
//          	JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//          	xrefOperation.setJdbcTemplate(jdbcTemplate);
//		} catch (Exception e) {
//			log.error("Could not create Datasource. "+e.getMessage(), e);
//		}
//    }
//
//    /**
//     * Returns the usage of the API
//     *
//     */
//    @GET
//    @Produces({
//        "text/plain"
//    })
//    public Response getUsage() {
//    	return Response.ok("Xref Service is operational.").build();
//    }
//
//    /**
//     * List the Entity Sets for this Tenant
//     *
//     * @param tenant
//     *
//     */
//    @GET
//    @Path("{tenant}")
//    @Produces({
//        "text/plain"
//    })
//    public Response getEntitySetsByTenant(
//        @PathParam("tenant")
//        String tenant) {
//    	return Response.ok("Working with Tenant "+tenant+". This endpoint performs no operations.").build();
//    }
//
//    /**
//     * Find a Relation based on an Endpoint (eg. Salesforce) and System ID (eg. 11111).
//     * If no query parameters are provided then the metadata about this Entity Set
//     * is returned showing the count of Relations and the Endpoints (eg. Salesforce, SAP, etc)
//     *
//     * @param id
//     *     Filter the Relations based on their ID e.g. 11111
//     * @param tenant
//     *
//     * @param entitySet
//     *
//     * @param endpoint
//     *     Filter the Relations based on the Endpoints e.g. salesforce
//     */
//    @GET
//    @Path("{tenant}/{entitySet}")
//    @Produces({
//        "application/xml"
//    })
//    public Response findRelation(
//        @PathParam("entitySet")
//        String entitySet,
//        @PathParam("tenant")
//        String tenant,
//        @QueryParam("endpoint")
//        String endpoint,
//        @QueryParam("id")
//        String id) {
//
//    	try {
//	    	Relation relation = xrefOperation.findRelation(entitySet, tenant, endpoint, id);
//	    	return Response.ok(relation).build();
//    	} catch (EntityNotFoundException e) {
//    		log.error(e.getMessage(), e);
//    		return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
//    	}
//    }
//
//    /**
//     * Create a new Relation for this Entity Set
//     *
//     * @param tenant
//     *
//     * @param entitySet
//     *
//     * @param entity
//     *      e.g. <Relation>
//     *          <Reference>
//     *             <Endpoint>SAP</Endpoint><Id>111111</Id>
//     *          </Reference>
//     *     </Relation>
//     *
//     */
//    @POST
//    @Path("{tenant}/{entitySet}")
//    @Consumes("application/xml")
//    @Produces({
//        "application/xml"
//    })
//    public Response createRelation(
//        @PathParam("entitySet")
//        String entitySet,
//        @PathParam("tenant")
//        String tenant, Relation relation) {
//
//		try {
//			relation = xrefOperation.createRelation(entitySet, tenant, relation);
//			return Response.ok(relation).build();
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//			return Response.serverError().entity(e.getMessage()).build();
//		}
// }
//
//    /**
//     * Update a Relation to add or modify a Reference. If the Reference Endpoint exists, it will overwrite what exists. If it does not exist it will create a new Reference
//     *
//     * @param tenant
//     *
//     * @param entitySet
//     *
//     * @param entity
//     *      e.g. <Relation>
//     *       <CommonId>55f623ace41d5524d0000001</CommonId>
//     *       <Reference>
//     *         <Endpoint>salesforce</Endpoint>
//     *         <Id>222222</Id>
//     *       </Reference>
//     *     </Relation>
//     *
//     */
//    @PUT
//    @Path("{tenant}/{entitySet}")
//    @Consumes("application/xml")
//    @Produces({
//        "application/xml"
//    })
//    public Response updateRelation(
//        @PathParam("entitySet")
//        String entitySet,
//        @PathParam("tenant")
//        String tenant, Relation relation) {
//
//		try {
//			relation = xrefOperation.updateRelation(entitySet, tenant, relation);
//			return Response.ok(relation).build();
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//			return Response.serverError().entity(e.getMessage()).build();
//		}
//
//    }
//
//    /**
//     * Retrieves a specific Relation identified by this Common ID
//     *
//     * @param tenant
//     *
//     * @param entitySet
//     *
//     * @param commonId
//     *
//     */
//    @GET
//    @Path("{tenant}/{entitySet}/{commonId}")
//    @Produces({
//        "application/xml"
//    })
//    public Response findRelationByCommonId(
//        @PathParam("commonId")
//        String commonId,
//        @PathParam("entitySet")
//        String entitySet,
//        @PathParam("tenant")
//        String tenant) {
//
//    	try {
//	    	Relation relation = xrefOperation.findRelationByCommonId(commonId, entitySet, tenant);
//	    	return Response.ok(relation).build();
//    	} catch (EntityNotFoundException e) {
//    		log.error(e.getMessage(), e);
//    		return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
//    	}
//    }
//
//    /**
//     * Deletes a Reference identified by this Endpoint name from the Relation identified by this Common ID. Eg. http://localhost:27017/xref/viva/customer/55f623ace41d5524d0000001/SAP - note that this is case sensitive and "sap" is not the same endpoint.
//     *
//     * @param tenant
//     *
//     * @param entitySet
//     *
//     * @param commonId
//     *
//     * @param endpoint
//     *
//     */
//    @DELETE
//    @Path("{tenant}/{entitySet}/{commonId}/{endpoint}/{endpointId}")
//    @Produces({
//        "application/xml"
//    })
//    public Response deleteReference(
//		@PathParam("commonId")
//		String commonId,
//		@PathParam("tenant")
//		String tenant,
//		@PathParam("entitySet")
//		String entitySet,
//        @PathParam("endpoint")
//        String endpoint,
//        @PathParam("endpointId")
//		String endpointId
//        ) {
//
//    	try {
//	    	Relation relation = xrefOperation.deleteReference(commonId, entitySet, tenant, endpoint, endpointId);
//	    	return Response.ok(relation).build();
//    	} catch (EntityNotFoundException e) {
//    		log.error(e.getMessage(), e);
//    		return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
//    	}
//    }
//
//
//    /**
//     * Add or update the Reference for this Endpoint for the Relation identified by this Common ID. Eg. http://localhost:27017/xref/viva/customer/55f623ace41d5524d0000001/sap/11111
//     *
//     * @param id
//     *
//     * @param tenant
//     *
//     * @param entitySet
//     *
//     * @param commonId
//     *
//     * @param endpoint
//     *
//     */
//    @PUT
//    @Path("{tenant}/{entitySet}/{commonId}/{endpoint}/{id}")
//    @Produces({
//        "application/xml"
//    })
//    public Response addOrUpdateReference(
//        @PathParam("id")
//        String id,
//        @PathParam("endpoint")
//        String endpoint,
//        @PathParam("commonId")
//        String commonId,
//        @PathParam("entitySet")
//        String entitySet,
//        @PathParam("tenant")
//        String tenant) {
//
//    	try {
//	    	Relation relation = xrefOperation.addOrUpdateReference(id, endpoint, commonId, entitySet, tenant);
//	    	return Response.ok(relation).build();
//    	} catch (EntityNotFoundException e) {
//    		log.error(e.getMessage(), e);
//    		return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
//    	}
//
//    }
//
//	public JDBCXrefOperation getXrefOperation() {
//		return xrefOperation;
//	}
//
//	public void setXrefOperation(JDBCXrefOperation xrefOperation) {
//		this.xrefOperation = xrefOperation;
//	}
//
//}
