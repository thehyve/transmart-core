#tranSMART RESTful API documentation




### Available calls for the tranSMART RESTful API

The following is a list of all HTTP requests that can be made to a tranSMART RESTful API. All URI's are relative to your tranSMART server's URI (eg. http://some.transmart.server/transmart).

The return message's body will contain JSON with additional HAL format, with the exception of the request for high dimensional data (see section below).

| Call | HTTP request | Description |
| --- | --- | --- |
| get all studies | GET `/studies`  | returns information on all available studies |
| get single study | GET `/studies/{studyId}`  | returns information on a single study |
| get all concepts | GET `/studies/{studyId}/concepts` | returns information on all concepts for a study |
| get single concept | GET `/studies/{studyId}/concepts/{conceptPath}` | returns information on one concept for a study |
| get all subjects | GET `/studies/{studyId}/subjects` | returns information on all subjects for a study |
| get single subject | GET `/studies/{studyId}/subjects/{subjectId}` | returns information on one subject for a study |
| get subjects with concept | GET `/studies/{studyId}/concepts/{conceptPath}/subjects` | returns the subjects which have data for this concept |
| get all observations | GET `/studies/{studyId}/observations` | returns all clinical observation values for a study |
| get observations for single concept | GET `/studies/{studyId}/concepts/{conceptPath}/observations` | returns clinical observation values for one concept for a study |
| get index of highdim data for single concept| GET `/studies/{studyId}/concepts/{conceptPath}/highdim` | returns index with the available datatype and projections for this highdim concept for a study |
| get highdim data for single concept| GET `/studies/{studyId}/concepts/{conceptPath}/highdim?dataType={dataType}&projection={projectionType}` | returns highdim data of a specific dataType and projection for one concept of a study |


#### Explanation of URI variables
| variable  | explanation |
| --- | --- |
| {studyId} | The id of the study, as returned by the `/studies` call. |
| {conceptPath} | A path that defines the concept within a study. This is similar to the concept path as defined within tranSMART, but without the initial part that defines the study path (and with necessary character conversion to make it compatible with URI syntax). The safest and most robust method of obtaining this value is by making use of the embedded links in the `/studies/{studyId}/concepts` result. |
| {subjectId} | A unique subject identifier, as returned by `/studies/{studyId}/subjects` call. |
| {dataType} | High dimensional concepts can be of several types, depending on what your tranSMART version supports. Possible data type options are contained in the highdim index returned by the `/studies/{studyId}/concepts/{conceptPath}/highdim` call. |
| {projectionType} | High dimensional data can have values stored in a variety of projections. Possible projection options are contained in the highdim index returned by the `/studies/{studyId}/concepts/{conceptPath}/highdim` call. |

#### HTTP exchange details
Each of the above GET requests needs two header fields set:

1. Authentication: if your tranSMART server requires OAuth authentication (see section on OAuth below), then this header needs to have a value set to `Bearer {accessToken}`.
2. accept: the return message will contain a body that contains JSON with HAL format. This needs to be specifically requested by setting this field to `application/hal+json`. Otherwise, plain JSON will be returned, and you will miss out on all the HAL explorable goodness.

Here is an example HTTP request for the `/studies` call:

    GET /transmart/studies HTTP/1.1
    Host: some.transmart.server
    Authorization: Bearer 12345-abcde
    accept: application/hal+json
    
    
### High dimensional protobuf data
To facilitate the many variants of high dimensional data formats to be returned by tranSMART, we have chosen to use Google's protobuf solution. Whereas all other calls return a body containing JSON with additional HAL format, the request for high dimensional data returns a binary protobuf stream.

This binary stream can be parsed using the protobuf library, the implementation of which depends on the libraries available for your client application. The exact structure of the incoming data is defined in the `highdim.proto` file, which is available in the `src/protobuf/highdim` folder of the transmart-rest-api repository. This proto file must be included in your client application's resources and loaded by your protobuf instance, for it allows the correct parsing of the messages contained in the binary stream.

There are two types of messages contained in the binary stream, which need to be parsed differently. The first message contains header information, and all subsequent messages will each contain information on one row of your dataset.

The header message primarily contains the definition of all assays in your dataset, for which the subsequent row messages will contain the values. An assay is usually equivalent to a single subject, but please see the `highdim.proto` file for what else is definable per assay (under `message Assay`). In addition to the assay information, the header message also defines the name and type for each of the returned values in the row messages (under `message ColumnSpec`)

Each row message contains one `ColumnValue` item per assay, and the order is identical to the order of the assay definitions in the header. The content of this item is defined in the `highdim.proto` file under `message ColumnValue`, and it can contain multiple values (eg. z-score and log-value), for which the name and type is defined in the header (under `message ColumnSpec`).

Example code for how to parse the protobuf binary stream into something sensible, please see the [transmartRClient](https://github.com/thehyve/RInterface/blob/transmartRClient/transmartRClient/R/getHighdimData.R#L50).


### Authentication with OAuth

The RESTful API supports authentication with OAuth 2.0. The client application needs to be registered with the OAuth server, after which each end-user of that client can authenticate with the OAuth server to allow the client access to all of tranSMART's resources on the end-user's behalve. This is done via the following steps:

1. End-users need to be redirected to the following OAuth URI to be visited in a web browser. Here they can authenticate themselves for this client application:
`{oauthServer}/oauth/authorize?response_type=code&client_id={clientId}&client_secret={clientSecret}&redirect_uri={oauthServer}/oauth/verify`

2. After the end-user has succesfuly authenticated at this URI, a request token is supplied, which the end-user needs to copy and paste as input to your client.

3. Your client needs to exchange this request token for a semi-permanent access token, using the following HTTP request: GET `{oauthServer}/oauth/token?grant_type=authorization_code&client_id={clientId}&client_secret={clientSecret}&code={requestToken}&redirect_uri={oauthServer}/oauth/verify`

The response of step 3 will be JSON containing the access token, in addition to its type, a refresh token (not yet supported), and when the access token will expire in seconds:

    {
        "access_token" : "12345-abcde",
        "token_type" : "bearer",
        "refresh_token" : "67890-fghij",
        "expires_in" : 99999 
    }

#### Explanation of URI variables

| variable  | explanation |
| --- | --- |
| {oauthServer} | The URI of the OAuth server to be used. By default this will be identical to the URI of your tranSMART server. |
| {clientId} | The client id assigned to your client application after registering it with the OAuth server. |
| {clientSe****cret} | The client secret assigned to your client application after registering it with the OAuth server. |
| {requestToken} | The temporary token received by your end-user after authenticating, and which needs to be exchanged by your client for an access token. |
