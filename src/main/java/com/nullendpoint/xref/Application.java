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

import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ehcache.EhcacheConstants;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.ehcache.CacheManager;
import org.ehcache.config.Configuration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Qualifier("dataSource")
    @Autowired
    DataSource dataSource;

    @Autowired
    CamelContext camelContext;

    @Bean(name = "json-jackson")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public JacksonDataFormat jacksonDataFormat() {
        return new JacksonDataFormat(new ObjectMapper(), Relation.class, SimpleResponse.class);
    }

//    @Bean
//    public ObjectMapper relationObjectMapper(){
//        ObjectMapper om = new ObjectMapper();
//        om.qrit
//
//    }
    @Bean
    ServletRegistrationBean servletRegistrationBean() {
        ServletRegistrationBean servlet = new ServletRegistrationBean(
            new CamelHttpTransportServlet(), "/*");
        servlet.setName("CamelServlet");
        return servlet;
    }

    @Bean
    JdbcTemplate jdbcTemplate(){
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate;
    }

    @Bean
    XrefOperationImpl xrefOperationImpl(){
            XrefOperationImpl xrefOperationImpl = new XrefOperationImpl(jdbcTemplate(),producerTemplate());
        return xrefOperationImpl;
    }

    @Bean
    ProducerTemplate producerTemplate(){
        return camelContext.createProducerTemplate();
    }

    @Component
    class RestApi extends RouteBuilder {

        @Override
        public void configure() {
            restConfiguration()
                .contextPath("/").apiContextPath("/api-doc")
                    .apiProperty("api.title", "Camel REST API")
                    .apiProperty("api.version", "1.0")
                    .apiProperty("cors", "true")
                    .apiContextRouteId("doc-api")
                .component("servlet")
                .bindingMode(RestBindingMode.json);

            onException(EntityNotFoundException.class)
                    .handled(true)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            exchange.getIn().setBody(new SimpleResponse("Could not find Relation with the provided Identifiers.  Perhaps provide queryString params..?"));
                        }
                    });

            rest("/xref").description("Identity Xref Service")
                    .get("/").description("simple hello i'm alive")
                        .route().routeId("xref-api")
                        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                exchange.getIn().setBody(new SimpleResponse("hello i'm alive"));
                            }
                        })
                    .endRest()
                    .get("/{tenant}/{entitySet}")
                        .param().name("endpoint").type(RestParamType.query).description("endpoint name").endParam()
                        .param().name("id").type(RestParamType.query).description("endpoint id").endParam()
                        .route().routeId("find-relation")
                        .bean(xrefOperationImpl(), "findRelation")
                    .endRest()
                    .post("/{tenant}/{entitySet}")
                        .route().routeId("create-relation")
                        .bean(xrefOperationImpl(), "createRelation")
                    .endRest()
                    .put("/{tenant}/{entitySet}")
                        .route().routeId("update-relation")
                        .bean(xrefOperationImpl(), "updateRelation")
                    .endRest()
                    .get("/{tenant}/{entitySet}/{commonId}")
                        .route().routeId("find-relation-commonId")
                        .bean(xrefOperationImpl(), "findRelationByCommonId")
                    .endRest()
                    .delete("/{tenant}/{entitySet}/{commonId}/{endpoint}/{endpointId}")
                        .route().routeId("delete-reference")
                        .bean(xrefOperationImpl(), "deleteReference")
                    .endRest()
                    .put("/{tenant}/{entitySet}/{commonId}/{endpoint}/{id}")
                        .route().routeId("add-update-reference")
                        .bean(xrefOperationImpl(), "addOrUpdateReference")
                    .endRest();
        }
    }

    @Component
    class Backend extends RouteBuilder {

        @Override
        public void configure() {

            from("direct:saveToCache").setExchangePattern(ExchangePattern.InOut)
                    .setHeader(EhcacheConstants.ACTION, constant(EhcacheConstants.ACTION_PUT))
                    .marshal().json(JsonLibrary.Jackson).convertBodyTo(String.class)
                    .setHeader(EhcacheConstants.VALUE, body())
                    .to("log:com.nullendpoint.xref?showAll=true")
                    .toD("ehcache://${header.CamelEhcacheName}?configuration=#myProgrammaticConfiguration&keyType=java.lang.String&valueType=java.lang.String");

            from("direct:getFromCache").setExchangePattern(ExchangePattern.InOut)
                    .setHeader(EhcacheConstants.ACTION, constant(EhcacheConstants.ACTION_GET))
                    .toD("ehcache://${header.CamelEhcacheName}?configuration=#myProgrammaticConfiguration&keyType=java.lang.String&valueType=java.lang.String")
                    .choice().when(simple("${body} != null")).unmarshal().json(JsonLibrary.Jackson);

            from("direct:deleteFromCache").setExchangePattern(ExchangePattern.InOut)
                    .setHeader(EhcacheConstants.ACTION, constant(EhcacheConstants.ACTION_REMOVE))
                    .toD("ehcache://${header.CamelEhcacheName}?configuration=#myProgrammaticConfiguration&keyType=java.lang.String&valueType=java.lang.String");

        }
    }
}