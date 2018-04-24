package com.integ.integ.web.services.util;

import com.itf.integclaims.ws.FindRefDataDataSetType;
import com.integ.integ.web.services.WebV3ResponseTransformer;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by IntegDev on 20/08/15.
 *
 * Written to deal with List -> Array property rewrite problem and also problem with rewriting Axis's complex type
 * properties from V4 to V3 classes
 *
 */
public class V4ToV3BeanUtil extends PropertyUtilsBean {
    public static final Logger LOG = LoggerFactory.getLogger(V4ToV3BeanUtil.class.getName());
    public static final String V3_MODEL_PACKAGE = FindRefDataDataSetType.class.getPackage().getName();

    public void setSimpleProperty(Object bean, String name, Object value) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

        try {
            PropertyDescriptor descriptor = getPropertyDescriptor(bean, name);
            Method writeMethod = MethodUtils.getAccessibleMethod(bean.getClass(), descriptor.getWriteMethod());

            Class writerMethodParamType = writeMethod.getParameterTypes()[0];
            Class arrayType = writerMethodParamType.getComponentType();

            if (value instanceof List) {
                Object arr = Array.newInstance(arrayType, ((List) value).size());
                for (int c = 0; c < ((List) value).size(); c++) {
                    Object v4Instance = ((List) value).get(c);
                    Object v3Instance = newV3Instance(v4Instance);
                    new V4ToV3BeanUtil().copyProperties(v3Instance, v4Instance);
                    Array.set(arr, c, v3Instance);
                }

                value = arr;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        Object v3Obj = V4ToV3BeanUtil.newV3Instance(value);
        if (v3Obj != null) { // we are working on V3 specific complex class
            new V4ToV3BeanUtil().copyProperties(v3Obj, value);
            super.setSimpleProperty(bean, name, v3Obj);
        } else { // we are working on non V3 specific type
            super.setSimpleProperty(bean, name, value);
        }
    }

    /**
     * We are creating V3 object instance knowing V4 object class
     *
     * @param obj
     * @return
     */
    public static Object newV3Instance(Object obj) {
        try {
            String simpleName = obj.getClass().getSimpleName();
            return WebV3ResponseTransformer.class.forName(V3_MODEL_PACKAGE + "." + simpleName).newInstance();
        } catch (Exception e) {
            //this is expected for axis non complex types
            return null;
        }
    }
}
