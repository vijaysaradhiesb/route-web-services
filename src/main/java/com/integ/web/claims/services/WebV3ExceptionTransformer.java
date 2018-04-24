
package com.integ.integ.web.services;

import org.apache.axis.AxisFault;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.message.SOAPFault;
import org.apache.camel.ExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Created by integdev on 13/08/15.
 */

public class WebV3ExceptionTransformer extends AbstractWebV3Transformer {
    private static final Logger LOG = LoggerFactory.getLogger(WebV3ExceptionTransformer.class);

    //Exception exceptionCaught = exchange.getProperty("CamelExceptionCaught", Exception.class);
    public String transform(@ExchangeException Exception exception) throws Exception {

        //We need synchronization as Axis is doing a lot of static stuff, so just want to avoid problems
        //Synchronization is only for transformations, not for waiting for real service impl to do it's work
        //so it should not affect responsiveness much.
        synchronized (skeletonMapping) {
            SOAPEnvelope req = new SOAPEnvelope();

            AxisFault axisFault = null;
            try {
                axisFault = buildAxisFault(exception);
            } catch (Exception e) {//in case we've failed to parse exception, we still have to do our best to provide axis'es soap fault
                axisFault = buildAxisFault(e);
            }

            SOAPFault rpcReq = new SOAPFault(axisFault);
            rpcReq.setNamespaceURI(INTEG_WEB_NS);
            req.addBodyElement(rpcReq);

            return req.getAsString();
        }
    }

    private AxisFault buildAxisFault(Exception exception) {
        LOG.error("Axis WebService invocation exception", exception);
        AxisFault axisFault = new AxisFault("", exception);
        if (exception instanceof org.apache.cxf.binding.soap.SoapFault) {
            org.apache.cxf.binding.soap.SoapFault cxfFault = (org.apache.cxf.binding.soap.SoapFault)exception;
            if (cxfFault.getDetail() != null) {
                axisFault.setFaultDetail(new Element[]{cxfFault.getDetail()});
            }
            axisFault.setFaultCode(cxfFault.getFaultCode());
            axisFault.setFaultString(cxfFault.getReason());
        }

        return axisFault;
    }

}

