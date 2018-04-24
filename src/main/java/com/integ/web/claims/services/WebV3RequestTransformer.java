
package com.integ.integ.web.services;

import com.integ.ice.webservices.model.GenericWebServiceMessage;
import com.integ.ice.webservices.model.KeyValuePair;
import com.integ.ice.webservices.model.ValueType;
import org.apache.axis.MessageContext;
import org.apache.axis.description.ParameterDesc;
import org.apache.axis.message.RPCElement;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.axis.providers.java.RPCProvider;
import org.apache.axis.wsdl.Skeleton;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.rpc.holders.LongHolder;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by integdev on 13/08/15.
 */

public class WebV3RequestTransformer extends AbstractWebV3Transformer {
    private static final Logger LOG = LoggerFactory.getLogger(WebV3RequestTransformer.class);

    public String[] transform(@Body String body, Exchange exchange) throws Exception {
        //We need synchronization as Axis is doing a lot of static stuff, so just want to avoid problems
        //Synchronization is only for transformations, not for waiting for real service impl to do it's work
        //so it should not affect responsiveness much.
        synchronized (skeletonMapping) {
            JAXBContext jaxbContext = JAXBContext.newInstance(GenericWebServiceMessage.class);
            String methodName = extractMethodName(body);

            LOG.info("AXIS Received call for method: " + methodName);

            Class<? extends Skeleton> skeleton = skeletonMapping.getSkeleton(methodName);
            exchange.setProperty(METHOD_HEADER, methodName);

            GenericWebServiceMessage message = new GenericWebServiceMessage();
            message.setName(methodName);
            final List<KeyValuePair> keyValuePairs = message.getKeyValuePair();

            MessageContext mc = buildMessageContext(skeleton);
            SOAPEnvelope req = new SOAPEnvelope(new ByteArrayInputStream(body.getBytes()));
            SOAPEnvelope res = new SOAPEnvelope();

            RPCElement rpcReq = new RPCElement(methodName);
            rpcReq.setNamespaceURI(INTEG_WEB_NS);
            req.addBodyElement(rpcReq);

            RPCProvider rpc = new RPCProvider() {
                protected Object invokeMethod(MessageContext msgContext, Method method, Object obj, Object[] argValues) throws Exception {
                    Class<?>[] parametersTypes = method.getParameterTypes();
                    ArrayList inParams = msgContext.getOperation().getAllInParams();
                    for (int i = 0; i < parametersTypes.length; i++) {
                        KeyValuePair kv = createKeyValuePair(((ParameterDesc) inParams.get(i)).getName(), parametersTypes[i], argValues[i]);
                        if (kv != null) {
                            keyValuePairs.add(kv);
                        }
                    }
                    return null;
                }
            };
            rpc.processMessage(mc, req, res, null);

            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter stringWriter = new StringWriter();
            marshaller.marshal(message, stringWriter);

            LOG.info("AXIS Request transformed to V4 for method: " + methodName);

            return new String[] {stringWriter.toString()};
        }
    }

    private KeyValuePair createKeyValuePair(String paramName, Class<?> paramType, Object paramValue) throws DatatypeConfigurationException {
        LOG.debug("AXIS received param: " + paramName + " with value: " + paramValue + " for type: " + paramType);

        if (paramValue == null) {
            return null;
        }

        KeyValuePair kv = new KeyValuePair();
        kv.setKey(paramName);

        if (paramType == String.class) {
            kv.setStringValue((String) paramValue);
            kv.setValueType(ValueType.STRING);

        } else if (paramType == boolean.class) {
            kv.setBooleanValue((boolean)paramValue);
            kv.setValueType(ValueType.BOOLEAN);

        } else if (paramType == Boolean.class) {
            kv.setBooleanValue((Boolean) paramValue);
            kv.setValueType(ValueType.BOOLEAN);

        } else if (paramType == Calendar.class) {
            kv.setDateTimeValue((Calendar) paramValue);
            kv.setValueType(ValueType.DATE_TIME);

        } else if (paramType.isArray()) {
            kv.setBinaryValue((byte[]) paramValue);
            kv.setValueType(ValueType.BYTE_ARRAY);

        } else if (paramType  == BigDecimal.class) {
            kv.setDecimalValue((BigDecimal) paramValue);
            kv.setValueType(ValueType.DECIMAL);

        } else if (paramType  == Long.class) {
            kv.setLongValue((Long)paramValue);
            kv.setValueType(ValueType.LONG);

        } else if (paramType  == long.class) {
            kv.setLongValue((long)paramValue);
            kv.setValueType(ValueType.LONG);

        } else if (paramType  == LongHolder.class) {
            if (paramValue instanceof LongHolder) {
                paramValue = ((LongHolder)paramValue).value;
            }
            kv.setLongValue(((Long)paramValue));
            kv.setValueType(ValueType.LONG);

        } else {
            throw new IllegalArgumentException("No support for parameter type: " + paramType);
        }

        return kv;
    }
}

