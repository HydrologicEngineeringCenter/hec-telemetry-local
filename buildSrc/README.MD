# buildSrc

The `buildSrc` directory contains a set of template Gradle files that can be pulled in to other builds like plugins.
This allows autoconfiguring many components of the build with a single line in the `build.gradle` file, and avoids 
copying and pasting a lot of boilerplate code. The single location of all of the logic also makes it much easier to
update for the entire library, such as updating the version of Java.

CWMS and Shared Code repositories have been updated in order to standardize to use the following three types of
templates:
- deps-conventions - for defining repositories that artifacts are pulled from
- java-conventions - for defining how to build java libraries: building jars, javadocs, and sources. specifying the JDK
version and adding the test library engine (JUnit) to the gradle runner
- publishing-conventions - for defining where to publish artifacts