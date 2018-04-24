package com.integ.integ.web.services;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;
import com.integ.integ.web.services.v4.GenericWebServiceEndpointPortType;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.test.spring.DisableJmx;
import org.apache.camel.test.spring.ShutdownTimeout;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.when;

/**
 * Created by integdev on 13/08/15.
 */
@DisableJmx
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/test-context.xml",
        "classpath:META-INF/spring/camel-context.xml"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ShutdownTimeout(300)
public class NegativeTest extends IntegClaimsWebServicesCamelTestSupport {
    public static final String V3_TO_V4_WS_URL = "http://localhost/integ-claims-ws/services/WebV4";

    public NegativeTest() throws Exception {
    }

    @Mock
    ClaimsMock claimsMock;

    @Before
    public void createRoutes() throws Exception {
        wsCamelContext.setTracing(true);
        wsCamelContext.setUseMDCLogging(true);

        wsCamelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {//localhost/");
                GenericWebServiceEndpointPortType claimsWS = new GenericWebServiceEndpointPortType() {
                    @Override
                    public String execute(String arg0) {
                        claimsMock.setRecordedRequest(arg0);
                        return claimsMock.getResponseBody();
                    }
                };

                CxfEndpoint cxf = new CxfEndpoint();
                cxf.setAddress("http://localhost:58264/test");
                cxf.setServiceClass(claimsWS);
                cxf.setCamelContext(wsCamelContext);

                from(cxf)
                        .convertBodyTo(String.class)
                        .bean(claimsWS);
            }
        });
    }

    @Test
    public void testExceptionThrown() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        servletClient.setExceptionsThrownOnErrorStatus(false);

        InputStream reqBody = sampleAsStream("request1.soap");
        String expectedResponse = sampleAsString("fault1_expected.soap");

        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenThrow(new RuntimeException("ONE TICKET TO NEVERLAND PLEASE!"));

        // Invoke
        WebResponse response = servletClient.getResponse(request);

        servletClient.getCurrentPage();
        String resBody = response.getText();

        //Verify
        assertEquals(expectedResponse, resBody.replaceAll("<ns1:hostname xmlns:ns1=\"http://xml.apache.org/axis/\">(.+?)</ns1:hostname>",
                "<ns1:hostname xmlns:ns1=\"http://xml.apache.org/axis/\">localhost</ns1:hostname>"));
    }

    @Test
    public void testGeneralFault() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        servletClient.setExceptionsThrownOnErrorStatus(false);

        InputStream reqBody = sampleAsStream("request7.soap");
        String expectedResponse = sampleAsString("fault7_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenThrow(new RuntimeException("ONE TICKET TO NEVERLAND PLEASE!"));

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        String resBody = response.getText();

        //Verify
        assertTrue(resBody.contains("<faultcode>soapenv:Server.generalException</faultcode>"));
        assertTrue(resBody.contains("com.ctc.wstx.exc.WstxUnexpectedCharException: Unexpected character '!' (code 33) expected '='"));
    }

    public class ClaimsMock {
        void setRecordedRequest(String responseBody) {}
        String getResponseBody() {
            return "";
        }
    }
}
