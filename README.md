# How to install
1. Download from this [link](http://sourceforge.net/projects/mpjexpress/files/releases)
2. File -> Project Structure -> Project SDK -> Select
3. File -> Project Structure -> Project compiler output -> `path_to_project/out`
4. File -> Project Structure -> Modules -> Dependencies -> Add > JARs or directories -> `/$MPJ_HOME$/lib/mpi.jar`
5. Add/Edit configuration -> Kotlin
6. VM Options : `-jar /$MPJ_HOME$/lib/starter.jar -np 6`
7. Environment variables : `MPJ_HOME=/path_to_mpj`