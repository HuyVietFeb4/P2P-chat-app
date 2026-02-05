cmake_minimum_required(VERSION 3.10.2)

# Name of your native library
project("session")

# Build a shared library from your C++ source files
add_library(
        ${CMAKE_PROJECT_NAME}
        SHARED
        HelloWorld.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        android
        ${log-lib}
)

#set(CMAKE_CXX_STANDARD 17)
#set(CMAKE_CXX_STANDARD_REQUIRED ON)
## Force shared STL
#set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -lc++_shared")