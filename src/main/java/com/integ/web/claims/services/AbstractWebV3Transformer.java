
package com.integ.integ.web.services;

import com.integ.integ.web.services.util.SkeletonMapping;
import org.apache.axis.AxisEngine;
import org.apache.axis.AxisFault;
import org.apache.axis.AxisProperties;
import org.apache.axis.MessageContext;
import org.apache.axis.description.JavaServiceDesc;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.server.AxisServer;
import org.apache.axis.wsdl.Skeleton;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;

/**
 * Created by integdev on 13/08/15.
 *
 */

public class AbstractWebV3Transformer {
    protected static final String INTEG_WEB_NS = "http://ws.integclaims.itf.com";
    protected static final String METHOD_HEADER = "CLAIMS_V3_METHOD_NAME";
    protected SkeletonMapping skeletonMapping;

    protected MessageContext buildMessageContext(Class<? extends Skeleton> skeleton) throws AxisFault {
        AxisServer axisServer = new AxisServer();
        MessageContext mc = new MessageContext(axisServer);
        SOAPService soapService = new SOAPService();
        JavaServiceDesc javaServiceDesc = new JavaServiceDesc();
        javaServiceDesc.setImplClass(skeleton);
        javaServiceDesc.setDefaultNamespace(INTEG_WEB_NS);
        soapService.setServiceDescription(javaServiceDesc);
        mc.setService(soapService);
        mc.setProperty(AxisEngine.PROP_DISABLE_PRETTY_XML, false);
        AxisProperties.setProperty(AxisEngine.PROP_DISABLE_PRETTY_XML, "false");
        return mc;
    }

    protected String extractMethodName(String body) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader xml = inputFactory.createXMLStreamReader(new ByteArrayInputStream(body.getBytes()));
        while (xml.hasNext()) {
            int event = xml.next();

            if (event == XMLEvent.START_ELEMENT && INTEG_WEB_NS.equals(xml.getNamespaceURI())) {
                return xml.getName().getLocalPart();
            }
        }
        return null;
    }

    public void setSkeletonMapping(SkeletonMapping skeletonMapping) {
        this.skeletonMapping = skeletonMapping;
    }
}

