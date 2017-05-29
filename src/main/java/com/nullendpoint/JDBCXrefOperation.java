package com.nullendpoint;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Component
public class JDBCXrefOperation implements XrefOperation {

	private static final Logger log = LoggerFactory.getLogger(JDBCXrefOperation.class);
	private final JdbcTemplate jdbcTemplate;
	private final ProducerTemplate producerTemplate;
	private final CacheAccessor cacheAccessor;

	public JDBCXrefOperation(JdbcTemplate jdbcTemplate, ProducerTemplate producerTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.producerTemplate = producerTemplate;
		this.cacheAccessor = new CacheAccessor(producerTemplate);
	}


	public Relation findRelation(String entitySet, String tenant, String endpoint, String endpointId) throws EntityNotFoundException {
		Relation cachedRelation = cacheAccessor.getRelationByEndpoint(tenant, entitySet, endpoint, endpointId);
		if(cachedRelation != null) {
			log.debug("Cache hit for endpoint "+endpoint+" and id "+endpointId);
			return cachedRelation;
		} else {
			log.debug("Cache miss for endpoint "+endpoint+" and id "+endpointId);
			Integer entityTypeId = findEntityType(tenant, entitySet);
			Relation uncachedRelation = findRelationByEndpointAndEndpointID(entityTypeId, endpoint, endpointId);
			cacheAccessor.putRelationByEndpoint(tenant, entitySet, endpoint, endpointId, uncachedRelation);
			return uncachedRelation;
		}
	}

	public Relation createRelation(final String entitySet, final String tenant, Relation relation) throws EntityNotFoundException {
		Integer entityTypeId = findOrCreateEntityType(tenant, entitySet);
		for(Relation.Reference reference : relation.getReferences()) {
			try {
				Relation existingRelation = findRelationByEndpointAndEndpointID(entityTypeId, reference.getEndpoint(), reference.getEndpointId());
				log.info("Attempting to create Relation but reference already exists for "+reference.getEndpoint()+" and ID "+reference.getEndpointId());
				return existingRelation;
			} catch (EntityNotFoundException e) {
				//ignore
			}
		}
		Integer relationId = saveRelation(entityTypeId);
		for(Relation.Reference reference : relation.getReferences()) {
			saveReference(relationId, reference.getEndpoint(), reference.getEndpointId());
		}
		relation = getRelation(relationId);
		for(Relation.Reference reference : relation.getReferences()) {
			cacheAccessor.putRelationByEndpoint(tenant, entitySet, reference.getEndpoint(), reference.getEndpointId(), relation);
		}
		return relation;
	}

	public Relation updateRelation(String entitySet, String tenant, Relation relation) throws EntityNotFoundException {
		Relation currentRelation = getRelationByCommonID(relation.getCommonID());
		for(Relation.Reference reference : relation.getReferences()) {
			saveOrUpdateReference(currentRelation.getId(), relation.getCommonID(), reference.getEndpoint(), reference.getEndpointId());
		}
		relation = getRelationByCommonID(relation.getCommonID());
		for(Relation.Reference reference : relation.getReferences()) {
			cacheAccessor.putRelationByEndpoint(tenant, entitySet, reference.getEndpoint(), reference.getEndpointId(), relation);
		}
		return relation;
	}

	public Relation findRelationByCommonId(String commonId, String entitySet, String tenant) throws EntityNotFoundException {
		Relation cachedRelation = cacheAccessor.getRelationByCommonId(tenant, entitySet, commonId);
		if(cachedRelation != null) {
			log.debug("Cache hit for common id "+commonId);
			return cachedRelation;
		} else {
			log.debug("Cache miss for common id "+commonId);
			Relation uncachedRelation = getRelationByCommonID(commonId);
			cacheAccessor.putRelationByCommonId(tenant, entitySet, commonId, uncachedRelation);
			return uncachedRelation;
			
		}
	}

	public Relation deleteReference(String commonId, String entitySet, String tenant, String endpoint, String endpointId) throws EntityNotFoundException {
		Relation relation = findRelationByCommonId(commonId, entitySet, tenant);
		deleteReference(relation.getId(), endpoint);
		cacheAccessor.deleteRelationByEndpoint(tenant, entitySet, endpoint, endpointId);
		return getRelation(relation.getId());
	}

	public Relation addOrUpdateReference(String endpointId, String endpoint,
			String commonId, String entitySet, String tenant) throws EntityNotFoundException {
		Relation relation = getRelationByCommonID(commonId);
		saveOrUpdateReference(relation.getId(), commonId, endpoint, endpointId);
		relation = getRelation(relation.getId());
		cacheAccessor.putRelationByEndpoint(tenant, entitySet, endpoint, endpointId, relation);
		cacheAccessor.putRelationByCommonId(tenant, entitySet, commonId, relation);
		return relation;
	}

	private Integer findOrCreateEntityType(final String tenant, final String entitySet) {
		Integer entityTypeId = findEntityType(tenant, entitySet);
		if(entityTypeId == null) {
			KeyHolder holder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
					PreparedStatement ps = connection.prepareStatement("insert into entitytype (tenant, entitytype) values (?, ?)", new String[] {"id"});
					ps.setString(1,  tenant);
					ps.setString(2, entitySet);
					return ps;
				}
			}, holder);
			entityTypeId = holder.getKey().intValue();
		}
		return entityTypeId;
	}
	
	private Integer findEntityType(final String tenant, final String entitySet) {
		try {
			return getEntityById("select id from entitytype where tenant = ? and entitytype = ?", new Object[] {tenant, entitySet}, new RowMapper<Integer>() {
				public Integer mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					return rs.getInt("id");
				}}, "Could not find EntityType with the provided Identifier");
		} catch (EntityNotFoundException e) {
			return null;
		}
	}
	
	private Integer saveRelation(final Integer entityTypeId) {
		KeyHolder holder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement("insert into relation (commonid, entitytype_id) values (?, ?)", new String[] {"id"});
				ps.setString(1, UUID.randomUUID().toString());
				ps.setInt(2, entityTypeId);
				return ps;
			}
		}, holder);
		return holder.getKey().intValue();
	}
	
	private Integer saveReference(final Integer relationId, final String endpoint, final String endpointId) {
		KeyHolder holder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement("insert into reference (relation_id, endpoint, endpointid) values (?, ?, ?)", new String[] {"id"});
				ps.setInt(1, relationId);
				ps.setString(2, endpoint);
				ps.setString(3, endpointId);
				return ps;
			}
		}, holder);
		return holder.getKey().intValue();
	}
	
	private Relation getRelation(Integer relationId) throws EntityNotFoundException {
		return getEntityById("select * from relation where id = ?", new Object[] {relationId}, new RowMapper<Relation>() {
				public Relation mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					Relation relation = RelationFactory.createRelation();
					relation.setId(rs.getInt("id"));
					relation.setCommonID(rs.getString("commonid"));
					relation.getReferences().addAll(getReferences(rs.getInt("id")));
					return relation;
				}}, "Could not find Relation with the provided Identifier");
	}
	
	private Relation getRelationByCommonID(String commonID) throws EntityNotFoundException {
		return getEntityById("select * from relation where commonid = ?", new Object[] {commonID}, new RowMapper<Relation>() {
			public Relation mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Relation relation = RelationFactory.createRelation();
				relation.setId(rs.getInt("id"));
				relation.setCommonID(rs.getString("commonid"));
				relation.getReferences().addAll(getReferences(rs.getInt("id")));
				return relation;
			}}, "Could not find Relation with the provided Identifier");
	}
	
	private List<Relation.Reference> getReferences(Integer relationId) {
		return jdbcTemplate.query("select * from reference where relation_id = ?",
				new Object[] { relationId }, 
				new RowMapper<Relation.Reference>() {
					public Relation.Reference mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						Relation.Reference reference = RelationFactory.createRelationReference(
								rs.getInt("id"),
								rs.getString("endpoint"),
								rs.getString("endpointid"));
						return reference;
					}
		});
	}
	
	private void saveOrUpdateReference(int relationId, String commonID, String endpoint, String endpointId) {
		try {
			Relation relation = getRelationByCommonID(commonID);
			relationId = relation.getId();
			Relation.Reference reference = findReferenceByEndpointAndCommonID(commonID, endpoint);
			updateReference(reference.getId(), endpoint, endpointId);
		} catch (EntityNotFoundException e) {
			saveReference(relationId, endpoint, endpointId);
		}
	}

	private void updateReference(int referenceId, String endpoint, String endpointId) {
		jdbcTemplate.update("update reference set endpoint = ?, endpointid = ? where id = ?",
				endpoint, endpointId, referenceId);
	}
	
	private void deleteReference(int relationId, String endpoint) {
		jdbcTemplate.update("delete reference where relation_id = ? and endpoint = ?",
				relationId, endpoint);
	}

	private Relation.Reference findReferenceByEndpointAndCommonID(String commonID, String endpoint) throws EntityNotFoundException {
		return getEntityById("select reference.* from reference inner join relation on relation.id = reference.relation_id where commonid = ? and endpoint = ?", 
				new Object[] {commonID, endpoint}, new RowMapper<Relation.Reference>() {
					public Relation.Reference mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						Relation.Reference reference = RelationFactory.createRelationReference(
								rs.getInt("id"),
								rs.getString("endpoint"), 
								rs.getString("endpointid"));
						return reference;
					}}, "Could not find Reference with the provided Identifier");

	}
	
	private Relation findRelationByEndpointAndEndpointID(Integer entityTypeId, String endpoint, String endpointId) throws EntityNotFoundException {
		return getEntityById("select relation.* from reference inner join relation on relation.id = reference.relation_id where entitytype_id = ? and endpoint = ? and endpointid = ?", 
				new Object[] {entityTypeId, endpoint, endpointId}, new RowMapper<Relation>() {
			public Relation mapRow(ResultSet rs, int rowNum)
					throws SQLException {
				Relation relation = RelationFactory.createRelation();
				relation.setId(rs.getInt("id"));
				relation.setCommonID(rs.getString("commonid"));
				relation.getReferences().addAll(getReferences(rs.getInt("id")));
				return relation;
			}}, "Could not find Relation with the provided Identifiers");
		
	}

	private <T> T getEntityById(String sql, Object[] ids, RowMapper<T> rowMapper, String errorMessage) throws EntityNotFoundException {
		try {
			return jdbcTemplate.queryForObject(sql, ids, rowMapper); 
		} catch(EmptyResultDataAccessException e) {
			throw new EntityNotFoundException(errorMessage);
		}
	}

	public CacheAccessor getCacheAccessor() {
		return cacheAccessor;
	}

}
