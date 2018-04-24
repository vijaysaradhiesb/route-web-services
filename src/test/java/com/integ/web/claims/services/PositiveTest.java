package com.integ.integ.web.services;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;
import com.integ.integ.web.services.v4.GenericWebServiceEndpointPortType;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.test.spring.DisableJmx;
import org.apache.camel.test.spring.ShutdownTimeout;
import org.apache.commons.lang.StringEscapeUtils;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.mockito.Mockito.*;

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
public class PositiveTest extends IntegClaimsWebServicesCamelTestSupport {
    public static final String V3_TO_V4_WS_URL = "http://localhost/integ-web-ws/services/V3ToV4";

    private static long WAIT_TIME;

    public PositiveTest() throws Exception {
    }

    @Mock
    ClaimsMock claimsMock;

    @Before
    public void createRoutes() throws Exception {
        WAIT_TIME=0;

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
                        .process(new Processor() {
                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Thread.sleep(WAIT_TIME);
                            }
                        })
                        .convertBodyTo(String.class)
                        .bean(claimsWS);
            }
        });
    }

    @Test
    public void testRequest() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request1.soap");
        String expectedV4ReqBody = sampleAsString("request1_expected.xml");
        String expectedV4Response = sampleAsString("response1_V4.xml");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn(expectedV4Response);

        // Invoke
        servletClient.getResponse(request);

        //Verify
        verify(claimsMock, timeout(5000)).setRecordedRequest(expectedV4ReqBody);
    }

    @Test
    public void testRequestWithNulls() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        servletClient.setExceptionsThrownOnErrorStatus(false);
        InputStream reqBody = sampleAsStream("request8.soap");
        String expectedV4ReqBody = sampleAsString("request8_expected.xml");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Invoke
        servletClient.getResponse(request);

        //Verify
        verify(claimsMock, timeout(5000)).setRecordedRequest(expectedV4ReqBody);
    }

    @Test
    public void testRequestResponse() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request1.soap");
        String expectedV4ReqBody = sampleAsString("request1_expected.xml");
        String expectedV4Response = sampleAsString("response1_V4.xml");
        String expectedResponse = sampleAsString("response1_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn(expectedV4Response);

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        String resBody = response.getText();

        //Verify
        verify(claimsMock, timeout(5000)).setRecordedRequest(expectedV4ReqBody);
        assertEquals(expectedResponse, resBody);
    }

    @Test
    public void testResponseComplex() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request1.soap");
        String expectedV4Response = sampleAsString("response1_V4.xml");
        String expectedResponse = sampleAsString("response1_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn(expectedV4Response);

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        String resBody = response.getText();

        //Verify
        assertEquals(expectedResponse, resBody);
    }

    @Test
    public void testResponseVoid() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request3.soap");
        String expectedResponse = sampleAsString("response3_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn("");

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        String resBody = response.getText();

        //Verify
        assertEquals(expectedResponse, resBody);
    }


    @Test
    public void testResponseString() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request5.soap");
        String expectedResponse = sampleAsString("response5_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn("String,String");

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        String resBody = response.getText();

        //Verify
        assertEquals(expectedResponse, resBody);
    }

    @Test
    public void testResponseHolder() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request6.soap");
        String expectedResponse = sampleAsString("response6_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn("321");

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        String resBody = response.getText();

        //Verify
        assertEquals(expectedResponse, resBody);
    }

    @Test
    public void testResponseHolderNullInRequest() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request10.soap");
        String expectedResponse = sampleAsString("response10_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn("321");

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        String resBody = response.getText();

        //Verify
        assertEquals(expectedResponse, resBody);
    }

    @Test
    public void testRequestWithComma() throws IOException, SAXException {
        // Init
        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request12.soap");
        String expectedResponse = sampleAsString("response12_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn("");

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        String resBody = response.getText();

        //Verify
        assertEquals(expectedResponse, resBody);
    }

    public class ClaimsMock {
        void setRecordedRequest(String responseBody) {}
        String getResponseBody() {
            return "";
        }
    }


    @Ignore
    @Test
    public void testWebserviceResponseTimeOut() throws IOException, SAXException {
        WAIT_TIME = 20000;

        long startTime   = System.currentTimeMillis();

        ServletUnitClient servletClient = servletRunner.newClient();
        InputStream reqBody = sampleAsStream("request13.soap");
        String expectedResponse = sampleAsString("response13_expected.soap");
        WebRequest request = new PostMethodWebRequest(V3_TO_V4_WS_URL, reqBody, "text/plain");

        // Mocking
        when(claimsMock.getResponseBody()).thenReturn("14891129");

        // Invoke
        WebResponse response = servletClient.getResponse(request);
        long endTime = System.currentTimeMillis();

        String resBody = response.getText();

        //Verify
        assertTrue(endTime - startTime < 10000);

        List<Difference> messageDifferences = new DetailedDiff(new Diff(expectedResponse, StringEscapeUtils.unescapeXml(resBody))).getAllDifferences();
        assertEquals("Only two differences are allowed", 2, messageDifferences.size());

    }

}
