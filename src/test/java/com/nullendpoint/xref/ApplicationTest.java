/*
 * Copyright 2005-2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.nullendpoint.xref;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @Test
    public void createTenantAndIdTest(){

        Relation r = RelationFactory.createRelation();
        r.getReferences().add(RelationFactory.createRelationReference("sso", "redfoo"));

        Relation relResult = restTemplate.postForObject("/xref/redhat/person", r, Relation.class);

        assertThat(relResult.getCommonId()).isNotNull();
        assertThat(relResult.getReferences().get(0).getEndpoint()).isEqualToIgnoringCase("sso");
        assertThat(relResult.getReferences().get(0).getEndpointId()).isEqualToIgnoringCase("redfoo");

        //try posting the same thing again...
        Relation relResult2 = restTemplate.postForObject("/xref/redhat/person", r, Relation.class);
        //ensure the commonId is the same
        assertThat(relResult.getCommonId()).isEqualToIgnoringCase(relResult2.getCommonId());

    }

    @Test
    public void createMultipleIds() throws UnsupportedEncodingException {

        Relation r = RelationFactory.createRelation();
        r.getReferences().add(RelationFactory.createRelationReference("systemFoo", "foo123"));
        //{"references": [{"endpoint": "systemFoo", "endpointId": "foo123"} ] }

        Relation relResult = restTemplate.postForObject("/xref/companya/person", r, Relation.class);
        //result: {"id": 12, "commonId": "132ddd59-9678-483a-a3f5-7c3be7eaa6de", "references": [{"id": 14, "endpoint": "systemFoo", "endpointId": "foo123"} ] }

        //PUT a new entity related to this one
        String resourceUrl = "/xref/companya/person/" + relResult.getCommonId() + UriUtils.encodePath("/systemBar/bar123", "UTF-8");
        //URL: http://localhost:8080/xref/companya/person/132ddd59-9678-483a-a3f5-7c3be7eaa6de/systemBar/bar123
        HttpEntity requestUpdate = new HttpEntity<>(null, null);
        restTemplate.exchange(resourceUrl, HttpMethod.PUT, requestUpdate, Void.class);
        //result: {"id": 12, "commonId": "132ddd59-9678-483a-a3f5-7c3be7eaa6de", "references": [{"id": 14, "endpoint": "systemFoo", "endpointId": "foo123"}, {"id": 15, "endpoint": "systemBar", "endpointId": "bar123"} ] }

        //GET http://localhost:8080/xref/companya/person/f1e8dbd5-ab30-46e5-9503-6c2c105d45ef
        ResponseEntity<Relation> relResult2 = restTemplate.getForEntity("/xref/companya/person/" + relResult.getCommonId(), Relation.class);
        //result: {"id": 12, "commonId": "132ddd59-9678-483a-a3f5-7c3be7eaa6de", "references": [{"id": 14, "endpoint": "systemFoo", "endpointId": "foo123"}, {"id": 15, "endpoint": "systemBar", "endpointId": "bar123"} ] }
        //ensure the commonId is the same
        assertThat(relResult.getCommonId()).isEqualToIgnoringCase(relResult2.getBody().getCommonId());
        assertThat(relResult2.getBody().getReferences().size()).isEqualTo(2);

    }

    @Test
    public void testRemovalOfAnId() throws UnsupportedEncodingException {

        Relation r = RelationFactory.createRelation();
        r.getReferences().add(RelationFactory.createRelationReference("sso", "redfoo"));
        r.getReferences().add(RelationFactory.createRelationReference("dfs", "12334142424"));

        Relation relResult = restTemplate.postForObject("/xref/redhat/person", r, Relation.class);

        //try posting the same thing again...
        String resourceUrl = "/xref/redhat/person/" + relResult.getCommonId() + UriUtils.encodePath("/ActiveDirectory/redfoo~1", "UTF-8");
        HttpEntity requestUpdate = new HttpEntity<>(null, null);
        restTemplate.exchange(resourceUrl, HttpMethod.PUT, requestUpdate, Void.class);

        //GET http://localhost:8080/xref/companya/person/f1e8dbd5-ab30-46e5-9503-6c2c105d45ef
        ResponseEntity<Relation> relResult2 = restTemplate.getForEntity("/xref/redhat/person/" + relResult.getCommonId(), Relation.class);
        //ensure the commonId is the same
        assertThat(relResult.getCommonId()).isEqualToIgnoringCase(relResult2.getBody().getCommonId());
        assertThat(relResult2.getBody().getReferences().size()).isEqualTo(3);

        //now remove an id
        String deleteResourceUrl = "/xref/redhat/person/" + relResult.getCommonId() + UriUtils.encodePath("/ActiveDirectory/redfoo~1", "UTF-8");
        HttpEntity requestDelete = new HttpEntity<>(null, null);
        ResponseEntity<Relation> deleteResponse = restTemplate.exchange(deleteResourceUrl, HttpMethod.DELETE, requestDelete, Relation.class);
        assertThat(deleteResponse.getStatusCodeValue()).isEqualTo(200);
        assertThat(relResult.getCommonId()).isEqualToIgnoringCase(deleteResponse.getBody().getCommonId());
        assertThat(deleteResponse.getBody().getReferences().size()).isEqualTo(2);



    }

}