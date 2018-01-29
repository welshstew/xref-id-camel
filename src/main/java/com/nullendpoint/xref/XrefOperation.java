package com.nullendpoint.xref;

public interface XrefOperation {

	public Relation addOrUpdateReference(String id,
                                         String endpoint,
                                         String commonId,
                                         String entitySet,
                                         String tenant) throws EntityNotFoundException;

	public Relation createRelation(String entitySet,
                                   String tenant,
                                   Relation relation) throws EntityNotFoundException;

	public Relation deleteReference(String commonId,
                                    String entitySet,
                                    String tenant,
                                    String endpoint,
                                    String endpointId) throws EntityNotFoundException;

	public Relation findRelation(String entitySet,
                                 String tenant,
                                 String endpoint,
                                 String id) throws EntityNotFoundException;

	public Relation findRelationByCommonId(String commonId,
                                           String entitySet,
                                           String tenant) throws EntityNotFoundException;

	public Relation updateRelation(String entitySet,
                                   String tenant,
                                   Relation relation) throws EntityNotFoundException;
}
