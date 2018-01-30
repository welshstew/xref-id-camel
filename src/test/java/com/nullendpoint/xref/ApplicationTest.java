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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriUtils;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleToIntFunction;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
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
        r.getReferences().add(RelationFactory.createRelationReference("sso", "redfoo"));

        Relation relResult = restTemplate.postForObject("/xref/redhat/person", r, Relation.class);

        //try posting the same thing again...
        String resourceUrl = "/xref/redhat/person/" + relResult.getCommonId() + UriUtils.encodePath("/ActiveDirectory/redfoo~1", "UTF-8");
        HttpEntity requestUpdate = new HttpEntity<>(null, null);
        restTemplate.exchange(resourceUrl, HttpMethod.PUT, requestUpdate, Void.class);

        //GET http://localhost:8080/xref/companya/person/f1e8dbd5-ab30-46e5-9503-6c2c105d45ef
        ResponseEntity<Relation> relResult2 = restTemplate.getForEntity("/xref/redhat/person/" + relResult.getCommonId(), Relation.class);
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

//    @Test
//    public void newOrderTest() {
//        // Wait for maximum 5s until the first order gets inserted and processed
//        NotifyBuilder notify = new NotifyBuilder(camelContext)
//            .fromRoute("generate-order")
//            .whenDone(2)
//            .and()
//            .fromRoute("process-order")
//            .whenDone(1)
//            .create();
//        assertThat(notify.matches(10, TimeUnit.SECONDS)).isTrue();
//
//        // Then call the REST API
//        ResponseEntity<Order> orderResponse = restTemplate.getForEntity("/camel-rest-sql/books/order/1", Order.class);
//        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        Order order = orderResponse.getBody();
//        assertThat(order.getId()).isEqualTo(1);
//        assertThat(order.getAmount()).isBetween(1, 10);
//        assertThat(order.getItem()).isIn("Camel", "ActiveMQ");
//        assertThat(order.getDescription()).isIn("Camel in Action", "ActiveMQ in Action");
//        assertThat(order.isProcessed()).isTrue();
//
//        ResponseEntity<List<Book>> booksResponse = restTemplate.exchange("/camel-rest-sql/books",
//            HttpMethod.GET, null, new ParameterizedTypeReference<List<Book>>(){});
//        assertThat(booksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
//        List<Book> books = booksResponse.getBody();
//        assertThat(books).hasSize(2);
//        assertThat(books).element(0)
//            .hasFieldOrPropertyWithValue("description", "ActiveMQ in Action");
//        assertThat(books).element(1)
//            .hasFieldOrPropertyWithValue("description", "Camel in Action");
//    }
//
//    @Test
//    public void testText() throws InterruptedException, JsonProcessingException {
//
//
//        Object quantity = new Integer(12);
//
//        if(quantity == null){
//            System.out.println("null!");
//        }else if(quantity instanceof Number){
//            System.out.println(((Number) quantity).doubleValue());
//        }else if(quantity instanceof String){
//            if("".equals(quantity)) {
//                System.out.println("null!");
//            }else{
//                Double myDouble = Double.parseDouble((String)quantity);
//                System.out.println(myDouble.toString());
//            }
//        }
//
//
//        if(quantity == null){
//            System.out.println("null!");
//        }else if(quantity instanceof Double){
//            System.out.println(((Double) quantity));
//        }else if(quantity instanceof String){
//            if("".equals(quantity)) {
//                System.out.println("null!");
//            }else{
//                Double myDouble = Double.parseDouble((String)quantity);
//                System.out.println(myDouble.toString());
//            }
//        }


//        TestObject to = new TestObject();
//        to.setName("hello");
//        to.setNumber(10.43);
//
//        ObjectMapper mapper = new ObjectMapper();
//        String item = mapper.writeValueAsString(to);
//
//        String something = "10.12";
//        Double thing = new Double(10.12);
//
//        if("".equals(something)){
//            System.out.print("hello");
//        }
//
//        if (something == null) {
//            System.out.println("null!");
//        }else if("".equals(something)){
//            System.out.println("null!");
//        } else {
//            Double ohHai = Double.parseDouble(something);
//            System.out.println(ohHai);
//        }
//
//        if (thing == null) {
//            System.out.println("null!");
//        }else if("".equals(thing)){
//            System.out.println("null!");
//        } else {
//            Double ohHai = Double.parseDouble(something);
//            System.out.println(ohHai);
//        }
//
//        Thread.sleep(10000);
//        // Then call the REST API
//        String result = restTemplate.getForObject("/camel/xref", String.class);
////        assertThat(stringResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(result).isEqualToIgnoringCase("{ \"status\":\"hello i'm alive\"}");
//
//    }

}