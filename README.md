## AWS Lambda Notification Service Example for Sunrise iOS App

[Sunrise iOS app](https://github.com/commercetools/commercetools-sunrise-ios) push notifications are triggered using Commercetools [subscriptions](http://dev.commercetools.com/http-api-projects-subscriptions.html). One of the possible [destinations](http://dev.commercetools.com/http-api-projects-subscriptions.html#destination) for a subscription is an [AWS SNS](https://aws.amazon.com/sns/) topic. Messages published to a topic can trigger lambda functions if subscribed.
This example will show you how you can easily get notifications to your mobile shop app running on [Commercetools](https://commercetools.com) platform, without a need for a server instance. The scope of this example covers _reservation notification_ scenario, triggered on `OrderCreated` [message](http://dev.commercetools.com/http-api-projects-messages.html#ordercreated-message).

### Setup

#### Credentials

In order to use this example you need:
- An active Commercetools project. Replace `PROJECT_KEY`, `SCOPE`, `CLIENT_ID`, `CLIENT_SECRET`, `API_URL` and `AUTH_URL` with values valid for your project.
- [AWS access key](https://aws.amazon.com/developers/access-keys/). Replace `AWS_ACCESS_KEY`, and `AWS_SECRET_KEY` with your key and secret.

#### Create a Topic

- Navigate to the SNS section of the AWS [console](https://console.aws.amazon.com/sns/v2).
- Create new topic, copy the `ARN` and use it to [subscribe](http://dev.commercetools.com/http-api-projects-subscriptions.html) to `OrderCreated` [message](http://dev.commercetools.com/http-api-projects-messages.html#ordercreated-message).
- Now navigate to _Applications_ section, and create new platform application. For the _push notification platform_, you should select _Apple production_ or _Apple development_ depending on your current app environment. Either upload the certificate file, or paste the certificate and private key into the popup and confirm to save.
- Copy the `ARN` you got for the newly created application, and set that value in `NotificationUtil.groovy`, line `platformEndpointRequest.setPlatformApplicationArn("arn:aws:sns:us-west-2:487164526243:app/APNS/Sunrise")`.

#### Build a JAR file

- Run `gradle fatJar` to build a JAR file which contains all dependencies used in the `NotificationUtil.groovy` script.
  - Optionally, you can use `gradle runScript` command to run the script locally for debugging purposes.

#### Add New Lambda Function

- Navigate to the Lambda section of the AWS [console](https://console.aws.amazon.com/lambda).
- Create new Lambda function, and use _configure function_ section to set the following values:
  - Configure _runtime_ to _Java 8_;
  - Function package should contain the JAR file created in the previous step;
  - _Handler_ should be set to _de.commercetools.NotificationService::handleRequest_ if you haven't changed the package name;
  - _Configure triggers_ section allow you to specify a trigger which will invoke your Lambda function. You should select SNS from the dropdown menu, and pick the topic created in the second step.
  
### Test

- Login with a valid customer account on your Sunrise app instance, and make sure to allow push notifications. Customer's push token will be stored in a [Customer](http://dev.commercetools.com/http-api-projects-customers.html#customer)'s [custom field](http://dev.commercetools.com/http-api-projects-custom-fields.html#customfields).
- Pick some product and make a reservation.
- The [Commercetools](https://commercetools.com) platform sends an `OrderCreated` message to the SNS you subscribed. The topic triggers the Lambda function, which retrieves customer's token from the Commercetools API, and passes it, along with the notification payload to the APNS platform application.