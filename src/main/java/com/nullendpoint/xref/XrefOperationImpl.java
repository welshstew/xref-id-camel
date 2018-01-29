package com.nullendpoint.xref;

import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Created by swinches on 24/05/17.
 */
@Component
public class XrefOperationImpl implements XrefOperation {

    private JDBCXrefOperation xrefOperation;

    @Autowired
    public XrefOperationImpl(JdbcTemplate jdbcTemplate, ProducerTemplate producerTemplate) {
        xrefOperation = new JDBCXrefOperation(jdbcTemplate, producerTemplate);
    }



    @Override
    public Relation addOrUpdateReference(@Header("id") String id,
                                         @Header("endpoint") String endpoint,
                                         @Header("commonId") String commonId,
                                         @Header("entitySet") String entitySet,
                                         @Header("tenant") String tenant) throws EntityNotFoundException {
        return xrefOperation.addOrUpdateReference(id, endpoint, commonId, entitySet, tenant);
    }

    @Override
    public Relation createRelation(@Header("entitySet") String entitySet,
                                   @Header("tenant") String tenant,
                                   @Body Relation relation) throws EntityNotFoundException {
        return xrefOperation.createRelation(entitySet, tenant, relation);
    }

    @Override
    public Relation deleteReference(@Header("commonId") String commonId,
                                    @Header("entitySet") String entitySet,
                                    @Header("tenant") String tenant,
                                    @Header("endpoint") String endpoint,
                                    @Header("endpointId") String endpointId) throws EntityNotFoundException {
        return xrefOperation.deleteReference(commonId, entitySet, tenant, endpoint, endpointId);
    }

    @Override
    public Relation findRelation(@Header("entitySet") String entitySet,
                                 @Header("tenant") String tenant,
                                 @Header("endpoint") String endpoint,
                                 @Header("id") String id) throws EntityNotFoundException {
        return xrefOperation.findRelation(entitySet, tenant, endpoint, id);
    }

    @Override
    public Relation findRelationByCommonId(@Header("commonId") String commonId,
                                           @Header("entitySet") String entitySet,
                                           @Header("tenant") String tenant) throws EntityNotFoundException {
        return xrefOperation.findRelationByCommonId(commonId, entitySet, tenant);
    }

    @Override
    public Relation updateRelation(@Header("entitySet") String entitySet,
                                   @Header("tenant") String tenant,
                                   @Body Relation relation) throws EntityNotFoundException {
        return xrefOperation.updateRelation(entitySet, tenant, relation);
    }

}
