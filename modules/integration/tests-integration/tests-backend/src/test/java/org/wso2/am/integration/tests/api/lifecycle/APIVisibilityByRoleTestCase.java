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

import org.json.JSONException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.APIMgtTestUtil;
import org.wso2.am.integration.test.utils.base.AMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import javax.xml.xpath.XPathExpressionException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * Create a API with Role visibility and check the visibility in Publisher Store.
 */
public class APIVisibilityByRoleTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "APILifeCycleTestAPI1";
    private static final String API_CONTEXT = "testAPI1";
    private static final String API_TAGS = "youtube, video, media";
    private static final String API_END_POINT_URL = "http://gdata.youtube.com/feeds/api/standardfeeds";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String CARBON_SUPER_TENANT2_KEY = "userKey2";
    private static final String TENANT_DOMAIN_KEY = "wso2.com";
    private static final String TENANT_DOMAIN_ADMIN_KEY = "admin";
    private static final String USER_KEY_USER2 = "userKey1";
    private String providerName;
    private APIPublisherRestClient apiPublisherClientUser2;
    private APIStoreRestClient apiStoreClientUser2;
    private APIPublisherRestClient apiPublisherClientUser1;
    private APIStoreRestClient apiStoreClientUser1;
    private APICreationRequestBean apiCreationRequestBean;
    private APIIdentifier apiIdentifierAdminVisibility;
    private APIIdentifier apiIdentifierSubscriberVisibility;
    private APIStoreRestClient apiStoreClientAnotherUserSameDomain;
    private APIPublisherRestClient apiPublisherClientUserAnotherUserSameDomain;
    private APIStoreRestClient apiStoreClientAnotherUserOtherDomain;
    private APIPublisherRestClient apiPublisherClientAnotherUserOtherDomain;
    private APIStoreRestClient apiStoreClientAdminOtherDomain;
    private APIPublisherRestClient apiPublisherClientAdminOtherDomain;
    private UserManagementClient userManagementClient;
    private AutomationContext otherDomainContext;
    private APIStoreRestClient apiStoreClientSubscriberUserSameDomain;
    private APIStoreRestClient apiStoreClientSubscriberUserOtherDomain;
    private static final String API_NAME_ADMIN_VISIBILITY = "APIAdminVisibility";
    private static final String API_NAME_SUBSCRIBER_VISIBILITY = "APISubscriberVisibility";
    private String apiCreatorStoreDomain;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();

        // apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
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
        //Login to API Publisher with  User1
        providerName = apimContext.getContextTenant().getTenantUser(USER_KEY_USER2).getUserName();
        String user2PassWord = apimContext.getContextTenant().getTenantUser(USER_KEY_USER2).getPassword();
        apiPublisherClientUser2.login(providerName, user2PassWord);
        //Login to API Store with  User1
        apiStoreClientUser2.login(providerName, user2PassWord);

        apiCreatorStoreDomain = apimContext.getContextTenant().getDomain();
        apiIdentifierAdminVisibility =
                new APIIdentifier(providerName, API_NAME_ADMIN_VISIBILITY, API_VERSION_1_0_0);
        apiIdentifierSubscriberVisibility =
                new APIIdentifier(providerName, API_NAME_SUBSCRIBER_VISIBILITY, API_VERSION_1_0_0);

        apiStoreClientAnotherUserSameDomain = new APIStoreRestClient(getStoreServerURLHttp());
        apiPublisherClientUserAnotherUserSameDomain =
                new APIPublisherRestClient(getPublisherServerURLHttp());
        String AnotherUserSameDomainUserName =
                apimContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getUserName();
        String AnotherUserSameDomainPassword =
                apimContext.getContextTenant().getTenantUser(CARBON_SUPER_TENANT2_KEY).getPassword();
        apiStoreClientAnotherUserSameDomain.login(AnotherUserSameDomainUserName, AnotherUserSameDomainPassword);
        apiPublisherClientUserAnotherUserSameDomain.login(AnotherUserSameDomainUserName, AnotherUserSameDomainPassword);
        userManagementClient = new UserManagementClient(
                apimContext.getContextUrls().getBackEndUrl(), getSessionCookie());
        userManagementClient.addUser("subscriberUser1", "password@123",
                new String[]{"Internal/subscriber"}, null);

        apiStoreClientSubscriberUserSameDomain = new APIStoreRestClient(getStoreServerURLHttp());
        apiStoreClientSubscriberUserSameDomain.login("subscriberUser1", "password@123");


        otherDomainContext = new AutomationContext(AMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                AMIntegrationConstants.AM_1ST_INSTANCE,
                TENANT_DOMAIN_KEY, TENANT_DOMAIN_ADMIN_KEY);

        apiStoreClientAnotherUserOtherDomain = new APIStoreRestClient(getStoreServerURLHttp());
        apiPublisherClientAnotherUserOtherDomain = new APIPublisherRestClient(getPublisherServerURLHttp());
        String userOtherDomainUserName = otherDomainContext.getContextTenant().getTenantUser("user1").getUserName();
        String userOtherDomainPassword = otherDomainContext.getContextTenant().getTenantUser("user1").getPassword();
        apiStoreClientAnotherUserOtherDomain.login(userOtherDomainUserName, userOtherDomainPassword);
        apiPublisherClientAnotherUserOtherDomain.login(userOtherDomainUserName, userOtherDomainPassword);

        apiStoreClientAdminOtherDomain = new APIStoreRestClient(getStoreServerURLHttp());
        apiPublisherClientAdminOtherDomain = new APIPublisherRestClient(getPublisherServerURLHttp());
        String adminOtherDomainUserName = otherDomainContext.getContextTenant().getContextUser().getUserName();
        String adminOtherDomainPassword = otherDomainContext.getContextTenant().getContextUser().getPassword();
        apiStoreClientAdminOtherDomain.login(adminOtherDomainUserName, adminOtherDomainPassword);
        apiPublisherClientAdminOtherDomain.login(adminOtherDomainUserName, adminOtherDomainPassword);

        userManagementClient = new UserManagementClient(
                otherDomainContext.getContextUrls().getBackEndUrl(), getSessionCookie());
        userManagementClient.addUser("subscriberUser2", "password@123",
                new String[]{"Internal/subscriber"}, null);
        apiStoreClientSubscriberUserOtherDomain = new APIStoreRestClient(getStoreServerURLHttp());
        apiStoreClientSubscriberUserOtherDomain.login("subscriberUser2", "password@123");


    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for API creator ")
    public void testVisibilityForCreatorInPublisher() throws APIManagerIntegrationTestException, JSONException,
            MalformedURLException {
        //Create API  with public visibility and publish.
        APICreationRequestBean apiCreationReqBeanVisibilityAdmin =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationReqBeanVisibilityAdmin.setTags(API_TAGS);
        apiCreationReqBeanVisibilityAdmin.setDescription(API_DESCRIPTION);
        apiCreationReqBeanVisibilityAdmin.setVisibility("restricted");
        apiCreationReqBeanVisibilityAdmin.setRoles("admin");
        apiPublisherClientUser2.addAPI(apiCreationReqBeanVisibilityAdmin);
        publishAPI(apiIdentifierAdminVisibility, apiPublisherClientUser2, false);
        APICreationRequestBean apiCreationReqBeanVisibilityInternalSubscriber =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, new URL(API_END_POINT_URL));
        apiCreationReqBeanVisibilityInternalSubscriber.setTags(API_TAGS);
        apiCreationReqBeanVisibilityInternalSubscriber.setDescription(API_DESCRIPTION);
        apiCreationReqBeanVisibilityInternalSubscriber.setVisibility("restricted");
        apiCreationReqBeanVisibilityInternalSubscriber.setRoles("admin");
        apiPublisherClientUser2.addAPI(apiCreationReqBeanVisibilityInternalSubscriber);
        publishAPI(apiIdentifierSubscriberVisibility, apiPublisherClientUser2, false);
        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUser2.getAllAPIs());

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList), true,
                "API with  Role admin  visibility is not visible to creator in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList), true,
                "API with  Role Internal/subscriber  visibility is not visible to creator in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for API creator",
            dependsOnMethods = "testVisibilityForCreatorInPublisher")
    public void testVisibilityForCreatorInStore() throws Exception {
        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser2.
                        getAllPublishedAPIs(apiCreatorStoreDomain));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList), true,
                "API with  Role admin  visibility is not visible to creator in API Store." +
                        getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList), true,
                "API  with  Role Internal/subscriber  is not visible to creator in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for admin in same domain ",
            dependsOnMethods = "testVisibilityForCreatorInStore")
    public void testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInPublisher()
            throws APIManagerIntegrationTestException, JSONException {

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiPublisherClientUser1.getAllAPIs());
        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList), true,
                "API with  Role admin  visibility is not visible to Admin user with Admin and subscriber role in same" +
                        " domain in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList), true,
                "API with  Role Internal/subscriber  visibility is not visible to Admin user with Admin and subscriber" +
                        " role in same domain  in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierSubscriberVisibility));

    }

    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for admin in same domain ",
            dependsOnMethods = "testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInPublisher")
    public void testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientUser1.
                        getAllPublishedAPIs(apiCreatorStoreDomain));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList), true,
                "API with  Role admin  visibility is not visible to Admin user with Admin and subscriber role in same " +
                        "domain  in API Store." + getAPIIdentifierString(apiIdentifierAdminVisibility));


        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList), true,
                "API  with  Role Internal/subscriber  is not visible to Admin user with Admin and subscriber role in same " +
                        "domain  in API Store. " + getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for another user in same domain",
            dependsOnMethods = "testVisibilityForAdminUserWithAdminAndSubscriberRoleInSameDomainInStore")
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInPublisher() throws
            APIManagerIntegrationTestException, JSONException {

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientUserAnotherUserSameDomain.getAllAPIs());

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList), true,
                "API with  Role admin  visibility is not visible to another user with Admin and subscriber role in same " +
                        "domain  in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList), true,
                "API with  Role Internal/subscriber  visibility is not visible to another user with Admin and subscriber " +
                        "role in same domain in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for another user in same domain",
            dependsOnMethods = "testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInPublisher")
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserSameDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList), true,
                "API with  Role admin  visibility is not visible to another user with Admin and subscriber role in same" +
                        " domain in API Store." +
                        getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList), true,
                "API  with  Role Internal/subscriber  is not visible to another user with Admin and subscriber role in" +
                        " same domain in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for another user in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserWithAdminAndSubscriberRoleInSameDomainInStore")
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInOtherDomainInPublisher() throws
            APIManagerIntegrationTestException, JSONException {

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientAnotherUserOtherDomain.getAllAPIs());

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList), false,
                "API with  Role admin  visibility is  visible to another user with Admin and subscriber role in other " +
                        "domain in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList), false,
                "API with  Role Internal/subscriber  visibility is  visible to another user with Admin and subscriber" +
                        " role in other domain in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierSubscriberVisibility));
    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for another user in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserWithAdminAndSubscriberRoleInOtherDomainInPublisher")
    public void testVisibilityForAnotherUserWithAdminAndSubscriberRoleInOtherDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientAnotherUserOtherDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList), false,
                "API with  Role admin  visibility is  visible to another user with Admin and subscriber role in other " +
                        "domain in API Store." +
                        getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList), false,
                "API  with  Role Internal/subscriber  is  visible to another user with Admin and subscriber role in other " +
                        "domain in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Publisher for admin in other domain",
            dependsOnMethods = "testVisibilityForAnotherUserWithAdminAndSubscriberRoleInOtherDomainInStore")
    public void testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInPublisher() throws
            APIManagerIntegrationTestException, JSONException {

        List<APIIdentifier> apiPublisherAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(
                        apiPublisherClientAdminOtherDomain.getAllAPIs());

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierAdminVisibility, apiPublisherAPIIdentifierList), false,
                "API with  Role admin  visibility is  visible to Admin user with Admin and subscriber role in other " +
                        "domain in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(this.apiIdentifierSubscriberVisibility, apiPublisherAPIIdentifierList), false,
                "API with  Role Internal/subscriber  visibility is  visible to Admin user with Admin and subscriber role" +
                        " in other domain in API Publisher." +
                        getAPIIdentifierString(this.apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for admin in other domain",
            dependsOnMethods = "testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInPublisher")
    public void testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientAdminOtherDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));
        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList), false,
                "API with  Role admin  visibility is  visible to Admin user with Admin and subscriber role in other " +
                        "domain in API Store." +
                        getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList), false,
                "API  with  Role Internal/subscriber  is  visible to Admin user with Admin and subscriber role in other " +
                        "domain in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for another user in same domain",
            dependsOnMethods = "testVisibilityForAdminWithAdminAndSubscriberRoleInOtherDomainInStore")
    public void testVisibilityForAnotherUserWithSubscriberRoleInSameDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientSubscriberUserSameDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList), false,
                "API with  Role admin  visibility is  visible to another user with subscriber role in same domain " +
                        "in API Store." + getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList), true,
                "API  with  Role Internal/subscriber  is not visible to another user with subscriber role in same " +
                        "domain in API Store. When Visibility is public. " +
                        getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility pf API in Store for another user in same domain",
            dependsOnMethods = "testVisibilityForAnotherUserWithSubscriberRoleInSameDomainInStore")
    public void testVisibilityForAnotherUserWithSubscriberRoleInOtherDomainInStore() throws Exception {

        List<APIIdentifier> apiStoreAPIIdentifierList =
                APIMgtTestUtil.getAPIIdentifierListFromHttpResponse(apiStoreClientSubscriberUserOtherDomain.
                        getAllPublishedAPIs(apiCreatorStoreDomain));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierAdminVisibility, apiStoreAPIIdentifierList), false,
                "API with  Role admin  visibility is  visible to another user with subscriber role in same domain " +
                        "in API Store." + getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertEquals(APIMgtTestUtil.isAPIAvailable(apiIdentifierSubscriberVisibility, apiStoreAPIIdentifierList), false,
                "API  with  Role Internal/subscriber  is  visible to another user with subscriber role in same domain " +
                        "in API Store. When Visibility is public. " + getAPIIdentifierString(apiIdentifierSubscriberVisibility));


    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in other domainStore for anonymous user",
            dependsOnMethods = "testVisibilityForAnotherUserWithSubscriberRoleInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInOtherDomainInStore() throws XPathExpressionException,
            APIManagerIntegrationTestException {

        HttpResponse httpResponse = new APIStoreRestClient(getStoreServerURLHttp()).getAPIStorePageAsAnonymousUser
                (otherDomainContext.getContextTenant().getDomain());

        assertEquals(httpResponse.getData().contains(API_NAME_ADMIN_VISIBILITY), false, "API with  Role admin  visibility " +
                " is  visible to anonymous user in other domain API Store." + getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertEquals(httpResponse.getData().contains(API_NAME_SUBSCRIBER_VISIBILITY), false, "API with  Role " +
                "Internal/subscriber is  visible to anonymous user in other domain API Store." +
                getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @Test(groups = {"wso2.am"}, description = "Test the visibility for API in Same domainStore for anonymous user",
            dependsOnMethods = "testVisibilityForAnonymousUserInOtherDomainInStore")
    public void testVisibilityForAnonymousUserInSameDomainInStore() throws XPathExpressionException,
            APIManagerIntegrationTestException {

        HttpResponse httpResponse = new APIStoreRestClient(getStoreServerURLHttp()).getAPIStorePageAsAnonymousUser(
                apimContext.getContextTenant().getDomain());
        assertEquals(httpResponse.getData().contains(API_NAME_ADMIN_VISIBILITY), false, "API with  Role admin  " +
                "visibility  is not visible to anonymous user in same domain API Store." +
                getAPIIdentifierString(apiIdentifierAdminVisibility));

        assertEquals(httpResponse.getData().contains(API_NAME_SUBSCRIBER_VISIBILITY), false, "API with  Role " +
                "Internal/subscriber is not visible to anonymous user in same domain API Store. " +
                getAPIIdentifierString(apiIdentifierSubscriberVisibility));

    }


    @AfterClass(alwaysRun = true)
    public void cleanup() throws Exception {
        deleteAPI(apiIdentifierAdminVisibility, apiPublisherClientUser1);
        deleteAPI(apiIdentifierSubscriberVisibility, apiPublisherClientUser1);
        userManagementClient.deleteUser("subscriberUser1");

    }

}
