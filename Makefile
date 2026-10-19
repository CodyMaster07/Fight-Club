test: all run
all:
	mvn compile -e
run:
	java -cp target/classes com.game.Main
