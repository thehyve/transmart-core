appender("stdout", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
	pattern = "%d [%thread] [%level] %logger - %msg%n"
  }
}
logger("org.springframework.batch", INFO)
logger("example", INFO)
root(WARN, ["stdout"])