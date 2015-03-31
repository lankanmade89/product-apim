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
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Publish a API and check its visibility in the API Store.
 */
public class APIPublishingAndVisibilityInStoreTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String APPLICATION_NAME = "APIPublishingAndVisibilityInStoreTestCase";
    private APIIdentifier apiIdentifier;
    private String providerName;
    private APICreationRequestBean apiCreationRequestBean;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        providerName = apimContext.getContextTenant().getContextUser().getUserName();
        apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiPublisherClientUser1 = new APIPublisherRestClient(getPublisherServerURLHttp());
        apiStoreClientUser1 = new APIStoreRestClient(getStoreServerURLHttp());
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(apimContext.getContextTenant().getContextUser().getUserName(),
                apimContext.getContextTenant().getContextUser().getPassword());
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        apiStoreClientUser1.addApplication(APPLICATION_NAME, "", "", "");
    }


    @Test(groups = {"wso2.am"}, description = "Create a API and  check its availability in Publisher.")
    public void testAPICreation() throws Exception {
        //Create APi
        // HttpResponse createAPIResponse = createAPI(API1_NAME, API1_CONTEXT, API_VERSION_1_0_0, apiPublisherClientUser1);
        HttpResponse createAPIResponse = apiPublisherClientUser1.addAPI(apiCreationRequestBean);
        assertEquals(createAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Create API Response Code is invalid." + getAPIIdentifierString(apiIdentifier));
        assertEquals(getValueFromJSON(createAPIResponse, "error"), "false",
                "Error in API Creation in " + getAPIIdentifierString(apiIdentifier) +
                        "Response Data:" + createAPIResponse.getData());
        //Verify the API in API Publisher
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUser1.getApi(API_NAME, providerName, API_VERSION_1_0_0));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiPublisherAPIIdentifierList), true,
                "Added Api is not available in APi Publisher. " + getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Check the visibility of API in Store before the API publish. " +
            "it should not be available in store.", dependsOnMethods = "testAPICreation")
    public void testVisibilityOfAPIInStoreBeforePublishing() throws Exception {
        //Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList), false,
                "Api is visible in API Store before publish." + getAPIIdentifierString(apiIdentifier));
    }


    @Test(groups = {"wso2.am"}, description = "Test the API publishing action. " +
            "Response HTTP message should contains API status change from  CREATED to PUBLISHED",
            dependsOnMethods = "testVisibilityOfAPIInStoreBeforePublishing")
    public void testAPIPublishing() throws Exception {
        //Publish the API
        APILifeCycleStateRequest publishUpdateRequest =
                new APILifeCycleStateRequest(API_NAME, providerName, APILifeCycleState.PUBLISHED);
        publishUpdateRequest.setVersion(API_VERSION_1_0_0);
        HttpResponse publishAPIResponse =
                apiPublisherClientUser1.changeAPILifeCycleStatusToPublish(apiIdentifier, false);
        assertEquals(publishAPIResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "API publish Response code is invalid " + getAPIIdentifierString(apiIdentifier));
        assertTrue(verifyAPIStatusChange(publishAPIResponse, APILifeCycleState.CREATED, APILifeCycleState.PUBLISHED),
                "API status Change is invalid in" + getAPIIdentifierString(apiIdentifier) +
                        "Response Data:" + publishAPIResponse.getData());
    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility of API in the store after API publish.",
            dependsOnMethods = "testAPIPublishing")
    public void testVisibilityOfAPIInStoreAfterPublishing() throws Exception {
        //Verify the API in API Store : API should not be available in the store.
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.getAPI(API_NAME));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifier, apiStoreAPIIdentifierList), true,
                "Api is not visible in API Store after publish. " + getAPIIdentifierString(apiIdentifier));

    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }


}
