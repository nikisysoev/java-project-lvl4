setup:
	gradle wrapper --gradle-version 7.4

start:
	APP_ENV=development ./gradlew run

start-dist:
	APP_ENV=production ./build/install/app/bin/app

generate-migrations:
	./gradlew generateMigrations

clean:
	./gradlew clean

build:
	./gradlew clean build

install:
	./gradlew install

lint:
	./gradlew checkstyleMain checkstyleTest

report:
	./gradlew jacocoTestReport

test:
	./gradlew test

check-updates:
	./gradlew dependencyUpdates

.PHONY: build