package com.nullendpoint.xref;

import com.google.common.collect.ImmutableMap;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.ehcache.EhcacheConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

public class CacheAccessor {
	
	private final ProducerTemplate template;
	private static final Logger log = LoggerFactory.getLogger(CacheAccessor.class);
	private static final String SAVE_TO_CACHE = "direct:saveToCache";
	private static final String LOAD_FROM_CACHE = "direct:getFromCache";
	private static final String DELETE_FROM_CACHE = "direct:deleteFromCache";

	public CacheAccessor(ProducerTemplate template) {
		this.template = template;
	}

	public Relation getRelationByEndpoint(String tenant, String entitySet, String endpoint, String endpointId) {
		String cacheKey = createCacheKey(tenant, entitySet);
		String endpointKey = createEndpointKey(endpoint, endpointId);
		Map headers = ImmutableMap.of("CamelEhcacheName", cacheKey, EhcacheConstants.KEY, endpointKey);
		try {
			Object obj = template.requestBodyAndHeaders(LOAD_FROM_CACHE, null, headers);
			if(obj != null) {
				log.trace("Cache hit for Endpoint: "+endpointKey);
				Relation r = (Relation) obj;
				return r;
			}
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		log.trace("Cache miss for Endpoint: "+endpointKey);
		return null;
	}

	public Relation getRelationByCommonId(String tenant, String entitySet, String commonId) {
		String cacheKey = createCacheKey(tenant, entitySet);
		Map headers = ImmutableMap.of("CamelEhcacheName", cacheKey, EhcacheConstants.KEY, commonId);
		try {
			Object obj = template.requestBodyAndHeaders(LOAD_FROM_CACHE, null, headers);
			//Element element = cache.get(commonId);
			if(obj != null) {
				log.trace("Cache hit for CommonId: "+commonId);
				Relation r = (Relation) obj;
				return r;
			}
		} catch(Exception e) {
			log.error(e.getMessage(), e);
		}
		log.trace("Cache miss for CommonId: "+commonId);
		return null;
	}


	public void putRelationByEndpoint(String tenant, String entitySet, String endpoint, String endpointId, Relation relation) {
		String cacheKey = createCacheKey(tenant, entitySet);
		try {
			Map headers = ImmutableMap.of("CamelEhcacheName", cacheKey, EhcacheConstants.KEY, createEndpointKey(endpoint, endpointId));
			template.requestBodyAndHeaders(SAVE_TO_CACHE, relation, headers);
		} catch(Exception e) {
			log.error("Could not insert into cache: "+e.getMessage(), e);
		}
	}

	public void putRelationByCommonId(String tenant, String entitySet, String commonId, Relation relation) {
		String cacheKey = createCacheKey(tenant, entitySet);
		try {
			Map headers = ImmutableMap.of("CamelEhcacheName", cacheKey, EhcacheConstants.KEY, commonId);
			template.requestBodyAndHeaders(SAVE_TO_CACHE, relation, headers);
		} catch(Exception e) {
			log.error("Could not insert into cache: "+e.getMessage(), e);
		}
	}

	public void deleteRelationByEndpoint(String tenant, String entitySet, String endpoint, String endpointId) {
		String cacheKey = createCacheKey(tenant, entitySet);
		try {
			Map headers = ImmutableMap.of("CamelEhcacheName", cacheKey, EhcacheConstants.KEY, createEndpointKey(endpoint, endpointId));
			template.requestBodyAndHeaders(DELETE_FROM_CACHE, null, headers);
		} catch(Exception e) {
			log.error("Could not delete from cache: "+e.getMessage(), e);
		}
	}
	
	public void deleteRelationByCommonId(String tenant, String entitySet, String commonId, Relation relation) {
		String cacheKey = createCacheKey(tenant, entitySet);
		try {
			Map headers = ImmutableMap.of("CamelEhcacheName", cacheKey, EhcacheConstants.KEY, commonId);
			template.requestBodyAndHeaders(DELETE_FROM_CACHE, null, headers);
		} catch(Exception e) {
			log.error("Could not delete from cache: "+e.getMessage(), e);
		}
	}	

	
	private String createCacheKey(String tenant, String cacheName) {
		return tenant + ":" + cacheName;
	}
	
	private String createEndpointKey(String endpoint, String endpointId) {
		return endpoint + ":" + endpointId;
	}
	
}
