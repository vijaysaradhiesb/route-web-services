
package com.integ.integ.web.services;

import com.integ.integ.web.services.util.V4ToV3BeanUtil;
import org.apache.axis.MessageContext;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.axis.wsdl.Skeleton;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.rpc.holders.LongHolder;
import java.io.StringReader;
import java.lang.reflect.Method;

/**
 * Created by integdev on 13/08/15.
 */

public class WebV3ResponseTransformer extends AbstractWebV3Transformer {
    private static final Logger LOG = LoggerFactory.getLogger(WebV3ResponseTransformer.class);

    private static JAXBContext RESPONSE_JAXB_CONTEXT;

    static {
        try {
            RESPONSE_JAXB_CONTEXT = JAXBContext.newInstance(com.integ.ice.webservices.model.FindRefDataDataSetType.class.getPackage().getName());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public String transform(@Body String xml, Exchange exchange) throws Exception {
        String methodName = exchange.getProperty(METHOD_HEADER, String.class);

        LOG.info("AXIS Received response for method: " + methodName);

        //We need synchronization as Axis is doing a lot of static stuff, so just want to avoid problems
        //Synchronization is only for transformations, not for waiting for real service impl to do it's work
        //so it should not affect responsiveness much.
        synchronized (skeletonMapping) {
            Object v3Obj;
            Class<?> returnType = skeletonMapping.getReturnType(methodName);

            LOG.debug("AXIS Expected response for method: " + methodName + " is: " + returnType);

            // As methods are returning different type: different primitives or complex objects, we have to find out
            // which type we are expecting to receive and then try to handle it
            if (returnType == Void.TYPE) {
                v3Obj = null;
            } else if (returnType == long.class) {
                v3Obj = Long.valueOf(xml);
            } else if (returnType == String.class) {
                v3Obj = xml;
            } else if (returnType == LongHolder.class) {
                v3Obj = new LongHolder(Long.valueOf(xml));
            } else { // handling complext object type response
                Unmarshaller unmarshaller = RESPONSE_JAXB_CONTEXT.createUnmarshaller();
                Object v4Obj = unmarshaller.unmarshal(new StringReader(xml));
                v3Obj = V4ToV3BeanUtil.newV3Instance(v4Obj);

                new V4ToV3BeanUtil().copyProperties(v3Obj, v4Obj);
            }

            String response = packAsRpcEncodedEnvelope(v3Obj, methodName, skeletonMapping.getSkeleton(methodName));

            LOG.info("AXIS Transformed received response for method: " + methodName);

            return response;
        }
    }

    /**
     * Simulating receiving response inside Axis and catching result
     *
     * @param v3Obj
     * @param methodName
     * @param skeleton
     * @return
     * @throws Exception
     */
    public String packAsRpcEncodedEnvelope(Object v3Obj, String methodName, Class<? extends Skeleton> skeleton) throws Exception {
        MessageContext mc = buildMessageContext(skeleton);

        SOAPEnvelope req = new SOAPEnvelope();
        SOAPEnvelope res = new SOAPEnvelope();

        RPCElement rpcReq = new RPCElement(methodName);
        rpcReq.setObjectValue(v3Obj);
        rpcReq.setNamespaceURI(INTEG_WEB_NS);
        req.addBodyElement(rpcReq);

        //This logic is to override Axis WS implementor execution to catch/override response
        //It is hard to clearly explain how it works, as current solution was rather worked out with massive debugging
        //and more with guessing as Axis is kind of ancient technology, with not well documented internall logic
        RPCProvider rpc = new RPCProvider() {
            protected Object invokeMethod(MessageContext msgContext, Method method, Object obj, Object[] argValues) throws Exception {
                if (obj instanceof LongHolder) {
                    for (Object arg : argValues) {
                        if (arg != null) {
                            ((LongHolder)arg).value = ((LongHolder)obj).value;
                        }
                    }
                    return null;
                } else {
                    return obj;
                }
            }
        };
        rpc.processMessage(mc, req, res, v3Obj);

        return res.getAsString();
    }
}

