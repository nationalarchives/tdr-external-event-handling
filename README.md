# TDR External Event Handler
This project handles incoming external events that are received via an SQS Queue that triggers a lambda on receipt of a message. It is designed to extensibly support different types of message from different sources. 

## Configuration
There are two environment variables used by the Lambda function that will modify how functionality:

* `ALLOW_FILE_STATUS_UPDATE`: A boolean flag controlling whether the function should update the status of files in TDR via the consignment API. This should be disabled by setting to 'false' if the number of updates adversely affects performance or cost.  
* `DEBUG_INCOMING_MESSAGES`: A debug flag that will output the body of an SQS message received by the lambda to the Cloudwatch logs. This will help debug messages that trigger the "Unrecoginsed event type" message because the content is not recognised by a decoder.

## Adding new event types
There are two main steps to adding a new event type:

1. Create a new decoder class in the Lambda that extends the IncomingEvent object and then add relevant handler code.
2. Set up the relevant permissions in the SQS queue policy to allow the source system to send messages to the queue. These are managed in `tdr-terraform-environments`

The source system will also likely need to be configured by its own team to be able to send messages to the SQS queue. 

## Running locally
You will need credentials for the AWS environments you are running this for, either set as envrionment variables or in your AWS credentials file.

In the [LambdaRunner.java](src/main/scala/uk/gov/nationalarchives/externalevent/LambdaRunner.scala) file, replace the `TestAssetID` variable with a valid asset value from the `tdr-export-intg` S3 bucket. Note - the tags on this asset will be updated.

Set the following environment variables for running on integration:
```
CLIENT_SECRET_PATH=/intg/keycloak/backend_checks_client/secret
CLIENT_ID=tdr-backend-checks
DEBUG_INCOMING_MESSAGE=true (optional - use if need to see incoming message content)
```
Run the `LambdaRunner` app from IntelliJ.
