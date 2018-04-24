package com.integ.web.claims.services;

import com.integ.integclaims.ws.*;
import com.integ.integ.web.services.util.SkeletonMapping;
import com.integ.ice.webservices.model.GenericWebServiceMessage;
import com.integ.ice.webservices.model.KeyValuePair;
import com.integ.ice.webservices.model.ValueType;
import org.apache.axis.wsdl.Skeleton;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by integ on 14/08/15.
 *
 */
public class TransformTest extends IntegClaimsWebServicesCamelTestSupport {

    public TransformTest() throws Exception {
    }

    @Test
    public void testSimpleRequest() throws Exception {
        String sample = sampleAsString("request1.soap");
        String expected = sampleAsString("request1_expected.xml");

        WebV3RequestTransformer WebV3Transformer = new WebV3RequestTransformer();
        SkeletonMapping skeletonMapping = new SkeletonMapping();
        List<Skeleton> skeletons = new ArrayList<Skeleton>();
        skeletons.add(new ICEGUIWebServiceSoapBindingSkeleton());
        skeletonMapping.setSkeletons(skeletons);
        WebV3Transformer.setSkeletonMapping(skeletonMapping);

        String[] result = WebV3Transformer.transform(sample, new DefaultExchange((CamelContext) null));
        assertEquals(expected, result[0]);
    }


    /**
     * Tests only the date JAXB xml serialization.
     *
     * @throws Exception
     */
    @Test
    public void testDateRequestJAXB() throws Exception {

        String expected = sampleAsString("jaxb_serialization_date_test.xml");

        JAXBContext jaxbContext = JAXBContext.newInstance(GenericWebServiceMessage.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        StringWriter stringWriter = new StringWriter();

        GenericWebServiceMessage message = new GenericWebServiceMessage();

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
        Date date = df.parse("2001-10-26T19:32:52+00:00");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        KeyValuePair pair = new KeyValuePair();
        pair.setValueType(ValueType.DATE_TIME);
        pair.setDateTimeValue(cal);

        message.getKeyValuePair().add(pair);

        marshaller.marshal(message, stringWriter);

        assertEquals(expected, stringWriter.toString());
    }

    @Test
    public void testLongHolderRequest() throws Exception {
        String sample = sampleAsString("request6.soap");
        String expected = sampleAsString("request6_expected.xml");

        WebV3RequestTransformer WebV3Transformer = new WebV3RequestTransformer();
        SkeletonMapping skeletonMapping = new SkeletonMapping();
        List<Skeleton> skeletons = new ArrayList<Skeleton>();
        skeletons.add(new ICEScanningWebServiceSoapBindingSkeleton());
        skeletonMapping.setSkeletons(skeletons);
        WebV3Transformer.setSkeletonMapping(skeletonMapping);

        String[] result = WebV3Transformer.transform(sample, new DefaultExchange((CamelContext) null));
        assertEquals(expected, result[0]);
    }


    @Test
    public void testResponse() throws Exception {
        String sample = sampleAsString("response1_V4.xml");
        String expected = sampleAsString("response1_expected.soap");

        WebV3ResponseTransformer WebV3Transformer = new WebV3ResponseTransformer();
        SkeletonMapping skeletonMapping = new SkeletonMapping();
        List<Skeleton> skeletons = new ArrayList<Skeleton>();
        skeletons.add(new ICEGUIWebServiceSoapBindingSkeleton());
        skeletonMapping.setSkeletons(skeletons);
        WebV3Transformer.setSkeletonMapping(skeletonMapping);

        DefaultExchange exchange = new DefaultExchange((CamelContext) null);
        exchange.setProperty(WebV3ResponseTransformer.METHOD_HEADER, "FindRefData");

        String result = WebV3Transformer.transform(sample, exchange);
        assertEquals(expected, result);
    }

    @Test
    public void testResponseIgnoringSchema() throws Exception {
        String sample = sampleAsString("response9_V4.xml");
        String expected = sampleAsString("response9_expected.soap");

        WebV3ResponseTransformer WebV3Transformer = new WebV3ResponseTransformer();
        SkeletonMapping skeletonMapping = new SkeletonMapping();
        List<Skeleton> skeletons = new ArrayList<Skeleton>();
        skeletons.add(new ICEGUIWebServiceSoapBindingSkeleton());
        skeletonMapping.setSkeletons(skeletons);
        WebV3Transformer.setSkeletonMapping(skeletonMapping);

        DefaultExchange exchange = new DefaultExchange((CamelContext) null);
        exchange.setProperty(WebV3ResponseTransformer.METHOD_HEADER, "FindRefData");

        String result = WebV3Transformer.transform(sample, exchange);
        assertEquals(expected, result);
    }

    @Test
    public void testResponseForTypeMismatchBug() throws Exception {
        String sample = sampleAsString("response11_V4.xml");
        String expected = sampleAsString("response11_expected.soap");

        WebV3ResponseTransformer WebV3Transformer = new WebV3ResponseTransformer();
        SkeletonMapping skeletonMapping = new SkeletonMapping();
        List<Skeleton> skeletons = new ArrayList<Skeleton>();
        skeletons.add(new ICEGUIWebServiceSoapBindingSkeleton());
        skeletonMapping.setSkeletons(skeletons);
        WebV3Transformer.setSkeletonMapping(skeletonMapping);

        DefaultExchange exchange = new DefaultExchange((CamelContext) null);
        exchange.setProperty(WebV3ResponseTransformer.METHOD_HEADER, "financebankstatementGetBankStatement");

        String result = WebV3Transformer.transform(sample, exchange);
        assertEquals(expected, result);
    }

    @Test
    public void testRequestComma() throws Exception {
        String sample = sampleAsString("request12.soap");
        String expected = sampleAsString("request12_expected.xml");

        WebV3RequestTransformer WebV3Transformer = new WebV3RequestTransformer();
        SkeletonMapping skeletonMapping = new SkeletonMapping();
        List<Skeleton> skeletons = new ArrayList<Skeleton>();
        skeletons.add(new ICEClaimNotifyImmediateSoapBindingSkeleton());
        skeletonMapping.setSkeletons(skeletons);
        WebV3Transformer.setSkeletonMapping(skeletonMapping);

        String[] result = WebV3Transformer.transform(sample, new DefaultExchange((CamelContext) null));
        assertEquals(expected, result[0]);
    }
}
