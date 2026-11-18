.PHONY: all test run
test:
	mvn test
all:
	mvn compile -e
run: all
	mvn exec:java
