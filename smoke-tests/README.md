# Smoke Tests

## Running the smoke tests

1. Make sure the environment is set up.
2. Ensure you are in the `smoke-tests` directory.
3. Run `./run-smoke-tests.sh <path to your vars.sh script>`, where the parameter is the location of your configuration shell script.

### Troubleshooting on M1 Mac

Issue: `zsh: permission denied: ./run-smoke-tests.sh`

Resolution: To give the smoke tests script permission to run, execute `chmod +x run-smoke-tests.sh` in your terminal.

----

Issue: `zsh: permission denied: ./gradlew`

Resolution: To give `gradlew` permission to run, execute `chmod +x gradlew` in your terminal.

----

Issue: `Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

Resolution: Ensure Gradle is installed and is the correct version within the `smoke-tests` directory. Run `brew install gradle` to install it.
