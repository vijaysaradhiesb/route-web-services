package com.integ.integ.web.services;


import com.meterware.servletunit.ServletRunner;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.junit4.TestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class IntegWebServicesCamelTestSupport extends TestSupport {

    protected ServletRunner servletRunner;

    @Autowired
    @Qualifier("integ-webservices-camel-context")
    protected ModelCamelContext wsCamelContext;


    public IntegWebServicesCamelTestSupport() throws Exception {
        ending();
        MockitoAnnotations.initMocks(this);
    }

    @Before
    public void beforeMethod() throws IOException, SAXException {
        File webXml = new File(IntegWebServicesCamelTestSupport.class.getResource("/web.xml").getFile());
        servletRunner = new ServletRunner(webXml, "/integ-claims-ws");
    }

    @After
    public void afterMethod() {
        servletRunner.shutDown();
    }

    @AfterClass
    public static void ending() throws Exception {
        try {
            FileUtils.deleteDirectory(new File("target/testing"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream sampleAsStream(String sampleName) throws IOException {
        return new ClassPathResource("samples/" + sampleName).getInputStream();
    }

    public String sampleAsString(String sampleName) throws IOException {
        File file = new ClassPathResource("samples/" + sampleName).getFile();
        return FileUtils.readFileToString(file);
    }
}
