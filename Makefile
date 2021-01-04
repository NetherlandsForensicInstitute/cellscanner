APK=app/build/outputs/apk/release/app-release-unsigned.apk

all: $(APK)

$(APK):
	./update-revision.sh
	./gradlew build

install: all
	cp $(APK) cellscanner.apk

clean:
	rm -f cellscanner.apk
	./gradlew clean
