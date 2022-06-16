## Getting Started

### Core SDK

First access the mParticle Core SDK repository via the following
link: [mParticle GitHub Repo](https://github.com/mParticle/mparticle-android-sdk)
You can easily "fork" the repository by clicking the **Fork** tab in the upper right corner, once
that is done you should be able to see it in your own Github account under the **Repositories** tab.
Open your forked version and in the green **Code** tab copy the https link. Go ahead and open
Android Studio and select Project from Version Control or Get from VCS, paste the Repository link
you just copied and select **Clone**.

### Testing the Core SDK

Once the project is loaded, run the default tests to validate that your environment is set up
correctly and to check that the code is working correctly before making changes of your own.

You can do so locally using the the following gradle wrapper commands in your terminal:

For Lint Checks:
`./gradlew lint`

For Unit Tests:
`./gradlew test`

For Instrumented Tests:
`./gradlew :android-core:cAT :android-kit-base:cAT --stacktrace`

In some machines running windows you may need to type the commands without **./**
eg. `gradlew lint`

#### Issues with JAVA_HOME

When running the tests for the first time, It is possible that you run into the following issue:

**ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.**

**Please set the JAVA_HOME variable in your environment to match the location of your Java
installation.**

Make sure that you have installed the Java 11 JDK and the path **JAVA_HOME** is created in the
system Environment Variables.

To install the JDK software, do the following:
Go to [Official Oracle JDK download Website](https://www.oracle.com/java/technologies/downloads/)
select the appropriate JDK software and click Download. The JDK software is installed on your
computer in the default location; for example, at C:\Program Files\Java\jdk1.6.0_02.

To set JAVA_HOME, do the following:

In Windows:
Right click My Computer and select Properties. On the Advanced tab, select Environment Variables,
and then edit *JAVA_HOME* to point to where the JDK software is located, for example, C:\Program
Files\Java\jdk1.6.0_02

In Mac: Open the `.bash_profile` file and add the following
line: `export PATH="{PATH_TO_MY_JAVA}:$PATH"`, where *PATH_TO_MY_JAVA* is the file path for you JDK
installation, for example, /usr/libexec/java_home

Lastly, edit the **Path** system variable and add a new variable *%JAVA_Home%\bin*.

Additionally If you have multiple JDKs downloaded make sure that your Gradle JDK matches Java Home.
You can do this by going in Android Studio to:  
Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> and choose the matching JDK
version in the **Gradle JDK** dropdown menu.

Remember to close and reopen your project after you've made your changes or the error message may
persist.

### Downloading the mParticle Kits

Now we will download the different available Kits which are developed as individual submodules each
having a dedicated repository. First make sure you have set both a SSH key and GPG key in your
GitHub account, SSH is used for authentication while GPG is used for signing tags and commits.

Now type the following commands `git submodule init` (creates the local configuration file for the
submodules). followed by `git submodule update --recursive` (updates all available submodules).

Or you can do it in one line with `git submodule update --init`

An error regarding the Javadoc file generation, **Illegal Package Name** may come up referencing
some files with the .kt extension, a current quick fix for this is adding the following code in
the `build.gradle` file:

```groovy
tasks.withType(Javadoc) {
    excludes = ['**/*.kt']    //Excludes all kotlin files from javadoc file
}
```

Once you have run both commands you should see your newly populated kit files easily in the Project
view -> **Kits** folder.

### Building and publishing core modules to the Maven Local Repository

Now we will build our Local Maven repository, this will allow us to make changes to our Kits and
publish it locally so that we can test them.

Run the following command in the terminal:
`./gradlew buildLocal`
`./gradlew -PisRelease=true clean publishReleaseLocal`

This will publish the core modules to mavenLocal(), you should see all .pom and .aar files located
in your local /.m2 folder.)

Now add mavenLocal() in repositories in the project-level `build.gradle` file:

```groovy
buildscript {
    repositories {
        mavenLocal()
    }
}
```

### Testing and Publishing Kits to the Maven Local Repository

To publish the Kits to mavenLocal first make sure you have only selected the current Kit you are
working on in the `settings-kits.gradle`, you can do so easily by commenting out the ones not
needed:

```groovy
include(
        ':kits:adjust-kit',
        ':kits:adobe-kit'
        /* ':kits:adobemedia-kit',
            ':kits:appboy-kit',
            ...                     */
)
```

This will ensure faster running times and easier error troubleshooting.

Now run the following command in the terminal:

`./gradlew -PisRelease=true clean testRelease publishReleaseLocal -c settings-kits.gradle`

You can now work on the specific kits you need, test them and even contribute through pull requests.

## Read More

* [Official Oracle JDK download Website](https://www.oracle.com/java/technologies/downloads/)
* [Adding a new SSH key to your Github account](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account)
* [Adding a new GPG key to your Github account](https://docs.github.com/en/authentication/managing-commit-signature-verification/adding-a-new-gpg-key-to-your-github-account)
