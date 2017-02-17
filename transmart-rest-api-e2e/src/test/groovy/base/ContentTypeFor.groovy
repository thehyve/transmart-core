package base

/**
 * Created by barteldklasens on 2/17/17.
 */
public interface ContentTypeFor {
    String HAL = 'application/hal+json',
            XML = 'application/xml',
            octetStream = 'application/octet-stream',
            Protobuf = 'application/x-protobuf',
            JSON = 'application/json'
}