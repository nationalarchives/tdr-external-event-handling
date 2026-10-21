# TDR External Event Handler
This project handles incoming external events that are received via an SQS Queue that triggers a lambda on receipt of a message. It is designed to extensibly support different types of message from different sources. 

## Configuration
The following environment variables control message handling:

* `ALLOW_FILE_STATUS_UPDATE`: A boolean flag controlling whether the function should update the status of files in TDR via the consignment API. This should be disabled by setting to 'false' if the number of updates adversely affects performance or cost.  
* `DEBUG_INCOMING_MESSAGES`: A debug flag that will output the body of an SQS message received by the lambda to the Cloudwatch logs. This will help debug messages that trigger the "Unrecoginsed event type" message because the content is not recognised by a decoder.

## DR2 ingest-complete tagging

On `preserve.digital.asset.ingest.complete`, the handler adds `PreserveDigitalAssetIngest=Complete` to exported records and metadata sidecars. For each record it also reads the export object's contextual tags and tags the upstream copies, preserving existing tags:

| Bucket configuration | Environment variable | Object key |
| --- | --- | --- |
| `s3.dirtyBucket` | `DIRTY_BUCKET` | `{UserId}/{ConsignmentId}/{fileId}`, or `{UserId}/{AssetSource}/{ConsignmentId}/{fileId}` when `AssetSource` is present |
| `s3.cleanBucket` | `CLEAN_BUCKET` | `{ConsignmentId}/{fileId}` |
| `s3.quarantineBucket` | `QUARANTINE_BUCKET` | `{ConsignmentId}/{fileId}` |

Bucket defaults target integration; set all three variables for the deployed environment. The contextual tag names default to `ConsignmentId`, `UserId`, and `AssetSource` and can be overridden with `CONSIGNMENT_ID_TAG`, `USER_ID_TAG`, and `ASSET_SOURCE_TAG`, matching the consignment export configuration.

Missing or empty consignment/user tags cause a warning and skip upstream tagging for that record; export tagging and file-status updates continue. An absent upstream object (`NoSuchKey`) is logged as a warning. Other upstream S3 errors are logged as errors, and processing continues with the remaining copies. These logged failures do not trigger SQS retries.

The Lambda role needs `s3:GetObjectTagging` and `s3:PutObjectTagging` on objects in all three upstream buckets, in addition to its existing export permissions. The corresponding environment variables and permissions are configured in `tdr-terraform-environments/root_external_event_handling.tf` and `templates/iam_policy/external_event_handler_lambda_policy.json.tpl`. Contextual tagging must also be enabled in consignment export (`BLOCK_ADD_CONTEXT_TAGGING=false`) for upstream tagging to work.

Metadata sidecars have no upstream record copy and are only tagged in the export bucket. BagIt packages in `tdr-consignment-export-{env}` are excluded (TDRD-1758).

## Adding new event types
There are two main steps to adding a new event type:

1. Create a new decoder class in the Lambda that extends the IncomingEvent object and then add relevant handler code.
2. Set up the relevant permissions in the SQS queue policy to allow the external source system to send messages to the queue. These are managed in `tdr-terraform-environments`

The external source system will also likely need to be configured by its own team to be able to send messages to the SQS queue. 

## Running locally
You will need credentials for the AWS environments you are running this for, either set as envrionment variables or in your AWS credentials file.

In the [LambdaRunner.java](src/main/scala/uk/gov/nationalarchives/externalevent/LambdaRunner.scala) file, replace the `TestAssetID` variable with a valid asset value from the `tdr-export-{env}` S3 bucket. Note - tags on this asset and its upstream record copies will be updated.

Set the following environment variables for running on respective environments:
```
CLIENT_SECRET_PATH=/{env}/keycloak/backend_checks_client/secret
CLIENT_ID=tdr-backend-checks
DEBUG_INCOMING_MESSAGE=true (optional - use if need to see incoming message content)
```
Run the `LambdaRunner` app from IntelliJ.
