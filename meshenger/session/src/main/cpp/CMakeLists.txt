cmake_minimum_required(VERSION 3.10.2)

# Name of your native library
project("session")

# Build a shared library from your C++ source files
add_library(
        session                # <-- must match System.loadLibrary("session")
        SHARED
        HelloWorld.cpp         # add more .cpp files here if needed
)

# Find Android log library (for logging with __android_log_print)
find_library(
        log-lib
        log
)

# Link your native library against the log library
target_link_libraries(
        session
        ${log-lib}
)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
# Force shared STL
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -lc++_shared")