package com.integ.integ.web.services.util;

import org.apache.axis.description.*;
import org.apache.axis.wsdl.Skeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.rpc.holders.Holder;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by klimczakp on 21/08/15.
 */
public class SkeletonMapping {
    private static final Logger LOG = LoggerFactory.getLogger(SkeletonMapping.class);
    private List<? extends Skeleton> skeletons;
    private Map<String, Class<?>> returnTypes = new HashMap<String, Class<?>>();
    private Map<String, Class<? extends Skeleton>> methodToSkeletons = new HashMap<String, Class<? extends Skeleton>>();

    public Class<? extends Skeleton> getSkeleton(String methodName) {
        return methodToSkeletons.get(methodName.toUpperCase());
    }

    public Class<?> getReturnType(String methodName) {
        return returnTypes.get(methodName.toUpperCase());
    }

    public void setSkeletons(List<? extends Skeleton> skeletons) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        this.skeletons = skeletons;

        for (Skeleton skeleton : skeletons) {
            //getOperationDescs
            mapWebServiceInterfaces(skeleton);
            makeWebServiveParamsOmmitable(skeleton);
        }
    }

    private void makeWebServiveParamsOmmitable(Skeleton skeleton) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        //For non complext types
        Method getOperationDescs = skeleton.getClass().getMethod("getOperationDescs");
        List<OperationDesc> operations = (List<OperationDesc>) getOperationDescs.invoke(skeleton);
        LOG.info("Found " + operations.size() + " operations in skeleton: " + skeleton);
        for (OperationDesc operation : operations) {
            List<ParameterDesc> params = operation.getParameters();
            for (ParameterDesc param : params) {
                LOG.info("Making ommitable/nullable " + param.getName() + " in operation " + operation.getName() + " in skeleton: " + skeleton);
                param.setOmittable(true);
            }
        }

        //For complex types
        Class<?> wsInterface = findWsInterface(skeleton);
        for (Method method : wsInterface.getDeclaredMethods()) {
            Class<?> returnType = mapReturnType(method);
            makeFieldOmmitable(skeleton, returnType);
        }
    }

    private void makeFieldOmmitable(Skeleton skeleton, Class<?> clazz) throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        if (clazz.isArray()) {
            clazz = clazz.getComponentType();
        }

        TypeDesc typeDesc = getTypeDesc(clazz);
        if (typeDesc != null) {
            for (FieldDesc field : typeDesc.getFields()) {
                LOG.info("Making field ommitable/nullable " + field.getFieldName() + " in type " + clazz.getName() + " in skeleton: " + skeleton);
                ((ElementDesc) field).setMinOccurs(0);
                makeFieldOmmitable(skeleton, clazz.getDeclaredField(field.getFieldName()).getType());
            }
        }
    }

    private TypeDesc getTypeDesc(Class<?> clazz) throws InvocationTargetException, IllegalAccessException {
        try {
            Method getTypeDesc = clazz.getMethod("getTypeDesc");
            return (TypeDesc) getTypeDesc.invoke(clazz);
        } catch (NoSuchMethodException e) {
            //no-op, non axis generated type
        }

        return null;
    }


    private void mapWebServiceInterfaces(Skeleton skeleton) {
        List<Class<?>> discoveredPT = new ArrayList<Class<?>>();

        Class<?> wsInterface = findWsInterface(skeleton);
        for (Method method : wsInterface.getDeclaredMethods()) {
            if (returnTypes.containsKey(method.getName())) {
                throw new IllegalStateException("Duplicated method: " + method.getName() + " in skeleton: " + skeleton);
            }

            LOG.info("Found method : " + method.getName() + " in skeleton: " + skeleton.getClass().getName());
            returnTypes.put(method.getName().toUpperCase(), mapReturnType(method));
            methodToSkeletons.put(method.getName().toUpperCase(), skeleton.getClass());

            for (Class<?> c : method.getParameterTypes()) {
                if (!discoveredPT.contains(c)) {
                    discoveredPT.add(c);
                }
            }
        }

        LOG.info("List of parameter types used in skeletons:");
        for (Class<?> c : discoveredPT) {
            LOG.info("Found paramater type: " + c.getName());
        }
    }

    private Class<?> mapReturnType(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == Void.TYPE) {
            for (Class<?> c : method.getParameterTypes()) {
                if (Holder.class.isAssignableFrom(c)) {
                    return c;
                }
            }
        }

        return returnType;
    }

    private Class<?> findWsInterface(Skeleton skeleton) {
        for (Class<?> clazz : skeleton.getClass().getInterfaces()) {
            if (clazz != Skeleton.class && clazz != Serializable.class && clazz != Remote.class) {
                return clazz;
            }
        }
        return null;
    }
}
