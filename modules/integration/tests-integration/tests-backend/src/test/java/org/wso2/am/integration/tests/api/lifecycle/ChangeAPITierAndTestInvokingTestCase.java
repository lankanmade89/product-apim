/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Publish a API under gold tier and test the invocation with throttling , then change the api tier to silver
 * and do a new silver subscription and test invocation under Silver tier.
 */
public class ChangeAPITierAndTestInvokingTestCase extends APIManagerLifecycleBaseTest {
    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_END_POINT_METHOD = "/most_popular";
    private static final String API_RESPONSE_DATA = "<feed";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String APPLICATION_NAME = "ChangeAPITierAndTestInvokingTestCase";
    private String applicationNameGold;
    private String applicationNameSilver;
    private  Map<String, String> requestHeadersGoldTier;
    private Map<String, String> requestHeadersSilverTier;
    private APIIdentifier apiIdentifier;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        providerName = apimContext.getContextTenant().getContextUser().getUserName();
        apiPublisherClientUser1 = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStoreClientUser1 = new APIStoreRestClient(getStoreServerURLHttp());
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under tier Gold.")
    public void testInvokingWithGoldTier() throws Exception {

        applicationNameGold = APPLICATION_NAME + TIER_GOLD;
        apiStoreClientUser1.addApplication(applicationNameGold, TIER_GOLD, "", "");
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setTier(TIER_GOLD);
        createPublishAndSubscribeToAPI(
                apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1, apiStoreClientUser1, applicationNameGold);

        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, applicationNameGold);

        // Create requestHeaders
        requestHeadersGoldTier = new HashMap<String, String>();
        requestHeadersGoldTier.put("Authorization", "Bearer " + accessToken);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= GOLD_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                            API_END_POINT_METHOD, requestHeadersGoldTier);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                    "Response code mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API level tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA),
                    "Response data mismatched. Invocation attempt:" + invocationCount + " failed  during :" +
                            (currentTime - startTime) + " milliseconds under Gold API level tier");
        }

        currentTime = System.currentTimeMillis();
        HttpResponse invokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                API_END_POINT_METHOD, requestHeadersGoldTier);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API level tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                "Response data mismatched. Invocation attempt:" + (GOLD_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Gold API level tier");
    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of APi after expire the throttling block time.",
            dependsOnMethods = "testInvokingWithGoldTier")
    public void testInvokingAfterExpireThrottleExpireTime() throws InterruptedException, IOException {
        //wait millisecond to expire the throttling block
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);
        HttpResponse invokeResponse =
                HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_END_POINT_METHOD,
                        requestHeadersGoldTier);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched, " +
                "Invocation fails after wait " + (THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME) +
                "millisecond to expire the throttling block");
        assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched. " +
                "Invocation fails after wait " + (THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME) +
                "millisecond to expire the throttling block");
    }

    @Test(groups = {"wso2.am"}, description = "Test changing of the API Tier from Gold to Silver",
            dependsOnMethods = "testInvokingAfterExpireThrottleExpireTime")
    public void testEditAPITierToSilver() throws Exception {
        apiCreationRequestBean = new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setTier(TIER_SILVER);
        apiCreationRequestBean.setTiersCollection(TIER_SILVER);
        //Update API with Edited information with Tier Silver
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Update API Response Code is" +
                " invalid. Updating of API information fail" + getAPIIdentifierString(apiIdentifier));
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false", "Error in API Update in " +
                getAPIIdentifierString(apiIdentifier) + "Response Data:" + updateAPIHTTPResponse.getData());

    }


    @Test(groups = {"wso2.am"}, description = "test  invocation of  api under tier Silver.",
            dependsOnMethods = "testEditAPITierToSilver")
    public void testInvokingWithSilverTier() throws Exception {

        applicationNameSilver = APPLICATION_NAME + TIER_SILVER;

        // create new application
        apiStoreClientUser1.addApplication(applicationNameSilver, TIER_GOLD, "", "");
        apiIdentifier.setTier(TIER_SILVER);
        // Do a API Silver subscription.
        subscribeToAPI(apiIdentifier, applicationNameSilver, apiStoreClientUser1);
        //get access token
        String accessToken = getAccessToken(apiStoreClientUser1, applicationNameSilver);
        // Create requestHeaders
        requestHeadersSilverTier = new HashMap<String, String>();
        requestHeadersSilverTier.put("Authorization", "Bearer " + accessToken);
        //millisecond to expire the throttling block
        Thread.sleep(THROTTLING_UNIT_TIME + THROTTLING_ADDITIONAL_WAIT_TIME);
        long startTime = System.currentTimeMillis();
        long currentTime;
        for (int invocationCount = 1; invocationCount <= SILVER_INVOCATION_LIMIT_PER_MIN; invocationCount++) {
            currentTime = System.currentTimeMillis();
            //Invoke  API
            HttpResponse invokeResponse =
                    HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_END_POINT_METHOD,
                            requestHeadersSilverTier);
            assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Response code mismatched. " +
                    "Invocation attempt:" + invocationCount + " failed  during :" + (currentTime - startTime) +
                    " milliseconds under Silver API level tier");
            assertTrue(invokeResponse.getData().contains(API_RESPONSE_DATA), "Response data mismatched." +
                    " Invocation attempt:" + invocationCount + " failed  during :" + (currentTime - startTime) +
                    " milliseconds under Silver API level tier");

        }

        currentTime = System.currentTimeMillis();
        HttpResponse invokeResponse = HttpRequestUtil.doGet(API_BASE_URL + API_CONTEXT + "/" + API_VERSION_1_0_0 +
                API_END_POINT_METHOD, requestHeadersSilverTier);
        assertEquals(invokeResponse.getResponseCode(), HTTP_RESPONSE_CODE_SERVICE_UNAVAILABLE,
                "Response code mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Silver API level tier");
        assertTrue(invokeResponse.getData().contains(MESSAGE_THROTTLED_OUT),
                "Response data mismatched. Invocation attempt:" + (SILVER_INVOCATION_LIMIT_PER_MIN + 1) +
                        " passed  during :" + (currentTime - startTime) + " milliseconds under Silver API level tier");
    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        apiStoreClientUser1.removeApplication(applicationNameGold);
        apiStoreClientUser1.removeApplication(applicationNameSilver);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);


    }


}
