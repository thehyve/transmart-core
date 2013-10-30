package inc

class Log {
    static void err(message) {
        System.err.println "\u001b[0;31m$message\u001b[m"
    }
    static void out(message) {
        System.out.println message
    }
}
