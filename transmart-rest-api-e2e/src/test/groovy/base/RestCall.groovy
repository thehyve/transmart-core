package base

class RestCall {
    def final path;
    def final acceptType;
    def contentType;

    def statusCode = 200;
    def query = null;
    def body = null;
    def oauth = true;

    RestCall(path, acceptType) {
        this.path = path
        this.acceptType = acceptType
        this.contentType = acceptType
    }
}
