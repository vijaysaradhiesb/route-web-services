package com.integ.integ.web.services;

import com.itf.integclaims.ws.FindRefDataDataSetRecordType;
import com.itf.integclaims.ws.FindRefDataDataSetType;
import com.itf.integclaims.ws.ICEGUIWebServiceSoapBindingSkeleton;
import org.junit.Test;

/**
 * Created by integdev on 19/08/15.
 */
public class RpcEncodingTest extends IntegClaimsWebServicesCamelTestSupport {

    public RpcEncodingTest() throws Exception {
    }

    @Test
    public void testRpcEncoding() throws Exception {
        String expected = sampleAsString("response2_expected.soap");

        FindRefDataDataSetType underTest = new FindRefDataDataSetType();
        FindRefDataDataSetRecordType e = new FindRefDataDataSetRecordType();
        e.setCode("CODE");
        e.setDescription("DESCRIPTION");
        e.setDescriptionCode("DESCR_CODE");
        e.setParentCode("PARENT_CODE");
        e.setCompanyCode("COMPANY_CODE");
        e.setFilterCode("FILTER_CODE");

        underTest.setFindRefDataDataSetRecord(new FindRefDataDataSetRecordType[]{e, e});

        String result = new WebV3ResponseTransformer().packAsRpcEncodedEnvelope(underTest, "FindRefData", ICEGUIWebServiceSoapBindingSkeleton.class);

        assertEquals(expected, result);
    }
}
