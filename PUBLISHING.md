# Publishing protocol

## Create release branch

```sh
git branch release
git checkout release
```

## Increase version number

In `app/build.gradle`, increase versionCode and versionName to the release version. The minor
version part should be even.

## Commit and tag new version

```sh
git add app/build.gradle
git commit
git tag VERSION
```

## Build release package

Build signed bundle or APK: Build -> Generate signed...

## Upload to Google

The release package is built in `app/release`. Upload this file to google and create a release.

## Push tags and cleanup

```sh
git push --tags
git checkout main
git branch -D release
```

## Increase development version

In `app/build.gradle`, increase `versionCode` and `versionName` again. This time, the minor version
should be odd numbered.
