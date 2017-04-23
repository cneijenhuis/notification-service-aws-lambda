package de.commercetools

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.services.sns.model.PublishResult
import groovy.json.JsonSlurper
import groovyx.net.http.RESTClient

import java.nio.charset.StandardCharsets

import static groovyx.net.http.ContentType.JSON

class NotificationService implements RequestHandler<SNSEvent, Object> {

    Object handleRequest(SNSEvent request, Context context) {
        JsonSlurper slurper = new JsonSlurper()
        Map map = slurper.parseText(request.getRecords().get(0).getSNS().getMessage())
        
        sendReservationNotification(map)
        
        return null
    }

    void sendReservationNotification(Map payload) {
        if (payload.order?.custom?.fields?.isReservation) {
            String reservationPayload = "{\"APNS\":\"{\\\"aps\\\":{\\\"alert\\\":\\\"Hello, your item is ready for pickup.\\\",\\\"category\\\":\\\"reservation_confirmation\\\"},\\\"reservation-id\\\":\\\"${payload.order.id}\\\"}\"}"
            pushNotification(payload.order?.customerId, reservationPayload)
        }
    }

    private void pushNotification(String userId, String payload) {
        String userToken = retrieveTokenForUserId(userId)
        if (userToken) {
            println "User has associated APNS token, sending out notification..."

            AmazonSNS snsClient = new AmazonSNSClient(new BasicAWSCredentials(AWS_ACCESS_KEY, AWS_SECRET_KEY))
            snsClient.setEndpoint("https://sns.us-west-2.amazonaws.com")

            PublishRequest publishRequest = new PublishRequest()
            publishRequest.setMessageStructure("json")
            CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest()
            platformEndpointRequest.setToken(userToken)
            platformEndpointRequest.setPlatformApplicationArn("arn:aws:sns:us-west-2:437163536233:app/APNS/YourAppName")
            CreatePlatformEndpointResult endpointResult = snsClient.createPlatformEndpoint(platformEndpointRequest)
            publishRequest.setTargetArn(endpointResult.endpointArn)
            publishRequest.setMessage(payload)
            PublishResult result = snsClient.publish(publishRequest)
            println(result)

        } else {
            println "User doesn't have associated APNS token, cannot send notification..."
        }
    }

    private String retrieveTokenForUserId(String userId) {
        if (userId == null) {
            return null
        }
        String accessToken = getAccessToken()
        def commercetoolsClient = new RESTClient("${API_URL}${PROJECT_KEY}/customers/${userId}")
        def response = commercetoolsClient.get(requestContentType: JSON, headers: [Authorization: "Bearer ${accessToken}"])
        return response.data?.custom?.fields?.apnsToken
    }

    private String getAccessToken() {
        def authClient = new RESTClient("${AUTH_URL}oauth/token?grant_type=client_credentials&scope=${SCOPE}")
        def response = authClient.post(requestContentType: JSON, headers: [Authorization: "Basic ${Base64.encoder.encodeToString("${CLIENT_ID}:${CLIENT_SECRET}".getBytes(StandardCharsets.UTF_8))}"])
        return response.data?.access_token
    }
}
