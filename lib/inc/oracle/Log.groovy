package inc.oracle

class Log {
    static void err(message) {
        System.err.println "\u001b[1;31mERROR\u001b[m: \u001b[1m$message\u001b[m"
    }
    static void warn(message) {
        System.err.println "\u001b[1;33mWARN\u001b[m: \u001b[1m$message\u001b[m"
    }
    static void out(message) {
        System.out.println message
    }
}
